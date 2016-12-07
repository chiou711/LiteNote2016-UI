package com.cw.litenote.util;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

public class UilApplication extends Application {
	
	static final boolean DEVELOPER_MODE = false;
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressWarnings("unused")
	@Override
	public void onCreate() {
//		if (DEVELOPER_MODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) 
		if (DEVELOPER_MODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) 
		{
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDialog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build());
		}

		super.onCreate();

		initImageLoader(getApplicationContext());
	}

	public static void initImageLoader(Context context) {
		// This configuration tuning is custom. You can tune every option, you may tune some of them,
		// or you can create default configuration by
		//  ImageLoaderConfiguration.createDefault(this);
		// method.
		
		//check available memory 
//		UtilMemory.getTotalMemorySize(context);
		
		
		ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration
												   .Builder(context)
												   .threadPriority(Thread.NORM_PRIORITY - 2)
												   .denyCacheImageMultipleSizesInMemory()
												   .memoryCache(new WeakMemoryCache())// for OOM 												   
//												   .memoryCacheExtraOptions(UtilImage.getScreenWidth(context)*2,UtilImage.getScreenHeight(context)*2) //add for image detail
//												   .memoryCacheExtraOptions(1920,1200) //add for image detail
												   .memoryCacheExtraOptions(1920,1080) //add for image detail
												   .threadPoolSize(1) //add for image detail
												   .diskCacheFileNameGenerator(new Md5FileNameGenerator())
												   .tasksProcessingOrder(QueueProcessingType.LIFO);
		ImageLoaderConfiguration config;
		
//		if(Util.CODE_MODE == Util.RELEASE_MODE) 
			config = builder.build();			
//		else
//			config = builder.writeDebugLogs().build(); // Remove for release app
		
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}
	

}