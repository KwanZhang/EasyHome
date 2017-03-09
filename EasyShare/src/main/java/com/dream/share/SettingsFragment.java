package com.dream.share;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioFormat;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

public class SettingsFragment extends PreferenceActivity 
		implements OnPreferenceChangeListener{
	private static final String TAG = "SettingsFragment";
	public static final String KEY_SAMPLERATE_LISTPREFERENCE = "SampleRate";
	public static final String KEY_CHANNELCONFIG_LISTPREFERENCE = "ChannelConfig";
	public static final String KEY_AUDIOFORMAT_LISTPREFERENCE = "AudioFormat";
	public static final String KEY_RECORDVOICE_LISTPREFERENCE = "RecordVoice";
	public static final String KEY_MUSICVOICE_LISTPREFERENCE = "MusicVoice";
    public static final String KEY_ADVANCED_CHECKBOX_PREFERENCE = "advanced_checkbox_preference";
	
	private ListPreference mSampleRateListPreference;
	private ListPreference mChannelConfigListPreference;
	private ListPreference mAudioFormatListPreference;
	private ListPreference mRecordVoiceListPreference;
	private ListPreference mMusicVoiceListPreference;
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mSampleRateListPreference = (ListPreference)getPreferenceScreen().findPreference(
				KEY_SAMPLERATE_LISTPREFERENCE);
		
		mSampleRateListPreference.setOnPreferenceChangeListener(this);
		Log.d(TAG,"SampleRate " + PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_SAMPLERATE_LISTPREFERENCE, "44100"));
		
		mChannelConfigListPreference = (ListPreference)getPreferenceScreen().findPreference(
				KEY_CHANNELCONFIG_LISTPREFERENCE);
		mChannelConfigListPreference.setOnPreferenceChangeListener(this);
		Log.d(TAG,"mChannelConfigListPreference value " + mChannelConfigListPreference.getValue());
		
		mAudioFormatListPreference = (ListPreference)getPreferenceScreen().findPreference(
				KEY_AUDIOFORMAT_LISTPREFERENCE);
		mAudioFormatListPreference.setOnPreferenceChangeListener(this);
		
		mRecordVoiceListPreference = (ListPreference)getPreferenceScreen().findPreference(
				KEY_RECORDVOICE_LISTPREFERENCE);
		mRecordVoiceListPreference.setOnPreferenceChangeListener(this);
		
		mMusicVoiceListPreference = (ListPreference)getPreferenceScreen().findPreference(
				KEY_MUSICVOICE_LISTPREFERENCE);
		mMusicVoiceListPreference.setOnPreferenceChangeListener(this);
	}
    @Override
    protected void onResume() {
        super.onResume();

        // Start the force toggle

        // Set up a listener whenever a key changes
        //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Let's do something when my counter preference value changes
    	Log.d(TAG,"key " + key);

    }
    @Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		Log.d(TAG,"newValue " + ((String)newValue));
		String key = preference.getKey();

        if (key.equals(KEY_SAMPLERATE_LISTPREFERENCE)) {
    		
    		if(((String)newValue).equals("48000")){
    			Globals.mSampleRate = 48000;
    		}else if(((String)newValue).equals("44100")){
    			Globals.mSampleRate = 44100;
    		}else if(((String)newValue).equals("22050")){
    			Globals.mSampleRate = 22050;
    		}
        	mSampleRateListPreference.setEntries(mSampleRateListPreference.getEntries());
        }else if (key.equals(KEY_CHANNELCONFIG_LISTPREFERENCE)){
    		if(((String)newValue).equals("1")){
    			Globals.mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    		}else{
    			Globals.mChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
    		}
        	mChannelConfigListPreference.setEntries(mChannelConfigListPreference.getEntries());
        }else if (key.equals(KEY_AUDIOFORMAT_LISTPREFERENCE)){
        	if(((String)newValue).equals("16")){
    			Globals.mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    		}else {
    			Globals.mAudioFormat = AudioFormat.ENCODING_PCM_8BIT;
    		}
        	mAudioFormatListPreference.setEntries(mAudioFormatListPreference.getEntries());	
        }else if (key.equals(KEY_RECORDVOICE_LISTPREFERENCE)){
        	Globals.mRecordVoice = Integer.parseInt((String)newValue);
        }else if (key.equals(KEY_MUSICVOICE_LISTPREFERENCE)){
        	Globals.mMusicVoice = Integer.parseInt((String)newValue);
        }
		preference.setSummary((String)newValue);			
		return true;  
	}
}











