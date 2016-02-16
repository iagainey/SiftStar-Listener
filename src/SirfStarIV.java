import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
/*
 * Author : Isaac Gainey
 * Date Last Edited : Feb 16, 2016
 * 
 * Log :
 */
public class SirfStarIV implements Runnable{
	
	private SimpleRead reader;
	private Thread  reader_thread;
	
	private boolean filling = true;
	private boolean debug_messages = false;
	
	private long auto_Update_Time = 30;
	private int maxMessageAmount = 100;
	
	private Queue<String[]> 
					data = new LinkedBlockingQueue<String[]>(maxMessageAmount);
	private final Map<String,GpsMessage> 
					GoodMessages = new HashMap<String, GpsMessage>();
	
	private int messageMaxAmount = 0,
				portNum 	 = 7;
	/*
	 * 
	 */
	public SirfStarIV(){
	}
	/*
	 * 
	 */
	public SirfStarIV(Queue<String[]> q){
		setQueue(q);
	}
	/*
	 * 
	 */
	public SirfStarIV(Queue<String[]> queue, int port_number){
		this.portNum = port_number;
		setQueue(queue);
	}
	/*
	 * 
	 */
	public SirfStarIV(int portNum){
		this.portNum = portNum;
	}
	/*
	 * 
	 */
	private boolean connect(int portNum){
		return reader.setCommPortId(portNum);
	}
	/*
	 * 
	 */
	public void debugMode(){
		debug_messages = true;
	}
	/*
	 * 
	 */
	public void setSleepTime(int sec){
		auto_Update_Time = sec;
	}
	/*
	 * 
	 */
	private boolean setupPort(){
		if(reader == null){
			reader = new SimpleRead();
			reader_thread = new Thread(reader);
			reader.setCommPortId(portNum);
			reader.read();
			reader_thread.run();
			if(debug_messages)
					System.out.println("Port Setup Complete");
			return true;
		}
		return false;
	}
	/*
	 * 
	 */
	public void run(){
		try {
			setupPort();
			if(connect(portNum))
				if( messageMaxAmount == 0){
					while(true){
						this.wait(1000*auto_Update_Time);
						fillDataQueue(1);
					}
				}else{
					fillDataQueue(messageMaxAmount);
				}
		} catch (InterruptedException e) {	
			System.err.println(" Connection To USB GPS Device disconnected.");
			throw new RuntimeException();
		}
	}
	/*
	 * 
	 */
	public String[] getNextMessage(){
		setupPort();
		return data.poll();
	}
	/*
	 * 
	 */
	public void setQueue(Queue<String[]> q){
		if (q != null)
			data = q;
	}
	/*
	 * 
	 */
	public Queue<String[]> getQueue(){
		return data;
	}
	/*
	 * 
	 */
	private final String[] getGpsLocation(){
		setupPort();
		
		String message;
		String type;
		String[] out = null;
		
		while (out == null){
			do{
				message = reader.getMessage();

				if(message != null && message.length() > 6)
					type = message.substring(0, 6);
				else
					type = "";
			}while(!GoodMessages.keySet().contains(type));
			out = GoodMessages.get(type).parseMessage(message);
		}
		
		return out;
	}
	/*
	 * 
	 */
	public void fillDataQueue(int num_of_data_points){
		setupPort();
		
		for(int i = 0; i < num_of_data_points;i++){
			String[] location = getGpsLocation();
			if(location != null)
				data.add(location);
		}
	}
	/*
	 * 
	 */
	public void fillDataQueue() throws InterruptedException{
		filling = true;
		while(filling){
			String[] location = getGpsLocation();
			if(location != null){
				if(data.size() == maxMessageAmount)
					data.poll();
				data.add(location);
			}
		}
	}
	/*
	 * 
	 */
	public void stopFillingDataQueue(){
		filling = false;
	}
	/*
	 * 
	 */
	public boolean addRecievedMessageType(String message_tag, GpsMessage type_class){
		if(message_tag == null || message_tag.equals(""))
			return false;
		GoodMessages.put(message_tag, type_class);
		return true;
	}
	/*
	 * 
	 */
	public void finalize(){
		reader.Stop();
	}
	/*
	 * 
	 */
	public static void main(String[] args){
		System.out.println("Connecting to usb port . . . (This does take a minute) ");
		
		SirfStarIV device = new SirfStarIV();
		device.debugMode();
		device.addRecievedMessageType("$GPRMC", new GPRMCMessage() );
		
		int x = 0;
		String[] previous = {""};
		
		System.out.println("Nothing will print if minial movement occurs or"
				+ " if the gps cannot get a clear signal.\n");
		System.out.println("X : Latitude, Longitiude, Time");
		while(true){
			device.fillDataQueue(1);
			String[] message = device.getNextMessage();
			if(message != null){
				if(!previous[0].equals(message[0]) || !previous[1].equals(message[1])){
					System.out.println(x++ + " : " 
							+ message[0] + ", " 
							+ message[1] + ", " 
							+ message[2]);
					previous = message;
				}
			}
		}
	}
}
