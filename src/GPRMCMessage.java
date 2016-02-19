
/*
 * 
 */
class GPRMCMessage extends GpsMessage{
	public static final String TYPE_TEXT = "$GPRMC";
	public static final int LATITUDE_POSITION = 0,
				LONGITUDE_POSITION = 1,
				TIME_POSITION = 2;
	
	public String[] parseMessage(String message_in) {
		if(message_in == null)
			return null;
		
		String[] Out_LocationAndTime = new String[3];
		String[] message_array = message_in.split(",");
		
		if(message_array == null 
				|| message_array.length < 7
				|| !message_array[2].equals("A"))
			return null;
		
		Out_LocationAndTime[LATITUDE_POSITION] = minuteToDecibal(
								( (message_array[4].equals("S") )
										?"-":"")
								+ message_array[3] );
		Out_LocationAndTime[LONGITUDE_POSITION] = minuteToDecibal(
								( (message_array[6].equals("W") )
										?"-":"")
								+ message_array[5]);
		Out_LocationAndTime[TIME_POSITION] = message_array[1];
		
		for(String m : Out_LocationAndTime)
			if(m == null || m.equals(""))
				return null;
		
		return Out_LocationAndTime;
	}
	public String getMessageType(){
		return TYPE_TEXT;
	}
	
}
