package com.cw.litenote.util;
/**
 * This file provides simple End User License Agreement
 * It shows a simple dialog with the license text, and two buttons.
 * If user clicks on 'cancel' button, app closes and user will not be granted access to app.
 * If user clicks on 'accept' button, app access is allowed and this choice is saved in preferences
 * so next time this will not show, until next upgrade.
 */
 
import com.cw.litenote.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

public class EULA_dlg {

    private String EULA_PREFIX = "appEULA";
    private Activity mAct;
 
    public EULA_dlg(Activity context) {
        mAct = context;
    }
 
    private PackageInfo getPackageInfo() {
        PackageInfo info = null;
        try {
            info = mAct.getPackageManager().getPackageInfo(
                    mAct.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return info;
    }
 
    public void show() {
        System.out.println("EULA_dlg / _show");
        PackageInfo versionInfo = getPackageInfo();
 
        // The eulaKey changes every time you increment the version number in
        // the AndroidManifest.xml
        final String eulaKey = EULA_PREFIX + versionInfo.versionCode;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mAct);
 
        // enable default state: show license dialog always
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putBoolean(eulaKey, false);
//        editor.apply();
        
        
        boolean bAlreadyAccepted = prefs.getBoolean(eulaKey, false);
        
        if (bAlreadyAccepted == false) {
 
            // EULA title
            String title = mAct.getString(R.string.app_name) +
            			   " v" + 
            			   versionInfo.versionName;
 
            // EULA text
            String message = mAct.getString(R.string.EULA_string);
 
            // Disable orientation changes, to prevent parent activity
            // re-initialization
//            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
 
            AlertDialog.Builder builder = new AlertDialog.Builder(mAct)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.accept,
                            new Dialog.OnClickListener() {
 
                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface, int i) {
                                    // Mark this version as read.
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean(eulaKey, true);
                                    editor.apply();
 
                                    // Close dialog
                                    dialogInterface.dismiss();
 
                                    // Enable orientation changes based on
                                    // device's sensor
//                                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new Dialog.OnClickListener() {
 
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // Close the activity as they have declined
                                    // the EULA
                                    mAct.finish();
//                                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                                }
 
                            });
            builder.create().show();
        }
    }
}