package com.dream.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class EasyPlayerReceiver {
	public static final String TAG = "EasyPlayerReceiver";
	private Context mContext;
	private Handler mHandler;
	private IntentFilter mIntentFilter  = new IntentFilter();;
	EasyPlayerReceiver(Context context, Handler handler){
		mContext = context;
		mHandler = handler;
	}
	public void registerReceiver(){
		mIntentFilter.addAction(Globals.ACTION_PLAY_AUDIO);
		mIntentFilter.addAction(Globals.ACTION_PLAY_VIDEO);
		mIntentFilter.addAction(Globals.ACTION_SHOW_IMAGE);
		mIntentFilter.addAction(Globals.ACTION_START_KTV);
		
		mIntentFilter.addAction(Globals.ACTION_SET_PLAY);
		mIntentFilter.addAction(Globals.ACTION_SET_VOICE_DOWN);
		mIntentFilter.addAction(Globals.ACTION_SET_VOICE_UP);
		mIntentFilter.addAction(Globals.ACTION_SEEK_PLAY);
		mContext.registerReceiver(mMainReceiver,mIntentFilter);
	}
	public void unregisterReceiver(){
		mContext.unregisterReceiver(mMainReceiver);
	}
	public void startPlay(String url, int port,String host) {
		if(url == null)
			return;
		if (Globals.isAudioFile(url)) {
			Log.d(TAG, "audio file");
			Intent intent = new Intent(mContext, AudioPlayback.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(Globals.AUDIO_FILENAME_EXTRA, url);
			intent.putExtra(Globals.PORT_EXTRA, port);
			intent.putExtra(Globals.IP_EXTRA, host);
			mContext.startActivity(intent);
		} else if (Globals.isImageFile(url)){
    		Intent intent = new Intent(mContext, ImagePlayer.class);
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		intent.putExtra(Globals.PATH_EXTRA, url);
    		mContext.startActivity(intent);
		} else {
			Intent intent = new Intent(mContext, VideoPlayback.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(Globals.VIDEO_FILENAME_EXTRA, url);
			intent.putExtra(Globals.PORT_EXTRA, port);
			intent.putExtra(Globals.IP_EXTRA, host);
			mContext.startActivity(intent);
		}
	}
	private void showImage(String url, int width, int height){
		Intent intent = new Intent(mContext, ImagePlayer.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Globals.PATH_EXTRA, url);
		intent.putExtra(Globals.IMAGE_WIDTH, width);
		intent.putExtra(Globals.IMAGE_HEIGHT, height);
		mContext.startActivity(intent);
	}
	private void startKtv(String host, int port){
		Intent intent = new Intent(mContext,AudioTrackActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Globals.PORT_EXTRA, port);
		intent.putExtra(Globals.IP_EXTRA, host);
		mContext.startActivity(intent);
	}
	private final BroadcastReceiver mMainReceiver = new BroadcastReceiver() {
		@Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
			Log.d(TAG,"action is " + action.toString() + "cur class " + mContext.getClass().getName());
			if(Globals.ACTION_PLAY_AUDIO.equals(action)){
				String url = intent.getStringExtra(Globals.URL_EXTRA);
				int port = intent.getIntExtra(Globals.PORT_EXTRA, 0);
				String host = intent.getStringExtra(Globals.IP_EXTRA);
				if(mContext.getClass().getName().equals("com.dream.player.AudioPlayback")){
					Message msg = new Message();
					msg.what = Globals.MSG_UPDATE_PLAY;
					Bundle data = new Bundle();
					data.putString(Globals.URL_EXTRA, url);
					data.putInt(Globals.PORT_EXTRA, port);
					data.putString(Globals.IP_EXTRA, host);
					msg.setData(data);
					mHandler.sendMessage(msg);
				}else{
					startPlay(url,port,host);
				}
			}else if (Globals.ACTION_PLAY_VIDEO.equals(action)){
				String url = intent.getStringExtra(Globals.URL_EXTRA);
				int port = intent.getIntExtra(Globals.PORT_EXTRA, 0);
				String host = intent.getStringExtra(Globals.IP_EXTRA);
				if(mContext.getClass().getName().equals("com.dream.player.VideoPlayback")){
					Message msg = new Message();
					msg.what = Globals.MSG_UPDATE_PLAY;
					Bundle data = new Bundle();
					data.putString(Globals.URL_EXTRA, url);
					data.putInt(Globals.PORT_EXTRA, port);
					data.putString(Globals.IP_EXTRA, host);
					msg.setData(data);
					mHandler.sendMessage(msg);
				}else{
					startPlay(url,port,host);
				}
			}else if (Globals.ACTION_SHOW_IMAGE.equals(action)){
				String url = intent.getStringExtra(Globals.URL_EXTRA);
				int width = intent.getIntExtra(Globals.IMAGE_WIDTH, 0);
				int height = intent.getIntExtra(Globals.IMAGE_HEIGHT, 0);
				if(mContext.getClass().getName().equals("com.dream.player.ImagePlayer")){
					Message msg = new Message();
					msg.what = Globals.MSG_UPDATE_PLAY;
					Bundle data = new Bundle();
					data.putString(Globals.URL_EXTRA, url);
					data.putInt(Globals.IMAGE_WIDTH, width);
					data.putInt(Globals.IMAGE_HEIGHT, height);
					msg.setData(data);
					mHandler.sendMessage(msg);
				}else{
					showImage(url,width,height);
				}
			}else if (Globals.ACTION_START_KTV.equals(action)){
				String host = intent.getStringExtra(Globals.IP_EXTRA);
				int port = intent.getIntExtra(Globals.PORT_EXTRA, 0);
				if(mContext.getClass().getName().equals("com.dream.player.AudioTrackActivity")){
					Message msg = new Message();
					msg.what = Globals.MSG_UPDATE_PLAY;
					Bundle data = new Bundle();
					data.putString(Globals.IP_EXTRA, host);
					data.putInt(Globals.PORT_EXTRA, port);
					msg.setData(data);
					mHandler.sendMessage(msg);
				}else{
					startKtv(host,port);
				}
			}else if (Globals.ACTION_SET_PLAY.equals(action)){
				int state = intent.getIntExtra(Globals.STATE_EXTRA, 0);
				Message msg = new Message();
				msg.what = Globals.MSG_UPDATE_PLAY;
				Bundle data = new Bundle();
				data.putInt(Globals.STATE_EXTRA, state);
				msg.setData(data);
				mHandler.sendMessage(msg);
			}else if(Globals.ACTION_SET_VOICE_DOWN.equals(action)){
				mHandler.sendEmptyMessage(Globals.MSG_SET_VOICE_DOWN);
			}else if(Globals.ACTION_SET_VOICE_UP.equals(action)){
				mHandler.sendEmptyMessage(Globals.MSG_SET_VOICE_UP);
			}else if (Globals.ACTION_SEEK_PLAY.equals(action)){
				mHandler.removeMessages(Globals.MSG_SEEK_PLAY);
				int pos = intent.getIntExtra(Globals.POS_EXTRA, -1);
				
				Log.d(TAG, "Pos" + pos);
				if(pos < 0)
					return;
				Message msg = new Message();
				msg.what = Globals.MSG_SEEK_PLAY;
				Bundle data = new Bundle();
				data.putInt(Globals.POS_EXTRA, pos);
				msg.setData(data);
				mHandler.sendMessageDelayed(msg, 500);
			}
		}
	};

}
