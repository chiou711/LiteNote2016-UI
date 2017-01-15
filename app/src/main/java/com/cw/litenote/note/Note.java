package com.cw.litenote.note;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.main.Page;
import com.cw.litenote.main.TabsHost;
import com.cw.litenote.util.CustomWebView;
import com.cw.litenote.util.DeleteFileAlarmReceiver;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.video.AsyncTaskVideoBitmapPager;
import com.cw.litenote.util.video.UtilVideo;
import com.cw.litenote.util.video.VideoPlayer;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.MailNotes;
import com.cw.litenote.util.UilCommon;
import com.cw.litenote.util.Util;

import android.R.color;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Note extends FragmentActivity
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public ViewPager mPager;
    public static boolean isPagerActive;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    public static PagerAdapter mPagerAdapter;

    // DB
    public DB_page mDb_page;
    public static Long mNoteId;
    int mEntryPosition;
    public static int mCurrentPosition;
    int EDIT_CURRENT_VIEW = 5;
    int MAIL_CURRENT_VIEW = 6;
    static int mStyle;
    
    static SharedPreferences mPref_show_note_attribute;

    Button editButton;
    Button sendButton;
    Button backButton;

	public static String mAudioUriInDB;
    public TextView audio_title;
    TextView audio_curr_pos;
    TextView audio_file_len;
    ViewGroup audioBlock;
    public FragmentActivity act;
    public static int mPlayVideoPositionOfInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note / _onCreate");

		// set current selection
		mEntryPosition = getIntent().getExtras().getInt("POSITION");
		mCurrentPosition = mEntryPosition;

		// init video
		UtilVideo.mPlayVideoPosition = 0;   // not played yet
		mPlayVideoPositionOfInstance = 0;
		AsyncTaskVideoBitmapPager.mRotationStr = null;
    } //onCreate end

	void setLayoutView()
	{
        System.out.println("Note / _setLayoutView");

		if( UtilVideo.mVideoView != null)
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

		// video view will be reset after _setContentView
		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

		ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.view_note_title);
            actionBar.setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(this)));
        }

		act = this;
		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

		UilCommon.init();

		// DB
		mDb_page = new DB_page(act,Util.getPref_lastTimeView_page_tableId(act));

		// Instantiate a ViewPager and a PagerAdapter.
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new Note_adapter(mPager,this);
		mPager.setAdapter(mPagerAdapter);
		mPager.setCurrentItem(mCurrentPosition);

		// tab style
		if(TabsHost.mDbFolder != null)
			TabsHost.mDbFolder.close();

		TabsHost.mDbFolder = new DB_folder(act,Util.getPref_lastTimeView_folder_tableId(act));

		mStyle = TabsHost.mDbFolder.getPageStyle(TabsHost.mNow_pageId, true);

		if(mDb_page != null) {
			mNoteId = mDb_page.getNoteId(mCurrentPosition, true);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
		}

		// audio block
		TextView tag = (TextView) findViewById(R.id.text_view_audio);
		tag.setTextColor(ColorSet.color_white);

		audio_title = (TextView) findViewById(R.id.pager_audio_title); // first setting
		audio_title.setTextColor(ColorSet.color_white);
		if (Util.isLandscapeOrientation(act))
		{
			audio_title.setMovementMethod(new ScrollingMovementMethod());
			audio_title.scrollTo(0,0);
		}
		else
		{
			audio_title.setSingleLine(true);
			audio_title.setSelected(true);
		}

		audio_curr_pos = (TextView) act.findViewById(R.id.pager_audio_current_pos);

		audio_file_len = (TextView) findViewById(R.id.pager_audio_file_length);

		audioBlock = (ViewGroup) findViewById(R.id.audioGroup);
		audioBlock.setBackgroundColor(ColorSet.color_black);

		mPager_audio_play_button = (ImageView) act.findViewById(R.id.pager_btn_audio_play);
		seekBar = (SeekBar) act.findViewById(R.id.pager_img_audio_seek_bar);

		// Note: if mPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
		//       be called again after rotation
		mPager.setOnPageChangeListener(onPageChangeListener);

		// edit note button
		editButton = (Button) findViewById(R.id.view_edit);
		editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
		editButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(Note.this, Note_edit.class);
				intent.putExtra(DB_page.KEY_NOTE_ID, mNoteId);
				intent.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_AUDIO_URI , mDb_page.getNoteAudioUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_PICTURE_URI , mDb_page.getNotePictureUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_LINK_URI , mDb_page.getNoteLinkUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_CREATED, mDb_page.getNoteCreatedTime_byId(mNoteId));
				startActivityForResult(intent, EDIT_CURRENT_VIEW);
			}
		});

		// send note button
		sendButton = (Button) findViewById(R.id.view_send);
		sendButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_send, 0, 0, 0);
		sendButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				// set Sent string Id
				List<Long> noteIdArray = new ArrayList<>();
				noteIdArray.add(0, mNoteId);

				String sentString = Util.getStringWithXmlTag(noteIdArray);
				sentString = Util.addXmlTag(sentString);

				String picFile = mDb_page.getNotePictureUri_byId(mNoteId);
				System.out.println("-> picFile = " + picFile);
				String[] picFileArray = null;
				if( (picFile != null) &&
						(picFile.length() > 0) )
				{
					picFileArray = new String[]{picFile};
				}
				new MailNotes(act,sentString,picFileArray);
			}
		});

		// back button
		backButton = (Button) findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
		backButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view) {
				if(isTextMode())
				{
					// back to view all mode
					setViewAllMode();
					setOutline(act);
				}
				else //view all mode
				{
					stopAV();
					finish();
				}
			}
		});

	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{
			if(AudioPlayer.getPlayMode()  == AudioPlayer.ONE_TIME_MODE)
				UtilAudio.stopAudioPlayer();

			mCurrentPosition = mPager.getCurrentItem();
			System.out.println("Note / _onPageSelected");
			System.out.println("    mCurrentPosition = " + mCurrentPosition);
			System.out.println("    nextPosition = " + nextPosition);

			mIsViewModeChanged = false;

			// show audio name
			mNoteId = mDb_page.getNoteId(nextPosition,true);
			System.out.println("Note / _onPageSelected / mNoteId = " + mNoteId);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
			System.out.println("Note / _onPageSelected / mAudioUriInDB = " + mAudioUriInDB);

			if(UtilAudio.hasAudioExtension(mAudioUriInDB))
			{
				audioBlock.setVisibility(View.VISIBLE);
				initAudioProgress(act,mAudioUriInDB);
			}
			else
				audioBlock.setVisibility(View.GONE);

			if((nextPosition == mCurrentPosition+1) || (nextPosition == mCurrentPosition-1))
			{
				if(AudioPlayer.getPlayMode() == AudioPlayer.ONE_TIME_MODE)
					AudioPlayer.mAudioIndex = mCurrentPosition;//update Audio index
			}

			// stop video when changing note
			String pictureUriInDB = mDb_page.getNotePictureUri_byId(mNoteId);
			if(UtilVideo.hasVideoExtension(pictureUriInDB,act)) {
				VideoPlayer.stopVideo();
				if(picUI != null) {
					if(picUI.handler != null)
						picUI.handler.removeCallbacks(Note.picUI.runnable);
					picUI = null;
				}
			}

            setOutline(act);
		}
	};

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		System.out.println("Note / _onActivityResult ");
        if((requestCode==EDIT_CURRENT_VIEW) || (requestCode==MAIL_CURRENT_VIEW))
        {
			stopAV();
        }
		else if(requestCode == MailNotes.EMAIL)
		{
			Toast.makeText(act,R.string.mail_exit,Toast.LENGTH_SHORT).show();
			// note: result code is always 0 (cancel), so it is not used
			new DeleteFileAlarmReceiver(act,
					                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//						    		    System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					                    MailNotes.mAttachmentFileName);
		}


	    // check if there is one note at least in the pager
		if( mPager.getAdapter().getCount() > 0 )
			setOutline(act);
		else
			finish();
	}

    /** Set outline for selected view mode
    *
    *   Determined by view mode: all, picture, text
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public static void setOutline(Activity act)
	{
        // Set full screen or not, and action bar
		if(isViewAllMode() || isTextMode())
		{
			Util.setNotFullScreen(act);
            if(act.getActionBar() != null)
			    act.getActionBar().show();
		}
		else if(isPictureMode())
		{
			Util.setFullScreen(act);
            if(act.getActionBar() != null)
    			act.getActionBar().hide();
		}

        // renew pager
        showSelectedView();

		LinearLayout buttonGroup = (LinearLayout) act.findViewById(R.id.view_button_group);
        // button group
        if(Note.isPictureMode() )
            buttonGroup.setVisibility(View.GONE);
        else
            buttonGroup.setVisibility(View.VISIBLE);

		TextView audioTitle = (TextView) act.findViewById(R.id.pager_audio_title);
        // audio title
        if(!Note.isPictureMode())
        {
            if(!Util.isEmptyString(audioTitle.getText().toString()) )
                audioTitle.setVisibility(View.VISIBLE);
            else
                audioTitle.setVisibility(View.GONE);
        }

        // renew options menu
        act.invalidateOptionsMenu();
	}

	
    //Refer to http://stackoverflow.com/questions/4434027/android-videoview-orientation-change-with-buffered-video
	/***************************************************************
	video play spec of Pause and Rotate:
	1. Rotate: keep pause state
	 pause -> rotate -> pause -> play -> continue

	2. Rotate: keep play state
	 play -> rotate -> continue play

	3. Key guard: enable pause
	 play -> key guard on/off -> pause -> play -> continue

	4. Key guard and Rotate: keep pause
	 play -> key guard on/off -> pause -> rotate -> pause
	 ****************************************************************/	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note / _onConfigurationChanged");


		if(picUI != null)
		{
			picUI.handler.removeCallbacks(picUI.runnable);
			picUI = null;
		}

        setLayoutView();

        if(canShowFullScreenPicture())
            Note.setPictureMode();
        else
            Note.setViewAllMode();

        // Set outline of view mode
        setOutline(act);
}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note / _onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Note / _onResume");

		setLayoutView();

		isPagerActive = true;

        if(canShowFullScreenPicture())
            Note.setPictureMode();
        else
            Note.setViewAllMode();

		setOutline(act);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note / _onPause");

		isPagerActive = false;

		// set pause when key guard is ON
		if( UtilVideo.mVideoView != null)
		{
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

			// keep play video position
			mPlayVideoPositionOfInstance = UtilVideo.mPlayVideoPosition;
			System.out.println("Note / _onPause / mPlayVideoPositionOfInstance = " + mPlayVideoPositionOfInstance);

			if(UtilVideo.mVideoPlayer != null)//??? try more to check if this is better? or still keep video view
				VideoPlayer.stopVideo();
		}

		// to stop YouTube web view running
    	String tagStr = "current"+ mPager.getCurrentItem()+"webView";
    	CustomWebView webView = (CustomWebView) mPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(webView);
    	CustomWebView.blankWebView(webView);

		// to stop Link web view running
    	tagStr = "current"+ mPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(linkWebView);
    	CustomWebView.blankWebView(linkWebView);

		if(picUI != null)
		{
			picUI.handler.removeCallbacks(picUI.runnable);
			picUI = null;
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("Note / _onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note / _onDestroy");
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		
		if(mPagerHandler != null)
			mPagerHandler.removeCallbacks(mOnBackPressedRun);		
	    
		ViewGroup view = (ViewGroup) getWindow().getDecorView();
	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
	    view.removeAllViews();
	    
	    super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note / _onSaveInstanceState");
	}

	Menu mMenu;
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);

		// inflate menu
		getMenuInflater().inflate(R.menu.pager_menu, menu);
		mMenu = menu;

		// menu item: checked status
		// get checked or not
		int isChecked = mDb_page.getNoteMarking(mCurrentPosition,true);
		if( isChecked == 0)
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
		else
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);

		// menu item: view mode
   		markCurrentSelected(menu.findItem(R.id.VIEW_ALL),"ALL");
		markCurrentSelected(menu.findItem(R.id.VIEW_PICTURE),"PICTURE_ONLY");
		markCurrentSelected(menu.findItem(R.id.VIEW_TEXT),"TEXT_ONLY");

	    // menu item: previous
		MenuItem itemPrev = menu.findItem(R.id.ACTION_PREVIOUS);
		itemPrev.setEnabled(mPager.getCurrentItem() > 0);
		itemPrev.getIcon().setAlpha(mPager.getCurrentItem() > 0?255:30);
		
		// menu item: Next or Finish
		MenuItem itemNext = menu.findItem(R.id.ACTION_NEXT);
		itemNext.setTitle((mPager.getCurrentItem() == mPagerAdapter.getCount() - 1)	?
									R.string.view_note_slide_action_finish :
									R.string.view_note_slide_action_next                  );

        // set Disable and Gray for Last item
		boolean isLastOne = (mPager.getCurrentItem() == (mPagerAdapter.getCount() - 1));
        if(isLastOne)
        	itemNext.setEnabled(false);

        itemNext.getIcon().setAlpha(isLastOne?30:255);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if(isTextMode())
            	{
        			// back to view all mode
            		setViewAllMode();
					setOutline(act);
            	}
            	else if(isViewAllMode())
            	{
					stopAV();
	            	finish();
            	}
                return true;

            case R.id.VIEW_NOTE_MODE:
            	return true;

			case R.id.VIEW_NOTE_CHECK:
				int markingNow = Page.toggleNoteMarking(mCurrentPosition);

				// update marking
				if(markingNow == 1)
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
				else
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);

				return true;

            case R.id.VIEW_ALL:
        		setViewAllMode();
				setOutline(act);
            	return true;
            	
            case R.id.VIEW_PICTURE:
        		setPictureMode();
				setOutline(act);
            	return true;

            case R.id.VIEW_TEXT:
        		setTextMode();
				setOutline(act);
            	return true;
            	
            case R.id.ACTION_PREVIOUS:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	Note.mCurrentPosition--;
            	mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                return true;

            case R.id.ACTION_NEXT:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
            	Note.mCurrentPosition++;
            	mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            	
            	//TO
//            	mMP.setVolume(0,0);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    //
//    // Open link of YouTube
//    //
//    // Due to "AdWords or copyright" server limitation, for some URI,
//    // "video is not available" message could show up.
//    // At this case, one solution is to switch current mobile website to desktop website by browser setting.
//    // So, base on URI key words to decide "YouTube App" or "browser" launch.
//    public void openLink_YouTube(String linkUri)
//    {
//        // by YouTube App
//        if(linkUri.contains("youtu.be"))
//        {
//            // stop audio and video if playing
//            stopAV();
//
//            String id = Util.getYoutubeId(linkUri);
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + id));
//            act.startActivity(intent);
//        }
//        // by Chrome browser
//        else if(linkUri.contains("youtube.com"))
//        {
//            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUri));
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.setPackage("com.android.chrome");
//
//            try
//            {
//                act.startActivity(i);
//            }
//            catch (ActivityNotFoundException e)
//            {
//                // Chrome is probably not installed
//                // Try with the default browser
//                i.setPackage(null);
//                act.startActivity(i);
//            }
//        }
//    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note / _onBackPressed");
    	// web view can go back
    	String tagStr = "current"+ mPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
        if (linkWebView.canGoBack()) 
        {
        	linkWebView.goBack();
        }
        else if(isPictureMode())
    	{
            // dispatch touch event to show buttons
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis() + 100;
            float x = 0.0f;
            float y = 0.0f;
            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
            int metaState = 0;
            MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN,
                                                    x, y,metaState);
            dispatchTouchEvent(event);
            event.recycle();

            // in order to make sure ImageViewBackButton is effective to be clicked
            mPagerHandler = new Handler();
            mPagerHandler.postDelayed(mOnBackPressedRun, 500);
        }
    	else if(isTextMode())
    	{
			// back to view all mode
    		setViewAllMode();
			setOutline(act);
    	}
    	else
    	{
    		System.out.println("Note / _onBackPressed / view all mode");
			stopAV();
        	finish();
    	}
    }
    
    static Handler mPagerHandler;
	Runnable mOnBackPressedRun = new Runnable()
	{   @Override
		public void run()
		{
            String tagStr = "current"+ Note.mCurrentPosition +"pictureView";
            ViewGroup pictureGroup = (ViewGroup) mPager.findViewWithTag(tagStr);
            System.out.println("Note / _showPictureViewUI / tagStr = " + tagStr);

            Button picView_back_button;
            if(pictureGroup != null)
            {
                picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
                picView_back_button.performClick();
            }

			if(Note_adapter.mIntentView != null)
				Note_adapter.mIntentView = null;
		}
	};
    
    // get current picture string
    public String getCurrentPictureString()
    {
		return mDb_page.getNotePictureUri(mCurrentPosition,true);
    }

    static void playAudioInPager(FragmentActivity act, String audioStr)
    {
		if(UtilAudio.hasAudioExtension(audioStr))
		{
    		AudioPlayer.mAudioIndex = mCurrentPosition;
    		// new instance
    		if(AudioPlayer.mMediaPlayer == null)
    		{
    			MainAct.mPlaying_pageTableId = Util.getPref_lastTimeView_page_tableId(act);
        		AudioPlayer.setPlayMode(AudioPlayer.ONE_TIME_MODE);
    		}
    		// If Audio player is NOT at One time mode and media exists
    		else if((AudioPlayer.mMediaPlayer != null) &&
    				(AudioPlayer.getPlayMode() == AudioPlayer.CONTINUE_MODE))
    		{
        		AudioPlayer.setPlayMode(AudioPlayer.ONE_TIME_MODE);
        		UtilAudio.stopAudioPlayer();
    		}

   			AudioPlayer.prepareAudioInfo();

    		AudioPlayer.runAudioState(act);

            updateAudioPlayState(act);
		}
    }
    
    // Mark current selected 
    void markCurrentSelected(MenuItem subItem, String str)
    {
        if(mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
        							.equalsIgnoreCase(str))
        	subItem.setIcon(R.drawable.btn_radio_on_holo_dark);
	  	else
        	subItem.setIcon(R.drawable.btn_radio_off_holo_dark);
    }    
    
    // show audio name
    static void showAudioName(FragmentActivity act)
    {
		TextView audio_title_text_view = (TextView) act.findViewById(R.id.pager_audio_title);
		// title: set marquee
		if(Util.isUriExisted(mAudioUriInDB, act)) {
			String audio_name = Util.getDisplayNameByUriString(mAudioUriInDB, act);
			audio_title_text_view.setText(audio_name);
		}
		else
			audio_title_text_view.setText(R.string.file_not_found);

		audio_title_text_view.setSelected(false);
    }
    
    // Set audio block
    static public ImageView mPager_audio_play_button;
    SeekBar seekBar;
    public static int mProgress;
	public static int mediaFileLength; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
    
	public static boolean isPausedAtSeekerAnchor;
	public static int mAnchorPosition; 

    static void setAudioBlockListener(final FragmentActivity act,final String audioStr)
    {
        SeekBar seekBarProgress = (SeekBar) act.findViewById(R.id.pager_img_audio_seek_bar);
        ImageView mPager_audio_play_button = (ImageView) act.findViewById(R.id.pager_btn_audio_play);

        // set audio play and pause control image
	    mPager_audio_play_button.setOnClickListener(new View.OnClickListener() 
	    {
			@Override
			public void onClick(View v) 
			{
				isPausedAtSeekerAnchor = false;
            	TabsHost.setAudioPlayingTab_WithHighlight(false);// in case playing audio in pager
            	playAudioInPager(act,audioStr);
			}
		});   		
   		
		// set seek bar listener
		seekBarProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) 
			{
				if( AudioPlayer.mMediaPlayer != null  )
				{
					int mPlayAudioPosition = (int) (((float)(mediaFileLength / 100)) * seekBar.getProgress());
					AudioPlayer.mMediaPlayer.seekTo(mPlayAudioPosition);
				}
				else
				{
					// pause at seek bar anchor
					isPausedAtSeekerAnchor = true;
					mAnchorPosition = (int) (((float)(mediaFileLength / 100)) * seekBar.getProgress());
					playAudioInPager(act,audioStr);
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// audio player is one time mode in pager
				if(AudioPlayer.getPlayMode() == AudioPlayer.CONTINUE_MODE)
					UtilAudio.stopAudioPlayer();
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
			{
				if(fromUser)
				{	
					// show progress change
			    	int currentPos = mediaFileLength *progress/(seekBar.getMax()+1);
			    	int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
			    	int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
			     	int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
	
			    	// set current play time
                    TextView audio_curr_pos = (TextView) act.findViewById(R.id.pager_audio_current_pos);
			    	audio_curr_pos.setText(String.format(Locale.ENGLISH,"%2d", curHour)+":" +
			    										       String.format(Locale.ENGLISH,"%02d", curMin)+":" +
			    										       String.format(Locale.ENGLISH,"%02d", curSec) );
				}
			}
		});
    }  
    
    public static void updateAudioProgress(FragmentActivity act)
    {
        SeekBar seekBar = (SeekBar) act.findViewById(R.id.pager_img_audio_seek_bar);
        int currentPos = AudioPlayer.mMediaPlayer.getCurrentPosition();
    	int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
    	int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
     	int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));

        TextView audio_curr_pos = (TextView) act.findViewById(R.id.pager_audio_current_pos);
    	// set current play time and the play length of audio file
     	if(audio_curr_pos != null)
     	{
     		audio_curr_pos.setText(String.format(Locale.ENGLISH,"%2d", curHour)+":" +
    										           String.format(Locale.ENGLISH,"%02d", curMin)+":" +
    										           String.format(Locale.ENGLISH,"%02d", curSec) );//??? why affect audio title?
     	}
     	
     	mProgress = (int)(((float)currentPos/ mediaFileLength)*100);
     	
     	if(seekBar != null)
     		seekBar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
    }
    
    public static void initAudioProgress(FragmentActivity act,String audioUriInDB)
    {
        SeekBar seekBar = (SeekBar) act.findViewById(R.id.pager_img_audio_seek_bar);
        ImageView mPager_audio_play_button = (ImageView) act.findViewById(R.id.pager_btn_audio_play);
        setAudioBlockListener(act,audioUriInDB);

    	mProgress = 0;

		showAudioName(act);
        TextView audioTitle = (TextView) act.findViewById(R.id.pager_audio_title);
		audioTitle.setSelected(false);
        mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
        audioTitle.setTextColor(ColorSet.getPauseColor(act));
        audioTitle.setSelected(false);

		// current position
    	int curHour = Math.round((float)(mProgress / 1000 / 60 / 60));
    	int curMin = Math.round((float)((mProgress - curHour * 60 * 60 * 1000) / 1000 / 60));
     	int curSec = Math.round((float)((mProgress - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));

        TextView audio_curr_pos = (TextView) act.findViewById(R.id.pager_audio_current_pos);
     	audio_curr_pos.setText(String.format(Locale.ENGLISH,"%2d", curHour)+":" +
    										       String.format(Locale.ENGLISH,"%02d", curMin)+":" +
    										       String.format(Locale.ENGLISH,"%02d", curSec) );//??? why affect audio title?
		audio_curr_pos.setTextColor(ColorSet.color_white);
	    // audio seek bar
     	seekBar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
		seekBar.setMax(99); // It means 100% .0-99
    	seekBar.setVisibility(View.VISIBLE);
     	
    	// get audio file length
    	try
    	{
    		MediaPlayer mp = MediaPlayer.create(act,Uri.parse(mAudioUriInDB));
    		mediaFileLength = mp.getDuration();
    		mp.release();
    	}
    	catch(Exception e)
    	{
    		System.out.println("Note / _initAudioProgress / exception");
    	}
    	// set audio file length
     	int fileHour = Math.round((float)(mediaFileLength / 1000 / 60 / 60));
     	int fileMin = Math.round((float)((mediaFileLength - fileHour * 60 * 60 * 1000) / 1000 / 60));
    	int fileSec = Math.round((float)((mediaFileLength - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));

        TextView audio_file_len = (TextView) act.findViewById(R.id.pager_audio_file_length);
    	audio_file_len.setText(String.format(Locale.ENGLISH,"%2d", fileHour)+":" +
    										String.format(Locale.ENGLISH,"%02d", fileMin)+":" +
    										String.format(Locale.ENGLISH,"%02d", fileSec));
		audio_file_len.setTextColor(ColorSet.color_white);
    }


    public static void updateAudioPlayState(FragmentActivity act)
    {
        ImageView audio_play_btn = (ImageView) act.findViewById(R.id.pager_btn_audio_play);

        if(AudioPlayer.getPlayMode() != AudioPlayer.ONE_TIME_MODE)
            return;

        TextView audioTitle = (TextView) act.findViewById(R.id.pager_audio_title);
        // update playing state
        if(AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_PLAY)
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_pause);
			showAudioName(act);
			audioTitle.setTextColor(ColorSet.getHighlightColor(act) );
            audioTitle.setSelected(true);
        }
        else if( (AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_PAUSE) ||
                (AudioPlayer.getPlayState() == AudioPlayer.PLAYER_AT_STOP)    )
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_play);
			showAudioName(act);
            audioTitle.setTextColor(ColorSet.getPauseColor(act));
            audioTitle.setSelected(false);
        }
    }

    // Show selected view
    static void showSelectedView()
    {
   		mIsViewModeChanged = false;

		if(!Note.isTextMode())
   		{
	   		if(UtilVideo.mVideoView != null)
	   		{
	   	   		// keep current video position for NOT text mode
				mPositionOfChangeView = UtilVideo.mPlayVideoPosition;
	   			mIsViewModeChanged = true;

	   			if(VideoPlayer.mVideoHandler != null)
	   			{
					System.out.println("Note / _showSelectedView / just remove callbacks");
	   				VideoPlayer.mVideoHandler.removeCallbacks(VideoPlayer.mRunPlayVideo);
	   				if(UtilVideo.hasMediaControlWidget)
	   					VideoPlayer.cancelMediaController();
	   			}
	   		}
   			Note_adapter.mLastPosition = -1;
   		}

    	if(mPagerAdapter != null)
    		mPagerAdapter.notifyDataSetChanged(); // will call Note_adapter / _setPrimaryItem
    }
    
    public static int mPositionOfChangeView;
    public static boolean mIsViewModeChanged;
    
    static void setViewAllMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","ALL")
		   						  .apply();
    }
    
    static void setPictureMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","PICTURE_ONLY")
		   						  .apply();
    }
    
    static void setTextMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","TEXT_ONLY")
		   						  .apply();
    }
    
    
    public static boolean isPictureMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("PICTURE_ONLY");
    }
    
    public static boolean isViewAllMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("ALL");
    }

    public static boolean isTextMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("TEXT_ONLY");
    }

    static Note_UI picUI;
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {

	        case MotionEvent.ACTION_DOWN:
	        case MotionEvent.ACTION_POINTER_DOWN: 
	        	 //??? how to detect zoom image?
        		 // update playing state of picture mode
    			 System.out.println("Note / _dispatchTouchEvent / MotionEvent.ACTION_DOWN / mPager.getCurrentItem() =" + mPager.getCurrentItem());

				if(picUI == null) {
					System.out.println("Note / _dispatchTouchEvent / picUI == null");
					picUI = new Note_UI(act,mPager, mPager.getCurrentItem());
					picUI.tempShow_picViewUI(5000,getCurrentPictureString());//1st touch to turn on UI
				}
				else
				{
					System.out.println("Note / _dispatchTouchEvent / picUI != null");
					if(picUI.handler != null)
						picUI.handler.removeCallbacks(picUI.runnable);

					picUI = new Note_UI(act,mPager, mPager.getCurrentItem());


					if((UtilVideo.mVideoView != null) && (UtilVideo.getVideoState() != UtilVideo.VIDEO_AT_STOP) )
					{
						if(Note_UI.isWithinDelay) {
							if(!Note_UI.showSeekBarProgress)
								picUI.tempShow_picViewUI(100,getCurrentPictureString());//2nd touch to turn off UI
							else
								picUI.tempShow_picViewUI(1000, getCurrentPictureString());//2nd touch to turn off UI
						}
						else
							picUI.tempShow_picViewUI(5008, getCurrentPictureString());//2nd touch to turn off UI
					}
					else
					{
						if(Note_UI.isWithinDelay) {
							picUI.tempShow_picViewUI(100,getCurrentPictureString());//2nd touch to turn off UI
						}
						else
							picUI.tempShow_picViewUI(5001, getCurrentPictureString());//2nd touch to turn off UI
					}

				}
    	  	  	 break;
	        case MotionEvent.ACTION_MOVE: 
	        case MotionEvent.ACTION_UP:
	        case MotionEvent.ACTION_POINTER_UP:
	        case MotionEvent.ACTION_CANCEL: 
	        	 break;
        }

        return super.dispatchTouchEvent(event);
    }

	public static void stopAV()
	{
		if(AudioPlayer.getPlayMode() == AudioPlayer.ONE_TIME_MODE)
			UtilAudio.stopAudioPlayer();

		VideoPlayer.stopVideo();
	}

	public static void changeToNext(ViewPager mPager)
	{
		mPager.setCurrentItem(mPager.getCurrentItem() + 1);
	}

	public static void changeToPrevious(ViewPager mPager)
	{
		mPager.setCurrentItem(mPager.getCurrentItem() + 1);
	}

    // Show full screen picture when device orientation and image orientation are the same
    boolean canShowFullScreenPicture()
    {
        String pictureStr = mDb_page.getNotePictureUri(mCurrentPosition,true);
        System.out.println(" Note / _canShowFullPicture / pictureStr = " +pictureStr);
        if( !Util.isEmptyString(pictureStr) &&
            ( (Util.isLandscapeOrientation(act) && UtilImage.isLandscapePicture(pictureStr) ) ||
            (!Util.isLandscapeOrientation(act) && !UtilImage.isLandscapePicture(pictureStr))  ) )
            return true;
        else
            return false;
    }
}