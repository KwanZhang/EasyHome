/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010, 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.dream.player;

import com.dream.player.R;
import com.dream.player.common.PlayerContainer;
import com.dream.player.network.PlayerFeedback;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;

public class AudioPlayback extends Activity implements OnPreparedListener,
        OnErrorListener, OnCompletionListener,OnInfoListener,OnBufferingUpdateListener {
	private final static String TAG = "AudioPlayblack";
	public static final String  TIME_START             = "00:00:00";
	public static final int     STATE_PLAY             = 0;
    public static final int     STATE_PAUSE            = 1;
    public static final int     STATE_STOP             = 2;
	private static final int SHOW_STOP = 3;
    private static final int SHOW_START = 2;
	private static final int STOP_AND_START = 4;
    private static final int SHOW_LOADING = 5;
    private static final int HIDE_LOADING = 6;
    private TextView mCurTime;
    private TextView totalTime;
    private TextView songtitle;
    private ImageView btn_play;
    private SeekBar mSeekBar;
    private View mAudioPanel;
    private TableLayout controlPanel;
	private LoadingDialog       progressDialog         = null;
	private static boolean paused;
	
	private MediaPlayer player;
	private int mTotalDuration;
	private AudioManager        mAudioManager;
    private int                 play_state             = STATE_PLAY;
    private String mCurAudioPath;
    private int mReportPort = 0;
    private String mReportHost;
    private PowerManager.WakeLock mWakeLock;
    private int mLastPos = 0;
    private EasyPlayerReceiver mEasyPlayerReceiver;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);		

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		setContentView(R.layout.audio_player);
		
		// Find the views whose visibility will change
		mSeekBar = (SeekBar) findViewById(R.id.progressbar);
		mCurTime = (TextView) findViewById(R.id.currenttime);
		mCurTime.setText(TIME_START);
		totalTime = (TextView) findViewById(R.id.totaltime);
		totalTime.setText(TIME_START);
		controlPanel = (TableLayout) findViewById(R.id.controlPanel);
		controlPanel.getBackground().setAlpha(55);
		mAudioPanel = findViewById(R.id.audioPanel);
		mAudioPanel.getBackground().setAlpha(55);
		songtitle = (TextView) findViewById(R.id.songtitle);

		Intent intent = getIntent();
		if(intent != null){
			Bundle extras = intent.getExtras();
			if (extras!= null) {
				mCurAudioPath = extras.getString(Globals.AUDIO_FILENAME_EXTRA);
				mReportPort = extras.getInt(Globals.PORT_EXTRA);
				mReportHost = extras.getString(Globals.IP_EXTRA);
            	PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
            	if(playerFeedback != null){
            		playerFeedback.setupSender(mReportHost, mReportPort);
            	}
            	
				Log.d(TAG,"Extras. get string audioFilename:" + mCurAudioPath);
				songtitle.setText(mCurAudioPath);
			}
		}
		mEasyPlayerReceiver = new EasyPlayerReceiver(this,handlerUI);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mSeekBar.setMax(1000);
	}

    private void setPlay(int state){
		Log.d(TAG,"Up paused:" + paused);		  
		if(paused) {
			player.pause();
		}
		else {
			player.start();
		}		        	
		paused = !paused;
    }

	private void updatePlay(String url,int port,String host){
		if (Globals.isAudioFile(url)) {
			mCurAudioPath = url;
			mReportPort = port;
			mReportHost = host;
        	PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
        	if(playerFeedback != null){
        		playerFeedback.setupSender(mReportHost, mReportPort);
        	}
			songtitle.setText(url);
			handlerUI.sendEmptyMessageDelayed(STOP_AND_START,1000);
			handlerUI.sendEmptyMessage(SHOW_LOADING);
		}
	}
	private void updatePlayPause() {
        if (btn_play != null) {
            if ((player != null) && player.isPlaying()) {
                btn_play.setImageResource(R.drawable.pause_shadow);
            } else {
                btn_play.setImageResource(R.drawable.play_shadow);
            }
        }
    }

	 private void start() {
        handlerUI.sendEmptyMessage(SHOW_LOADING);

        if (player == null) {
            player = new MediaPlayer();
			player.setOnPreparedListener(this);
			player.setOnCompletionListener(this);
			player.setOnErrorListener(this);
			player.setOnInfoListener(this);
			player.setOnBufferingUpdateListener(this);
			player.setLooping(false);
        }
		if(mCurAudioPath == null)
			return;
		
        try {
        	player.setDataSource (this, Uri.parse ( mCurAudioPath) );
        	player.prepareAsync();
        } catch (Exception ex) {
            Log.d(TAG, "Failed to open file: " + ex);
            exitNow();
            return;
        }
    }
    private void play() {
        mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC,
               AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        player.start();
        play_state = STATE_PLAY;
        //mProgressRefresher.postDelayed(new ProgressRefresher(), 500);
        updatePlayPause();
        
        PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
        if(playerFeedback != null)
        	playerFeedback.sendMessageStatus("Start");
    }
    
    private void pause() {
        player.pause();
        play_state = STATE_PAUSE;
        updatePlayPause();
    }
	private void stopAndStart(){
		if(player != null){
			player.pause();
			stopPlayback();
		}
		start();
	}
	
    public void exitNow(){
		Log.d(TAG,"ExitNow...");
		AudioPlayback.this.finish();
    }

	private void setMute(Boolean mute){
		Log.d(TAG, "*******setMute = " + mute);
		mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
	}

	private void volumeUP(){
		int mmax = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int current = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		int volume_level = current * 100 / mmax + 10;
		if(volume_level > 100)
			volume_level = 100;
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				volume_level * mmax / 100, AudioManager.FLAG_SHOW_UI);

	}
	
	private void volumeDOWN(){
		int mmax = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int current = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		int volume_level = current * 100 / mmax - 10;
		if(volume_level < 0)
			volume_level = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				volume_level * mmax / 100, AudioManager.FLAG_SHOW_UI);

	}
	private void seekPlay(int pos){
		Log.d(TAG, "pos " + pos + "mTotalDuration " + mTotalDuration);
		int seekPos = pos*mTotalDuration/100;
		mSeekBar.setProgress ( seekPos );
        mCurTime.setText ( timeFormatToString ( seekPos ) );
        
        player.seekTo(seekPos);
	}
	@Override
	public void onPrepared(MediaPlayer mp) {
		player = mp;
        hideLoading();
        play();		
		mTotalDuration = player.getDuration();
		Log.d(TAG, "mTotalDuration " + mTotalDuration);
        if ( mTotalDuration > 0 ) {
        	mSeekBar.setMax ( mTotalDuration );
        	mSeekBar.setVisibility ( View.VISIBLE );
            totalTime.setText ( timeFormatToString ( mTotalDuration ) );
        }
	}
	@Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "##########onError####################");
        handlerUI.removeMessages(SHOW_LOADING);
        stopPlayback();
		PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
        if(playerFeedback != null)
        	playerFeedback.sendMessageStatus("Stop");
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
    	Log.d(TAG, "##########onInfo####################");
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            handlerUI.sendEmptyMessageDelayed(SHOW_LOADING, 1000);
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            handlerUI.removeMessages(SHOW_LOADING);
            handlerUI.sendEmptyMessage(HIDE_LOADING);
        }
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "##########onCompletion####################");
    	if(play_state != STATE_STOP){
    		PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
            if(playerFeedback != null)
            	playerFeedback.sendMessageStatus("Stop");
            mSeekBar.setProgress(mTotalDuration);
            play_state = STATE_STOP;
            updatePlayPause();
    		stopPlayback();
    		handlerUI.sendEmptyMessage(SHOW_LOADING);	
    		
    	}
    }
    private String timeFormatToString ( int relTime ) {
        int time;
        StringBuffer timeBuf = new StringBuffer();
        relTime = ( int ) relTime / 1000;
        time = relTime / 3600;
        if ( time >= 10 ) {
            timeBuf.append ( time );
        } else {
            timeBuf.append ( "0" ).append ( time );
        }
        relTime = relTime % 3600;
        time = relTime / 60;
        if ( time >= 10 ) {
            timeBuf.append ( ":" ).append ( time );
        } else {
            timeBuf.append ( ":0" ).append ( time );
        }
        time = relTime % 60;
        if ( time >= 10 ) {
            timeBuf.append ( ":" ).append ( time );
        } else {
            timeBuf.append ( ":0" ).append ( time );
        }
        return timeBuf.toString();
    }
	private Handler handlerUI = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		  switch (msg.what) {
		      case SHOW_START:
		          start();
		          return;
		      case SHOW_STOP:
		          //wait2Exit();
		          break;
			  case STOP_AND_START:
			  	  stopAndStart();
			  	  break;
		      case SHOW_LOADING:
		          showLoading();
		          break;
		      case HIDE_LOADING:
		          hideLoading();
		          break;
		      case Globals.MSG_UPDATE_PLAY:{
		    	  String url = msg.getData().getString(Globals.URL_EXTRA);
		    	  int port = msg.getData().getInt(Globals.PORT_EXTRA);
		    	  String host = msg.getData().getString(Globals.IP_EXTRA);
		    	  updatePlay(url,port,host);
		      	}
		    	  break;
		      case Globals.MSG_SEEK_PLAY:
		    	  int pos = msg.getData().getInt(Globals.POS_EXTRA);
		    	  Log.d(TAG,"seek play " + pos);
		    	  seekPlay(pos);
		    	  break;
		      case Globals.MSG_SET_PLAY:{
		    	  int state = msg.getData().getInt(Globals.STATE_EXTRA, 0);
				  setPlay(state);  
		      	  }
		    	  break;
		      case Globals.MSG_SET_VOICE_DOWN:
		    	  volumeDOWN();
		    	  break;
		      case Globals.MSG_SET_VOICE_UP:
		    	  volumeUP();
		    	  break;
		  }
		}
	};
    private void stopPlayback() {
		play_state = STATE_STOP;
		mCurTime.setText(TIME_START);
		mSeekBar.setProgress(0);

        if ( player != null ) {
            //mPlayer.pause();
            try {
                //mPlayer.setDataSource(this,null);
            	player.release();
            } catch ( Exception ex ) {}
            finally {
            	player = null;
            }
			
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
           public void onAudioFocusChange(
                   int focusChange) {
               Log.d(TAG,
                       "##########onAudioFocusChange####################");
               if (player == null) {
                   mAudioManager
                           .abandonAudioFocus(this);
                   return;
               }
               switch (focusChange) {
                   case AudioManager.AUDIOFOCUS_LOSS:
                       //mPausedByTransientLossOfFocus = false;
                       //pause();
                       break;
                   case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                   case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                       /*if (player.isPlaying()) {
                           mPausedByTransientLossOfFocus = true;
                           pause();
                       }*/
                       break;
                   case AudioManager.AUDIOFOCUS_GAIN:
                       /*if (mPausedByTransientLossOfFocus) {
                           mPausedByTransientLossOfFocus = false;
                           play();
                       }*/
                       break;
               }
               updatePlayPause();
           }
       };

    private void showLoading() {
        if (progressDialog == null ) {
			//playDisable();
            progressDialog = new LoadingDialog(this,
                    LoadingDialog.TYPE_LOADING, this.getResources().getString(
                            R.string.loading));
            progressDialog.show();
        }
    }
    
    private void hideLoading() {
        if (progressDialog != null) {
			//playEnable();
            progressDialog.stopAnim();
            progressDialog.dismiss();
            progressDialog = null;
        }
    }


    @Override
    protected void onResume(){
        super.onResume();
        mEasyPlayerReceiver.registerReceiver();
        handlerUI.removeMessages ( SHOW_START );
		handlerUI.sendEmptyMessageDelayed(SHOW_START,500);
        /* enable backlight */
        PowerManager pm = ( PowerManager ) getSystemService ( Context.POWER_SERVICE );
        mWakeLock = pm.newWakeLock ( PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG );
        mWakeLock.acquire();
		Log.d(TAG, "onResume");
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
        hideLoading();
       	if(player != null){
        	player.pause();
        }
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        mEasyPlayerReceiver.unregisterReceiver();
        mWakeLock.release();
        // sendPlayStateChangeBroadcast(MediaRendererDevice.PLAY_STATE_PAUSED);
    }
    @Override
    protected void onStop() {
        if ( player != null ) {
            player.pause();
            stopPlayback();
        }
        Log.d ( TAG, "##########onStop####################" );
        super.onStop();
    }
    @Override
    protected void onDestroy(){
		super.onDestroy();
    }

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		//mSeekBar.setSecondaryProgress(percent);
		if ( mTotalDuration > 0 ) {
            int curPos = player.getCurrentPosition();
            if(curPos >= mTotalDuration){
            	Log.d(TAG,"curPos >= mTotalDuration play_state " + play_state);
            	if(play_state != STATE_STOP){
            		PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
                    if(playerFeedback != null)
                    	playerFeedback.sendMessageStatus("Stop");
                    mSeekBar.setProgress(mTotalDuration);
                    play_state = STATE_STOP;
                    updatePlayPause();
            		stopPlayback();
            		handlerUI.sendEmptyMessage(SHOW_LOADING);	
            		
            	}
        		return;
            }
            mSeekBar.setProgress ( curPos );
            mCurTime.setText ( timeFormatToString ( curPos ) );
            int currentProgress = curPos*100/mTotalDuration;
            Log.d(TAG, "curPos " + curPos + " currentProgress " + currentProgress + " mTotalDuration " + mTotalDuration);
    		if(currentProgress - mLastPos > 0){
                PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
                if(playerFeedback != null)
                	playerFeedback.sendMessagePostion(currentProgress);
    		}
    		mLastPos = currentProgress;
            
        } else {
            mTotalDuration = player.getDuration();
            if ( mTotalDuration > 0 ) {
            	mSeekBar.setMax ( mTotalDuration );
            	mSeekBar.setVisibility ( View.VISIBLE );
                totalTime.setText ( timeFormatToString ( mTotalDuration ) );
            }
                Log.d ( TAG, "****ProgressRefresher: mTotalDuration="
                          + mTotalDuration + ",   player=" + player );
        }

	}
}
