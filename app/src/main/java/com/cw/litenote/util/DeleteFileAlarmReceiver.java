package com.cw.litenote.util;

import java.io.File;
import java.io.FilenameFilter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

public class DeleteFileAlarmReceiver extends BroadcastReceiver 
{
   private static final String EXTRA_FILENAME = "com.cwc.litenote.extras.filename";

   public DeleteFileAlarmReceiver(){}

   public DeleteFileAlarmReceiver(Context context, long timeMilliSec, String filename)
   {
		Intent intent = new Intent(context, DeleteFileAlarmReceiver.class);
		intent.putExtra(EXTRA_FILENAME, filename);
		
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, timeMilliSec, pendIntent);
   }

   @Override
   public void onReceive(final Context context, Intent intent) 
   {
	   System.out.println("DeleteFileAlarmReceiver / _onReceive");
		// Note: if launch Send mail twice, file name founded is the first one, not the second one
	    // so, delete any file starts with LiteNote_SEND and ends with txt
		
	    // SD card path + "/" + directory path
	    String folderString = Environment.getExternalStorageDirectory().toString() + 
	    		              "/" + 
        					  Util.getStorageDirName(context);
	    File folder = new File( folderString);       
		
		File[] files = folder.listFiles( new FilenameFilter() 
		{
		    @Override
		    public boolean accept( final File dir,
		                           final String name ) {
//		        return name.matches( "LiteNote_SEND.*\\.txt" ); // starts with LiteNote_SEND, ends with txt
//		        return name.matches( ".*\\.txt" ); // end with txt
//		        return name.matches("(LiteNote_SEND.+(\\.(?i)(txt))$)" );
		        return name.matches("("+ Util.getStorageDirName(context)+"_SEND.+(\\.(?i)(xml))$)" );
		    }
		} );
		for ( final File fileFound : files )
		{
			System.out.println("fileFound = " + fileFound.getName());
		    if ( !fileFound.delete() )
			{
		        System.err.println( "Can't remove " + fileFound.getAbsolutePath() );
		    }
		}
   }
}
