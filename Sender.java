import java.io.*;
import java.net.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.nio.ByteBuffer;

public class Sender extends Thread{

    private static byte[] id;
    private static byte[] msgSz;
    public static void main(String args[]) throws Exception
    {   
        id = getRandomID();
        
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        byte[] rawData = readFile(args[2]);
        System.out.println("file size: " + rawData.length + " bytes");

        byte[][] allData = biteOff(rawData, 1450);

        System.out.println("frames required: " + allData.length);
        for(int i = 0; i < allData.length; i++){
            DatagramPacket sendPacket = new DatagramPacket(allData[i], allData[i].length, IPAddress, Integer.parseInt(args[1]));
            clientSocket.send(sendPacket);
            //System.out.print(i);

            byte[] receiveData = new byte["ACK".getBytes().length];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            System.out.print("_ACK_");
        }
        
        

    //    String modifiedSentence = new String(receivePacket.getData());
    //    System.out.println("FROM SERVER:" + modifiedSentence);
        clientSocket.close();
    }

    static byte[] readFile(String name) throws Exception{

        File file = new File(name);
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return fileContent;
    }

    static byte[][] biteOff(byte[] input, int biteSize){
        int originalLength = input.length;
        int divisions = (int)(Math.ceil((double)originalLength / (double)biteSize));

        msgSz = get4ByteInt(divisions);

        byte[][] answer = new byte[divisions][];

        for(int i = 0; i < divisions; i++){
            int startIndex = i * biteSize;
            int endIndex = (i + 1) * biteSize;
            if(endIndex > input.length){
                endIndex = input.length - 1;
            }
            int arraySize = endIndex - startIndex;

            answer[i] = new byte[arraySize + 12];

            for(int j = 0; j < 4; j++){
                answer[i][j] = id[j];
            }

            for(int j = 0; j < 4; j++){
                answer[i][j + 4] = msgSz[j];
            }

            byte[] packetNum = get4ByteInt(i);
            
            for(int j = 0; j < 4; j++){
                answer[i][j + 8] = packetNum[j];
            }

            for(int j = 0; j < arraySize; j++){
                answer[i][j + 12] = input[i * biteSize + j];
            }
        }

        return answer;
    }

    //Returns a random 4-bit id number
    private static byte[] getRandomID(){
        String idInfo = "";
        Random r = new Random();
        while(idInfo.getBytes().length < 4){
            idInfo += r.nextInt(9);
        }
        return idInfo.getBytes();
    }

    private static byte[] get4ByteInt(int num){
        String info = Integer.toString(num);
        while(info.getBytes().length < 4){
            info = "0" + info;
        }
        return info.getBytes();
    }
}