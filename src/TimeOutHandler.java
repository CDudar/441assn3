import java.util.TimerTask;

public class TimeOutHandler extends TimerTask {

	
	FastFtp master;
	
	public TimeOutHandler(FastFtp master){
		this.master = master;
	
	}
	
	
	public void run(){
		master.processTimeout();
		
	}
	
	
}
