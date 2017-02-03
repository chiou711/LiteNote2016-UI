package com.cw.litenote.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.cw.litenote.R;
import com.cw.litenote.config.Import_selectedFileAct;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.note.Note_addAudio;
import com.cw.litenote.note.Note_addCameraImage;
import com.cw.litenote.note.Note_addCameraVideo;
import com.cw.litenote.note.Note_addLink;
import com.cw.litenote.note.Note_addText;
import com.cw.litenote.note.Note_addNew_optional;
import com.cw.litenote.note.Note_addNew_optional_for_multiple;
import com.cw.litenote.note.Note_addReadyImage;
import com.cw.litenote.note.Note_addReadyVideo;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.Util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.cw.litenote.main.MainAct.REQUEST_ADD_WEB_LINK;
import static com.cw.litenote.main.MainAct.REQUEST_ADD_YOUTUBE_LINK;


public class MainUi 
{
	MainUi(){}


	static Folder.FolderListener_click folderClick;
	static Folder.FolderListener_longClick folderLongClick;

	static int mMenuUiState;

	public static int getMenuUiState() {
		return mMenuUiState;
	}

	public static void setMenuUiState(int mMenuState) {
		MainUi.mMenuUiState = mMenuState;
	}


	static void addFolderListeners()
	{
		folderClick = new Folder.FolderListener_click();
		folderLongClick = new Folder.FolderListener_longClick();
	}

    /**
     * Add new folder: 2 dialogs
     * 
     */
	public static  void addNewFolder_2dialogs(final Activity act, final int newTableId)
	{
		final DB_drawer db_drawer = new DB_drawer(act);
		final DB_folder db_folder = new DB_folder(act,newTableId);

		// get folder name
		String folderName = act.getResources()
				            .getString(R.string.default_folder_name)
                            .concat(String.valueOf(newTableId));
        
        final EditText editText1 = new EditText(act.getBaseContext());
        editText1.setText(folderName);
        editText1.setSelection(folderName.length()); // set edit text start position
        
        //update tab info
        Builder builder = new Builder(act);
        builder.setTitle(R.string.edit_folder_title)
                .setMessage(R.string.edit_folder_message)
                .setView(editText1)   
                .setNegativeButton(R.string.edit_page_button_ignore, new OnClickListener(){   
                	@Override
                    public void onClick(DialogInterface dialog, int which)
                    {/*nothing*/}
                })
                .setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
                {   @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                		
	    	            final String[] items = new String[]{
	    	            		act.getResources().getText(R.string.add_new_note_top).toString(),
	    	            		act.getResources().getText(R.string.add_new_note_bottom).toString() };
	    	            
						AlertDialog.Builder builder = new AlertDialog.Builder(act);
						  
						builder.setTitle(R.string.add_new_page_select_position)
						.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {
								
				        	String folderTitle =  editText1.getText().toString();
				        	MainAct.mFolderTitles.add(folderTitle);
				    		// insert new drawer Id and Title
				        	db_drawer.insertFolder(newTableId, folderTitle );
				        	
				        	// insert folder table
				        	db_drawer.insertFolderTable(db_drawer,newTableId, false);
				        	
				    		// insert page table
				    		for(int i = 1; i<= Define.ORIGIN_PAGES_COUNT; i++)
				    		{
				    			db_folder.insertPageTable(db_folder,newTableId, i, false);
				    		}
				    		
				    		// add new drawer to the top
							if(which == 0)
							{
						        int startCursor = db_drawer.getFoldersCount()-1;
						        int endCursor = 0;
								
								//reorder data base storage for ADD_NEW_TO_TOP option
								int loop = Math.abs(startCursor-endCursor);
								for(int i=0;i< loop;i++)
								{
									swapFolderRows(startCursor,endCursor);
									if((startCursor-endCursor) >0)
										endCursor++;
									else
										endCursor--;
								}
								
								// focus position is 0 for Add to top
								MainAct.mFocus_folderPos = 0;
								Util.setPref_lastTimeView_folder_tableId(act,db_drawer.getFolderTableId(MainAct.mFocus_folderPos) );
								
								// update playing highlight if needed
								if(AudioPlayer.mMediaPlayer != null) 
									MainAct.mPlaying_folderPos++;
							}				    		
				    		
							MainAct.mFolder.adapter.notifyDataSetChanged();
				    		
				        	//end
							dialog.dismiss();
							
							updateFocus_folderPosition();
						}})
						.setNegativeButton(R.string.btn_Cancel, null)
						.show();
                    }
                })	 
                .setIcon(android.R.drawable.ic_menu_edit);
        
	        final AlertDialog dlg = builder.create();
	        dlg.show();
	        // android.R.id.button1 for negative: cancel 
	        ((Button)dlg.findViewById(android.R.id.button1))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
	        // android.R.id.button2 for positive: save
	        ((Button)dlg.findViewById(android.R.id.button2))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	}

	/**
	 *
	 * 	Add new note
	 *
	 */
    static List<NewNote> mAddNote;

    private final static int ID_NEW_TEXT = 1;
    private final static int ID_NEW_AUDIO = 2;
    private final static int ID_NEW_CAMERA_IMAGE = 3;
    private final static int ID_NEW_READY_IMAGE = 4;
    private final static int ID_NEW_CAMERA_VIDEO = 5;
    private final static int ID_NEW_READY_VIDEO = 6;
    private final static int ID_NEW_YOUTUBE_LINK = 7;
    private final static int ID_NEW_WEB_LINK = 8;

    static void addNewNote(final FragmentActivity act)
	{
		AbsListView gridView;

		// get layout inflater
		View rootView = act.getLayoutInflater().inflate(R.layout.add_note_grid, null);

		// check camera feature
		PackageManager packageManager = act.getPackageManager();

        mAddNote = new ArrayList<>();

        // text
        mAddNote.add(new NewNote(ID_NEW_TEXT,
                     android.R.drawable.ic_menu_edit,
                     R.string.note_text));

        // audio
        mAddNote.add(new NewNote(ID_NEW_AUDIO,
                     R.drawable.ic_audio_unselected,
                     R.string.note_audio));

		// camera image
		if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            mAddNote.add(new NewNote(ID_NEW_CAMERA_IMAGE,
                                     android.R.drawable.ic_menu_camera,
                                     R.string.note_camera_image));
		}

		// ready image
        mAddNote.add(new NewNote(ID_NEW_READY_IMAGE,
                                 android.R.drawable.ic_menu_gallery,
                                 R.string.note_ready_image));

		// camera video
		if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            mAddNote.add(new NewNote(ID_NEW_CAMERA_VIDEO,
                                     android.R.drawable.presence_video_online,
                                     R.string.note_camera_video));
		}

		// ready video
        mAddNote.add(new NewNote(ID_NEW_READY_VIDEO,
                                 R.drawable.ic_ready_video,
                                 R.string.note_ready_video));

		// YouTube link
        mAddNote.add(new NewNote(ID_NEW_YOUTUBE_LINK,
                                 android.R.drawable.ic_menu_share,
                                 R.string.note_youtube_link));

		// Web link
        mAddNote.add(new NewNote(ID_NEW_WEB_LINK,
                                 android.R.drawable.ic_menu_share,
                                 R.string.note_web_link));

		gridView = (GridView) rootView.findViewById(R.id.add_note_grid_view);

		// check if directory is created AND not empty
        if( (mAddNote != null  ) && (mAddNote.size() > 0))
		{
            GridIconAdapter mGridIconAdapter = new GridIconAdapter(act);
			gridView.setAdapter(mGridIconAdapter);
		}
		else
		{
			Toast.makeText(act,R.string.gallery_toast_no_file,Toast.LENGTH_SHORT).show();
			act.finish();
		}

		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				System.out.println("MainUi / _addNewNote / _OnItemClickListener / position = " + position +" id = " + id);
                startAddNoteActivity(act,mAddNote.get(position).option_id);
			}
		});

		// set view to dialog
		AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
		builder1.setView(rootView);
		final AlertDialog dialog1 = builder1.create();
		dialog1.show();
	}

	private static void startAddNoteActivity(FragmentActivity act,int option)
	{
		System.out.println("MainUi / _startAddNoteActivity / option = " + option);
		switch (option) {
			case ID_NEW_TEXT:
			{
				Intent intent = new Intent(act, Note_addText.class);
				new Note_addNew_optional(act, intent);
			}
			break;

            case ID_NEW_AUDIO:
            {
                Intent intent = new Intent(act, Note_addAudio.class);
                new Note_addNew_optional_for_multiple(act, intent);
            }
            break;

			case ID_NEW_CAMERA_IMAGE:
			{
				Intent intent = new Intent(act, Note_addCameraImage.class);
				new Note_addNew_optional(act, intent);
			}
			break;

			case ID_NEW_READY_IMAGE:
			{
				Intent intent = new Intent(act, Note_addReadyImage.class);
				new Note_addNew_optional_for_multiple(act, intent);
			}
			break;

			case ID_NEW_CAMERA_VIDEO:
			{
				Intent intent = new Intent(act, Note_addCameraVideo.class);
				new Note_addNew_optional(act, intent);
			}
			break;

			case ID_NEW_READY_VIDEO:
			{
				Intent intent = new Intent(act, Note_addReadyVideo.class);
				new Note_addNew_optional_for_multiple(act, intent);
			}
			break;

			case ID_NEW_YOUTUBE_LINK:
			{
				Intent intent = new Intent(act, Note_addLink.class);
				intent.putExtra("LinkType",REQUEST_ADD_YOUTUBE_LINK);
				act.startActivity(intent);
			}
			break;

			case ID_NEW_WEB_LINK:
			{
				Intent intent = new Intent(act, Note_addLink.class);
				intent.putExtra("LinkType",REQUEST_ADD_WEB_LINK);
				act.startActivity(intent);
			}
			break;

			// default
			default:
				break;
		}

	}


	/**
     * Add new folder
     *
     */
    static private int mAddFolderAt;
    static private SharedPreferences mPref_add_new_folder_location;
    static void addNewFolder(final FragmentActivity act, final int newTableId)
    {
        final DB_drawer db_drawer = new DB_drawer(act);
        final DB_folder db_folder = new DB_folder(act,newTableId);

        // get folder name
        String folderName = act.getResources()
                               .getString(R.string.default_folder_name)
                               .concat(String.valueOf(newTableId));

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.add_new_folder, null);
        final EditText editFolderName = (EditText) rootView.findViewById(R.id.new_folder_name);

        // set cursor
        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editFolderName, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        // set hint
        editFolderName.setHint(folderName);
        editFolderName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ((EditText) v).setText("");
                    ((EditText) v).setSelection(0);
                }
            }
        });

        // radio buttons
        final RadioGroup mRadioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_new_folder_at);

        // get new folder location option
        mPref_add_new_folder_location = act.getSharedPreferences("add_new_folder_option", 0);
        if (mPref_add_new_folder_location.getString("KEY_ADD_NEW_FOLDER_TO", "bottom").equalsIgnoreCase("top")) {
            mRadioGroup.check(mRadioGroup.getChildAt(0).getId());
            mAddFolderAt = 0;
        } else if (mPref_add_new_folder_location.getString("KEY_ADD_NEW_FOLDER_TO", "bottom").equalsIgnoreCase("bottom")) {
            mRadioGroup.check(mRadioGroup.getChildAt(1).getId());
            mAddFolderAt = 1;
        }

        // update new folder location option
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup RG, int id) {
                mAddFolderAt = mRadioGroup.indexOfChild(mRadioGroup.findViewById(id));
                if (mAddFolderAt == 0) {
                    mPref_add_new_folder_location.edit().putString("KEY_ADD_NEW_FOLDER_TO", "top").apply();
                } else if (mAddFolderAt == 1) {
                    mPref_add_new_folder_location.edit().putString("KEY_ADD_NEW_FOLDER_TO", "bottom").apply();
                }
            }
        });

        // set view to dialog
        AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
        builder1.setView(rootView);
        final AlertDialog dialog1 = builder1.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) rootView.findViewById(R.id.new_folder_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // add button
        Button btnAdd = (Button) rootView.findViewById(R.id.new_folder_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String folderTitle;
                if (!Util.isEmptyString(editFolderName.getText().toString()))
                    folderTitle = editFolderName.getText().toString();
                else
                    folderTitle = act.getResources().getString(R.string.default_folder_name)
                                                    .concat(String.valueOf(newTableId));

                MainAct.mFolderTitles.add(folderTitle);
                // insert new drawer Id and Title
                db_drawer.insertFolder(newTableId, folderTitle );

                // insert folder table
                db_drawer.insertFolderTable(db_drawer,newTableId, false);

                // insert page table
                for(int i = 1; i<= Define.ORIGIN_PAGES_COUNT; i++)
                {
                    db_folder.insertPageTable(db_folder,newTableId, i, false);
                }

                // add new folder to the top
                if(mAddFolderAt == 0)
                {
                    int startCursor = db_drawer.getFoldersCount()-1;
                    int endCursor = 0;

                    //reorder data base storage for ADD_NEW_TO_TOP option
                    int loop = Math.abs(startCursor-endCursor);
                    for(int i=0;i< loop;i++)
                    {
                        swapFolderRows(startCursor,endCursor);
                        if((startCursor-endCursor) >0)
                            endCursor++;
                        else
                            endCursor--;
                    }

                    // focus position is 0 for Add to top
                    MainAct.mFocus_folderPos = 0;
                    Util.setPref_lastTimeView_folder_tableId(act,db_drawer.getFolderTableId(MainAct.mFocus_folderPos) );

                    // update playing highlight if needed
                    if(AudioPlayer.mMediaPlayer != null)
                        MainAct.mPlaying_folderPos++;
                }

                MainAct.mFolder.adapter.notifyDataSetChanged();

                //end
                dialog1.dismiss();

                updateFocus_folderPosition();
            }
        });
    }


    /*
	 * Change Page Color
	 * 
	 */
	static void changePageColor(final Activity act)
	{
		// set color
		final AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setTitle(R.string.edit_page_color_title)
	    	   .setPositiveButton(R.string.edit_page_button_ignore, new OnClickListener(){   
	            	@Override
	                public void onClick(DialogInterface dialog, int which)
	                {/*cancel*/}
	            	});
		// inflate select style layout
		LayoutInflater mInflater= (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = mInflater.inflate(R.layout.select_style, null);//??? how to set group view?
		RadioGroup RG_view = (RadioGroup)view.findViewById(R.id.radioGroup1);
		
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio0),0);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio1),1);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio2),2);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio3),3);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio4),4);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio5),5);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio6),6);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio7),7);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio8),8);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio9),9);
		
		// set current selection
		for(int i=0;i< Util.getStyleCount();i++)
		{
			if(Util.getCurrentPageStyle(act) == i)
			{
				RadioButton buttton = (RadioButton) RG_view.getChildAt(i);
		    	if(i%2 == 0)
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_dark);
		    	else
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_light);		    		
			}
		}
		
		builder.setView(view);
		
		RadioGroup radioGroup = (RadioGroup) RG_view.findViewById(R.id.radioGroup1);
		    
		final AlertDialog dlg = builder.create();
	    dlg.show();
	    
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) {
				DB_folder db = TabsHost.mDbFolder;
				TabsHost.mStyle = RG.indexOfChild(RG.findViewById(id));
				db.updatePage(db.getPageId(TabsHost.mNow_pageId, true),
							  db.getPageTitle(TabsHost.mNow_pageId, true),
							  db.getPageTableId(TabsHost.mNow_pageId, true),
							  TabsHost.mStyle,
                              true);
	 			dlg.dismiss();
	 			TabsHost.updateTabChange(act);
		}});
	}
	
	/**
	 * shift page right or left
	 * 
	 */
	static void shiftPage(final Activity act)
	{
	    Builder builder = new Builder(act);
	    builder.setTitle(R.string.rearrange_page_title)
	      	   .setMessage(null)
	           .setNegativeButton(R.string.rearrange_page_left, null)
	           .setNeutralButton(R.string.edit_note_button_back, null)
	           .setPositiveButton(R.string.rearrange_page_right,null)
	           .setIcon(R.drawable.ic_dragger_horizontal);
	    final AlertDialog dlg = builder.create();
	    
	    // disable dim background 
		dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		dlg.show();
		
		
		final int dividerWidth = act.getResources().getDrawable(R.drawable.ic_tab_divider).getMinimumWidth();
		
		// Shift to left
	    dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
	    {  @Override
	       public void onClick(View v)
	       {
	    		//change to OK
	    		Button mButton=(Button)dlg.findViewById(android.R.id.button3);
		        mButton.setText(R.string.btn_Finish);
		        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);
	
		        int[] leftMargin = {0,0};
		        if(TabsHost.mNow_pageId == 0)
		        	TabsHost.mTabsHost.getTabWidget().getChildAt(0).getLocationInWindow(leftMargin);
		        else
		        	TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId -1).getLocationInWindow(leftMargin);
	
				int curTabWidth,nextTabWidth;
				curTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId).getWidth();
				if(TabsHost.mNow_pageId == 0)
					nextTabWidth = curTabWidth;
				else
					nextTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId -1).getWidth();
	
				// when leftmost tab margin over window border
	       		if(leftMargin[0] < 0) 
	       			TabsHost.mHorScrollView.scrollBy(- (nextTabWidth + dividerWidth) , 0);
				
	    		dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
	    	    if(TabsHost.mNow_pageId == 0)
	    	    {
	    	    	Toast.makeText(TabsHost.mTabsHost.getContext(), R.string.toast_leftmost ,Toast.LENGTH_SHORT).show();
	    	    	dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);//avoid long time toast
	    	    }
	    	    else
	    	    {
	    	    	Util.setPref_lastTimeView_page_tableId(act, TabsHost.mDbFolder.getPageTableId(TabsHost.mNow_pageId, true));
	    	    	swapPage(TabsHost.mNow_pageId,
	    	    			    TabsHost.mNow_pageId -1);

                    // shift left when audio playing
                    if(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos) {
                        // target is playing index
                        if (TabsHost.mNow_pageId == MainAct.mPlaying_pageId)
                            MainAct.mPlaying_pageId--;
                        // target is at right side of playing index
                        else if ((TabsHost.mNow_pageId - MainAct.mPlaying_pageId) == 1)
                            MainAct.mPlaying_pageId++;
                    }
					TabsHost.updateTabChange(act);
	    	    }
	       }
	    });
	    
	    // done
	    dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
	    {   @Override
	       public void onClick(View v)
	       {
	           dlg.dismiss();
	       }
	    });
	    
	    // Shift to right
	    dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
	    {  @Override
	       public void onClick(View v)
	       {
	    		dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
	    		
	    		// middle button text: change to OK
	    		Button mButton=(Button)dlg.findViewById(android.R.id.button3);
		        mButton.setText(R.string.btn_Finish);
		        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);
	    		
		        DB_folder db = TabsHost.mDbFolder;
	    		int count = db.getPagesCount(true);
	            
				int[] rightMargin = {0,0};
				if(TabsHost.mNow_pageId == (count-1))
					TabsHost.mTabsHost.getTabWidget().getChildAt(count-1).getLocationInWindow(rightMargin);
				else
					TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId +1).getLocationInWindow(rightMargin);
	
				int curTabWidth, nextTabWidth;
				curTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId).getWidth();
				if(TabsHost.mNow_pageId == (count-1))
					nextTabWidth = curTabWidth;
				else
					nextTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId +1).getWidth();
				
	    		// when rightmost tab margin plus its tab width over screen border 
				int screenWidth = UtilImage.getScreenWidth(act);
	    		if( screenWidth <= rightMargin[0] + nextTabWidth )
	    			TabsHost.mHorScrollView.scrollBy(nextTabWidth + dividerWidth, 0);
				
	    		dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
	    		
	   	    	if(TabsHost.mNow_pageId == (count-1))
	   	    	{
	   	    		// end of the right side
	   	    		Toast.makeText(TabsHost.mTabsHost.getContext(),R.string.toast_rightmost,Toast.LENGTH_SHORT).show();
	   	    		dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);//avoid long time toast
	   	    	}
	   	    	else
	   	    	{
	    	    	Util.setPref_lastTimeView_page_tableId(act, db.getPageTableId(TabsHost.mNow_pageId, true));
					swapPage(TabsHost.mNow_pageId, TabsHost.mNow_pageId +1);

                    // shift right when audio playing
                    if(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos) {
                        // target is playing index
                        if (TabsHost.mNow_pageId == MainAct.mPlaying_pageId)
                            MainAct.mPlaying_pageId++;
                        // target is at left side of plying index
                        else if ((MainAct.mPlaying_pageId - TabsHost.mNow_pageId) == 1)
                            MainAct.mPlaying_pageId--;
                    }
					TabsHost.updateTabChange(act);
	   	    	}
	       }
	    });
	    
	    // android.R.id.button1 for positive: next 
	    ((Button)dlg.findViewById(android.R.id.button1))
	              .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_forward, 0, 0, 0);
	    // android.R.id.button2 for negative: previous
	    ((Button)dlg.findViewById(android.R.id.button2))
	              .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
	    // android.R.id.button3 for neutral: cancel
	    ((Button)dlg.findViewById(android.R.id.button3))
	              .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	}

	/**
	 * swap page
	 * 
	 */
	static void swapPage(int start, int end)
	{
		System.out.println("MainUi / _swapPage / start = " + start + " , end = " + end);
		DB_folder db = TabsHost.mDbFolder;

        db.open();

        // start
        int startPageId = db.getPageId(start,false);
        String startPageTitle = db.getPageTitle(start,false);
        int startPageTableId = db.getPageTableId(start,false);
        int startPageStyle = db.getPageStyle(start, false);

        // end
        int endPageId = db.getPageId(end,false);
		String endPageTitle = db.getPageTitle(end,false);
		int endPageTableId = db.getPageTableId(end,false);
		int endPageStyle = db.getPageStyle(end, false);

        // swap
		db.updatePage(endPageId,
                      startPageTitle,
			          startPageTableId,
				      startPageStyle,
                      false);
		
		db.updatePage(startPageId,
					  endPageTitle,
					  endPageTableId,
					  endPageStyle,
                      false);
        db.close();
	}

	/**
	 * Add new page: 2 dialogs
	 *
	 */
	public static  void addNewPage_2dialogs(final FragmentActivity act, final int newTabId) {
		// get tab name
		String tabName = Define.getTabTitle(act,newTabId);
	    
		// check if name is duplicated
		DB_folder dbFolder = TabsHost.mDbFolder;
		dbFolder.open();
		int pagesCount = dbFolder.getPagesCount(false);
		
		for(int i=0; i<pagesCount; i++ )
		{
			String tabTitle = dbFolder.getPageTitle(i,false);
			// new name for differentiation
			if(tabName.equalsIgnoreCase(tabTitle))
			{
				tabName = tabTitle.concat("b");
			}
		}
		dbFolder.close();

        // set cursor
	    final EditText editText1 = new EditText(act.getBaseContext());
        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editText1, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        // set hint
		editText1.setHint(tabName);
		editText1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus)
				{
					((EditText) v).setText("");
					((EditText) v).setSelection(0);
				}
			}
		});

	    //update tab info
	    Builder builder = new Builder(act);
	    builder.setTitle(R.string.edit_page_tab_title)
	            .setMessage(R.string.edit_page_tab_message)
	            .setView(editText1)   
	            .setNegativeButton(R.string.edit_page_button_ignore, new OnClickListener(){   
	            	@Override
	                public void onClick(DialogInterface dialog, int which)
	                {/*nothing*/}
	            })
	            .setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	            {   @Override
	                public void onClick(DialogInterface dialog, int which)
	                {
	            		
	    	            final String[] items = new String[]{
	    	            		act.getResources().getText(R.string.add_new_page_leftmost).toString(),
	    	            		act.getResources().getText(R.string.add_new_page_rightmost).toString() };
	    	            
						AlertDialog.Builder builder = new AlertDialog.Builder(act);
						  
						builder.setTitle(R.string.add_new_page_select_position)
						.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {

							String pageName;
							if(!Util.isEmptyString(editText1.getText().toString()) )
								pageName = editText1.getText().toString();
							else
								pageName = Define.getTabTitle(act,newTabId);;

							if(which ==0)
								insertPage_leftmost(act, newTabId, pageName);
							else
								insertPage_rightmost(act, newTabId, pageName);

							//end
							dialog.dismiss();
						}})
						.setNegativeButton(R.string.btn_Cancel, null)
						.show();
	                }
	            })	 
	            .setIcon(android.R.drawable.ic_menu_edit);
	    
	        final AlertDialog d = builder.create();
        	d.show();
	        // android.R.id.button1 for negative: cancel
	        ((Button)d.findViewById(android.R.id.button1))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
	        // android.R.id.button2 for positive: save
	        ((Button)d.findViewById(android.R.id.button2))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	}

	/**
	 * Add new page: 1 dialog
	 *
	 */
    static int mAddAt;
    static SharedPreferences mPref_add_new_page_location;
	public static  void addNewPage(final FragmentActivity act, final int newTabId) {
        // get tab name
        String pageName = Define.getTabTitle(act, newTabId);

        // check if name is duplicated
        DB_folder dbFolder = TabsHost.mDbFolder;
        dbFolder.open();
        int pagesCount = dbFolder.getPagesCount(false);

        for (int i = 0; i < pagesCount; i++) {
            String tabTitle = dbFolder.getPageTitle(i, false);
            // new name for differentiation
            if (pageName.equalsIgnoreCase(tabTitle)) {
                pageName = tabTitle.concat("b");
            }
        }
        dbFolder.close();

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.add_new_page, null);
        final EditText editPageName = (EditText) rootView.findViewById(R.id.new_page_name);

        // set cursor
        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editPageName, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        // set hint
        editPageName.setHint(pageName);
        editPageName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ((EditText) v).setText("");
                    ((EditText) v).setSelection(0);
                }
            }
        });

        // radio buttons
        final RadioGroup mRadioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_new_page_at);

        // get new page location option
        mPref_add_new_page_location = act.getSharedPreferences("add_new_page_option", 0);
        if (mPref_add_new_page_location.getString("KEY_ADD_NEW_PAGE_TO", "right").equalsIgnoreCase("left")) {
            mRadioGroup.check(mRadioGroup.getChildAt(0).getId());
            mAddAt = 0;
        } else if (mPref_add_new_page_location.getString("KEY_ADD_NEW_PAGE_TO", "right").equalsIgnoreCase("right")) {
            mRadioGroup.check(mRadioGroup.getChildAt(1).getId());
            mAddAt = 1;
        }

        // update new page location option
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup RG, int id) {
                mAddAt = mRadioGroup.indexOfChild(mRadioGroup.findViewById(id));
                if (mAddAt == 0) {
                    mPref_add_new_page_location.edit().putString("KEY_ADD_NEW_PAGE_TO", "left").apply();
                } else if (mAddAt == 1) {
                    mPref_add_new_page_location.edit().putString("KEY_ADD_NEW_PAGE_TO", "right").apply();
                }
            }
        });

        // set view to dialog
        AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
        builder1.setView(rootView);
        final AlertDialog dialog1 = builder1.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) rootView.findViewById(R.id.new_page_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // add button
        Button btnAdd = (Button) rootView.findViewById(R.id.new_page_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String pageName;
                if (!Util.isEmptyString(editPageName.getText().toString()))
                    pageName = editPageName.getText().toString();
                else
                    pageName = Define.getTabTitle(act, newTabId);


                if (mAddAt == 0)
                    insertPage_leftmost(act, newTabId, pageName);
                else
                    insertPage_rightmost(act, newTabId, pageName);

                dialog1.dismiss();
            }
        });
    }

	/*
	 * Insert Page to Rightmost
	 * 
	 */
	static void insertPage_rightmost(final FragmentActivity act, int newTblId, String tabName)
	{
		DB_folder dbFolder = TabsHost.mDbFolder;
	    // insert tab name
		int style = Util.getNewPageStyle(act);
		dbFolder.insertPage(DB_folder.getFocusFolder_tableName(),tabName,newTblId,style );
		
		// insert table for new tab
		dbFolder.insertPageTable(dbFolder,DB_folder.getFocusFolder_tableId(),newTblId, false);
		TabsHost.mPagesCount++;
		
		// commit: final page viewed
		Util.setPref_lastTimeView_page_tableId(act, newTblId);
		
	    // set scroll X
		final int scrollX = (TabsHost.mPagesCount) * 60 * 5; //over the last scroll X
		
		TabsHost.updateTabChange(act);
		
		TabsHost.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHost.mHorScrollView.scrollTo(scrollX, 0);
	        	Util.setPref_lastTimeView_scrollX_byFolderTableId(act, scrollX );
	        } 
	    });
	}

	/* 
	 * Insert Page to Leftmost
	 * 
	 */
	static void insertPage_leftmost(final FragmentActivity act, int newTabId, String tabName)//??? why exception
	{
		DB_folder dbFolder = TabsHost.mDbFolder;
		
		
	    // insert tab name
		int style = Util.getNewPageStyle(act);
		dbFolder.insertPage(DB_folder.getFocusFolder_tableName(),tabName, newTabId, style );
		
		// insert table for new tab
		dbFolder.insertPageTable(dbFolder,DB_folder.getFocusFolder_tableId(),newTabId, false);
		TabsHost.mPagesCount++;
		
		//change to leftmost tab Id
		int tabTotalCount = dbFolder.getPagesCount(true);
		for(int i=0;i <(tabTotalCount-1);i++)
		{
			int tabIndex = tabTotalCount -1 -i ;
			swapPage(tabIndex,tabIndex-1);
			updateFinalPageViewed(act);
		}
		
	    // set scroll X
		final int scrollX = 0; // leftmost
		
		// commit: scroll X
		TabsHost.updateTabChange(act);
		
		TabsHost.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHost.mHorScrollView.scrollTo(scrollX, 0);
	        	Util.setPref_lastTimeView_scrollX_byFolderTableId(act, scrollX );
	        } 
	    });
		
		// update highlight tab
		if(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos)
			MainAct.mPlaying_pageId++;
	}
	
	
	/*
	 * Update Final page which was viewed last time
	 * 
	 */
	protected static void updateFinalPageViewed(FragmentActivity act)
	{
	    // get final viewed table Id
	    int tableId = Util.getPref_lastTimeView_page_tableId(act);
		DB_page.setFocusPage_tableId(tableId);
	
		DB_folder dbFolder = TabsHost.mDbFolder;
		dbFolder.open();

		// get final view tab index of last time
		for(int i = 0; i<dbFolder.getPagesCount(false); i++)
		{
			if(Integer.valueOf(tableId) == dbFolder.getPageTableId(i, false))
				TabsHost.mFinalPageViewed_pageId = i;	// starts from 0
			
	    	if(	dbFolder.getPageId(i, false)== TabsHost.mFirstExist_PageId)
	    		Util.setPref_lastTimeView_page_tableId(act, dbFolder.getPageTableId(i, false) );
		}
		dbFolder.close();
	}

	/**
	 * delete selected drawer
	 * 
	 */
	private static int mFirstExist_folderId = 0;
	static int mLastExist_folderTableId;
	private static void deleteFolder(int position, final Activity act)
	{

		System.out.println("MainUi / _deleteFolder");
		// Before delete: renew first FolderId and last FolderId
		renewFirstAndLast_folderId();
		
		// keep one folder at least
        DB_drawer db_drawer = new DB_drawer(act);
		int foldersCount = db_drawer.getFoldersCount();
		if(foldersCount == 1)
		{
			 // show toast for only one folder
             Toast.makeText(act, R.string.toast_keep_one_drawer , Toast.LENGTH_SHORT).show();
             return;
		}

		// get drawer folder table Id
		int folderTableId = db_drawer.getFolderTableId(position);

		// get drawer Id
		int drawerId =  (int) db_drawer.getFolderId(position);

		// 1) delete related page table
        DB_folder dbFolder = new DB_folder(TabsHost.mAct, folderTableId);
		for(int i = 0; i< dbFolder.getPagesCount(true); i++)
		{
			int pageTableId = dbFolder.getPageTableId(i, true);
			dbFolder.dropPageTable(folderTableId, pageTableId);
		}

		// 2) delete folder table
		db_drawer.dropFolderTable(folderTableId);
		
		// 3) delete folder row in drawer table
        db_drawer.deleteFolderId(drawerId);
		
		renewFirstAndLast_folderId();

		// After Delete
        // - update mFocus_folderPos
        // - select first existing drawer item 
		foldersCount = db_drawer.getFoldersCount();

		// get new focus position
		// if focus item is deleted, set focus to new first existing drawer
        if(MainAct.mFocus_folderPos == position)
        {
	        for(int item = 0; item < foldersCount; item++)
	        {
	        	if(	db_drawer.getFolderId(item)== mFirstExist_folderId)
	        		MainAct.mFocus_folderPos = item;
	        }
        }
        else if(position < MainAct.mFocus_folderPos)
        	MainAct.mFocus_folderPos--;

        // set new focus position
        MainAct.mFolder.listView.setItemChecked(MainAct.mFocus_folderPos, true);
        
        // update folder table Id of last time view
        Util.setPref_lastTimeView_folder_tableId(act,
        										 db_drawer.getFolderTableId(MainAct.mFocus_folderPos) );
        
        // update audio playing highlight if needed
        if(AudioPlayer.mMediaPlayer != null)
        {
           if( MainAct.mPlaying_folderPos > position)
        	   MainAct.mPlaying_folderPos--;
           else if(MainAct.mPlaying_folderPos == position)
           {
			   // stop audio since the folder is deleted
        	   UtilAudio.stopAudioPlayer();
			   // update
        	   selectFolder(MainAct.mFocus_folderPos); // select folder to clear old playing view
			   MainAct.setFolderTitle(MainAct.mFolderTitle);
           }
        }
		
        // refresh drawer list view
		MainAct.mFolder.adapter.notifyDataSetChanged();
        
        // clear folder
        if(TabsHost.mTabsHost != null)
        	TabsHost.mTabsHost.clearAllTabs();
        TabsHost.mTabsHost = null;

        // remove last time view Key
        Util.removePref_lastTimeView_key(act,folderTableId);
	}


	// Renew first and last folder Id
	private static Cursor mFolderCursor;
	static void renewFirstAndLast_folderId()
	{
        Activity act = MainAct.mAct;
		DB_drawer db_drawer = new DB_drawer(act);
		int i = 0;
		int foldersCount = db_drawer.getFoldersCount();
		mLastExist_folderTableId = 0;
		while(i < foldersCount)
    	{
			boolean isFirst;
			db_drawer.open();
			mFolderCursor = DB_drawer.mCursor_folder;
			mFolderCursor.moveToPosition(i);
			isFirst = mFolderCursor.isFirst();
			db_drawer.close();

			if(isFirst)
				mFirstExist_folderId = (int) db_drawer.getFolderId(i) ;
			
			if(db_drawer.getFolderTableId(i) >= mLastExist_folderTableId)
				mLastExist_folderTableId = db_drawer.getFolderTableId(i);
			
			i++;
    	} 
	}

    private static SharedPreferences mPref_delete_warn;
	static void editFolder(final int position)
	{
        final Activity act = MainAct.mAct;
		DB_drawer db = new DB_drawer(act);

		// insert when table is empty, activated only for the first time 
		final String folderTitle = db.getFolderTitle(position);
	
		final EditText editText = new EditText(act);
	    editText.setText(folderTitle);
	    editText.setSelection(folderTitle.length()); // set edit text start position
	    //update tab info
	    Builder builder = new Builder(act);
	    builder.setTitle(R.string.edit_folder_title)
	    	.setMessage(R.string.edit_folder_message)
	    	.setView(editText)   
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
	                   mPref_delete_warn.getString("KEY_DELETE_FOLDER_WARN","yes").equalsIgnoreCase("yes"))
	            	{
	        			Util util = new Util(act);
	    				util.vibrate();
	    				
	            		Builder builder1 = new Builder(act); 
	            		builder1.setTitle(R.string.confirm_dialog_title)
	                    .setMessage(R.string.confirm_dialog_message_drawer)
	                    .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener(){
	                    	@Override
	                        public void onClick(DialogInterface dialog1, int which1){
	                    		/*nothing to do*/}})
	                    .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener(){
	                    	@Override
	                        public void onClick(DialogInterface dialog1, int which1){
	                    		deleteFolder(position, act);
	                    	}})
	                    .show();
	            	} //warning:end
	            	else
	            	{
	            		deleteFolder(position, act);
	            	}
	            	
	            }
	        })		    	
	    	.setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	    	{   @Override
	    		public void onClick(DialogInterface dialog, int which)
	    		{
	    			DB_drawer db_drawer = new DB_drawer(act);
	    			// save
	    			int drawerId =  (int) db_drawer.getFolderId(position);
	    			int drawerTabInfoTableId =  db_drawer.getFolderTableId(position);
					db_drawer.updateFolder(drawerId,
									drawerTabInfoTableId,
									editText.getText().toString());
					// update
					MainAct.mFolder.adapter.notifyDataSetChanged();
	                MainAct.setFolderTitle(editText.getText().toString());
	            }
	        })	
	        .setIcon(android.R.drawable.ic_menu_edit);
	        
	    AlertDialog d1 = builder.create();
	    d1.show();
	    // android.R.id.button1 for positive: save
	    ((Button)d1.findViewById(android.R.id.button1))
	    .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
	    
	    // android.R.id.button2 for negative: cancel 
	    ((Button)d1.findViewById(android.R.id.button2))
	    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	    
	    // android.R.id.button3 for neutral: delete
	    ((Button)d1.findViewById(android.R.id.button3))
	    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
	}

	// swap rows
	private static Long mFolderId1 = (long) 1;
	private static Long mFolderId2 = (long) 1;
	private static int mFolderTableId1;
	private static int mFolderTableId2;
	private static String mFolderTitle1;
	private static String mFolderTitle2;
	static void swapFolderRows(int startPosition, int endPosition)
	{
        Activity act = MainAct.mAct;
		DB_drawer db_drawer = new DB_drawer(act);
	
		mFolderId1 = db_drawer.getFolderId(startPosition);
		mFolderTableId1 = db_drawer.getFolderTableId(startPosition);
		mFolderTitle1 = db_drawer.getFolderTitle(startPosition);
	
		mFolderId2 = db_drawer.getFolderId(endPosition);
		mFolderTableId2 = db_drawer.getFolderTableId(endPosition);
		mFolderTitle2 = db_drawer.getFolderTitle(endPosition);
	
	    db_drawer.updateFolder(mFolderId1,
				mFolderTableId2,
				mFolderTitle2);
		
		db_drawer.updateFolder(mFolderId2,
				mFolderTableId1,
				mFolderTitle1);
	}

    // Update focus position
    static void updateFocus_folderPosition()
    {
    	Activity act = MainAct.mAct;
        DB_drawer db_drawer = new DB_drawer(act);

		//update focus position
		int iLastView_folderTableId = Util.getPref_lastTimeView_folder_tableId(act);
		int count = db_drawer.getFoldersCount();
    	for(int i=0;i<count;i++)
    	{
        	if(	db_drawer.getFolderTableId(i)== iLastView_folderTableId)
        	{
        		MainAct.mFocus_folderPos =  i;
				MainAct.mFolder.listView.setItemChecked(MainAct.mFocus_folderPos, true);
        	}
    	}
    	
    }	
    
    // select folder
    public static void selectFolder(final int position)
    {
    	System.out.println("MainUi / _selectFolder / position = " + position);
    	MainAct.mFolderTitle = MainAct.mDb_drawer.getFolderTitle(position);

		// update selected item and title, then close the drawer
		MainAct.mFolder.listView.setItemChecked(position, true);

        // will call Drawer / _onDrawerClosed
		MainAct.mDrawer.drawerLayout.closeDrawer(MainAct.mFolder.listView);
        
		if(Define.HAS_PREFERENCE)
		{
			// Create default tables
			if( (position < Define.ORIGIN_FOLDERS_COUNT) &&
				!Util.getPref_has_default_import(MainAct.mAct,position) )
			{
				String fileName = "default"+ (position+1) + ".xml";
				Activity act = MainAct.mAct;

				// set focus folder table Id
				int folderTableId = Util.getPref_lastTimeView_folder_tableId(act);
				System.out.println("MainUi / _selectFolder / folderTableId = " + folderTableId);
				DB_folder.setFocusFolder_tableId(folderTableId);

				// set tab Id
				TabsHost.setLastExist_tabId(0);

				// check DB: before importing
//				DB_drawer.listFolders();

				// import default tables
				Import_selectedFileAct.createDefaultTables(act,fileName);

				// check DB: after importing
//				DB_drawer.listFolders();

				Util.setPref_has_default_import(act,true,position);

				// add default image
				String imageFileName = "local"+ (position+1) + ".jpg";
				Util.createAssetsFile(act,imageFileName);

				// add default video
				String videoFileName = "local"+ (position+1) + ".mp4";
				Util.createAssetsFile(act,videoFileName);

				// add default audio
				String audioFileName = "local"+ (position+1) + ".mp3";
				Util.createAssetsFile(act,audioFileName);
			}
		}

        // use Runnable to make sure only one folder background is seen
        mHandler = new Handler();
       	mHandler.post(mTabsHostRun);
    }
    
    static Handler mHandler;
    // runnable to launch folder host
    static Runnable mTabsHostRun =  new Runnable() 
    {
        @Override
        public void run() 
        {
        	System.out.println("MainUi / mTabsHostRun");
            Fragment fragment = new TabsHost();
        	FragmentTransaction fragmentTransaction = MainAct.fragmentManager.beginTransaction();
        	fragmentTransaction.replace(R.id.content_frame, fragment).commit();
        	MainAct.fragmentManager.executePendingTransactions();
        } 
    };    
    
    public static boolean isSamePageTable()
    {
	    return ( (MainAct.mPlaying_pageTableId == TabsHost.mNow_pageTableId) &&
			     (MainAct.mPlaying_pageId == TabsHost.mNow_pageId) &&
	     	     (MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos)        );
    }
    
}


class MenuId {
    static final int ADD_NEW_NOTE = R.id.ADD_NEW_NOTE;

    static final int OPEN_PLAY_SUBMENU = R.id.PLAY;
    static final int PLAY_OR_STOP_AUDIO = R.id.PLAY_OR_STOP_MUSIC;
    static final int SLIDE_SHOW = R.id.SLIDE_SHOW;
    static final int GALLERY = R.id.GALLERY;

    static final int ADD_NEW_PAGE = R.id.ADD_NEW_PAGE;
    static final int CHANGE_PAGE_COLOR = R.id.CHANGE_PAGE_COLOR;
    static final int SHIFT_PAGE = R.id.SHIFT_PAGE;
    static final int SHOW_BODY = R.id.SHOW_BODY;
    static final int ENABLE_NOTE_DRAG_AND_DROP = R.id.ENABLE_NOTE_DRAG_AND_DROP;
    static final int SEND_PAGES = R.id.SEND_PAGES;
    static final int EXPORT_TO_SD_CARD = R.id.EXPORT_TO_SD_CARD;
    static final int IMPORT_FROM_SD_CARD = R.id.IMPORT_FROM_SD_CARD;
    static final int CONFIG_PREFERENCE = R.id.CONFIG_PREF;

    static final int ADD_NEW_FOLDER = R.id.ADD_NEW_FOLDER;
    static final int ENABLE_FOLDER_DRAG_AND_DROP = R.id.ENABLE_FOLDER_DRAG_AND_DROP;
}


class GridIconAdapter extends BaseAdapter {
	private FragmentActivity act;
	GridIconAdapter(FragmentActivity fragAct){act = fragAct;}

	@Override
	public int getCount() {
        return MainUi.mAddNote.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		View view = convertView;
		if (view == null) {
			view = act.getLayoutInflater().inflate(R.layout.add_note_grid_item, parent, false);
			holder = new ViewHolder();
			assert view != null;
			holder.imageView = (ImageView) view.findViewById(R.id.grid_item_image);
			holder.text = (TextView) view.findViewById(R.id.grid_item_text);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

        Drawable drawable = act.getResources().getDrawable(MainUi.mAddNote.get(position).option_drawable_id);
		holder.imageView.setImageDrawable(drawable);
        holder.text.setText(MainUi.mAddNote.get(position).option_string_id);
		return view;
	}

	private class ViewHolder {
		ImageView imageView;
		TextView text;
	}
}

class NewNote {
    int option_id;
    int option_drawable_id;
    int option_string_id;

    NewNote(int id, int draw_id, int string_id)
    {
        this.option_id = id;
        this.option_drawable_id = draw_id;
        this.option_string_id = string_id;
    }
}