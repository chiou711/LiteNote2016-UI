package com.cw.litenote.util;

import android.support.v4.app.FragmentActivity;

/**
 * Created by CW on 2016/6/3.
 * ref: http://stackoverflow.com/questions/5448653/how-to-implement-onbackpressed-in-android-fragments
 */

public class BaseBackPressedListener implements OnBackPressedListener {
    final FragmentActivity activity;

    public BaseBackPressedListener(FragmentActivity activity) {
        this.activity = activity;
    }

    @Override
    public void doBack() {
        System.out.println("BaseBackPressedListener / _doBack");
//        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        activity.getSupportFragmentManager().popBackStack();
    }
}