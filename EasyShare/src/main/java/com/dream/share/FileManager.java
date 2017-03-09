package com.dream.share;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;


import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.dream.share.R;
import com.dream.share.TFile.Builder;
import com.dream.share.TFile.MimeType;
import com.dream.share.util.FileUtils;

/***
 * 本地文件管理器
 * 
 * @author zhanglei
 *
 */
public class FileManager {
	private static final String TAG = "FileManager";
	public static final int DEFAULT_MAX_CHOOSED_CNT = 5;//一次最多选5个文件(默认)
	public static final long DEFAULT_MAX_FILESIZE = 10*1024*1024;//单个最大文件为10M(默认)
	
	private int maxFileCnt = DEFAULT_MAX_CHOOSED_CNT ;
	private long maxFileSize = DEFAULT_MAX_FILESIZE;
	
	private static FileManager instance;
	private final Map<String ,MimeType> map;//mimeType集合
	private final Map<MimeType,Integer> resMap;//mimeType对应图片资源集合
	private final List<TFile> choosedFiles;//已选择文件集合
	public static FileManager getInstance(){
		if(null == instance){
			instance = new FileManager();
		}
		return instance;
	}
	
	/**
	 * 恢复默认文件选择设置
	 */
	public void reSetDefaultConfiguration(){
		maxFileCnt = DEFAULT_MAX_CHOOSED_CNT ;
		maxFileSize = DEFAULT_MAX_FILESIZE;
	}
	
	/***
	 * 重新配置文件选择设置
	 * @param maxChoosedCnt
	 * @param maxFileSize
	 */
	public void initConfiguration(int maxChoosedCnt , long maxFileSize){
		if(maxChoosedCnt>0)
			this.maxFileCnt = maxChoosedCnt;
		if(maxFileSize>0)
			this.maxFileSize = maxFileSize;
	}
	
	public int getMaxFileCnt() {
		return maxFileCnt;
	}

	public long getMaxFileSize() {
		return maxFileSize;
	}
	
	//初始化数据
	private FileManager(){
		map = new HashMap<String,MimeType>();
		map.put(".amr", MimeType.MUSIC);
		map.put(".mp3", MimeType.MUSIC);
		map.put(".ogg", MimeType.MUSIC);
		map.put(".wav", MimeType.MUSIC);
		map.put(".3gp", MimeType.VIDEO);
		map.put(".mp4", MimeType.VIDEO);
		map.put(".rmvb", MimeType.VIDEO);
		map.put(".mpeg", MimeType.VIDEO);
		map.put(".mpg", MimeType.VIDEO);
		map.put(".asf", MimeType.VIDEO);
		map.put(".avi", MimeType.VIDEO);
		map.put(".wmv", MimeType.VIDEO);
		map.put(".apk", MimeType.APK);
		map.put(".bmp", MimeType.IMAGE);
		map.put(".gif", MimeType.IMAGE);
		map.put(".jpeg", MimeType.IMAGE);
		map.put(".jpg", MimeType.IMAGE);
		map.put(".png", MimeType.IMAGE);
		map.put(".doc", MimeType.DOC);
		map.put(".docx", MimeType.DOC);
		map.put(".rtf", MimeType.DOC);
		map.put(".wps", MimeType.DOC);
		map.put(".xls", MimeType.XLS);
		map.put(".xlsx", MimeType.XLS);
		map.put(".gtar", MimeType.RAR);
		map.put(".gz", MimeType.RAR);
		map.put(".zip", MimeType.RAR);
		map.put(".tar", MimeType.RAR);
		map.put(".rar", MimeType.RAR);
		map.put(".jar", MimeType.RAR);
		map.put(".htm", MimeType.HTML);
		map.put(".html", MimeType.HTML);
		map.put(".xhtml", MimeType.HTML);
		map.put(".java", MimeType.TXT);
		map.put(".txt", MimeType.TXT);
		map.put(".xml", MimeType.TXT);
		map.put(".log", MimeType.TXT);
		map.put(".pdf", MimeType.PDF);
		map.put(".ppt", MimeType.PPT);
		map.put(".pptx", MimeType.PPT);
		
		resMap = new HashMap<MimeType,Integer>();
		resMap.put(MimeType.APK, R.drawable.bxfile_file_apk);
		resMap.put(MimeType.DOC, R.drawable.bxfile_file_doc);
		resMap.put(MimeType.HTML, R.drawable.bxfile_file_html);
		resMap.put(MimeType.IMAGE, R.drawable.bxfile_file_unknow);
		resMap.put(MimeType.MUSIC, R.drawable.bxfile_file_mp3);
		resMap.put(MimeType.VIDEO, R.drawable.bxfile_file_video);
		resMap.put(MimeType.PDF, R.drawable.bxfile_file_pdf);
		resMap.put(MimeType.PPT, R.drawable.bxfile_file_ppt);
		resMap.put(MimeType.RAR, R.drawable.bxfile_file_zip);
		resMap.put(MimeType.TXT, R.drawable.bxfile_file_txt);
		resMap.put(MimeType.XLS, R.drawable.bxfile_file_xls);
		resMap.put(MimeType.UNKNOWN, R.drawable.bxfile_file_unknow);
		
		choosedFiles = new ArrayList<TFile>();
	}
	
	public MimeType getMimeType(String exspansion){
		return map.get(exspansion.toLowerCase());
	}
	public Integer getMimeDrawable(MimeType type){
		return resMap.get(type);
	}

	//已选择文件集合
	public List<TFile> getChoosedFiles() {
		return choosedFiles;
	}
	
	//已选择文件大小 
	public String getFilesSizes(){
		long sum = 0;
		for(TFile f:choosedFiles){
			sum+=f.getFileSize();
		}
		return FileUtils.getFileSizeStr(sum);
	}
	
	//已选择文件数
	public int getFilesCnt(){
		return choosedFiles.size();
	}
	
	public void clear(){
		choosedFiles.clear();
	}
	
	//一次最多选10个文件
	public boolean isOverMaxCnt(){
		return getFilesCnt()>=maxFileCnt;
	}
	private List<TFile>  getAudio(Activity cxt) {
		Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; //uri to sd-card
		String[] selection = new String[] {
				android.provider.MediaStore.Audio.Media._ID,
				android.provider.MediaStore.Audio.Media.ALBUM_ID,
				android.provider.MediaStore.Audio.Media.TITLE,
				android.provider.MediaStore.Audio.Media.DATA,
				android.provider.MediaStore.Audio.Media.ARTIST,
				android.provider.MediaStore.Audio.Media.ALBUM,
				android.provider.MediaStore.Audio.Media.DURATION,
				android.provider.MediaStore.Audio.Media.DISPLAY_NAME
		};
		Cursor mCursor = cxt.managedQuery(uri, selection,null, null, null);
		cxt.startManagingCursor(mCursor);
		int count = mCursor.getCount();
		List<TFile> data = new ArrayList<TFile>();
		while(mCursor.moveToNext()) {
			
			long songId = mCursor.getLong(0);
			long albumId = mCursor.getLong(1);
			String path = mCursor.getString(3);
			String artist = mCursor.getString(mCursor.getColumnIndexOrThrow("ARTIST"));
			String title = mCursor.getString(mCursor.getColumnIndexOrThrow("TITLE"));
			String album = mCursor.getString(mCursor.getColumnIndexOrThrow("ALBUM"));
			String duration = mCursor.getString(mCursor.getColumnIndexOrThrow("DURATION"));
			String displayName = mCursor.getString(mCursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME));
			TFile.Builder builder = new TFile.Builder(path,artist,
					title, album, duration, displayName,songId, albumId);
			TFile bxfile = builder.build();
			Log.d(TAG, "path " + path);
			if(null != bxfile)
				data.add(bxfile);
		}	     	   
		return data;
	}
	private List<TFile>  getImage(Activity cxt) {
		Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI; //uri to sd-card
		String[] selection = {  MediaStore.Images.Media.DATA,
					MediaStore.Images.Media.WIDTH,
				 MediaStore.Images.Media.HEIGHT};
		Cursor mCursor = cxt.managedQuery(uri, selection,null, null, null);
		cxt.startManagingCursor(mCursor);
		int count = mCursor.getCount();
		List<TFile> data = new ArrayList<TFile>();
		while(mCursor.moveToNext()) {

			String path = mCursor.getString(mCursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
			int width = mCursor.getInt(mCursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
			int height = mCursor.getInt(mCursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
			TFile.Builder builder = new TFile.Builder(path,width,height);
			TFile bxfile = builder.build();
			if(null != bxfile)
				data.add(bxfile);
		}	     	   
		return data;
	}
	//查找external多媒体文件
	public synchronized List<TFile> getMediaFiles(Activity cxt , Uri uri) {
		Log.d(TAG, "uri " + uri + " " + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		if( MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.equals(uri)){
			Log.d(TAG, "MediaStore.Audio.Media.EXTERNAL_CONTENT_URI");
			return getAudio(cxt);
		}else if(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.equals(uri)){
			Log.d(TAG, "MediaStore.Images.Media.EXTERNAL_CONTENT_URI");
			return getImage(cxt);
		}
		Cursor mCursor = cxt.managedQuery(
				uri,
				new String[] {MediaStore.Audio.Media.DATA}, null,
				null, " date_modified desc");
		cxt.startManagingCursor(mCursor);
		int count = mCursor.getCount();
		if(count>0){
			List<TFile> data = new ArrayList<TFile>();
			if (mCursor.moveToFirst()) {
				do {
					TFile.Builder builder = new TFile.Builder(mCursor.getString(0));
					TFile bxfile = builder.build();
					if(null != bxfile)
						data.add(bxfile);
				} while (mCursor.moveToNext());
			}
			return data;
		}else{
			return null;
		}
	}
	
	//external多媒体文件计数
	public synchronized int getMediaFilesCnt(Activity cxt , Uri uri) {
		Cursor mCursor = cxt.managedQuery(
				uri,
				new String[] {MediaStore.Audio.Media.DATA}, null,
				null, null);
		cxt.startManagingCursor(mCursor);
		int cnt = mCursor.getCount();
		return cnt;
	}
	public static Bitmap getArtwork(Context context, long song_id, long album_id,
            boolean allowdefault) {
        if (album_id < 0) {
            // This is something that is not in the database, so get the album art directly
            // from the file.
            if (song_id >= 0) {
                Bitmap bm = getArtworkFromFile(context, song_id, -1);
                if (bm != null) {
                    return bm;
                }
            }
            if (allowdefault) {
                return getDefaultArtwork(context);
            }
            return null;
        }
        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                return BitmapFactory.decodeStream(in, null, sBitmapOptions);
            } catch (FileNotFoundException ex) {
                // The album art thumbnail does not actually exist. Maybe the user deleted it, or
                // maybe it never existed to begin with.
                Bitmap bm = getArtworkFromFile(context, song_id, album_id);
                if (bm != null) {
                    if (bm.getConfig() == null) {
                        bm = bm.copy(Bitmap.Config.RGB_565, false);
                        if (bm == null && allowdefault) {
                            return getDefaultArtwork(context);
                        }
                    }
                } else if (allowdefault) {
                    bm = getDefaultArtwork(context);
                }
                return bm;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                }
            }
        }
        
        return null;
    }
    
    private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {
        Bitmap bm = null;
        if (albumid < 0 && songid < 0) {
            throw new IllegalArgumentException("Must specify an album or a song id");
        }
        try {
            if (albumid < 0) {
                Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            } else {
                Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            }
        } catch (FileNotFoundException ex) {
        	
        }
        if (bm != null) {
            mCachedBit = bm;
        }
        return bm;
    }
    
    private static Bitmap getDefaultArtwork(Context context) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;        
        return BitmapFactory.decodeStream(
                context.getResources().openRawResource(R.drawable.maron5), null, opts);               
    }
private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    private static Bitmap mCachedBit = null;
}
