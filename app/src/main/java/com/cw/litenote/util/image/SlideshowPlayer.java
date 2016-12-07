package com.cw.litenote.util.image;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.R;
import com.cw.litenote.util.UilCommon;
import com.cw.litenote.util.Util;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
public class SlideshowPlayer extends FragmentActivity
{
	private static final String IMAGE_INDEX = "IMAGE_INDEX";
	private static final int DURATION = 5000; // 5 seconds per slide
	private ImageView imageView; // displays the current image
   
	private SlideshowInfo slideshow; // slide show being played
	private Handler imageHandler; // used to update the slide show
	private int imageIndex; // index of the next image to display
	private BroadcastReceiver mReceiver;
   
	// initializes the SlideshowPlayer Activity
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.slideshow_player);
      
		UilCommon.init();
		Util.setFullScreen(this);
		
		// disable screen saving
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		// disable key guard
		getWindow().addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
      
		System.out.println("SlideshowPlayer / _onCreate ");
		if (savedInstanceState == null) // Activity starting
		{
			System.out.println("_onCreate / savedInstanceState == null"); 
			imageIndex = 0; // start from first image
		}
		else // Activity resuming
		{
			imageIndex = savedInstanceState.getInt(IMAGE_INDEX);     
		}       
      
		// get SlideshowInfo for slideshow to play
		slideshow = MainAct.slideshowInfo;
   	  	imageHandler = new Handler(); // create handler to control slideshow

   	  	IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
   	  	filter.addAction(Intent.ACTION_SCREEN_OFF);
   	  	mReceiver = new SlideshowScreenReceiver();
   	  	registerReceiver(mReceiver, filter);   
	}
   
	@Override
	protected void onRestart()
	{
//		System.out.println("SlideshowPlayer / onRestart ");
		super.onRestart();
	}   

   
	// called after onCreate and sometimes onStop
	@Override
	protected void onStart()
	{
//		System.out.println("SlideshowPlayer / onStart ");
		super.onStart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
//		System.out.println("SlideshowPlayer / onRestoreInstanceState ");
		super.onRestoreInstanceState(savedInstanceState);
		imageIndex = savedInstanceState.getInt(IMAGE_INDEX); 		
	}
   
	// called after onStart or onPause
	@Override
	protected void onResume()
	{
//		System.out.println("SlideshowPlayer / onResume ");
		super.onResume();
   	  	imageHandler.post(runSlideshow); // post updateSlideshow to execute
	}

	// called when the Activity is paused
	@Override
	protected void onPause()
	{
//		System.out.println("SlideshowPlayer / onPause ");
		super.onPause();
   	  	imageHandler.removeCallbacks(runSlideshow);
	}

	// save slide show state so it can be restored in onCreate
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
//		System.out.println("SlideshowPlayer / onSaveInstanceState ");
		super.onSaveInstanceState(outState);
		// save nextItemIndex and slideshowName
		imageIndex--;
		if(imageIndex<0)
			imageIndex =0;
		outState.putInt(IMAGE_INDEX, imageIndex); 
	}    
   
	// called when the Activity stops
	@Override
	protected void onStop()
	{
//		System.out.println("SlideshowPlayer / onStop ");
		super.onStop();
	}

	// called when the Activity is destroyed
	@Override
	protected void onDestroy()
	{
		System.out.println("SlideshowPlayer / onDestroy ");
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}


	// Runnable: runSlideshow
	private Runnable runSlideshow = new Runnable()
	{
		@Override
		public void run()
		{
			if(imageIndex >= slideshow.imageSize())
				imageIndex = 0;

			String uriStr = slideshow.getImageAt(imageIndex);
			System.out.println(" Runnable updateSlideshow / imageIndex = " + imageIndex);
//			if(SlideshowScreenReceiver.toBeScreenOn) //not needed
			{
				// check if Uri exists
				boolean uriOK;
				
				if(UtilImage.hasImageExtension(uriStr,SlideshowPlayer.this))
					uriOK = Util.isUriExisted(Uri.parse(uriStr).toString(), SlideshowPlayer.this);
				else
					uriOK = false;
				
				if(uriOK)
				{
					imageView = (ImageView) findViewById(R.id.imageView);
	  		      	UilCommon.imageLoader.displayImage(Uri.parse(uriStr).toString() ,
	  		    		  					 imageView,
	  		    		  					 UilCommon.optionsForFadeIn,
	  		    		  					 UilCommon.animateFirstListener);
					imageIndex++;
	  				imageHandler.postDelayed(runSlideshow, DURATION);
				}
				else
				{
					imageIndex++;
					imageHandler.post(runSlideshow); // go to display next instantly
				}
			}
		}
	}; 
   
}