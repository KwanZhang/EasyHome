package com.dream.player.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.dream.player.Globals;
import com.dream.player.dlna.DlnaService;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * A single-connection HTTP server that will respond to requests for files and
 * pull them from the application's SD card.
 */
public class WifiDirectListener implements Runnable {
	private static final String TAG = "WifiDirectListener"; // LocalFileStreamingServer.class.getName();
	private int port = 0;
	private boolean isRunning = false;
	private ServerSocket socket;
	private Thread mMainThread;

	private Handler mHandler;
	/**
	 * This server accepts HTTP request and returns files from device.
	 */
	public WifiDirectListener(Handler handler) {
		mHandler = handler;
	}
	
	
	/**
	 * @return A port number assigned by the OS.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Prepare the server to start.
	 * 
	 * This only needs to be called once per instance. Once initialized, the
	 * server can be started and stopped as needed.
	 */
	public String init(String ip) {
		String url = null;
		try {
//			InetAddress inet = InetAddress.getByName(ip);
//			byte[] bytes = inet.getAddress();
//			socket = new ServerSocket(port, 0 , InetAddress.getByAddress(bytes));
			socket = new ServerSocket(port);
			port = socket.getLocalPort();
		} catch (UnknownHostException e) {
			Log.e(TAG, "Error UnknownHostException server", e);
		} catch (IOException e) {
			Log.e(TAG, "Error IOException server", e);
		}
		return url;
	}


	/**
	 * Start the server.
	 */
	public void start() {
		mMainThread = new Thread(this);
		mMainThread.start();
		isRunning = true;
	}

	/**
	 * Stop the server.
	 * 
	 * This stops the thread listening to the port. It may take up to five
	 * seconds to close the service and this call blocks until that occurs.
	 */
	public void stop() {
		isRunning = false;
		if (mMainThread == null) {
			Log.e(TAG, "Server was stopped without being started.");
			return;
		}
		Log.e(TAG, "Stopping server.");
		mMainThread.interrupt();
	}

	/**
	 * Determines if the server is running (i.e. has been <code>start</code>ed
	 * and has not been <code>stop</code>ed.
	 * 
	 * @return <code>true</code> if the server is running, otherwise
	 *         <code>false</code>
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * This is used internally by the server and should not be called directly.
	 */
	@Override
	public void run() {
		Log.e(TAG, "running");
		while (isRunning) {
			try {
				Socket client = socket.accept();
				if (client == null) {
					continue;
				}
				Log.e(TAG, "client connected at " + port);
				new WorkerThread(client).start();
			} catch (SocketTimeoutException e) {
				Log.e(TAG, "No client connected, waiting for client...", e);
				// Do nothing
			} catch (IOException e) {
				Log.e(TAG, "Error connecting to client", e);
				// break;
			}
		}
		Log.e(TAG, "Server interrupted or stopped. Shutting down.");
	}


	// One thread per client
	class WorkerThread extends Thread implements Runnable {

		private final Socket mClient;
		private final InputStream mInput;
		private Thread thread;
		// Each client has an associated session

		public WorkerThread(final Socket client) throws IOException {
			mInput = client.getInputStream();
			mClient = client;
		}
		public void start() {
			thread = new Thread(this);
			thread.start();
			isRunning = true;
		}
		public void stoped() {
			isRunning = false;
			if (thread == null) {
				Log.e(TAG, "Server was stopped without being started.");
				return;
			}
			Log.e(TAG, "Stopping server.");
			thread.interrupt();
		}
		public boolean isRunning() {
			return isRunning;
		}
		public void run() {
			while (isRunning) {
				try {
					processRequest(mClient);
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	
	/*
	 * Sends the HTTP response to the client, including headers (as applicable)
	 * and content.
	 */
	private void processRequest(Socket client) throws IllegalStateException, IOException {
		final int bufsize = 8192;
		byte[] buf = new byte[bufsize];
		int read = mInput.read(buf, 0, bufsize);
		String receiveStr = new String(buf,0,read);
		Log.d(TAG, "receiver str " + receiveStr);
		String in[] = receiveStr.split("\r\n");
		if(in.length < 2) return;
		Log.d(TAG, in[0] + in[1]);
		String actionName = in[0];
		if(actionName.equals("SetPlay")){
			String uri = null;
			if(in[1].startsWith("uri:")){
				uri = in[1].substring(4);
			}
			if(uri == null)
				return;
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putString(Globals.URL_EXTRA, uri);
			msg.setData(bundle);
			msg.what = DlnaService.PLAY_URI;
			mHandler.sendMessage(msg);
		}else if (actionName.equals("ShowImage")){
			String path = null;
			int width = 0;
			int height = 0;
			if (in[1].startsWith("filepath:")){
				path = in[1].substring(9);
			}
			if (in[2].startsWith("width:")){
				width = Integer.parseInt(in[2].substring(6));
			}
			if (in[3].startsWith("height:")){
				height = Integer.parseInt(in[3].substring(7));
			}
			Log.d(TAG,"receiver setup send image action width " + width + " height " 
					+ height + " path " + path);
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putString(Globals.PATH_EXTRA, path);
			bundle.putInt(Globals.IMAGE_WIDTH, width);
			bundle.putInt(Globals.IMAGE_HEIGHT, height);
			msg.setData(bundle);
			msg.what = DlnaService.SHOW_IMAGE;
			mHandler.sendMessage(msg);
		}else if (actionName.equals("SetupWifiDirect")){

		}else if (actionName.equals("SetPlayStatus")){
			int state = 0;
			if(in[1].startsWith("state:")){
				state = Integer.parseInt(in[1].substring(6));
			}
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(Globals.STATE_EXTRA, state);
			msg.setData(bundle);
			msg.what = DlnaService.SET_PLAY;
			mHandler.sendMessage(msg);

		}else if (actionName.equals("SetVoiceDown")){
			mHandler.sendEmptyMessage(DlnaService.SET_VOICE_DOWN);
		}else if (actionName.equals("SetVoiceUp")) {
			mHandler.sendEmptyMessage(DlnaService.SET_VOICE_UP);

		}else if (actionName.equals("RegisterStatusListener")){
			int port = 0; // action.getArgument("Port").getIntegerValue();
			String ip = null; // action.getArgument("IP").getValue();
			if(in[1].startsWith("Port:")){
				port = Integer.parseInt(in[1].substring(5));
			}

			if (in[2].startsWith("IP:")){
				ip = in[2].substring(3);
			}
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(Globals.PORT_EXTRA, port);
			bundle.putString(Globals.IP_EXTRA, ip);
			msg.setData(bundle);
			msg.what = DlnaService.REGISTER_STATUS_LISTNER;
			mHandler.sendMessage(msg);
		}else if (actionName.equals("GetInfo")){
			
		}
	}
	}

}
