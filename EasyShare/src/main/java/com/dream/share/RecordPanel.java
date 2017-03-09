package com.dream.share;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.soundcapture.Recorder;
import com.android.soundcapture.sdBrowser;
import com.dream.share.R;
import com.dream.share.rtp.RtpSender;
import com.dream.share.util.CircularMusicProgressBar;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

public class RecordPanel extends SlidingFragmentActivity{
	private final static String TAG = "ControlPanel";
	public final static int STATE_PLAY = 1;
	public final static int STATE_PAUSE = 2;
	private ImageButton mPlay;
	private ImageButton mAdd;
	private ImageButton mSetting;
	private TextView mSongName;
	private TextView mArtist;
	private TFile mSelFile;
	private CircularMusicProgressBar progressBar;
	private Handler mHandler;
	private RtpSender mRtpSender;
	//private Recorder mRecorder;
	private int mState = Recorder.IDLE_STATE;
	private String mFilePath = null;
	private void initLeftMenu()
	{
		Fragment leftMenuFragment = new MenuLeftFragment();
		setBehindContentView(R.layout.left_menu_frame);
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.id_left_menu_frame, leftMenuFragment).commit();
		SlidingMenu menu = getSlidingMenu();
		menu.setMode(SlidingMenu.LEFT); 
		menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		menu.setShadowWidthRes(R.dimen.shadow_width);
		menu.setShadowDrawable(R.drawable.shadow);  
		
		menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		menu.setFadeDegree(0.35f);
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);		
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.record_panel);
		
		progressBar = (CircularMusicProgressBar) findViewById(R.id.bg_music);
		progressBar.setValue(100);
		mPlay = (ImageButton)findViewById(R.id.img_play);
		mPlay.setOnClickListener(mOnClickListener);
		mAdd = (ImageButton)findViewById(R.id.img_add);
		mAdd.setOnClickListener(mOnClickListener);
		mSetting = (ImageButton)findViewById(R.id.img_setting);
		mSetting.setOnClickListener(mOnClickListener);
		mSongName = (TextView)findViewById(R.id.song_name);
		mArtist = (TextView)findViewById(R.id.artist);
		
		mHandler = new Handler();
		mPlay.setImageResource(R.drawable.mic_btn);
		mRtpSender = new RtpSender(mHandler);
		//mRecorder = new Recorder(mHandler);
		initLeftMenu();
	}
	private Runnable mRefreshUI = new Runnable(){
		@Override
		public void run() {
			mSongName.setText(mSelFile.getFileName());
			mSongName.invalidate();
			mArtist.setText(mSelFile.getArtist());
			mArtist.invalidate();
			long songid = mSelFile.getSongId();
			long albumid = mSelFile.getAlbumId();
			Log.d(TAG, "songid " + songid + " " +  albumid);
			Bitmap bm = FileManager.getArtwork(RecordPanel.this,songid, albumid, true);
			progressBar.setImageBitmap(bm);
		}
		
	};
	@Override
	protected void onResume(){
		SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(this); 

		String sample_rate =  shp.getString(
				SettingsFragment.KEY_SAMPLERATE_LISTPREFERENCE, "44100");
		
		if(sample_rate.equals("48000")){
			Globals.mSampleRate = 48000;
		}else if(sample_rate.equals("44100")){
			Globals.mSampleRate = 44100;
		}else if(sample_rate.equals("22050")){
			Globals.mSampleRate = 22050;
		}
		
		if(shp.getString(SettingsFragment.KEY_CHANNELCONFIG_LISTPREFERENCE,"1").equals("1")){
			Globals.mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		}else{
			Globals.mChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
		}
		
		if(shp.getString(SettingsFragment.KEY_AUDIOFORMAT_LISTPREFERENCE,"16").equals("16")){
			Globals.mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
		}else {
			Globals.mAudioFormat = AudioFormat.ENCODING_PCM_8BIT;
		}
		Globals.mRecordVoice = Integer.parseInt(shp.getString(
				SettingsFragment.KEY_RECORDVOICE_LISTPREFERENCE,"1"));
		Globals.mMusicVoice = Integer.parseInt(shp.getString(
				SettingsFragment.KEY_MUSICVOICE_LISTPREFERENCE,"1"));
		Log.d(TAG, "mSampleRate " + Globals.mSampleRate + 
				" mChannelConfig " + Globals.mChannelConfig + 
				" mAudioFormat " + Globals.mAudioFormat +
				" mRecordVoice " + Globals.mRecordVoice +
				" mMusicVoice " + Globals.mMusicVoice);
		super.onResume();
	}
	@Override
	protected void onStop(){
		if(mState == Recorder.RECORDING_STATE){
			mState = Recorder.IDLE_STATE;
		}
		super.onStop();
	}
	public void selectSound(){ //Select which files should be used with each button
		Intent intent = new Intent(RecordPanel.this, sdBrowser.class);
		startActivityForResult(intent,1);
	}
	private void settingRecord(){
		Intent intent = new Intent(RecordPanel.this, SettingsFragment.class);
		startActivityForResult(intent,1);
	}
	@Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		if(data != null){
			mSelFile = (TFile) data.getSerializableExtra("file");
			mFilePath = mSelFile.getFilePath();
			if(mFilePath != null){
				mHandler.post(mRefreshUI);
			}
			mRtpSender.setMixFile(mFilePath);
			//mRecorder.setPlayFile(mFilePath);
			Log.d(TAG, "file " + mFilePath);
		}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
    /*
     * Make sure we're not recording music playing in the background, ask
     * the MediaPlaybackService to pause playback.
     */
    private void stopAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java, which
        // should be public, but isn't.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");

        sendBroadcast(i);
    }

	private OnClickListener mOnClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			switch(v.getId()){
				case R.id.img_play:
					if(mState == Recorder.IDLE_STATE){
						stopAudioPlayback();
	                    mPlay.setImageResource(R.drawable.micing_btn);
	                    int port = mRtpSender.getRtspPort();
	                    DLNAContainer.getInstance().getDLNAClient().startKtv(port);
	                    mState = Recorder.RECORDING_STATE;
					}else {
						mPlay.setImageResource(R.drawable.mic_btn);
						mRtpSender.close();
						mRtpSender = new RtpSender(mHandler);
						//mRtpSender = null;
						mState = Recorder.IDLE_STATE;
					}
					break;
				case R.id.img_add:
					selectSound();
					break;
				case R.id.img_setting:
					settingRecord();
					break;
			}
		}
	};
}

