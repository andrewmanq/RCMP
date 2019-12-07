import java.io.*;
import java.net.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.nio.ByteBuffer;

// _______  _______  __    _  ______   _______  ______              ___  _______  __   __  _______ 
// |       ||       ||  |  | ||      | |       ||    _ |            |   ||   _   ||  | |  ||   _   |
// |  _____||    ___||   |_| ||  _    ||    ___||   | ||            |   ||  |_|  ||  |_|  ||  |_|  |
// | |_____ |   |___ |       || | |   ||   |___ |   |_||_           |   ||       ||       ||       |
// |_____  ||    ___||  _    || |_|   ||    ___||    __  | ___   ___|   ||       ||       ||       |
//  _____| ||   |___ | | |   ||       ||   |___ |   |  | ||   | |       ||   _   | |     | |   _   |
// |_______||_______||_|  |__||______| |_______||___|  |_||___| |_______||__| |__|  |___|  |__| |__|
/**
 * @author  Andrew Quist, Sambridhi Acharya
 * created on Dec 7, 2019
 * 
 * Sender.java is a program that utilizes UDP to make a new internet protocol -- RCMP
 * RCMP is a protocol that is both message-based and reliable.
 * 
 * Sender sends packets and receives ACK messages to acknowledge that the packets have sent.
 * A Receiver object must be opened at the corresponding IP and port (given in arguments)
 * 
 * HOW TO USE:
 * argument 0 - host address (can be host name OR an IP address)
 * argument 1 - port number of the receiver program
 * argument 2 - the name of the file that will be sent
 */
public class Sender extends Thread{

    //Colors for nice formatting on the console output
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";

    //The socket that we will send data over
    private static DatagramSocket clientSocket;

    //id is the unique id of this sender. It is used so that the receiver knows which sender it is
    //receiving this file from.
    private static byte[] id;
    //msgSz (message size for short) is the total amount of packets to be sent over the network.
    private static byte[] msgSz;

    //This constant is the data portion of our RCMP packet.
    private static final int DATASIZE = 1450;

    private static final int TIMEOUT = 50;
    public static void main(String args[]) throws Exception
    {   
        //This generates a unique id for this sender (4 bytes)
        id = getRandomID();

        //Initialize the address we will send to with the first argument
        InetAddress IPAddress;
        String ip = args[0];
        IPAddress = InetAddress.getByName(ip);

        //Open the socket we will send data over
        try{
            clientSocket = new DatagramSocket();
        }catch(Exception e){
            System.out.println("There was a problem opening an internet connection. quitting...");
            return;
        }
        
        //allData is the file contents loaded into the RAM.
        //the data is loaded into chunks that are ready to be sent in a datagram.
        byte[][] allData;

        //This will read the file that we will open
        try{
            //Read in all the data and process it
            byte[] rawData = readFile(args[2]);
            //tell the user how big the file is
            System.out.println("file size: " + rawData.length + " bytes");
            //Process the data
            allData = biteOff(rawData, DATASIZE);
        }catch(Exception e){
            System.out.println("There was a problem reading and processing data. quitting...");
            return;
        }

        //gapCount is increased when ACKs are successfully delivered.
        int gapCount = 0;
        //This records when we will send another ACK
        int nextExpectedAck = 0;
        //This is the packet that was last designated to send an ACK
        int lastReceivedAck = 0;
        //This is incremented when an ACK is not received at the last packet.
        int lastAckIncomplete = 0;

        System.out.println("frames required: " + allData.length);

        //Iterates through all the data to send over the network.
        //i is always the current datagram getting sent
        for(int i = 0; i < allData.length; i++){

            //Checks if this datagram needs and ack request
            if(nextExpectedAck == i){
                insertAckRequest(allData[i]);
            }

            //Creates a new packet to be sent
            DatagramPacket sendPacket = new DatagramPacket(allData[i],
                allData[i].length, IPAddress, Integer.parseInt(args[1]));
            //sends datagram
            clientSocket.send(sendPacket);
            System.out.print(i + ".");
            //System.out.print(i);

            //Runs when the sender requires an ACK
            if(nextExpectedAck == i){

                //ackNum is what we will use to record the ack number received.
                int ackNum;

                //Timeout for the ack request
                clientSocket.setSoTimeout(TIMEOUT);

                //Will contain the data from the ACK
                byte[] receiveData = new byte[8];

                //correctACK is used to detect if the ACK is the ACK we are ACKtually looking for
                Boolean correctACK = false;

                //Loops if the ACK is not the one we're looking for
                while(!correctACK){
                    //The incoming packet information
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    //gets an ACK, sees if it's the number of this packet
                    try{
                        clientSocket.receive(receivePacket);
                        ackNum = parseACKnumber(receiveData);

                        //Exits ACK loop
                        if(parseACKvalid(receiveData) == true && ackNum == i){
                            correctACK = true;
                        }

                    //If the ACK request times out
                    }catch (SocketTimeoutException e){
                        System.out.print("NO ACK RECIEVED!");

                        //Failsafe for if the ack was dropped for the end of the file and
                        //the receiver has closed
                        if(i == allData.length - 1){
                            lastAckIncomplete ++;

                            //If the ack was ignored 5 times, just give up
                            if(lastAckIncomplete >= 5){
                                System.out.println("File transfer success unknown.");
                                clientSocket.close();
                                return;
                            }
                        }

                        //Go back to the old ACK checkpoint because there was a packet dropped
                        i = lastReceivedAck - 1;

                        //Resets the gap count (unreliable network)
                        gapCount = 0;

                        break;
                    }

                    if(correctACK){
                        //ACK has been received correctly, record this ACK checkpoint
                        System.out.println(ANSI_GREEN + "ack " + ackNum + " recieved" + ANSI_RESET);
                        lastReceivedAck = i;
                    }
                }

                //Gap count increases because the ACK was successfully received
                gapCount ++;
                nextExpectedAck = lastReceivedAck + gapCount;
                //This sets the last datagram as an ACK
                if(nextExpectedAck >= allData.length){
                    nextExpectedAck = allData.length - 1;
                }
            }
        }

        clientSocket.close();
    }

    //Makes sure the ACK info is matching my ID
    //pass in byte[] information (the ACK packet bytes)
    static Boolean parseACKvalid(byte[] information){
        Boolean answer = true;
        if(information[0] != id[0]){ answer = false; }
        if(information[1] != id[1]){ answer = false; }
        if(information[2] != id[2]){ answer = false; }
        if(information[3] != id[3]){ answer = false; }
        return answer;
    }
    
    //Returns the ACK packet number from an ACK byte[] array
    static int parseACKnumber(byte[] information){
        int answer = 0;

        byte[] numData = new byte[4];
        numData[0] = information[4]; numData[1] = information[5];
        numData[2] = information[6]; numData[3] = information[7];
        answer = Integer.parseInt(new String(numData));

        return answer;
    }

    //reads all of the data in a file
    static byte[] readFile(String name) throws Exception{

        File file = new File(name);
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return fileContent;
    }

    //Takes a ton of data and cuts it into bytes that are biteSize length, and inserts the packet's header
    static byte[][] biteOff(byte[] input, int biteSize){
        int originalLength = input.length;
        //How many packets needed to send the data
        int divisions = (int)(Math.ceil((double)originalLength / (double)biteSize));

        //msgSz will be put in the header to say how many packets to expect
        msgSz = get4ByteInt(divisions);

        //HEADER FORMAT: |ID - 4 bytes | Number of packets - 4 bytes | Packet Number - 4 bytes | ACK request - 1 byte |
        // (payload is after header, is 1450)

        byte[][] answer = new byte[divisions][];

        //Goes through each byte and copies it to a packet
        for(int i = 0; i < divisions; i++){
            int startIndex = i * biteSize;
            int endIndex = (i + 1) * biteSize;
            //The last packet has less data
            if(endIndex > input.length){
                endIndex = input.length;
            }

            //Calculates how large the packet is
            int arraySize = endIndex - startIndex;
            answer[i] = new byte[arraySize + 13];

            //Inserts ID
            for(int j = 0; j < 4; j++){
                answer[i][j] = id[j];
            }

            //Inserts total packet number
            for(int j = 0; j < 4; j++){
                answer[i][j + 4] = msgSz[j];
            }

            //Inserts current packet number
            byte[] packetNum = get4ByteInt(i);

            for(int j = 0; j < 4; j++){
                answer[i][j + 8] = packetNum[j];
            }

            //Default ACK is 0, ACK requests are inserted while sending
            answer[i][12] = 0;

            //Fills the payload
            for(int j = 0; j < arraySize; j++){
                answer[i][j + 13] = input[i * biteSize + j];
            }
        }

        return answer;
    }

    //This goes in and inserts a '1' byte into the ack field of byte[] data
    private static byte[] insertAckRequest(byte[] data){
        data[12] = 1;
        return data;
    }

    //Returns a random 4-byte id number
    private static byte[] getRandomID(){
        String idInfo = "";
        Random r = new Random();
        while(idInfo.getBytes().length < 4){
            idInfo += r.nextInt(9);
        }
        return idInfo.getBytes();
    }

    //Converts an integer into a 4-byte minimum byte array
    private static byte[] get4ByteInt(int num){
        String info = Integer.toString(num);
        while(info.getBytes().length < 4){
            info = "0" + info;
        }
        return info.getBytes();
    }
}