import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Receiver extends Thread{

    private static DatagramSocket serverSocket;
    public static void main(String args[]) throws Exception
    {
        byte[] id = new byte[4];
        int fullSize;

        Map<Integer, byte[]> allDataRecieved = new HashMap<Integer, byte[]>();

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

        System.out.println("Recieving file...");

        int iterator = 0;
        while(true)
        {
            
            byte[] receiveData = new byte[1450 + 12];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            boolean valid = true;
            if(iterator == 0){
                id[0] = receiveData[0]; id[1] = receiveData[1];
                id[2] = receiveData[2]; id[3] = receiveData[3];

                byte[] sizeIndicator = new byte[4];
                sizeIndicator[0] = receiveData[4]; sizeIndicator[1] = receiveData[5];
                sizeIndicator[2] = receiveData[6]; sizeIndicator[3] = receiveData[7];
                fullSize = Integer.parseInt(new String(sizeIndicator));

                System.out.println("Amount of packets required: " + fullSize);

                System.out.println("ID: " + id.toString());
            }else{
                if(receiveData[0] != id[0]){ valid = false; }
                if(receiveData[1] != id[1]){ valid = false; }
                if(receiveData[2] != id[2]){ valid = false; }
                if(receiveData[3] != id[3]){ valid = false; }
            }

            byte[] numData = new byte[4];
            numData[0] = receiveData[8]; numData[1] = receiveData[9];
            numData[2] = receiveData[10]; numData[3] = receiveData[11];
            int packetNum = Integer.parseInt(new String(numData));

            if(valid){
                InetAddress IPAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();
                // String capitalizedSentence = sentence.toUpperCase();
                byte[] sendData = "ACK".getBytes();
                DatagramPacket sendPacket =
                new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);

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
                    if(receiveData.length < 1450 + 12){
                        fos.close();
                        System.out.println("File recieved.");
                        return;
                    }
                }catch(Exception e){
                    System.out.println("there was a problem writing out the file.");
                    return;
                }

                iterator ++;
                System.out.println("packet " + packetNum + " recieved");
            }
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