package com.cw.litenote.note;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

/*
 * Note: 
 * Note_common: used to do DB operation
 */
public class Note_addReadyImage extends FragmentActivity {

    static Long mRowId;
    Note_common note_common;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("Note_addReadyPicture / onCreate");
		
        note_common = new Note_common(this);
	
        // get row Id from saved instance
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB_page.KEY_NOTE_ID);
        
        // at the first beginning
        if(savedInstanceState == null)
        	addPicture();
        
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
    	System.out.println("Note_addReadyPicture / onPause");
        super.onPause();
    }

    // for Add Ok picture (stage 2)
    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addReadyPicture / onSaveInstanceState");
        outState.putSerializable(DB_page.KEY_NOTE_ID, mRowId);
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
    }
    
    void addPicture()
    {
	    startActivityForResult(Util.chooseMediaIntentByType(Note_addReadyImage.this, "image/*"),
	    					   Util.CHOOSER_SET_PICTURE);
    }

    public static Uri writeToTempImageAndGetPathUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		System.out.println("Note_addReadyPicture / onActivityResult");
		if (resultCode == Activity.RESULT_OK)
		{

			// for ready picture
			if(requestCode == Util.CHOOSER_SET_PICTURE)
            {
				Uri selectedUri = imageReturnedIntent.getData();
				String authority = selectedUri.getAuthority();
				// SAF support, take persistent Uri permission
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				{
			    	// Check for the freshest data.
                    // for Google drive
			    	if(authority.equalsIgnoreCase("com.google.android.apps.docs.storage") )//??? add condition?
			    	{
                        int takeFlags = imageReturnedIntent.getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

						// add for solving inspection error
						takeFlags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;

						getContentResolver().takePersistableUriPermission(selectedUri, takeFlags);
			    	}
                    // for Google Photos
                    else if(authority.equalsIgnoreCase("com.google.android.apps.photos.contentprovider") )
                    {
                        InputStream is = null;
                        try {
                            is = getContentResolver().openInputStream(selectedUri);
                            Bitmap bmp = BitmapFactory.decodeStream(is);
                            selectedUri = Uri.parse(writeToTempImageAndGetPathUri(this, bmp).toString());
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }finally {
                            try {
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
				}

				String scheme = selectedUri.getScheme();
				// check option of Add multiple
				String option = getIntent().getExtras().getString("EXTRA_ADD_EXIST", "single_to_bottom");

				// add single file
				if(option.equalsIgnoreCase("single_to_top") ||
           		   option.equalsIgnoreCase("single_to_bottom")	)
				{

					System.out.println("Note_addReadyImage / onActivityResult / selectedUri = " + selectedUri);
					String realPath = Util.getLocalRealPathByUri(this, selectedUri);
					System.out.println("Note_addReadyImage / onActivityResult / realPath = " + realPath);

					String uriStr;

					if(realPath != null)
						uriStr = "file://".concat(realPath); // local
					else
						uriStr = selectedUri.toString(); // remote

					System.out.println("Note_addReadyImage / onActivityResult / uriStr = " + uriStr);
		  		    mRowId = null; // set null for Insert
		        	mRowId = Note_common.savePictureStateInDB(mRowId,true,uriStr, "", "", "");

		        	if( (Note_common.getCount() > 0) &&
		        		option.equalsIgnoreCase("single_to_top"))
		        	{
		        		Page.swap();
		        	}

		        	if(!Util.isEmptyString(uriStr))
		        	{
		                String name = Util.getDisplayNameByUriString(uriStr, this);
		        		Util.showSavedFileToast(name,this);
		        	}
				}
				// add multiple files in the selected file's directory
				else if((option.equalsIgnoreCase("directory_to_top") ||
						 option.equalsIgnoreCase("directory_to_bottom")) &&
						 (scheme.equalsIgnoreCase("file") ||
						  scheme.equalsIgnoreCase("content") )              )
				{
					String realPath = Util.getLocalRealPathByUri(this, selectedUri);
					if(realPath != null)
					{
						// get file name
						File file = new File("file://".concat(realPath));
						String fileName = file.getName();

						// get directory
						String dirStr = realPath.replace(fileName, "");
						File dir = new File(dirStr);

						// get Urls array
						String[] urlsArray = Util.getUrlsByFiles(dir.listFiles(),Util.IMAGE);
						if(urlsArray == null)
						{
							Toast.makeText(this,"No file is found",Toast.LENGTH_SHORT).show();
							finish();
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
				  		    mRowId = null; // set null for Insert
				  		    if(!Util.isEmptyString(urlStr))
				  		    	mRowId = Note_common.savePictureStateInDB(mRowId,true,urlStr, "", "", "");

				        	if( (Note_common.getCount() > 0) &&
	  		        			option.equalsIgnoreCase("directory_to_top") )
				        	{
				        		Page.swap();
				        	}

				        	// avoid showing empty toast
				        	if(!Util.isEmptyString(urlStr))
				        	{
				                String name = Util.getDisplayNameByUriString(urlStr, this);
				                name = i + "/" + total + ": " + name;
				        		Util.showSavedFileToast(name,this);
				        	}
				        	i++;
						}
					}
					else
					{
						Toast.makeText(this,
								"For multiple files, please check if your selection is a local file.",
								Toast.LENGTH_LONG)
								.show();
					}
				}
				
				addPicture();
			}
		} 
		else if (resultCode == RESULT_CANCELED)
		{
	        // hide action bar
			getActionBar().hide();
			// set background to transparent
			getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
			
			Toast.makeText(Note_addReadyImage.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED, getIntent());
            finish();
            return; // must add this
		}
	}
    
}