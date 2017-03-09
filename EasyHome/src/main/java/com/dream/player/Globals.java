package com.dream.player;

import java.util.Arrays;

public class Globals {

	public static String fileName; 
	public static String listenerIP;
	public static int listenerPort;
	public static int AudioBufferConfig   = 0;
	public static final int MSG_UPDATE_PLAY = 0x100;
	public static final int MSG_SET_PLAY = 0x101;
	public static final int MSG_SET_VOICE_DOWN = 0x102;
	public static final int MSG_SET_VOICE_UP = 0x103;
	public static final int MSG_SEEK_PLAY = 0x104;
	public static final String IMAGE_WIDTH = "IMAGE_WIDTH";
	public static final String IMAGE_HEIGHT = "IMAGE_HEIGHT";
	public static final String IP_EXTRA = "IP_EXTRA";
	public static final String PORT_EXTRA = "PORT_EXTRA";
	public static final String PATH_EXTRA = "PATH_EXTRA";
	public static final String URL_EXTRA = "URL_EXTRA";
	public static final String STATE_EXTRA = "STATE_EXTRA";
	public static final String MAC_EXTRA   = "MAC_EXTRA";
	public static final String POS_EXTRA	= "POS_EXTRA";
	public static final String AUDIO_FILENAME_EXTRA = "AUDIO_FILENAME_EXTRA";
	public static final String VIDEO_FILENAME_EXTRA = "VIDEO_FILENAME_EXTRA";
	public static final String ACTION_P2P_CONNECT = "com.dream.player.P2P_CONNECT";
	public static final String ACTION_P2P_DISCONNECT = "com.dream.player.P2P_DISCONNECT";
	public static final String ACTION_P2P_SETUP_TIMEOUT = "com.dream.share.P2P_SETUP_TIMEOUT";
	public static final String ACTION_PLAY_AUDIO = "com.dream.share.PALY_AUDIO";
	public static final String ACTION_PLAY_VIDEO = "com.dream.share.PLAY_VIDEO";
	public static final String ACTION_SHOW_IMAGE = "com.dream.share.SHOW_IMAGE";
	public static final String ACTION_START_KTV = "com.dream.share.START_KTV";
	public static final String ACTION_SET_PLAY   = "com.dream.share.SET_PLAY";
	public static final String ACTION_SEEK_PLAY   = "com.dream.share.SEEK_PLAY";
	public static final String ACTION_SET_VOICE_DOWN = "com.dream.share.SET_VOICE_DOWN";
	public static final String ACTION_SET_VOICE_UP = "com.dream.share.SET_VOICE_UP";
	public static void setFileName(String fName) {
		fileName = fName;
	}

	public static String getFileName() {
		if (fileName == null) {
			return "";
		}
		return fileName;
	}


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

	public static String supportedFileFormats[] =concat(supportedAudioFileFormats, supportedVideoFileFormats, supportedFontFileType);


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

	public static Boolean isAudioStream(String file){
		if (file==null) return false;
		String ext = file.toString();
		String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);
		if (Arrays.asList(supportedAudioStreamFileFormats).contains(sub_ext.toLowerCase())){
			return true; 
		}
		return false;
	}

	public static Boolean isVideoStream(String file){
		if (file ==null) return false;
		String ext = file.toString();
		String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);
		if (Arrays.asList(supportedVideoStreamFileFormats).contains(sub_ext.toLowerCase())){
			return true; 
		}

		return false;
	}
}