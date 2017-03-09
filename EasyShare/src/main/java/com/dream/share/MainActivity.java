package com.dream.share;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.dream.share.R;
import com.dream.share.service.ShareService;
import com.dream.share.util.FileUtils;
import com.dream.share.util.NetUtil;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;
import com.xiaomi.market.sdk.XiaomiUpdateAgent;

public class MainActivity  extends SlidingFragmentActivity {
	private final static String TAG = "MainActivty";
	private String extSdCardPath;
	private FileManager mFileManager;
	private Intent mShareService;
	private PowerManager.WakeLock    mWakeLock;
	private final int REQUEST = 1;
	
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
	private void startShareService() {
		if(mShareService == null){
			mShareService = new Intent(this, ShareService.class);
			startService(mShareService);	
		}
	}
	private void stopShareService(){
		if(mShareService != null){
			stopService(mShareService);
		}
	}
	private void notifyOpenWifi(Context context){
		Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(R.string.notify_connect_rounter);
		builder.setPositiveButton(R.string.ok,new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
				dialog.dismiss();
				startActivity(intent);
				
			}
		});
		builder.setNegativeButton(R.string.quit,new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
			
		});
		builder.show();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        /* enable backlight */
		XiaomiUpdateAgent.update(this);
		setContentView(R.layout.localefile_main);
		setTitle(getString(R.string.localeFile));
		mFileManager = FileManager.getInstance();
		
		extSdCardPath = FileUtils.getExtSdCardPath();
		if(!TextUtils.isEmpty(extSdCardPath)){
			View localefile_sdcard = findViewById(R.id.localefile_sdcard);
			View localefile_sdcard2 = findViewById(R.id.localefile_sdcard2);
			View localefile_extSdcard = findViewById(R.id.localefile_extSdcard);
			localefile_sdcard2.setVisibility(View.VISIBLE);
			localefile_extSdcard.setVisibility(View.VISIBLE);
			localefile_sdcard.setVisibility(View.GONE);
		}
		initLeftMenu();
		DMCApplication.getInstance().addActivity(this);

        //InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG);
        //InMobiSdk.init(this, "ab9fab056f084963b2c6391ae569e20b");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		String addr = NetUtil.getHostAddr(this);
		if(addr == null){
			notifyOpenWifi(this);
		}else{
			startShareService();
		}
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mWakeLock.release();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, getString(R.string.cancel));
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(0 == item.getItemId()){
			mFileManager.clear();
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onClick(View v){
		switch(v.getId()){
		case R.id.localefile_ram:
			Intent intent2 = new Intent(this,LocaleFileBrowser.class);
			intent2.putExtra(Globals.TITLE_EXTRA, getString(R.string.bxfile_ram));
			intent2.putExtra(Globals.STARTPATH_EXTRA, "/");
			startActivityForResult(intent2,REQUEST);
			break;
		case R.id.localefile_sdcard:
		case R.id.localefile_sdcard2:
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				Intent intent3 = new Intent(this,LocaleFileBrowser.class);
				intent3.putExtra(Globals.TITLE_EXTRA, getString(R.string.bxfile_sdcard));
				intent3.putExtra(Globals.STARTPATH_EXTRA, Environment.getExternalStorageDirectory().getAbsolutePath());
				startActivityForResult(intent3,REQUEST);
			}else{
				Toast.makeText(this, getString(R.string.SDCardNotMounted), Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.localefile_extSdcard:
			Intent intent4 = new Intent(this,LocaleFileBrowser.class);
			intent4.putExtra(Globals.TITLE_EXTRA, getString(R.string.bxfile_extsdcard));
			intent4.putExtra(Globals.STARTPATH_EXTRA, extSdCardPath);
			startActivityForResult(intent4,REQUEST);
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(REQUEST == requestCode && 1==resultCode){
			mFileManager.clear();
			finish();
		}else if(REQUEST == requestCode && 2==resultCode){
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		DMCApplication.getInstance().removeActivity(this);
		stopShareService();
		super.onDestroy();
		
		System.gc();
	}

}
