import java.io.*;
import java.net.*;

public class Receiver extends Thread{
    public static void main(String args[]) throws Exception
    {
        DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
        byte[] receiveData = new byte[1450];
        //byte[] sendData = new byte[500];
        while(true)
        {
            //receiveData = new byte[1450];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            String sentence = new String( receivePacket.getData());
            System.out.println("RECEIVED: " + sentence);
            // return;
            //InetAddress IPAddress = receivePacket.getAddress();
            //int port = receivePacket.getPort();
            // String capitalizedSentence = sentence.toUpperCase();
            // sendData = capitalizedSentence.getBytes();
            // DatagramPacket sendPacket =
            // new DatagramPacket(sendData, sendData.length, IPAddress, port);
            // serverSocket.send(sendPacket);
        }
    }
}