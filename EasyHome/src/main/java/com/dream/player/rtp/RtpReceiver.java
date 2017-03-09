package com.dream.player.rtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Timer;

import com.dream.player.AudioTrackActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;

public class RtpReceiver {
	private final static String TAG = "RtpReceiver";
	
    //RTP variables:
    //----------------
    DatagramPacket rcvdp; //UDP packet received from the server
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets
    
    Timer timer; //timer used to receive data from the UDP socket
    byte[] buf = new byte[10000];; //buffer used to store data received from the server 
   
    //RTSP variables
    //----------------
    //rtsp states 
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    static int state; //RTSP state == INIT or READY or PLAYING
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    InetAddress ServerIPAddr;
    int RTSP_server_port;
    //input and output stream filters
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file to request to the server
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
    int RTSPid = 0; // ID of the RTSP session (given by the RTSP Server)

    final static String CRLF = "\r\n";
    final static String DES_FNAME = "session_info.txt";

    //RTCP variables
    //----------------
    DatagramSocket RTCPsocket;          //UDP socket for sending RTCP packets
    static int RTCP_RCV_PORT = 19001;   //port where the client will receive the RTP packets
    static int RTCP_PERIOD = 400;       //How often to send RTCP packets
    //RtcpSender rtcpSender;

    //Video constants:
    //------------------
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video

    //Statistics variables:
    //------------------
    double statDataRate;        //Rate of video data received in bytes/s
    int statTotalBytes;         //Total number of bytes received in a session
    double statStartTime;       //Time in milliseconds when start is pressed
    double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
    float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
    int statCumLost;            //Number of packets lost
    int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
    int statHighSeqNb;          //Highest sequence number received in session

    FrameSynchronizer fsynch;
 
	
	private AudioTrackActivity.PlayerHandler mHandler;
	public RtpReceiver(AudioTrackActivity.PlayerHandler handler,String host, int port) throws IOException{
		mHandler = handler;
		
		ServerIPAddr = InetAddress.getByName(host);
		RTSP_server_port = port;
        //create the frame synchronizer
        fsynch = new FrameSynchronizer();
        //init RTSP state:
        state = INIT;
        mHandler.sendEmptyMessage(AudioTrackActivity.RTSP_SETUP);
	}
	private boolean runFlag = true;
	private Thread myThread = new Thread( new Runnable()
	{
		public void run()
		{
			while(runFlag)
				RTPRecieve();
		}
	});

	public void close(){
		runFlag = false;
		RTPsocket.close();
		try {
			myThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    public void RTPRecieve() {
      
        //Construct a DatagramPacket to receive data from the UDP socket
        rcvdp = new DatagramPacket(buf, buf.length);

        try {
            //receive the DP from the socket, save time for stats
            RTPsocket.receive(rcvdp);

            double curTime = System.currentTimeMillis();
            statTotalPlayTime += curTime - statStartTime; 
            statStartTime = curTime;

            //create an RTPpacket object from the DP
            RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
            int seqNb = rtp_packet.getsequencenumber();

            //this is the highest seq num received



            //get the payload bitstream from the RTPpacket object
            int payload_length = rtp_packet.getpayload_length();
            byte [] payload = new byte[payload_length];
            rtp_packet.getpayload(payload);
            //print important header fields of the RTP packet received: 
//            Log.d(TAG,"Got RTP packet with SeqNum # " + seqNb
//                               + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
//                               + rtp_packet.getpayloadtype() + "len " + rcvdp.getLength() +
//                               " payload_length " + payload_length);

            //print header bitstream:
            //rtp_packet.printheader();
            //compute stats and update the label in GUI
            statExpRtpNb++;
            if (seqNb > statHighSeqNb) {
                statHighSeqNb = seqNb;
            }
            if (statExpRtpNb != seqNb) {
                statCumLost++;
            }
            statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
            statFractionLost = (float)statCumLost / statHighSeqNb;
            statTotalBytes += payload_length;
            fsynch.addFrame(payload,payload_length,seqNb);
            mHandler.sendEmptyMessage(AudioTrackActivity.FILL_DATA);
        }
        catch (InterruptedIOException iioe) {
            Log.d(TAG,"Nothing to read");
        }
        catch (IOException ioe) {
            Log.d(TAG,"Exception caught: "+ioe);
        }
    }
    public byte[] getData(){
    	if(fsynch.isEmpty())
    		return null;
    	return fsynch.nextFrame();
    }
	public void RTPSetup() throws IOException{
    Log.d(TAG,"Setup Button pressed !");      
	RTSPsocket = new Socket(ServerIPAddr, RTSP_server_port);

    //Set input and output stream filters:
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));
    if (state == INIT) {
        //Init non-blocking RTPsocket that will be used to receive data
        try {
            //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
            RTPsocket = new DatagramSocket(RTP_RCV_PORT);
            //UDP socket for sending QoS RTCP packets
            RTCPsocket = new DatagramSocket();
            //set TimeOut value of the socket to 5msec.
            //RTPsocket.setSoTimeout(50);
        }
        catch (SocketException se)
        {
            Log.d(TAG,"Socket exception: "+se);
            
        }

        //init RTSP sequence number
        RTSPSeqNb = 1;

        //Send SETUP message to the server
        send_RTSP_request("SETUP");

        //Wait for the response 
        if (parse_setup_response() != 200)
            Log.d(TAG,"Invalid Server Response");
        else 
        {
            //change RTSP state and print new state 
            state = READY;
            //mHandler.sendEmptyMessage(AudioTrackActivity.START_PLAY_PCM);
            mHandler.sendEmptyMessage(AudioTrackActivity.RTSP_PLAY);
            
            Log.d(TAG,"New RTSP state: READY");
        }
    }
    //else if state != INIT then do nothing
	}
	public void RTPPlay(){
            Log.d(TAG,"Play Button pressed!"); 

            //Start to save the time in stats
            statStartTime = System.currentTimeMillis();

            if (state == READY) {
                //increase RTSP sequence number
                RTSPSeqNb++;

                //Send PLAY message to the server
                send_RTSP_request("PLAY");

                //Wait for the response 
                if (parse_server_response() != 200) {
                    Log.d(TAG,"Invalid Server Response");
                }
                else {
                    //change RTSP state and print out new state
                    state = PLAYING;
                    Log.d(TAG,"New RTSP state: PLAYING");

                    //start the timer
                    myThread.start();
                    /*if(rtcpSender == null){
                    	rtcpSender = new RtcpSender(400);
                    }
                    rtcpSender.startSend();*/
                }
            }
            //else if state != READY then do nothing
        }
	private void RTPPause(){
            Log.d(TAG,"Pause Button pressed!");   

            if (state == PLAYING) 
            {
                //increase RTSP sequence number
                RTSPSeqNb++;

                //Send PAUSE message to the server
                send_RTSP_request("PAUSE");

                //Wait for the response 
                if (parse_server_response() != 200)
                    Log.d(TAG,"Invalid Server Response");
                else 
                {
                    //change RTSP state and print out new state
                    state = READY;
                    Log.d(TAG,"New RTSP state: READY");
                      
                    //stop the timer
                    mHandler.sendEmptyMessage(AudioTrackActivity.STOP_PLAY_PCM);
                    //timer.stop();
                    //rtcpSender.stopSend();
                }
            }
            //else if state != PLAYING then do nothing
	}


    public void RTPTeardown(){

        Log.d(TAG,"Teardown Button pressed !");  

        //increase RTSP sequence number
        RTSPSeqNb++;

        //Send TEARDOWN message to the server
        send_RTSP_request("TEARDOWN");

        //Wait for the response 
        if (parse_server_response() != 200)
            Log.d(TAG,"Invalid Server Response");
        else {     
            //change RTSP state and print out new state
            state = INIT;
            Log.d(TAG,"New RTSP state: INIT");

            //stop the timer
            //timer.stop();
            mHandler.sendEmptyMessage(AudioTrackActivity.STOP_PLAY_PCM);
            //rtcpSender.stopSend();

            //exit
            
        }
    }


    public void RTPDescribe() {
        Log.d(TAG,"Sending DESCRIBE request");  

        //increase RTSP sequence number
        RTSPSeqNb++;

        //Send DESCRIBE message to the server
        send_RTSP_request("DESCRIBE");

        //Wait for the response 
        if (parse_server_response() != 200) {
            Log.d(TAG,"Invalid Server Response");
        }
        else {     
            Log.d(TAG,"Received response for DESCRIBE");
        }
    }
    //------------------------------------
    //Parse Server Response
    //------------------------------------
    private int parse_server_response() 
    {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = RTSPBufferedReader.readLine();
            Log.d(TAG,"RTSP Client - Received from Server:");
            Log.d(TAG,StatusLine);
          
            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());
            
            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = RTSPBufferedReader.readLine();
                Log.d(TAG,SeqNumLine);
                
                String SessionLine = RTSPBufferedReader.readLine();
                Log.d(TAG,SessionLine);

                tokens = new StringTokenizer(SessionLine);
                String temp = tokens.nextToken();
                //if state == INIT gets the Session Id from the SessionLine
                if (state == INIT && temp.compareTo("Session:") == 0) {
                    RTSPid = Integer.parseInt(tokens.nextToken());
                }
                else if (temp.compareTo("Content-Base:") == 0) {
                    // Get the DESCRIBE lines
                    String newLine;
                    for (int i = 0; i < 6; i++) {
                        newLine = RTSPBufferedReader.readLine();
                        Log.d(TAG,newLine);
                    }
                }
            }
        } catch(Exception ex) {
            Log.d(TAG,"Exception caught: "+ex);
            
        }
      
        return(reply_code);
    }

    private int parse_setup_response() 
    {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = RTSPBufferedReader.readLine();
            Log.d(TAG,"RTSP Client - Received from Server:");
            Log.d(TAG,StatusLine);
          
            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());
            
            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = RTSPBufferedReader.readLine();
                Log.d(TAG,SeqNumLine);
                
                String SessionLine = RTSPBufferedReader.readLine();
                Log.d(TAG,SessionLine);

                tokens = new StringTokenizer(SessionLine);
                String temp = tokens.nextToken();
                //if state == INIT gets the Session Id from the SessionLine
                if (state == INIT && temp.compareTo("Session:") == 0) {
                    RTSPid = Integer.parseInt(tokens.nextToken());
                }
                else if (temp.compareTo("Content-Base:") == 0) {
                    // Get the DESCRIBE lines
                    String newLine;
                    for (int i = 0; i < 6; i++) {
                        newLine = RTSPBufferedReader.readLine();
                        Log.d(TAG,newLine);
                    }
                }
                String config,prefix;
                int SampleRate = 0,ChannelConfig = 0,AudioFormat = 0,BufferSize = 0;
                int RecordVoice = 0;
                int MusicVoice = 0;
                String uri = null;
                for(int i =0 ; i < 7; i++){
                	config = RTSPBufferedReader.readLine();
                    Log.d(TAG, config);
                    tokens = new StringTokenizer(config);
                    prefix = tokens.nextToken();
                    if(prefix.compareTo("SampleRate:") == 0){
                    	SampleRate = Integer.parseInt(tokens.nextToken());
                    }else if(prefix.compareTo("ChannelConfig:") == 0){
                    	ChannelConfig = Integer.parseInt(tokens.nextToken());
                    }else if(prefix.compareTo("AudioFormat:") == 0){
                    	AudioFormat = Integer.parseInt(tokens.nextToken());
                    }else if(prefix.compareTo("BufferSize:") == 0){
                    	BufferSize = Integer.parseInt(tokens.nextToken());
                    }else if(prefix.compareTo("Uri:") == 0){
                    	uri = tokens.nextToken();
                    }else if (prefix.compareTo("RecordVoice:") == 0){
                    	RecordVoice = Integer.parseInt(tokens.nextToken());
                    }else if(prefix.compareTo("MusicVoice:") == 0){
                    	MusicVoice = Integer.parseInt(tokens.nextToken());
                    }
                }
                Log.d(TAG,"SampleRate: " + SampleRate + " BufferSize:" + BufferSize + " Uri " 
                		+uri + " RecordVoice: " + RecordVoice + " MusicVoice: " + MusicVoice);
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putInt("SampleRate", SampleRate);
                data.putInt("ChannelConfig", ChannelConfig);
                data.putInt("AudioFormat", AudioFormat);
                data.putInt("BufferSize", BufferSize);
                data.putString("Uri", uri);
                data.putInt("RecordVoice", RecordVoice);
                data.putInt("MusicVoice", MusicVoice);
                
                msg.setData(data);
                msg.what = AudioTrackActivity.START_PLAY_PCM;
                mHandler.sendMessage(msg);
                
            }
        } catch(Exception ex) {
            Log.d(TAG,"Exception caught: "+ex);
            
        }
      
        return(reply_code);
    }
    //------------------------------------
    //Send RTSP Request
    //------------------------------------

    private void send_RTSP_request(String request_type)
    {
        try {
            //Use the RTSPBufferedWriter to write to the RTSP socket

            //write the request line:
            RTSPBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + CRLF);

            //write the CSeq line: 
            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

            //check if request_type is equal to "SETUP" and in this case write the 
            //Transport: line advertising to the server the port used to receive 
            //the RTP packets RTP_RCV_PORT
            if (request_type == "SETUP") {
                RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
            }
            else if (request_type == "DESCRIBE") {
                RTSPBufferedWriter.write("Accept: application/sdp" + CRLF);
            }
            else {
                //otherwise, write the Session line from the RTSPid field
                RTSPBufferedWriter.write("Session: " + RTSPid + CRLF);
            }

            RTSPBufferedWriter.flush();
        } catch(Exception ex) {
            Log.d(TAG,"Exception caught: "+ex);
            
        }
    }    
    //------------------------------------
    //Synchronize frames
    //------------------------------------
    class RtpNote{
    	public byte[] data;
    	public int len;
    	public int seqNb;
    }
    class FrameSynchronizer{
    	private LinkedList<RtpNote> rtpQ;
    	private int lastSeq;
    	public FrameSynchronizer(){
    		rtpQ = new LinkedList<RtpNote>();
    		lastSeq = -1;
    	}
    	public void addFrame(byte[] buf, int len, int seqNum) {
    		RtpNote note = new RtpNote();
    		note.data = buf;
    		note.len = len;
    		note.seqNb = seqNum;
    		if(seqNum > lastSeq){
    			rtpQ.add(note);
    			lastSeq = seqNum;
    			return;
    		}
    		int size = rtpQ.size();
    		RtpNote elem;
    		for(int i = 0; i < size; i++){
    			elem = rtpQ.get(i);
    			if(seqNum > elem.seqNb){
    				rtpQ.add(i, note);
    				return;
    			}
    			if(seqNum == elem.seqNb)
    				return;
    		}
    	}
    	public boolean isEmpty(){
    		return rtpQ.isEmpty();
    	}
    	public byte[] nextFrame() {
    		RtpNote note = rtpQ.removeLast();
    		
    		return note.data;
    	}
    }
    class FrameSynchronizerO {

        private ArrayDeque<byte[]> queue;
        private int bufSize;
        private int curSeqNb;
        private byte[] lastImage;

        public FrameSynchronizerO(int bsize) {
            curSeqNb = 1;
            bufSize = bsize;
            queue = new ArrayDeque<byte[]>(bufSize);
        }

        //synchronize frames based on their sequence number
        public void addFrame(byte[] image, int seqNum) {
            if (seqNum < curSeqNb) {
                queue.add(lastImage);
            }
            else if (seqNum > curSeqNb) {
                for (int i = curSeqNb; i < seqNum; i++) {
                    queue.add(lastImage);
                }
                queue.add(image);
            }
            else {
                queue.add(image);
            }
        }

        //get the next synchronized frame
        public byte[] nextFrame() {
            curSeqNb++;
            lastImage = queue.peekLast();
            return queue.remove();
        }
    }

    //------------------------------------
    // Send RTCP control packets for QoS feedback
    //------------------------------------
    class RtcpSender{

    	private HandlerThread mRtcpSenderThread;
    	private Handler mRtcpSenderhandler;
        int interval;

        // Stats variables
        private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
        private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
        private int lastHighSeqNb;      // The last highest Seq number received
        private int lastCumLost;        // The last cumulative packets lost
        private float lastFractionLost; // The last fraction lost


        public RtcpSender(int interval) {
            this.interval = interval;
            mRtcpSenderThread = new HandlerThread("RtcpSender");
            mRtcpSenderThread.start();
            mRtcpSenderhandler = new Handler(mRtcpSenderThread.getLooper());
        }
        private Runnable mRtcpSender = new Runnable(){
    		@Override
    		public void run() {
                // Calculate the stats for this period
                numPktsExpected = statHighSeqNb - lastHighSeqNb;
                numPktsLost = statCumLost - lastCumLost;
                lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
                lastHighSeqNb = statHighSeqNb;
                lastCumLost = statCumLost;

                //To test lost feedback on lost packets
                // lastFractionLost = randomGenerator.nextInt(10)/10.0f;

                RTCPpacket rtcp_packet = new RTCPpacket(lastFractionLost, statCumLost, statHighSeqNb);
                int packet_length = rtcp_packet.getlength();
                byte[] packet_bits = new byte[packet_length];
                rtcp_packet.getpacket(packet_bits);

                try {
                    DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, ServerIPAddr, RTCP_RCV_PORT);
                    RTCPsocket.send(dp);
                } catch (InterruptedIOException iioe) {
                    Log.d(TAG,"Nothing to read");
                } catch (IOException ioe) {
                    Log.d(TAG,"Exception caught: "+ioe);
                }
    		}
        	
        };

        // Start sending RTCP packets
        public void startSend() {
        	mRtcpSenderhandler.post(mRtcpSender);
        }

        // Stop sending RTCP packets
        public void stopSend() {
        	mRtcpSenderhandler.removeCallbacks(mRtcpSender);
        	mRtcpSenderThread.stop();
        }
    }
}
