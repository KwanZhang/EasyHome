/**
 * @Package com.amlogic.mediacenter
 * @Description Copyright (c) Inspur Group Co., Ltd. Unpublished Inspur Group
 *              Co., Ltd. Proprietary & Confidential This source code and the
 *              algorithms implemented therein constitute confidential
 *              information and may comprise trade secrets of Inspur or its
 *              associates, and any use thereof is subject to the terms and
 *              conditions of the Non-Disclosure Agreement pursuant to which
 *              this source code was originally received.
 */
package com.dream.player;


import com.dream.player.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.ComponentName;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import android.widget.ImageView;
import android.widget.TextView;
/**
 * @ClassName LoadingDialog
 * @Description TODO
 * @Date 2013-8-27
 * @Email
 * @Author
 * @Version V1.0
 */
public class LoadingDialog extends Dialog {
    public static final int   TYPE_LOADING    = 0;
    public static final int   TYPE_ERROR      = 1;
    public static final int   TYPE_INIT       = 2;
    public static final int   TYPE_EXIT_TIMER = 3;
    protected static final String TAG = "LoadingDialog";
  
    private static final int COUNT_TIMER = 1;
    private TextView          mTvShow         = null;
    private int mCountTime = 10;
    private AnimationDrawable anim_loading    = null;
    private int type = TYPE_LOADING;
    public LoadingDialog(Context context, int type, String mtitle) {
        super(context, R.style.theme_dialog_loading);
        WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.flags |= LayoutParams.FLAG_NOT_FOCUSABLE|LayoutParams.FLAG_NOT_TOUCHABLE;
        getWindow().setAttributes(params);
        this.setCanceledOnTouchOutside(true);
        if (type == TYPE_LOADING) {
            setContentView(R.layout.loading);
            TextView tx = (TextView) findViewById(R.id.tx);
            tx.setText(mtitle);
            ImageView img = (ImageView) findViewById(R.id.anim);
            anim_loading = (AnimationDrawable) img.getDrawable();
            anim_loading.start();
            // setCancelable(false);
        } else if (type == TYPE_ERROR) {
            setContentView(R.layout.dialog_error);
            mTvShow = (TextView) findViewById(R.id.tx);
            mTvShow.setText(mtitle);
        } else if (type == TYPE_EXIT_TIMER) {
            setContentView(R.layout.dialog_error);
            mTvShow = (TextView) findViewById(R.id.tx);
            mTvShow.setText(""+mCountTime);
            mHandler.sendEmptyMessageDelayed(COUNT_TIMER,1000);
        }
    }
        private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case COUNT_TIMER:
                    showTimer();
            }
        }
    };

    public String getTopActivity(Context cxt) {
        ActivityManager mactivitymanager = (ActivityManager)cxt.getSystemService(Activity.ACTIVITY_SERVICE);
        ComponentName cn = mactivitymanager.getRunningTasks(1).get(0).topActivity;
        return cn.getClassName();
    }
    
    public int getCountNum(){
        Log.d(TAG,"Type:"+type+" mCountTime:"+mCountTime);
        return mCountTime;
    }
    public void setCountNum(int num){
        mCountTime = num;
    }
    private void showTimer(){
        if(mCountTime==0){
            LoadingDialog.this.dismiss();
        }else{
            mCountTime--;
            mTvShow.setText(""+mCountTime);
            mHandler.sendEmptyMessageDelayed(COUNT_TIMER,1000);
        }
            
    }
    public void stopAnim() {
        if (anim_loading != null) {
            anim_loading.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}
