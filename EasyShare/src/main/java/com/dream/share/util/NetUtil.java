package com.dream.share.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

import org.cybergarage.net.HostInterface;
import org.cybergarage.util.Debug;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * 网络连接的一些工具类
 */
public class NetUtil {
	private static final String TAG = "NetUtil";
	private final static String P2P_IP_PREFIX = "192.168.49";
	private final static String AP_IP_PREFIX = "192.168.43";
	
	/**
	 * 判断当前网络是否可用
	 */
	public static boolean isNetAvailable(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isAvailable();
	}

	/**
	 * 判断WIFI是否使用
	 */
	public static boolean isWIFIActivate(Context context) {
		return ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
				.isWifiEnabled();
	}

	/**
	 * 修改WIFI状态
	 * 
	 * @param status
	 *            true为开启WIFI，false为关闭WIFI
	 */
	public static void changeWIFIStatus(Context context, boolean status) {
		((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
				.setWifiEnabled(status);
	}

	private final static int getNHostAddresses()
	{
			
		int nHostAddrs = 0;
		try {
			Enumeration nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()){
				NetworkInterface ni = (NetworkInterface)nis.nextElement();
				Enumeration addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					InetAddress addr = (InetAddress)addrs.nextElement();
					if (HostInterface.isUsableAddress(addr) == false)
						continue;
					nHostAddrs++;
				}
			}
		}
		catch(Exception e){
			Debug.warning(e);
		};
		return nHostAddrs;
	}
	public final static String getHostAddress(int n)
	{		
		int hostAddrCnt = 0;
		try {
			Enumeration nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()){
				NetworkInterface ni = (NetworkInterface)nis.nextElement();
				Enumeration addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					InetAddress addr = (InetAddress)addrs.nextElement();
					if (HostInterface.isUsableAddress(addr) == false)
						continue;
					if (hostAddrCnt < n) {
						hostAddrCnt++;
						continue;
					}
					String host = addr.getHostAddress();
					//if (addr instanceof Inet6Address)
					//	host = "[" + host + "]";
					return host;
				}
			}
		}
		catch(Exception e){};
		return "";
	}
	public static String getP2pAddr(){
		int addrNum = getNHostAddresses();
		for(int n = 0; n < addrNum; n++){
			String addr = getHostAddress(n);
			if(addr.startsWith(P2P_IP_PREFIX))
				return addr;			
		}
		return null;
	}
	public static String getHostAddr(Context context){
		int addrNum = getNHostAddresses();
		for(int n = 0; n < addrNum; n++){
			String addr = getHostAddress(n);
			if(addr.startsWith(P2P_IP_PREFIX))
				return addr;
			else if(addr.startsWith(AP_IP_PREFIX))
				return addr;
			
		}

		ConnectivityManager connectivityManager = 
			(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if(networkInfo == null)
			return null;
		if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
			WifiManager wifiManager = (WifiManager) context.getSystemService(
                Context.WIFI_SERVICE);
        	WifiInfo info = wifiManager.getConnectionInfo();
			if(info != null && info.getNetworkId() > -1){
	            int i = info.getIpAddress();
	            String addr = String.format(Locale.ENGLISH,
	                    "%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,
	                    i >> 16 & 0xff,i >> 24 & 0xff);
	            Log.d(TAG,"get wifi ip is " + addr);
	            return addr;
	        }
		}
		return null;
	}
}
