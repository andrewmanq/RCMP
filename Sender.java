 
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.File;
import java.nio.file.Files;

public class Sender extends Thread{
    private DatagramSocket sock;
    private byte[] buffer = new byte[1450];

    private static String ipAddress;
    private static int portNum;
    private static String fileName;

    public Sender(String ip, int port, String file) throws Exception{
        sock = new DatagramSocket(port);
        System.out.println(sock.getLocalPort());

        try{
            InetAddress address = InetAddress.getByName("localhost");
            byte[] info = readFile(file);
            buffer = info;
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            sock.send(packet);
            sock.close();
        }catch(Exception e){
            System.out.println("Something went wrong!");
            System.out.println(e.toString());
        }
    }

    byte[] readFile(String name) throws Exception{

        File file = new File(name);
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return fileContent;
    }

    public static void main(String[] args){
        ipAddress = args[0];
        portNum = Integer.parseInt(args[1]);
        fileName = args[2];

        try{
            Sender s = new Sender(ipAddress, portNum, fileName);
            s.run();
        }catch (Exception e){
            //do nothing
        }
    }
}