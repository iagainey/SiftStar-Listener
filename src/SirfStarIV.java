import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

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
	public void run(){
		try {
			if(connect(portNum))
				if( messageCount == 0){
					while(true){
						this.wait(1000*sleepTime);
						fillDataQueue(1);
					}
				}else{
					fillDataQueue(messageCount);
				}
		} catch (InterruptedException e) {	}
	}
	public String[] getNextMessage(){
		return data.poll();
	}
	public void setQueue(LinkedBlockingQueue<String[]> q){
		if (q != null)
			data = q;
	}
	public Queue<String[]> getQueue(){
		return data;
	}
	
	private final String[] getGpsLocation(){
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
			String[] location = getGpsLocation();
			if(location != null)
				data.add(location);
		}
	}
	
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
	
	public void stopFillingDataQueue(){
		filling = false;
		reader.waitAll();
	}
	
	public void finalize(){
		reader_thread.stop();
	}
	public static void main(String[] args){
		System.out.println("Connecting to usb port . . . (This does take a minute) ");
		
		SirfStarIV.debugMode();
		SirfStarIV device = new SirfStarIV();
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
