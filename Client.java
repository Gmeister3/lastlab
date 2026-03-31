import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client – a TCP client that connects to ConcurrentServer and exchanges SPEPacket objects.
 *
 * The user types commands interactively:
 *   ECHO <message>
 *   UPPERCASE <message>
 *   REVERSE <message>
 *   SAVE <data>
 *   QUIT
 */
public class Client {

    public static void main(String[] args) {
        String host = "localhost";

        try (
            Socket socket = new Socket(host, ProtocolConstants.PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in)
        ) {
            // Restrict deserialization to known-safe classes only
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "SPEPacket;java.lang.*;!*"));
            System.out.println("Connected to ConcurrentServer at "
                    + host + ":" + ProtocolConstants.PORT);
            System.out.println("Commands: ECHO <msg>  UPPERCASE <msg>  REVERSE <msg>  SAVE <data>  QUIT");

            boolean running = true;
            while (running) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;

                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                // Split on first whitespace to separate command from payload
                String command;
                String payload = "";
                int spaceIndex = line.indexOf(' ');
                if (spaceIndex == -1) {
                    command = line.toUpperCase();
                } else {
                    command = line.substring(0, spaceIndex).toUpperCase();
                    payload = line.substring(spaceIndex + 1);
                }

                SPEPacket request = new SPEPacket(command, payload);
                out.writeObject(request);
                out.flush();

                SPEPacket response = (SPEPacket) in.readObject();
                System.out.println("Server response [" + response.getHeader() + "]: "
                        + response.getPayload());

                if (ProtocolConstants.QUIT.equals(response.getHeader())) {
                    running = false;
                }
            }

            System.out.println("Connection closed.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
