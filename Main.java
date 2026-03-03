import java.net.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.err.println(serverSocket.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("hello");
    }
}
