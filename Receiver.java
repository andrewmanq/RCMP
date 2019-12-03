import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.File;
import java.nio.file.Files;


public class Receiver extends Thread{
    private DatagramSocket sock;
    private boolean listening;
    private byte[] buffer = new byte[1450];
 
    private static int portNum;
    private static String fileName;

    public Receiver(int port, String name) {
        
        portNum = port;
        fileName = name;
        try{
            sock = new DatagramSocket(portNum);
            run();
        }catch(Exception e){
            System.out.println(e.toString());
        }
    }

    public static void main(String[] args){
        portNum = Integer.parseInt(args[0]);
        fileName = args[1];

        Receiver r = new Receiver(portNum, fileName);
    }
 
    public void run() {
        listening = true;
 
        while (listening) {
            try{
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                sock.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("got something");

            }catch(Exception e){
                System.out.println(e.toString());
            }

        }
        sock.close();
    }
}