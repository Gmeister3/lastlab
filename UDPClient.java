import java.io.*;
import java.net.*;

public class UDPClient {
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 8080;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(hostname); // Address container 
            
            String message = "Hello UDP Server!";
            byte[] buffer = message.getBytes();

            // Create and send the packet 
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(request);

            // Prepare to receive the echo response
            byte[] responseBuffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(response);

            String echoed = new String(response.getData(), 0, response.getLength());
            System.out.println("Server echoed: " + echoed);

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}