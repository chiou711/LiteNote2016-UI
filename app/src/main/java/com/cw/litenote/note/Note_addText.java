package com.cw.litenote.note;

import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.ColorSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Note_addText extends Activity {

    static Long mRowId;
    Note_common note_common;
    static boolean mEnSaveDb = true;
    Button addButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_add_new_text);
        setTitle(R.string.add_new_note_title);// set title
        
        System.out.println("Note_addNew / onCreate");

        getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(this)));

        note_common = new Note_common(this);
        note_common.UI_init_text();
			
        // get row Id from saved instance
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB_page.KEY_NOTE_ID);
        
        Note_common.populateFields_text(mRowId);
        
    	// button: add
        addButton = (Button) findViewById(R.id.note_add_new_add);
        addButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_input_add, 0, 0, 0);

		// set add note button visibility
        if(!note_common.isTextAdded())
        	addButton.setVisibility(View.GONE);
        
        Note_common.mTitleEditText.addTextChangedListener(setTextWatcher());
        Note_common.mBodyEditText.addTextChangedListener(setTextWatcher());

        // listener: add new note
        addButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view) 
            {
    			//add new note again
       			if(note_common.isTextAdded())
    			{
       				mEnSaveDb = true;
       				mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,"", "", "");
       				
       				if( getIntent().getExtras().getString("extra_ADD_NEW_TO_TOP", "false").equalsIgnoreCase("true") &&
       				    (Note_common.getCount() > 0) )
       					Page.swap();

       				Toast.makeText(Note_addText.this, R.string.toast_saved , Toast.LENGTH_SHORT).show();
       				
       		        note_common = new Note_common(Note_addText.this);
       		        note_common.UI_init_text();
       				mRowId = null;
       		        Note_common.populateFields_text(mRowId);
    			}
            }
        });
        
        // button: cancel new note
        Button cancelButton = (Button) findViewById(R.id.note_add_new_cancel);
        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        // listener: cancel
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	
            	if(note_common.isTextAdded())
            	{
            		confirmUpdateChangeDlg();
            	}
            	else
            	{
        			Toast.makeText(Note_addText.this, R.string.btn_Cancel, Toast.LENGTH_SHORT).show();
        			System.out.println("NoteFragment / Activity.RESULT_CANCELED");
                    Note_common.deleteNote(mRowId);
                    mEnSaveDb = false;
                    setResult(RESULT_CANCELED, getIntent());
                    finish();
            	}
            }
        });

        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    TextWatcher setTextWatcher()
    {
    	return new TextWatcher(){
	        public void afterTextChanged(Editable s) 
	        {
			    if(!note_common.isTextAdded())
		        	addButton.setVisibility(View.GONE);
		        else
		        	addButton.setVisibility(View.VISIBLE);
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
    	};
    }
    
    // confirmation to update change or not
    void confirmUpdateChangeDlg()
    {
        getIntent().putExtra("NOTE_ADDED","edited");

        AlertDialog.Builder builder = new AlertDialog.Builder(Note_addText.this);
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.add_new_note_confirm_save)
			   .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
					    mEnSaveDb = true;
		            	setResult(RESULT_OK, getIntent());
					    finish();
					}})
			   .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{   // do nothing
					}})					
			   .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Note_common.deleteNote(mRowId);
	                    mEnSaveDb = false;
	                    setResult(RESULT_CANCELED, getIntent());
	                    finish();
					}})
			   .show();
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

    // for Add new note
    // for Rotate screen
    @Override
    protected void onPause() {
    	System.out.println("Note_addNewText / onPause");
        super.onPause();
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,"", "", "");
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addNew / onSaveInstanceState");
        note_common.mNoteId = mRowId;
        outState.putSerializable(DB_page.KEY_NOTE_ID, mRowId);
    }

    @Override
    public void onBackPressed()
    {
    	if(note_common.isTextAdded())
    		confirmUpdateChangeDlg();
    	else
    	{
            Note_common.deleteNote(mRowId);
            mEnSaveDb = false;
            NavUtils.navigateUpFromSameTask(this);
    	}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                if(note_common.isTextAdded())
                    confirmUpdateChangeDlg();
                else
                {
                    Note_common.deleteNote(mRowId);
                    mEnSaveDb = false;
                    NavUtils.navigateUpFromSameTask(this);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
