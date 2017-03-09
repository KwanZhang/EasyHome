package com.android.soundcapture;


import java.util.Collections;
import java.util.List;

import com.dream.share.DMCApplication;
import com.dream.share.FileManager;
import com.dream.share.Globals;
import com.dream.share.LocaleFileAdapter;
import com.dream.share.R;
import com.dream.share.TFile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class sdBrowser extends Activity implements OnItemClickListener {
	private final static String TAG = "sdBrowser";
	private ListView mFileList;
	private List<TFile> data;
	private LocaleFileAdapter adapter;
	private TextView emptyView;
	private FileManager mFileManager;
	
	private DMCApplication mApp ;
	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if(1 == msg.what){
				mFileList.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
				adapter = new LocaleFileAdapter(data,sdBrowser.this,null,null);
				mFileList.setAdapter(adapter);
			}else if(0 == msg.what){
				mFileList.setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
				emptyView.setText(getString(R.string.curCatagoryNoFiles));
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.localefile_browser);
		mApp = DMCApplication.getInstance();	
		mFileManager = FileManager.getInstance();
		initViews();
		initData();
	}

	private void initData() {
		// TODO Auto-generated method stub
		Intent intent = getIntent();
		
		setTitle(intent.getStringExtra(Globals.TITLE_EXTRA));
		setData();
	}
	
	private void setData(){
		mApp.execRunnable(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				data= mFileManager.getMediaFiles(sdBrowser.this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
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
		Intent intent = new Intent();
		intent.putExtra("file", bxfile);
		setResult(1,intent);
		finish();
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
