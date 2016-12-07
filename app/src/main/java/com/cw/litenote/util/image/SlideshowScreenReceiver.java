package com.cw.litenote.util.image;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SlideshowScreenReceiver extends BroadcastReceiver {

    public static boolean toBeScreenOn = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            // do whatever you need to do here
        	System.out.println("SlideshowScreenReceiver / toBeScreenOn = false");
            toBeScreenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            // and do whatever you need to do here
        	System.out.println("SlideshowScreenReceiver / toBeScreenOn = true");
            toBeScreenOn = true;
        }
    }
}