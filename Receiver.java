import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


//  ______    _______  _______  _______  ___   __   __  _______  ______              ___  _______  __   __  _______ 
// |    _ |  |       ||       ||       ||   | |  | |  ||       ||    _ |            |   ||   _   ||  | |  ||   _   |
// |   | ||  |    ___||       ||    ___||   | |  |_|  ||    ___||   | ||            |   ||  |_|  ||  |_|  ||  |_|  |
// |   |_||_ |   |___ |       ||   |___ |   | |       ||   |___ |   |_||_           |   ||       ||       ||       |
// |    __  ||    ___||      _||    ___||   | |       ||    ___||    __  | ___   ___|   ||       ||       ||       |
// |   |  | ||   |___ |     |_ |   |___ |   |  |     | |   |___ |   |  | ||   | |       ||   _   | |     | |   _   |
// |___|  |_||_______||_______||_______||___|   |___|  |_______||___|  |_||___| |_______||__| |__|  |___|  |__| |__|
/**
 * @author  Andrew Quist, Sambridhi Acharya
 * created on Dec 7, 2019
 * 
 * Receiver.java is a program that utilizes UDP to make a new internet protocol -- RCMP
 * RCMP is a protocol that is both message-based and reliable.
 * 
 * Receiver.java receives packets and sends ACK messages for the Sender to continue sending.
 * 
 * HOW TO USE:
 * argument 0 - port number that the file will be received on
 * argument 1 - the name of the file that will be written out to the disk (in the current directory)
 */
public class Receiver extends Thread{

    //Records the ID of the sender
    private static byte[] id;
    //Records the total packet number we will receive
    private static int fullSize = 0;

    //Socket that we get data from
    private static DatagramSocket serverSocket;
    //File output for writing the received file
    private static FileOutputStream fos;

    //This constant is the data portion of our RCMP packet.
    private static final int DATASIZE = 1450;
    private static final int HEADERSIZE = 13;
    public static void main(String args[]) throws Exception
    {
        //PARSE ARGUMENTS----------------------------------------
        //filePath defualt
        String filePath = "newFile.txt";
        File newFile = new File(filePath);
        try{
            filePath = args[1];
        }catch(Exception e){
            System.out.println("default filename used (newFile.txt)");
        }

        //FILE OPEN----------------------------------------
        //Try to make the new file with file path
        try{
            fos = new FileOutputStream(filePath, true);
        }catch(Exception e){
            System.out.println("there was a problem writing out the file.");
            return;
        }

        //OPEN SOCKET----------------------------------------
        try{
            //Open a socket with the first argument (the port number)
            serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
        }catch(Exception e){
            System.out.print("Port configuration failed. Did you use a proper port number?");
            fos.close();
            return;
        }

        System.out.println("Recieving file...");

        //INITIALIZE LOOP-DEPENDANT VARIABLES----------------------------------------
        id = new byte[4];
        fullSize = 0;

        //Records the last received packet
        int previousPacket = -1;
        //Counts how many messages it has received
        int iterator = 0;

        while(true)
        {
            //Get data from socket
            byte[] receiveData = new byte[DATASIZE + HEADERSIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            //valid is a simple way of determining whether we want to drop or process the packet
            boolean valid = true;

            //IF FIRST PACKET----------------------------------------
            if(iterator == 0){
                //Parses the ID number from the first incoming packet
                assignID(receiveData);
                System.out.println("ID: " + id.toString());

                //Parses the total packet number from our first packet
                assignSize(receiveData);
                System.out.println("Amount of packets required: " + fullSize);

            //ALL OTHER PACKETS----------------------------------------
            }else{
                //Is this the right ID? if not, drop
                valid = isValidID(receiveData);
            }

            //NUMDATA = number of all incoming packets------------------------
            int packetNum = extractPacketNumber(receiveData);

            //Is this a future packet? DROP!
            if(packetNum > previousPacket + 1){
                valid = false;
                System.out.println("packet lost");
            }

            //IF ID IS FROM THE RIGHT SENDER----------------------------------------
            if(valid){

                //Looks for an ACK request
                if(receiveData[12] == 1){
                    //FIND SENDER----------------------------------------
                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();

                    //CREATE and SEND ACK-------------------------------------------
                    sendAck(packetNum, IPAddress, port, serverSocket);
                }

                //ADJUSTS SIZE----------------------------------------
                //This looks to see if the packet is smaller than the max packet size
                if(receivePacket.getLength() < receiveData.length){
                    byte[] resizedData = new byte[receivePacket.getLength()];
                    //Copy all data to the new smaller array
                    System.arraycopy(receiveData, 0, resizedData, 0, receivePacket.getLength());
                    receiveData = resizedData;
                }

                //WRITE FILE----------------------------------------

                //On the offchance that this is the next packet in the sequence...
                if(packetNum == previousPacket + 1){
                    //Write that data
                    if(writeToFile(receiveData)){
                        System.out.print("| " + packetNum + " |");
                    }
                    //Records the last successful packet sent in order
                    previousPacket = packetNum;

                }else{
                    System.out.println("old packet recieved");
                }
                iterator ++;

                //Is this the last packet? cool
                if(packetNum == fullSize - 1){
                    //Cleanup time
                    fos.close();
                    System.out.println("File recieved.");
                    //Close the program
                    return;
                }
            }
        }
    }

    //This takes byte data and writes it to the currently open file (and strips off the header)
    //Returns true if write is successful
    private static Boolean writeToFile(byte[] receiveData) throws Exception{
        try{
            //Extract payload
            byte[] strippedData = new byte[receiveData.length - HEADERSIZE];
            System.arraycopy(receiveData, HEADERSIZE, strippedData, 0, receiveData.length - HEADERSIZE);
            //Write bytes
            fos.write(strippedData);
        }catch(Exception e){
            System.out.println("there was a problem writing out the file.");
            fos.close();
            return false;
        }
        return true;
    }

    //Takes the packet number, address, port, and socket, then sends an ack with packetNum response
    private static void sendAck(int packetNum, InetAddress IPAddress, int port, DatagramSocket serverSocket) throws Exception{
        byte[] sendData = createAck(packetNum);
        DatagramPacket sendPacket =
            new DatagramPacket(sendData, sendData.length, IPAddress, port);
        serverSocket.send(sendPacket);
    }

    //Parses the packet number from the datagram header
    private static int extractPacketNumber(byte[] receiveData){
        byte[] numData = new byte[4];
        numData[0] = receiveData[8]; numData[1] = receiveData[9];
        numData[2] = receiveData[10]; numData[3] = receiveData[11];
        int packetNum = Integer.parseInt(new String(numData));
        return packetNum;
    }

    //Makes sure the packet ID is correct
    private static Boolean isValidID(byte[] receiveData){
        Boolean valid = true;
        if(receiveData[0] != id[0]){ valid = false; }
        if(receiveData[1] != id[1]){ valid = false; }
        if(receiveData[2] != id[2]){ valid = false; }
        if(receiveData[3] != id[3]){ valid = false; }
        return valid;
    }

    //Extracts the number of packets we will recieve, puts it in private variable fullSize
    private static void assignSize(byte[] receiveData){
        byte[] sizeIndicator = new byte[4];
        sizeIndicator[0] = receiveData[4]; sizeIndicator[1] = receiveData[5];
        sizeIndicator[2] = receiveData[6]; sizeIndicator[3] = receiveData[7];
        fullSize = Integer.parseInt(new String(sizeIndicator));
    }

    //Fills ID private variable with the packet ID header field
    private static void assignID(byte[] receiveData){
        id[0] = receiveData[0]; id[1] = receiveData[1];
        id[2] = receiveData[2]; id[3] = receiveData[3];
    }

    //Quickly cooks up an ACK request in a byte array to be sent back to the sender
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

    // private static void writeFile(String pathname, byte[] info){
    //     try{
    //         FileOutputStream fos = new FileOutputStream(pathname, true);
    //         fos.write(info);
    //      }catch(Exception e){
    //         System.out.println("File output failed.");
    //      }
    // }

    //A nice function to convert an int to a 4-byte array
    private static byte[] get4ByteInt(int num){
        String info = Integer.toString(num);
        while(info.getBytes().length < 4){
            info = "0" + info;
        }
        return info.getBytes();
    }
}