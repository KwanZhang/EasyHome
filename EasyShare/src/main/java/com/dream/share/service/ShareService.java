package com.dream.share.service;

import java.io.IOException;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.cybergarage.net.HostInterface;

import com.dream.share.DLNAContainer;
import com.dream.share.DMCApplication;
import com.dream.share.Globals;
import com.dream.share.control.MasterControlPoint;
import com.dream.share.control.P2pControl;
import com.dream.share.stream.HttpServer;
import com.dream.share.stream.PlayerStatusListener;
import com.dream.share.util.NetUtil;

public class ShareService extends Service {
	private final static String TAG = "ShareService";

    private static final int START_DMC = 101;
    private static final int STOP_DMC = 102;
    private static final int START_SEARCH = 103;

    private static final int SHOW_IMAGE = 105;
    private static final int PLAY_NODE = 106;
    private static final int START_WIFI_DIRECT = 107;
    private static final int SET_STATE	= 108;
	private static final int SET_VOICE = 109;
	private static final int GET_VOICE = 110;
	private static final int SET_VOICE_UP = 111;
	private static final int SET_VOICE_DOWN = 112;
	private static final int REGISTER_STATUS_LISTNER = 113;
	private static final int CREATE_P2P_CONTROL = 114;
	private static final int CLEAR_P2P_CONTROL = 115;
	private static final int CREATE_P2P_FILE_SERVER = 116;
	private static final int SEEK_PLAYER = 117;
	private static final int START_KTV = 118;
	private static final int STOP_KTV = 118;
	private HandlerThread mDMCThread;
	private DmcHandler mHandler;
	private MasterControlPoint mCtrlPoint;
	private P2pControl mP2pControl;
	private HandlerThread mP2pControlThread;
	private Intent mWifiDirectService = null;
	private boolean isStart;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Dlna client onCreate");

        String addr = NetUtil.getHostAddr(this);
        if(addr != null)
        	HostInterface.setInterface(addr);
    	mCtrlPoint = new MasterControlPoint();
    	mCtrlPoint.setIP(addr);
    	try {
			HttpServer fileServer = new HttpServer(0);
			DMCApplication.getInstance().setFileServer(fileServer);
			fileServer.setIP(addr);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	isStart = false;
        mDMCThread = new HandlerThread("dmsThread");
        mDMCThread.start();
        mHandler = new DmcHandler(mDMCThread.getLooper());
        mHandler.sendEmptyMessage(START_DMC); 
		
		PlayerStatusListener playerStatus = new PlayerStatusListener(this);
    	playerStatus.init(addr);
    	playerStatus.start();
    	DMCApplication.getInstance().setPlayerStatusListener(playerStatus);
		
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Globals.ACTION_P2P_CONNECT);
        intentFilter.addAction(Globals.ACTION_P2P_DISCONNECT);
        intentFilter.addAction(Globals.ACTION_P2P_SETUP_TIMEOUT);
        intentFilter.addAction(Globals.ACTION_P2P_START);
        intentFilter.addAction(Globals.ACTION_P2P_STOP);
		//intentFilter.addAction(Globals.ACTION_P2P_INFO);
        registerReceiver(mNetStateReceiver,intentFilter);
        DLNAContainer.getInstance().setDLNAClient(this);
    }
    
    private final BroadcastReceiver mNetStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
			Log.d(TAG,"action is " + action.toString());
			if(ConnectivityManager.CONNECTIVITY_ACTION.equals(action)){
				ConnectivityManager mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);  
				NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();    
	            if(netInfo != null && netInfo.isAvailable()) {  
	            	if(netInfo.getType()==ConnectivityManager.TYPE_MOBILE)
	            		return;
	            	String addr = NetUtil.getHostAddr(ShareService.this);
	            	String beforeAddr = HostInterface.getInterface();
	            	if(addr.equals(beforeAddr))
	            		return;
	                if(addr != null)
	                	HostInterface.setInterface(addr);
	                mCtrlPoint.stop();
	                isStart = false;
	                mCtrlPoint = null;
	                DLNAContainer.getInstance().clear();
	            	mCtrlPoint = new MasterControlPoint();
	            	mCtrlPoint.setIP(addr);
	            	DMCApplication.getInstance().getFileServer().setIP(addr);
	            	mHandler.sendEmptyMessage(START_DMC);
	              } else {  
	             
	              }  
			} else if (Globals.ACTION_P2P_CONNECT.equals(action)){
            	String addr = NetUtil.getHostAddr(ShareService.this);
            	String beforeAddr = HostInterface.getInterface();
            	if(addr.equals(beforeAddr))
            		return;
                if(addr != null)
                	HostInterface.setInterface(addr);
                mCtrlPoint.stop();
                isStart = false;
                mCtrlPoint = null;
                DLNAContainer.getInstance().clear();
            	mCtrlPoint = new MasterControlPoint();
            	mCtrlPoint.setIP(addr);
            	DMCApplication.getInstance().getFileServer().setIP(addr);
            	mHandler.sendEmptyMessage(START_DMC);
				/*String ip = intent.getStringExtra(Globals.IP_EXTRA);
				Message msg = new Message();
				msg.what = CREATE_P2P_FILE_SERVER;
				Bundle data = new Bundle();
				data.putString(Globals.IP_EXTRA, ip);
				msg.setData(data);
				mHandler.sendMessage(msg);	*/
			}else if(Globals.ACTION_P2P_START.equals(action)){
				if(mWifiDirectService == null){
					Log.d(TAG,"create wifi p2p service");
		    		mWifiDirectService = new Intent(ShareService.this,WifiDirectService.class);
		    		startService(mWifiDirectService);
	    		}
			}else if (Globals.ACTION_P2P_STOP.equals(action)){
				if(mWifiDirectService !=null){
					stopService(mWifiDirectService);
					mWifiDirectService = null;
				}
			}else if(Globals.ACTION_P2P_INFO.equals(action)){
				String ip = intent.getStringExtra(Globals.IP_EXTRA);
				int port = intent.getIntExtra(Globals.PORT_EXTRA,0);
				Message msg = new Message();
				msg.what = CREATE_P2P_CONTROL;
				Bundle data = new Bundle();
				data.putString(Globals.IP_EXTRA, ip);
				data.putInt(Globals.PORT_EXTRA, port);
				msg.setData(data);
				mHandler.sendMessage(msg);	
			}else if(Globals.ACTION_P2P_DISCONNECT.equals(action)){
            	String addr = NetUtil.getHostAddr(ShareService.this);
            	String beforeAddr = HostInterface.getInterface();
            	if(addr.equals(beforeAddr))
            		return;
                if(addr != null)
                	HostInterface.setInterface(addr);
                mCtrlPoint.stop();
                isStart = false;
                mCtrlPoint = null;
                DLNAContainer.getInstance().clear();
            	mCtrlPoint = new MasterControlPoint();
            	mCtrlPoint.setIP(addr);
            	DMCApplication.getInstance().getFileServer().setIP(addr);
            	mHandler.sendEmptyMessage(START_DMC);
				//mHandler.sendEmptyMessage(CLEAR_P2P_CONTROL);
			}else if(Globals.ACTION_P2P_SETUP_TIMEOUT.equals(action)){
				Log.d(TAG,"ACTION_P2P_SETUP_TIMEOUT mWifiDirectService " + (mWifiDirectService != null));
				if(mWifiDirectService !=null){
					Boolean ret = stopService(mWifiDirectService);
					Log.d(TAG,"stopService " + ret);
					mWifiDirectService = null;
				}
			}
		}
    };
    
    public void onDestroy() {
    	unregisterReceiver(mNetStateReceiver);
    	mCtrlPoint.Off();
    	
    	mHandler.sendEmptyMessage(STOP_DMC);
		if(mWifiDirectService !=null){
			ShareService.this.stopService(mWifiDirectService);
		}
        Log.e(TAG, "Dlna Client onDestroy");
        super.onDestroy();
    }
    public void search(){
    	mHandler.sendEmptyMessage(START_SEARCH);
    }
    public void setIP(String ip){
    	mCtrlPoint.setIP(ip);
    }

	public void setVoice(int process){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt(Globals.VOICE_EXTRA,process);
		msg.what = SET_VOICE;
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}
	public void setVoiceUp(){
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.what = SET_VOICE_UP;
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}
	public void setVoiceDown(){
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.what = SET_VOICE_DOWN;
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}
	public void getVoice(int process){
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.what = GET_VOICE;
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}
	public void startKtv(int port){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt(Globals.PORT_EXTRA,port);
		data.putString(Globals.IP_EXTRA,HostInterface.getInterface());
		msg.what = START_KTV;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
	public void stopKtv(int port){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt(Globals.PORT_EXTRA,port);
		data.putString(Globals.IP_EXTRA,HostInterface.getInterface());
		msg.what = STOP_KTV;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
	public void playNode(String filepath){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString(Globals.PATH_EXTRA,filepath);
		msg.what = PLAY_NODE;
		msg.setData(data);
		mHandler.sendMessage(msg);
    }
	public void showImage(String filepath,int width, int heigth){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString(Globals.PATH_EXTRA,filepath);
		data.putInt(Globals.IMAGE_WIDTH, width);
		data.putInt(Globals.IMAGE_HEIGHT, heigth);
		msg.what = SHOW_IMAGE;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
	public void startWifiDirect(String mac){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString(Globals.MAC_EXTRA,mac);
		msg.what = START_WIFI_DIRECT;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
	public void setPlay(int state){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt(Globals.STATE_EXTRA, state);
		msg.what = SET_STATE;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
	public void registerStatusListner(int port){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt(Globals.PORT_EXTRA, port);
		msg.what = REGISTER_STATUS_LISTNER;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
	public void seekPlayer(int pos){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt(Globals.SEEK_EXTRA, pos);
		msg.what = SEEK_PLAYER;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
    class DmcHandler extends Handler{
        public DmcHandler(){
            
        }
        public DmcHandler(Looper looper){
            super(looper);
        }
        public void handleMessage(Message msg){
        	Log.d(TAG,"msg is " + msg.what);
            switch(msg.what){
                case START_DMC:
                	if(!isStart){
                		mCtrlPoint.start();
                		isStart = true;
                	}
                	break;
                case STOP_DMC:
                	if(isStart){
                		mCtrlPoint.stop();
                		isStart = false;
                	}
                    break;
                case START_SEARCH:
                	if(isStart)
                		mCtrlPoint.search();
                	else{
                		mCtrlPoint.start();
                		isStart = true;
                		mCtrlPoint.search();
                	}
                	break;
				case SHOW_IMAGE:{
					String filepath = msg.getData().getString(Globals.PATH_EXTRA);
					int width = msg.getData().getInt(Globals.IMAGE_WIDTH, 0);
					int height = msg.getData().getInt(Globals.IMAGE_HEIGHT, 0);
					if(mP2pControl != null)
						mP2pControl.showImage(filepath,width,height);
					else
						mCtrlPoint.showImage(filepath,width,height);
					}
					break;
                case PLAY_NODE:{
					String filepath = msg.getData().getString(Globals.PATH_EXTRA);
					if(mP2pControl != null)
						mP2pControl.playNode(filepath);
					else
                		mCtrlPoint.playNode(filepath);
					}
                	break;
                case START_WIFI_DIRECT:{
                	String mac = msg.getData().getString(Globals.MAC_EXTRA);
                		mCtrlPoint.startWifiDirect(mac);
                	}
                	break;
                case SET_STATE:{
                		int state = msg.getData().getInt(Globals.STATE_EXTRA);
						if(mP2pControl  != null)
							mP2pControl.setPlayStatus(state);
						else
                			mCtrlPoint.setPlayStatus(state);
                	}
                	break;
				case GET_VOICE:{
					mCtrlPoint.getVoice();
				}
				break;
				case SET_VOICE_DOWN:{
					if(mP2pControl != null)
						mP2pControl.setVoiceDown();
					else
						mCtrlPoint.setVoiceDown();
				}
				break;
				case SET_VOICE_UP:{
					if(mP2pControl != null)
						mP2pControl.setVoiceUp();
					else
						mCtrlPoint.setVoiceUp();
				}
				break;
				case REGISTER_STATUS_LISTNER:{
					int port = msg.getData().getInt(Globals.PORT_EXTRA);
					if(mP2pControl != null)
						mP2pControl.registerStatusListener(port);
					else
						mCtrlPoint.registerStatusListener(port);
				}
				break;
				case CREATE_P2P_FILE_SERVER:{    
					String ip = msg.getData().getString(Globals.IP_EXTRA);
					HttpServer fileServer = DMCApplication.getInstance().getFileServer();
					if(fileServer !=null)
						fileServer.setIP(ip);
				}
		    	break;
				case CREATE_P2P_CONTROL:{
					Globals.whatConnect = Globals.CONNECT_P2P;
					String ip = msg.getData().getString(Globals.IP_EXTRA);
					int port = msg.getData().getInt(Globals.PORT_EXTRA);
					if(mP2pControl == null){
						mP2pControlThread = new HandlerThread("P2pControlThread");
						mP2pControlThread.start();
						mP2pControl = new P2pControl(mP2pControlThread.getLooper());
					}
					mP2pControl.setupUDP(ip, port);
				}
				break;
				case CLEAR_P2P_CONTROL:{
					if(mWifiDirectService != null){
						stopService(mWifiDirectService);
						mWifiDirectService = null;
					}
					Globals.whatConnect = Globals.CONNECT_WIFI;
					if(mP2pControl != null){
						mP2pControl.close();
						mP2pControl = null;
						mP2pControlThread.quit();
						mP2pControlThread = null;
					}
				}
				break;
				case SEEK_PLAYER:{
					int pos = msg.getData().getInt(Globals.SEEK_EXTRA);
					if(mP2pControl != null)
						mP2pControl.seekTo(pos);
					else
						mCtrlPoint.seekTo(pos);
				}
				break;
				case START_KTV:{
					int port = msg.getData().getInt(Globals.PORT_EXTRA);
					String host = msg.getData().getString(Globals.IP_EXTRA);
					mCtrlPoint.startKtv(port,host);
				}
				break;
            }
        }
    } 

	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}
