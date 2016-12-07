package com.cw.litenote.util.audio;

import java.io.File;
import java.util.Locale;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.main.TabsHost;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.ImageView;
import android.widget.TextView;

public class UtilAudio {
	
	// Stop audio media player and audio handler
    public static void stopAudioPlayer()
    {
    	System.out.println("UtilAudio / _stopAudioPlayer");
        if(AudioPlayer.mMediaPlayer != null)
    	{
			if(AudioPlayer.mMediaPlayer.isPlaying())
				AudioPlayer.mMediaPlayer.pause();
    		AudioPlayer.mMediaPlayer.release();
    		AudioPlayer.mMediaPlayer = null;
    		AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunOneTimeMode); 
    		AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunContinueMode); 
    		AudioPlayer.setPlayState(AudioPlayer.PLAYER_AT_STOP);
    	}
    }
    
    public static void stopAudioIfNeeded()
    {
		if( (AudioPlayer.mMediaPlayer != null)    &&
			(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos)&&
			(TabsHost.mNow_pageId == MainAct.mPlaying_pageId)&&
			(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)      )
		{
			UtilAudio.stopAudioPlayer();
			AudioPlayer.mAudioIndex = 0;
			AudioPlayer.setPlayState(AudioPlayer.PLAYER_AT_STOP);
			if(MainAct.mSubMenuItemAudio != null)
				MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
			Page.mItemAdapter.notifyDataSetChanged(); // disable focus
		}     	
    }
    
    // update audio panel
    public static void updateAudioPanel(ImageView playBtn, TextView title)
    {
    	System.out.println("UtilAudio/ _updateAudioPanel / AudioPlayer.getPlayState() = " + AudioPlayer.getPlayState());
		title.setBackgroundColor(ColorSet.color_black);
		if(AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_PLAY)
		{
			title.setTextColor(ColorSet.getHighlightColor(MainAct.mAct));
			title.setSelected(true);
			playBtn.setImageResource(R.drawable.ic_media_pause);
		}
		else if( (AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_PAUSE) ||
				 (AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_STOP)    )
		{
			title.setSelected(false);
			title.setTextColor(ColorSet.getPauseColor(MainAct.mAct));
			playBtn.setImageResource(R.drawable.ic_media_play);
		}

    }

    // check if file has audio extension
    // refer to http://developer.android.com/intl/zh-tw/guide/appendix/media-formats.html
    public static boolean hasAudioExtension(File file)
    {
    	boolean hasAudio = false;
    	String fn = file.getName().toLowerCase(Locale.getDefault());
    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
       		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
       		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
       		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
       		fn.endsWith("wav") || fn.endsWith("wma")
    		) 
	    	hasAudio = true;
	    
    	return hasAudio;
    }
    
    // check if string has audio extension
    public static boolean hasAudioExtension(String string)
    {
    	boolean hasAudio = false;
    	if(!Util.isEmptyString(string))
    	{
	    	String fn = string.toLowerCase(Locale.getDefault());
	    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
	           		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
	           		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
	           		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
	           		fn.endsWith("wav") || fn.endsWith("wma")
	        		) 
	    		hasAudio = true;
    	}
    	return hasAudio;
    }     
    
    public static boolean mIsCalledWhilePlayingAudio;
    // for Pause audio player when incoming call
    // http://stackoverflow.com/questions/5610464/stopping-starting-music-on-incoming-calls
    public static PhoneStateListener phoneStateListener = new PhoneStateListener() 
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) 
        {
            if ( (state == TelephonyManager.CALL_STATE_RINGING) || 
                 (state == TelephonyManager.CALL_STATE_OFFHOOK )   ) 
            {
                //Incoming call or Call out: Pause music
            	System.out.println("Incoming call:");
            	if(AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_PLAY)
            	{
            		AudioPlayer.runAudioState(MainAct.mAct);
            		mIsCalledWhilePlayingAudio = true;
            	}
            } 
            else if(state == TelephonyManager.CALL_STATE_IDLE) 
            {
                //Not in call: Play music
            	System.out.println("Not in call:");
            	if( (AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_PAUSE) &&
            		mIsCalledWhilePlayingAudio )	
            	{
            		AudioPlayer.runAudioState(MainAct.mAct); // pause => play
            		mIsCalledWhilePlayingAudio = false;
            	}
            } 
            else if(state == TelephonyManager.CALL_STATE_OFFHOOK) 
            {
                //A call is dialing, active or on hold
            	System.out.println("A call is dialing, active or on hold:");
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
    
    
}
