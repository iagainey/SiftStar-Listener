

public abstract class GpsMessage{
	abstract public String[] parseMessage(String message);
	abstract public String getMessageType();
	
	protected static final String minuteToDecibal(String in){
		int x;
		String[] num = in.split(".");
		if(num == null || num.length < 2)
			return null;
		try{
			x = Integer.parseInt(num[1]);
		}catch(NumberFormatException e){
			return null;
		}
		num[1] = ( x / 60)+"" ;
		return num[0] + "." + num[1];
		
	}
}
