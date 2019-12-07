import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Receiver extends Thread{

    private static byte[] id;
    private static int fullSize = 0;

    private static DatagramSocket serverSocket;
    public static void main(String args[]) throws Exception
    {

        Map<Integer, byte[]> allDataRecieved = new HashMap<Integer, byte[]>();

        //PARSE ARGUMENTS----------------------------------------
        String filePath = "newFile.txt";
        File newFile = new File(filePath);
        try{
            filePath = args[1];
        }catch(Exception e){
            System.out.println("default filename used (newFile.txt)");
        }

        //FILE OPEN----------------------------------------
        FileOutputStream fos;
        try{
            fos = new FileOutputStream(filePath, true);
        }catch(Exception e){
            System.out.println("there was a problem writing out the file.");
            return;
        }

        //OPEN SOCKET----------------------------------------
        try{
            serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
        }catch(Exception e){
            System.out.print("Port configuration failed. Did you use a proper port number?");
            return;
        }

        System.out.println("Recieving file...");

        //INITIALIZE NON-LOOP VARIABLES----------------------------------------
        id = new byte[4];
        fullSize = 0;

        int iterator = 0;
        while(true)
        {

            byte[] receiveData = new byte[1450 + 13];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            boolean valid = true;

            //IF FIRST PACKET----------------------------------------
            if(iterator == 0){
                id[0] = receiveData[0]; id[1] = receiveData[1];
                id[2] = receiveData[2]; id[3] = receiveData[3];

                byte[] sizeIndicator = new byte[4];
                sizeIndicator[0] = receiveData[4]; sizeIndicator[1] = receiveData[5];
                sizeIndicator[2] = receiveData[6]; sizeIndicator[3] = receiveData[7];
                fullSize = Integer.parseInt(new String(sizeIndicator));

                System.out.println("Amount of packets required: " + fullSize);

                System.out.println("ID: " + id.toString());
            //ALL OTHER PACKETS----------------------------------------
            }else{
                if(receiveData[0] != id[0]){ valid = false; }
                if(receiveData[1] != id[1]){ valid = false; }
                if(receiveData[2] != id[2]){ valid = false; }
                if(receiveData[3] != id[3]){ valid = false; }
            }

            //NUMDATA = number of all incoming packets------------------------
            byte[] numData = new byte[4];
            numData[0] = receiveData[8]; numData[1] = receiveData[9];
            numData[2] = receiveData[10]; numData[3] = receiveData[11];
            int packetNum = Integer.parseInt(new String(numData));

            //IF ID IS FROM THE RIGHT SENDER----------------------------------------
            if(valid){

                if(receiveData[12] == 1){
                    //FIND SENDER----------------------------------------
                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();

                    //CREATE ACK-------------------------------------------
                    byte[] sendData = createAck(packetNum);
                    DatagramPacket sendPacket =
                    new DatagramPacket(sendData, sendData.length, IPAddress, port);
                    serverSocket.send(sendPacket);
                }

                //ADJUSTS SIZE----------------------------------------
                if(receivePacket.getLength() < receiveData.length){
                    byte[] resizedData = new byte[receivePacket.getLength() - 1];
                    System.arraycopy(receiveData, 0, resizedData, 0, receivePacket.getLength() - 1);
                    receiveData = resizedData;
                }
                
                allDataRecieved.put(iterator, receiveData);

                //WRITE FILE----------------------------------------
                try{
                    byte[] strippedData = new byte[receiveData.length - 13];
                    System.arraycopy(receiveData, 13, strippedData, 0, receiveData.length - 13);
                    fos.write(strippedData);
                }catch(Exception e){
                    System.out.println("there was a problem writing out the file.");
                    return;
                }

                iterator ++;
                System.out.print("| packet " + packetNum + " recieved |");

                if(packetNum == fullSize - 1){
                    fos.close();
                    System.out.println("File recieved.");
                    return;
                }
            }
        }
    }

    private static byte[] createAck(int packetNum){
        byte[] ack = new byte[8];

        ack[0] = id[0];
        ack[1] = id[1];
        ack[2] = id[2];
        ack[3] = id[3];

        byte[] numData = get4ByteInt(packetNum);

        ack[4] = numData[0];
        ack[5] = numData[1];
        ack[6] = numData[2];
        ack[7] = numData[3];

        return ack;
    }

    private static void writeFile(String pathname, byte[] info){
        try{
            FileOutputStream fos = new FileOutputStream(pathname, true);
            fos.write(info);
         }catch(Exception e){
            System.out.println("File output failed.");
         }
    }

    private static byte[] get4ByteInt(int num){
        String info = Integer.toString(num);
        while(info.getBytes().length < 4){
            info = "0" + info;
        }
        return info.getBytes();
    }
}