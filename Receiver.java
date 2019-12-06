import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Receiver extends Thread{

    private static DatagramSocket serverSocket;
    public static void main(String args[]) throws Exception
    {
        Map<Integer, byte[]> allDataRecieved = new HashMap<Integer, byte[]>();
        int iterator = 0;

        String filePath = "newFile.txt";
        File newFile = new File(filePath);
        try{
            filePath = args[1];
        }catch(Exception e){
            System.out.println("default filename used (newFile.txt)");
        }

        FileOutputStream fos;
        try{
            fos = new FileOutputStream(filePath, true);
        }catch(Exception e){
            System.out.println("there was a problem writing out the file.");
            return;
        }
        
        try{
            serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
        }catch(Exception e){
            System.out.print("Port configuration failed. Did you use a proper port number?");
            return;
        }
        while(true)
        {
            byte[] receiveData = new byte[1450];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            if(receivePacket.getLength() < receiveData.length){
                byte[] resizedData = new byte[receivePacket.getLength() - 1];
                System.arraycopy(receiveData, 0, resizedData, 0, receivePacket.getLength() - 1);
                receiveData = resizedData;
            }
            //String sentence = new String( receivePacket.getData());
            //System.out.println("RECEIVED: " + sentence);
            
            allDataRecieved.put(iterator, receiveData);

            try{
                fos.write(receiveData);
            }catch(Exception e){
                System.out.println("there was a problem writing out the file.");
                return;
            }
            //writeFile(filePath, receiveData);

            // return;
            //InetAddress IPAddress = receivePacket.getAddress();
            //int port = receivePacket.getPort();
            // String capitalizedSentence = sentence.toUpperCase();
            // sendData = capitalizedSentence.getBytes();
            // DatagramPacket sendPacket =
            // new DatagramPacket(sendData, sendData.length, IPAddress, port);
            // serverSocket.send(sendPacket);
            iterator ++;
            System.out.println("packet " + iterator + " recieved");
        }
    }

    private static void writeFile(String pathname, byte[] info){
        try{
            FileOutputStream fos = new FileOutputStream(pathname, true);
            fos.write(info);
         }catch(Exception e){
            System.out.println("File output failed.");
         }
    }
}