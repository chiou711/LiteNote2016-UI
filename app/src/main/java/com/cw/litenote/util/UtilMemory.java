package com.cw.litenote.util;

import java.io.File;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

public class UtilMemory {

	static String ERROR = "Get memroy error";
	
	public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static String getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return formatSize(availableBlocks * blockSize);
    }

    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return formatSize(totalBlocks * blockSize);
    }

    public static String getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return formatSize(availableBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    public static String getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return formatSize(totalBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    public static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }
    
    
    //
    static String TAG = "Memory tag";
    public static void getTotalMemorySize(Context context)    
    {
    	ActivityManager activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
    	MemoryInfo memoryInfo = new MemoryInfo();
    	activityManager.getMemoryInfo(memoryInfo);

//    	Log.i(TAG, " memoryInfo.availMem " + memoryInfo.availMem + "\n" );
//    	Log.i(TAG, " memoryInfo.lowMemory " + memoryInfo.lowMemory + "\n" );
//    	Log.i(TAG, " memoryInfo.threshold " + memoryInfo.threshold + "\n" );
    	System.out.println(" memoryInfo.availMem " + memoryInfo.availMem + "\n" );
    	System.out.println(" memoryInfo.lowMemory " + memoryInfo.lowMemory + "\n" );
    	System.out.println(" memoryInfo.threshold " + memoryInfo.threshold + "\n" );

//    	List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
//
//    	Map<Integer, String> pidMap = new TreeMap<Integer, String>();
//    	for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses)
//    	{
//    	    pidMap.put(runningAppProcessInfo.pid, runningAppProcessInfo.processName);
//    	}
//
//    	Collection<Integer> keys = pidMap.keySet();
//
//    	for(int key : keys)
//    	{
//    	    int pids[] = new int[1];
//    	    pids[0] = key;
//    	    android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
//    	    for(android.os.Debug.MemoryInfo pidMemoryInfo: memoryInfoArray)
//    	    {
//    	        Log.i(TAG, String.format("** MEMINFO in pid %d [%s] **\n",pids[0],pidMap.get(pids[0])));
//    	        Log.i(TAG, " pidMemoryInfo.getTotalPrivateDirty(): " + pidMemoryInfo.getTotalPrivateDirty() + "\n");
//    	        Log.i(TAG, " pidMemoryInfo.getTotalPss(): " + pidMemoryInfo.getTotalPss() + "\n");
//    	        Log.i(TAG, " pidMemoryInfo.getTotalSharedDirty(): " + pidMemoryInfo.getTotalSharedDirty() + "\n");
//    	    }
//    	}
    }
}
