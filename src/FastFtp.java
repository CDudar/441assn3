
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
	Socket tcpSocket;
	DataInputStream inputStream;
	DataOutputStream outputStream;
	
	File file;
	long fileSize;
	
	Timer timer;
	TimeOutHandler timeoutHandler;
	
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
		
		//System.out.println("SENDINGINGGINGONGGO");
		
		try {
			tcpSocket = new Socket(serverName, serverPort);
			outputStream = new DataOutputStream(tcpSocket.getOutputStream());
			inputStream = new DataInputStream(tcpSocket.getInputStream());
			
			udpSocket = new DatagramSocket(5555);
			
			file = new File(fileName);
			fileSize = file.length();
			
			outputStream.writeUTF(fileName);
			outputStream.writeLong(fileSize);
			outputStream.writeInt(udpSocket.getLocalPort());
			outputStream.flush();
			
			serverUDPPortNumber = inputStream.readInt();
			
			
			
			inputStream.close();
			outputStream.close();
			
			System.out.println("Received port number " + serverUDPPortNumber );
			udpSocket.connect(tcpSocket.getInetAddress(), serverUDPPortNumber);
			
			System.out.println("udpSocket local address " +udpSocket.getLocalAddress());
			System.out.println("udpSocket listening on " + udpSocket.getLocalPort());
		
			System.out.println("udpSocket connected to " + udpSocket.getPort());

			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		ReceiverThread receiver = new ReceiverThread(this, udpSocket);
		receiver.start();
		
		queue = new TxQueue(windowSize);
		
		
		FileInputStream fis = null;
		try {
			
			System.out.println("Sending file " + file.getName());
			System.out.println("File content length: " + fileSize + " bytes");
			fis = new FileInputStream(file);
			
			
			
			
		} 
		
			catch (IOException e) {
			e.printStackTrace();
		}
		
		
		int counter = 0;
		int numBytesRead = 0;
		
		System.out.println("About to send file");
		
		while(numBytesRead != -1){
			

			
			if(counter == fileSize){
				break;
			}
	
			byte[] payLoad = new byte[1000];
			if(fileSize - counter < 1000) {
				payLoad = new byte[(int)fileSize - counter];
			}

			
			try {
				numBytesRead = fis.read(payLoad);
				
			} catch (IOException e) {
				e.printStackTrace();
			}



			
			Segment send = new Segment(nextSequenceNumber, payLoad);
			System.out.println("Sending packet " + nextSequenceNumber + " of size " + send.getLength());
			
			nextSequenceNumber++;
			
			while(queue.isFull()){
				Thread.yield();
			}

			processSend(send, false);
			
			counter += numBytesRead;
			
			System.out.println("Counter is currently at: " + counter);
			
			System.out.println("numBytesRead is currently at:" + numBytesRead);
			
			
			
		}
		
		System.out.println("All packets sent");
		
		System.out.println("Waiting for un-acked packets to be confirmed");
		
		while(!(queue.isEmpty()))
				{Thread.yield();	}
		
		
		System.out.println("Queue is empty, all packets acked");
		
		receiver.setNotTerminated(false);
		timer.cancel();
		
		System.out.println("Closing file input stream, udp socket, tcp socket");
		try {
			fis.close();
			udpSocket.close();
			tcpSocket.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	public synchronized void processSend(Segment seg, boolean retransmission){	
		try {
			DatagramPacket packet = new DatagramPacket(seg.getBytes(), seg.getBytes().length);
			udpSocket.send(packet);
			
			if(!retransmission) {
			queue.add(seg);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(queue.size() == 1){
			timer = new Timer(true);
			timeoutHandler = new TimeOutHandler(this);
			timer.schedule(timeoutHandler, rtoTimer);
		}
		
	}
	
	public synchronized void processACK(Segment ack){
		System.out.println("Received seq +" + ack.getSeqNum());
		
		int ackNum = ack.getSeqNum();
	
		
		
		if(queue.element() == null) {
			//do nothing
		}
		
		else if((ackNum < queue.element().getSeqNum()) || (ackNum > queue.element().getSeqNum() + windowSize)) {
			
			System.out.println(ackNum + " IS ACK");
			
		}
		else {
			
			base = queue.element().getSeqNum();
			
			timeoutHandler.cancel();
			
			for(int i = base; i < ackNum; i++) {
				try {
					System.out.println("Removing " + i );
					queue.remove();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
			
			
			
			if(! queue.isEmpty()){
				timeoutHandler = new TimeOutHandler(this);
				timer.schedule(timeoutHandler, rtoTimer);
			}
			
			
		}
		
	}
	
	public synchronized void processTimeout() {
		//resend all packets in window
		
		System.out.println("Retransmitting");
		
		Segment[] pendingSegments = queue.toArray();
		System.out.println("queue has " + pendingSegments.length + " elements");
		
		for(int i = 0; i < pendingSegments.length; i++) {
			System.out.println("sending " + pendingSegments[i].getSeqNum());
			processSend(pendingSegments[i], true);
		}
		
		if(! queue.isEmpty()){
			timeoutHandler = new TimeOutHandler(this);
			timer.schedule(timeoutHandler, rtoTimer);
		}
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
