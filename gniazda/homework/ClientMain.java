import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.*;
import java.io.*;

public class ClientMain {
  public static void main(String[] args) {
    new Client("localhost", 5000).start();
  }
}

class Client {
  private final String host;
  private final int port;
  private final AtomicBoolean running;
  private final String asciiCode;
  private static final String ASCII_ART = """
       /\\_/\\
      ( o.o )
       > ^ <
      """;

  public Client(String host, int port) {
    this.host = host;
    this.port = port;
    this.running = new AtomicBoolean(false);
    this.asciiCode = "U";
  }

  public void start() {
    running.set(true);
    try (Socket tcpSocket = new Socket(host, port);
        DatagramSocket udpSocket = new DatagramSocket();
        Scanner scanner = new Scanner(System.in);
        PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true)) {

      System.out.println("Connected to server at " + host + ":" + port);
      out.println(udpSocket.getLocalPort());
      System.out.println("UDP port sent to server: " + udpSocket.getLocalPort());

      new Thread(() -> listenTCP(tcpSocket)).start();
      new Thread(() -> listenUDP(udpSocket)).start();

      while (running.get()) {
        if (!scanner.hasNextLine())
          break;
        String message = scanner.nextLine();
        if (asciiCode.equals(message)) {
          sendASCIIArt(udpSocket);
        } else {
          sendMessage(message, out);
        }
      }
    } catch (IOException e) {
      System.err.println("Error connecting to server: " + e.getMessage());
    }
  }

  private void sendMessage(String message, PrintWriter out) {
    out.println(message);
    if ("quit".equalsIgnoreCase(message)) {
      running.set(false);
    }
  }

  private void sendASCIIArt(DatagramSocket udpSocket) {
    try {
      byte[] buffer = ASCII_ART.getBytes();
      InetAddress serverAddress = InetAddress.getByName(host);

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, port);

      udpSocket.send(packet);
    } catch (IOException e) {
      System.err.println("Failed to send UDP art: " + e.getMessage());
    }
  }

  private void listenTCP(Socket tcpSocket) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()))) {
      String serverMessage;

      while (running.get() && (serverMessage = in.readLine()) != null) {
        System.out.println(serverMessage);
      }
    } catch (IOException e) {
      if (running.get()) {
        System.err.println("Connection lost: " + e.getMessage());
      }
    } finally {
      running.set(false);
      try {
        tcpSocket.close();
      } catch (IOException ignored) {
      }
    }
  }

  private void listenUDP(DatagramSocket udpSocket) {
    byte[] buffer = new byte[1024];

    try {
      while (running.get()) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        udpSocket.receive(packet);

        String message = new String(
            packet.getData(), 0, packet.getLength());

        System.out.println("[UDP]\n" + message);
      }
    } catch (IOException e) {
      if (running.get()) {
        System.err.println("UDP error: " + e.getMessage());
      }
    }
  }
}