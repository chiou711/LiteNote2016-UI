package com.cw.litenote.note;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.RadioGroup;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.main.Page;
import com.cw.litenote.R;

public class Note_addLink extends Activity {
    RadioGroup mRadioGroup;
    AlertDialog mDialog = null;
    SharedPreferences mPref_add_new_note_location;
    int mAddAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPref_add_new_note_location = getSharedPreferences("add_new_note_option", 0);
        // inflate select style layout
        LayoutInflater inflater;
        inflater= (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.note_add_new_link_optional, null);

        mRadioGroup = (RadioGroup)view.findViewById(R.id.radioGroup_new_at);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.add_new_note_option_title)
                .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //cancel
                        if (mDialog.isShowing())
                            mDialog.dismiss();
                    }
                });

        builder.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener()
        {	@Override
             public void onClick(DialogInterface dialog, int which)
            {
                respondToSelection(mAddAt);
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

        // checked Text View: enable link title Save
        CheckedTextView enLinkSaveCheck = (CheckedTextView) view.findViewById(R.id.enable_link_title_save);
        if(Page.mPref_show_note_attribute
                   .getString("KEY_ENABLE_LINK_TITLE_SAVE", "yes")
                   .equalsIgnoreCase("yes") )
            enLinkSaveCheck.setChecked(true);
        else
            enLinkSaveCheck.setChecked(false);

        enLinkSaveCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean currentCheck = ((CheckedTextView) view).isChecked();
                ((CheckedTextView) view).setChecked(!currentCheck);

                if (((CheckedTextView) view).isChecked())
                    Page.mPref_show_note_attribute
                            .edit().putString("KEY_ENABLE_LINK_TITLE_SAVE", "yes").apply();
                else
                    Page.mPref_show_note_attribute
                            .edit().putString("KEY_ENABLE_LINK_TITLE_SAVE", "no").apply();
            }
        });

        // show dialog
        builder.setView(view);
        mDialog = builder.create();
        mDialog.show();

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup RG, int id) {
                mAddAt = mRadioGroup.indexOfChild(mRadioGroup.findViewById(id));
                respondToSelection(mAddAt);
            }
        });

        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
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

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mDialog.isShowing())
            mDialog.dismiss();
    }

    // respond to selection
    void respondToSelection(int mAddAt)
    {
        if(mAddAt ==0)
            mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_NOTE_TO", "top").apply();
        else if(mAddAt ==1)
            mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_NOTE_TO", "bottom").apply();

        Intent intent = null;
        int type = getIntent().getExtras().getInt("LinkType");
        if(type == MainAct.REQUEST_ADD_YOUTUBE_LINK)
            intent = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.youtube.com"));
        else if(type == MainAct.REQUEST_ADD_WEB_LINK)
            intent = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.google.com"));

        startActivity(intent);

        if (mDialog.isShowing())
            mDialog.dismiss();
    }
}