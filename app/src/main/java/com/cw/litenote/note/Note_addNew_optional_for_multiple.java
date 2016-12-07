package com.cw.litenote.note;

import com.cw.litenote.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class Note_addNew_optional_for_multiple 
{
	RadioGroup mRadioGroup1,mRadioGroup2;
    AlertDialog mDialog = null;
	SharedPreferences mPref_add_new_note_location;
	int mAddAt;

	public Note_addNew_optional_for_multiple(final Activity activity, final Intent intent)
	{
		mPref_add_new_note_location = activity.getSharedPreferences("add_new_note_option", 0);
  		// inflate select style layout
  		LayoutInflater inflater;
  		inflater= (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  		View view = inflater.inflate(R.layout.note_add_new_optional_for_multiple, null);
  		
  		mRadioGroup1 = (RadioGroup)view.findViewById(R.id.radioGroup1_new_at);
  		mRadioGroup2 = (RadioGroup)view.findViewById(R.id.radioGroup2_new_at);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	
		builder.setTitle(R.string.dialog_add_new_option_title)
			.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
	        {	@Override
	    		public void onClick(DialogInterface dialog, int which) {
	    			//cancel
	    		}
	        });
		
		builder.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener()
        {	@Override
    		public void onClick(DialogInterface dialog, int which)
        	{
        		respondToSelection(activity, intent, mAddAt);
			}
        });			
		
		if(mPref_add_new_note_location.getString("KEY_ADD_NEW_AUDIO","single_to_bottom").equalsIgnoreCase("single_to_top"))
		{
			mRadioGroup1.check(mRadioGroup1.getChildAt(0).getId());
			mAddAt = 0;
		}
		else if (mPref_add_new_note_location.getString("KEY_ADD_NEW_AUDIO","single_to_bottom").equalsIgnoreCase("single_to_bottom"))
		{
			mRadioGroup1.check(mRadioGroup1.getChildAt(1).getId());
			mAddAt = 1;
		}
		else if (mPref_add_new_note_location.getString("KEY_ADD_NEW_AUDIO","single_to_bottom").equalsIgnoreCase("directory_to_top"))
		{
			mRadioGroup2.check(mRadioGroup2.getChildAt(0).getId());
			mAddAt = 2;
		}		
		else if (mPref_add_new_note_location.getString("KEY_ADD_NEW_AUDIO","single_to_bottom").equalsIgnoreCase("directory_to_bottom"))
		{
			mRadioGroup2.check(mRadioGroup2.getChildAt(1).getId());
			mAddAt = 3;
		}  		
  		builder.setView(view);
  		mDialog = builder.create();
  		mDialog.show();
	
		mRadioGroup1.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) 
			{
				mAddAt =  mRadioGroup1.indexOfChild(mRadioGroup1.findViewById(id));
				respondToSelection(activity, intent, mAddAt);
				if(mDialog.isShowing())
					mDialog.dismiss();
			}
		});
		
		mRadioGroup2.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) 
			{
				mAddAt =  mRadioGroup2.indexOfChild(mRadioGroup2.findViewById(id));
				respondToSelection(activity, intent, mAddAt+2);
				if(mDialog.isShowing())
					mDialog.dismiss();
			}
		});		
	}
	
	// respond to selection
	void respondToSelection(Activity activity, Intent intent, int mAddAt)
	{
		if(mAddAt ==0)
		{
			mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_AUDIO", "single_to_top").apply();
			intent.putExtra("EXTRA_ADD_EXIST", "single_to_top");
			activity.startActivity(intent);
		}
		else if(mAddAt ==1)
		{
			mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_AUDIO", "single_to_bottom").apply();
			intent.putExtra("EXTRA_ADD_EXIST", "single_to_bottom");
			activity.startActivity(intent);
		}
		else if(mAddAt ==2)
		{
			mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_AUDIO", "directory_to_top").apply();
			intent.putExtra("EXTRA_ADD_EXIST", "directory_to_top");
			activity.startActivity(intent);
		}		
		else if(mAddAt ==3)
		{
			mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_AUDIO", "directory_to_bottom").apply();
			intent.putExtra("EXTRA_ADD_EXIST", "directory_to_bottom");
			activity.startActivity(intent);
		}			
	}
}