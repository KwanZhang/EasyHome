package com.dream.share;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dream.share.R;
import com.dream.share.DLNAContainer.PlayerChangeListener;
import com.dream.share.util.CircularMusicProgressBar;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

public class ControlPanel extends SlidingFragmentActivity{
	private final static String TAG = "ControlPanel";
	public final static int STATE_PLAY = 1;
	public final static int STATE_PAUSE = 2;
	private DMCApplication mApp ;
	private int mCurType;
	private ImageButton mBackward;
	private ImageButton mPlay;
	private ImageButton mForward;
	private ImageButton mRepeat;
	private TextView mSongName;
	private TextView mArtist;
	private CircularMusicProgressBar progressBar;
	private Handler mHandler = new Handler();
	private float mNextPos;
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
		setContentView(R.layout.control_panel);
		
		progressBar = (CircularMusicProgressBar) findViewById(R.id.album_art);
		progressBar.setValue(100);
		progressBar.setGestureListener(mOnGestureListener);
		mBackward = (ImageButton)findViewById(R.id.img_backward);
		mBackward.setOnClickListener(mOnClickListener);
		mPlay = (ImageButton)findViewById(R.id.img_play);
		mPlay.setOnClickListener(mOnClickListener);
		mForward = (ImageButton)findViewById(R.id.img_forward);
		mForward.setOnClickListener(mOnClickListener);
		mRepeat = (ImageButton)findViewById(R.id.img_repeat);
		mRepeat.setOnClickListener(mOnClickListener);
		mSongName = (TextView)findViewById(R.id.song_name);
		mArtist = (TextView)findViewById(R.id.artist);
		
		mApp = DMCApplication.getInstance();
		DLNAContainer.getInstance().setPlayerChangeListener(new PlayerChangeListener(){

			@Override
			public void onPlayerChangeListener(final int status, final int value) {
				Log.d(TAG, "onPlayerChangeListener");
				ControlPanel.this.runOnUiThread(new Runnable() {
					public void run() {
						Log.d(TAG, "status " + status + " value " + value);
						refresh(status,value);
					}
				});
			}
		});
		initLeftMenu();
	}
	CircularMusicProgressBar.OnGestureListener mOnGestureListener = 
			new CircularMusicProgressBar.OnGestureListener(){

				@Override
				public void onLongPress() {
					Log.d(TAG,"onLongPress");
					progressBar.setBorderProgressColor(Color.YELLOW);
					mNextPos = progressBar.getValue();
				}

				@Override
				public void onRelease() {
					Log.d(TAG,"onRelease");
					progressBar.setBorderProgressColor(0xFF80CBC4);
				}

				@Override
				public void onMoveLeft() {
					Log.d(TAG,"onMoveLeft");
					if(mNextPos - 1 > 0){
						//progressBar.setValueDirect(value - 1);
						mNextPos -= 1;
						mHandler.removeCallbacks(mSetSeekVaule);
						mHandler.postDelayed(mSetSeekVaule, 1500);
					}
				}

				@Override
				public void onMoveRight() {
					Log.d(TAG,"onMoveRight");
					if(mNextPos + 1 < 100){
						//progressBar.setValueDirect(value + 1);
						mNextPos += 1;
						mHandler.removeCallbacks(mSetSeekVaule);
						mHandler.postDelayed(mSetSeekVaule, 1500);
					}
				}
		
	};
	Runnable mSetSeekVaule = new Runnable(){
		@Override
		public void run() {
			progressBar.setValue(mNextPos);
			DLNAContainer.getInstance().getDLNAClient().seekPlayer((int)mNextPos);
		}
	};	
	private void refresh(int status,int value){
		switch(status){
		case Globals.PLAYER_STATUS_PLAY:
			mPlay.setImageResource(R.drawable.ic_play_circle_normal_o);
			break;
		case Globals.PLAYER_STATUS_PAUSE:
			mPlay.setImageResource(R.drawable.ic_pause_circle_normal_o);
			break;
		case Globals.PLAYER_STATUS_STOP:
			mPlay.setImageResource(R.drawable.ic_play_circle_normal_o);
			break;
		case Globals.PLAYER_POSTION_UPDATE:
			progressBar.setValue(value);
			break;
		}
	}
	@Override
	protected void onResume(){
		super.onResume();
		initDiffView();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) { 
	      //do something here
	    	Log.d(TAG, "KEYCODE_VOLUME_DOWN");
	    	DLNAContainer.getInstance().getDLNAClient().setVoiceDown();
	        return true;
	    }else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
	    	Log.d(TAG, "KEYCODE_VOLUME_DOWN");
	    	DLNAContainer.getInstance().getDLNAClient().setVoiceUp();
	    	return true;
	    }

	    return super.onKeyDown(keyCode, event);
	}

	
	private void initDiffView(){
		mCurType = mApp.getCurrentType();
		if(mCurType == DMCApplication.PHOTO_TYPE){
			List<TFile> data = mApp.getCurrentData();
			int size = data.size();
			int pos = mApp.getCurrentPos();
			Log.d(TAG, "size " + size + " pos " + pos);
			if(size <= 1)
				return;
			TFile bxfile = data.get(pos);

			Display display = getWindowManager().getDefaultDisplay();
			//final float scale = getResources().getDisplayMetrics().density; 
			int height = display.getWidth(); 
			int width = display.getWidth();
			Log.d(TAG, "height " + height + " width " + width);
			Drawable drawable = null;
			try {
				drawable = decodeFile(bxfile.getFilePath(),height,width);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if(drawable != null){
				progressBar.setImageDrawable(drawable);
			}
			
		}else if(mCurType == DMCApplication.MUSIC_TYPE){
			List<TFile> data = mApp.getCurrentData();
			int size = data.size();
			int pos = mApp.getCurrentPos();
			Log.d(TAG, "size " + size + " pos " + pos);
			if(size <= 1)
				return;
			TFile bxfile = data.get(pos);
			long songid = bxfile.getSongId();
			long albumid = bxfile.getAlbumId();
			Log.d(TAG, "songid " + songid + " " +  albumid);
			Bitmap bm = FileManager.getArtwork(this,songid, albumid, true);
			progressBar.setImageBitmap(bm);
			Log.d(TAG, "filename " + bxfile.getFileName() + " artist " + bxfile.getArtist());
			mSongName.setText(bxfile.getFileName());
			mArtist.setText(bxfile.getArtist());
		}else if(mCurType == DMCApplication.VIDEO_TYPE){
			List<TFile> data = mApp.getCurrentData();
			int size = data.size();
			int pos = mApp.getCurrentPos();
			Log.d(TAG, "size " + size + " pos " + pos);
			if(size == 0)
				return;
			TFile bxfile = data.get(pos);
			mSongName.setText(bxfile.getFileName());
		}
	}

	private Drawable decodeFile(String path , int HSize, int WSize) throws FileNotFoundException {
		// Decode image size
		File f = new File(path);
		if(!f.exists())
			return null;
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(new FileInputStream(f), null, o);

		// The new size we want to scale to

		// Find the correct scale value. It should be the power of 2.
		int wscale = 1;
		while (o.outWidth / wscale / 2 >= WSize)
			wscale *= 2;
		
		int hscale = 1;
		while (o.outHeight / hscale / 2 >= HSize)
			hscale *= 2;
		int scale = wscale > hscale ? wscale : hscale;
		
		// Decode with inSampleSize
//		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o.inJustDecodeBounds = false;
		o.inSampleSize = scale;
		//Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(f),
		//		null, o);
		Bitmap bitmap = BitmapFactory.decodeFile(path);
		Log.d(TAG, "height " + o.outHeight + " width " + o.outWidth + " scale " + scale);
		o = null;
		return new BitmapDrawable(bitmap);
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void doBackward(){
		mCurType = mApp.getCurrentType();
		List<TFile> data = mApp.getCurrentData();
		int size = data.size();
		int pos = mApp.getCurrentPos();
		Log.d(TAG, "size " + size + " pos " + pos);
		if(size <= 1)
			return;
		pos -= 1;
		if (pos < 0)
			pos = size - 1;
		TFile bxfile = data.get(pos);
		mApp.setCurrentPos(pos);
		if(mCurType == DMCApplication.MUSIC_TYPE){
			DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		}else if(mCurType == DMCApplication.VIDEO_TYPE){
			DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		}else if(mCurType == DMCApplication.PHOTO_TYPE){
			DLNAContainer.getInstance().getDLNAClient().showImage(bxfile.getFilePath(),
									bxfile.getWidth(), bxfile.getHeight());
		}
	}

	public static void doForward(){
		int curType = DMCApplication.getInstance().getCurrentType();
		List<TFile> data = DMCApplication.getInstance().getCurrentData();
		int size = data.size();
		int pos = DMCApplication.getInstance().getCurrentPos();
		Log.d(TAG, "size " + size + " pos " + pos);
		if(size <= 1)
			return;
		
		pos += 1;
		
		if (pos == size)
			pos = 0;
		
		TFile bxfile = data.get(pos);
		DMCApplication.getInstance().setCurrentPos(pos);
		if(curType == DMCApplication.MUSIC_TYPE){
			DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		}else if(curType == DMCApplication.VIDEO_TYPE){
			DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		}else if(curType == DMCApplication.PHOTO_TYPE){
			DLNAContainer.getInstance().getDLNAClient().showImage(bxfile.getFilePath(),
					bxfile.getWidth(), bxfile.getHeight());
		}
	}
	public static void doStop(){
		int curType = DMCApplication.getInstance().getCurrentType();
		List<TFile> data = DMCApplication.getInstance().getCurrentData();
		int size = data.size();
		int pos = DMCApplication.getInstance().getCurrentPos();
		Log.d(TAG, "size " + size + " pos " + pos);
		if(size == 0)
			return;
		
		TFile bxfile = null;
		if(curType == DMCApplication.MUSIC_TYPE){
			if(Globals.musicPlayMode == Globals.PLAY_ALL){
				pos += 1;
				if (pos == size)
					pos = 0;
				bxfile = data.get(pos);
			}else{
				bxfile = data.get(pos);
			}
			DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		}else if(curType == DMCApplication.VIDEO_TYPE){
			if(Globals.videoPlayMode == Globals.PLAY_ALL){
				pos += 1;
				if (pos == size)
					pos = 0;
				bxfile = data.get(pos);
			}else{
				bxfile = data.get(pos);
			}
			DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		}else if(curType == DMCApplication.PHOTO_TYPE){
			//DLNAContainer.getInstance().getDLNAClient().showImage(bxfile.getFilePath());
		}
		DMCApplication.getInstance().setCurrentPos(pos);
	}
	private void doRepeat(){
		mCurType = mApp.getCurrentType();
		if(mCurType == DMCApplication.MUSIC_TYPE){
			if(Globals.musicPlayMode == Globals.PLAY_ONCE){
				Globals.musicPlayMode = Globals.PLAY_ALL;
				mRepeat.setImageResource(R.drawable.repeat_all);
			}else if(Globals.musicPlayMode == Globals.PLAY_ALL){
				Globals.musicPlayMode = Globals.PLAY_ONCE;
				mRepeat.setImageResource(R.drawable.repeat_once);
			}
			mRepeat.invalidate();
		}else if(mCurType == DMCApplication.VIDEO_TYPE){
			if(Globals.videoPlayMode == Globals.PLAY_ONCE){
				Globals.videoPlayMode = Globals.PLAY_ALL;
				mRepeat.setImageResource(R.drawable.repeat_all);
			}else if(Globals.videoPlayMode == Globals.PLAY_ALL){
				Globals.videoPlayMode = Globals.PLAY_ONCE;
				mRepeat.setImageResource(R.drawable.repeat_once);
			}
			mRepeat.invalidate();
		}
	}
	private void refreshUI(){
		progressBar.invalidate();
		mBackward.invalidate();
		mPlay.invalidate();
		mForward.invalidate();
		mRepeat.invalidate();
		mRepeat.invalidate();
		mSongName.invalidate();
		mArtist.invalidate();
	}
	private OnClickListener mOnClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			switch(v.getId()){
				case R.id.img_backward:
					doBackward();
					initDiffView();
					refreshUI();
					break;
				case R.id.img_play:
					DLNAContainer.getInstance().getDLNAClient().setPlay(1);
					break;
				case R.id.img_forward:
					doForward();
					initDiffView();
					refreshUI();
					break;
				case R.id.img_repeat:
					doRepeat();
					break;
			}
		}
	};
	
	
	
}

