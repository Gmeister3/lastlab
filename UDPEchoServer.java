import java.io.*;
import java.net.*;

public class UDPEchoServer {
    public static void main(String[] args) {
        int port = 8080;
        // DatagramSocket is used for UDP instead of ServerSocket 
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP Echo Server started on port " + port);
            byte[] buffer = new byte[1024];

            while (true) {
                // Prepare a packet to receive data 
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request); // This blocks until a packet arrives

                String message = new String(request.getData(), 0, request.getLength());
                InetAddress clientAddress = request.getAddress(); // Identifies the endpoint 
                int clientPort = request.getPort();

                System.out.println("Received from " + clientAddress + ": " + message);

                // Prepare response packet 
                byte[] responseData = ("Echo: " + message).getBytes();
                DatagramPacket response = new DatagramPacket(
                    responseData, responseData.length, clientAddress, clientPort
                );
                
                socket.send(response); // Best effort delivery 
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}