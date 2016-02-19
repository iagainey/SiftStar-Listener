import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
/*
 * Author : Isaac Gainey
 *
 * Date Edited : Feb 19, 2016
 * Log:
 *	Add comments to variables and methods
 *
 * Date Edited : Feb 16, 2016
 * Log :
 * 	Reworked the architure to be more clean and readable
 * 	Added GpsMessage class to add scalibly and modulilty
 * 	Remove static status from severial variables and methods
 * 	Fix the fraction of the second setting bug in the sleep mode
 * 	Changed setQueue() to accept any queue not just LinkedBlockingQ
 * 	Added stop() to SimpleRead class to replace the stop() for the Thread class
 * 	Added base cases to the new architure and error handling
 *
 * Summary:
 * On a 32 bit java system, reads message from SimpleRead class
 * parse the expected messages out and places them in a Queue
 * that will replace the most outDated Messages (by default)
 */
public class SirfStarIV implements Runnable{
	/*
	 * The Usb Reader Class and Thread to run
	 */
	private SimpleRead 	reader;
	private Thread  	reader_thread;
	/*
	 * If this class is made into a thread, bool filling
	 * will allow SirfStar to stop safely
	 *
	 * debug_messages : to set to allow debugging message
	 * 	to print
	 */
	private boolean filling 		= true;
	private boolean debug_messages 	= false;
	/*
	 * maxMessageAmount : The Size the queue will start deleting messages 
	 * portNum : the port number the reader will listening to
	 */
	private static final int maxMessageAmount = 100;
	private volatile     int portNum		  = 6;
	/*
	 * data : the default queue used to store messages
	 * GoodMessages : stores acceptable messsages and 
	 * 	how they should be parsed
	 */
	private volatile Queue<String[]> 
			data 		= new LinkedBlockingQueue<String[]>(maxMessageAmount);
	private volatile Map<String, GpsMessage> 
			GoodMessages 	= new HashMap<String, GpsMessage>();
	/*
	 * RunMessageAmount : the number of messages to 
	 * 	auto read as a thread before stopping
	 * auto_Update_Time : as a thread, the amount of time
	 *	to wait between reading messages
	 */
	private int  RunMessageAmount= 0;
	private long auto_Update_Time 	= 30;
	/*
	 * Default Constructor
	 */
	public SirfStarIV(){}
	/*
	 * Constructor for when the user wants to switch out the default queue
	 *
	 * @param queue, the queue where incoming messages are stored. The default
	 *	queue is LinkedBlockingQueue<String[]>
	 */
	public SirfStarIV(Queue<String[]> queue){
		setQueue(queue);
	}
	/*
	 * Constructor for when the user wants to set the port Number
	 *
	 * @param port_number, the port to listen to
	 */
	public SirfStarIV(int port_number){
		this.portNum = port_number;
	}
	/*
	 * Constructor for when the user wants tos witch out the default queue 
	 *	and set the port Number
	 *
	 * @param queue, the queue where incoming messages are stored. The default
	 *	queue is LinkedBlockingQueue<String[]>
	 * @param port_number, the port to listen to
	 *
	 */
	public SirfStarIV(Queue<String[]> queue, int port_number){
		this.portNum = port_number;
		setQueue(queue);
	}
	/*
	 * Attempt to open a link to established port_number
	 * 
	 * @param port_number, the port to attempt to open a link to
	 * @return boolean, if a link could be established
	 */
	protected boolean connect(int port_number){
		return reader.setCommPortId(port_number);
	}
	/*
	 * Turn default debug messages on
	 */
	public void debugMode(){
		debug_messages = true;
	}
	/*
	 * Alters the sleep time that run as a thread
	 * @param sleep_seconds, the number (or fraction) of seconds
	 * 	that are waited through before scanning till a acceptable
	 *	message is found
	 */
	public void setSleepTime(long sleep_seconds){
		auto_Update_Time = sleep_seconds;
	}
	/*
	 * Connects to the Usb port using the portNum
	 *
	 * @return boolean, if connection is complete
	 */
	protected boolean setupPort(){
		reader = new SimpleRead();
		reader.setCommPortId(portNum);
		reader.read();
		
		reader_thread = new Thread(reader);
		reader_thread.run();
		
		if(debug_messages)
			System.out.println("Port Setup Complete");
		return true;
	}
	/*
	 * Throws RunTimeException if it seems the SirfStarIV
	 * is disconnected
	 */
	public void run(){
		try {
			setupPort();
			if(connect(portNum))
				if( RunMessageAmount == 0){
					while(true){
						fillDataQueue(1);
						this.wait(1000*auto_Update_Time);
					}
				}else{
					for(int i = 0; i < RunMessageAmount;i++){
						fillDataQueue(1);
						this.wait(1000*auto_Update_Time);
					}
				}
		} catch (InterruptedException e) {	
			System.err.println(" Connection To USB GPS Device disconnected.");
			throw new RuntimeException();
		}
	}
	/*
	 * Gets a message from the queue
	 *
	 * @return String[], returns the oldest message collected
	 *	null if no messages have been found yet
	 */
	public String[] getNextMessage(){
		setupPort();
		return data.poll();
	}
	/*
	 * use to switch out the default queue
	 *
	 * @param Queue<String[], the queue where incoming messages are stored. The default
	 *	queue is LinkedBlockingQueue<String[]>
	 */
	public void setQueue(Queue<String[]> q){
		if (q != null)
			data = q;
	}
	/*
	 * use to get the default queue
	 *
	 * @return Queue<String[]>, the queue where incoming messages are stored. The default
	 *	queue is LinkedBlockingQueue<String[]>
	 */
	public Queue<String[]> getQueue(){
		return data;
	}
	/*
	 * scans incoming messages for any accepting messages and
	 * parses the message out
	 * 
	 * @return String[], the message after parse from the GPSMessage class
	 *	link to the message
	 */
	protected final String[] getGpsMessage(){
		setupPort();
		
		String message;
		String type;
		String[] out = null;
		
		if(GoodMessages.keySet().size() == 0)
			return null;
		
		while (out == null){
			do{
				message = reader.getMessage();
				if(debug_messages && message != null)
					System.out.println(message);
				if(message != null && message.length() > 6)
					type = message.substring(0, 6);
				else
					type = "";
			}while( !GoodMessages.keySet().contains(type) );
			out = GoodMessages.get(type).parseMessage(message);
		}
		
		return out;
	}
	/*
	 * Scans incoming messages that fits the assigned message_type
	 * 
	 * @return String[], the message after parse from the given GpsMessage class
	 */
	protected final String[] getGpsMessage(GpsMessage message_type){
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
			}while( !message_type.getMessageType().equals(type) );
			
			out = message_type.parseMessage(message);
		}
		
		return out;
	}
	/*
	 * Fills out the queue another num_of_data_points 
	 * 	from the port, and adds to the Queue
	 *
	 * @param num_of_data_points, the number of messages to recieve
	 *	before stopping
	 */
	public void fillDataQueue(int num_of_data_points){
		setupPort();
		
		for(int i = 0; i < num_of_data_points;i++){
			String[] message = getGpsMessage();
			if(message != null)
				data.add(message);
		}
	}
	/*
	 * Fills out the queue with messages from the port
	 *
	 * @param num_of_data_points, the number of messages to recieve
	 *	before stopping
	 */
	public void fillDataQueue() throws InterruptedException{
		filling = true;
		while(filling){
			String[] location = getGpsMessage();
			if(location != null){
				if(data.size() == maxMessageAmount)
					data.poll();
				data.add(location);
			}
		}
	}
	/*
	 * As a Thread, stops the auto fill of the thread 
	 * and the thread itself
	 */
	public void stopFillingDataQueue(){
		filling = false;
	}
	/*
	 * Adds a messageType that is auto-accepted
	 *
	 * @param type_class, what message to look for
	 * @return boolean, if the type_class is valid
	 */
	public boolean addRecievedMessageType(GpsMessage type_class){
		if(type_class == null || type_class.getMessageType().equals(""))
			return false;
		GoodMessages.put(type_class.getMessageType(), type_class);
		return true;
	}
	/*
	 * Also stops the background thread
	 */
	public void finalize(){
		reader.Stop();
	}
	/*
	 * Base Case that listens to GPRMC messages and prints the location and time out
	 */
	public static void main(String[] args){
		System.out.println("Connecting to usb port . . . (This does take a minute) ");
		
		SirfStarIV device = new SirfStarIV();
		device.debugMode();
		device.addRecievedMessageType( new GPRMCMessage() );
		
		int x = 0;
		String[] previous = {""};
		
		System.out.println("Connected to the port.\n\n"
				+ "Nothing will print if minial movement occurs or"
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
