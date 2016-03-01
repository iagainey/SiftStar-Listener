import java.util.ArrayList;
import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;


public class UsbManager {

	public static void main(String[] args) {
		
		try {
			UsbServices service = UsbHostManager.getUsbServices();
			UsbHub hub = service.getRootUsbHub();
			
			UsbDevice[] devices = getDevices(hub);
			
			for(UsbDevice device : devices){
				UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
				System.out.println( desc.idProduct() + " : " + desc.idVendor());
			}
			
		} catch (SecurityException | UsbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public static UsbDevice[] getDevices(UsbHub hub){
		List<UsbDevice> list = new ArrayList<UsbDevice>();
		UsbDevice[] devices = (UsbDevice[]) hub.getAttachedUsbDevices().toArray(new UsbDevice[0]);
		for(UsbDevice device : devices){
			UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
			list.add(device);
			if(device.isUsbHub())
				addChilden((UsbHub)device, list);
		}
		return list.toArray(new UsbDevice[0]);
	}
	private static void addChilden(UsbHub hub, List<UsbDevice> list){
		UsbDevice[] devices = (UsbDevice[]) hub.getAttachedUsbDevices().toArray(new UsbDevice[0]);
		for(UsbDevice device : devices)
			list.add(device);
	}
	public UsbDevice findDevice(UsbHub hub, short vendorId, short productId){
		UsbDevice[] devices = (UsbDevice[]) hub.getAttachedUsbDevices().toArray(new UsbDevice[0]);
	    for (UsbDevice device : devices){
	        UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
	        if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
	        if (device.isUsbHub()){
	            device = findDevice((UsbHub) device, vendorId, productId);
	            if (device != null) 
	            	return device;
	        }
	    }
	    return null;
	}
	public String readConfigNum(UsbDevice device) throws IllegalArgumentException, UsbDisconnectedException, UsbException{
		UsbControlIrp irp = device.createUsbControlIrp(
			    (byte) (UsbConst.REQUESTTYPE_DIRECTION_IN
			          | UsbConst.REQUESTTYPE_TYPE_STANDARD
			          | UsbConst.REQUESTTYPE_RECIPIENT_DEVICE),
			    UsbConst.REQUEST_GET_CONFIGURATION,
			    (short) 0,
			    (short) 0
			    );
			irp.setData(new byte[1]);
			device.syncSubmit(irp);
			return irp.getData()[0]+"";
	}
	public UsbInterface openInterface(UsbDevice device){
		UsbConfiguration configuration = device.getActiveUsbConfiguration();
		UsbInterface iface = configuration.getUsbInterface((byte) 1);
		try {
			iface.claim();
		} catch (UsbNotActiveException | UsbDisconnectedException
				| UsbException e) {
			e.printStackTrace();
			return null;
		}
		return iface;
	}
	public boolean closeInterface(UsbInterface iface){
		try {
			iface.release();
			return true;
		} catch (UsbNotActiveException | UsbDisconnectedException
				| UsbException e) {
			return false;
		}
	}
	public boolean write(UsbInterface iface, byte endpnt, byte[] message){
		if(message.length != 8)
			return false;
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpnt);
		UsbPipe pipe = endpoint.getUsbPipe();
		boolean out = true;
		try {
			pipe.open();
			int sent = pipe.syncSubmit(message);
			System.out.println(sent + " bytes sent");
		} catch (UsbNotActiveException | UsbNotClaimedException
				| UsbDisconnectedException | UsbException e) {
			out = false;
		}finally{
			try {
				pipe.close();
			} catch (UsbNotActiveException | UsbNotOpenException
					| UsbDisconnectedException | UsbException e) {
				// TODO Auto-generated catch block
				out = false;
			}
			return out;
		}
		
	}
	public boolean read(UsbInterface iface, byte endpnt, byte[] message){
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpnt);
		UsbPipe pipe = endpoint.getUsbPipe();
		try {
			pipe.open();
			int received = pipe.syncSubmit(message);
			System.out.println(received + " bytes received");
		} catch (UsbNotActiveException | UsbNotClaimedException
				| UsbDisconnectedException | UsbException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}
	public static void dump(UsbDevice device, int level){
        for (int i = 0; i < level; i += 1)
            System.out.print("  ");
        System.out.println(device);
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices()) {
                dump(child, level + 1);
            }
        }
    }
}
