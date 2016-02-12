import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.comm.CommPortIdentifier;
import javax.sound.sampled.Port;
import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;

import org.usb4java.ConfigDescriptor;
import org.usb4java.Context;
import org.usb4java.DescriptorUtils;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;


public class UsbConnector {
	private static Context context;
	public static boolean UsbConnect = false;
	public static boolean debug = false;
	protected static final short usbNull = -32634; 
	
	protected static Map<Device, DeviceDescriptor> devices = new HashMap<Device, DeviceDescriptor>();
	
	public UsbConnector(){
		while(context == null)
			try{
				setContext();
			}catch (LibUsbException e){
				
			}
	}
	public UsbConnector(boolean debug){
		this.debug = debug;
		setContext();
	}
	
	private static void setContext(){
		if (context != null)
			return;
		
		Context context = new Context();
		int result = LibUsb.init(context);
		if (result != LibUsb.SUCCESS) {
			if( debug )
				System.out.println("Could not connect to usb drivers");
			else
				throw new LibUsbException("Unable to initialize libusb.", result);
		}
		UsbConnect = true;
	}
	public static Device findADevice(short idVendor, short idProduct){
		return findADevice((int)idVendor,(int)idProduct);
	}
	public static Device findADevice(int idVendor, int idProduct){
	    // Read the USB device list
		if( !UsbConnect )
			return null;
		
	    DeviceList list = new DeviceList();
	    int result = LibUsb.getDeviceList(null, list);
	    if (result < 0) 
	    	throw new LibUsbException("Unable to get device list", result);

	    try{
	        // Iterate over all devices and scan for the right one
	        for (Device device: list){
	            DeviceDescriptor descriptor = new DeviceDescriptor();
	            result = LibUsb.getDeviceDescriptor(device, descriptor);
	            if (result != LibUsb.SUCCESS) 
	            	throw new LibUsbException("Unable to read device descriptor", result);
	            if (descriptor.idVendor() == idVendor && descriptor.idProduct() == idProduct) 
	            	return device;
	        }
	    }finally{
	        // Ensure the allocated device list is freed
	        LibUsb.freeDeviceList(list, true);
	    }
	    // Device not found
	    return null;
	}
	
	public static Map<Device, DeviceDescriptor> getDeviceInfoList(){
		if( !UsbConnect )
			return null;
		
		// Read the USB device list
	    DeviceList list = new DeviceList();
	    int result = LibUsb.getDeviceList(null, list);
	    	    
	    for (Device device: list){
            DeviceDescriptor descriptor = new DeviceDescriptor();
            result = LibUsb.getDeviceDescriptor(device, descriptor);
            if (result == LibUsb.SUCCESS && descriptor.idVendor() != usbNull) 
            	devices.put(device, descriptor);
        }
	    
	    return devices;
	}
			
	public static void main(String[] args) {
		/*
		Enumeration ports = CommPortIdentifier.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier port = (CommPortIdentifier)ports.nextElement();
			String type;
			switch (port.getPortType()) {
			case CommPortIdentifier.PORT_SERIAL:
				type = "Serial"; 
				break;
			default: /// Shouldn't happen
				type = "Unknown"; 
				break;
			}
			System.out.println(port.getName() + ": " + type);
			//System.out.println("\t"+port);
		}
		
		
		System.out.println("Starting manager . . .");
		//UsbConnector Manager = new UsbConnector();
		UsbConnector.setContext();
		*/
		/*
		System.out.println("Getting Descriptor List . . .");
		Map<Device, DeviceDescriptor> list = UsbConnector.getDeviceInfoList();
		System.out.println("List : ");
		DeviceDescriptor[] desc_list = (DeviceDescriptor[]) list.entrySet().toArray();
		for(DeviceDescriptor desc : desc_list)
			System.out.println(desc.idVendor()+" "+ desc.idProduct() );
		*/
		/*
		System.out.println("\nConnecting to device (1659, 8963) . . .");
		Device device = UsbConnector.findADevice(1659, 8963);
		System.out.println("Connected");
		System.out.println("Getting Descriptor . . .");
		DeviceDescriptor desc = new DeviceDescriptor();
        int result = LibUsb.getDeviceDescriptor(device, desc);
		System.out.println("Printing Results : ");
		System.out.println(desc.bcdUSB());
		System.out.println(desc.dump());
		System.out.println(desc.bcdDevice());
		System.out.println(desc.bDescriptorType());
		System.out.println(desc.iSerialNumber());
		//System.out.println(desc);
		 */
		try {
			UsbServices services = UsbHostManager.getUsbServices();
			UsbDevice device = (UsbDevice) services.getRootUsbHub();
			System.out.println(device + " : " + device.getParentUsbPort().getPortNumber());
		} catch (SecurityException | UsbException e) {
		}
		
	}
	protected void finalize(){
		LibUsb.exit(context);
	}
	public DeviceHandle openDevice(Device device){
		DeviceHandle handle = new DeviceHandle();
		int result = LibUsb.open(device, handle);
		if (result != LibUsb.SUCCESS) 
			throw new LibUsbException("Unable to open USB device" + device, result);
		
		return handle;
	}
	public static boolean closeDevice(DeviceHandle handle){
		LibUsb.close(handle);
		return true;
	}
	public static ByteBuffer createBuffer(byte a, byte b, byte c, byte d, byte e, byte f, byte g, byte h){
		ByteBuffer buffer = ByteBuffer.allocateDirect(8);
		buffer.put(new byte[] {a,b,c,d,e,f,g,h} );
		return buffer;
	}
	public static String writeSynchronus(ByteBuffer buffer, DeviceHandle handle, long timeout){
		int transfer = LibUsb.controlTransfer(handle, 
				(byte) (LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE), 
				(byte) 0x09,(short) 2,(short) 1, buffer, timeout );
		if(transfer < 0)
			throw new LibUsbException("Control transfer failed", transfer);
		return transfer + "";
	}
	public static String writeSynchronus(ByteBuffer buffer, DeviceHandle handle, byte endpnt, long timeout){
		IntBuffer transferred = IntBuffer.allocate(1);
		
		int result;
		if( (result = LibUsb.bulkTransfer(handle, endpnt, buffer, transferred, timeout)) 
				!= LibUsb.SUCCESS)
			throw new LibUsbException("Control tranfer failed", result);
		
		return transferred.get() + "";
	}
	public static String read(DeviceHandle handle, byte endpnt, long timeout){
		
		return "";
	}
	
	public static boolean connectInterface(DeviceHandle handle, int interfaceNum){
		int result;
		if(( result = LibUsb.claimInterface(handle, interfaceNum)) != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to claim interface", result);		
		
		return true;
	}
	public static boolean disconnectInterface(DeviceHandle handle, int interfaceNum){
		int result;
		if( (result = LibUsb.releaseInterface(handle, interfaceNum)) != LibUsb.SUCCESS)
				throw new LibUsbException("Unable to release interface", result);
		
		return true;
	}
	public static boolean releaseKernelDriver(DeviceHandle handle, int interfaceNum){
		boolean detach = LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)
						 && LibUsb.kernelDriverActive(handle, interfaceNum) == 1;
		int result;
		
		if(detach && ( result = LibUsb.detachKernelDriver(handle, interfaceNum)) != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to detach kernel driver", result);
		
		return true;
	}
	public static boolean reattachKernelDriver(DeviceHandle handle, int interfaceNum){
		int result;
		if( (result = LibUsb.detachKernelDriver(handle, interfaceNum)) != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to re-attach kernel driver",result);
		return true;
	}
	public static void dumpDevice(Device device){
		final int address = LibUsb.getDeviceAddress(device);
		final int busNum  = LibUsb.getBusNumber(device);
		if(debug)
			System.out.println(String.format("Device %03d/%03d", busNum, address));
		
		final int portNum = LibUsb.getPortNumber(device);
		if(debug)
			System.out.println("Connected to Port: " + portNum);
		
		final Device parent = LibUsb.getParent(device);
		if(parent != null){
			final int parentAddr = LibUsb.getDeviceAddress(parent);
			final int parentBus = LibUsb.getBusNumber(parent);
			if(debug)
				System.out.println(String.format("Parent : %03d/%03d", parentBus, parentAddr));
		}
		
		if(debug) 
			System.out.println("Speed: " + DescriptorUtils.getSpeedName(LibUsb.getDeviceSpeed(device)));
		
		final DeviceDescriptor descriptor = new DeviceDescriptor();
		int result = LibUsb.getDeviceDescriptor(device, descriptor);
		if(result < 0)
			throw new LibUsbException("Unable read device descriptor:", result);
		
		DeviceHandle handle = new DeviceHandle();
		result = LibUsb.open(device, handle);
		if(result < 0){
			System.err.println(String.format("Unable to open device %s. Continuing without device handle", LibUsb.strError(result)));
			handle = null;
		}
			
		System.out.println(descriptor.dump(handle));
		dumpConfigurationDescriptors(device, descriptor.bNumConfigurations());
		
		if(handle != null)
			LibUsb.close(handle);
	}
	private static void dumpConfigurationDescriptors(Device device, final int numConfig){
		for(int i = 0; i < numConfig; i++){
			final ConfigDescriptor descriptor = new ConfigDescriptor();
			final int result = LibUsb.getConfigDescriptor(device, (byte) i, descriptor);
			if(result < 0){
				return;
				//throw new LibUsbException("Unable to read config descriptor", result);
			}
			try{
				System.out.println(descriptor.dump().replaceAll("(?m)^", "\n"));
			}finally{
				LibUsb.freeConfigDescriptor(descriptor);
			}
		}
	}
}
class usbDevice {
	private Device device;
	private DeviceHandle handle;
	private DeviceDescriptor descriptor;
	
	private static final short VendorIsNull = -32634;
	
	public usbDevice(){}
	public usbDevice(Device device){
		this.device = device;
	}
	public usbDevice( DeviceHandle handle){
		this.handle = handle;
	}
	public usbDevice(DeviceDescriptor device){
		this.descriptor = device;
	}
	
	public void setDevice(Device device){
		if (this.device == null){
			handle = null;
			descriptor = null;
		}
		this.device = device;
	}
	
	public boolean setHandle(){
		DeviceHandle handle = new DeviceHandle();
		int result = LibUsb.open(device, handle);
		if (result != LibUsb.SUCCESS){ 
			handle = null;
			return false;
		}
		return true;
	}
	public boolean setDescriptor(){
		DeviceDescriptor descriptor = new DeviceDescriptor();
        int result = LibUsb.getDeviceDescriptor(device, descriptor);
        if (result != LibUsb.SUCCESS || descriptor.idVendor() == VendorIsNull){
        	descriptor = null;
        	return false;
        }
        return true;
	}
	public short getVendorId(){
		return (descriptor != null) ? descriptor.idVendor() : null;
	}
	public short getProductID(){
		return (descriptor != null) ? descriptor.idVendor() : null;
	}
	public DeviceHandle getHandle(){
		return handle;
	}
	public DeviceList getDeviceList(){
		 DeviceList list = new DeviceList();
		 int result = LibUsb.getDeviceList(null, list);
		 return (result == LibUsb.SUCCESS) ? list : null;
	}
}
