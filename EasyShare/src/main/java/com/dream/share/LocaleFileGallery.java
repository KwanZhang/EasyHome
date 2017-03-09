package com.dream.share;

import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.dream.share.R;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

/***
 * 本地图片浏览
 * 
 * @author zhanglei
 *
 */
public class LocaleFileGallery extends SlidingFragmentActivity implements OnItemClickListener {
	
	private final static String TAG = "LocaleFileGallery";
	private GridView gv;
	private MyGVAdapter adapter;
	private List<TFile> data;
	private TextView emptyView;
	private FileManager bfm;
	
	private SyncImageLoader syncImageLoader;
	private int gridSize;
	private AbsListView.LayoutParams gridItemParams;//主要根据不同分辨率设置item宽高
    //show toast
    protected void showToast(final int strId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(BaseActivity.this, getString(strId), Toast.LENGTH_SHORT).show();
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
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(1 == msg.what){
				syncImageLoader = new SyncImageLoader();
				gridItemParams = new AbsListView.LayoutParams(gridSize,gridSize);
				adapter = new MyGVAdapter();
				gv.setAdapter(adapter);
				gv.setOnScrollListener(adapter.onScrollListener);
				gv.setOnItemClickListener(LocaleFileGallery.this);
			}else if(0 == msg.what){
				gv.setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
				emptyView.setText(getString(R.string.curCatagoryNoFiles));
			}
			super.handleMessage(msg);
		}
	};
	@SuppressWarnings("deprecation")
	private int getScreenWidth(Activity cxt) {
		WindowManager m = cxt.getWindowManager();
		Display d = m.getDefaultDisplay();
		return d.getWidth();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.localefile_gallery);
		setTitle(getIntent().getStringExtra(Globals.TITLE_EXTRA));
		bfm = FileManager.getInstance();
		gv = (GridView) findViewById(R.id.gridView);
		emptyView = (TextView) findViewById(R.id.emptyView);
		//计算一下在不同分辨率下gridItem应该站的宽度，在adapter里重置一下item宽高
		gridSize = (getScreenWidth(this) - getResources().getDimensionPixelSize(R.dimen.view_8dp)*5)/4 ;// 4列3个间隔，加上左右padding，共计5个
//		Log.i(tag, "gridSize:"+gridSize);
		initLeftMenu();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(null == data){
			DMCApplication bxApp = (DMCApplication) getApplication();
			bxApp.execRunnable(new Runnable(){
				@Override
				public void run() {
					data = bfm.getMediaFiles(LocaleFileGallery.this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					if(null!=data)
						handler.sendEmptyMessage(1);
					else
						handler.sendEmptyMessage(0);
				}
				
			});
		}
	}
	

	@Override
	protected void onDestroy() {
		if(null!=data)
			data.clear();
		syncImageLoader = null;
		handler = null;
		data = null;
		adapter = null;
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, getString(R.string.cancel));
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(0 == item.getItemId()){
			setResult(1);
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	class MyGVAdapter extends BaseAdapter{
		
		AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {
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
		
		public void loadImage() {
			int start = gv.getFirstVisiblePosition();
			int end = gv.getLastVisiblePosition();
			if (end >= getCount()) {
				end = getCount() - 1;
			}
			syncImageLoader.setLoadLimit(start, end);
			syncImageLoader.unlock();
		}
		
		@Override
		public int getCount() {
			if(null!=data)
				return data.size();
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(null == convertView){
				convertView = LayoutInflater.from(LocaleFileGallery.this).inflate(R.layout.gallery_item, null);
			}
			ImageView img = (ImageView) convertView.findViewById(R.id.img);
			img.setImageResource(R.drawable.bxfile_file_default_pic);
			View itemView = convertView.findViewById(R.id.itemView);
			itemView.setLayoutParams(gridItemParams);
			TFile bxfile = data.get(position);
			img.setTag(position);
			syncImageLoader.loadDiskImage(position, bxfile.getFilePath(), imageLoadListener);
			return convertView;
		}
		
		
		SyncImageLoader.OnImageLoadListener imageLoadListener = new SyncImageLoader.OnImageLoadListener() {
			@Override
			public void onImageLoad(Integer t, Drawable drawable) {
				View view = gv.findViewWithTag(t);
				if (view != null) {
					ImageView iv = (ImageView) view
							.findViewById(R.id.img);
					iv.setImageDrawable(drawable);
				}else{
					Log.i(TAG, "View not exists");
				}
			}

			@Override
			public void onError(Integer t) {
				View view = gv.findViewWithTag(t);
				if (view != null) {
					ImageView iv = (ImageView) view
							.findViewById(R.id.img);
					iv.setImageResource(R.drawable.bxfile_file_default_pic);
				}else{
					Log.i(TAG, " onError View not exists");
				}
			}
		};
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View convertView, int pos, long arg3) {
		TFile bxfile = data.get(pos);
		Log.d(TAG, "width " + bxfile.getWidth() + " height " + bxfile.getHeight());
		DLNAContainer.getInstance().getDLNAClient().playNode(bxfile.getFilePath());
		DMCApplication.getInstance().setCurrentType(DMCApplication.PHOTO_TYPE);
		DMCApplication.getInstance().setCurrentData(data);
		DMCApplication.getInstance().setCurrentPos(pos);
	}

}
