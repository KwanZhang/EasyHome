package com.dream.share;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;

public class Globals {

	public final static String TITLE_EXTRA = "title_extra";
	public final static String TYPE_EXTRA = "type_extra";
	public static final String IP_EXTRA = "IP_EXTRA";
	public static final String PORT_EXTRA = "PORT_EXTRA";
	public static final String PATH_EXTRA = "PATH_EXTRA";
	public static final String IMAGE_WIDTH = "IMAGE_WIDTH";
	public static final String IMAGE_HEIGHT = "IMAGE_HEIGHT";
	public static final String URL_EXTRA = "URL_EXTRA";
	public static final String STATE_EXTRA = "STATE_EXTRA";
	public static final String MAC_EXTRA   = "MAC_EXTRA";
	public static final String VOICE_EXTRA = "VOICE_EXTRA";
	public static final String SEEK_EXTRA  = "SEEK_EXTRA";
	public static final String STARTPATH_EXTRA = "startPath";
	public static final String ACTION_P2P_CONNECT = "com.dream.share.P2P_CONNECT";
	public static final String ACTION_P2P_DISCONNECT = "com.dream.share.P2P_DISCONNECT";
	public static final String ACTION_P2P_SETUP_TIMEOUT = "com.dream.share.P2P_SETUP_TIMEOUT";
	public static final String ACTION_P2P_START = "com.dream.share.P2P_START";
	public static final String ACTION_P2P_STOP = "com.dream.share.P2P_STOP";
	public static final String ACTION_P2P_INFO = "com.dream.share.P2P_INFO";
	public static final String NULL = "none";
	public static String  dbSelDevice	 = "none";
	public static String  selfP2pMac	 = "none";
	public static final String PREFS_SEL_DEVICE = "seldevices";
	public static final String PREFS_NAME = "BroovPrefsFileTypeTwo";	//user preference file name
	public static final String PREFS_SELF_P2P_MAC = "selfp2pmac";
	
	public static final int PLAYER_STATUS_PLAY = 0;
	public static final int PLAYER_STATUS_PAUSE = 1;
	public static final int PLAYER_STATUS_STOP = 2;
	public static final int PLAYER_POSTION_UPDATE = 3;
	
	public static final int CONNECT_WIFI= 1;
	public static final int CONNECT_P2P = 2;
	
	public static final int P2P_STATE_DISCONNECT = 1;
	public static final int P2P_STATE_CONNECTING = 2;
	public static final int P2P_STATE_CONNECTED = 2;
	public static int P2PState = P2P_STATE_DISCONNECT;
	
	public static  int whatConnect = 0;
	
	public static final String supportedVideoFileFormats[] = 
		{   "mp4","wmv","avi","mkv","dv",
		"rm","mpg","mpeg","flv","divx",
		"swf","dat","h264","h263","h261",
		"3gp","3gpp","asf","mov","m4v", "ogv",
		"vob", "vstream", "ts", "webm",
		//to verify below file formats - reference vlc
		"vro", "tts", "tod", "rmvb", "rec", "ps", "ogx",
		"ogm", "nuv", "nsv", "mxf", "mts", "mpv2", "mpeg1", "mpeg2", "mpeg4",
		"mpe", "mp4v", "mp2v", "mp2", "m2ts",
		"m2t", "m2v", "m1v", "amv", "3gp2"
		//"ttf"
		};

	public static final String supportedAudioFileFormats[] = 
		{   "mp3","wma","ogg","mp2","flac",
			"aac","ac3","amr","pcm","wav",
			"au","aiff","3g2","m4a", "astream",
		//to verify below file formats - reference vlc
			"a52", "adt", "adts", "aif", "aifc",
			"aob", "ape", "awb", "dts", "cda", "it", "m4p",
			"mid", "mka", "mlp", "mod", "mp1", "mp2", "mpc",
			"oga", "oma", "rmi", "s3m", "spx", "tta",
			"voc", "vqf", "w64", "wv", "xa", "xm"
		};		

	public static final String supportedFontFileType[] = 
		{   "ttf"
		};

	public static final String supportedImageFileFormats[] = 
		{
		"gif","bmp","png","jpg"
		};

	public static final String supportedAudioStreamFileFormats[] = 
		{
		"astream"
		};

	public static final String supportedVideoStreamFileFormats[] = 
		{
		"vstream"
		};

	public static String supportedFileFormats[] =concat(supportedAudioFileFormats, supportedVideoFileFormats, supportedImageFileFormats);

	public static Boolean isAudioFile(String file){
		if(file==null || file =="" ){
			return false;
		}		
		String ext = file.toString();
		String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);
		if(Arrays.asList(supportedAudioFileFormats).contains(sub_ext.toLowerCase())){
			return true; 
		}
		return false;
	}

	public static Boolean isVideoFile(String file){
		if(file==null || file ==""){
			return false;
		}
		String ext = file.toString();
		String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);
		if (Arrays.asList(supportedVideoFileFormats).contains(sub_ext.toLowerCase())){
			return true; 
		}
		return false;
	}

	public static Boolean isImageFile(String file){
		String ext = file.toString();
		String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);
		if (Arrays.asList(supportedImageFileFormats).contains(sub_ext.toLowerCase())){
			return true; 
		}
		return false;
	}

	public static final int PLAY_ONCE =0;
	public static final int PLAY_ALL =1;
	public static final int REPEAT_ONE =2;
	public static final int REPEAT_ALL =3;

	public static int musicPlayMode = PLAY_ALL;
	public static int videoPlayMode = PLAY_ALL;
	public static boolean isP2pSetup = false;
	public static int mSampleRate = 44100;
	public static int mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	public static int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
	public static int mRecordVoice = 1;
	public static int mMusicVoice = 1;
	/**
	 * This method is used to concat 2 string arrays to one
	 * @param A
	 * First string Array
	 * @param B
	 * Second string Array
	 * @return
	 * Concatenated string Array of First and Second
	 */
	public static String[] concat(String[] A, String[] B, String[] C) {
		String[] temp= new String[A.length+B.length+C.length];
		System.arraycopy(A, 0, temp, 0, A.length);
		System.arraycopy(B, 0, temp, A.length, B.length);
		System.arraycopy(C, 0, temp, A.length+B.length, C.length);
		return temp;
	}
	public static void setSelDevice(Context context,String device){
		dbSelDevice = device;
		Editor edit = context.getSharedPreferences(Globals.PREFS_NAME,
				Context.MODE_PRIVATE).edit();
		edit.putString(PREFS_SEL_DEVICE, device);
		edit.commit();
	}
	public static String getSelDevice(Context context){
		SharedPreferences preferences = context.getSharedPreferences(Globals.PREFS_NAME,
				Context.MODE_PRIVATE);
		dbSelDevice = preferences.getString(PREFS_SEL_DEVICE, "none");
		return dbSelDevice;
	}
	public static void setSelfP2pMac(Context context,String mac){
		selfP2pMac = mac;
		Editor edit = context.getSharedPreferences(Globals.PREFS_NAME,
				Context.MODE_PRIVATE).edit();
		edit.putString(PREFS_SELF_P2P_MAC, mac);
		edit.commit();
	}
	public static String getSelfP2pMac(Context context){
		SharedPreferences preferences = context.getSharedPreferences(Globals.PREFS_NAME,
				Context.MODE_PRIVATE);
		selfP2pMac = preferences.getString(PREFS_SELF_P2P_MAC, "none");
		return selfP2pMac;
	}
}
