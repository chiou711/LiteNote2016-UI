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

public class Note_addNew_optional 
{
	RadioGroup mRadioGroup;
    AlertDialog mDialog = null;
	SharedPreferences mPref_add_new_note_location;
	int mAddAt;

	public Note_addNew_optional(final Activity activity, final Intent intent)
	{
		mPref_add_new_note_location = activity.getSharedPreferences("add_new_note_option", 0);
  		// inflate select style layout
  		LayoutInflater inflater;
  		inflater= (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  		View view = inflater.inflate(R.layout.note_add_new_optional, null);

  		mRadioGroup = (RadioGroup)view.findViewById(R.id.radioGroup_new_at);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(R.string.add_new_note_option_title)
			.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener() {
                @Override
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

		if(mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top"))
		{
			mRadioGroup.check(mRadioGroup.getChildAt(0).getId());
			mAddAt = 0;
		}
		else if (mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("bottom"))
		{
			mRadioGroup.check(mRadioGroup.getChildAt(1).getId());
			mAddAt = 1;
		}

  		builder.setView(view);
  		mDialog = builder.create();
  		mDialog.show();

		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id)
			{
				mAddAt =  mRadioGroup.indexOfChild(mRadioGroup.findViewById(id));
				respondToSelection(activity, intent, mAddAt);
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
            mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_NOTE_TO", "top").apply();
			intent.putExtra("extra_ADD_NEW_TO_TOP", "true");
			activity.startActivity(intent);
		}
		else if(mAddAt ==1)
		{
            mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_NOTE_TO", "bottom").apply();
			intent.putExtra("extra_ADD_NEW_TO_TOP", "false");
			activity.startActivity(intent);
		}
	}
}