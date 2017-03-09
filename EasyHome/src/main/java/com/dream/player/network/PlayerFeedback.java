package com.dream.player.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class PlayerFeedback {
	private static final String TAG = "PlayerFeedback";
	public final static int MESSAGE_SEND_DATA = 1;
	public final static String MSG_KEY_DATA = "Data";
    private SocketSender 		 mSocketSender;
    private HandlerThread		 mSocketThread;
    
    public  PlayerFeedback(){
		if(mSocketSender == null){
			mSocketThread = new HandlerThread("SocketClient");
			mSocketThread.start();
			mSocketSender = new SocketSender(mSocketThread.getLooper());
		}
    }
	public void stopSender(){
		if(mSocketSender != null){
			mSocketSender.close();
			mSocketThread.quit();

		}
	}
	public void setupSender(String ip, int port){

		mSocketSender.setupUDP(ip, port);
	}
    public void sendMessageStatus(String Status){
    	Message msg = new Message();
    	Bundle data = new Bundle();
    	msg.what = MESSAGE_SEND_DATA;
    	String status = "STATUS" + "\r\n" +
    			"Status:" + Status + "\r\n";
    	data.putString(MSG_KEY_DATA, status);
    	msg.setData(data);
    	mSocketSender.sendMessage(msg);
    }
    public void sendMessagePostion(int value){
    	Message msg = new Message();
    	Bundle data = new Bundle();
    	msg.what = MESSAGE_SEND_DATA;
    	String status = "POSTION" + "\r\n" +
    			"Value:" + value + "\r\n";
    	data.putString(MSG_KEY_DATA, status);
    	msg.setData(data);
    	mSocketSender.sendMessage(msg);
    }
    public void sendMessageListener(String ip, int port){
    	Message msg = new Message();
    	Bundle data = new Bundle();
    	msg.what = MESSAGE_SEND_DATA;
    	String status = "P2PINFO" + "\r\n" +
    			"Host:" + ip + "\r\n" +
    			"Port:"+port + "\r\n";
    	Log.d(TAG, "host " + ip + ":" + port);
    	data.putString(MSG_KEY_DATA, status);
    	msg.setData(data);
    	mSocketSender.sendMessage(msg);
    }
    class SocketSender extends Handler{
    	private static final int SOCKET_TIMEOUT = 5000;
		
    	private String mHost = null;
    	private int mDataPort = 0;
        private InputStream mInputstream;
        private OutputStream mOutputstream;
        public SocketSender(){
        }
        public SocketSender(Looper looper){
        	super(looper);
        }
        public void close(){
        }
        public void setupUDP(String ip, int dataPort){
        	mDataPort = dataPort;
        	mHost = ip;
        }

        public void sendData(String host, int port,byte[] data) throws IOException {
    		if (data == null) {
    			return;
    		}
    		Socket socket = new Socket();

        	try{
        		socket.bind(null);
        		socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
        		mInputstream = socket.getInputStream();
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
  
}
