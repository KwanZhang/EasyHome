/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.soundcapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;


import com.android.soundcapture.mix.CheapMP3;
import com.android.soundcapture.mix.FileBuilderService;
import com.dream.share.Globals;
import com.dream.share.rtp.RtpSender;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Recorder {
	private static final String TAG = "Recorder";
	//private static final int SAMPLING_RATE = Globals.mSampleRate;
	//private static final int AUDIO_FORMAT = Globals.mAudioFormat;
	//private static final int CHANNEL_CONFIG = Globals.mChannelConfig;
    static final String SAMPLE_PREFIX = "recording";
    static final String SAMPLE_PATH_KEY = "sample_path";
    static final String SAMPLE_LENGTH_KEY = "sample_length";

    public static final int IDLE_STATE = 0;
    public static final int RECORDING_STATE = 1;
    public static final int PLAYING_STATE = 2;
    int mState = IDLE_STATE;
	// The interval in which the recorded samples are output to the file
	// Used only in uncompressed mode
    public static final int NO_ERROR = 0;
    public static final int SDCARD_ACCESS_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int IN_CALL_RECORD_ERROR = 3;
	// File writer (only in uncompressed mode)
	private String mCurPlayFile;
	private int				 mAudioBufferSize;
	private int				 mShortAudioBufferSize;
	private Thread mRecordAudioThread;
	private Thread mMixThread = null;
	private final static int DATA_LEN = 20000;
    private short[] mMusicData = new short[DATA_LEN];
    int mMusicHead = 0;
    int mMusicTail = 0;
    int mMusicDataLen = 0;
    private AudioRecord 	 aRecorder = null;
    private int mAudioSessionId;
	NoiseSuppressor mNoiseSuppressor = null;
	AcousticEchoCanceler mAcousticEchoCanceler = null;
	AutomaticGainControl mAutomaticGainControl = null;
	private Handler mHandler;
    public Recorder(Handler handler) {
    	mHandler = handler;
    }
    
    public void setPlayFile(String file){
    	mCurPlayFile = file;
    }
    

	public void start(){
		aRecorder.startRecording();
		mMixThread = new Thread(mSendMP3Data);
		mMixThread.start();

		mRecordAudioThread = new Thread(mRecordAudio);
		mRecordAudioThread.start();
	}
	/**
	 *  Stops the recording, and sets the state to STOPPED.
	 * In case of further usage, a reset is needed.
	 * Also finalizes the wave file in case of uncompressed recording.
	 */
	public void stopAudioRecord(){
		if(aRecorder != null){
			aRecorder.stop();
			mState = IDLE_STATE;
			try {
				mRecordAudioThread.join();
			} catch (InterruptedException e) {
				Log.e(TAG, e.toString());
			}
			try {
				mMixThread.join();
			} catch (InterruptedException e) {
				Log.e(TAG, e.toString());
			}
		}
			
	}
	/**
	 *  Releases the resources associated with this class, and removes the unnecessary files, 
	 *  when necessary
	 */
	public void release(){
		if (aRecorder != null){
			aRecorder.release();
		}
	}
	private void getMp3Buffer(short[] MP3Data){
		for (int i = 0; i <  MP3Data.length; i++){
			mMusicData[mMusicHead++] = MP3Data[i];
			if(DATA_LEN == mMusicHead){
				mMusicHead = 0;
			}
			mMusicDataLen++;
			while(mMusicDataLen == DATA_LEN){
				//Log.e(TAG,"#################3mMusicDataLen > DATA_LEN ##############3");
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
		}
	}

	private final Runnable mSendMP3Data = new Runnable(){
		@Override
		public void run() {
			//Open the mp3 file
			InputStream mp3Stream = null;
			int frameCount = 0;
			CheapMP3 mp3 = new CheapMP3();
			
			try {
				mp3.ReadFile(new File(mCurPlayFile));
				mp3Stream = new FileInputStream(mCurPlayFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return ;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			//Library used to get Raw PCM data from mp3 stream
			Decoder d = new Decoder();
			Bitstream stream = new Bitstream(mp3Stream);
			frameCount = mp3.getNumFrames();
			Log.d(TAG,"Getting mp3  sampleRate = "+mp3.getSampleRate() + "frameCount " + frameCount);
			
			//Begin getting PCM data from mp3 frames
			//The goal is to only collect frames we need to be added to eachother
			Log.d("MP3","getMP3PCM now " + System.currentTimeMillis());
			
			try {
				Header header = stream.readFrame();
				
				for ( int frame = 0; frame < frameCount; frame++){
					
					if(header == null)
						break;
								
					SampleBuffer decoderOutput = (SampleBuffer)d.decodeFrame(header, stream);
					short[] pcm = decoderOutput.getBuffer();
					getMp3Buffer(pcm);

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					stream.closeFrame();

					header = stream.readFrame();
				}
				stream.close();
			} catch (BitstreamException e) {
				e.printStackTrace();
			} catch (JavaLayerException e) {
				e.printStackTrace();
			}
			Log.d("MP3","getMP3PCM get now " + System.currentTimeMillis());
		}
	};
	private short[] mixMP3Data(short[] recData){
		while(mMusicDataLen < recData.length){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			continue;
		}
		short flow;
		for(int i = 0; i < recData.length; i++){
			flow = (short) ((recData[i]>>1));// + (mMusicData[mMusicTail]>>2));
			mMusicTail++;
			if(mMusicTail == DATA_LEN){
				mMusicTail = 0;
			}
			mMusicDataLen--;
            /*if(flow > 32767){
                flow = 32767;
            }
            else if (flow < -32768){
                flow = -32768;
            }*/
            recData[i] =  flow;
		}
		return recData;
	}
	
    public void SendAudio(short[] buffer) {
    	byte[] rawBytes;
        try {
            InetAddress addr = InetAddress.getByName("192.168.43.86");;
            DatagramSocket sock = new DatagramSocket();
            rawBytes = FileBuilderService.shortsToBytes(buffer);
            DatagramPacket pack = new DatagramPacket(rawBytes, rawBytes.length,
                    addr, 1234);
            sock.send(pack);
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "FileNotFoundException");
        } catch (SocketException se) {
            Log.e(TAG, "SocketException");
        } catch (UnknownHostException uhe) {
            Log.e(TAG, "UnknownHostException");
        } catch (IOException ie) {
            Log.e(TAG, "IOException");
        }
    }
 final Runnable mRecordAudio = new Runnable() {
		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			//short[] buffer = new short[mShortAudioBufferSize];
//			byte[] buffer = new byte[mAudioBufferSize];
			while (mState == RECORDING_STATE) {
				//short[] buffer = new short[mShortAudioBufferSize];
				byte[] buffer = new byte[mAudioBufferSize];
				int len = aRecorder.read(buffer, 0, buffer.length);
//				for(int i = 0; i < len; i++){
//					buffer[i] = (byte)(buffer[i]>>1);
//				}
				//buffer = mixMP3Data(buffer);
				//SendAudio(buffer);
				Message msg = new Message();
				msg.what = RtpSender.CMD_POLL_DATA;
				Bundle data = new Bundle();
				data.putByteArray("RecordData", buffer);
				data.putInt("len", len);
				msg.setData(data);
				mHandler.sendMessage(msg);
			}
			aRecorder.stop();
		}
	};
	public int getSampleRateInHz(){
		return Globals.mSampleRate;
	}
	public int getChannelConfig(){
		return Globals.mChannelConfig;
	}
	public int getAudioFormat(){
		return Globals.mAudioFormat;
	}
	public int getBufferSizeInBytes(){
		return AudioRecord.getMinBufferSize(Globals.mSampleRate, Globals.mChannelConfig,
				Globals.mAudioFormat);
	}
    public void startRecording(String extension) {
		mAudioBufferSize = AudioRecord.getMinBufferSize(Globals.mSampleRate, Globals.mChannelConfig,
				Globals.mAudioFormat);
		mShortAudioBufferSize = mAudioBufferSize/2;
		Log.d(TAG, "bufferSize " + mAudioBufferSize);
		try{
			aRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Globals.mSampleRate, 
					Globals.mChannelConfig, 
					Globals.mAudioFormat, mAudioBufferSize);
			
			if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
				throw new Exception("AudioRecord initialization failed");
			mAudioSessionId = aRecorder.getAudioSessionId();
		} catch (Exception e){
			Log.e(TAG, " create AudioRecord " + e.toString());
		}
		killNoise();
		setState(RECORDING_STATE);
		start();
    }
    
    public void stopRecording() {
    	if(aRecorder == null)
    		return;
    	releaseKillNoise();
		stopAudioRecord();
		release();
        setState(IDLE_STATE);
    }
    
    public void stop() {
        stopRecording();
    }
    
    private void setState(int state) {
        if (state == mState)
            return;
        
        mState = state;
    }
    
    class ReceiveBuffer{
    	public short buffer[];
    	public int length;
    };

	public void killNoise(){
		if (mAudioSessionId != 0 && android.os.Build.VERSION.SDK_INT >= 16){
			if (NoiseSuppressor.isAvailable()){
				if (mNoiseSuppressor != null){
					mNoiseSuppressor.release();
					mNoiseSuppressor = null;
				}
				
				mNoiseSuppressor = NoiseSuppressor.create(mAudioSessionId);
				if (mNoiseSuppressor != null) {
					mNoiseSuppressor.setEnabled(true);
				}
				else{
					Log.i(TAG, "Failed to create NoiseSuppressor.");								
				}
			}
			else{
				Log.i(TAG, "Doesn't support NoiseSuppressor");								
			}	
			
			if (AcousticEchoCanceler.isAvailable()){
				if (mAcousticEchoCanceler != null){
					mAcousticEchoCanceler.release();
					mAcousticEchoCanceler = null;
				}
				
				mAcousticEchoCanceler = AcousticEchoCanceler.create(mAudioSessionId);
				if (mAcousticEchoCanceler != null){
					mAcousticEchoCanceler.setEnabled(true);
					// mAcousticEchoCanceler.setControlStatusListener(listener)setEnableStatusListener(listener)
			    }
				else{
					Log.i(TAG, "Failed to initAEC.");	
					mAcousticEchoCanceler = null;
				}
			}
			else{
				Log.i(TAG, "Doesn't support AcousticEchoCanceler");								
			}

			if (AutomaticGainControl.isAvailable()){
				if (mAutomaticGainControl != null){
					mAutomaticGainControl.release();
					mAutomaticGainControl = null;
				}
				
				mAutomaticGainControl = AutomaticGainControl.create(mAudioSessionId);
				if (mAutomaticGainControl != null){
					mAutomaticGainControl.setEnabled(true);
				}
				else{
					Log.i(TAG, "Failed to create AutomaticGainControl.");								
				}
			}
			else{
				Log.i(TAG, "Doesn't support AutomaticGainControl");								
			}
		}
	}
	public void releaseKillNoise(){
		if (mAutomaticGainControl != null){
			mAutomaticGainControl.release();
			mAutomaticGainControl = null;
		}
		if (mAcousticEchoCanceler != null){
			mAcousticEchoCanceler.release();
			mAcousticEchoCanceler = null;
		}
		if (mNoiseSuppressor != null){
			mNoiseSuppressor.release();
			mNoiseSuppressor = null;
		}
	}
}
