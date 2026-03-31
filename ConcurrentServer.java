import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * ConcurrentServer – a TCP server that accepts multiple simultaneous client connections.
 *
 * Key design decisions:
 *  - ServerSocket replaces DatagramSocket (TCP vs. UDP).
 *  - A FixedThreadPool (ExecutorService) dispatches each accepted connection to a worker
 *    thread, preventing one slow client from blocking others.
 *  - Communication is performed via ObjectOutputStream / ObjectInputStream so that
 *    SPEPacket objects are transmitted in serialized form rather than raw strings.
 *  - InetAddress is used to log each connecting client's host name and IP address.
 */
public class ConcurrentServer {

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(ProtocolConstants.POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(ProtocolConstants.PORT)) {
            System.out.println("ConcurrentServer listening on port " + ProtocolConstants.PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Log client origin using InetAddress
                InetAddress clientAddress = clientSocket.getInetAddress();
                System.out.println("New connection from " + clientAddress.getHostName()
                        + " (" + clientAddress.getHostAddress() + ")");

                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    /** Class-level lock that serializes file writes across all ClientHandler instances. */
    private static final Object FILE_LOCK = new Object();

    // -------------------------------------------------------------------------
    // Inner class: handles one client connection on a worker thread
    // -------------------------------------------------------------------------
    private static class ClientHandler implements Runnable {

        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())
            ) {
                // Restrict deserialization to known-safe classes only
                in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                        "SPEPacket;java.lang.*;!*"));

                boolean running = true;
                while (running) {
                    SPEPacket request = (SPEPacket) in.readObject();
                    System.out.println("Received: " + request);

                    String command = request.getHeader();
                    String payload = request.getPayload();
                    SPEPacket response;

                    switch (command) {
                        case ProtocolConstants.ECHO:
                            response = new SPEPacket(ProtocolConstants.ECHO, payload);
                            break;

                        case ProtocolConstants.UPPERCASE:
                            response = new SPEPacket(ProtocolConstants.UPPERCASE,
                                    payload.toUpperCase());
                            break;

                        case ProtocolConstants.REVERSE:
                            response = new SPEPacket(ProtocolConstants.REVERSE,
                                    new StringBuilder(payload).reverse().toString());
                            break;

                        case ProtocolConstants.SAVE:
                            saveToFile(payload);
                            response = new SPEPacket(ProtocolConstants.SAVE,
                                    "Data saved successfully.");
                            break;

                        case ProtocolConstants.QUIT:
                            response  = new SPEPacket(ProtocolConstants.QUIT, "Goodbye.");
                            running   = false;
                            break;

                        default:
                            response = new SPEPacket("ERROR",
                                    "Unknown command: " + command);
                            break;
                    }

                    out.writeObject(response);
                    out.flush();
                }
            } catch (EOFException | SocketException e) {
                // Client disconnected cleanly
                System.out.println("Client disconnected.");
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Handler error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        /** Appends the given data to the persistence file using BufferedWriter. */
        private void saveToFile(String data) {
            synchronized (FILE_LOCK) {
                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(ProtocolConstants.SAVE_FILE, true))) {
                    writer.write(data);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("Failed to save data: " + e.getMessage());
                }
            }
        }
    }
}
