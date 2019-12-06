import java.io.*;
import java.net.*;
import java.io.File;
import java.nio.file.Files;

public class Sender extends Thread{
    public static void main(String args[]) throws Exception
    {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        //sendData = sentence.getBytes();
        //sendData = readFile(args[2]);
        byte[] rawData = readFile(args[2]);
        System.out.println("file size: " + rawData.length + " bytes");
        byte[][] allData = biteOff(rawData, 1450);
        System.out.println("frames required: " + allData.length);
        for(int i = 0; i < allData.length; i++){
            DatagramPacket sendPacket = new DatagramPacket(allData[i], allData[i].length, IPAddress, Integer.parseInt(args[1]));
            clientSocket.send(sendPacket);
            sleep((long)2);
            //System.out.print(i);
        }
        
    //    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    //    clientSocket.receive(receivePacket);
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
        byte[][] answer = new byte[divisions][];

        for(int i = 0; i < divisions; i++){
            int startIndex = i * biteSize;
            int endIndex = (i + 1) * biteSize;
            if(endIndex > input.length){
                endIndex = input.length - 1;
            }
            int arraySize = endIndex - startIndex;

            answer[i] = new byte[arraySize];

            for(int j = 0; j < arraySize; j++){
                answer[i][j] = input[i * biteSize + j];
            }
        }

        return answer;
    }
}