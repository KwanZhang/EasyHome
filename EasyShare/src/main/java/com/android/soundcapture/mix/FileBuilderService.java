package com.android.soundcapture.mix;
/*
 * Remember to create a layout for SDK lower than 11.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;


import android.app.Notification;
import android.os.Environment;
import android.util.Log;

public class FileBuilderService {

	static String folderLocation =Environment.getExternalStorageDirectory().getPath()+"/Custom SoundClips/";
	boolean building = true;
	Notification note;
	Notification.Builder builder;
	
	long totalBytes = 0;
	long currentBytesWritten = 0;
	
	static int bytesPerMillisecond = (2 * 44100 * 16 / 8)/1000;
	static int BYTESIZE = 4194304;
	int BUFFSIZE = BYTESIZE/2; //4MB should be good enough for the standard buffer
	long BUFFSIZEMS = BUFFSIZE/bytesPerMillisecond;
	static int HEADERSIZE = 44;


	protected void mixFiles(String fileName, long mainFileTimeMS,
			String secondFileLocation, long startSecondaryFileTimeMS,
			long endSecondaryFileTimeMS, int secondaryFileVolume) throws IOException {
		// TODO Auto-generated method stub
		File mainFile = new File(folderLocation+fileName);
		RandomAccessFile mainFileRAF = new RandomAccessFile(mainFile,"rw");
		
		long totalTimeRemaining = endSecondaryFileTimeMS - startSecondaryFileTimeMS;
		long duration = totalTimeRemaining;
		long currentMainTime = mainFileTimeMS;
		long currentSecondaryTime = startSecondaryFileTimeMS;
		
		short[] clipData;
		short[] primaryData;
		
		while(totalTimeRemaining > 0){
			//Read clipFile into a buffer to append to the primary file
			
			long size = (endSecondaryFileTimeMS - currentSecondaryTime)*bytesPerMillisecond;
			
			if(size < BUFFSIZE){
				//Get left over PCM data
				
				//Determine if the file in question is a WAV or MP3 file
				if(secondFileLocation.contains(".mp3"))
					clipData = getMP3PCM(secondFileLocation, currentSecondaryTime, endSecondaryFileTimeMS);
				else
					clipData = getWAVPCM(secondFileLocation,currentSecondaryTime,endSecondaryFileTimeMS);
				
				//Combine the clip with the primary file
				primaryData = getWAVPCM(mainFileRAF,currentMainTime,mainFileTimeMS+duration);
				
				//Combine the clip with the primary file
				primaryData = addPCM(primaryData,clipData,secondaryFileVolume);
				currentBytesWritten = currentBytesWritten+(primaryData.length*2);
				
				//Remix the combined data
				writePCMToWAV(mainFileRAF,currentMainTime,mainFileTimeMS+duration,primaryData);
				
				currentMainTime = currentMainTime+duration;
				totalTimeRemaining = 0;
				mainFileRAF.close();
			}else{
				//Get BUFFSIZE of short data
				
				//Determine if the file in question is a WAV or MP3 file
				if(secondFileLocation.contains(".mp3"))
					clipData = getMP3PCM(secondFileLocation, currentSecondaryTime, currentSecondaryTime+BUFFSIZEMS);
				else
					clipData = getWAVPCM(secondFileLocation, currentSecondaryTime, currentSecondaryTime+BUFFSIZEMS);
				
				//grab the section of data from the primary file to mix on top
				primaryData = getWAVPCM(mainFileRAF,currentMainTime,currentMainTime+BUFFSIZEMS);
				
				//Combine the clip with the primary file
				primaryData = addPCM(primaryData,clipData,secondaryFileVolume);
				currentBytesWritten = currentBytesWritten+(primaryData.length*2);
				//Remix the combined data
				writePCMToWAV(mainFileRAF,currentMainTime,currentMainTime+BUFFSIZEMS,primaryData);
				
				currentSecondaryTime = currentSecondaryTime+BUFFSIZEMS;
				currentMainTime = currentMainTime+BUFFSIZEMS;
			}
		}
	}

	
	//Combines two short arrays of PCM data at the new data volume
	public static short[] addPCM (short[] mainData, short[] newData, int volume){
		
		int flow = 0;
		int j = 0;
		
		int length = 0;
		
		if(mainData.length > newData.length)
			length = newData.length;
		else
			length = mainData.length;
		
		
		double percentage = (double)volume/100;
		
		for(int i = 0; i< length; i++){
			
		    flow = (int) (mainData[i] + ((double)newData[j]*percentage));
            if(flow > 32767){
                flow = 32767;
            }
            else if (flow < -32768){
                flow = -32768;
            }
            mainData[i] = (short) flow;
            j++;
		}
		
		
		return mainData;
		
	}

	public static void writePCMToWAV(RandomAccessFile raf, long start_MS,long end_MS,short[] primaryData){
		
		try {
			byte[] rawBytes = shortsToBytes(primaryData);
			raf.seek(HEADERSIZE+(start_MS*bytesPerMillisecond));
			raf.write(rawBytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void writePCMToWAV(RandomAccessFile raf,byte[] primaryData){
		
		try {
			raf.write(primaryData);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    public static int calculateDecibel(short[] buf) {
        int sum = 0;
        byte[] rawBytes;
		try {
			rawBytes = shortsToBytes(buf);
	        int rawSize = rawBytes.length;
	        for (int i = 0; i < rawSize; i++) {
	            sum += Math.abs(rawBytes[i]);
	        }
	        return sum / rawSize;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // avg 10-50
        return 0;
    }
    public static int calculateDecibel(byte[] buf) {
        int sum = 0;
	
        int rawSize = buf.length;
        for (int i = 0; i < rawSize; i++) {
            sum += Math.abs(buf[i]);
        }
        return sum / rawSize;
    }
	//Get PCM data from the primary file while holding onto the file pointer
	//Return a short array of the selected section
	public static short[] getWAVPCM(RandomAccessFile raf, long start_MS, long end_MS){
		int byteBuffer = (int) ((end_MS - start_MS)*bytesPerMillisecond);
		byte[] rawDATA = new byte[byteBuffer];
		
		try {
			raf.seek((long) (HEADERSIZE+start_MS*bytesPerMillisecond));
			raf.read(rawDATA);
			short[] pcmData = bytesToShorts(rawDATA);
			
			return pcmData;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	//Get PCM data from the secondary clip.
	//Overloaded function that takes the file location instead of a RandomAccessFile pointer
	public static short[] getWAVPCM(String file, long start_MS, long end_MS){
		
		int byteBuffer = (int) ((end_MS - start_MS)*bytesPerMillisecond);
		byte[] rawDATA = new byte[byteBuffer];
		
		try {
			RandomAccessFile raf = new RandomAccessFile(new File(file),"rw");
			raf.seek((long) (HEADERSIZE+start_MS*bytesPerMillisecond));
			raf.read(rawDATA);
			short[] pcmData = bytesToShorts(rawDATA);
			raf.close();
			return pcmData;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static class MP3Info{
		public CheapMP3 mp3;
		public String file;
		public int frameCount;
	}
	public static MP3Info getMP3Info(String file){
		InputStream mp3Stream = null;
		MP3Info mp3Info = new MP3Info();
		mp3Info.mp3 = new CheapMP3();
		try {
			
			mp3Info.mp3.ReadFile(new File(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		mp3Info.file = file;
		mp3Info.frameCount = mp3Info.mp3.getNumFrames();
		return mp3Info;
	}
	public static short[] getMP3PCM(MP3Info mp3Info, float start_MS, float end_MS){
		Decoder d = new Decoder();
		int frame = 0;
		InputStream mp3Stream = null;
		short[] fullList = null;
		try {
			
			mp3Stream = new FileInputStream(mp3Info.file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Bitstream stream = new Bitstream(mp3Stream);
		try {
			
			Header header = stream.readFrame();
			
			float frameMS = header.ms_per_frame();
			
			int frame_Start = (int)(start_MS/frameMS); // Determine the frame at which we want to start converting
			int frame_End = (int)(end_MS/frameMS); // Determine the end frame at which we want to stop converting
			
			for ( ; frame < mp3Info.frameCount; frame++){
				
				if(header == null || frame > frame_End)
					break;
								
				if(frame >= frame_Start && frame < frame_End){
					SampleBuffer decoderOutput = (SampleBuffer)d.decodeFrame(header, stream);
					short[] pcm = decoderOutput.getBuffer();
					fullList = append(fullList,pcm);
				}
				
				stream.closeFrame();
				header = stream.readFrame();
			}
			stream.close();
		} catch (BitstreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("MP3","getMP3PCM get now " + System.currentTimeMillis());
		return fullList;
	}
	/*
	 * Returns pcm data between a selected number of milliseconds
	 * into a short array
	 */
	public static short[] getMP3PCM(String file, float start_MS, float end_MS){
		Log.d("Recorder","start_MS " + start_MS +  "  end_MS " + end_MS);
		//Open the mp3 file
		InputStream mp3Stream = null;
		int frameCount = 0;
		CheapMP3 mp3 = new CheapMP3();
		
		try {
			
			mp3.ReadFile(new File(file));
			mp3Stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		
		
		//Library used to get Raw PCM data from mp3 stream
		Decoder d = new Decoder();
		Bitstream stream = new Bitstream(mp3Stream);
		
		int frame = 0;
		System.out.println("Getting mp3 frame count and sampleRate = "+mp3.getSampleRate());
		
		frameCount = mp3.getNumFrames();
		System.out.println("Frame count = "+frameCount);
		//Begin getting PCM data from mp3 frames
		//The goal is to only collect frames we need to be added to eachother
		Log.d("MP3","getMP3PCM now " + System.currentTimeMillis());
		
		short[] fullList = null;
		
		try {
			
			
			Header header = stream.readFrame();
			
			float frameMS = header.ms_per_frame();
			
			int frame_Start = (int)(start_MS/frameMS); // Determine the frame at which we want to start converting
			int frame_End = (int)(end_MS/frameMS); // Determine the end frame at which we want to stop converting
			
			for ( ; frame < frameCount; frame++){
				
				if(header == null || frame > frame_End)
					break;
								
				if(frame >= frame_Start && frame < frame_End){
					SampleBuffer decoderOutput = (SampleBuffer)d.decodeFrame(header, stream);
					short[] pcm = decoderOutput.getBuffer();
					fullList = append(fullList,pcm);
				}
				
				stream.closeFrame();
				header = stream.readFrame();
			}
			stream.close();
		} catch (BitstreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("MP3","getMP3PCM get now " + System.currentTimeMillis());
		return fullList;
	}
	
	public static short[] append(short[] a, short[] b){
		
		if(a == null)
			return b;
		else{
			short[] newShort = new short[a.length + b.length];
			newShort = Arrays.copyOf(a, a.length + b.length);
			int length = a.length + b.length;
			int indexB = 0;
			
			for(int i =a.length; i < length; i++){
				newShort[i] = b[indexB];
				indexB++;
			}
			
			return newShort;
		}

	}
	
	public static byte[] toByteArray(char[] array) {
	    return toByteArray(array, Charset.defaultCharset());
	}

	public static byte[] toByteArray(char[] array, Charset charset) {
	    CharBuffer cbuf = CharBuffer.wrap(array);
	    ByteBuffer bbuf = charset.encode(cbuf);
	    return bbuf.array();
	}
	
	/*Creates a WAV file from PCM data
	 * This should be possibly considered to be done differently.
	 * Currently this only writes files of a specific freq as well as other hardcoded info
	 * Consider changing this.
	 * 
	 * Also consider making this function write portions instead of the whole thing at once
	 */
	static public boolean prepareFile(int duration, String file){

		
		try {

			OutputStream out = new FileOutputStream(folderLocation+file);
			
			
			writeId(out, "RIFF");
	        writeInt(out, 36 + bytesPerMillisecond*duration);
	        writeId(out, "WAVE");

	        /* fmt chunk */
	        writeId(out, "fmt ");
	        writeInt(out, 16);
	        writeShort(out, (short) 1);
	        writeShort(out, (short) 2);
	        writeInt(out, 44100);
	        writeInt(out, 2 * 44100 * 16 / 8);
	        writeShort(out, (short)(2 * 16 / 8));
	        writeShort(out, (short) 16);

	        /* data chunk */
	        writeId(out, "data");
	        writeInt(out, bytesPerMillisecond*duration);
	        
	        int bytesLeft = bytesPerMillisecond*duration;
	        byte[] bytes = new byte[BYTESIZE];
	        int writeAmount = BYTESIZE;
	        
	        while(bytesLeft != 0){
	        	
	        	if(bytesLeft < BYTESIZE){
	        		writeAmount = bytesLeft;
	        		bytes = new byte[writeAmount];
	        	}
	        	
	        	out.write(bytes);
	        	
	        	bytesLeft = bytesLeft - writeAmount;
	        	//Log.v("Bytes", " Bytes left  = "+ bytesLeft);
	        }
	        
	        return true;
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		} catch (IOException e2){
			e2.printStackTrace();
			return false;
		}
	}
	
	private static void writeId(OutputStream out, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) out.write(id.charAt(i));
    }

    private static void writeInt(OutputStream out, int val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
        out.write(val >> 16);
        out.write(val >> 24);
    }

    private static void writeShort(OutputStream out, short val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
    }
    
    public static byte[] shortsToBytes(short[] array) throws IOException{
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
		
		for (short s : array) {
            outStream.write(s & 0xff);
            outStream.write((s >> 8 ) & 0xff);
        }
		
		
		byte[] finalData = outStream.toByteArray();
		
		
		return finalData;
	}
    
    public static byte[] shortsToBytes(short[] array,int start, int end) throws IOException{
		short s;
    	if(end > array.length){
    		end = array.length;
    	}
    	
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
		
		for (int i = start; i < end; i++) {
			s = array[i];
            outStream.write(s & 0xff);
            outStream.write((s >> 8 ) & 0xff);
        }
		
		
		byte[] finalData = outStream.toByteArray();
		
		
		return finalData;
	}
    
    /*
	 * Converts a byte array into a short array BigEndian
	 * Do this to prevent more IO.
	 */
	public static short[] bytesToShorts(byte[] array){
		short[] shorts = new short[array.length/2];
		
		
		for(int i = 0; i < shorts.length; i++){
			shorts[i] = ( (short)( ( array[i*2] & 0xff )|( array[i*2 + 1] << 8 ) ) );
		}
		
		return shorts;
		
	}

}
