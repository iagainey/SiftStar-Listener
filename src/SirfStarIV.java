import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
<<<<<<< HEAD
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
=======

public class SirfStarIV implements Runnable{
	private static final short SirfStarIVProductId = 8963,
							   SirfStarIVVendorId  = 1659;
	
	private SimpleRead reader;
	private Thread  reader_thread;
	
	private boolean filling = true;
	private int sleepTime = 30;
	private static boolean debug_messages = false;
	
	private static final int maxMessageAmount = 100;
	private Queue<String[]> data = new LinkedBlockingQueue<String[]>(maxMessageAmount);
	private static final List<String> GoodMessages = new LinkedList<String>();
	private static final List<String> numerials = new ArrayList<String>();

	
	private int messageCount = 0,
				portNum 	 = 7;
	
	public SirfStarIV(){
		fillStaticVariables();
		setupPort();
		if(debug_messages)
			System.out.println("Port Setup Complete");
	}
	
	public SirfStarIV(LinkedBlockingQueue<String[]> q){
		fillStaticVariables();
		setQueue(q);
		setupPort();
	}
	public SirfStarIV(LinkedBlockingQueue<String[]> q, int portNum){
		fillStaticVariables();
		this.portNum = portNum;
		setQueue(q);
		setupPort();
	}
	public SirfStarIV(int portNum){
		fillStaticVariables();
		this.portNum = portNum;
		setupPort();
	}
	public boolean connect(int portNum){
		return reader.setCommPortId(portNum);
	}
	public static void debugMode(){
		debug_messages = true;
	}
	public void setSleepTime(int sec){
		sleepTime = sec;
	}
	private static final void fillStaticVariables(){
		if(GoodMessages.size() == 0){
			GoodMessages.add("$GPRMC");
		}
		if(numerials.size() == 0){
			String[] numbers = {"0", "1","2", "3", "4", "5","6","7","8","9","."};
			for(String number : numbers)
				numerials.add(number);
		}
	}
	public boolean setupPort(){
		if(reader == null){
			reader = new SimpleRead();
			reader_thread = new Thread(reader);
			reader.setCommPortId(portNum);
			reader.read();
			reader_thread.run();
			return true;
		}
		return false;
	}
>>>>>>> parent of ca7c3ea... Clean Up
	public void run(){
		try {
			if(connect(portNum))
<<<<<<< HEAD
				if( RunMessageAmount == 0){
					while(true){
=======
				if( messageCount == 0){
					while(true){
						this.wait(1000*sleepTime);
>>>>>>> parent of ca7c3ea... Clean Up
						fillDataQueue(1);
						this.wait(1000*auto_Update_Time);
					}
				}else{
<<<<<<< HEAD
					for(int i = 0; i < RunMessageAmount;i++){
						fillDataQueue(1);
						this.wait(1000*auto_Update_Time);
					}
=======
					fillDataQueue(messageCount);
>>>>>>> parent of ca7c3ea... Clean Up
				}
		} catch (InterruptedException e) {	}
	}
<<<<<<< HEAD
	/*
	 * Gets a message from the queue
	 *
	 * @return String[], returns the oldest message collected
	 *	null if no messages have been found yet
	 */
=======
>>>>>>> parent of ca7c3ea... Clean Up
	public String[] getNextMessage(){
		return data.poll();
	}
<<<<<<< HEAD
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
		
=======
	public void setQueue(LinkedBlockingQueue<String[]> q){
		if (q != null)
			data = q;
	}
	public Queue<String[]> getQueue(){
		return data;
	}
	
	private final String[] getGpsLocation(){
>>>>>>> parent of ca7c3ea... Clean Up
		String message;
		String type;
		do{
			message = reader.getMessage();
			if(message != null && message.length() > 6)
				type = message.substring(0, 6);
			else
				type = "";
		}while(!GoodMessages.contains(type));
		
		String[] out = parseLocation(type,message);
		//System.out.println("Time stamp "+out[2]);
		return out;
	}
	private static final String[] parseLocation(String type, String message){
		if(type.equals("$GPRMC")){
			if(message.charAt(18) != 'A'){
				int x = -1;
				for(int i = 0; i < message.length(); i++){
					if(message.charAt(i) == 'A'){
						x = i;
						break;
					}
					if( i+1 == message.length())
						return null;
				}
					
				return parseRMC(message, x - 18);
			}else{
				return parseRMC(message,0);
			}
		}else{
			System.out.println('\n'+type);
		}
		return null;
	}
	private static final String[] parseRMC(String message, int shift){
		if(shift < -7 || shift+42 >= message.length())
			return null;
		
<<<<<<< HEAD
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
=======
		String [] gps = new String[3];
		if(message.charAt(30 + shift) == 'S')
			gps[0] = "-";
		else
			gps[0] = "";
		gps[0] += message.substring(20+ shift, 22+ shift) 
				+ '.' 
				+ minuteToDecibal(message.substring(22+ shift, 24+ shift)
						+ message.substring(25+ shift, 29+ shift));

		if(message.charAt(43+ shift) == 'W')
			gps[1] = "-";
		else
			gps[1] = "";
		gps[1] += message.substring(( //Degrees
									(message.charAt(32+ shift) != '0')?
											32+ shift
											:33+ shift)
									, 35+ shift)
				+ '.' 
				+ minuteToDecibal(message.substring(35+ shift,37+ shift)
						+ message.substring(38+ shift,42+ shift));
		gps[2] = message.substring(7+ shift, 17+ shift);
		
		if(gps[0].contains(",") || gps[0].contains("null")
				|| gps[1].contains(",")  || gps[1].contains("null")
				|| gps[2].contains(",")){
			if(debug_messages)
				System.out.println("Shift trial failed "
							+shift + " " 
							+ message + "\n"
							+ gps[0] +", "
							+ gps[1]+ ", "
							+ gps[2]);
			return null;
		}else{
			return gps;
		}
	}
	private static final String minuteToDecibal(String in){
		int x;
		try{
			x = Integer.parseInt(keepNumerals(in));
		}catch(NumberFormatException e){
			if(debug_messages)
				System.out.println(in + " was translated to (" + keepNumerals(in) + ") Thus not readable by parseInt()");
			return null;
>>>>>>> parent of ca7c3ea... Clean Up
		}
		x = x / 60;
		return x+"";
		
	}
	private static final String keepNumerals(String in){
		String out = in;
		
		for(char letter : out.toCharArray())
			if( !numerials.contains(letter+""))
				out = removeChars(out , letter);
		
		return out;
	}
	/*
<<<<<<< HEAD
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
=======
	 * ParseNumerial job to to return the String of numerial
	 * that could be surrounded by junk then commas as follows
	 * 
	 * 	asdaaf,12443,afacd 	-> 12443
	 * 	12446,sadaa			-> 12446
	 *  dfvsv,651454		-> 651454
	 *  asdasd3242			-> 3242
	 *  ,,1231,				-> 1231
	 *  
	 *  ParseNumerial must also catch
	 *  
	 *   asdasd,2asda1,asd 	-> null
	 *   23ax53,s			-> null
	 *   sda,234d			-> null
	 *   2sdax5				-> null
	 *   2141fas2,4asda2	-> null
>>>>>>> parent of ca7c3ea... Clean Up
	 */
	@SuppressWarnings("unused")
	private static final String parseNumerials(String in){
		int countCommas = 0;
		int numCrit 	= 0;
		
		int[] commaLoc 	 = new int[2];
		int[] numCritLoc = new int[2];
		
		for( int i = 0; i < in.length(); i++ )
			if(in.charAt(i) == ','){
				if(countCommas >= commaLoc.length){
					countCommas++;
				}else{
					commaLoc[countCommas++] = i;
				}
			}else if( numerials.contains(in.charAt(i)+"") ){
				if(numCrit == numCritLoc.length-1){
					numCritLoc[numCrit] = i;
				}else{
					numCritLoc[numCrit++] = i;
				}
			}
		if(countCommas == 0){
			String numbersIn = parseNumerials(
					in.substring(commaLoc[0], commaLoc[1]));
			if(numbersIn.length() == numCritLoc[1] -numCritLoc[0])
				return numbersIn;
		}
		if(countCommas == 1){
			
		}
		if(countCommas == 2){
			
		}
		return null;
	}
	private static final String removeChars(String in, char x){
		if(in == null)
			return null;
		
		String out = "";
		for(char letter : in.toCharArray())
			if(letter != x)
				out+=letter;
		
		return out;
	}
	public void fillDataQueue(int num_of_data_points){
		for(int i = 0; i < num_of_data_points;i++){
			String[] message = getGpsMessage();
			if(message != null)
				data.add(message);
		}
	}
<<<<<<< HEAD
	/*
	 * Fills out the queue with messages from the port
	 *
	 * @param num_of_data_points, the number of messages to recieve
	 *	before stopping
	 */
=======
	
>>>>>>> parent of ca7c3ea... Clean Up
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
<<<<<<< HEAD
	/*
	 * As a Thread, stops the auto fill of the thread 
	 * and the thread itself
	 */
=======
	
>>>>>>> parent of ca7c3ea... Clean Up
	public void stopFillingDataQueue(){
		filling = false;
		reader.waitAll();
	}
<<<<<<< HEAD
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
=======
	
>>>>>>> parent of ca7c3ea... Clean Up
	public void finalize(){
		reader_thread.stop();
	}
<<<<<<< HEAD
	/*
	 * Base Case that listens to GPRMC messages and prints the location and time out
	 */
=======
>>>>>>> parent of ca7c3ea... Clean Up
	public static void main(String[] args){
		System.out.println("Connecting to usb port . . . (This does take a minute) ");
		
		SirfStarIV.debugMode();
		SirfStarIV device = new SirfStarIV();
<<<<<<< HEAD
		device.debugMode();
		device.addRecievedMessageType( new GPRMCMessage() );
		
=======
>>>>>>> parent of ca7c3ea... Clean Up
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
