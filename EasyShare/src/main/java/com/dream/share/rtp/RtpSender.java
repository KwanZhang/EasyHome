package com.dream.share.rtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.StringTokenizer;

import com.android.soundcapture.Recorder;
import com.android.soundcapture.mix.FileBuilderService;
import com.dream.share.DMCApplication;
import com.dream.share.Globals;
import com.dream.share.stream.HttpServer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class RtpSender {
	private final static String TAG = "RtpSender";
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    //rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    final static int DESCRIBE = 7;
    final static int STOP = 7;
    
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    public final static int CMD_POLL_DATA = 1;
    static int state; //RTSP Server state == INIT or READY or PLAY
    
	private Handler mHandler;
	private int myTcpPort;
	private Socket mRTSPSocket;
	private InetAddress mClientIPAddr;   //Client IP address
	private  ServerSocket myServerSocket = null ;
	private Thread myThread;
    //input and output stream filters
    private BufferedReader RTSPBufferedReader;
    private BufferedWriter RTSPBufferedWriter;
    private RTSPSession mRTSPSession;
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    DatagramSocket RTCPsocket;
    //RtcpReceiver rtcpReceiver;
    int congestionLevel;
    private static int MAUDIO_TYPE = 10;
    static int RTSP_ID = 123456; //ID of the RTSP session
    static int RTCP_RCV_PORT = 19001; //port where the client will receive the RTP packets
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
    int RTP_dest_port = 0;      //destination port for RTP packets  (given by the RTSP Client)
    int RTSP_dest_port = 0;
    
    private String mFilePath = null;
    private Recorder mRecorder;
    private int frameNb = 0;
    private HandlerThread mRTPSenderThread;
    private Handler mRTPSenderHandler;
	public RtpSender(Handler handler){
		mHandler = handler;

		mRTPSenderThread = new HandlerThread("RTPSenderThread");
		mRTPSenderThread.start();
		mRTPSenderHandler = new Handler(mRTPSenderThread.getLooper()){
			@Override
			public void handleMessage(Message msg) {
				DatagramPacket  senddp;
				//byte[] rawBytes = null;
				if(CMD_POLL_DATA == msg.what){
					if(state != PLAYING)
						return;
					byte[] buffer = msg.getData().getByteArray("RecordData");
					int len = msg.getData().getInt("len");
					/*try {
						rawBytes = FileBuilderService.shortsToBytes(buffer);
					} catch (IOException e) {
						e.printStackTrace();
					}
					buffer = null;*/
	                //Builds an RTPpacket object containing the frame
	                RTPpacket rtp_packet = new RTPpacket(MAUDIO_TYPE, frameNb, frameNb*FRAME_PERIOD, 
	                		buffer, len);
	                
	                //get to total length of the full rtp packet to send
	                int packet_length = rtp_packet.getlength();

	                //retrieve the packet bitstream and store it in an array of bytes
	                byte[] packet_bits = new byte[packet_length];
	                rtp_packet.getpacket(packet_bits);

	                //send the packet as a DatagramPacket over the UDP socket 
	                senddp = new DatagramPacket(packet_bits, packet_length, mClientIPAddr, RTP_dest_port);
	                try {
						RTPsocket.send(senddp);
						frameNb++;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                
				}
				super.handleMessage(msg);
			}
		};
		mRecorder = new Recorder(mRTPSenderHandler);
		state = INIT;
		try {
			myServerSocket = new ServerSocket(0);
			myTcpPort = myServerSocket.getLocalPort();
			myThread = new Thread( new Runnable()
				{
					public void run()
					{
						try {
							mRTSPSocket = myServerSocket.accept();
							mRTSPSession = new RTSPSession(mRTSPSocket);
							mClientIPAddr = mRTSPSocket.getInetAddress();
							myServerSocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			myThread.setDaemon( true );
			myThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void setMixFile(String filePath){
		mFilePath = filePath;
	}
	public int getRtspPort(){
		return myTcpPort;
	}
	public void close(){
		state = STOP;
	}

	private class RTSPSession implements Runnable
	{
		private Socket mySocket;
		
		final static String CRLF = "\r\n";
		
		public RTSPSession( Socket s ) throws IOException
		{
			mySocket = s;
	        RTSPBufferedReader = new BufferedReader(new InputStreamReader(mySocket.getInputStream()) );
	        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(mySocket.getOutputStream()) );

			Thread t = new Thread( this );
			t.setDaemon( true );
			t.start();
		}

		public void run()
		{
	        //Wait for the SETUP message from the client
	        int request_type;
	        boolean done = false;
	        while(!done) {
	            request_type = parse_RTSP_request(); //blocking
	    
	            if (request_type == SETUP) {
	                done = true;

	                //update RTSP state
	                state = READY;
	                Log.d(TAG,"New RTSP state: READY");
	             
	                //Send response
	                send_SETUP_response();

	                //init RTP and RTCP sockets
	                try {
						RTPsocket = new DatagramSocket();
						RTCPsocket = new DatagramSocket(RTCP_RCV_PORT);
					} catch (SocketException e) {
						e.printStackTrace();
					}
	                
	            }
	        }
	        
	        //loop to handle RTSP requests
	        while(true) {
	            //parse the request
	            request_type = parse_RTSP_request(); //blocking
	                
	            if ((request_type == PLAY) && (state == READY)) {
	                //send back response
	                send_RTSP_response();
	                //start Record
	                if(mRecorder == null){
						mRecorder = new Recorder(mRTPSenderHandler);
					}
	                
	                mRecorder.setPlayFile(mFilePath);
	                mRecorder.startRecording(".wav");
	                /*if(rtcpReceiver == null){
	                	rtcpReceiver = new RtcpReceiver(400);
	                }
	                rtcpReceiver.startRcv();*/
	                //update state
	                state = PLAYING;
	                Log.d(TAG,"New RTSP state: PLAYING");
	            }
	            else if ((request_type == PAUSE) && (state == PLAYING)) {
	                //send back response
	                send_RTSP_response();
	                //stop Record
	                mRecorder.stop();
	                
	                //rtcpReceiver.stopRcv();
	                //update state
	                state = READY;
	                Log.d(TAG,"New RTSP state: READY");
	            }
	            else if (request_type == TEARDOWN) {
	                //send back response
	                send_RTSP_response();
	                //stop Recorder
	                mRecorder.stop();
	                //rtcpReceiver.stopRcv();
	                //close sockets
	                try {
						mySocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	                RTPsocket.close();
	            }
	            else if (request_type == DESCRIBE) {
	                Log.d(TAG,"Received DESCRIBE request");
	                send_RTSP_describe();
	            }
	        }
		}
	    //------------------------------------
	    //Parse RTSP Request
	    //------------------------------------
	    private int parse_RTSP_request()
	    {
	        int request_type = -1;
	        try { 
	            //parse request line and extract the request_type:
	            String RequestLine = RTSPBufferedReader.readLine();
	            Log.d(TAG,"RTSP Server - Received from Client:");
	            Log.d(TAG,RequestLine);

	            StringTokenizer tokens = new StringTokenizer(RequestLine);
	            String request_type_string = tokens.nextToken();

	            //convert to request_type structure:
	            if ((new String(request_type_string)).compareTo("SETUP") == 0)
	                request_type = SETUP;
	            else if ((new String(request_type_string)).compareTo("PLAY") == 0)
	                request_type = PLAY;
	            else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
	                request_type = PAUSE;
	            else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
	                request_type = TEARDOWN;
	            else if ((new String(request_type_string)).compareTo("DESCRIBE") == 0)
	                request_type = DESCRIBE;

	            if (request_type == SETUP) {
	                //extract VideoFileName from RequestLine
	                tokens.nextToken();
	            }

	            //parse the SeqNumLine and extract CSeq field
	            String SeqNumLine = RTSPBufferedReader.readLine();
	            Log.d(TAG,SeqNumLine);
	            tokens = new StringTokenizer(SeqNumLine);
	            tokens.nextToken();
	            RTSPSeqNb = Integer.parseInt(tokens.nextToken());
	        
	            //get LastLine
	            String LastLine = RTSPBufferedReader.readLine();
	            Log.d(TAG,LastLine);

	            tokens = new StringTokenizer(LastLine);
	            if (request_type == SETUP) {
	                //extract RTP_dest_port from LastLine
	                for (int i=0; i<3; i++)
	                    tokens.nextToken(); //skip unused stuff
	                RTP_dest_port = Integer.parseInt(tokens.nextToken());
	            }
	            else if (request_type == DESCRIBE) {
	                tokens.nextToken();
	                String describeDataType = tokens.nextToken();
	            }
	            else {
	                //otherwise LastLine will be the SessionId line
	                tokens.nextToken(); //skip Session:
	                RTSP_ID = Integer.parseInt(tokens.nextToken());
	            }
	        } catch(Exception ex) {
	            Log.d(TAG,"Exception caught: "+ex);
	            System.exit(0);
	        }
	      
	        return(request_type);
	    }

	    // Creates a DESCRIBE response string in SDP format for current media
	    private String describe() {
	        StringWriter writer1 = new StringWriter();
	        StringWriter writer2 = new StringWriter();
	        
	        // Write the body first so we can get the size later
	        writer2.write("v=0" + CRLF);
	        writer2.write("m=video " + RTSP_dest_port + " RTP/AVP " + "null" + CRLF);
	        writer2.write("a=control:streamid=" + RTSP_ID + CRLF);
	        writer2.write("a=mimetype:string;\"audio/MJPEG\"" + CRLF);
	        String body = writer2.toString();

	        writer1.write("Content-Base: " + "null" + CRLF);
	        writer1.write("Content-Type: " + "application/sdp" + CRLF);
	        writer1.write("Content-Length: " + body.length() + CRLF);
	        writer1.write(body);
	        
	        return writer1.toString();
	    }

	    //------------------------------------
	    //Send RTSP Response
	    //------------------------------------
	    private void send_RTSP_response() {
	        try {
	            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
	            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
	            RTSPBufferedWriter.write("Session: "+RTSP_ID+CRLF);
	            RTSPBufferedWriter.flush();
	            Log.d(TAG,"RTSP Server - Sent response to Client.");
	        } catch(Exception ex) {
	            Log.d(TAG,"Exception caught: "+ex);
	        }
	    }
	    private void send_SETUP_response() {
	        try {
	            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
	            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
	            RTSPBufferedWriter.write("Session: "+RTSP_ID+CRLF);
	            RTSPBufferedWriter.write("SampleRate: " + Globals.mSampleRate+CRLF);
	            RTSPBufferedWriter.write("ChannelConfig: " + Globals.mChannelConfig+CRLF);
	            RTSPBufferedWriter.write("AudioFormat: " + Globals.mAudioFormat+CRLF);
	            RTSPBufferedWriter.write("BufferSize: " + mRecorder.getBufferSizeInBytes()+CRLF);
	            RTSPBufferedWriter.write("RecordVoice: " + Globals.mRecordVoice+CRLF);
	            RTSPBufferedWriter.write("MusicVoice: " + Globals.mMusicVoice+CRLF);
	            HttpServer fileserver = DMCApplication.getInstance().getFileServer();
	    		String uri = null;
	    		if(fileserver != null){
	    			uri = fileserver.getFileUrl(mFilePath);
	    		}
	    		RTSPBufferedWriter.write("Uri: " + uri +CRLF);
	            RTSPBufferedWriter.flush();
	            Log.d(TAG,"RTSP Server - Sent response to Client.");
	        } catch(Exception ex) {
	            Log.d(TAG,"Exception caught: "+ex);
	        }
	    }
	    private void send_RTSP_describe() {
	        String des = describe();
	        try {
	            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
	            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
	            RTSPBufferedWriter.write(des);
	            RTSPBufferedWriter.flush();
	            Log.d(TAG,"RTSP Server - Sent response to Client.");
	        } catch(Exception ex) {
	            Log.d(TAG,"Exception caught: "+ex);
	        }
	    }
	
	}
    //------------------------
    //Listener for RTCP packets sent from client
    //------------------------
    class RtcpReceiver{
        private byte[] rtcpBuf;
    	private HandlerThread mRtcpSenderThread;
    	private Handler mRtcpSenderhandler;
        int interval;

        public RtcpReceiver(int interval) {
            //set timer with interval for receiving packets
            this.interval = interval;

            //allocate buffer for receiving RTCP packets
            rtcpBuf = new byte[512];
            mRtcpSenderThread = new HandlerThread("RtcpReceiver");
            mRtcpSenderThread.start();
            mRtcpSenderhandler = new Handler(mRtcpSenderThread.getLooper());
        }
        private Runnable mRtcpSender = new Runnable(){
    		@Override
    		public void run() {
                //Construct a DatagramPacket to receive data from the UDP socket
                DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
                float fractionLost;
                
                try {
                    RTCPsocket.receive(dp);   // Blocking
                    RTCPpacket rtcpPkt = new RTCPpacket(dp.getData(), dp.getLength());
                    System.out.println("[RTCP] " + rtcpPkt);

                    //set congestion level between 0 to 4
                    fractionLost = rtcpPkt.fractionLost;
                    if (fractionLost >= 0 && fractionLost <= 0.01) {
                        congestionLevel = 0;    //less than 0.01 assume negligible
                    }
                    else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                        congestionLevel = 1;
                    }
                    else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                        congestionLevel = 2;
                    }
                    else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                        congestionLevel = 3;
                    }
                    else {
                        congestionLevel = 4;
                    }
                }
                catch (InterruptedIOException iioe) {
                    System.out.println("Nothing to read");
                }
                catch (IOException ioe) {
                    System.out.println("Exception caught: "+ioe);
                }
                mRtcpSenderhandler.postDelayed(mRtcpSender, interval);
    		}
        };

        public void startRcv() {
        	mRtcpSenderhandler.post(mRtcpSender);
        }

        public void stopRcv() {
        	mRtcpSenderhandler.removeCallbacks(mRtcpSender);
        	mRtcpSenderThread.stop();
        }
    }
}
