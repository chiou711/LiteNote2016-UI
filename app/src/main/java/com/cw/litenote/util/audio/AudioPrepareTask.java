package com.cw.litenote.util.audio;

import com.cw.litenote.main.MainUi;
import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

/***************************************************************
 * 
 * audio prepare task
 * 
 */
public class AudioPrepareTask extends AsyncTask<String,Integer,String>
{
	 Activity mActivity;
	 public ProgressDialog mPrepareDialog;
	 int mProgress;
	 public AudioPrepareTask(Activity act)
	 {
		 mActivity = act;
	 }	 
	 
	 @Override
	 protected void onPreExecute() 
	 {
	 	super.onPreExecute();


		if(Util.isLandscapeOrientation(mActivity) &&
		   !Page.mDndListView.isShown() &&
		   (MainUi.getMenuUiState() != R.id.SLIDE_SHOW) && // add for Slide show state
		   Note.isPictureMode()                )
		{
			Util.setImmersiveNavigator(mActivity);
		}
	 	System.out.println("AudioPrepareTask / onPreExecute" );

		mPrepareDialog = new ProgressDialog(mActivity);
	 	if (!Note.isPausedAtSeekerAnchor)
		{
			mPrepareDialog.setMessage(mActivity.getResources().getText(R.string.audio_message_preparing_to_play));
			mPrepareDialog.setCancelable(true); // set true for enabling Back button
			mPrepareDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //ProgressDialog.STYLE_HORIZONTAL
			mPrepareDialog.show();
		}

        AudioPlayer.mIsPrepared = false;
	 } 
	 
	 @Override
	 protected String doInBackground(String... params) 
	 {
		 boolean isTimeOut = false;
		 mProgress = 0;
		 System.out.println("AudioPrepareTask / doInBackground / params[0] = " + params[0] );
		 int count = 0;
		 while(!AudioPlayer.mIsPrepared && !isTimeOut )
		 {
			 System.out.println("AudioPrepareTask / doInBackground / count = " + count);
			 count++;
			 
			 if(count >= 40) // 10 seconds, 1/4 * 40
				 isTimeOut = true;
			 
			 publishProgress(Integer.valueOf(mProgress));
			 
			 mProgress =+ 20;
			 if(mProgress >= 100)
				 mProgress = 0;	
			 
			 try {
				Thread.sleep(Util.oneSecond/4); //??? java.lang.InterruptedException
			 } catch (InterruptedException e) {
				e.printStackTrace();
			 } 
		 }
		 
		 if(isTimeOut)
			return "timeout";
		 else
			return "ok";
	 }
	
	 @Override
	 protected void onProgressUpdate(Integer... progress) 
	 { 
//		 System.out.println("AudioPrepareTask / OnProgressUpdate / progress[0] " + progress[0] );
	     super.onProgressUpdate(progress);
	     
	     if((mPrepareDialog != null) && mPrepareDialog.isShowing())
	    	 mPrepareDialog.setProgress(progress[0]);
	 }
	 
	 // This is executed in the context of the main GUI thread
	 protected void onPostExecute(String result)
	 {
//	 	System.out.println("AudioPrepareTask / _onPostExecute / result = " + result);
	 	
	 	// dialog off
		if((mPrepareDialog != null) && mPrepareDialog.isShowing())
			mPrepareDialog.dismiss();

		mPrepareDialog = null;

		// show time out
		if(result.equalsIgnoreCase("timeout"))
		{
			Toast toast = Toast.makeText(mActivity.getApplicationContext(), R.string.audio_message_preparing_time_out, Toast.LENGTH_SHORT);
			toast.show();
		}

		// unlock orientation
//		Util.unlockOrientation(mActivity);
		// disable rotation
//	 	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	 }
	 
}
