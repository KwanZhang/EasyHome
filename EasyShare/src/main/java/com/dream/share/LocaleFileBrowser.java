package com.dream.share;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dream.share.R;
import com.dream.share.util.FileUtils;

/***
 * 本地文件 浏览器
 * 可以由外部传入一个dir path
 * 
 * @author zhanglei
 *
 */
public class LocaleFileBrowser extends Activity implements OnItemClickListener {
	private String tag = "LocaleFileBrowser";
	private File curFile;//当前目录
	private String startPath;//初始path
	private TextView curDir;
	private ListView lv;
	private List<TFile> data;
	private LocaleFileAdapter adapter;
	private TextView emptyView;
	private FileManager bfm;
	
	private SyncImageLoader.OnImageLoadListener imageLoadListener;
	private SyncImageLoader syncImageLoader;
	private AbsListView.OnScrollListener onScrollListener;
	
	private int firstImageFileIndex;//第一个图片文件的index(滚动时只对于普通文件loadImage)
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.localefile_browser);
		bfm = FileManager.getInstance();
		initViews();
		initData();
	}

	private void initData() {
		// TODO Auto-generated method stub
		Intent intent = getIntent();
		setTitle(intent.getStringExtra("title"));
		startPath = intent.getStringExtra("startPath");
		if(!FileUtils.isDir(startPath)){
			startPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		syncImageLoader = new SyncImageLoader();
		imageLoadListener= new SyncImageLoader.OnImageLoadListener() {
			@Override
			public void onImageLoad(Integer t, Drawable drawable) {
				View view = lv.findViewWithTag(t);
				if (view != null) {
					ImageView iv = (ImageView) view
							.findViewById(R.id.fileType);
					iv.setImageDrawable(drawable);
				}else{
					Log.i(tag, "View not exists");
				}
			}

			@Override
			public void onError(Integer t) {
				View view = lv.findViewWithTag(t);
				if (view != null) {
					ImageView iv = (ImageView) view
							.findViewById(R.id.fileType);
					iv.setImageResource(R.drawable.bxfile_file_unknow);
				}else{
					Log.i(tag, " onError View not exists");
				}
			}
		};
		onScrollListener = new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
				case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
					syncImageLoader.lock();
					break;
				case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
					loadImage();
					break;
				case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
					syncImageLoader.lock();
					break;
				default:
					break;
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

			}
		};
		setData(startPath);
	}
	
	//在文件夹区域，不用load
	public void loadImage() {
		int start = lv.getFirstVisiblePosition();
		int end = lv.getLastVisiblePosition();
		if(end<firstImageFileIndex){
			Log.i(tag, "loadImage return");
			return;
		}
		if(start<firstImageFileIndex)
			start = firstImageFileIndex;
		if (end >= data.size()) {
			end = data.size() - 1;
		}
//		Log.i(tag, "loadImage start:"+start+" , end:"+end);
		syncImageLoader.setLoadLimit(start, end);
		syncImageLoader.unlock();
	}
	
	private void setData(String dirPath){
		curDir.setText(dirPath);
		curFile = new File(dirPath);
		File[] childs = curFile.listFiles();
		if(null == childs || 0==childs.length){
			lv.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		}else{
			lv.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
			if(null!=data)
				data.clear();
			else
				data = new ArrayList<TFile>();
			for(File f:childs){
				TFile.Builder builder = new TFile.Builder(f.getAbsolutePath());
				TFile bxfile = builder.build();
				if(null != bxfile)
					data.add(bxfile);
			}
			Collections.sort(data);
			initFirstFileIndex();
			if(null == adapter){
				syncImageLoader.restore();
				adapter = new LocaleFileAdapter(data,this,syncImageLoader,imageLoadListener);
				lv.setAdapter(adapter);
				lv.setOnScrollListener(onScrollListener);
				loadImage();
			}else{
				syncImageLoader.restore();
				loadImage();
				adapter.notifyDataSetChanged();
				lv.setSelection(0);
			}
		}
	}
	
	//找到第一个图片类型文件index
	private void initFirstFileIndex(){
		firstImageFileIndex = -1;
		for(int i=0;i<data.size();i++){
			TFile f = data.get(i);
			if(!f.isDir() && f.getMimeType().equals(TFile.MimeType.IMAGE)){
				firstImageFileIndex = i;
				return ;
			}
		}
	}

	private void initViews() {
		// TODO Auto-generated method stub
		curDir = (TextView) findViewById(R.id.curDir);
		lv = (ListView) findViewById(R.id.listView);
		lv.setOnItemClickListener(this);
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

	//点击 目录 进入下一级；点击文件进行勾选操作
	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int pos, long arg3) {
		TFile bxfile = data.get(pos);
		if(bxfile.isDir()){
			setData(bxfile.getFilePath());
		}else{
			if(Globals.isAudioFile(bxfile.getFilePath())){
				DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
				DMCApplication.getInstance().setCurrentType(DMCApplication.MUSIC_TYPE);
				DMCApplication.getInstance().setCurrentData(data);
				DMCApplication.getInstance().setCurrentPos(pos);
			}else if(Globals.isImageFile(bxfile.getFilePath())){
				DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
				DMCApplication.getInstance().setCurrentType(DMCApplication.PHOTO_TYPE);
				DMCApplication.getInstance().setCurrentData(data);
				DMCApplication.getInstance().setCurrentPos(pos);
			}else if (Globals.isVideoFile(bxfile.getFilePath())){
				DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
				DMCApplication.getInstance().setCurrentType(DMCApplication.VIDEO_TYPE);
				DMCApplication.getInstance().setCurrentData(data);
				DMCApplication.getInstance().setCurrentPos(pos);
			}
		}
	}


	//退到初始目录，finish() , else显示上级目录
	@Override
	public void onBackPressed() {
		if(startPath.equals(curFile.getAbsolutePath())){
			super.onBackPressed();
		}else{
			setData(curFile.getParentFile().getAbsolutePath());
		}
	}
}
