import java.net.*;
import java.io.*;

public class Main {

    private static final int PORT = 5000;

    public static void main(String[] args) {

        System.out.println("Starting server on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            Socket clientSocket = listenForClient(serverSocket);

            handleClient(clientSocket);

        } catch (IOException e) {
            System.err.println("Fatal server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Socket listenForClient(ServerSocket serverSocket) throws IOException {
        System.out.println("Waiting for client connection...");
        return serverSocket.accept();
    }

    private static void handleClient(Socket clientSocket) {

        System.out.println("Client connected: " + clientSocket.getInetAddress());

        try (
            Socket socket = clientSocket;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {

            String message;
            while (true) {
                try {
                    message = in.readLine();

                    if (message == null) {
                        System.out.println("Client closed the connection unexpectedly.");
                        break;
                    }

                    System.out.println("Received: " + message);

                    if (message.equalsIgnoreCase("exit")) {
                        System.out.println("Client requested to disconnect.");
                        break;
                    }

                    out.println("REC");

                    if (out.checkError()) {
                        System.err.println("Error while sending confirmation.");
                        break;
                    }
                } catch (IOException e) {
                    System.err.println("I/O error during communication: " + e.getMessage());
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Error setting up client streams: " + e.getMessage());
        }

        System.out.println("Client disconnected.");
    }
}