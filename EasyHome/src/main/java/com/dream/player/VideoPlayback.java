package com.dream.player;

import com.dream.player.R;
import com.dream.player.common.PlayerContainer;
import com.dream.player.network.PlayerFeedback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.os.PowerManager;

public class VideoPlayback extends Activity  {
	public static final String       TAG                    = "VideoPlayer";
    public static final int VIDEO_START         = 0;
    public static final int NEXT_VIDEO          = 5;
    public static final int SHOW_LOADING        = 6;
    public static final int HIDE_LOADING        = 7;
    public static final int FORE_VIDEO          = 8;
    public static final int  STATE_PLAY          = 0;
    public static final int  STATE_PAUSE         = 1;
    public static final int  STATE_STOP          = 2;
    public static final int  DIALOG_VOLUME_ID    = 2;
    public static final int  DIALOG_EXIT_ID      = 3;
    public static final int  DIALOG_NEXT_ID      = 4;
    public static final int SHOW_STOP           = 1;
    
    private static final int HNALDE_HIDE_LOADING = 4;
	
	private SurfaceView mSurfaceView;
	private SeekBar skbProgress; 
	private Player player;
	private LoadingDialog	 progressDialog 	 = null;
    private PowerManager.WakeLock mWakeLock;
    private View mHideContainer;

	private View imgPlay; 
    private String mCurVideoPath;
    private int mReportPort = 0;
    private String mReportHost;
	long totalDuration;

	private PowerManager.WakeLock wakeLock     = null;

	private static boolean paused;
	private EasyPlayerReceiver mEasyPlayerReceiver;
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onStop() 
	{
		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}

		super.onStop();
	}


    private void showLoading() {
        if (progressDialog == null) {
            progressDialog = new LoadingDialog(this,
                    LoadingDialog.TYPE_LOADING, this.getResources().getString(
                            R.string.loading));
            progressDialog.show();
        }
    }
    
    private void hideLoading() {
        if (progressDialog != null) {
            progressDialog.stopAnim();
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

	
	private Handler handlerUI = new Handler() {
	      @Override
	      public void handleMessage(Message msg) {
	          switch (msg.what) {
	              case SHOW_STOP:
	                  //wait2Exit();
	                  return;
	              case VIDEO_START:
	                  start();
	                  return;
	              case HNALDE_HIDE_LOADING:
	                  hideLoading();
	                  return;
	              case NEXT_VIDEO:
	                  //next();
	                  return;
	              case SHOW_LOADING:
	                  showLoading();
	                  return;
	              case HIDE_LOADING:
	                  hideLoading();
	                  return;
	              case FORE_VIDEO:
	                  //before();
	                  return;
			      case Globals.MSG_SEEK_PLAY:
			    	  int pos = msg.getData().getInt(Globals.POS_EXTRA);
			    	  Log.d(TAG,"seek play " + pos);
			    	  player.seekTo(pos);
			    	  break;
			      case Globals.MSG_SET_PLAY:{
			    	  int state = msg.getData().getInt(Globals.STATE_EXTRA, 0);
					  setPlay(state);  
			      	  }
			    	  break;
			      case Globals.MSG_SET_VOICE_DOWN:
			    	  player.volumeDOWN();
			    	  break;
			      case Globals.MSG_SET_VOICE_UP:
			    	  player.volumeUP();
			    	  break;
			      case Globals.MSG_UPDATE_PLAY:{
			    	  String url = msg.getData().getString(Globals.URL_EXTRA);
			    	  int port = msg.getData().getInt(Globals.PORT_EXTRA);
			    	  String host = msg.getData().getString(Globals.IP_EXTRA);
			    	  updatePlay(url,port,host);
			      	}
			      	break;
	          }
	      }
	  };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG,"VideoPlayer onCreate");
		paused = false;

		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);		

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

	     setContentView(R.layout.video_player);

	     Intent intent = getIntent();

		Bundle extras = intent.getExtras();
		if (extras != null) {
			mCurVideoPath = extras.getString(Globals.VIDEO_FILENAME_EXTRA);
			mReportPort = extras.getInt(Globals.PORT_EXTRA);
			mReportHost = extras.getString(Globals.IP_EXTRA);
        	PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
        	if(playerFeedback != null){
        		playerFeedback.setupSender(mReportHost, mReportPort);
        	}
		}

		// Find the views whose visibility will change
		mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
		skbProgress = (SeekBar) this.findViewById(R.id.skbProgress); 
		skbProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
		player = new Player(mSurfaceView, skbProgress,handlerUI,this);
		handlerUI.sendEmptyMessageDelayed(VIDEO_START, 1000);
		mEasyPlayerReceiver = new EasyPlayerReceiver(this,handlerUI);
	}
    @SuppressWarnings("deprecation")
	@Override
    protected void onResume() {
        super.onResume();
        mEasyPlayerReceiver.registerReceiver();
        /* enable backlight */
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();
    }
    @Override
    protected void onPause() {
    	Log.d(TAG,"onPause");
        player.pause();
        player.stopPlayback();

        mEasyPlayerReceiver.unregisterReceiver();

        hideLoading();
        super.onPause();
        finish();
    }
    private void start() {
        Log.d(TAG, "*********************currentURI= " + mCurVideoPath);
        handlerUI.sendEmptyMessage(SHOW_LOADING);
        player.setUrl(mCurVideoPath);
    }

	
	class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
		int progress;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
			this.progress = progress * player.getDuration()
					/ seekBar.getMax();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			player.seekTo(progress);
		}
	}

	private void updatePlay(String url,int port,String host){
		if(Globals.isVideoFile(url)){
			Globals.setFileName(url);
			mCurVideoPath = url;
			mReportPort = port;
			mReportHost = host;
        	PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
        	if(playerFeedback != null){
        		playerFeedback.setupSender(mReportHost, mReportPort);
        	}
			handlerUI.sendEmptyMessageDelayed(VIDEO_START, 1000);
			//demoRenderer.nativePlayerForward();
		}
	}
    private void setPlay(int state){
		Log.d(TAG,"Up paused:" + paused);		  
		if(paused) {
			player.pause();
			((ImageView) imgPlay).setImageResource(R.drawable.pause_shadow);
		}
		else {
			player.play();
			((ImageView) imgPlay).setImageResource(R.drawable.play_shadow);
		}		        	
		paused = !paused;
    }

	OnClickListener mGoneListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			Log.d(TAG,"Inside mGone Click");
			if ((mHideContainer.getVisibility() == View.INVISIBLE) ||
					(mHideContainer.getVisibility() == View.GONE))
			{
				mHideContainer.setVisibility(View.VISIBLE);
			}	else 
			{
				mHideContainer.setVisibility(View.INVISIBLE);
			}
		}
	};

	OnClickListener mVisibleListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			if ((mHideContainer.getVisibility() == View.GONE) ||
					(mHideContainer.getVisibility() == View.INVISIBLE)) 
			{
				mHideContainer.setVisibility(View.VISIBLE);
			} else 
			{
				mHideContainer.setVisibility(View.INVISIBLE);
			}
		}
	};

}
