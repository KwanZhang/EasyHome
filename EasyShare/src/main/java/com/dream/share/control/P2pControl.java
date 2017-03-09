package com.dream.share.control;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.dream.share.DMCApplication;
import com.dream.share.stream.HttpServer;
import com.dream.share.util.NetUtil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


public class P2pControl extends Handler{
	private static final String TAG = "P2pControl";
	private static final int SOCKET_TIMEOUT = 5000;
	public final static int MESSAGE_SEND_DATA = 1;
	public final static String MSG_KEY_DATA = "Data";
	private String mHost = null;
	private int mDataPort = 0;
	private OutputStream mOutputstream;
	public P2pControl(){
	}
	public P2pControl(Looper looper){
		super(looper);
	}
	public void close(){
	}
	public void setupUDP(String ip, int dataPort){
		mDataPort = dataPort;
		mHost = ip;
	}

	private void sendMessage(String action){
		Log.d(TAG, "action " +action);
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.what = MESSAGE_SEND_DATA;
		
		data.putString(MSG_KEY_DATA, action);
		
		msg.setData(data);
		P2pControl.this.sendMessage(msg);
	}
	private boolean streamFile(String filepath){
		/*LocalFileStreamingServer fileserver = DMCApplication.getInstance().getFileServer();
		if(fileserver != null){
			fileserver.setLocalFileStreamingServer(new File(filepath));
			if(fileserver.isRunning()){
				fileserver.stop();
			}
			fileserver.start();
			return true;
		}
		return false;*/
		return true;
	}
	public void showImage(String filepath,int width, int height){
		if(filepath == null){
			return;
		}

		if(!streamFile(filepath)){
			return;
		}
		String action = "ShowImage" + "\r\n" +
			"filepath:"+filepath+"\r\n" +
			"width:" + width + "\r\n" +
			"height:" + height + "\r\n";
		
		sendMessage(action);
	}

	
	public void playNode(String filepath){
		if(filepath == null){
			return;
		}
		if(!streamFile(filepath)){
			return;
		}
		HttpServer fileserver = DMCApplication.getInstance().getFileServer();
		String uri = fileserver.getFileUrl(filepath);
		String action = "SetPlay" + "\r\n" +
			"uri:"+uri+"\r\n";
		sendMessage(action);
	}

	public int setPlayStatus(int state){
		String action = "SetPlayStatus" + "\r\n" +
			"state:"+state+"\r\n";
		sendMessage(action);
		return 0;
	}

	public int setVoice(int process){
		String action = "SetVoice" + "\r\n" +
			"Voice:"+process+"\r\n";
		sendMessage(action);
		return 0;
	}
	public int setVoiceDown(){
		String action = "SetVoiceDown" + "\r\n" ;
		sendMessage(action);
		return 0;
	}
	
	public int setVoiceUp(){
		String action = "SetVoiceUp" + "\r\n" ;
		sendMessage(action);
		return 0;
	}
	public int registerStatusListener(int port){
		String addr = NetUtil.getP2pAddr();
		String action = "RegisterStatusListener" + "\r\n" +
			"Port:"+port+"\r\n"+
			"IP:"+addr+"\r\n";
		
		sendMessage(action);
		return 0;
	}
	public int seekTo(int pos){
		String action = "SeekPlayer" + "\r\n" +
			"Pos:"+pos+"\r\n";
		
		sendMessage(action);
		return 0;
	}
	public void sendMessageStatus(String Status){
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.what = MESSAGE_SEND_DATA;
		String status = "STATUS" + "\r\n" +
				"Status:" + Status + "\r\n";
		data.putString(MSG_KEY_DATA, status);
		msg.setData(data);
		sendMessage(msg);
	}
	public void sendMessagePostion(int value){
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.what = MESSAGE_SEND_DATA;
		String status = "POSTION" + "\r\n" +
				"Value:" + value + "\r\n";
		data.putString(MSG_KEY_DATA, status);
		msg.setData(data);
		sendMessage(msg);
	}
	public void sendData(String host, int port,byte[] data) throws IOException {
		if (data == null) {
			return;
		}
		Socket socket = new Socket();

		try{
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
			//mInputstream = socket.getInputStream();
			mOutputstream = socket.getOutputStream();
		}catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return ;
		}
		try {
			mOutputstream.write(data);
			mOutputstream.flush();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void handleMessage(Message msg){
		Log.d(TAG,"msg is " + msg.what);
		switch(msg.what){
		case MESSAGE_SEND_DATA:{
				if(mHost == null || mDataPort == 0)
					return;
				String data = msg.getData().getString(MSG_KEY_DATA);
				try {
					sendData(mHost, mDataPort, data.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG,"send data UnsupportedEncodingException " 
							+ e.toString());
				} catch (IOException e) {
					Log.e(TAG,"send data IOException " + e.toString());
				}
			}
			break;
		}
	}
} 
