/**
 * @Package com.amlogic.miracast
 * @Description Copyright (c) Inspur Group Co., Ltd. Unpublished Inspur Group
 *              Co., Ltd. Proprietary & Confidential This source code and the
 *              algorithms implemented therein constitute confidential
 *              information and may comprise trade secrets of Inspur or its
 *              associates, and any use thereof is subject to the terms and
 *              conditions of the Non-Disclosure Agreement pursuant to which
 *              this source code was originally received.
 */
package com.dream.player;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;

import android.util.Log;

import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.graphics.drawable.AnimationDrawable;

import com.dream.player.R;
import com.dream.player.dlna.DlnaService;
import com.xiaomi.market.sdk.XiaomiUpdateAgent;

public class MainActivity extends Activity {
    public static final String       TAG                    = "MainActivity";
    public static final boolean      DEBUG                  = true;
    private PowerManager.WakeLock    mWakeLock;
    private ImageView                mConnectAnimation;
	private Intent mDlnaService = null;
	private Handler mHandler = new Handler();
	private EasyPlayerReceiver mEasyPlayerReceiver;
    @Override
    public void onContentChanged() {
        super.onContentChanged();
    }
    
    /** register the BroadcastReceiver with the intent values to be matched */
    @SuppressWarnings("deprecation")
	@Override
    public void onResume() {
        super.onResume();
        /* enable backlight */
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();
        
        //mConnectAnimation = (ImageView) findViewById(R.id.show_connect);
        if (DEBUG)
            Log.d(TAG, "onResume() " + mConnectAnimation);
        mEasyPlayerReceiver.registerReceiver();
        resetData();
    }

    @Override
    public void onPause() {
        super.onPause();
        mEasyPlayerReceiver.unregisterReceiver();
        mWakeLock.release();
    }

    
    public void resetData() {
        mConnectAnimation.setBackgroundResource(R.drawable.wifi_connect);
    }

    public void setConnect() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XiaomiUpdateAgent.update(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.connect_layout);
        
        mConnectAnimation = (ImageView) findViewById(R.id.show_connect);
        
        if (DEBUG)
            Log.d(TAG, "onCreate() " + mConnectAnimation);
		if(mDlnaService == null){
        	Log.d(TAG, "start dlna device service");
        	mDlnaService = new Intent(this,DlnaService.class);
        	startService(mDlnaService);
        }
		mEasyPlayerReceiver = new EasyPlayerReceiver(this,mHandler);
    }
	
    @Override
    protected void onDestroy() {
    	if(mDlnaService != null){
    		stopService(mDlnaService);
    		mDlnaService = null;
    	}
        super.onDestroy();
    }

    @Override  
    public void onWindowFocusChanged(boolean hasFocus) {  
        super.onWindowFocusChanged(hasFocus);
        mConnectAnimation.setBackgroundResource(R.drawable.wifi_connect);  
        AnimationDrawable anim = (AnimationDrawable) mConnectAnimation.getBackground();  
        anim.start(); 
    }
}
