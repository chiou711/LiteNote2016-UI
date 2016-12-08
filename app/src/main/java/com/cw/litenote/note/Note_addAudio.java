package com.cw.litenote.note;

import java.io.File;

import com.cw.litenote.main.MainUi;
import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class Note_addAudio extends FragmentActivity { 

    static Long mNoteId;
    static String mSelectedAudioUri;
    Note_common note_common;
    static boolean mEnSaveDb = true;
	static String mAudioUriInDB;
	private static DB_page mDb;
    boolean bUseSelectedFile;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("Note_addAudio / onCreate");
        
        note_common = new Note_common(this);
        mAudioUriInDB = "";
        mSelectedAudioUri = "";
        bUseSelectedFile = false;
			
        // get row Id from saved instance
        mNoteId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB_page.KEY_NOTE_ID);
        
        // get audio Uri in DB if instance is not null
        mDb = Page.mDb_page;
        if(savedInstanceState != null)
        {
	        System.out.println("Note_addAudio / mNoteId =  " + mNoteId);
	        if(mNoteId != null)
	        	mAudioUriInDB = mDb.getNoteAudioUri_byId(mNoteId);
        }
        
        // at the first beginning
        if(savedInstanceState == null)
        	chooseAudioMedia();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    }

    // for Rotate screen
    @Override
    protected void onPause() {
    	System.out.println("Note_addAudio / onPause");
        super.onPause();
    }

    // for Add new picture (stage 2)
    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addNew / onSaveInstanceState");
        outState.putSerializable(DB_page.KEY_NOTE_ID, mNoteId);
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        mEnSaveDb = false;
        finish();
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		System.out.println("Note_addAudio / onActivityResult");
		if (resultCode == Activity.RESULT_OK)
		{
			// for audio
			if(requestCode == Util.CHOOSER_SET_AUDIO)
			{
				Uri selectedUri = imageReturnedIntent.getData();
				System.out.println("Note_addAudio / onActivityResult / selectedUri = " + selectedUri);
				
				// SAF support, take persistent Uri permission
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				{
			    	int takeFlags = imageReturnedIntent.getFlags()
			                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
			                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

					// add for solving inspection error
					takeFlags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;

					//fix: no permission grant found for UID 10070 and Uri content://media/external/file/28
			    	String authority = selectedUri.getAuthority();
			    	if(authority.equalsIgnoreCase("com.google.android.apps.docs.storage")) //??? add condition? 	
			    	{
			    		getContentResolver().takePersistableUriPermission(selectedUri, takeFlags);
			    	}
				}
				
				String scheme = selectedUri.getScheme();
				// check option of Add new audio
				String option = getIntent().getExtras().getString("EXTRA_ADD_EXIST", "single_to_bottom");
     			
				// add single file
				if((option.equalsIgnoreCase("single_to_top") || 
           		    option.equalsIgnoreCase("single_to_bottom") ) && 
           		   (scheme.equalsIgnoreCase("file") ||
 					scheme.equalsIgnoreCase("content"))              )
				{
					String uriStr = selectedUri.toString();
					
					// check if content scheme points to local file
					if(scheme.equalsIgnoreCase("content"))
					{
						String realPath = Util.getLocalRealPathByUri(this, selectedUri);
						
						if(realPath != null)
							uriStr = "file://".concat(realPath);
					}
					
		  		    mNoteId = null; // set null for Insert
		        	mNoteId = Note_common.insertAudioToDB(uriStr);
		        	mSelectedAudioUri = uriStr;
		        	
		        	if( (Note_common.getCount() > 0) &&  
		        		option.equalsIgnoreCase("single_to_top"))
		        	{
		        		Page.swap();
		        		//update playing focus
		        		AudioPlayer.mAudioIndex++;
		        	}
		        	
		        	if(!Util.isEmptyString(uriStr))	
		        	{
		                String audioName = Util.getDisplayNameByUriString(uriStr, Note_addAudio.this);
		        		Util.showSavedFileToast(audioName,this);
		        	}
				}
				// add multiple audio files in the selected file's directory
				else if((option.equalsIgnoreCase("directory_to_top") || 
						 option.equalsIgnoreCase("directory_to_bottom")) &&
						 (scheme.equalsIgnoreCase("file") ||
						  scheme.equalsIgnoreCase("content") )              )
				{
					// get file path and add prefix (file://)
					String realPath = Util.getLocalRealPathByUri(this, selectedUri);
					
					// when scheme is content, it could be local or remote
					if(realPath != null)
					{
						// get file name
						File file = new File("file://".concat(realPath));
						String fileName = file.getName();
						
						// get directory
						String dirStr = realPath.replace(fileName, "");
						File dir = new File(dirStr);
						
						// get Urls array
						String[] urlsArray = Util.getUrlsByFiles(dir.listFiles(), Util.AUDIO);
						if(urlsArray == null)
						{
							Toast.makeText(this,"No file is found",Toast.LENGTH_SHORT).show();
							finish();
						}
						else
						{
							// show Start
							Toast.makeText(this, R.string.add_new_start, Toast.LENGTH_SHORT).show();
						}

						int i= 1;
						int total=0;
						
						for(int cnt = 0; cnt < urlsArray.length; cnt++)
						{
							if(!Util.isEmptyString(urlsArray[cnt]))
								total++;
						}
						
						// note: the order add insert items depends on file manager 
						for(String urlStr:urlsArray)
						{
							System.out.println("urlStr = " + urlStr);
				  		    mNoteId = null; // set null for Insert
				  		    if(!Util.isEmptyString(urlStr))
				  		    	mNoteId = Note_common.insertAudioToDB(urlStr);
				        	mSelectedAudioUri = urlStr;
				        	
				        	if( (Note_common.getCount() > 0) &&
	  		        			option.equalsIgnoreCase("directory_to_top") ) 
				        	{
				        		Page.swap();
				        		//update playing focus
				        		AudioPlayer.mAudioIndex++;
				        	}
				    		
				        	// avoid showing empty toast
				        	if(!Util.isEmptyString(urlStr))
				        	{
				                String audioName = Util.getDisplayNameByUriString(urlStr, Note_addAudio.this);
				                audioName = i + "/" + total + ": " + audioName;
				        		Util.showSavedFileToast(audioName,this);	
				        	}
				        	i++;
						}
						// show Stop
						Toast.makeText(this,R.string.add_new_stop,Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(this,
								"For multiple files, please check if your selection is a local file.",
								Toast.LENGTH_LONG)
								.show();					
					}
				}
				
				// do again
	        	chooseAudioMedia();	
	        	
	        	// to avoid exception due to playing tab is different with focus tab
	        	if(MainUi.isSamePageTable())
	        	{
		        	AudioPlayer.prepareAudioInfo();
		        	Page.mItemAdapter.notifyDataSetChanged();
	        	}
			}
		} 
		else if (resultCode == RESULT_CANCELED)
		{
			Toast.makeText(Note_addAudio.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED, getIntent());
            finish();
            return; // must add this
		}
	}

    void chooseAudioMedia()
    {
	    mEnSaveDb = true;
        startActivityForResult(Util.chooseMediaIntentByType(Note_addAudio.this,"audio/*"),
        					   Util.CHOOSER_SET_AUDIO);        
    }	
	

}