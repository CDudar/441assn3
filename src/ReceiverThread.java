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
			System.out.println("Receiving ACKS");
				
			byte[] buf = new byte[4];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			udpSocket.receive(packet);
			Segment receivedSeg = new Segment(packet);
			
			master.processACK(receivedSeg);
			System.out.println("returned from process ack");
			
			
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			
		}
		
		
	}
	
	void setNotTerminated(boolean notTerminated){
		this.notTerminated = notTerminated;
	}
	
	
}
