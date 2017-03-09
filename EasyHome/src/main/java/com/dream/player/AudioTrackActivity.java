package com.dream.player;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.ArrayList;

import com.dream.player.rtp.RtpReceiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;

import android.util.Log;

/**
 * AudioTrackActivity activity sends and recv audio data through udp
 */
public class AudioTrackActivity extends Activity {
	private static final String TAG = "AudioTrackActivity";
	private HandlerThread mPlayThread;
	private PlayerHandler mPlayHandler;
	private RtpReceiver mRtpReceiver;
	private MediaPlayer player;
	public static final int START_PLAY_PCM = 1;
	public static final int STOP_PLAY_PCM = 2;
	public static final int FILL_DATA = 3;
	public static final int RTSP_SETUP = 4;
	public static final int RTSP_PLAY = 5;
	public static final int RTSP_TEARDOWN = 6;
	private ArrayList<ReceiveBuffer> mReceiveLists = new ArrayList<ReceiveBuffer>();
	
	private EasyPlayerReceiver mEasyPlayerReceiver;
	private AudioTrack mAudioTrack;
	private PowerManager.WakeLock mWakeLock;
	private String mCurAudioPath;
    static final int SAMPLE_RATE = 44100;
    static final int AUDIO_PORT = 1234;
    static final int AUDIO_REC_PORT = 1235;
    static final int SAMPLE_INTERVAL = 20; // milliseconds
    static final int BUF_SIZE = 4096; //AudioTrack.getMinBufferSize(SAMPLE_RATE, 
    		//AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_player);
        mPlayThread = new HandlerThread("AudioTrackActivity");
        mPlayThread.start();
        mPlayHandler = new PlayerHandler(mPlayThread.getLooper());
        String host = null;
        int port = 0;
		Intent intent = getIntent();
		if(intent != null){
			Bundle extras = intent.getExtras();
			if (extras!= null) {
				port = extras.getInt(Globals.PORT_EXTRA);
				host = extras.getString(Globals.IP_EXTRA);
			}
		}
		Log.d(TAG,"host " + host + " port " + port);
        try {
			mRtpReceiver = new RtpReceiver(mPlayHandler,host,port);
		} catch (IOException e) {
			e.printStackTrace();
		}
        mEasyPlayerReceiver = new EasyPlayerReceiver(this,mPlayHandler);
        //RecvAudio();
    }
    @Override
    protected void onResume(){
        super.onResume();
        /* enable backlight */
        PowerManager pm = ( PowerManager ) getSystemService ( Context.POWER_SERVICE );
        mWakeLock = pm.newWakeLock ( PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG );
        mWakeLock.acquire();
        mEasyPlayerReceiver.registerReceiver();
		Log.d(TAG, "onResume");
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	stop();
        mWakeLock.release();
        mEasyPlayerReceiver.unregisterReceiver();
        // sendPlayStateChangeBroadcast(MediaRendererDevice.PLAY_STATE_PAUSED);
    }

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener(){

		@Override
		public void onPrepared(MediaPlayer mp) {
			player = mp;
			player.start();
		}
    };
	private void start() {
		if (player == null) {
		    player = new MediaPlayer();
			player.setOnPreparedListener(mPreparedListener);
			player.setLooping(false);
		}
		if(mCurAudioPath == null)
			return;
		
		try {
			player.setDataSource (this, Uri.parse ( mCurAudioPath) );
			player.prepareAsync();
		} catch (Exception ex) {
		    Log.d(TAG, "Failed to open file: " + ex);
		    return;
		}
	}
	private void stop(){
		mRtpReceiver.close();
		mRtpReceiver = null;
        if ( player != null ) {
            try {
            	player.release();
            } catch ( Exception ex ) {}
            finally {
            	player = null;
            }
        }
        mAudioTrack.stop();
        
	}
    public class PlayerHandler extends Handler{
        
		public PlayerHandler(){
            
        }
        public PlayerHandler(Looper looper){
            super(looper);
        }
        public void handleMessage(Message msg){
        	Log.d(TAG,"what " + msg.what);
            switch(msg.what){
                case START_PLAY_PCM:{
                	int SampleRate,ChannelConfig,AudioFormat,BufferSize,RecordVoice,MusicVoice;
                	SampleRate = msg.getData().getInt("SampleRate");
                	ChannelConfig = msg.getData().getInt("ChannelConfig");
                	AudioFormat = msg.getData().getInt("AudioFormat");
                	BufferSize = msg.getData().getInt("BufferSize");
                	mCurAudioPath = msg.getData().getString("Uri");
                	RecordVoice = msg.getData().getInt("RecordVoice");
                	MusicVoice = msg.getData().getInt("MusicVoice");
                	mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                			SampleRate, ChannelConfig,
                			AudioFormat, BufferSize,
                            AudioTrack.MODE_STREAM);
                	mAudioTrack.setStereoVolume(RecordVoice/10f, RecordVoice/10f);
                	
                	mAudioTrack.play();
                	if(mCurAudioPath != null){
                		start();
                		if(player != null){
                			player.setVolume(RecordVoice/10f, RecordVoice/10f);
                		}
                	}
                	//mPlayHandler.sendEmptyMessageDelayed(FILL_DATA, 50);
                }
                break;
                case FILL_DATA:
                	byte[] buffer =  mRtpReceiver.getData();
                	if(buffer != null)
                		mAudioTrack.write(buffer, 0, buffer.length);
                	/*if(mReceiveLists.size() > 0){
                		ReceiveBuffer receive = mReceiveLists.get(0);
                		if(receive != null){
                			byte[] buffer = mReceiveLists.get(0).buffer;
                    		int len = mReceiveLists.get(0).length;
                    		mAudioTrack.write(buffer, 0, len);
                    		mReceiveLists.get(0).buffer = null;
                    		mReceiveLists.remove(0);
                		}
                	}*/
                	//mPlayHandler.sendEmptyMessage(FILL_DATA);
                	//mPlayHandler.sendEmptyMessageDelayed(FILL_DATA,100);
                	break;
                case RTSP_PLAY:
                	mRtpReceiver.RTPPlay();
                	break;
                case RTSP_SETUP:
					try {
						mRtpReceiver.RTPSetup();
					} catch (IOException e) {
						e.printStackTrace();
					}
                	break;
                case RTSP_TEARDOWN:
                	mRtpReceiver.RTPTeardown();
                	break;
                case Globals.MSG_UPDATE_PLAY:{
                		stop();
                		String host = msg.getData().getString(Globals.IP_EXTRA);
                		int port = msg.getData().getInt(Globals.PORT_EXTRA);
                		try {
                			mRtpReceiver = new RtpReceiver(mPlayHandler,host,port);
                		} catch (IOException e) {
                			e.printStackTrace();
                		}
                	}
                	break;
            }
        }
    } 
    public void RecvAudio() {
        Thread thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Buffered size: " + BUF_SIZE);
                Log.e(TAG, "start recv thread, thread id: "
                        + Thread.currentThread().getId());
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE,
                        AudioTrack.MODE_STREAM);
            	mAudioTrack.play();
                //mPlayHandler.sendEmptyMessage(START_PLAY_PCM);
                try {
                    DatagramSocket sock = new DatagramSocket(AUDIO_PORT);
                    
                    while (true) {
                    	byte[] buf = new byte[BUF_SIZE];
                        DatagramPacket pack = new DatagramPacket(buf, BUF_SIZE);
                        sock.receive(pack);
                        Log.d(TAG, "get data on port " + AUDIO_PORT);
                        mAudioTrack.write(pack.getData(), 0, pack.getLength());
                        /*ReceiveBuffer receive = new ReceiveBuffer();
                        receive.buffer = buf;
                        receive.length = BUF_SIZE;
                        //Log.d(TAG,"receive.length " + receive.length);
                        mReceiveLists.add(receive);*/
                        //totalByteReceived += pack.getLength();
                    }
                } catch (SocketException se) {
                    Log.e(TAG, "SocketException: " + se.toString());
                } catch (IOException ie) {
                    Log.e(TAG, "IOException" + ie.toString());
                }
            } // end run
        });
        thrd.start();
    }
    class ReceiveBuffer{
    	public byte buffer[];
    	public int length;
    };
}
