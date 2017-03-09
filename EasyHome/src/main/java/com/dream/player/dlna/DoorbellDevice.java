package com.dream.player.dlna;

import java.io.IOException;

import org.cybergarage.http.HTTPRequest;
import org.cybergarage.http.HTTPResponse;
import org.cybergarage.http.HTTPStatus;
import org.cybergarage.net.HostInterface;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.control.QueryListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.cybergarage.util.Debug;

import com.dream.player.Globals;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DoorbellDevice extends Device implements ActionListener, QueryListener{
	private final static String TAG = "DoorbellDevive";
	private final static String PRESENTATION_URI = "/presentation";
	private Handler mHandler;
	
	public final static String DESCRIPTION = 
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
			"<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
			"   <specVersion>\n" +
			"      <major>1</major>\n" +
			"      <minor>0</minor>\n" +
			"   </specVersion>\n" +
			"   <device>\n" +
			"      <deviceType>urn:schemas-upnp-org:device:MediaServer:1</deviceType>\n" +
			"      <friendlyName>Dream Player</friendlyName>\n" +
			"      <manufacturer>kwan</manufacturer>\n" +
			"      <manufacturerURL>null</manufacturerURL>\n" +
			"      <modelDescription>Provides content through UPnP ContentDirectory service</modelDescription>\n" +
			"      <modelName>Dream Player</modelName>\n" +
			"      <modelNumber>1.0</modelNumber>\n" +
			"      <modelURL>http://www.cybergarage.org</modelURL>\n" +
			"      <UDN>uuid:362d9414-31a0-48b6-b684-2b4bd38391d1</UDN>\n" +
			"      <serviceList>\n" +
			"         <service>\n" +
			"            <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>\n" +
			"            <serviceId>urn:upnp-org:serviceId:urn:schemas-upnp-org:service:ConnectionManager</serviceId>\n" +
			"            <SCPDURL>/service/ConnectionManager1.xml</SCPDURL>\n" +
			"            <controlURL>/service/ConnectionManager_control</controlURL>\n" +
			"            <eventSubURL>/service/ConnectionManager_event</eventSubURL>\n" +
			"         </service>\n" +
			"      </serviceList>\n" +
			"   </device>\n" +
			"</root>";
	public final static String SCPD = 
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
			"<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\n" +
			"   <specVersion>\n" +
			"      <major>1</major>\n" +
			"      <minor>0</minor>\n" +
			"	</specVersion>\n" +
			" 	<actionList> \n" +
			" 		<action> \n" +
			" 			<name>SetPlay</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>uri</name> \n" +
			" 					<relatedStateVariable>Uri</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>StatusReportHost</name> \n" +
			" 					<relatedStateVariable>StatusReportHost</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>StatusReportPort</name> \n" +
			" 					<relatedStateVariable>StatusReportPort</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>SetupWifiDirect</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>Mac</name> \n" +
			" 					<relatedStateVariable>Mac</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>SetPlayStatus</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>State</name> \n" +
			" 					<relatedStateVariable>State</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>SetVoiceDown</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>SetVoiceUp</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>RegisterStatusListener</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>IP</name> \n" +
			" 					<relatedStateVariable>IP</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Port</name> \n" +
			" 					<relatedStateVariable>State</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>StartKtv</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>IP</name> \n" +
			" 					<relatedStateVariable>IP</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Port</name> \n" +
			" 					<relatedStateVariable>State</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>SeekPlayer</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>Pos</name> \n" +
			" 					<relatedStateVariable>Pos</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 		<action> \n" +
			" 			<name>ShowImage</name> \n" +
			" 			<argumentList> \n" +
			" 				<argument> \n" +
			" 					<name>filepath</name> \n" +
			" 					<relatedStateVariable>FilePath</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>width</name> \n" +
			" 					<relatedStateVariable>width</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>height</name> \n" +
			" 					<relatedStateVariable>height</relatedStateVariable> \n" +
			" 					<direction>in</direction> \n" +
			" 				</argument> \n" +
			" 				<argument> \n" +
			" 					<name>Result</name> \n" +
			" 					<relatedStateVariable>Result</relatedStateVariable> \n" +
			" 					<direction>out</direction> \n" +
			" 				</argument> \n" +
			" 			</argumentList> \n" +
			" 		</action> \n" +
			" 	</actionList> \n" +
			" 	<serviceStateTable> \n" +
			" 		<stateVariable sendEvents=\"yes\"> \n" +
			" 			<name>Time</name> \n" +
			" 			<dataType>string</dataType> \n" +
			" 		</stateVariable> \n" +
			" 		<stateVariable sendEvents=\"no\"> \n" +
			" 			<name>Result</name> \n" +
			" 			<dataType>string</dataType> \n" +
			" 		</stateVariable> \n" +
			" 	</serviceStateTable> \n" +
			"</scpd>";	
	public final static String SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1";
	private void loadDescription(){
		try {
			initialize(DESCRIPTION,SCPD);
		} catch (InvalidDescriptionException e) {
			Log.e(TAG, "load description " + e.toString());
		}
	}
	private void initialize(String description, String connectionManagerSCPD) throws InvalidDescriptionException
	{
		loadDescription(description);
		
		Service servConDir = getService(SERVICE_TYPE);
		servConDir.loadSCPD(connectionManagerSCPD);
	}
	public DoorbellDevice(Handler handler) throws InvalidDescriptionException, IOException{
		super();
		mHandler = handler;
		Debug.on();
		Log.d(TAG, "load doorbell Description");
		loadDescription();
		setSSDPBindAddress(
				HostInterface.getInetAddress(HostInterface.IPV4_BITMASK, null)
		);
		setHTTPBindAddress(
				HostInterface.getInetAddress(HostInterface.IPV4_BITMASK, null)
		);
		
		Service servConMan = getService(SERVICE_TYPE);
		servConMan.setActionListener(this);
		servConMan.setQueryListener(this);
	}
	
	////////////////////////////////////////////////
	// ActionListener
	////////////////////////////////////////////////

	public boolean actionControlReceived(Action action)
	{
		String actionName = action.getName();
		Log.d(TAG,"actionControlReceived actionName " + actionName);

		if (actionName.equals("SetPlay") == true) {
			String uri = action.getArgument("uri").getValue();
			int port = action.getArgumentIntegerValue("StatusReportPort");
			String host = action.getArgumentValue("StatusReportHost");
			Log.d(TAG,"uri is " + uri + " StatusReportPort " + port + 
					"StatusReportHost " + host);
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putString(Globals.URL_EXTRA, uri);
			bundle.putInt(Globals.PORT_EXTRA, port);
			bundle.putString(Globals.IP_EXTRA, host);
			msg.setData(bundle);
			msg.what = DlnaService.PLAY_URI;
			mHandler.sendMessage(msg);
			return true;
		}else if (actionName.equals("ShowImage")){
			
			String path = action.getArgument("filepath").getValue();
			int width = action.getArgument("width").getIntegerValue();
			int height = action.getArgument("height").getIntegerValue();
			Log.d(TAG,"receiver setup send image action width " + width + " height " 
					+ height + " path " + path);
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(Globals.IMAGE_WIDTH, width);
			bundle.putInt(Globals.IMAGE_WIDTH, height);
			bundle.putString(Globals.PATH_EXTRA, path);
			msg.setData(bundle);
			msg.what = DlnaService.SHOW_IMAGE;
			mHandler.sendMessage(msg);
			return true;
		}else if (actionName.equals("SetupWifiDirect")){
			String mac = action.getArgument("Mac").getValue();
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putString(Globals.MAC_EXTRA, mac);
			msg.setData(bundle);
			msg.what = DlnaService.SETUP_WIFI_DIRECT;
			mHandler.sendMessage(msg);
			return true;
		} else if (actionName.equals("SetPlayStatus")){
			int state = action.getArgument("State").getIntegerValue();
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(Globals.STATE_EXTRA, state);
			msg.setData(bundle);
			msg.what = DlnaService.SET_PLAY;
			mHandler.sendMessage(msg);
			return true;
		} else if(actionName.equals("SetVoiceDown")){
			mHandler.sendEmptyMessage(DlnaService.SET_VOICE_DOWN);
			return true;
		} else if(actionName.equals("SetVoiceUp")){
			mHandler.sendEmptyMessage(DlnaService.SET_VOICE_UP);
			return true;
		} else if(actionName.equals("RegisterStatusListener")){
			int port = action.getArgument("Port").getIntegerValue();
			String ip = action.getArgument("IP").getValue();
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(Globals.PORT_EXTRA, port);
			bundle.putString(Globals.IP_EXTRA, ip);
			msg.setData(bundle);
			msg.what = DlnaService.REGISTER_STATUS_LISTNER;
			mHandler.sendMessage(msg);
			return true;
		} else if (actionName.equals("SeekPlayer")){
			int pos = action.getArgument("Pos").getIntegerValue();
			Log.d(TAG,"Pos " + pos);
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(Globals.POS_EXTRA, pos);
			msg.setData(bundle);
			msg.what = DlnaService.SEEK_PLAYER;
			mHandler.sendMessage(msg);
			return true;
		} else if(actionName.equals("StartKtv")){
			int port = action.getArgument("Port").getIntegerValue();
			String ip = action.getArgument("IP").getValue();
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(Globals.PORT_EXTRA, port);
			bundle.putString(Globals.IP_EXTRA, ip);
			msg.setData(bundle);
			msg.what = DlnaService.START_KTV;
			mHandler.sendMessage(msg);
			return true;
		}
		return false;
	}

	////////////////////////////////////////////////
	// QueryListener
	////////////////////////////////////////////////

	public boolean queryControlReceived(StateVariable stateVar)
	{
		Log.d(TAG,"queryControlReceived ");
		return true;
	}
	
	////////////////////////////////////////////////
	// HttpRequestListner
	////////////////////////////////////////////////
	
	public void httpRequestRecieved(HTTPRequest httpReq)
	{
		String uri = httpReq.getURI();
		Log.d(TAG,"httpRequestRecieved uri" + uri + 
				"des " + httpReq.toString());
		if (uri.startsWith(PRESENTATION_URI) == false) {
			super.httpRequestRecieved(httpReq);
			return;
		}
			 
		String contents = "<HTML><BODY><H1>" + "http request echo" + "</H1></BODY></HTML>";
		
		HTTPResponse httpRes = new HTTPResponse();
		httpRes.setStatusCode(HTTPStatus.OK);
		httpRes.setContent(contents);
		httpReq.post(httpRes);
	}
}
