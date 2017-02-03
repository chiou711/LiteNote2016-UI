package com.cw.litenote.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.cw.litenote.R;
import com.cw.litenote.config.Config;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.DeleteFileAlarmReceiver;
import com.cw.litenote.config.Export_toSDCardFragment;
import com.cw.litenote.config.Import_fromSDCardFragment;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.NoisyAudioStreamReceiver;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.GalleryGridAct;
import com.cw.litenote.util.image.SlideshowInfo;
import com.cw.litenote.util.image.SlideshowPlayer;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.EULA_dlg;
import com.cw.litenote.util.MailNotes;
import com.cw.litenote.util.OnBackPressedListener;
import com.cw.litenote.config.MailPagesFragment;
import com.cw.litenote.util.Util;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainAct extends FragmentActivity implements OnBackStackChangedListener
{
    static CharSequence mFolderTitle;
    static CharSequence mAppTitle;
    public Context mContext;
    public Config mConfigFragment;
	public static boolean bEnableConfig;
    static Menu mMenu;
    public static DB_drawer mDb_drawer;
    public static DB_folder mDb_folder;
    public DB_page mDb_page;
    static List<String> mFolderTitles;
    public static int mFocus_folderPos;
	static NoisyAudioStreamReceiver noisyAudioStreamReceiver;
	static IntentFilter intentFilter;
    public static FragmentActivity mAct;
	public static FragmentManager fragmentManager;
	public static FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;
	public static int mLastOkTabId = 1;
	static SharedPreferences mPref_show_note_attribute;
	OnBackPressedListener onBackPressedListener;
    static Drawer mDrawer;
	public static Folder mFolder;

	// Main Act onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	///
//    	 StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//    	   .detectDiskReads()
//    	   .detectDiskWrites()
//    	   .detectNetwork() 
//    	   .penaltyLog()
//    	   .build());
//
//    	    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
////    	   .detectLeakedSqlLiteObjects() //??? unmark this line will cause strict mode error
//    	   .penaltyLog() 
//    	   .penaltyDeath()
//    	   .build());     	
    	///
        super.onCreate(savedInstanceState);
        
        mAct = this;
        setContentView(R.layout.drawer);

		// Show Api version
        if(Define.CODE_MODE == Define.DEBUG_MODE)
		    Toast.makeText(mAct, "API_" + Build.VERSION.SDK_INT , Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(mAct, "Welcome", Toast.LENGTH_SHORT).show();

		// Release mode: no debug message
        if(Define.CODE_MODE == Define.RELEASE_MODE)
        {
        	OutputStream nullDev = new OutputStream() 
            {
                public  void    close() {}
                public  void    flush() {}
                public  void    write(byte[] b) {}
                public  void    write(byte[] b, int off, int len) {}
                public  void    write(int b) {}
            }; 
            System.setOut( new PrintStream(nullDev));
        }
        
        //Log.d below can be disabled by applying proguard
        //1. enable proguard-android-optimize.txt in project.properties
        //2. be sure to use newest version to avoid build error
        //3. add the following in proguard-project.txt
        /*-assumenosideeffects class android.util.Log {
        public static boolean isLoggable(java.lang.String, int);
        public static int v(...);
        public static int i(...);
        public static int w(...);
        public static int d(...);
        public static int e(...);
    	}
        */
        Log.d("test log tag","start app");         
        
        System.out.println("================start application ==================");
        System.out.println("MainAct / _onCreate");

        UtilImage.getDefaultScaleInPercent(MainAct.this);
        
        mAppTitle = getTitle();
        
        mFolderTitles = new ArrayList<String>();

		Context context = getApplicationContext();

        // init Drawer DB
        mDb_drawer = new DB_drawer(context);

        // init Folder DB
		int folderTableId = Util.getPref_lastTimeView_folder_tableId(context);
        mDb_folder = new DB_folder(context, folderTableId);

        // init Page DB
		int pageTableId = Util.getPref_lastTimeView_page_tableId(context);
        mDb_page = new DB_page(context,pageTableId);

		//Add note with the link got from other App
		String intentLink = addNote_IntentLink(getIntent());
		if(!Util.isEmptyString(intentLink) )
		{
			finish();
		}
		else
		{
			// check DB
			final boolean ENABLE_DB_CHECK = true;//true;//false
			if(ENABLE_DB_CHECK)
			{
		        // list all folder tables
                Folder.listAllFolderTables(mAct);

				// recover focus
				folderTableId = Util.getPref_lastTimeView_folder_tableId(this);
	    		DB_folder.setFocusFolder_tableId(folderTableId);
				pageTableId = Util.getPref_lastTimeView_page_tableId(this);
				DB_page.setFocusPage_tableId(pageTableId);
			}//if(ENABLE_DB_CHECK)

	        // get last time folder table Id, default folder table Id: 1
	        if (savedInstanceState == null)
	        {
	        	for(int i = 0; i< mDb_drawer.getFoldersCount(); i++)
	        	{
		        	if(	mDb_drawer.getFolderTableId(i)==
		        		Util.getPref_lastTimeView_folder_tableId(this))
		        	{
		        		mFocus_folderPos =  i;
		    			System.out.println("MainAct / _onCreate /  mFocusFolderId = " + mFocus_folderPos);
		        	}
	        	}
	        	AudioPlayer.setPlayState(AudioPlayer.PLAYER_AT_STOP);
	        	UtilAudio.mIsCalledWhilePlayingAudio = false;
	        }

            // new drawer
            mDrawer = new Drawer(mAct);
            mDrawer.initDrawer();

            // new folder
            mFolder = new Folder(mAct);
			mFolder.initFolder();

	        // enable ActionBar app icon to behave as action to toggle nav drawer
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActionBar().setHomeButtonEnabled(true);
			getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(mAct)));

	        mContext = getBaseContext();
	        bEnableConfig = false;

			// add on back stack changed listener
	        fragmentManager = getSupportFragmentManager();
			mOnBackStackChangedListener = MainAct.this;//??? = this?
	        fragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);

			// register an audio stream receiver
			if(noisyAudioStreamReceiver == null)
			{
				noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
				intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY); 
				registerReceiver(noisyAudioStreamReceiver, intentFilter);
			}
		}

		// Show license dialog
		new EULA_dlg(this).show();
    }

    //Add note with Intent link
	String addNote_IntentLink(Intent intent)
	{
		Bundle extras = intent.getExtras();
		String pathOri = null;
		String path;
		if(extras != null)
			pathOri = extras.getString(Intent.EXTRA_TEXT);
		else
			System.out.println("MainAct / _addNote_IntentLink / extras == null");

		path = pathOri;

		if(!Util.isEmptyString(pathOri))
		{
			System.out.println("-------link path of Share 1 = " + pathOri);
			// for SoundCloud case, path could contain other strings before URI path
			if(pathOri.contains("http"))
			{
				String[] str = pathOri.split("http");

				for(int i=0;i< str.length;i++)
				{
					if(str[i].contains("://"))
						path = "http".concat(str[i]);
				}
			}

			System.out.println("-------link path of Share 2 = " + path);
			mDb_page.open();
			mDb_page.insertNote("", "", "", "", path, "", 0, (long) 0);// add new note, get return row Id
			mDb_page.close();
			String title;

			// save to top or to bottom
			int count = mDb_page.getNotesCount(true);
			SharedPreferences mPref_add_new_note_location = getSharedPreferences("add_new_note_option", 0);
			if( mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top") &&
					(count > 1)        )
			{
				Page.swap();
			}

			if( Util.isYouTubeLink(path))
				title = Util.getYoutubeTitle(path);
			else
				title = pathOri; //??? better way?

			Toast.makeText(this,
					getResources().getText(R.string.add_new_note_option_title) + title,
					Toast.LENGTH_SHORT)
					.show();
			return title;
		}
		else
			return null;
	}

    /*
     * Life cycle
     * 
     */

    // one Intent is already running, call it again in YouTube or Browser will run into this
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
		System.out.println("MainAct / _onNewIntent");
        addNote_IntentLink(intent);
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) 
    {
       super.onSaveInstanceState(outState);
  	   System.out.println("MainAct / onSaveInstanceState / mFocus_folderPos = " + mFocus_folderPos);
       outState.putInt("NowFolderPosition", mFocus_folderPos);
       outState.putInt("Playing_pageId", mPlaying_pageId);
       outState.putInt("Playing_folderPos", mPlaying_folderPos);
       outState.putInt("SeekBarProgress", Page.mProgress);
       outState.putInt("AudioPlayerState",AudioPlayer.getPlayState());
       outState.putBoolean("CalledWhilePlayingAudio", UtilAudio.mIsCalledWhilePlayingAudio);
       if(MainUi.mHandler != null)
    	   MainUi.mHandler.removeCallbacks(MainUi.mTabsHostRun);
       MainUi.mHandler = null;
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
		System.out.println("MainAct / _onRestoreInstanceState ");
    	if(savedInstanceState != null)
    	{
    		mFocus_folderPos = savedInstanceState.getInt("NowFolderPosition");
    		mPlaying_pageId = savedInstanceState.getInt("Playing_pageId");
    		mPlaying_folderPos = savedInstanceState.getInt("Playing_folderPos");
    		AudioPlayer.setPlayState(savedInstanceState.getInt("AudioPlayerState"));
    		Page.mProgress = savedInstanceState.getInt("SeekBarProgress");
    		UtilAudio.mIsCalledWhilePlayingAudio = savedInstanceState.getBoolean("CalledWhilePlayingAudio");
    	}    
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	System.out.println("MainAct / _onPause");
    }

	@Override
    protected void onResume() 
    {
    	System.out.println("MainAct / _onResume");

      	// To Registers a listener object to receive notification when incoming call
     	TelephonyManager telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
     	if(telMgr != null) 
     	{
     		telMgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
     	}
        super.onResume();
    }
	
    @Override
    protected void onResumeFragments() {
    	System.out.println("MainAct / _onResumeFragments ");
    	super.onResumeFragments();

		// fix: home button failed after power off/on in Config fragment
		fragmentManager.popBackStack();

        System.out.println("MainAct / _onResumeFragments / mFocus_folderPos = " + mFocus_folderPos);
    	MainUi.selectFolder(mFocus_folderPos);
    	setTitle(mFolderTitle);
    }
    
    @Override
    protected void onDestroy() 
    {
    	System.out.println("MainAct / onDestroy");
    	
    	//unregister TelephonyManager listener 
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
		// unregister an audio stream receiver
		if(noisyAudioStreamReceiver != null)
		{
			try
			{
				unregisterReceiver(noisyAudioStreamReceiver);//??? unregister here? 
			}
			catch (Exception e)
			{
			}
			noisyAudioStreamReceiver = null;
		}

        // stop audio player
        if(AudioPlayer.mMediaPlayer != null)
            UtilAudio.stopAudioPlayer();

		super.onDestroy();
    }
    
    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        System.out.println("MainAct / onPostCreate");
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawer.drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("MainAct / onConfigurationChanged");
        // Pass any configuration change to the drawer toggles
        mDrawer.drawerToggle.onConfigurationChanged(newConfig);
    }
    
    
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        System.out.println("MainAct / _onPrepareOptionsMenu");

        //??? why still got here even already call finish()after having got YouTube link
		if((mDrawer == null) || (mDrawer.drawerLayout == null))
			return false;

        // If the navigation drawer is open, hide action items related to the content view
        if(mDrawer.isDrawerOpen())
        {
        	mMenu.setGroupVisible(R.id.group0, false);
    		mMenu.setGroupVisible(R.id.group1, true);
        }
        else
        {
            setTitle(mFolderTitle);
    		mMenu.setGroupVisible(R.id.group1, false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

	@Override
    public void setTitle(CharSequence title) {
    	super.setTitle(title);
        initConfigFragment();
        setFolderTitle(title);
    }

    public static void setFolderTitle(CharSequence title) {
        if(title == null)
        {
        	title = mFolderTitle;
        	initActionBar();
            mDrawer.closeDrawer();
        }
        System.out.println("MainAct / _setFolderTitle / title = " + title);
        mAct.getActionBar().setTitle(title);
    }	    
    
	/******************************************************
	 * Menu
	 * 
	 */
    // Menu identifiers
	/*
	 * onCreate Options Menu
	 */
	public static MenuItem mSubMenuItemAudio;
	MenuItem playOrStopMusicButton;
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu)
	{
//		System.out.println("MainAct / _onCreateOptionsMenu");
		mMenu = menu;

		// inflate menu
		getMenuInflater().inflate(R.menu.main_menu, menu);

		playOrStopMusicButton = menu.findItem(R.id.PLAY_OR_STOP_MUSIC);

	    // show body
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
			menu.findItem(R.id.SHOW_BODY)
     	   		.setIcon(R.drawable.ic_menu_collapse)
				.setTitle(R.string.preview_note_body_no) ;
    	else
			menu.findItem(R.id.SHOW_BODY)
				.setIcon(R.drawable.ic_menu_expand)
				.setTitle(R.string.preview_note_body_yes) ;

    	// show draggable
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
			menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_off)
				.setTitle(R.string.draggable_no) ;
		else
			menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_on)
				.setTitle(R.string.draggable_yes) ;

		//
	    // Group 1 sub_menu for drawer operation
		//

	    // add sub_menu item: add drawer dragger setting
    	if(mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
    								.equalsIgnoreCase("yes"))
			menu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_off)
				.setTitle(R.string.draggable_no) ;
    	else
			menu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_on)
				.setTitle(R.string.draggable_yes) ;

		return super.onCreateOptionsMenu(menu);
	}
	
	/*
	 * on options item selected
	 * 
	 */
	public static SlideshowInfo slideshowInfo;
	static FragmentTransaction mFragmentTransaction;
	public static int mPlaying_pageTableId;
	public static int mPlaying_pageId;
	public static int mPlaying_folderPos;
	public static int mPlaying_folderTableId;
	public final static int REQUEST_ADD_YOUTUBE_LINK = 1;
	public final static int REQUEST_ADD_WEB_LINK = 2;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) //??? java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
    {
		MainUi.setMenuUiState(item.getItemId());

		// Go back: check if Configure fragment now
		if( (item.getItemId() == android.R.id.home ))
    	{
    		System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed ");

			if(fragmentManager.getBackStackEntryCount() > 0 )
			{
				fragmentManager.popBackStack();
				if(bEnableConfig)
				{
                    initConfigFragment();
                    initActionBar();
                    setTitle(mFolderTitle);
                    mDrawer.closeDrawer();
				}
				return true;
			}
    	}

    	
    	// The action bar home/up action should open or close the drawer.
    	// ActionBarDrawerToggle will take care of this.
    	if (mDrawer.drawerToggle.onOptionsItemSelected(item))
    	{
    		System.out.println("MainAct / _onOptionsItemSelected / drawerToggle.onOptionsItemSelected(item) == true ");
    		return true;
    	}
    	
        switch (item.getItemId())
        {
	    	case MenuId.ADD_NEW_FOLDER:
	    		MainUi.renewFirstAndLast_folderId();
	    		MainUi.addNewFolder(mAct, MainUi.mLastExist_folderTableId +1);
				return true;
				
	    	case MenuId.ENABLE_FOLDER_DRAG_AND_DROP:
            	if(mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
            			                    .equalsIgnoreCase("yes"))
            	{
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","no")
            								 .apply();
					mFolder.listView.setDragEnabled(false);
            	}
            	else
            	{
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","yes")
            								 .apply();
					mFolder.listView.setDragEnabled(true);
            	}
				mFolder.adapter.notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                return true;

			case MenuId.ADD_NEW_NOTE:
				MainUi.addNewNote(this);
				return true;

        	case MenuId.OPEN_PLAY_SUBMENU:
        		// new play instance: stop button is off
        	    if( (AudioPlayer.mMediaPlayer != null) && 
        	    	(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP))
        		{
       		    	// show Stop
           			playOrStopMusicButton.setTitle(R.string.menu_button_stop_audio);
           			playOrStopMusicButton.setIcon(R.drawable.ic_media_stop);
        	    }
        	    else
        	    {
       		    	// show Play
           			playOrStopMusicButton.setTitle(R.string.menu_button_play_audio);
           			playOrStopMusicButton.setIcon(R.drawable.ic_media_play);        	    	
        	    }
        		return true;
        	
        	case MenuId.PLAY_OR_STOP_AUDIO:
        		if( (AudioPlayer.mMediaPlayer != null) &&
        			(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP))
        		{
					UtilAudio.stopAudioPlayer();
					TabsHost.setAudioPlayingTab_WithHighlight(false);
					Page.mItemAdapter.notifyDataSetChanged();
					Page.showFooter();
					return true; // just stop playing, wait for user action
        		}
        		else
        		{
        			AudioPlayer.setPlayMode(AudioPlayer.CONTINUE_MODE);
        			AudioPlayer.mAudioIndex = 0;
       				AudioPlayer.prepareAudioInfo();
        			
        			AudioPlayer.runAudioState(this);
        			
					Page.mItemAdapter.notifyDataSetChanged();
	        		Page.showFooter();
	        		
					// update page table Id
					mPlaying_pageTableId = TabsHost.mNow_pageTableId;
					// update playing tab index
					mPlaying_pageId = TabsHost.mNow_pageId;
					// update playing drawer position
				    mPlaying_folderPos = mFocus_folderPos;
        		}
        		return true;

        	case MenuId.SLIDE_SHOW:
        		slideshowInfo = new SlideshowInfo();
    			
        		int pageTableId = Util.getPref_lastTimeView_page_tableId(this);
    			DB_page.setFocusPage_tableId(pageTableId);
    			
        		// add images for slide show
    			mDb_page.open();
        		for(int i = 0; i< mDb_page.getNotesCount(false) ; i++)
        		{
        			if(mDb_page.getNoteMarking(i,false) == 1)
        			{
        				String pictureUri = mDb_page.getNotePictureUri(i,false);
        				if((pictureUri.length() > 0) && UtilImage.hasImageExtension(pictureUri,this)) // skip empty
        					slideshowInfo.addImage(pictureUri);
        			}
        		}
        		mDb_page.close();
        		          		
        		if(slideshowInfo.imageSize() > 0)
        		{
					// create new Intent to launch the slideShow player Activity
					Intent playSlideshow = new Intent(this, SlideshowPlayer.class);
					startActivity(playSlideshow);  
        		}
        		else
        			Toast.makeText(mContext,R.string.file_not_found,Toast.LENGTH_SHORT).show();
        		return true;
				
            case MenuId.ADD_NEW_PAGE:
            	System.out.println("--- MainUi.Constant.ADD_NEW_PAGE / TabsHost.mLastExist_pageTableId = " + TabsHost.mLastExist_pageTableId);
                MainUi.addNewPage(mAct, TabsHost.mLastExist_pageTableId + 1);
                
                return true;
                
            case MenuId.CHANGE_PAGE_COLOR:
            	MainUi.changePageColor(mAct);
                return true;    
                
            case MenuId.SHIFT_PAGE:
            	MainUi.shiftPage(mAct);
                return true;  
                
            case MenuId.SHOW_BODY:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","no").apply();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","yes").apply();
            	TabsHost.updateTabChange(this);
                return true; 

            case MenuId.ENABLE_NOTE_DRAG_AND_DROP:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","no").apply();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","yes").apply();
            	TabsHost.updateTabChange(this);
                return true;

			case MenuId.EXPORT_TO_SD_CARD:
				mMenu.setGroupVisible(R.id.group0, false); //hide the menu
				DB_folder dbFolder = new DB_folder(this,DB_folder.getFocusFolder_tableId());
				if(dbFolder.getPagesCount(true)>0)
				{
					Export_toSDCardFragment exportFragment = new Export_toSDCardFragment();
					FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
					transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
					transaction.replace(R.id.content_frame, exportFragment,"export").addToBackStack(null).commit();
				}
				else
				{
					Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
				}
				return true;

			case MenuId.IMPORT_FROM_SD_CARD:
				mMenu.setGroupVisible(R.id.group0, false); //hide the menu
				Import_fromSDCardFragment importFragment = new Import_fromSDCardFragment();
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
				transaction.replace(R.id.content_frame, importFragment,"import").addToBackStack(null).commit();
				return true;

			case MenuId.SEND_PAGES:
				mMenu.setGroupVisible(R.id.group0, false); //hide the menu

				DB_folder dbFolderMail = new DB_folder(this,DB_folder.getFocusFolder_tableId());
				if(dbFolderMail.getPagesCount(true)>0)
				{
					MailPagesFragment mailFragment = new MailPagesFragment();
					FragmentTransaction transactionMail = getSupportFragmentManager().beginTransaction();
					transactionMail.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
					transactionMail.replace(R.id.content_frame, mailFragment,"mail").addToBackStack(null).commit();
				}
				else
				{
					Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
				}
            	return true;

            case MenuId.GALLERY:
				Intent i_browsePic = new Intent(this, GalleryGridAct.class);
				startActivity(i_browsePic);
            	return true; 	

            case MenuId.CONFIG_PREFERENCE:
            	mMenu.setGroupVisible(R.id.group0, false); //hide the menu
        		setTitle(R.string.settings);
        		bEnableConfig = true;
        		
            	mConfigFragment = new Config();
            	mFragmentTransaction = fragmentManager.beginTransaction();
				mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    /**
     *  on Back button pressed
     *
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("MainAct / _onBackPressed");

		if (onBackPressedListener != null)
        {
            onBackPressedListener.doBack();
        }
		else
			super.onBackPressed();
    }

    void initConfigFragment()
    {
        mConfigFragment = null;
    }

    static void initActionBar()
    {
//		mConfigFragment = null;
		bEnableConfig = false;
		mMenu.setGroupVisible(R.id.group0, true);
		mAct.getActionBar().setDisplayShowHomeEnabled(true);
		mAct.getActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawer.drawerToggle.setDrawerIndicatorEnabled(true);
    }

	@Override
	public void onBackStackChanged() {
		int backStackEntryCount = fragmentManager.getBackStackEntryCount();
		System.out.println("MainAct / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1) // Config fragment
		{
			bEnableConfig = true;
			System.out.println("MainAct / _onBackStackChanged / Config");
			getActionBar().setDisplayShowHomeEnabled(false);
			getActionBar().setDisplayHomeAsUpEnabled(true);
            mDrawer.drawerToggle.setDrawerIndicatorEnabled(false);
		}
		else if(backStackEntryCount == 0) // Folder
		{
            onBackPressedListener = null;
			bEnableConfig = false;
			System.out.println("MainAct / _onBackStackChanged / Folder");
            initConfigFragment();
            initActionBar();
            setTitle(mFolderTitle);
			invalidateOptionsMenu();
		}
	}

	public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
		this.onBackPressedListener = onBackPressedListener;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		System.out.println("MainAct / _onActivityResult ");
		String stringFileName = null;

		if(requestCode== MailNotes.EMAIL)
			stringFileName = MailNotes.mAttachmentFileName;
		else if(requestCode== MailPagesFragment.EMAIL_PAGES)
			stringFileName = MailPagesFragment.mAttachmentFileName;

		Toast.makeText(mAct,R.string.mail_exit,Toast.LENGTH_SHORT).show();

		// note: result code is always 0 (cancel), so it is not used
		new DeleteFileAlarmReceiver(mAct,
					System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//					System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					stringFileName);
	}
}