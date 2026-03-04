import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Server3 {
        public static void main(String args[])
    {
        System.out.println("JAVA SERVER3");
        DatagramSocket socket = null;
        int portNumber = 9009;

        try{
            socket = new DatagramSocket(portNumber);
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                ByteBuffer receiveBufferWrapper = ByteBuffer.wrap(receivePacket.getData()).order(ByteOrder.LITTLE_ENDIAN);
                int msg = receiveBufferWrapper.getInt();
                System.out.println("received msg: " + msg);

                int confirmation = msg + 1;
                ByteBuffer sendBufferWrapper = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                sendBufferWrapper.putInt(confirmation);
                byte[] sendBuffer = sendBufferWrapper.array();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, receivePacket.getAddress(), receivePacket.getPort());
                socket.send(sendPacket);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
