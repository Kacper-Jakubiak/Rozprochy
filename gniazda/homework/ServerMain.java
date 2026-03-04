import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerMain {
  private static final int PORT = 5000;

  public static void main(String[] args) {
    new Server(PORT).start();
  }
}

class Server {
  private final Set<PrintWriter> clientWriters;
  private final ExecutorService threadPool;
  private final Set<InetSocketAddress> udpClients;
  private DatagramSocket udpSocket;
  private final int port;

  public Server(int port) {
    this.clientWriters = Collections.synchronizedSet(new HashSet<>());
    this.udpClients = Collections.synchronizedSet(new HashSet<>());
    this.threadPool = Executors.newCachedThreadPool();
    this.port = port;
  }

  public void start() {
    try {
      udpSocket = new DatagramSocket(port);
    } catch (SocketException e) {
      System.err.println("Error starting UDP socket: " + e.getMessage());
      return;
    }
    try (ServerSocket serverSocket = new ServerSocket(port);) {
      threadPool.submit(this::udpHandler);
      System.out.println("Server started on port " + port);

      while (!threadPool.isShutdown()) {
        try {
          Socket clientSocket = serverSocket.accept();
          threadPool.submit(new ClientHandler(clientSocket));
        } catch (IOException e) {
          System.err.println("Error accepting client connection: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.err.println("Error starting server: " + e.getMessage());
    } finally {
      shutdown();
    }
  }

  private void broadcastMessage(String message, PrintWriter sender) {
    for (PrintWriter writer : clientWriters) {
      if (writer != sender) {
        writer.println(message);
      }
    }
  }

  private void broadcastASCII(DatagramPacket packet, InetSocketAddress senderAddress) {
    for (InetSocketAddress client : udpClients) {
      if (client.equals(senderAddress)) {
        continue;
      }
      try {
        udpSocket.send(new DatagramPacket(packet.getData(), packet.getLength(), client.getAddress(), client.getPort()));
      } catch (IOException e) {
        System.err.println("Error sending UDP message to " + client + ": " + e.getMessage());
      }
    }
  }

  public void shutdown() {
    System.out.println("Shutting down server...");
    if (udpSocket != null) {
      udpSocket.close();
    }
    threadPool.shutdown();
    try {
      if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
      }
    } catch (InterruptedException e) {
      threadPool.shutdownNow();
    }
    clientWriters.forEach(PrintWriter::close);
    System.out.println("Server stopped.");
  }

  private void udpHandler() {
    byte[] buffer = new byte[1024];
    while (!threadPool.isShutdown()) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        udpSocket.receive(packet);
        InetSocketAddress clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
        broadcastASCII(packet, clientAddress);
      } catch (IOException e) {
        System.err.println("Error handling UDP message: " + e.getMessage());
      }
    }
  }

  class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      PrintWriter out = null;
      try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {

        out = new PrintWriter(socket.getOutputStream(), true);
        clientWriters.add(out);
        System.out.println("New client connected: " + socket.getRemoteSocketAddress());

        System.out.println("Waiting for UDP address...");
        String message = in.readLine();

        int clientUdpPort = Integer.parseInt(message.trim());
        InetSocketAddress udpAddress = new InetSocketAddress(socket.getInetAddress(), clientUdpPort);
        udpClients.add(udpAddress);
        System.out.println("Registered UDP client: " + udpAddress);

        while ((message = in.readLine()) != null) {
          broadcastMessage(socket.getRemoteSocketAddress() + ": " + message, out);
        }

      } catch (IOException e) {
        System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
      } catch (IllegalArgumentException e) {
        System.err.println("Invalid UDP, disconnecting " + socket.getRemoteSocketAddress());
      } finally {
        if (out != null) {
          clientWriters.remove(out);
        }
        try {
          socket.close();
        } catch (IOException ignored) {
        }
        System.out.println("Removed client: " + socket.getRemoteSocketAddress());
      }
    }
  }
}