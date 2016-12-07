package com.cw.litenote.util.audio;

import com.cw.litenote.R;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

// A class that will show progress bar in the main GUI context
//
// audio Url verification task
//
public class AsyncTaskAudioUrlVerify extends AsyncTask<String,Integer,String>
{
	 public ProgressDialog mUrlVerifyDialog;
	 Activity mAct;
	 public AudioPrepareTask mAudioPrepareTask;

	 public AsyncTaskAudioUrlVerify(Activity act)
	 {
		 mAct = act;
	 }	 
	 
	 @Override
	 protected void onPreExecute() 
	 {
	 	 super.onPreExecute();
	 	 // lock orientation
//	 	 Util.lockOrientation(mActivity);

	 	 // disable rotation
//	 	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	 	
	 	 System.out.println("AudioUrlVerifyTask / onPreExecute" );

		 mUrlVerifyDialog = new ProgressDialog(mAct);
		 if (!Note.isPausedAtSeekerAnchor)
		 {
			 mUrlVerifyDialog.setMessage(mAct.getResources().getText(R.string.audio_message_searching_media));
			 mUrlVerifyDialog.setCancelable(true); // set true for enabling Back button
			 mUrlVerifyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //ProgressDialog.STYLE_HORIZONTAL
			 mUrlVerifyDialog.show();
		 }

		 AudioPlayer.mIsPrepared = false;
	 } 
	 
	 static boolean mIsOkUrl;
	 static int mProgress =0;
	 @Override
	 protected String doInBackground(String... params) 
	 {
		 System.out.println("AudioUrlVerifyTask / doInBackground / params[0] = " + params[0] );
		 mProgress =0;
 		 // check if audio file exists or not
		 String audioStr = AudioPlayer.mAudioInfo.getAudioAt(AudioPlayer.mAudioIndex);
 		 mIsOkUrl = false;
 		 String scheme  = Util.getUriScheme(audioStr);
 		 System.out.println("scheme = " + scheme + " / path = " + audioStr);
 		
 		 // if scheme is https or http
 		 boolean isUriExisted = false;
 		 
 		 if(scheme == null)
 			 return  "ng";
 		 
 		 if(scheme.equalsIgnoreCase("http")|| scheme.equalsIgnoreCase("https") )						
 		 {
			 if(Util.isNetworkConnected(AudioPlayer.mAct))
			 {
		 		 isUriExisted = Util.isUriExisted(audioStr, AudioPlayer.mAct );
		 		 System.out.println("AudioUrlVerifyTask / isUriExisted  = " + isUriExisted);
		 		 if(isUriExisted)
		 		 {
		 			 try 
		 			 {
		 				 boolean isEnd = false;
		 				 int i = 0;
		 				 while(!isEnd)
		 				 {
		 					 // check if network connection is OK
		 					 publishProgress(Integer.valueOf(mProgress));
		 					 mProgress =+ 20;
		 					 if(mProgress >= 100)
		 						 mProgress = 0;
 				         
		 					 Util.tryUrlConnection(audioStr,AudioPlayer.mAct);
		 					 // wait for response
		 					 Thread.sleep(Util.oneSecond); //??? better idea?
 						
		 					 // check response
		 					 if(200 <= Util.mResponseCode && Util.mResponseCode <= 399)
		 						 mIsOkUrl =  true;
		 					 else
		 						 mIsOkUrl =  false;
 						
		 					 System.out.println("mIsOkUrl = " + mIsOkUrl +
		 							 			" / count = " + i);
		 					 if(mIsOkUrl)
		 						 isEnd = true;
		 					 else
		 					 {
		 						 i++;
		 						 if(i==5) //??? better idea?
		 							 isEnd = true; // no more try
		 					 }
		 				 }
		 			 } 
		 			 catch (Exception e1) 
		 			 {
		 				 e1.printStackTrace();
		 			 }
		 		}
			 }
 		 } 
 		 // if scheme is content or file
 		 else if(scheme.equalsIgnoreCase("content") ||
 				scheme.equalsIgnoreCase("file")    )
 		 {
 			 String strName = null;
 			 isUriExisted = Util.isUriExisted(audioStr, AudioPlayer.mAct );
	 		 
 			 if(isUriExisted)
	 			 strName = Util.getDisplayNameByUriString(audioStr, AudioPlayer.mAct);
 			 
	 		 if(!Util.isEmptyString(strName))
 				mIsOkUrl = true;
 			 else
 				mIsOkUrl = false;
 		 }
 		
 		 System.out.println("Url mIsOkUrl = " + mIsOkUrl);	    	 

 		 if(mIsOkUrl)
 			 return "ok";
 		 else
 			 return "ng";
	 }
	
	 @Override
	 protected void onProgressUpdate(Integer... progress) 
	 { 
		 System.out.println("AudioUrlVerifyTask / OnProgressUpdate / progress[0] " + progress[0] );
	     super.onProgressUpdate(progress);
	     if(mUrlVerifyDialog != null)
	    	 mUrlVerifyDialog.setProgress(progress[0]);
	 }
	 
	 // This is executed in the context of the main GUI thread
	 protected void onPostExecute(String result)
	 {
	 	System.out.println("AudioUrlVerifyTask / onPostExecute / result = " + result);
		
	 	// dialog off
		if((mUrlVerifyDialog != null) && mUrlVerifyDialog.isShowing() )
			mUrlVerifyDialog.dismiss();

 		mUrlVerifyDialog = null;

 		// task for audio prepare
	 	if(mIsOkUrl)
	 	{
	 		mAudioPrepareTask = new AudioPrepareTask(mAct);
	 		mAudioPrepareTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Preparing to play ...");
	 	}
 		
	 	// call runnable
	 	if(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)
	 	{
			if(AudioPlayer.getPlayMode() == AudioPlayer.ONE_TIME_MODE)
			 	AudioPlayer.mAudioHandler.postDelayed(AudioPlayer.mRunOneTimeMode,Util.oneSecond/4); 
			else if(AudioPlayer.getPlayMode() == AudioPlayer.CONTINUE_MODE)
			 	AudioPlayer.mAudioHandler.postDelayed(AudioPlayer.mRunContinueMode,Util.oneSecond/4);
	 	}	 	
	 }
}