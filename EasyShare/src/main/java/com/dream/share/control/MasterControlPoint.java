package com.dream.share.control;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.UPnPStatus;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.NotifyListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.event.EventListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;

import com.dream.share.DLNAContainer;
import com.dream.share.DMCApplication;
import com.dream.share.stream.HttpServer;
import com.dream.share.util.LogUtil;

import android.util.Log;

public class MasterControlPoint extends ControlPoint 
	implements NotifyListener, EventListener, SearchResponseListener{
	private final static String TAG = "MasterControlPoint";
	public final static String SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1";
	private String mIP = "192.168.43.1";
	
	private DeviceChangeListener mDeviceChangeListener = new DeviceChangeListener() {

		@Override
		public void deviceRemoved(Device dev) {
			LogUtil.d(TAG, "control point remove a device");
			DLNAContainer.getInstance().removeDevice(dev);
		}

		@Override
		public void deviceAdded(Device dev) {
			//  setPower();
			LogUtil.d(TAG, "control point add a device..." + dev.getDeviceType() + dev.getFriendlyName());
			DLNAContainer.getInstance().addDevice(dev);
		}
	};
	
	public MasterControlPoint(){
		addNotifyListener(this);
		addSearchResponseListener(this);
		addEventListener(this);
		addDeviceChangeListener(mDeviceChangeListener);
	}
	
	public void setIP(String ip){
		if(ip != null){
			mIP = ip;
		}
	}
	
	public void Off(){
		unsubscribe();
	}
	
	////////////////////////////////////////////////
	//	Device (Common)
	////////////////////////////////////////////////
	
	public boolean isDevice(SSDPPacket packet, String deviceType)
	{
		String usn = packet.getUSN();
		if (usn.endsWith(deviceType))
			return true;
		return false;
	}
	
	public org.cybergarage.upnp.Service getDeviceService(String deviceType, String serviceType)
	{
		Device dev = getDevice(deviceType);
		if (dev == null)
			return null;
		org.cybergarage.upnp.Service service = dev.getService(serviceType);
		if (service == null)
			return null;
		return service;
	}

	public boolean subscribeService(SSDPPacket packet, String deviceType, String serviceType)
	{
		org.cybergarage.upnp.Service service = getDeviceService(deviceType, serviceType);
		if (service == null)
			return false;
		return subscribe(service);
	}

	////////////////////////////////////////////////
	//	Listener
	////////////////////////////////////////////////
	
	public void deviceNotifyReceived(SSDPPacket packet)
	{
		Log.d(TAG,"deviceNotifyReceived "  + 
				"Remote address"+ packet.getRemoteAddress() + 
				"Local address " + packet.getLocalAddress());
	}
	
	public void deviceSearchResponseReceived(SSDPPacket packet)
	{
		Log.d(TAG,"deviceSearchResponseReceived "  + 
				"Remote address"+ packet.getRemoteAddress() + 
				"Local address " + packet.getLocalAddress());
	}
	
	
	public void eventNotifyReceived(String uuid, long seq, String name, String value)
	{
		Log.d(TAG,"Notify = " + uuid + ", " + seq + "," + name + "," + value);
	}
	
	
	public void showImage(String filepath,int width, int height){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return;
			}
			if (filepath == null){
				LogUtil.e(TAG, "filepath is null");
				return;
			}
			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return;
			}
		}

		Action action = service.getAction("ShowImage");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return;
		}
		HttpServer fileserver = DMCApplication.getInstance().getFileServer();
		String uri = null;
		if(fileserver != null){
			uri = fileserver.getFileUrl(filepath);
		}
		if(uri == null)
			return;
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("filepath").setValue(uri);
		argumentList.getArgument("width").setValue(width);
		argumentList.getArgument("height").setValue(height);
		if (action.postControlAction()) {
			Log.d(TAG,"send setup image action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			DMCApplication.getInstance().setCurrentUrl(filepath);
			LogUtil.d(TAG,"result value = \n" + result.getValue());	
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
	}
	public void playNode(String filepath){

		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return;
			}
			if (filepath == null){
				LogUtil.e(TAG, "filepath is null");
				return;
			}
			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return;
			}
		}
		Action action = service.getAction("SetPlay");
		if(action == null){
			LogUtil.e(TAG,"action for SetPlay is null!!!");
			return;
		}
		ArgumentList argumentList = action.getArgumentList();
		//argumentList.getArgument("uri").setValue("rtsp://" + mIP + ":8554/" + filepath);
		HttpServer fileserver = DMCApplication.getInstance().getFileServer();
		String uri = null;
		if(fileserver != null){
			uri = fileserver.getFileUrl(filepath);
			
			/*fileserver.setLocalFileStreamingServer(new File(filepath));
			if(fileserver.isRunning()){
				fileserver.stop();
			}
			fileserver.start();*/
		}
		if(uri == null)
			return;
		argumentList.getArgument("uri").setValue(uri);
		int port = DMCApplication.getInstance().getPlayerStatusListener().getPort();
		
		argumentList.getArgument("StatusReportPort").setValue(port);
		argumentList.getArgument("StatusReportHost").setValue(mIP);
		if (action.postControlAction()) {
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			
			DMCApplication.getInstance().setCurrentUrl(filepath);
			LogUtil.d(TAG,"result value = \n" + result.getValue());	
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
	}
    public void startWifiDirect(String mac){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return;
			}
			if (mac == null){
				LogUtil.e(TAG, "filepath is null");
				return;
			}
			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return;
			}
		}
		Action action = service.getAction("SetupWifiDirect");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return;
		}
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("Mac").setValue(mac);
		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue());	
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
    }
    public int getPlayStatus(int state){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("getPlayStatus");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("state").setValue(""+state);
		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue());	
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
    }
    public int setPlayStatus(int state){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
    	Action action = service.getAction("SetPlayStatus");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}

		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue());	
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
    }

	public int setVoice(int process){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("SetVoice");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("Voice").setValue(""+process);

		if (action.postControlAction()) {
			LogUtil.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue()); 
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
	}
	public int getVoice(){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("SetVoice");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}

		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue()); 
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
	}
	public int setVoiceDown(){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("SetVoiceDown");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}

		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue()); 
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
	}
	public int setVoiceUp(){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("SetVoiceUp");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}

		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue()); 
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
	}
	public int registerStatusListener(int port){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("RegisterStatusListener");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("Port").setValue(port);
		argumentList.getArgument("IP").setValue(mIP);
		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue()); 
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
	}
	public int seekTo(int pos){

		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("SeekPlayer");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("Pos").setValue(pos);
		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue()); 
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
	}
	public int startKtv(int port,String host){
		org.cybergarage.upnp.Service service = DLNAContainer.getInstance().getCurService();
		if(service == null){
			Device selDevice = DLNAContainer.getInstance().getSelectedDevice();
			if (selDevice == null) {
				LogUtil.e(TAG,"no selDevice!!!");
				return -1;
			}

			service = selDevice.getService(SERVICE_TYPE);
			if (service == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return -1;
			}
		}
		Action action = service.getAction("StartKtv");
		if(action == null){
			LogUtil.e(TAG,"action for Browse is null!!!");
			return -1;
		}
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("Port").setValue(port);
		argumentList.getArgument("IP").setValue(mIP);
		if (action.postControlAction()) {
			Log.d(TAG, "send start WifiDirect action");
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			//mWifiDirectHandler.sendEmptyMessage(CMD_DISCOVER);
			LogUtil.d(TAG,"result value = \n" + result.getValue()); 
			return result.getIntegerValue();
		} else {
			UPnPStatus err = action.getControlStatus();
			LogUtil.e(TAG,"Error Code = " + err.getCode());
			LogUtil.e(TAG,"Error Desc = " + err.getDescription());
		}
		return -1;
	}
}
