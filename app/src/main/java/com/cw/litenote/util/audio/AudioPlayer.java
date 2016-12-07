package com.cw.litenote.util.audio;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.main.TabsHost;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.Util;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class AudioPlayer 
{
	private static final String TAG = "AUDIO_PLAYER"; // error logging tag
	static final int DURATION_1S = 1000; // 1 seconds per slide
	static AudioInfo mAudioInfo; // slide show being played
	public static Handler mAudioHandler; // used to update the slide show
	public static int mAudioIndex; // index of current media to play
	public static int mPlaybackTime; // time in miniSeconds from which media should play 
	public static MediaPlayer mMediaPlayer; // plays the background music, if any
	static FragmentActivity mAct;

	public static int mAudioPlayMode;
	static int mAudio_tryTimes; // use to avoid useless looping in Continue mode
	public final static int ONE_TIME_MODE = 0;
	public final static int CONTINUE_MODE = 1;

	public static int mPlayerState;
	public static int PLAYER_AT_STOP = 0;
	public static int PLAYER_AT_PLAY = 1;
	public static int PLAYER_AT_PAUSE = 2;

	public static int getPlayState() {
		return mPlayerState;
	}

	public static void setPlayState(int mPlayerState) {
		AudioPlayer.mPlayerState = mPlayerState;
	}


	public static int getPlayMode() {
		return mAudioPlayMode;
	}

	public static void setPlayMode(int mAudioPlayMode) {
		AudioPlayer.mAudioPlayMode = mAudioPlayMode;
	}

	// Manage audio state
	public static void runAudioState(FragmentActivity fragAct)
	{
	   	System.out.println("AudioPlayer / _manageAudioState ");
	   	// if media player is null, set new fragment
		if(mMediaPlayer == null)
		{
		 	// show toast if Audio file is not found or No selection of audio file
			if( (AudioInfo.getAudioFilesSize() == 0) &&
				(getPlayMode() == AudioPlayer.CONTINUE_MODE)        )
			{
				Toast.makeText(fragAct,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
			}
			else
			{
				mPlaybackTime = 0;
				setPlayState(PLAYER_AT_PLAY);
				mAudio_tryTimes = 0;
				mAct = fragAct;
				startNewAudio();
			}
		}
		else
		{
			// from play to pause
			if(mMediaPlayer.isPlaying())
			{
				System.out.println("AudioPlayer / _manageAudioState / play -> pause");
				mMediaPlayer.pause();
				mAudioHandler.removeCallbacks(mRunOneTimeMode); 
				mAudioHandler.removeCallbacks(mRunContinueMode);
				setPlayState(PLAYER_AT_PAUSE);
			}
			else // from pause to play
			{
				System.out.println("AudioPlayer / _manageAudioState / pause -> play");

				mMediaPlayer.start();

				if(getPlayMode() == ONE_TIME_MODE)
					mAudioHandler.post(mRunOneTimeMode);
				else if(getPlayMode() == CONTINUE_MODE)
					mAudioHandler.post(mRunContinueMode);

				setPlayState(PLAYER_AT_PLAY);
			}
		}
	}   	
	
	//
	// One time mode
	//
	public static Runnable mRunOneTimeMode = new Runnable()
	{   @Override
		public void run()
		{
	   		if(mMediaPlayer == null)
	   		{
	   			String audioStr = mAudioInfo.getAudioAt(mAudioIndex);
	   			if(AsyncTaskAudioUrlVerify.mIsOkUrl)
	   			{
				    System.out.println("Runnable updateMediaPlay / play mode: OneTime");
	   				
				    //create a MediaPlayer
				    mMediaPlayer = new MediaPlayer();
	   				mMediaPlayer.reset();
	   				
	   				//set audio player listeners
	   				setAudioPlayerListeners();
	   				
	   				try
	   				{
	   					mMediaPlayer.setDataSource(mAct, Uri.parse(audioStr));
	   					
					    // prepare the MediaPlayer to play, this will delay system response 
   						mMediaPlayer.prepare();
   						
	   					//Note: below
	   					//Set 1 second will cause Media player abnormal on Power key short click
	   					mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S * 2);
	   				}
	   				catch(Exception e)
	   				{
	   					Toast.makeText(mAct,R.string.audio_message_could_not_open_file,Toast.LENGTH_SHORT).show();
	   					stopAudio();
	   				}
	   			}
	   			else
	   			{
	   				Toast.makeText(mAct,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();
   					stopAudio();
	   			}
	   		}
	   		else if(mMediaPlayer != null)
	   		{
				if(getPlayMode() == ONE_TIME_MODE)
	   				Note.updateAudioProgress(mAct);
				mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S);
	   		}		    		
		} 
	};

	//
	// Continue mode
	//
	public static String mAudioStrContinueMode;
	public static Runnable mRunContinueMode = new Runnable()
	{   @Override
		public void run()
		{
	   		if( AudioInfo.getAudioMarking(mAudioIndex) == 1 )
	   		{ 
	   			if(mMediaPlayer == null)
	   			{
		    		// check if audio file exists or not
   					mAudioStrContinueMode = mAudioInfo.getAudioAt(mAudioIndex);

					if(!AsyncTaskAudioUrlVerify.mIsOkUrl)
					{
						mAudio_tryTimes++;
						playNextAudio();
					}
					else
   					{
   						System.out.println("* Runnable updateMediaPlay / play mode: continue");
	   					
   						//create a MediaPlayer 
   						mMediaPlayer = new MediaPlayer(); 
	   					mMediaPlayer.reset();
	   					willPlayNext = true; // default: play next
	   					Page.mProgress = 0;
	   					
	   					// for network stream buffer change
	   					mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener()
	   					{
	   						@Override
	   						public void onBufferingUpdate(MediaPlayer mp, int percent) {
	   							Page.seekBarProgress.setSecondaryProgress(percent);
	   						}
	   					});
   						
	   					// set listeners
   						setAudioPlayerListeners();
   						
   						try
   						{
   							// set data source
							mMediaPlayer.setDataSource(mAct, Uri.parse(mAudioStrContinueMode));
   							
   							// prepare the MediaPlayer to play, could delay system response
   							mMediaPlayer.prepare();
   						}
   						catch(Exception e)
   						{
   							System.out.println("on Exception");
   							Log.e(TAG, e.toString());
							mAudio_tryTimes++;
   							playNextAudio();
   						}
   					}
	   			}
	   			else if(mMediaPlayer != null )
	   			{
	   				// keep looping, do not set post() here, it will affect slide show timing
	   				if(mAudio_tryTimes < AudioInfo.getAudioFilesSize())
	   				{
						// update seek bar
	   					Page.update_audioPanel_progress();

						if(mAudio_tryTimes == 0)
							mAudioHandler.postDelayed(mRunContinueMode,DURATION_1S);
						else
							mAudioHandler.postDelayed(mRunContinueMode,DURATION_1S/10);
	   				}
	   			}
	   		}
	   		else if( AudioInfo.getAudioMarking(mAudioIndex) == 0 )// for non-marking item
	   		{
	   			System.out.println("--- for non-marking item");
	   			// get next index
	   			if(willPlayNext)
	   				mAudioIndex++;
	   			else
	   				mAudioIndex--;
	   			
	   			if( mAudioIndex >= AudioInfo.getAudioList().size())
	   				mAudioIndex = 0; //back to first index
	   			else if( mAudioIndex < 0)
	   			{
	   				mAudioIndex ++;
	   				willPlayNext = true;
	   			}
	   			
	   			startNewAudio();
	   		}
		} 
	};	
	
	static boolean mIsPrepared;
	static void setAudioPlayerListeners()	
	{
			// - on completion listener
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
			{	@Override
				public void onCompletion(MediaPlayer mp) 
				{
					System.out.println("AudioPlayer / _setAudioPlayerListeners / onCompletion");
					
					if(mMediaPlayer != null)
								mMediaPlayer.release();
	
					mMediaPlayer = null;
					mPlaybackTime = 0;

					// get next index
					if(getPlayMode() == CONTINUE_MODE)
					{
						mAudio_tryTimes = 0; //reset try times
						mAudioIndex++;
						if(mAudioIndex == AudioInfo.getAudioList().size())
							mAudioIndex = 0;	// back to first index

						startNewAudio();
						Page.mItemAdapter.notifyDataSetChanged();
					}
					else // one time mode
					{
	   					stopAudio();
	   					Note.initAudioProgress(mAct, Note.mAudioUriInDB);
                        Note.updateAudioPlayState(mAct);
					}
				}
			});
			
			// - on prepared listener
			mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
			{	@Override
				public void onPrepared(MediaPlayer mp)
				{
					System.out.println("AudioPlayer / _setAudioPlayerListeners / _onPrepared");

					if (getPlayMode() == CONTINUE_MODE)
					{
						// media file length
						Page.media_file_length = mMediaPlayer.getDuration(); // gets the song length in milliseconds from URL

						// set footer message: media name
						if (!Util.isEmptyString(mAudioStrContinueMode) &&
                            Page.mDndListView.isShown()                )
						{
                            Page.initAudioBlock();
                            Page.showAudioPanel(mAudioStrContinueMode,true);

							// set seek bar progress
							Page.update_audioPanel_progress();
						}

						if (mMediaPlayer != null)
						{
							mIsPrepared = true;
							mMediaPlayer.start();
                            mMediaPlayer.seekTo(mPlaybackTime);

							// set highlight of playing tab
							if ((getPlayMode() == CONTINUE_MODE) &&
								(MainAct.mPlaying_folderPos ==
								 MainAct.mFocus_folderPos)  )
								TabsHost.setAudioPlayingTab_WithHighlight(true);
							else
								TabsHost.setAudioPlayingTab_WithHighlight(false);

							Page.mItemAdapter.notifyDataSetChanged();

                            Page.isOnAudioClick = false;

							// add for calling runnable
							if (getPlayMode() == CONTINUE_MODE)
								mAudioHandler.postDelayed(mRunContinueMode, Util.oneSecond / 4);
						}
					}
					else if (getPlayMode() == ONE_TIME_MODE)
					{
                        if (mMediaPlayer != null)
                        {
                            mIsPrepared = true;
                            if (!Note.isPausedAtSeekerAnchor)
                            {
                                mMediaPlayer.start();
                                mMediaPlayer.getDuration();
                                mMediaPlayer.seekTo(mPlaybackTime);
                            }
                            else
                            {
                                mMediaPlayer.seekTo(Note.mAnchorPosition);
                                Note.mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
								TextView audio_title_text_view = (TextView) mAct.findViewById(R.id.pager_audio_title);
                                audio_title_text_view.setSelected(false);
                            }
                        }
					}
				}
			});
			
			// - on error listener
			mMediaPlayer.setOnErrorListener(new OnErrorListener()
			{	@Override
				public boolean onError(MediaPlayer mp,int what,int extra) 
				{
					// more than one error when playing an index 
					System.out.println("AudioPlayer / _setAudioPlayerListeners / on Error: what = " + what + " , extra = " + extra);
					return false;
				}
			});
	}

	/*
	* Scroll highlight audio item to visible position
	*
	* At the following conditions
	* 	1) click audio item of list view (this highlight is not good for user expectation, so cancel this condition now)
	* 	2) click previous/next item in audio controller
	* 	3) change tab to playing tab
	* 	4) back from key protect off
	* 	5) if seeker bar reaches the end
	* In order to view audio highlight item, playing(highlighted) audio item can be auto scrolled to top,
	* unless it is at the end page of list view, there is no need to scroll.
	*/
	public static void scrollHighlightAudioItemToVisible()
	{
		System.out.println("AudioPlayer / _scrollHighlightAudioItemToVisible");
		// version limitation: _scrollListBy
		// NoteFragment.mDndListView.scrollListBy(firstVisibleIndex_top);
		if(Build.VERSION.SDK_INT < 19)
			return;

		// check playing drawer and playing tab
//		System.out.println("---------------- TabsHost.mNow_pageId == MainAct.mPlaying_pageId = " + (TabsHost.mNow_pageId == MainAct.mPlaying_pageId));
//		System.out.println("---------------- MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos = " + (MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos));
//		System.out.println("---------------- Page.mDndListView.getChildAt(0) != null = " + (Page.mDndListView.getChildAt(0) != null));
		if( (TabsHost.mNow_pageId == MainAct.mPlaying_pageId) &&
			(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos) &&
			(Page.mDndListView.getChildAt(0) != null)                   )
		{
			int itemHeight = Page.mDndListView.getChildAt(0).getHeight();
			int dividerHeight = Page.mDndListView.getDividerHeight();

			int firstVisible_noteId = Page.mDndListView.getFirstVisiblePosition();
			View v = Page.mDndListView.getChildAt(0);
			int firstVisibleNote_top = (v == null) ? 0 : v.getTop();

//			System.out.println("---------------- itemHeight = " + itemHeight);
//			System.out.println("---------------- dividerHeight = " + dividerHeight);
//			System.out.println("---------------- firstVisible_noteId = " + firstVisible_noteId);
//			System.out.println("---------------- firstVisibleNote_top = " + firstVisibleNote_top);
//			System.out.println("---------------- AudioPlayer.mAudioIndex = " + AudioPlayer.mAudioIndex);

			if(firstVisibleNote_top < 0)
			{
				Page.mDndListView.scrollListBy(firstVisibleNote_top);
//				System.out.println("-----scroll backwards by firstVisibleNote_top " + firstVisibleNote_top);
				firstVisibleNote_top = 0; // update top after scrolling
			}

			boolean noScroll = false;
			// base on AudioPlayer.mAudioIndex to scroll
			if(firstVisible_noteId != AudioPlayer.mAudioIndex)
			{
				while ((firstVisible_noteId != AudioPlayer.mAudioIndex) && (!noScroll))
				{
					int offset = itemHeight + dividerHeight;
					// scroll forwards
					if (firstVisible_noteId > AudioPlayer.mAudioIndex)
					{
						Page.mDndListView.scrollListBy(-offset);
//						System.out.println("-----scroll forwards " + (-offset));
					}
					// scroll backwards
					else if (firstVisible_noteId < AudioPlayer.mAudioIndex)
					{
						Page.mDndListView.scrollListBy(offset);
//						System.out.println("-----scroll backwards " + offset);
					}

//					System.out.println("---------------- firstVisible_noteId 2 = " + firstVisible_noteId);
//					System.out.println("---------------- NoteFragment.mDndListView.getFirstVisiblePosition() = " + NoteFragment.mDndListView.getFirstVisiblePosition());
					if(firstVisible_noteId == Page.mDndListView.getFirstVisiblePosition())
						noScroll = true;
					else {
						// update first visible index
						firstVisible_noteId = Page.mDndListView.getFirstVisiblePosition();
					}
				}
			}
			// backup scroll Y
			Util.setPref_lastTimeView_list_view_first_visible_index(Page.mAct,firstVisible_noteId);
			Util.setPref_lastTimeView_list_view_first_visible_index_top(Page.mAct,firstVisibleNote_top);

			Page.mItemAdapter.notifyDataSetChanged();
		}
	}

	public static AsyncTaskAudioUrlVerify mAudioUrlVerifyTask;
	static void startNewAudio()
	{
		// remove call backs to make sure next toast will appear soon
		if(mAudioHandler != null)
		{
			mAudioHandler.removeCallbacks(mRunOneTimeMode); 
			mAudioHandler.removeCallbacks(mRunContinueMode);
		}

		// start a new handler
		mAudioHandler = new Handler();
		
		if( (getPlayMode() == CONTINUE_MODE) && (AudioInfo.getAudioMarking(mAudioIndex) == 0))
		{
			mAudioHandler.postDelayed(mRunContinueMode,Util.oneSecond/4);		}
		else
		{
			mAudioUrlVerifyTask = new AsyncTaskAudioUrlVerify(mAct);
			mAudioUrlVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");
		}
	}
	
	public static boolean willPlayNext;
	public static void playNextAudio()
	{		
//		Toast.makeText(act,"Can not open file, try next one.",Toast.LENGTH_SHORT).show();
		System.out.println("AudioPlayer / _playNextAudio");
		if(mMediaPlayer != null)
		{
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		mPlaybackTime = 0;
   
		// new audio index
		mAudioIndex++;
		
		if(mAudioIndex >= AudioInfo.getAudioList().size())
			mAudioIndex = 0; //back to first index

		// check try times,had tried or not tried yet, anyway the audio file is found
		System.out.println("check mTryTimes = " + mAudio_tryTimes);
		if(mAudio_tryTimes < AudioInfo.getAudioFilesSize())
		{
			startNewAudio();
		}
		else // try enough times: still no audio file is found 
		{
			Toast.makeText(mAct,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();
			
			// do not show highlight
			MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
			TabsHost.setAudioPlayingTab_WithHighlight(false);
			Page.mItemAdapter.notifyDataSetChanged();

			// stop media player
			stopAudio();
		}		
		System.out.println("Next mAudioIndex = " + mAudioIndex);
	}

	public static void stopAudio()
	{
		System.out.println("AudioPlayer / _stopAudio");
		if(mMediaPlayer != null)
			mMediaPlayer.release();
		mMediaPlayer = null;
		mAudioHandler.removeCallbacks(mRunOneTimeMode); 
		mAudioHandler.removeCallbacks(mRunContinueMode);
		setPlayState(PLAYER_AT_STOP);
		
		// make sure progress dialog will disappear
	 	if( !mAudioUrlVerifyTask.isCancelled() )
	 	{
	 		mAudioUrlVerifyTask.cancel(true);
	 		
	 		if( (mAudioUrlVerifyTask.mUrlVerifyDialog != null) &&
	 		 	mAudioUrlVerifyTask.mUrlVerifyDialog.isShowing()	)
	 		{
	 			mAudioUrlVerifyTask.mUrlVerifyDialog.dismiss();
	 		}

	 		if( (mAudioUrlVerifyTask.mAudioPrepareTask != null) &&
	 			(mAudioUrlVerifyTask.mAudioPrepareTask.mPrepareDialog != null) &&
	 			mAudioUrlVerifyTask.mAudioPrepareTask.mPrepareDialog.isShowing()	)
	 		{
	 			mAudioUrlVerifyTask.mAudioPrepareTask.mPrepareDialog.dismiss();
	 		}
	 	}
	}
	
	// prepare audio info
	public static void prepareAudioInfo()
	{
		mAudioInfo = new AudioInfo(); 
		mAudioInfo.updateAudioInfo();
	}

}