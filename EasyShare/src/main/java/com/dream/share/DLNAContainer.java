package com.dream.share;

import java.util.ArrayList;
import java.util.List;

import org.cybergarage.upnp.Device;

import android.util.Log;

import com.dream.share.service.ShareService;
import com.dream.share.util.LogUtil;

public class DLNAContainer {
	private static final String TAG = "DLNAContainer";
	private List<Device> mDevices;
	private Device mSelectedDevice;
	public org.cybergarage.upnp.Service mCurService;
	private DeviceChangeListener mDeviceChangeListener;
	private PlayerChangeListener mPlayerChangeListener;
	public final static String SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1";
	public final static String DREAM_PLAYER = "Dream Player";
	private static final DLNAContainer mDLNAContainer = new DLNAContainer();
	private ShareService mDLNA = null;

	private DLNAContainer() {
		mDevices = new ArrayList<Device>();
	}

	public static DLNAContainer getInstance() {
		return mDLNAContainer;
	}
	public void setDLNAClient(ShareService service){
		mDLNA = service;
	}
	public ShareService getDLNAClient(){
		return mDLNA;
	}

	public synchronized void addDevice(Device d) {
		//if (!DLNAUtil.isMediaRenderDevice(d))
		//	return;
		if(!d.getFriendlyName().startsWith(DREAM_PLAYER))
			return;
		int size = mDevices.size();
		for (int i = 0; i < size; i++) {
			String udnString = mDevices.get(i).getUDN();
			if (d.getUDN().equalsIgnoreCase(udnString)) {
				return;
			}
		}
		
		mDevices.add(d);
		LogUtil.d(TAG, "Devices add a device" + d.getDeviceType());
		if (mDeviceChangeListener != null) {
			mDeviceChangeListener.onDeviceChange(d);
		}
	}

	public synchronized void removeDevice(Device d) {
//		if (!DLNAUtil.isMediaRenderDevice(d)) {
//			return;
//		}
		int size = mDevices.size();
		for (int i = 0; i < size; i++) {
			String udnString = mDevices.get(i).getUDN();
			if (d.getUDN().equalsIgnoreCase(udnString)) {
				Device device = mDevices.remove(i);
				LogUtil.d(TAG, "Devices remove a device");

				boolean ret = false;
				if (mSelectedDevice != null) {
					ret = mSelectedDevice.getUDN().equalsIgnoreCase(
							device.getUDN());
				}
				if (ret) {
					mSelectedDevice = null;
				}
				if (mDeviceChangeListener != null) {
					mDeviceChangeListener.onDeviceChange(d);
				}
				break;
			}
		}
	}
	public synchronized void clearDevice(){
		mDevices.clear();
		if (mDeviceChangeListener != null) {
			mDeviceChangeListener.onDeviceChange(null);
		}
	}

	public synchronized void clear() {
		if (mDevices != null) {
			mDevices.clear();
			mSelectedDevice = null;
		}
	}

	public Device getSelectedDevice() {
		return mSelectedDevice;
	}
	
	public org.cybergarage.upnp.Service getCurService(){
		return mCurService;
	}
	
	public void setSelectedDevice(Device mSelectedDevice) {
		this.mSelectedDevice = mSelectedDevice;
		if(mSelectedDevice != null){
			mCurService = mSelectedDevice.getService(SERVICE_TYPE);
			if (mCurService == null){
				LogUtil.e(TAG,"no service for ContentDirectory!!!");
				return;
			}
		}
	}

	public void setDeviceChangeListener(
			DeviceChangeListener deviceChangeListener) {
		mDeviceChangeListener = deviceChangeListener;
	}

	public void setPlayerChangeListener(PlayerChangeListener playerChangeListener){
		mPlayerChangeListener = playerChangeListener;
	}
	public List<Device> getDevices() {
		return mDevices;
	}

	public interface DeviceChangeListener {
		void onDeviceChange(Device device);
	}
	public interface PlayerChangeListener{
		void onPlayerChangeListener(final int status, final int value);
	}

	public void updateStutus(int status, int value){
		Log.d(TAG, "value " + value);
		if(status == Globals.PLAYER_STATUS_STOP){
			ControlPanel.doStop();
		}
		if(mPlayerChangeListener != null){
			mPlayerChangeListener.onPlayerChangeListener(status, value);
		}
	}
}
