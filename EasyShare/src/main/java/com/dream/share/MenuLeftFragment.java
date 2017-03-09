package com.dream.share;

import java.util.List;
import java.util.Map;

import org.cybergarage.upnp.Device;

//import com.broov.player.Settings;
import com.dream.share.R;
import com.dream.share.DLNAContainer.DeviceChangeListener;
import com.dream.share.util.LogUtil;
import com.dream.share.util.SwitchView;
import com.dream.share.util.SwitchView.OnStateChangedListener;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdSize;
//import com.google.android.gms.ads.AdView;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class MenuLeftFragment extends Fragment
{
	private final static String TAG = "MenuLeftFragment";
	private View mView;
	private ImageView mSchButton;
	private LinearLayout mLocalFilePicture;
	private LinearLayout mLocalFileMusic;
	private LinearLayout mLocalFileVideo;
	private LinearLayout mCtrlPanel;
	private LinearLayout mRecPanel;
	private SwitchView mSwitchWifiP2p;
	private ListView mDevicesListView;
	private List<Device> mDevices;
	private DeviceAdapter mDeviceAdapter;
	private  String mSelDevices = "none";
	
	private FileManager bfm;
	private final static int REQUEST = 0x01;
	
	//for inmob ad
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (mView == null){
			initView(inflater, container);
		} 
		return mView;
	}
	private void initView(LayoutInflater inflater, ViewGroup container)
	{
		mView = inflater.inflate(R.layout.left_menu, container, false);
		mDevicesListView = (ListView)mView.findViewById(R.id.devices);
		mDeviceAdapter = new DeviceAdapter();
		mDevices = DLNAContainer.getInstance().getDevices();
		mDevicesListView.setAdapter(mDeviceAdapter);
		mDevicesListView.setOnItemClickListener(mItemClickListener);
		
		bfm = FileManager.getInstance();
		TextView picCnt = (TextView) mView.findViewById(R.id.localefile_pic_cnt);
		picCnt.setText(String.format(getString(R.string.bxfile_media_cnt), 
				bfm.getMediaFilesCnt(getActivity(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
		TextView musicCnt = (TextView) mView.findViewById(R.id.localefile_music_cnt);
		musicCnt.setText(String.format(getString(R.string.bxfile_media_cnt), 
				bfm.getMediaFilesCnt(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)));
		TextView videoCnt = (TextView) mView.findViewById(R.id.localefile_video_cnt);
		videoCnt.setText(String.format(getString(R.string.bxfile_media_cnt), 
				bfm.getMediaFilesCnt(getActivity(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)));
		
		mSchButton = (ImageView) mView.findViewById(R.id.btn_search);
		mSchButton.setOnClickListener(mOnClickListener);

		mLocalFilePicture = (LinearLayout)mView.findViewById(R.id.localefile_picture);
		mLocalFilePicture.setOnClickListener(mOnClickListener);

		mLocalFileMusic = (LinearLayout)mView.findViewById(R.id.localefile_music);
		mLocalFileMusic.setOnClickListener(mOnClickListener);

		mLocalFileVideo = (LinearLayout)mView.findViewById(R.id.localefile_video);
		mLocalFileVideo.setOnClickListener(mOnClickListener);

		mSwitchWifiP2p = (SwitchView)mView.findViewById(R.id.wifip2p_switch);
		if(Globals.P2PState == Globals.P2P_STATE_DISCONNECT){
			mSwitchWifiP2p.setOpened(false);
		}else{
			mSwitchWifiP2p.setOpened(false);
		}
		mSwitchWifiP2p.setOnClickListener(mOnClickListener);
		mSwitchWifiP2p.setOnStateChangedListener(mWifiP2pSwitchChangeListener);

		//mSetting = (LinearLayout)mView.findViewById(R.id.setting);
		//mSetting.setOnClickListener(mOnClickListener);
		
		mCtrlPanel = (LinearLayout)mView.findViewById(R.id.ctrlPanel);
		mCtrlPanel.setOnClickListener(mOnClickListener);
		mRecPanel = (LinearLayout)mView.findViewById(R.id.recPanel);
		mRecPanel.setOnClickListener(mOnClickListener);
		DLNAContainer.getInstance().setDeviceChangeListener(
			new DeviceChangeListener() {
				@Override
				public void onDeviceChange(Device device) {
					if(getActivity() != null){
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								refresh();
							}
						});	
					}
				}
			});
	}
	OnStateChangedListener mWifiP2pSwitchChangeListener = new OnStateChangedListener(){

		@Override
		public void toggleToOn(View view) {
			Log.d(TAG, "toggle on button");
			((SwitchView)view).toggleSwitch(SwitchView.STATE_SWITCH_ON);
			sendP2PStartBroadcast();
		}

		@Override
		public void toggleToOff(View view) {
			Log.d(TAG, "toggle off button");
			((SwitchView)view).toggleSwitch(SwitchView.STATE_SWITCH_OFF);
			sendP2PStopBroadcast();
		}
		
	};
    private void sendP2PStartBroadcast(){
			Intent intent = new Intent(Globals.ACTION_P2P_START);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			getActivity().sendBroadcast(intent);
	}
    private void sendP2PStopBroadcast(){
			Intent intent = new Intent(Globals.ACTION_P2P_STOP);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			getActivity().sendBroadcast(intent);
	}
	private OnClickListener mOnClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			switch(v.getId()){
				case R.id.localefile_music:
					Intent intent5 = new Intent(getActivity(),LocaleMediaFileBrowser.class);
					intent5.putExtra(Globals.TITLE_EXTRA, getString(R.string.bxfile_music));
					intent5.putExtra(Globals.TYPE_EXTRA, DMCApplication.MUSIC_TYPE);
					intent5.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
					getActivity().startActivityForResult(intent5,REQUEST);
					if(!getActivity().getClass().getName().equals("com.dream.share.MainActivity")){
						getActivity().finish();
					}
					break;
				case R.id.localefile_video:
					Intent intent6 = new Intent(getActivity(),LocaleMediaFileBrowser.class);
					intent6.putExtra(Globals.TITLE_EXTRA, getString(R.string.bxfile_video));
					intent6.putExtra(Globals.TYPE_EXTRA, DMCApplication.VIDEO_TYPE);
					intent6.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
					getActivity().startActivityForResult(intent6,REQUEST);
					if(!getActivity().getClass().getName().equals("com.dream.share.MainActivity")){
						getActivity().finish();
					}
					break;
				case R.id.localefile_picture:
					Intent intent7 = new Intent(getActivity(),LocaleFileGallery.class);
					intent7.putExtra(Globals.TYPE_EXTRA, DMCApplication.PHOTO_TYPE);
					intent7.putExtra(Globals.TITLE_EXTRA, getString(R.string.bxfile_image));
					intent7.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					getActivity().startActivityForResult(intent7,REQUEST);
					if(!getActivity().getClass().getName().equals("com.dream.share.MainActivity")){
						getActivity().finish();
					}
					break;
				case R.id.wifip2p_switch:
					Log.d(TAG,"wifip2p_switch");
					sendP2PStartBroadcast();
					break;
				case R.id.ctrlPanel:{
					Intent intent8 = new Intent(getActivity(),ControlPanel.class);
					intent8.putExtra(Globals.TITLE_EXTRA, getString(R.string.bxfile_image));
					getActivity().startActivityForResult(intent8,REQUEST);
					if(!getActivity().getClass().getName().equals("com.dream.share.MainActivity")){
						getActivity().finish();
					}
				}
				break;
				case R.id.recPanel:{
						Intent intent8 = new Intent(getActivity(),RecordPanel.class);
						getActivity().startActivityForResult(intent8,REQUEST);
						if(!getActivity().getClass().getName().equals("com.dream.share.MainActivity")){
							getActivity().finish();
						}
					}
					break;
				case R.id.btn_search:
					DLNAContainer.getInstance().getDLNAClient().search();
					mDevices = DLNAContainer.getInstance().getDevices();
					LogUtil.d(TAG, "mDevices size:" + mDevices.size());
					refresh();
					break;
			}
		}
	};

	private void refresh() {
		if (mDeviceAdapter != null) {
			mDeviceAdapter.notifyDataSetChanged();
		}
	}
	
	OnItemClickListener mItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			DLNAContainer.getInstance().setSelectedDevice(mDevices.get(position));
			//Globals.dbSelDevies = mDevices.get(position).getFriendlyName();
			Log.d(TAG,"mSelDevices " + mSelDevices);
			Globals.setSelDevice(getActivity(), mDevices.get(position).getFriendlyName());
			mDeviceAdapter.notifyDataSetChanged();
			//int port = DMCApplication.getInstance().getPlayerStatusListener().getPort();
			//DLNAContainer.getInstance().getDLNAClient().registerStatusListner(port);
		}
	};
	@Override
	public void onDestroyView(){
		Log.d(TAG,"destory view");
		super.onDestroyView();
	}
	@Override 
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onResume");
		if(Globals.P2PState == Globals.P2P_STATE_DISCONNECT){
			mSwitchWifiP2p.setOpened(false);
		}else{
			mSwitchWifiP2p.setOpened(false);
		}
	}
	
	private class DeviceAdapter extends BaseAdapter {
		
		@Override
		public int getCount() {
			Log.d(TAG, "connt " + mDevices.size());
			
			if (mDevices == null) {
				return 0;
			} else {
				return mDevices.size();
			}
		}

		@Override
		public Object getItem(int position) {
			Log.d(TAG, "get Item " + position);
			if (mDevices != null) {
				return mDevices.get(position);
			}

			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.d(TAG,"get View");
			ViewHolder holder;
			if (convertView == null) {
				convertView = View.inflate(getActivity(),
						R.layout.devices_item, null);
				holder = new ViewHolder();
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.tv_name_item = (TextView) convertView
					.findViewById(R.id.friend_name);
			holder.tv_name_item.setText(mDevices.get(position)
					.getFriendlyName());
			holder.select_item = (RadioButton)convertView
					.findViewById(R.id.radio_btn);

			//Log.d(TAG,"pos " + position + " select " + Globals.dbSelDevice);
			if(mDevices.get(position).getFriendlyName().equals(Globals.dbSelDevice)){
				holder.select_item.setChecked(true);
			}else{
				holder.select_item.setChecked(false);
			}
			//Log.d(TAG,"friend name " + mDevices.get(position)
			//		.getFriendlyName());
			return convertView;
		}

	}

	static class ViewHolder {
		private TextView tv_name_item;
		private RadioButton select_item;
	}
	
}
