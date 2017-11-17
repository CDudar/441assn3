import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ReceiverThread extends Thread {

	
	FastFtp master;
	
	boolean notTerminated = true;
	int currentExpectedACK = 0;
	
	
	DatagramSocket udpSocket;
	
	ReceiverThread(FastFtp master, DatagramSocket udpSocket){
		this.master = master;
		this.udpSocket = udpSocket;
	}
	
	
	public void run(){
		
		while(notTerminated){

			try {
			byte[] buf = new byte[1000];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			udpSocket.receive(packet);
			Segment receivedSeg = new Segment(packet);
			
			System.out.println("Received seq +" + receivedSeg.getSeqNum());
			

			
			
			
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			
		}
		
		
	}
	
}
