package com.cw.litenote.main;

import java.util.ArrayList;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

public class TabsHost extends Fragment
{
    static FragmentTabHost mTabsHost;
    static int mPagesCount;
	static String TAB_SPEC_PREFIX = "tab";
	static String TAB_SPEC;
	static String mClassName;
	// for DB
	public static DB_folder mDbFolder;
	private static Cursor mPageCursor;
	
	static SharedPreferences mPref_FinalPageViewed;
	private static SharedPreferences mPref_delete_warn;
	public static int mFinalPageViewed_pageId;
	public static int mNow_pageId;
	public static int mNow_pageTableId;
	static ArrayList<String> mTabIndicator_ArrayList = new ArrayList<>();
	static int mFirstExist_PageId =0;
	static int mLastExist_pageId =0;
	static int mLastExist_pageTableId;
	static HorizontalScrollView mHorScrollView;
    public static Activity mAct;

    public TabsHost(){}
    
	@Override
	public void onCreate(final Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        // get final viewed table Id
		mAct = getActivity();
		int tableId = Util.getPref_lastTimeView_page_tableId(mAct);
		mClassName = getClass().getSimpleName();
		//System.out.println("TabsHost / onCreate / strFinalPageViewed_tableId = " + tableId);
        System.out.println(mClassName + " / onCreate / strFinalPageViewed_tableId = " + tableId);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
    	System.out.println("TabsHost / _onCreateView");
		View rootView;

		// set layout by orientation
		if (Util.isLandscapeOrientation(mAct))
		{
			rootView = inflater.inflate(R.layout.page_view_landscape, container, false);
		}
		else
		{
			rootView = inflater.inflate(R.layout.page_view_portrait, container, false);
		}

        setRootView(rootView);

		if(mDbFolder != null)
			mDbFolder.close();
		mDbFolder = new DB_folder(mAct,Util.getPref_lastTimeView_folder_tableId(mAct));

        setTabHost();
        setTab(mAct);
        
        return rootView;
    }	
	
	@Override
	public void onResume() {
		super.onResume();
		System.out.println("TabsHost / onResume");
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
//		System.out.println("TabsHost / onPause");
		if( (mTabsHost != null) && MainAct.bEnableConfig)
			mTabsHost.clearAllTabs(); // workaround: clear for changing to Config
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
//		System.out.println("TabsHost / onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onStop() {
//		System.out.println("TabsHost / onStop");
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		System.out.println("TabsHost / onDestroy");
		if(mTabsHost != null)
			mTabsHost.clearAllTabs(); // clear for changing drawer
	}
    
    static View mRootView;
	private void setRootView(View rootView) {
		mRootView = rootView;
	}
	
	private static View getRootView()
	{
		return mRootView;
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("TabsHost / _onConfigurationChanged");

		//for layout configuration change
        MainUi.selectFolder(MainAct.mFocus_folderPos);
    }

    /**
	 * set tab host
	 * 
	 */
	protected void setTabHost()
	{
		// declare tab widget
        TabWidget tabWidget = (TabWidget) getRootView().findViewById(android.R.id.tabs);
        
        // declare linear layout
        LinearLayout linearLayout = (LinearLayout) tabWidget.getParent();
        
        // set horizontal scroll view
        HorizontalScrollView horScrollView = new HorizontalScrollView(mAct);
        horScrollView.setLayoutParams(new FrameLayout.LayoutParams(
								            FrameLayout.LayoutParams.MATCH_PARENT,
								            FrameLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(horScrollView, 0);
        linearLayout.removeView(tabWidget);
        
        horScrollView.addView(tabWidget);
        horScrollView.setHorizontalScrollBarEnabled(true); //set scroll bar
        horScrollView.setHorizontalFadingEdgeEnabled(true); // set fading edge
        mHorScrollView = horScrollView;

		// tab host
        mTabsHost = (FragmentTabHost)getRootView().findViewById(android.R.id.tabhost);
        
        //for android-support-v4.jar
        //mTabsHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
        
        //add frame layout for android-support-v13.jar
        //Note: must use getChildFragmentManager() for nested fragment
        mTabsHost.setup(mAct, getChildFragmentManager(), android.R.id.tabcontent);
	}
	
	static void setTab(Activity act)
	{
//		System.out.println("TabsHost/ _setTab");
        //set tab indicator
    	setTabIndicator(act);
    	
    	// set tab listener
    	setTabChangeListener(act);
    	setTabEditListener(act);
	}
	
	/**
	 * set tab indicator
	 * 
	 */
	protected static void setTabIndicator(final Activity act)
	{
		int folderTableId = DB_folder.getFocusFolder_tableId();
		System.out.println("TabsHost / _setTabIndicator / folderTableId = " + folderTableId);
		
		// get final viewed table Id
        int tableId = Util.getPref_lastTimeView_page_tableId(act);
		
		mDbFolder.open();
		mPagesCount = mDbFolder.getPagesCount(false);
		System.out.println("TabsHost / _setTabIndicator / mPagesCount = " + mPagesCount);

		// get first tab id and last tab id
		int i = 0;
		while(i < mPagesCount)
    	{
    		mTabIndicator_ArrayList.add(i, mDbFolder.getPageTitle(i, false));
    		
    		int pageId = mDbFolder.getPageId(i, false);
    		
    		mPageCursor = mDbFolder.getPageCursor();
    		mPageCursor.moveToPosition(i);
    		
			if(mPageCursor.isFirst())
			{
				mFirstExist_PageId = pageId ;
			}
			
			if(mPageCursor.isLast())
			{
				mLastExist_pageId = pageId ;
			}
			i++;
    	}
    	
		mLastExist_pageTableId = 0;
		// get final view table id of last time
		for(int iPosition = 0; iPosition< mPagesCount; iPosition++)
		{
			int pageTableId = mDbFolder.getPageTableId(iPosition,false);
			if(tableId == pageTableId)
			{
				mFinalPageViewed_pageId = iPosition;	// starts from 0
			}
			
			if( pageTableId >= mLastExist_pageTableId)
				mLastExist_pageTableId = pageTableId;
		}
		mDbFolder.close();
		
		System.out.println("TabsHost / mLastExist_pageTableId = " + mLastExist_pageTableId);
		
    	//add tab
//        mTabsHost.getTabWidget().setStripEnabled(true); // enable strip
        i = 0;
        while(i < mPagesCount)
        {
            TAB_SPEC = TAB_SPEC_PREFIX.concat(String.valueOf(mDbFolder.getPageId(i,true)));
//        	System.out.println(mClassName + " / addTab / " + i);
            mTabsHost.addTab(mTabsHost.newTabSpec(TAB_SPEC).setIndicator(mTabIndicator_ArrayList.get(i)),
							 Page.class, //interconnection
							 null);
            
            //set round corner and background color
            int style = mDbFolder.getPageStyle(i, true);
    		switch(style)
    		{
    			case 0:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_0);
    				break;
    			case 1:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_1);
    				break;
    			case 2:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_2);
    				break;
    			case 3:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_3);
    				break;
    			case 4:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_4);
    				break;	
    			case 5:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_5);
    				break;	
    			case 6:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_6);
    				break;	
    			case 7:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_7);
    				break;	
    			case 8:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_8);
    				break;		
    			case 9:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_9);
    				break;		
    			default:
    				break;
    		}
    		
            //set text color
	        TextView tv = (TextView) mTabsHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
		    if((style%2) == 1)
    		{	
		        tv.setTextColor(Color.argb(255,0,0,0));
    		}
           	else
           	{
		        tv.setTextColor(Color.argb(255,255,255,255));
           	}
            // set tab text center
	    	int tabCount = mTabsHost.getTabWidget().getTabCount();
	    	for (int j = 0; j < tabCount; j++) {
	    	    final View view = mTabsHost.getTabWidget().getChildTabViewAt(j);
	    	    if ( view != null ) {
	    	        //  get title text view
	    	        final View textView = view.findViewById(android.R.id.title);
	    	        if ( textView instanceof TextView ) {
	    	            ((TextView) textView).setGravity(Gravity.CENTER);
	    	            ((TextView) textView).setSingleLine(true);
	    	            textView.setPadding(6, 0, 6, 0);
	    	            textView.setMinimumWidth(96);
	    	            ((TextView) textView).setMaxWidth(UtilImage.getScreenWidth(act)/2);
	    	            textView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
	    	        }
	    	    }
	    	}
	    	i++;
        }
        
        setTabMargin(act);

		mNow_pageId = mFinalPageViewed_pageId;
		
		System.out.println("TabsHost / setTabIndicator / mNow_pageId = " + mNow_pageId);
		
		//set background color to selected tab 
		mTabsHost.setCurrentTab(mNow_pageId);
        
		// scroll to last view
        mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
		        mPref_FinalPageViewed = act.getSharedPreferences("last_time_view", 0);
		        int scrollX = Util.getPref_lastTimeView_scrollX_byFolderTableId(act);
	        	mHorScrollView.scrollTo(scrollX, 0);
	            updateTabSpec(mTabsHost.getCurrentTabTag(),act);
	        } 
	    });
        
	}
	
	public static void setAudioPlayingTab_WithHighlight(boolean highlightIsOn)
	{
		// get first tab id and last tab id
		int tabCount = mTabsHost.getTabWidget().getTabCount();
		for (int i = 0; i < tabCount; i++)	
		{
	        TextView textView= (TextView) mTabsHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
			if(highlightIsOn && (MainAct.mPlaying_pageId == i))
			    textView.setTextColor(ColorSet.getHighlightColor(mAct));
			else
			{
		        int style = mDbFolder.getPageStyle(i, true);
			    if((style%2) == 1)
				{	
			        textView.setTextColor(Color.argb(255,0,0,0));
				}
		       	else
		       	{
			        textView.setTextColor(Color.argb(255,255,255,255));
		       	}
			}
		}
	}

	static void setTabMargin(Activity activity)
	{
    	mTabsHost.getTabWidget().setShowDividers(TabWidget.SHOW_DIVIDER_MIDDLE);
        mTabsHost.getTabWidget().setDividerDrawable(R.drawable.ic_tab_divider);
    	
        TabWidget tabWidget = (TabWidget) getRootView().findViewById(android.R.id.tabs);
        
        LinearLayout.LayoutParams tabWidgetLayout;
        for (int j = 0; j < mPagesCount; j++)
        {
        	tabWidgetLayout = (LinearLayout.LayoutParams) tabWidget.getChildAt(j).getLayoutParams();
        	int oriLeftMargin = tabWidgetLayout.leftMargin;
        	int oriRightMargin = tabWidgetLayout.rightMargin;
        	
        	// fix right edge been cut issue when single one note
        	if(mPagesCount == 1)
        		oriRightMargin = 0;
        	
        	if (j == 0) {
        		tabWidgetLayout.setMargins(0, 2, oriRightMargin, 5);
        	} else if (j == (mPagesCount - 1)) {
        		tabWidgetLayout.setMargins(oriLeftMargin, 2, 0, 5);
        	} else {
        		tabWidgetLayout.setMargins(oriLeftMargin, 2, oriRightMargin, 5);
        	}
        }
        tabWidget.requestLayout();
	}
	
	
	/**
	 * set tab change listener
	 * 
	 */
	static String mTabSpec;
	protected static void setTabChangeListener(final Activity activity)
	{
        // set on tab changed listener
	    mTabsHost.setOnTabChangedListener(new OnTabChangeListener()
	    {
			@Override
			public void onTabChanged(String tabSpec)
			{
				System.out.println(mClassName + " / onTabChanged");
				mTabSpec = tabSpec;
				updateTabSpec(tabSpec,activity);
			}
		}
	    );    
	}
	
	static void updateTabSpec(String tabSpec,Activity activity)
	{
//		System.out.println("TabsHost / _updateTabSpec");
		// get scroll X
		int scrollX = mHorScrollView.getScrollX();
		
		//update final page currently viewed: scroll x
        mPref_FinalPageViewed = activity.getSharedPreferences("last_time_view", 0);
    	Util.setPref_lastTimeView_scrollX_byFolderTableId(activity, scrollX );
		
    	mDbFolder.open();
		int pagesCount = mDbFolder.getPagesCount(false);
		for(int i=0;i<pagesCount;i++)
		{
			int iTabId = mDbFolder.getPageId(i, false);
			int pageTableId = mDbFolder.getPageTableId(i, false);
			TAB_SPEC = TAB_SPEC_PREFIX.concat(String.valueOf(iTabId)); // TAB_SPEC starts from 1
	    	
			if(TAB_SPEC.equals(tabSpec) )
	    	{
	    		mNow_pageId = i;
	    		//update final page currently viewed: tab Id
	    		Util.setPref_lastTimeView_page_tableId(activity,pageTableId);

				// get current playing page table Id
				mNow_pageTableId = Util.getPref_lastTimeView_page_tableId(activity);
	    		DB_page.setFocusPage_tableId(pageTableId);
	    		System.out.println(mClassName + " / _updateTabSpec / tabSpec = " + tabSpec);
	    	} 
		}
		mDbFolder.close();
		
    	// set current audio playing tab with highlight
		if( (AudioPlayer.mMediaPlayer != null) &&
			(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)&&
		    (MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos))
			setAudioPlayingTab_WithHighlight(true);
		else
			setAudioPlayingTab_WithHighlight(false);
	}
	
	/**
	 * set tab Edit listener
	 *
	 */
	protected static void setTabEditListener(final Activity activity)
	{
	    // set listener for editing tab info
	    int i = 0;
	    while(i < mPagesCount)
		{
			final int tabCursor = i;
			View tabView= mTabsHost.getTabWidget().getChildAt(i);
			
			// on long click listener
			tabView.setOnLongClickListener(new OnLongClickListener() 
	    	{	
				@Override
				public boolean onLongClick(View v) 
				{
					editPageTitle(tabCursor, activity);
					return true;
				}
			});
			i++;
		}
	}
	
	/**
	 * delete page
	 * 
	 */
	public static  void deletePage(int TabId, final Activity activity)
	{
		mDbFolder.open();
		// check if only one page left
		int pagesCount = mDbFolder.getPagesCount(false);
		if(pagesCount != 1)
		{
			final int tabId =  mDbFolder.getPageId(mNow_pageId, false);
			//if current page is the first page and will be delete,
			//try to get next existence of note page
			System.out.println("deletePage / mCurrentTabIndex = " + mNow_pageId);
			System.out.println("deletePage / mFirstExist_PageId = " + mFirstExist_PageId);
	        if(tabId == mFirstExist_PageId)
	        {
	        	int cGetNextExistIndex = mNow_pageId +1;
	        	boolean bGotNext = false;
				while(!bGotNext){
		        	try{
		        	   	mFirstExist_PageId =  mDbFolder.getPageId(cGetNextExistIndex, false);
		        		bGotNext = true;
		        	}catch(Exception e){
    		        	 bGotNext = false;
    		        	 cGetNextExistIndex++;}}		            		        	
	        }
            
	        //change to first existing page
	        int newFirstPageTblId = 0;
	        for(int i=0 ; i<pagesCount; i++)
	        {
	        	if(	mDbFolder.getPageId(i, false)== mFirstExist_PageId)
	        	{
	        		newFirstPageTblId =  mDbFolder.getPageTableId(i, false);
	    			System.out.println("deletePage / newFirstPageTblId = " + newFirstPageTblId);
	        	}
	        }
	        System.out.println("--- after delete / newFirstPageTblId = " + newFirstPageTblId);
	        Util.setPref_lastTimeView_page_tableId(activity, newFirstPageTblId);
		}
		else{
             Toast.makeText(activity, R.string.toast_keep_one_page , Toast.LENGTH_SHORT).show();
             return;
		}
		mDbFolder.close();
		
		// set scroll X
		int scrollX = 0; //over the last scroll X
        mPref_FinalPageViewed = activity.getSharedPreferences("last_time_view", 0);
    	Util.setPref_lastTimeView_scrollX_byFolderTableId(activity, scrollX );
	 	  
		
		// get page table Id for dropping
		int pageTableId = mDbFolder.getPageTableId(mNow_pageId, true);
		System.out.println("TabsHost / _deletePage / pageTableId =  " + pageTableId);
		
 	    // delete tab name
		mDbFolder.dropPageTable(pageTableId);
		mDbFolder.deletePage(DB_folder.getFocusFolder_tableName(),TabId);
		mPagesCount--;
		
		// After Delete page, update highlight tab
    	if(mNow_pageId < MainAct.mPlaying_pageId)
    	{
    		MainAct.mPlaying_pageId--;
    	}
    	else if(mNow_pageId == MainAct.mPlaying_pageId)
    	{
    		if(AudioPlayer.mMediaPlayer != null)
    		{
    			UtilAudio.stopAudioPlayer();
				AudioPlayer.mAudioIndex = 0;
				AudioPlayer.setPlayState(AudioPlayer.PLAYER_AT_STOP);
    		}    		
    	}
    	
    	// update change after deleting tab
		updateTabChange(activity);
    	
    	// Note: _onTabChanged will reset scroll X to another value,
    	// so we need to add the following to set scroll X again
        mHorScrollView.post(new Runnable() 
        {
	        @Override
	        public void run() {
	        	mHorScrollView.scrollTo(0, 0);
	        	Util.setPref_lastTimeView_scrollX_byFolderTableId(activity, 0 );
	        }
	    });
	}

	/**
	 * edit page title
	 * 
	 */
	public static int mStyle = 0;
	static void editPageTitle(int pageCursor, final Activity act)
	{
		final int pageId = mDbFolder.getPageId(pageCursor, true);
		mDbFolder.open();
		mPageCursor = mDbFolder.getPageCursor();
		if(mPageCursor.isFirst())
			mFirstExist_PageId = pageId;
		mDbFolder.close();

		// get tab name
		String title = mDbFolder.getPageTitle(pageCursor, true);
		
		if(pageCursor == mNow_pageId)
		{
	        final EditText editText1 = new EditText(act.getBaseContext());
	        editText1.setText(title);
	        editText1.setSelection(title.length()); // set edit text start position
	        //update tab info
	        Builder builder = new Builder(mTabsHost.getContext());
	        builder.setTitle(R.string.edit_page_tab_title)
	                .setMessage(R.string.edit_page_tab_message)
	                .setView(editText1)   
	                .setNegativeButton(R.string.btn_Cancel, new OnClickListener()
	                {   @Override
	                    public void onClick(DialogInterface dialog, int which)
	                    {/*cancel*/}
	                })
	                .setNeutralButton(R.string.edit_page_button_delete, new OnClickListener()
	                {   @Override
	                    public void onClick(DialogInterface dialog, int which)
	                	{
	                		// delete
							//warning:start
		                	mPref_delete_warn = act.getSharedPreferences("delete_warn", 0);
		                	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
		 	                   mPref_delete_warn.getString("KEY_DELETE_PAGE_WARN","yes").equalsIgnoreCase("yes")) 
		                	{
		            			Util util = new Util(act);
		        				util.vibrate();
		        				
		                		Builder builder1 = new Builder(mTabsHost.getContext());
		                		builder1.setTitle(R.string.confirm_dialog_title)
	                            .setMessage(R.string.confirm_dialog_message_page)
	                            .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener(){
	                            	@Override
	                                public void onClick(DialogInterface dialog1, int which1){
	                            		/*nothing to do*/}})
	                            .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener(){
	                            	@Override
	                                public void onClick(DialogInterface dialog1, int which1){
	                                	deletePage(pageId, act);
	                            	}})
	                            .show();
		                	} //warning:end
		                	else
		                	{
		                		deletePage(pageId, act);
		                	}
		                	
	                    }
	                })	
	                .setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	                {   @Override
	                    public void onClick(DialogInterface dialog, int which)
	                    {
	                		// save
        					final int pageId =  mDbFolder.getPageId(mNow_pageId, true);
        					final int pageTableId =  mDbFolder.getPageTableId(mNow_pageId, true);
        					
	                        int tabStyle = mDbFolder.getPageStyle(mNow_pageId, true);
							mDbFolder.updatePage(pageId,
                                                 editText1.getText().toString(),
                                                 pageTableId,
                                                 tabStyle,
                                                 true);
	                        
							// Before _recreate, store latest page number currently viewed
							Util.setPref_lastTimeView_page_tableId(act, pageTableId);
	                        
	                        updateTabChange(act);
	                    }
	                })	
	                .setIcon(android.R.drawable.ic_menu_edit);
	        
			        AlertDialog d1 = builder.create();
			        d1.show();
			        // android.R.id.button1 for positive: save
			        ((Button)d1.findViewById(android.R.id.button1))
			        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
			        
			        // android.R.id.button2 for negative: color 
			        ((Button)d1.findViewById(android.R.id.button2))
  			        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
			        
			        // android.R.id.button3 for neutral: delete
			        ((Button)d1.findViewById(android.R.id.button3))
			        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
			}
	}
	
    
	/**
	 * update tab change 
	 */
	static void updateTabChange(Activity act)
	{
//		System.out.println("TabsHost / _updateChange ");
		mTabsHost.clearAllTabs(); //must add this in order to clear onTanChange event
    	setTab(act);
	}    
	
	static public int getLastExist_TabId()
	{
		return mLastExist_pageId;
	}
	
	static public void setLastExist_tabId(int lastTabId)
	{
		mLastExist_pageId = lastTabId;
	}
}