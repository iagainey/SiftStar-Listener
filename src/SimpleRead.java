import java.io.IOException;
import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import javax.comm.*;


public class SimpleRead implements Runnable, SerialPortEventListener {
	private CommPortIdentifier portId;
	private static Enumeration portList;
	
	private int baudRate = 4800;
	private InputStream inputStream;
	private SerialPort serialPort;
	private Thread readThread;

	private boolean ready = false,
					stop = false;
	private char newLineChar = '$';
	private int queueSize = 100;
	private String message = "";
	private Queue<String> data = new LinkedBlockingQueue<String>(queueSize);
	
	private boolean debug_messages = false;
	
	public static void main(String[] args) {
		SimpleRead read = new SimpleRead();
		read.setCommPortId(6);
		read.read();
		int x = 0;
		while(true)
			if(read.data.size() > 0){
				System.out.println(x++);
				System.out.println(read.data.poll() );
			}
	}
	public boolean setCommPortId(int portNum) {
		if(portList != null){
			while (portList.hasMoreElements()) {
				portId = (CommPortIdentifier) portList.nextElement();
				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) 
					if (portId.getName().equals("COM"+portNum)){
						reset();
						return true;
					}
			}
			return false;
		}else{
			setEnumeration();
			return setCommPortId(portNum);
		}
	}
	public static void setEnumeration(){
		portList = CommPortIdentifier.getPortIdentifiers();
	}
	public boolean isReady(){
		return ready;
	}
	
	public SimpleRead() {
		reset();
	}
	public void setBaudRate(int rate){
		if(rate > 0)
			baudRate = rate;
	}
	public String getMessage(){
		return data.poll();
	}
	public boolean reset(){
		try {
			serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
		} catch (PortInUseException e) {
			return false;
			//System.out.println(e);
		} catch (NullPointerException e){
			return false;
		}
		try {
			inputStream = serialPort.getInputStream();
		} catch (IOException e) {
			return false;
			//System.out.println(e);
		}
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			return false;
			//System.out.println(e);
		}
		
		serialPort.notifyOnDataAvailable(true);
		try {
			serialPort.setSerialPortParams(baudRate,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {
			return false;
			//System.out.println(e);
		}
		ready = true;
		readThread = new Thread();
		readThread.start();
		return true;
	}
	public boolean read(){
		if(debug_messages)
			System.out.println("SimpleRead.read() : opening serial port");
		if(portId == null)
			return false;
		try {
			if(serialPort == null)
				serialPort = (SerialPort) portId.open("USB Comm Reader", 2000);
		} catch (PortInUseException e) {
			System.out.println(e);
			System.err.println("Port currently owned and in use");
		}
		if(serialPort == null)
			return false;
		if(debug_messages)
			System.out.println("SimpleRead.read() : openings input stream");
		try {
			if(inputStream == null)
					inputStream = serialPort.getInputStream();
				
		} catch (IOException e) {
			System.out.println(e);
		} catch(NullPointerException e){
			System.err.println("Please reset the Computer : There is a Thread hogging the usb ports"
					+ " If you still see this message then cry");
		}
		if(debug_messages)
			System.out.println("SimpleRead.read() : Adding Event Listener");
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			//System.out.println(e);
		} catch (NullPointerException e){
			return false;
		}
		if(debug_messages)
			System.out.println("SimpleRead.read() : Uploading comm settings");
		serialPort.notifyOnDataAvailable(true);
		try {
			serialPort.setSerialPortParams(baudRate,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {
			System.out.println(e);
		}
		return true;
	}
	public void run() {
		try {
			Thread.sleep(20000);
			if(stop){
				System.exit(0);
				this.finalize();
			}
		} catch (Throwable e) {
			System.out.println(e);
		}
	}

	public void serialEvent(SerialPortEvent event) {
		switch(event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			byte[] readBuffer = new byte[20];

			try {
				while (inputStream.available() > 0) {
					int numBytes = inputStream.read(readBuffer);
				}
				readBuffer = removeEmpty(readBuffer);
				
				addLetter( new String(readBuffer)) ;
			} catch (IOException e) {

			}
			break;
		}
	}
	private byte[] removeEmpty(byte[] arr){
		int numE = 0;
		for(byte e : arr)
			if(e != 0)
				numE++;
		
		byte[] out = new byte[numE];
		numE = 0;
		
		for(int i = 0; i < arr.length ; i++)
			if(arr[i] != 0)
				out[numE++] = arr[i];
		
		return out;
	}
	public final void waitAll(){
		try {
			readThread.wait();
			super.wait();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
		
	}
	private void addLetter(String m){
		if(m == null)
			return;
		m = m.trim();
		
		if(m.equals(""))
			return;
		
		
		if(m.contains(newLineChar+"")){
			if(data.size() >= queueSize)
				data.poll();
			
			try{
				data.add(message);
			}catch( IllegalStateException e){
				data.poll();
				data.add(message);				
			}
			message = m;
		}else{
			message += m;
		}
	}
	public void Stop(){
		stop = true;
	}
	public void debugMode(){
		debug_messages = true;
	}
}