package com.dream.share.stream;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.dream.share.DLNAContainer;
import com.dream.share.Globals;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A single-connection HTTP server that will respond to requests for files and
 * pull them from the application's SD card.
 */
public class PlayerStatusListener implements Runnable {
	private static final String TAG = "StatusUpdatelistener"; // LocalFileStreamingServer.class.getName();
	private int port = 0;
	private boolean isRunning = false;
	private ServerSocket socket;
	private Thread thread;
	private Context mContext;
	/**
	 * This server accepts HTTP request and returns files from device.
	 */
	public PlayerStatusListener(Context context) {
		mContext = context;
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
			//InetAddress inet = InetAddress.getByName(ip);
			//byte[] bytes = inet.getAddress();
			socket = new ServerSocket(port, 0);//,InetAddress.getByAddress(bytes));

			//socket.setSoTimeout(20000);
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
		thread = new Thread(this);
		thread.start();
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
		if (thread == null) {
			Log.e(TAG, "Server was stopped without being started.");
			return;
		}
		Log.e(TAG, "Stopping server.");
		thread.interrupt();
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
				processRequest(client);
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

    private void sendP2PInfoBroadcast(String ip, int port){
			Intent intent = new Intent(Globals.ACTION_P2P_INFO);
			intent.putExtra(Globals.IP_EXTRA, ip);
			intent.putExtra(Globals.PORT_EXTRA, port);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			mContext.sendBroadcast(intent);
	}
	/*
	 * Sends the HTTP response to the client, including headers (as applicable)
	 * and content.
	 */
	private void processRequest(Socket client) throws IllegalStateException, IOException {
		InputStream is = client.getInputStream();
		final int bufsize = 8192;
		byte[] buf = new byte[bufsize];
		int read = is.read(buf, 0, bufsize);
		String receiveStr = new String(buf,0,read);
		Log.d(TAG, "receiver str " + receiveStr);
		String in[] = receiveStr.split("\r\n");
		if(in.length < 2) return;
		Log.d(TAG, in[0] + in[1]);
		if(in[0].equals("STATUS")){
			if(in[1].startsWith("Status:")){
				String status = in[1].substring(7);
				if(status.startsWith("Play")){
					DLNAContainer.getInstance().updateStutus(Globals.PLAYER_STATUS_PLAY, 0);
				}else if(status.startsWith("Pause")){
					DLNAContainer.getInstance().updateStutus(Globals.PLAYER_STATUS_PAUSE, 0);
				}else if(status.startsWith("Stop")){
					DLNAContainer.getInstance().updateStutus(Globals.PLAYER_STATUS_STOP, 0);
				}
			}
		}else if (in[0].equals("POSTION")){
			if(in[1].startsWith("Value:")){
				int pos = Integer.parseInt(in[1].substring(6));
				DLNAContainer.getInstance().updateStutus(Globals.PLAYER_POSTION_UPDATE, pos);
			}
		}else if (in[0].equals("P2PINFO")){
			String addr = null;
			int port = 0;
			if(in[1].startsWith("Host:")){
				addr = in[1].substring(5);
			}
			if(in[2].startsWith("Port:")){
				port = Integer.parseInt(in[2].substring(5));
			}
			Log.d(TAG, "host " + addr + " port " + port);
			if((addr != null)&&(port != 0)){
				sendP2PInfoBroadcast(addr, port);
			}
		}
	}


}