package com.dream.player.common;

import android.app.Activity;

import com.dream.player.network.PlayerFeedback;


public class PlayerContainer {
	//private static final String TAG = "PlayerContainer";
	private static final PlayerContainer mPlayerContainer = new PlayerContainer();
	private PlayerFeedback mPlayerFeedback;
	private Activity mTopPlayer; 
	
	public static PlayerContainer getInstance(){
		return mPlayerContainer;
	}
	public void setPlayerFeedback(PlayerFeedback feedback){
		mPlayerFeedback = feedback;
	}
	public PlayerFeedback getPlayerFeedback(){
		return mPlayerFeedback;
	}
	public void setTopActivity(Activity activity){
		mTopPlayer = activity;
	}
	public Activity getTopActivity(){
		return mTopPlayer;
	}
}
