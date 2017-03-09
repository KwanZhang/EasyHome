package com.dream.player;


import java.io.IOException;

import com.dream.player.common.PlayerContainer;
import com.dream.player.network.PlayerFeedback;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class Player implements OnBufferingUpdateListener,
		OnCompletionListener, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnInfoListener, SurfaceHolder.Callback {
	private final static String TAG = "Player";
	    // all possible internal states
    public static final int STATE_ERROR              = -1;
    public static final int STATE_IDLE               = 0;
    public static final int STATE_PREPARING          = 1;
    public static final int STATE_PREPARED           = 2;
    public static final int STATE_PLAYING            = 3;
    public static final int STATE_PAUSED             = 4;
    public static final int STATE_PLAYBACK_COMPLETED = 5;
	
	private int mCurrentState = STATE_IDLE;
	private int videoWidth;
	private int videoHeight;
	public MediaPlayer mMediaPlayer;
	private SurfaceHolder surfaceHolder;
	private SeekBar skbProgress;
	private Handler mHandler;
	private AudioManager     mAudioManager;
    private boolean          mPausedByTransientLossOfFocus;
    private int         mDuration;
    private int mLastPos = 0;
    private Context mContext;
	//private DownloadPlayer mDownloadPlayer;
	@SuppressWarnings("deprecation")
	public Player(SurfaceView surfaceView,SeekBar skbProgress, Handler handler,Context context)
	{
		mContext = context;
		this.skbProgress=skbProgress;
		surfaceHolder=surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHandler = handler;
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		mCurrentState = STATE_IDLE;
		mDuration = -1;
	}
	
	
	//*****************************************************
    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
        }
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }
	public void play()
	{
		mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		mMediaPlayer.start();
	}
	
	public void setUrl(String videoUrl)
	{
		Log.d(TAG, "video url " + videoUrl);
		try {
			mMediaPlayer.reset();
			mMediaPlayer.setDataSource(videoUrl);
			
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnInfoListener(this);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.prepareAsync();
			mCurrentState = STATE_PREPARING;
			//mMediaPlayer.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
				mAudioManager.abandonAudioFocus(mAudioFocusListener);
            }
        }
    }
	
	public void release()
	{
		if (mMediaPlayer != null) { 
            mMediaPlayer.reset();
            mMediaPlayer.release(); 
            mMediaPlayer = null; 
        } 
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.d(TAG, "surface changed");
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		Log.d(TAG, "mMediaPlayer surfaceChanged");
		try {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDisplay(surfaceHolder);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnPreparedListener(this);
		} catch (Exception e) {
			Log.e(TAG, "error", e);
		}
		Log.d(TAG, "surface created");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		Log.d(TAG, "surface destroyed");
	}

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            mHandler.sendEmptyMessageDelayed(VideoPlayback.SHOW_LOADING, 1000);
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
        	mHandler.removeMessages(VideoPlayback.SHOW_LOADING);
        	mHandler.sendEmptyMessage(VideoPlayback.HIDE_LOADING);

        }
        return false;
    }
	@Override
	/**
	 * 通过onPrepared播放
	 */
	public void onPrepared(MediaPlayer player) {
		videoWidth = mMediaPlayer.getVideoWidth();
		videoHeight = mMediaPlayer.getVideoHeight();
		
		if (videoHeight != 0 && videoWidth != 0) {
			LinearLayout linearLayout = (LinearLayout) ((Activity) mContext).findViewById(R.id.video_layout);
			SurfaceView surfaceView = (SurfaceView) ((Activity) mContext).findViewById(R.id.surfaceView);
            int lw = linearLayout.getWidth();
            int lh = linearLayout.getHeight();
            Log.d(TAG,"lw " + lw + " lh " + lh);
            Log.d(TAG, "videoWidth " + videoWidth + " videoHeight " + videoHeight);
            if (videoWidth > lw || videoHeight > lh) {
                // 如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
                float wRatio = (float) videoWidth / (float) lw;
                float hRatio = (float) videoHeight / (float) lh;

                // 选择大的一个进行缩放
                float ratio = Math.max(wRatio, hRatio);
                videoWidth = (int) Math.ceil((float) videoWidth / ratio);
                videoHeight = (int) Math.ceil((float) videoHeight / ratio);
                Log.d(TAG, "videoWidth " + videoWidth + " videoHeight " + videoHeight);
                // 设置surfaceView的布局参数
                LinearLayout.LayoutParams lp= new LinearLayout.LayoutParams(videoWidth, videoHeight);
                int left = (lw - videoWidth)/2;
                int right = left ;
                int top = (lh - videoHeight)/2;
                int bottom = top;
                lp.setMargins(left, top, right, bottom);
                lp.gravity = Gravity.CENTER_VERTICAL;
                
                surfaceView.setLayoutParams(lp);
            }else {
            	// 如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
                /*float wRatio = (float) videoWidth / (float) lw;
                float hRatio = (float) videoHeight / (float) lh;

                // 选择大的一个进行缩放
                float ratio = Math.max(wRatio, hRatio);
                videoWidth = (int) Math.ceil((float) videoWidth / ratio);
                videoHeight = (int) Math.ceil((float) videoHeight / ratio);*/
                LinearLayout.LayoutParams lp= new LinearLayout.LayoutParams(lw, lh);
                lp.gravity = Gravity.CENTER_VERTICAL;
                //lp.gravity = Gravity.CENTER;
                surfaceView.setLayoutParams(lp);
            } 
			mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			player.start();
	        PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
	        if(playerFeedback != null)
	        	playerFeedback.sendMessageStatus("Start");
			mCurrentState = STATE_PLAYING;
		}
		mHandler.sendEmptyMessageDelayed ( VideoPlayback.HIDE_LOADING, 500 );
		Log.d(TAG, "onPrepared");
	}
	public void setMute(Boolean mute){
		Log.d(TAG, "*******setMute = " + mute);
		mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
	}

	public void volumeUP(){
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
	
	public void volumeDOWN(){
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
	public void seekTo(int pos) {
        if (isInPlaybackState()) {
        	int seekPos = pos*mMediaPlayer.getDuration()/100;
            mMediaPlayer.seekTo(seekPos);
        } else {
        }
    }
    public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0) {
                return mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }
	
	public boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
	       public void onAudioFocusChange(
	               int focusChange) {
	           Log.d(TAG, "##########onAudioFocusChange####################");
	           switch (focusChange) {
	               case AudioManager.AUDIOFOCUS_LOSS:
	                   mPausedByTransientLossOfFocus = false;
	                   pause();
	                   break;
	               case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	               case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	                   if (mMediaPlayer.isPlaying()) {
	                       mPausedByTransientLossOfFocus = true;
	                       pause();
	                   }
	                   break;
	               case AudioManager.AUDIOFOCUS_GAIN:
	                   if (mPausedByTransientLossOfFocus) {
	                       mPausedByTransientLossOfFocus = false;
	                       play();
	                   }
	                   break;
	           }
	       }
	   };

	@Override
	public void onCompletion(MediaPlayer arg0) {
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
		mHandler.sendEmptyMessage(VideoPlayback.SHOW_STOP);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int bufferingProgress) {
		skbProgress.setSecondaryProgress(bufferingProgress);
		int currentProgress=skbProgress.getMax()*mMediaPlayer.getCurrentPosition()/mMediaPlayer.getDuration();
		Log.d(TAG,"currentProgress "+  currentProgress +"%"  +", " +bufferingProgress + "% buffer");
		
		if(currentProgress == 100){
	        PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
	        if(playerFeedback != null)
	        	playerFeedback.sendMessageStatus("Stop");
			return;
		}
		if(currentProgress - mLastPos > 0){
            PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
            if(playerFeedback != null)
            	playerFeedback.sendMessagePostion(currentProgress);
		}
		mLastPos = currentProgress;
	}

}
