
/**
 * FastFtp Class
 *
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;

import cpsc441.a3.shared.*;

public class FastFtp {
	
	
	
	int windowSize;
	int rtoTimer;
	int serverUDPPortNumber;
	int nextSequenceNumber = 0;
	int base = 0;
	
	
	DatagramSocket udpSocket;
	DataInputStream inputStream;
	DataOutputStream outputStream;
	
	TxQueue queue;

	/**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		// to be completed
		this.windowSize = windowSize;
		this.rtoTimer = rtoTimer;
	}
	

    /**
     * Sends the specified file to the specified destination host:
     * 1. send file/connection infor over TCP
     * 2. start receving thread to process coming ACKs
     * 3. send file segment by segment
     * 4. wait until transmit queue is empty, i.e., all segments are ACKed
     * 5. clean up (cancel timer, interrupt receving thread, close sockets/files)
     * 
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName		Name of the file to be trasferred to the rmeote server
     */
	public void send(String serverName, int serverPort, String fileName) {
		// to be completed
		
		
		try {
			Socket socket = new Socket(serverName, serverPort);
			outputStream = new DataOutputStream(socket.getOutputStream());
			inputStream = new DataInputStream(socket.getInputStream());
			
			udpSocket = new DatagramSocket(5555);
			
			File file = new File("fileName");
			Long fileSize = file.length();
			
			outputStream.writeUTF(fileName);
			outputStream.writeLong(fileSize);
			outputStream.writeInt(udpSocket.getLocalPort());
			outputStream.flush();
			
			serverUDPPortNumber = inputStream.readInt();
			
			udpSocket.connect(InetAddress.getByName(null), serverUDPPortNumber);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		ReceiverThread receiver = new ReceiverThread(this, udpSocket);
		receiver.start();
		
		queue = new TxQueue(10);
		
		
		FileInputStream fis = null;
		long contentLength = 0;
		try {
			
			File file = new File(fileName);
			
			contentLength = file.length();
			
			fis = new FileInputStream(new File(fileName));
			
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		int counter = 0;
		int numBytesRead = 0;
		
		while(numBytesRead != -1){
			
			if(counter == contentLength){
				break;
			}
			
			byte[] payLoad = new byte[1000];
			

			try {
				numBytesRead = fis.read(payLoad);
			} catch (IOException e) {
				e.printStackTrace();
			}


			Segment send = new Segment(nextSequenceNumber, payLoad);
			
			while(queue.isFull()){
				//setup wait
			}

			processSend(send);
			
			counter += numBytesRead;
			
		}
		
		
		
	}
	
	public synchronized void processSend(Segment seg){
		
		try {
			DatagramPacket packet = new DatagramPacket(seg.getBytes(), seg.getBytes().length);
			udpSocket.send(packet);
			queue.add(seg);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(seg.getSeqNum() == 0){
			Timer timer = new Timer(true);
			timer.schedule(new TimeOutHandler(), 1000);
		}
		
	}
	
	public synchronized void processACK(Segment ack){
		System.out.println("Received seq +" + ack.getSeqNum());
		
		
		
		
	}
	
	
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// all srguments should be provided
		// as described in the assignment description 
		if (args.length != 5) {
			System.out.println("incorrect usage, try again.");
			System.out.println("usage: FastFtp server port file window timeout");
			System.exit(1);
		}
		
		// parse the command line arguments
		// assume no errors
		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		int windowSize = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);

		// send the file to server
		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}
}
