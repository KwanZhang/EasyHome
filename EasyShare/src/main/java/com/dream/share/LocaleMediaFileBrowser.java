package com.dream.share;

import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.dream.share.R;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

/***
 * 本地多媒体文件 浏览器
 * 图片 音频 视频
 * 
 * @author zhanglei
 *
 */
public class LocaleMediaFileBrowser extends SlidingFragmentActivity implements OnItemClickListener {
	
	private final static String TAG = "LocaleMediaFileBrowser";
	private ListView mFileList;
	private List<TFile> data;
	private LocaleFileAdapter adapter;
	private TextView emptyView;
	private FileManager mFileManager;
	
	private DMCApplication mApp ;
	private int mCurType;
	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if(1 == msg.what){
				mFileList.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
				adapter = new LocaleFileAdapter(data,LocaleMediaFileBrowser.this,null,null);
				mFileList.setAdapter(adapter);
			}else if(0 == msg.what){
				mFileList.setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
				emptyView.setText(getString(R.string.curCatagoryNoFiles));
			}
			super.handleMessage(msg);
		}
		
	};
	//show toast
    protected void showToast(final int strId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
            }
        });
    }
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
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.localefile_browser);
		mApp = DMCApplication.getInstance();
		
		mFileManager = FileManager.getInstance();
		initViews();
		initData();
		initLeftMenu();
	}

	private void initData() {
		// TODO Auto-generated method stub
		Intent intent = getIntent();
		
		setTitle(intent.getStringExtra(Globals.TITLE_EXTRA));
		mCurType = intent.getIntExtra(Globals.TYPE_EXTRA,0);
		setData(intent.getData());
	}
	
	private void setData(final Uri uri){
		mApp.execRunnable(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				data= mFileManager.getMediaFiles(LocaleMediaFileBrowser.this, uri);
				if(null != data){
					Collections.sort(data);
					handler.sendEmptyMessage(1);
				}
				else
					handler.sendEmptyMessage(0);
			}
			
		});
	}

	private void initViews() {
		// TODO Auto-generated method stub
		TextView curDir = (TextView) findViewById(R.id.curDir);
		curDir.setVisibility(View.GONE);
		mFileList = (ListView) findViewById(R.id.listView);
		mFileList.setOnItemClickListener(this);
		emptyView = (TextView) findViewById(R.id.emptyView);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 0, 0, getString(R.string.cancel));
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if(0 == item.getItemId()){
			setResult(1);
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	//点击文件进行勾选操作
	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int pos, long arg3) {
		TFile bxfile = data.get(pos);
		Log.d(TAG,"data size " + data.size());

		DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		mApp.setCurrentType(mCurType);
		mApp.setCurrentData(data);
		mApp.setCurrentPos(pos);
	}
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(null!=data){
			data.clear();
		}
		data = null;
		adapter = null;
		handler = null;
	}

}
