package com.cw.litenote.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.audio.AudioInfo;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.note.Note_edit;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.MailNotes;
import com.cw.litenote.util.UilCommon;
import com.cw.litenote.util.UilListViewBaseFragment;
import com.cw.litenote.util.Util;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Page extends UilListViewBaseFragment
						  implements LoaderManager.LoaderCallbacks<List<String>> 
{
	private static Cursor mCursor_note;
	public static DB_page mDb_page;
    SharedPreferences mPref_delete_warn;
	public static SharedPreferences mPref_show_note_attribute;

    private static Long mNoteNumber1 = (long) 1;
	private static String mNoteTitle1;
	private static String mNotePictureUri1;
	private static String mNoteAudioUri1;
	private static String mNoteLinkUri1;
	private static String mNoteBodyString1;
	private static int mMarkingIndex1;
	private static Long mCreateTime1;
	private static Long mNoteNumber2 ;
	private static String mNotePictureUri2;
	private static String mNoteAudioUri2;
	private static String mNoteLinkUri2;
	private static String mNoteTitle2;
	private static String mNoteBodyString2;
	private static int mMarkingIndex2;
	private static Long mCreateTime2;
	private List<Boolean> mSelectedList = new ArrayList<>();
	
	// This is the Adapter being used to display the list's data.
	NoteListAdapter mAdapter;
	public static DragSortListView mDndListView;
	private DragSortController mController;
	public static int MOVE_TO = 0;
	public static int COPY_TO = 1;
    public static int mStyle = 0;
	public static FragmentActivity mAct;
	String mClassName;
    public static int mHighlightPosition;
	public static SeekBar seekBarProgress;
	public static int media_file_length; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
	static ProgressBar mSpinner;

    public Page(){}

	// page
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		System.out.println("Page / _onActivityCreated");
		mAct = getActivity();
		mClassName = getClass().getSimpleName();

		listView = (DragSortListView)getActivity().findViewById(R.id.list1);
		mDndListView = listView;

		if(Build.VERSION.SDK_INT >= 21)
			mDndListView.setSelector(R.drawable.ripple);

	    mFooterMessage = (TextView) mAct.findViewById(R.id.footerText);
		mSpinner = (ProgressBar) getActivity().findViewById(R.id.list1_progress);
		new SpinnerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		//new ProgressBarTask().execute();
		//refer to
		// http://stackoverflow.com/questions/9119627/android-sdk-asynctask-doinbackground-not-running-subclass
		//Behavior of AsyncTask().execute(); has changed through Android versions.
		// -Before Donut (Android:1.6 API:4) tasks were executed serially,
		// -from Donut to Gingerbread (Android:2.3 API:9) tasks executed paralleled;
		// -since Honeycomb (Android:3.0 API:11) execution was switched back to sequential;
		// a new method AsyncTask().executeOnExecutor(Executor) however, was added for parallel execution.

		// show scroll thumb
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//			mDndListView.setFastScrollAlwaysVisible(true);

		mDndListView.setScrollbarFadingEnabled(true);
		mDndListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
		Util.setScrollThumb(getActivity(),mDndListView);

    	mStyle = Util.getCurrentPageStyle(getActivity());
//    	System.out.println("Page / _onActivityCreated / mStyle = " + mStyle);

    	UilCommon.init();

    	//listener: view note
    	mDndListView.setOnItemClickListener(new OnItemClickListener()
    	{   @Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
			{
    			System.out.println("Page / _onItemClick");

    			mDb_page.open();
	    		int count = mDb_page.getNotesCount(false);
	    		mDb_page.close();

				if(position < count)// avoid footer error
				{
					Intent intent;
					intent = new Intent(getActivity(), Note.class);
			        intent.putExtra("POSITION", position);
			        startActivity(intent);
				}
			}
        });

    	// listener: edit note
    	mDndListView.setOnItemLongClickListener(new OnItemLongClickListener()
    	{
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
             {
		        Intent i = new Intent(getActivity(), Note_edit.class);
				Long rowId = mDb_page.getNoteId(position,true);
		        i.putExtra("list_view_position", position);
		        i.putExtra(DB_page.KEY_NOTE_ID, rowId);
		        i.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(rowId));
		        i.putExtra(DB_page.KEY_NOTE_PICTURE_URI , mDb_page.getNotePictureUri_byId(rowId));
		        i.putExtra(DB_page.KEY_NOTE_AUDIO_URI , mDb_page.getNoteAudioUri_byId(rowId));
		        i.putExtra(DB_page.KEY_NOTE_LINK_URI , mDb_page.getNoteLinkUri_byId(rowId));
		        i.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(rowId));
		        i.putExtra(DB_page.KEY_NOTE_CREATED, mDb_page.getNoteCreatedTime_byId(rowId));
		        startActivity(i);
            	return true;
             }
	    });

        mController = buildController(mDndListView);
        mDndListView.setFloatViewManager(mController);
        mDndListView.setOnTouchListener(mController);
        //??? Custom view com/cwc/litenote/lib/DragSortListView has setOnTouchListener
        //called on it but does not override performClick
  		mDndListView.setDragEnabled(true);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new NoteListAdapter(getActivity());

		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(true); //set progress indicator

		// Prepare the loader. Either re-connect with an existing one or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}

	private class SpinnerTask extends AsyncTask <Void,Void,Void>{
	    @Override
	    protected void onPreExecute(){
			mDndListView.setVisibility(View.GONE);
			mFooterMessage.setVisibility(View.GONE);
			mSpinner.setVisibility(View.VISIBLE);
	    }

	    @Override
	    protected Void doInBackground(Void... arg0) {
			return null;   
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	    	mSpinner.setVisibility(View.GONE);
			mDndListView.setVisibility(View.VISIBLE);
			mFooterMessage.setVisibility(View.VISIBLE);
			if(!this.isCancelled())
			{
				this.cancel(true);
			}
	    }
	}
	
    // list view listener: on drag
    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener()
    {
                @Override
                public void drag(int startPosition, int endPosition) {
                	//add highlight boarder
//                    View v = mDndListView.mFloatView;
//                    v.setBackgroundColor(Color.rgb(255,128,0));
//                	v.setBackgroundResource(R.drawable.listview_item_shape_dragging);
//                    v.setPadding(0, 4, 0,4);
                }
    };
	
    // list view listener: on drop
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() 
    {
        @Override
        public void drop(int startPosition, int endPosition) {

        	int oriStartPos = startPosition;
        	int oriEndPos = endPosition;
        	
			if(startPosition >= mDb_page.getNotesCount(true)) // avoid footer error
				return;

			mSelectedList.set(startPosition, true);
			mSelectedList.set(endPosition, true);
			
			
			//reorder data base storage
			int loop = Math.abs(startPosition-endPosition);
			for(int i=0;i< loop;i++)
			{
				swapRows(startPosition,endPosition);
				if((startPosition-endPosition) >0)
					endPosition++;
				else
					endPosition--;
			}
			
			if( MainUi.isSamePageTable() &&
	     		(AudioPlayer.mMediaPlayer != null)				   )
			{
				if( (mHighlightPosition == oriEndPos)  && (oriStartPos > oriEndPos))      
				{
					mHighlightPosition = oriEndPos+1;
				}
				else if( (mHighlightPosition == oriEndPos) && (oriStartPos < oriEndPos))
				{
					mHighlightPosition = oriEndPos-1;
				}
				else if( (mHighlightPosition == oriStartPos)  && (oriStartPos > oriEndPos))      
				{
					mHighlightPosition = oriEndPos;
				}
				else if( (mHighlightPosition == oriStartPos) && (oriStartPos < oriEndPos))
				{
					mHighlightPosition = oriEndPos;
				}				
				else if(  (mHighlightPosition < oriEndPos) && 
						  (mHighlightPosition > oriStartPos)   )    
				{
					mHighlightPosition--;
				}
				else if( (mHighlightPosition > oriEndPos) && 
						 (mHighlightPosition < oriStartPos)  )
				{
					mHighlightPosition++;
				}

				AudioPlayer.mAudioIndex = mHighlightPosition;
				AudioPlayer.prepareAudioInfo();
			}
			mItemAdapter.notifyDataSetChanged();
			showFooter();
        }
    };
	
    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        
        //drag
	  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
	  		controller.setDragInitMode(DragSortController.ON_DOWN); // click
	  	else
	        controller.setDragInitMode(DragSortController.MISS); 

	  	controller.setDragHandleId(R.id.img_dragger);// handler
//        controller.setDragInitMode(DragSortController.ON_LONG_PRESS); //long click to drag
	  	controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging
//        controller.setBackgroundColor(Util.mBG_ColorArray[mStyle]);// background color when dragging
        
	  	// mark
        controller.setMarkEnabled(true);
        controller.setClickMarkId(R.id.img_check);
        controller.setMarkMode(DragSortController.ON_DOWN);//??? how to avoid conflict?
        // audio
        controller.setAudioEnabled(true);
//        controller.setClickAudioId(R.id.img_audio);
        controller.setClickAudioId(R.id.audio_block);
        controller.setAudioMode(DragSortController.ON_DOWN);

        return controller;
    }        

    @Override
    public void onResume() {
    	super.onResume();
		mDb_page = new DB_page(getActivity(),Util.getPref_lastTimeView_page_tableId(getActivity()));
    	System.out.println(mClassName + " / _onResume");

        // recover scroll Y
        mFirstVisibleIndex = Util.getPref_lastTimeView_list_view_first_visible_index(getActivity());
        mFirstVisibleIndexTop = Util.getPref_lastTimeView_list_view_first_visible_index_top(getActivity());

        // init audio block for TV dongle case
		if(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)
        	initAudioBlock();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	System.out.println("Page / _onPause");
		// make sure progress dialog will disappear after rotation
    	// to avoid exception: java.lang.IllegalArgumentException... not attached to window manager
 		if(AudioPlayer.mAudioUrlVerifyTask != null)
	 	{ 
	 		if((AudioPlayer.mAudioUrlVerifyTask.mUrlVerifyDialog != null) &&
	 		    AudioPlayer.mAudioUrlVerifyTask.mUrlVerifyDialog.isShowing()	)
	 		{
	 			AudioPlayer.mAudioUrlVerifyTask.mUrlVerifyDialog.dismiss();
	 		}
	
	 		if( (AudioPlayer.mAudioUrlVerifyTask.mAudioPrepareTask != null) &&
	 			(AudioPlayer.mAudioUrlVerifyTask.mAudioPrepareTask.mPrepareDialog != null) &&
	 			AudioPlayer.mAudioUrlVerifyTask.mAudioPrepareTask.mPrepareDialog.isShowing()	)
	 		{
	 			AudioPlayer.mAudioUrlVerifyTask.mAudioPrepareTask.mPrepareDialog.dismiss();
	 		}
 		}

		if( (AudioPlayer.mMediaPlayer != null) &&
			(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP))
		{
			audio_panel.setVisibility(View.GONE);
		}
	 }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	System.out.println(mClassName + " / onSaveInstanceState");
    }
    
	@Override
	public Loader<List<String>> onCreateLoader(int id, Bundle args) 
	{
		// This is called when a new Loader needs to be created. 
		return new NoteListLoader(getActivity());
	}
	
	@Override
	public void onLoadFinished(Loader<List<String>> loader,
							   List<String> data) 
	{
		System.out.println("Page / _onLoadFinished");
		// Set the new data in the adapter.
		mAdapter.setData(data);

		// The list should now be shown.
		if (isResumed()) 
			setListShown(true);
		else 
			setListShownNoAnimation(true);
		
		fillData();
		
		getLoaderManager().destroyLoader(0); // add for fixing callback twice
	}
	
	@Override
	public void onLoaderReset(Loader<List<String>> loader) {
		// Clear the data in the adapter.
		mAdapter.setData(null);
	}

    int mFirstVisibleIndex;
	int mFirstVisibleIndexTop;
	/**
	 * fill data
	 */
	public static Page_adapter mItemAdapter;
    public void fillData()
    {
    	System.out.println("Page / _fillData");
    	
    	// save index and top position
//    	int index = mDndListView.getFirstVisiblePosition();
//      View v = mDndListView.getChildAt(0);
//      int top = (v == null) ? 0 : v.getTop();

    	/*
        // set background color of list view
        mDndListView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);

    	//show divider color
        if(mStyle%2 == 0)
	    	mDndListView.setDivider(new ColorDrawable(0xFFffffff));//for dark
        else
          mDndListView.setDivider(new ColorDrawable(0xff000000));//for light

        mDndListView.setDividerHeight(3);
        */
    	int count = mDb_page.getNotesCount(true);
    	
    	mDb_page.open();
    	mCursor_note = DB_page.mCursor_note;
        mDb_page.close();
        
        // set adapter
        String[] from = new String[] { DB_page.KEY_NOTE_TITLE};
        int[] to = new int[] { R.id.whole_row};
        mItemAdapter = new Page_adapter(
				getActivity(),
				R.layout.page_view_row,
				mCursor_note,
				from,
				to,
				0
				);
        
        mDndListView.setAdapter(mItemAdapter);
        
		// selected list
		for(int i=0; i< count ; i++ )
		{
			mSelectedList.add(true);
			mSelectedList.set(i,true);
		}

        System.out.println("Page / _fillData / mFirstVisibleIndex = " + mFirstVisibleIndex +
                                           " , mFirstVisibleIndexTop = " + mFirstVisibleIndexTop);
        // restore index and top position
        mDndListView.setSelectionFromTop(mFirstVisibleIndex, mFirstVisibleIndexTop);
        
        mDndListView.setDropListener(onDrop);
        mDndListView.setDragListener(onDrag);
        mDndListView.setMarkListener(onMark);
        mDndListView.setAudioListener(onAudio);
		mDndListView.setOnScrollListener(onScroll);

        showFooter();
    }

    OnScrollListener onScroll = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			System.out.println("_onScrollStateChanged");
	        mFirstVisibleIndex = mDndListView.getFirstVisiblePosition();
	        View v = mDndListView.getChildAt(0);
	        mFirstVisibleIndexTop = (v == null) ? 0 : v.getTop();

			if( (TabsHost.mNow_pageId == MainAct.mPlaying_pageId)&&
				(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos) &&
					(Page.mDndListView.getChildAt(0) != null)                                      )
			{
				// do nothing when playing audio
				System.out.println("_onScrollStateChanged / do nothing");
			}
			else
            {
				// keep index and top position
				Util.setPref_lastTimeView_list_view_first_visible_index(getActivity(), mFirstVisibleIndex);
				Util.setPref_lastTimeView_list_view_first_visible_index_top(getActivity(), mFirstVisibleIndexTop);
			}
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			
//			System.out.println("_onScroll / firstVisibleItem " + firstVisibleItem);
//			System.out.println("_onScroll / visibleItemCount " + visibleItemCount);
//			System.out.println("_onScroll / totalItemCount " + totalItemCount);

		}
	};

	
	
    // swap rows
	protected static void swapRows(int startPosition, int endPosition) 
	{
		mDb_page.open();
		mNoteNumber1 = mDb_page.getNoteId(startPosition,false);
        mNoteTitle1 = mDb_page.getNoteTitle(startPosition,false);
        mNotePictureUri1 = mDb_page.getNotePictureUri(startPosition,false);
        mNoteAudioUri1 = mDb_page.getNoteAudioUri(startPosition,false);
        mNoteLinkUri1 = mDb_page.getNoteLinkUri(startPosition,false);
        mNoteBodyString1 = mDb_page.getNoteBody(startPosition,false);
        mMarkingIndex1 = mDb_page.getNoteMarking(startPosition,false);
    	mCreateTime1 = mDb_page.getNoteCreatedTime(startPosition,false);

		mNoteNumber2 = mDb_page.getNoteId(endPosition,false);
        mNoteTitle2 = mDb_page.getNoteTitle(endPosition,false);
        mNotePictureUri2 = mDb_page.getNotePictureUri(endPosition,false);
        mNoteAudioUri2 = mDb_page.getNoteAudioUri(endPosition,false);
        mNoteLinkUri2 = mDb_page.getNoteLinkUri(endPosition,false);
        mNoteBodyString2 = mDb_page.getNoteBody(endPosition,false);
        mMarkingIndex2 = mDb_page.getNoteMarking(endPosition,false);
    	mCreateTime2 = mDb_page.getNoteCreatedTime(endPosition,false);
        mDb_page.updateNote(mNoteNumber2,
				 mNoteTitle1,
				 mNotePictureUri1,
				 mNoteAudioUri1, 
				 "", //??? TBD
				 mNoteLinkUri1,
				 mNoteBodyString1,
				 mMarkingIndex1,
				 mCreateTime1,false);		        
		
		mDb_page.updateNote(mNoteNumber1,
		 		 mNoteTitle2,
		 		 mNotePictureUri2,
		 		 mNoteAudioUri2, 
				 "", //??? TBD
				 mNoteLinkUri2,
		 		 mNoteBodyString2,
		 		 mMarkingIndex2,
		 		 mCreateTime2,false);	
		mDb_page.close();
	}

    // list view listener: on mark
    private DragSortListView.MarkListener onMark =
    new DragSortListView.MarkListener() 
	{   @Override
        public void mark(int position) 
		{
			System.out.println("Page / _onMark");

            // toggle marking
			int markingNow = toggleNoteMarking(position);

            // Stop if unmarked item is at playing state
            if(AudioPlayer.mAudioIndex == position) {
				UtilAudio.stopAudioIfNeeded();
				if(markingNow == 0)
					TabsHost.setAudioPlayingTab_WithHighlight(false);
			}

			// update list view
            mItemAdapter.notifyDataSetChanged(); //note: add this can avoid conflict of onMark and onItemClick

			// update footer
            showFooter();

			// update audio info
            if(MainUi.isSamePageTable())
            	AudioPlayer.prepareAudioInfo();
        }
    };    

	// toggle mark of note
	public static int toggleNoteMarking(int position)
	{
		int marking = 0;
		mDb_page.open();
		int count = mDb_page.getNotesCount(false);
		if(position >= count) //end of list
		{
			mDb_page.close();
			return marking;
		}

		String strNote = mDb_page.getNoteTitle(position,false);
		String strPictureUri = mDb_page.getNotePictureUri(position,false);
		String strAudioUri = mDb_page.getNoteAudioUri(position,false);
		String strLinkUri = mDb_page.getNoteLinkUri(position,false);
		String strNoteBody = mDb_page.getNoteBody(position,false);
		Long idNote =  mDb_page.getNoteId(position,false);

		// toggle the marking
		if(mDb_page.getNoteMarking(position,false) == 0) {
			mDb_page.updateNote(idNote, strNote, strPictureUri, strAudioUri, "", strLinkUri, strNoteBody, 1, 0, false); //??? TBD
			marking = 1;
		}
		else {
			mDb_page.updateNote(idNote, strNote, strPictureUri, strAudioUri, "", strLinkUri, strNoteBody, 0, 0, false); //??? TBD
			marking = 0;
		}

		mDb_page.close();
		return  marking;
	}


	public static boolean isOnAudioClick;
    // list view listener: on audio
    private DragSortListView.AudioListener onAudio = new DragSortListView.AudioListener() 
	{   @Override
        public void audio(int position) 
		{
//			System.out.println("Page / _onAudio");
			AudioPlayer.setPlayMode(AudioPlayer.CONTINUE_MODE);
			
			mDb_page.open();
    		int count = mDb_page.getNotesCount(false);
            if(position >= count) //end of list
            {
            	mDb_page.close();
            	return ;
            }
    		int marking = mDb_page.getNoteMarking(position,false);
    		String uriString = mDb_page.getNoteAudioUri(position,false);
    		mDb_page.close();

    		boolean isAudioUri = false;
    		if( !Util.isEmptyString(uriString) && (marking == 1))
    			isAudioUri = true;
    		System.out.println("Page / _onAudio / isAudioUri = " + isAudioUri);

    		boolean itemIsMarked = (marking == 1);
    		
            if(position < count) // avoid footer error
			{
				if(isAudioUri && itemIsMarked)
				{
					// cancel playing
					if(AudioPlayer.mMediaPlayer != null)
					{
						if(AudioPlayer.mMediaPlayer.isPlaying())
		   			   	{
		   					AudioPlayer.mMediaPlayer.pause();
		   			   	}
						AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunOneTimeMode);     
						AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunContinueMode);
						AudioPlayer.mMediaPlayer.release();
						AudioPlayer.mMediaPlayer = null;
					}

					isOnAudioClick = true;

					// create new Intent to play audio
					AudioPlayer.mAudioIndex = position;
					AudioPlayer.prepareAudioInfo();
					AudioPlayer.runAudioState(mAct);


					// update playing page table Id
					MainAct.mPlaying_pageTableId = TabsHost.mNow_pageTableId;
					// update playing page Id
					MainAct.mPlaying_pageId = TabsHost.mNow_pageId;
					// update playing folder position
				    MainAct.mPlaying_folderPos = MainAct.mFocus_folderPos;
				    // update playing folder table Id
					MainAct.mPlaying_folderTableId = MainAct.mDb_drawer.getFolderTableId(MainAct.mPlaying_folderPos);
					
		            mItemAdapter.notifyDataSetChanged();
				}
			}
        }
	};            

    static TextView mFooterMessage;

	// set footer
    static void showFooter()
    {
    	System.out.println("Page / _setFooter ");

		// show footer
        mFooterMessage.setTextColor(ColorSet.color_white);
        if(mFooterMessage != null) //add this for avoiding null exception when after e-Mail action
        {
            mFooterMessage.setText(getFooterMessage());
            mFooterMessage.setBackgroundColor(ColorSet.getBarColor(mAct));
        }

        // for showing audio panel
        if( (AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP) &&
             !Util.isEmptyString(AudioPlayer.mAudioStrContinueMode)       )
            showAudioPanel(AudioPlayer.mAudioStrContinueMode,true);
        else
            showAudioPanel(AudioPlayer.mAudioStrContinueMode,false);

    }

	// get footer message of list view
    static String getFooterMessage()
    {
        return mAct.getResources().getText(R.string.footer_checked).toString() +
               "/" +
               mAct.getResources().getText(R.string.footer_total).toString() +
                  ": " +
               mDb_page.getCheckedNotesCount() +
                  "/" +
               mDb_page.getNotesCount(true);
    }

    public static TextView audio_panel_title;
    static TextView audioPanel_curr_pos;
    public static ImageView audioPanel_play_button;
    static View audio_panel;

    /**
     * init audi block
     */
    public static void initAudioBlock()
    {
        System.out.println("Page / _initAudioBlock");

        audio_panel = mAct.findViewById(R.id.audio_panel);
        audio_panel_title = (TextView) audio_panel.findViewById(R.id.audio_panel_title);

		// scroll audio title to start position at landscape orientation
		// marquee of audio title is enabled for Portrait, not Landscape
		if (Util.isLandscapeOrientation(mAct))
		{
			audio_panel_title.setMovementMethod(new ScrollingMovementMethod());
			audio_panel_title.scrollTo(0,0);
		}
		else {
			// set marquee
			audio_panel_title.setSingleLine(true);
			audio_panel_title.setSelected(true);
		}

        // update play button status
        audioPanel_play_button = (ImageView) mAct.findViewById(R.id.audioPanel_play);

        UtilAudio.updateAudioPanel(audioPanel_play_button, audio_panel_title);

        if((AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP) && (!Page.isOnAudioClick))
            AudioPlayer.scrollHighlightAudioItemToVisible();

        ImageView audioPanel_previous_btn = (ImageView) mAct.findViewById(R.id.audioPanel_previous);
        audioPanel_previous_btn.setImageResource(R.drawable.ic_media_previous);

        ImageView audioPanel_next_btn = (ImageView) mAct.findViewById(R.id.audioPanel_next);
        audioPanel_next_btn.setImageResource(R.drawable.ic_media_next);

        audioPanel_curr_pos = (TextView) mAct.findViewById(R.id.audioPanel_current_pos);
        TextView audioPanel_file_length = (TextView) mAct.findViewById(R.id.audioPanel_file_length);
        TextView audioPanel_audio_number = (TextView) mAct.findViewById(R.id.audioPanel_audio_number);

        // init audio seek bar
        seekBarProgress = (SeekBar)mAct.findViewById(R.id.audioPanel_seek_bar);
        seekBarProgress.setMax(99); // It means 100% .0-99
        seekBarProgress.setProgress(mProgress);

        // seek bar behavior is not like other control item
        //, it is seen when changing drawer, so set invisible at xml
        seekBarProgress.setVisibility(View.VISIBLE);

        // show audio file length of playing
        int fileHour = Math.round((float)(media_file_length / 1000 / 60 / 60));
        int fileMin = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000) / 1000 / 60));
        int fileSec = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
        audioPanel_file_length.setText(String.format(Locale.US,"%2d", fileHour)+":" +
                String.format(Locale.US,"%02d", fileMin)+":" +
                String.format(Locale.US,"%02d", fileSec));

        // show playing audio item message
        String message = mAct.getResources().getString(R.string.menu_button_play) +
                "#" +
                (AudioPlayer.mAudioIndex +1);
        audioPanel_audio_number.setText(message);

        // add for Pause audio and wake up from key protection
        if(AudioPlayer.mMediaPlayer != null)
            update_audioPanel_progress();

        //
        // Set up listeners
        //

        // Seek bar listener
        seekBarProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if( AudioPlayer.mMediaPlayer != null  )
                {
                    int mPlayAudioPosition = (int) (((float)(media_file_length / 100)) * seekBar.getProgress());
                    AudioPlayer.mMediaPlayer.seekTo(mPlayAudioPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser)
                {
                    // show progress change
                    int currentPos = media_file_length *progress/(seekBar.getMax()+1);
                    int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
                    int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
                    int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));

                    // set current play time
                    audioPanel_curr_pos.setText(String.format(Locale.US,"%2d", curHour)+":" +
                            String.format(Locale.US,"%02d", curMin)+":" +
                            String.format(Locale.US,"%02d", curSec) );
                }
            }
        });

        // Audio play and pause button on click listener
        audioPanel_play_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AudioPlayer.runAudioState(MainAct.mAct);

                // update status
                UtilAudio.updateAudioPanel((ImageView)v, audio_panel_title); // here v is audio play button
                if(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)
                    AudioPlayer.scrollHighlightAudioItemToVisible();
            }
        });

        // Audio play previous on click button listener
        audioPanel_previous_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AudioPlayer.willPlayNext = false;
                AudioPlayer.stopAudio();
                AudioPlayer.mAudioIndex--;
                if( AudioPlayer.mAudioIndex < 0)
                    AudioPlayer.mAudioIndex++; //back to first index

                AudioPlayer.runAudioState(MainAct.mAct);

                // update status
                UtilAudio.updateAudioPanel(audioPanel_play_button, audio_panel_title);

                if(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)
                    AudioPlayer.scrollHighlightAudioItemToVisible();
            }
        });

        // Audio play next on click button listener
        audioPanel_next_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AudioPlayer.willPlayNext = true;
                AudioPlayer.stopAudio();
                AudioPlayer.mAudioIndex++;
                if( AudioPlayer.mAudioIndex >= AudioInfo.getAudioList().size())
                    AudioPlayer.mAudioIndex = 0; //back to first index

                AudioPlayer.runAudioState(MainAct.mAct);

                // update status
                UtilAudio.updateAudioPanel(audioPanel_play_button, audio_panel_title);

                if(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)
                    AudioPlayer.scrollHighlightAudioItemToVisible();
            }
        });

    }

	// set list view footer audio control
    public static void showAudioPanel(String string, boolean enable)
    {
		System.out.println("Page / _setFooterAudioControl");

        audio_panel = mAct.findViewById(R.id.audio_panel);

        // show audio panel
        if(enable) {
            audio_panel.setVisibility(View.VISIBLE);
            audio_panel_title.setVisibility(View.VISIBLE);

            // set footer message with audio name
            audio_panel_title.setText(Util.getDisplayNameByUriString(string, mAct));

            seekBarProgress.setVisibility(View.VISIBLE);
        }
        else {
            audio_panel.setVisibility(View.GONE);
        }

    }


	/*******************************************
	 * 					menu
	 *******************************************/
    // Menu identifiers
    static final int CHECK_ALL = R.id.CHECK_ALL;
    static final int UNCHECK_ALL = R.id.UNCHECK_ALL;
    static final int INVERT_SELECTED = R.id.INVERT_SELECTED;
    static final int MOVE_CHECKED_NOTE = R.id.MOVE_CHECKED_NOTE;
    static final int COPY_CHECKED_NOTE = R.id.COPY_CHECKED_NOTE;
    static final int MAIL_CHECKED_NOTE = R.id.MAIL_CHECKED_NOTE;
    static final int DELETE_CHECKED_NOTE = R.id.DELETE_CHECKED_NOTE;
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) 
        {
	        case CHECK_ALL:
	        	checkAll(1); 
	            return true;
	        case UNCHECK_ALL:
	        	checkAll(0); 
	            return true;
	        case INVERT_SELECTED:
	        	invertSelected(); 
	            return true;
	        case MOVE_CHECKED_NOTE:
	        case COPY_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
	    			int count = mDb_page.getCheckedNotesCount();
		    		String copyItems[] = new String[count];
		    		String copyItemsPicture[] = new String[count];
		    		String copyItemsLink[] = new String[count];
		    		String copyItemsAudio[] = new String[count];
		    		String copyItemsBody[] = new String[count];
		    		Long copyItemsTime[] = new Long[count];
		    		int cCopy = 0;
		    		
		    		mDb_page.open();
		    		int noteCount = mDb_page.getNotesCount(false);
		    		for(int i=0; i<noteCount; i++)
		    		{
		    			if(mDb_page.getNoteMarking(i,false) == 1)
		    			{
		    				copyItems[cCopy] = mDb_page.getNoteTitle(i,false);
		    				copyItemsPicture[cCopy] = mDb_page.getNotePictureUri(i,false);
		    				copyItemsLink[cCopy] = mDb_page.getNoteLinkUri(i,false);
		    				copyItemsAudio[cCopy] = mDb_page.getNoteAudioUri(i,false);
		    				copyItemsBody[cCopy] = mDb_page.getNoteBody(i,false);
		    				copyItemsTime[cCopy] = mDb_page.getNoteCreatedTime(i,false);
		    				cCopy++;
		    			}
		    		}
		    		mDb_page.close();
		           
		    		if(item.getItemId() == MOVE_CHECKED_NOTE)
		    			operateCheckedTo(copyItems, copyItemsPicture, copyItemsLink, copyItemsAudio, copyItemsBody, copyItemsTime, MOVE_TO); // move to
		    		else if(item.getItemId() == COPY_CHECKED_NOTE)
			    		operateCheckedTo(copyItems, copyItemsPicture, copyItemsLink, copyItemsAudio, copyItemsBody, copyItemsTime, COPY_TO);// copy to
		    			
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
					     .show();
	            return true;
	            
	        case MAIL_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
		        	// set Sent string Id
					List<Long> noteIdArray = new ArrayList<>();
					List<String> pictureFileNameList = new ArrayList<>();
	            	int j=0;
	            	mDb_page.open();
	            	int count = mDb_page.getNotesCount(false);
		    		for(int i=0; i<count; i++)
		    		{
		    			if(mDb_page.getNoteMarking(i,false) == 1)
		    			{
		    				noteIdArray.add(j, mDb_page.getNoteId(i,false));
		    				j++;
		    				
		    				String picFile = mDb_page.getNotePictureUri_byId(mDb_page.getNoteId(i,false),false,false);
		    				if((picFile != null) && (picFile.length() > 0))
		    					pictureFileNameList.add(picFile);
		    			}
		    		}
		    		mDb_page.close();

					// message
					String sentString = Util.getStringWithXmlTag(noteIdArray);
					sentString = Util.addXmlTag(sentString);

		    		// picture array
		    		int cnt = pictureFileNameList.size();
		    		String pictureFileNameArr[] = new String[cnt];
		    		for(int i=0; i < cnt ; i++ )
		    		{
		    			pictureFileNameArr[i] = pictureFileNameList.get(i);
		    		}
					new MailNotes(mAct,sentString,pictureFileNameArr);
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
						 .show();
	        	return true;
	        	
	        case DELETE_CHECKED_NOTE:
	        	if(!noItemChecked())
	        		deleteCheckedNotes();
	        	else
	    			Toast.makeText(getActivity(),
	    						   R.string.delete_checked_no_checked_items,
	    						   Toast.LENGTH_SHORT)
	    				 .show();
	            return true;     

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	
	static public void swap()
	{
        int startCursor = mDb_page.getNotesCount(true)-1;
        int endCursor = 0;
		
		//reorder data base storage for ADD_NEW_TO_TOP option
		int loop = Math.abs(startCursor-endCursor);
		for(int i=0;i< loop;i++)
		{
			swapRows(startCursor,endCursor);
			if((startCursor-endCursor) >0)
				endCursor++;
			else
				endCursor--;
		}
	}
    
	/**
	 *  check all or uncheck all
	 */
	public void checkAll(int action) 
	{
		boolean bStopAudio = false;
		mDb_page.open();
		int count = mDb_page.getNotesCount(false);
		for(int i=0; i<count; i++)
		{
			Long rowId = mDb_page.getNoteId(i,false);
			String noteTitle = mDb_page.getNoteTitle(i,false);
			String pictureUri = mDb_page.getNotePictureUri(i,false);
			String audioUri = mDb_page.getNoteAudioUri(i,false);
			String linkUri = mDb_page.getNoteLinkUri(i,false);
			String noteBody = mDb_page.getNoteBody(i,false);
			mDb_page.updateNote(rowId, noteTitle, pictureUri, audioUri, "", linkUri, noteBody , action, 0,false);// action 1:check all, 0:uncheck all
	        // Stop if unmarked item is at playing state
	        if((AudioPlayer.mAudioIndex == i) && (action == 0) ) 
	        	bStopAudio = true;		
		}
		mDb_page.close();
		
		if(bStopAudio)
			UtilAudio.stopAudioIfNeeded();	
		
		// update audio play list
        if(MainUi.isSamePageTable())
        	AudioPlayer.prepareAudioInfo();
        
		mItemAdapter.notifyDataSetChanged();
		showFooter();
	}
	
	/**
	 *  Invert Selected
	 */
	public void invertSelected() 
	{
		boolean bStopAudio = false;
		mDb_page.open();
		int count = mDb_page.getNotesCount(false);
		for(int i=0; i<count; i++)
		{
			Long rowId = mDb_page.getNoteId(i,false);
			String noteTitle = mDb_page.getNoteTitle(i,false);
			String pictureUri = mDb_page.getNotePictureUri(i,false);
			String audioUri = mDb_page.getNoteAudioUri(i,false);
			String linkUri = mDb_page.getNoteLinkUri(i,false);
			String noteBody = mDb_page.getNoteBody(i,false);
			long marking = (mDb_page.getNoteMarking(i,false)==1)?0:1;
			mDb_page.updateNote(rowId, noteTitle, pictureUri, audioUri, "", linkUri, noteBody , marking, 0,false);// action 1:check all, 0:uncheck all
	        // Stop if unmarked item is at playing state
	        if((AudioPlayer.mAudioIndex == i) && (marking == 0) ) 
	        	bStopAudio = true;			
		}
		mDb_page.close();
		
		if(bStopAudio)
			UtilAudio.stopAudioIfNeeded();	
		
		// update audio play list
        if(MainUi.isSamePageTable())
        	AudioPlayer.prepareAudioInfo();
		
		mItemAdapter.notifyDataSetChanged();
		showFooter();
	}	
	
	
    /**
     *   operate checked to: move to, copy to
     * 
     */
	void operateCheckedTo(final String[] copyItems, final String[] copyItemsPicture, final String[] copyItemsLink, 
						  final String[] copyItemsAudio, final String[] copyItemsBody,
						  final Long[] copyItemsTime, final int action)
	{
		//list all pages
		DB_folder db_folder = MainAct.mDb_folder;
		db_folder.open();
		int tabCount = db_folder.getPagesCount(false);
		final String[] pageNames = new String[tabCount];
		final int[] pageTableIds = new int[tabCount];
		for(int i=0;i<tabCount;i++)
		{
			pageNames[i] = db_folder.getPageTitle(i,false);
			pageTableIds[i] = db_folder.getPageTableId(i,false);
		}
		db_folder.close();
		
		pageNames[TabsHost.mNow_pageId] = pageNames[TabsHost.mNow_pageId] + " *"; // add mark to current page
		   
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				//keep focus page table id
				int srcPageTableId = DB_page.getFocusPage_tableId();

				//copy checked item to destination page
				int destPageTableId = pageTableIds[which];
				DB_page.setFocusPage_tableId(destPageTableId);
				for(int i=0;i< copyItems.length;i++)
				{
					int marking = 0;
					// default marking of picture or audio is 1
					if( (!Util.isEmptyString(copyItemsPicture[i])) || (!Util.isEmptyString(copyItemsAudio[i])))
						marking = 1;

					// move to same page is not allowed
					if(!((action == MOVE_TO) && (srcPageTableId == destPageTableId)))
						mDb_page.insertNote(copyItems[i],copyItemsPicture[i], copyItemsAudio[i], "", copyItemsLink[i], copyItemsBody[i],marking, copyItemsTime[i]); //??? TBD
				}
				
				//recover table Id of original page
				if((action == MOVE_TO) && (srcPageTableId != destPageTableId))
				{
					DB_page.setFocusPage_tableId(srcPageTableId);
					mDb_page.open();
					int count = mDb_page.getNotesCount(false);

                    //delete checked items that were moved
					for(int i=0; i<count; i++)
					{
						if(mDb_page.getNoteMarking(i,false) == 1)
						{
							mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
							// update playing highlight
							UtilAudio.stopAudioIfNeeded();
						}
					}
					mDb_page.close();
					
					mItemAdapter.notifyDataSetChanged();
					showFooter();
				}
				else if(action == COPY_TO)
				{
					DB_page.setFocusPage_tableId(srcPageTableId);
					if(destPageTableId == srcPageTableId)
					{
						mItemAdapter.notifyDataSetChanged();
						showFooter();
					}
				}
				
				dialog.dismiss();
			}
		};
		
		if(action == MOVE_TO)
			builder.setTitle(R.string.checked_notes_move_to_dlg);
		else if(action == COPY_TO)
			builder.setTitle(R.string.checked_notes_copy_to_dlg);
		
		builder.setSingleChoiceItems(pageNames, -1, listener)
		  	.setNegativeButton(R.string.btn_Cancel, null);
		
		// override onShow to mark current page status
		AlertDialog alertDlg = builder.create();
		alertDlg.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dlgInterface) {
				// add mark for current page
				Util util = new Util(getActivity());
				util.addMarkToCurrentPage(dlgInterface,action);
			}
		});
		alertDlg.show();
	}
	
	
	/**
	 * delete checked notes
	 */
	public void deleteCheckedNotes()
	{
		final Context context = getActivity();

		mPref_delete_warn = context.getSharedPreferences("delete_warn", 0);
    	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
           mPref_delete_warn.getString("KEY_DELETE_CHECKED_WARN","yes").equalsIgnoreCase("yes"))
    	{
			Util util = new Util(getActivity());
			util.vibrate();
    		
    		// show warning dialog
			Builder builder = new Builder(context);
			builder.setTitle(R.string.delete_checked_note_title)
					.setMessage(R.string.delete_checked_message)
					.setNegativeButton(R.string.btn_Cancel, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{/*cancel*/} })
					.setPositiveButton(R.string.btn_OK, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							mDb_page.open();
							int count = mDb_page.getNotesCount(false);
							for(int i=0; i<count; i++)
							{
								if(mDb_page.getNoteMarking(i,false) == 1)
									mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
							}
							mDb_page.close();
							
							// Stop Play/Pause if current tab's item is played and is not at Stop state
							if(AudioPlayer.mAudioIndex == Page.mHighlightPosition)
								UtilAudio.stopAudioIfNeeded();
							
							mItemAdapter.notifyDataSetChanged();
							showFooter();
						}
					});
			
	        AlertDialog d = builder.create();
	        d.show();
    	}
    	else
    	{
    		// not show warning dialog
    		mDb_page.open();
    		int count = mDb_page.getNotesCount(false);
			for(int i=0; i<count; i++)
			{
				if(mDb_page.getNoteMarking(i,false) == 1)
					mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
			}
			mDb_page.close();
			
			mItemAdapter.notifyDataSetChanged();
			showFooter();
    	}
	}
    
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	boolean noItemChecked()
	{
		int checkedItemCount = mDb_page.getCheckedNotesCount();
		return (checkedItemCount == 0);
	}
	
	/*
	 * inner class for note list loader
	 */
	public static class NoteListLoader extends AsyncTaskLoader<List<String>> 
	{
		List<String> mApps;

		NoteListLoader(Context context) {
			super(context);

		}

		@Override
		public List<String> loadInBackground() {
			return new ArrayList<>();
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}
	}

	/*
	 * 	inner class for note list adapter
	 */
	public static class NoteListAdapter extends ArrayAdapter<String> 
	{
		NoteListAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}
		public void setData(List<String> data) {
			clear();
			if (data != null) {		
					addAll(data);
			}
		}
	}

	
	// update audio panel progress
	public static int mProgress;
    public static void update_audioPanel_progress()
    {
		if(!mDndListView.isShown())
			return;

//		System.out.println("Page / _update_audioPanel_progress");

		// get current playing position
    	int currentPos = AudioPlayer.mMediaPlayer.getCurrentPosition();
    	int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
    	int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
     	int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));

		// set current playing time
    	audioPanel_curr_pos.setText(String.format(Locale.US,"%2d", curHour)+":" +
    										   String.format(Locale.US,"%02d", curMin)+":" +
    										   String.format(Locale.US,"%02d", curSec) );//??? why affect audio title?

		// set current progress
		mProgress = (int)(((float)currentPos/ media_file_length)*100);
    	seekBarProgress.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
    }
    
    
}
