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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.dream.player.R;
import com.dream.player.ScalingUtilities.ScalingLogic;
/**
 * @ClassName ImageFromUrl
 * @Description TODO
 * @Date 2013-8-27
 * @Email
 * @Author
 * @Version V1.0
 */
public class ImagePlayer extends Activity{
    private static final String  TAG                 = "ImagePlayer";
	private PowerManager.WakeLock mWakeLock;
    private Handler              mHandler;
    private DecodeBitmapTask     mDecodeBitmapTask;
    private Bitmap               myBitmap;
    private ImageView            mShowView;
    private static final float   TOPSIZE             = 2048.0f;
    private static final int     LOADING_URL_IMAG    = 1;
    private static final int     SHOW_BITMAP_URL     = 2;
    private static final int     SHOW_BITMAP_URL_CACHE     = 3;
    private static final int     SHOW_STOP           = 4;
    private static final int     SLID_SHOW           = 5;
    private static final int     SHOWPANEL           = 6;
    private static final int     HIDEPANEL           = 7;
    private static final int     STOP_BY_SEVER       = 8;
    private static final int     SLIDE_SHOW_INTERVAL = 5000;
    public static boolean        isShowingForehand;
    private static final int     SLIDE_UNSTATE       = 0;
    private static final int     SLIDE_START         = 1;
    private static final int     SLIDE_STOP          = 2;
    private static final int     TOP_LEVEL = 32;
    private int                  mSlideShow          = SLIDE_UNSTATE;
    //private int                  mSlideShowDirection = 0;
    private EasyPlayerReceiver mEasyPlayerReceiver;
    private double zoomCount = 1;
    private Drawable mDrawable;
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);		

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.image_player);
        mShowView = (ImageView) findViewById(R.id.imageview);
        mHandler = new DecodeHandler();
        mEasyPlayerReceiver = new EasyPlayerReceiver(this,mHandler);
        Intent intent = getIntent();
        if (intent != null) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString(Globals.PATH_EXTRA,intent.getStringExtra(Globals.PATH_EXTRA));
            bundle.putInt(Globals.IMAGE_WIDTH, intent.getIntExtra(Globals.IMAGE_WIDTH, 0));
            bundle.putInt(Globals.IMAGE_HEIGHT, intent.getIntExtra(Globals.IMAGE_HEIGHT, 0));
            msg.setData(bundle);
            msg.what = LOADING_URL_IMAG;
            mHandler.sendMessage(msg);
        }
        mEasyPlayerReceiver.registerReceiver();

    }

private void updateImage(String path,int width, int height){
    Message msg = new Message();
    Bundle bundle = new Bundle();
    bundle.putString(Globals.PATH_EXTRA,path);
    bundle.putInt(Globals.IMAGE_WIDTH, width);
    bundle.putInt(Globals.IMAGE_HEIGHT,height);
    msg.setData(bundle);
    msg.what = LOADING_URL_IMAG;
    mHandler.sendMessage(msg);
}
    
public void startPlay(String url,int port, String host) {
	if(url == null)
		return;
	if (Globals.isAudioFile(url)) {
		Log.d(TAG, "audio file");
		Intent intent = new Intent(this, AudioPlayback.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Globals.AUDIO_FILENAME_EXTRA, url);
		intent.putExtra(Globals.PORT_EXTRA, port);
		intent.putExtra(Globals.IP_EXTRA, host);
		startActivity(intent);

	} else if (Globals.isVideoFile(url)){
		Intent intent = new Intent(this, VideoPlayback.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Globals.VIDEO_FILENAME_EXTRA, url);
		intent.putExtra(Globals.PORT_EXTRA, port);
		intent.putExtra(Globals.IP_EXTRA, host);
		startActivity(intent);
	}
}
	private final BroadcastReceiver mImageReceiver = new BroadcastReceiver() {
		@Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
			Log.d(TAG,"action is " + action.toString());
			if(Globals.ACTION_PLAY_AUDIO.equals(action)){
				String url = intent.getStringExtra(Globals.URL_EXTRA);
				int port = intent.getIntExtra(Globals.PORT_EXTRA, 0);
				String  host = intent.getStringExtra(Globals.IP_EXTRA);
				startPlay(url,port,host);
			}else if (Globals.ACTION_PLAY_VIDEO.equals(action)){
				String url = intent.getStringExtra(Globals.URL_EXTRA);
				int port = intent.getIntExtra(Globals.PORT_EXTRA, 0);
				String  host = intent.getStringExtra(Globals.IP_EXTRA);
				startPlay(url,port,host);
			}else if (Globals.ACTION_SHOW_IMAGE.equals(action)){
				String url = intent.getStringExtra(Globals.URL_EXTRA);
				int width = intent.getIntExtra(Globals.IMAGE_WIDTH, 0);
				int height = intent.getIntExtra(Globals.IMAGE_HEIGHT, 0);
				updateImage(url,width, height);
			}
		}

	};
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "******keycode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {

                mHandler = null;
                hideLoading();
                ImagePlayer.this.finish();
            return true;
        } else if ((keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                && (mSlideShow == SLIDE_START)) {
            mHandler.sendEmptyMessage(SHOWPANEL);
            hideBar(3000);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    
    @Override
    protected void onDestroy() {
    	unregisterReceiver(mImageReceiver);
        super.onDestroy();
    }
    
    private void zoomIn(){
        Display display = getWindowManager().getDefaultDisplay();
        Rect rect = new Rect();
        display.getRectSize(rect);
        if(myBitmap == null){
            return;
        }

        double newCount = zoomCount;
        newCount = newCount*2;
        int newWidth = (int) (myBitmap.getWidth()*newCount);
        int newHeight = (int) (myBitmap.getHeight()*newCount);
        if(newCount < TOP_LEVEL || (newWidth <= rect.width() && newHeight <= rect.height())){
            zoomCount = newCount;
            Bitmap scaledBitmap;
            if(newWidth >= rect.width() || newHeight >= rect.height()){
                newWidth = newWidth>rect.width()?rect.width():newWidth;
                newHeight = newHeight>rect.height()?rect.height():newHeight;
                scaledBitmap = ScalingUtilities.createScaledBitmap(myBitmap, newWidth,
                                newHeight, ScalingLogic.CROP);
            }else{
                scaledBitmap = ScalingUtilities.createScaledBitmap(myBitmap, newWidth,
                        newHeight, ScalingLogic.FIT);
            }
            mShowView.setImageBitmap(scaledBitmap);
            scaledBitmap = null;
        }else{
        }
    }
    
    private void zoomOut(){
        Display display = getWindowManager().getDefaultDisplay();
        Rect rect = new Rect();
        display.getRectSize(rect);
        /*BitmapDrawable bitmapDrawable = (BitmapDrawable)mShowView.getDrawable();
        Bitmap bm = bitmapDrawable.getBitmap();*/
        if(myBitmap == null){
            return;
        }
        Log.d(TAG,"zoomCount:"+zoomCount+" Top_level:"+(1.0/TOP_LEVEL));
        double newCount = zoomCount/2;
        int newWidth = (int) (myBitmap.getWidth()*newCount);
        int newHeight = (int) (myBitmap.getHeight()*newCount);
        if(newCount > (1.0/TOP_LEVEL) || (newWidth > 16 && newHeight > 16)){

            zoomCount = newCount;
            Bitmap scaledBitmap = null;
            if(newCount == 1){
                mShowView.setImageBitmap(myBitmap);
            }else {
                if(newWidth > rect.width() || newHeight > rect.height()){
                    newWidth = newWidth>rect.width()?rect.width():newWidth;
                    newHeight = newHeight>rect.height()?rect.height():newHeight;
                    scaledBitmap = ScalingUtilities.createScaledBitmap(myBitmap, newWidth,
                            newHeight, ScalingLogic.CROP);
                }else{
                    scaledBitmap = ScalingUtilities.createScaledBitmap(myBitmap, newWidth,
                        newHeight, ScalingLogic.FIT);
                }
                mShowView.setImageBitmap(scaledBitmap);
            }
            scaledBitmap = null;
        }else {
            return;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        isShowingForehand = true;
        mHandler.removeMessages(STOP_BY_SEVER);
        mHandler.sendEmptyMessageDelayed(STOP_BY_SEVER,5000);
		/* enable backlight */
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
		mWakeLock.acquire();
    }
    
    private void hideBar(int timeout) {
        mHandler.removeMessages(HIDEPANEL);
        mHandler.sendEmptyMessageDelayed(HIDEPANEL, timeout);
    }
    
    
    @Override
    protected void onPause() {
        super.onPause();
        mEasyPlayerReceiver.unregisterReceiver();
        isShowingForehand = false;
        hideLoading();
        mSlideShow = SLIDE_STOP;
        
		if(mDecodeBitmapTask!=null){
        	mDecodeBitmapTask.stopThread();
		}
        mDecodeBitmapTask = null;
		mWakeLock.release();
    }
    
    
    class DecodeBitmapTask extends Thread {
        private boolean   stop = false;
        private String mPath;
        private int mWidth;
        private int mHeight;
        public DecodeBitmapTask(String path,int width, int height) {
            mPath = path;
            mWidth = width;
            mHeight = height;
        }
        public void setDecodeBitmapTask(String path) {
            mPath = path;
        }
        public void stopThread() {
            stop = true;
        }
        
        private int calculateInSampleSize(int width,int height, int reqWidth, int reqHeight) {
        	// 源图片的高度和宽度
        	Log.d(TAG, "height " + height + " width  " + width);
	        	int inSampleSize = 1;
	        	if (height > reqHeight || width > reqWidth) {
	        	// 计算出实际宽高和目标宽高的比率
	        	final int heightRatio = Math.round((float) height / (float) reqHeight);
	        	final int widthRatio = Math.round((float) width / (float) reqWidth);
	        	// 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
	        	// 一定都会大于等于目标的宽和高。
	        	inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        	}
        	return inSampleSize;
        	}
        
        @Override
        public void run() {
        	String url = mPath; //"http://" + mIP + ":" + mPort  + mPath;
        	Log.d(TAG, "load file from " + url);
        	
        	try{
        		URL myFile = new URL(url);
        		HttpURLConnection conn = (HttpURLConnection)myFile.openConnection();
        		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        		if((mWidth == 0) || (mHeight == 0)){
            		conn.setConnectTimeout(3000);
            		conn.setDoInput(true);
            		conn.setUseCaches(false);
            		InputStream in = conn.getInputStream();
            		//myBitmap = BitmapFactory.decodeStream(in);
                    
                    bmOptions.inJustDecodeBounds = true;
                    myBitmap = BitmapFactory.decodeStream(in,null,bmOptions);
                    conn.disconnect();
                    mWidth = bmOptions.outWidth;
                    mHeight = bmOptions.outHeight;
        		}

                int reqWidth = mShowView.getWidth();
                int reqHeight = mShowView.getHeight();
                Log.d(TAG, "reqWidth " + reqWidth + " reqHeight  " + reqHeight);
                bmOptions.inSampleSize =  calculateInSampleSize(mWidth,mHeight, reqWidth, reqHeight);
                conn = (HttpURLConnection)myFile.openConnection();
        		conn.setConnectTimeout(3000);
        		conn.setDoInput(true);
        		conn.setUseCaches(false);
        		InputStream in = conn.getInputStream();
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inPurgeable = true;	
                if(myBitmap != null){
                    myBitmap.recycle();
                    myBitmap = null;              	
                }

                myBitmap = BitmapFactory.decodeStream(in,null,bmOptions);
                conn.disconnect();
        	}catch(Exception e){
        		Log.e(TAG, "http connection err " + e.toString());
        	}
            Log.d(TAG," is myBitmap==null "+(myBitmap==null));
            if(mHandler != null){
                 mHandler.sendEmptyMessage(SHOW_BITMAP_URL);
            }
        }
    }

    private void showLoading() {}
    
    private void stopExit() {
        mHandler.removeMessages(SHOW_STOP);
    }
    
    /**
     * @Description TODO
     */
    public void wait2Exit() {
        Log.d(TAG, "wait2Exit......");
        if(!isShowingForehand){
            this.finish();
            return;
        }
        hideLoading();
    }
    
    private void hideLoading() {
    }
    
    class DecordUri {
        private String mDecodeUrl;
        
        public synchronized void setUrl(String url) {
            mDecodeUrl = url;
        }
        
        public synchronized String getUrl() {
            String url = mDecodeUrl;
            mDecodeUrl = null;
            return url;
        }
    }
    
    private void showImage() {
        if (mSlideShow == SLIDE_STOP) {
            hideLoading();
            return;
        }
        if (myBitmap != null) {
            int height = myBitmap.getHeight();
            int width = myBitmap.getWidth();
            float reSize = 1.0f;
            if (width > TOPSIZE || height > TOPSIZE) {
                if (height > width) {
                    reSize = TOPSIZE / height;
                } else {
                    reSize = TOPSIZE / width;
                }
                Matrix matrix = new Matrix();
                matrix.postScale(reSize, reSize);
                myBitmap = Bitmap.createBitmap(myBitmap, 0, 0, width,
                        height, matrix, true);
                Log.d(TAG,"shwo image1 ");
                mShowView.setImageBitmap(myBitmap);
            } else {
            	Log.d(TAG,"shwo image ");
            	mShowView.setImageBitmap(myBitmap);
            }
            //myBitmap = null;
        } else {
        }
        hideLoading();
        if (mSlideShow == SLIDE_START) {
            mHandler.sendEmptyMessageDelayed(SLID_SHOW, SLIDE_SHOW_INTERVAL);
        }
    }
    
    private void slideShow() {
        if (mSlideShow != SLIDE_START) {
            return;
        }
    }
    
    class DecodeHandler extends Handler {
        
        public DecodeHandler() {
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOADING_URL_IMAG:{
                    //stopExit();
                    showLoading();
                    Bundle data = msg.getData();
                    String path = data.getString(Globals.PATH_EXTRA);
                    int width = data.getInt(Globals.IMAGE_WIDTH, 0);
                    int height = data.getInt(Globals.IMAGE_HEIGHT, 0);
                    Log.d(TAG,"get msg LOADING_URL_IMAG "+path + "width " + width + " height " + height);
                    if(mDecodeBitmapTask == null) {
                        mDecodeBitmapTask = new DecodeBitmapTask(path,width,height);
                        mDecodeBitmapTask.start();
                    }else {
                    	mDecodeBitmapTask.interrupt();
                    	mDecodeBitmapTask = null;
                        mDecodeBitmapTask = new DecodeBitmapTask(path,width,height);
                        mDecodeBitmapTask.start();	
                    }
                }
                    break;
                case SHOW_BITMAP_URL:
                    hideLoading();
                    showImage();
                    break;
                case SHOW_BITMAP_URL_CACHE:
                	hideLoading();
                	mShowView.setImageDrawable(mDrawable);
                	if (mSlideShow == SLIDE_START) {
			            mHandler.sendEmptyMessageDelayed(SLID_SHOW, SLIDE_SHOW_INTERVAL);
			        }
                	break;
                case SHOW_STOP:
                    wait2Exit();
                    break;
                case SLID_SHOW:
                    slideShow();
                    break;
                 case STOP_BY_SEVER:
                      if(!isShowingForehand){
                          ImagePlayer.this.finish();
                        }else{
                          mHandler.sendEmptyMessageDelayed(STOP_BY_SEVER,5000);
                        }
                      break;
                 case Globals.MSG_UPDATE_PLAY:{
                	 String url = msg.getData().getString(Globals.URL_EXTRA);
                	 int width = msg.getData().getInt(Globals.IMAGE_WIDTH);
                	 int height = msg.getData().getInt(Globals.IMAGE_HEIGHT);
     				updateImage(url,width, height);
                 }
                break;
            }
        }
    }
    
}
