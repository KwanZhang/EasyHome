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
import java.io.IOException;
import java.io.RandomAccessFile;

import com.android.soundcapture.mix.FileBuilderService;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class Player implements OnCompletionListener, OnErrorListener {
	private static final String TAG = "Player";
    static final String SAMPLE_PREFIX = "recording";
    static final String SAMPLE_PATH_KEY = "sample_path";
    static final String SAMPLE_LENGTH_KEY = "sample_length";

    public static final int IDLE_STATE = 0;
    public static final int RECORDING_STATE = 1;
    public static final int PLAYING_STATE = 2;
    
    int mState = IDLE_STATE;
    String mCurPlayFile;
	// The interval in which the recorded samples are output to the file
	// Used only in uncompressed mode
    public static final int NO_ERROR = 0;
    public static final int SDCARD_ACCESS_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int IN_CALL_RECORD_ERROR = 3;

	
    public interface OnStateChangedListener {
        public void onStateChanged(int state);
        public void onError(int error);
    }
    
    long mSampleStart = 0;       // time at which latest record or play operation started
    int mSampleLength = 0;      // length of current sample
    File mSampleFile = null;
    
    MediaPlayer mPlayer = null;

    public Player() {
    }
    
    public void saveState(Bundle recorderState) {
        recorderState.putString(SAMPLE_PATH_KEY, mSampleFile.getAbsolutePath());
        recorderState.putInt(SAMPLE_LENGTH_KEY, mSampleLength);
    }
    
    
    public int sampleLength() {
        return mSampleLength;
    }

    public File sampleFile() {
        return mSampleFile;
    }
    
    
    public void startPlayback(String filepath) {
    	stopPlayback();
    	mCurPlayFile = filepath;
    	
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(filepath);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IllegalArgumentException e) {
            mPlayer = null;
            return;
        } catch (IOException e) {
            mPlayer = null;
            return;
        }
        
        mSampleStart = System.currentTimeMillis();
        //setState(PLAYING_STATE);
    }
    public void stopPlayback() {
        if (mPlayer == null) // we were not in playback
            return;

        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
    }
    

    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
    }
}
