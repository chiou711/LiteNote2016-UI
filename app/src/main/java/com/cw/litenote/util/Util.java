package com.cw.litenote.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.main.TabsHost;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.video.UtilVideo;
import com.cw.litenote.preference.Define;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

public class Util 
{
    SharedPreferences mPref_vibration;
    private static Context mContext;
	private Activity mAct;
	private String mEMailString;
    private static DB_folder mDbFolder;
	private static DB_page mDbPage;
    public static String NEW_LINE = "\r" + System.getProperty("line.separator");

	private static int STYLE_DEFAULT = 1;
    
    public static int ACTIVITY_TAKE_PICTURE = 3;
    public static int CHOOSER_SET_PICTURE = 4;
    public static int CHOOSER_SET_AUDIO = 5;

	private int defaultBgClr;
	private int defaultTextClr;



    
    public Util(){}
    
	public Util(FragmentActivity activity) {
		mContext = activity;
		mAct = activity;
	}
	
	public Util(Context context) {
		mContext = context;
	}
	
	// set vibration time
	public void vibrate()
	{
		mPref_vibration = mContext.getSharedPreferences("vibration", 0);
    	if(mPref_vibration.getString("KEY_ENABLE_VIBRATION","yes").equalsIgnoreCase("yes"))
    	{
			Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
			if(!mPref_vibration.getString("KEY_VIBRATION_TIME","25").equalsIgnoreCase(""))
			{
				int vibLen = Integer.valueOf(mPref_vibration.getString("KEY_VIBRATION_TIME","25"));
				mVibrator.vibrate(vibLen); //length unit is milliseconds
				System.out.println("vibration len = " + vibLen);
			}
    	}
	}
	
	// export to SD card: for checked pages
	public String exportToSdCard(String filename, List<Boolean> checkedArr)
	{   
		//first row text
		String data ="";
		//get data from DB
		if(checkedArr == null)
			data = queryDB(data,null);// all pages
		else
			data = queryDB(data,checkedArr);
		
		// sent data
		data = addXmlTag(data);
		mEMailString = data;
		
		exportToSdCardFile(data,filename);
		
		return mEMailString;
	}
	
	// save to SD card: for NoteView class
	public String exportStringToSdCard(String filename, String curString)
	{   
		//sent data
		String data = "";
		data = data.concat(curString);
		
		mEMailString = data;
		
		exportToSdCardFile(data,filename);
		
		return mEMailString;
	}
	
	// Export data to be SD Card file
	private void exportToSdCardFile(String data,String filename)
	{
	    // SD card path + "/" + directory path
	    String dirString = Environment.getExternalStorageDirectory().toString() + 
	    		              "/" + 
	    		              Util.getStorageDirName(mContext);
	    
		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();
		File file = new File(dir, filename);
		file.setReadOnly();
		
//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(file);
//		} catch (IOException e1) {
//			System.out.println("_FileWriter error");
//			e1.printStackTrace();
//		}
//		BufferedWriter bw = new BufferedWriter(fw);
		
		BufferedWriter bw = null;
		OutputStreamWriter osw = null;

		int BUFFER_SIZE = 8192;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(file.getPath()), "UTF-8");
			bw = new BufferedWriter(osw,BUFFER_SIZE);

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try {
			bw.write(data);
			bw.flush();
			osw.close();
			bw.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
    /**
     * Query current data base
     *
     */
    private String queryDB(String data, List<Boolean> checkedArr)
    {
    	String curData = data;
    	
    	// folder
    	int folderTableId = Util.getPref_lastTimeView_folder_tableId(mContext);
    	mDbFolder = new DB_folder(mContext, folderTableId);

    	// page
        int pageTableId = Util.getPref_lastTimeView_page_tableId(mContext);
		mDbPage = new DB_page(mContext,pageTableId);

    	int tabCount = mDbFolder.getPagesCount(true);
    	for(int i=0;i<tabCount;i++)
    	{
    		// null: all pages
        	if((checkedArr == null ) || ( checkedArr.get(i)  ))
    		{
	        	// set Sent string Id
				List<Long> noteIdArray = new ArrayList<>();
				DB_page.setFocusPage_tableId(mDbFolder.getPageTableId(i,true));
				
				mDbPage.open();
				int count = mDbPage.getNotesCount(false);
	    		for(int k=0; k<count; k++)
	    		{
    				noteIdArray.add(k, mDbPage.getNoteId(k,false));
	    		}
	    		mDbPage.close();
	    		curData = curData.concat(getStringWithXmlTag(noteIdArray));
    		}
    	}
    	return curData;
    	
    }
    
    // get current time string
    public static String getCurrentTimeString()
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		
//		int hour = cal.get(Calendar.HOUR);//12h 
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		int mSec = cal.get(Calendar.MILLISECOND);
		
		String strTime = year 
				+ "" + String.format(Locale.US,"%02d", month)
				+ "" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "_" + String.format(Locale.US,"%02d", hour)
				+ "" + String.format(Locale.US,"%02d", min)
				+ "" + String.format(Locale.US,"%02d", sec) 
				+ "_" + String.format(Locale.US,"%03d", mSec);
//		System.out.println("time = "+  strTime );
		return strTime;
    }
    
    // get time string
    public static String getTimeString(Long time)
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		int hour = cal.get(Calendar.HOUR);//12h 
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		
		String strTime = year 
				+ "-" + String.format(Locale.US,"%02d", month)
				+ "-" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "    " + String.format(Locale.US,"%02d", hour)
				+ ":" + String.format(Locale.US,"%02d", min)
				+ ":" + String.format(Locale.US,"%02d", sec) ;
//		System.out.println("time = "+  strTime );
		
		return strTime;
    }
    
//    void deleteAttachment(String mAttachmentFileName)
//    {
//		// delete file after sending
//		String attachmentPath_FileName = Environment.getExternalStorageDirectory().getPath() + "/" +
//										 mAttachmentFileName;
//		File file = new File(attachmentPath_FileName);
//		boolean deleted = file.delete();
//		if(deleted)
//			System.out.println("delete file is OK");
//		else
//			System.out.println("delete file is NG");
//    }
    
    // add mark to current page
	public void addMarkToCurrentPage(DialogInterface dialogInterface,final int action)
	{
		mDbFolder = new DB_folder(MainAct.mAct, Util.getPref_lastTimeView_folder_tableId(MainAct.mAct));
	    ListView listView = ((AlertDialog) dialogInterface).getListView();
	    final ListAdapter originalAdapter = listView.getAdapter();
	    final int style = Util.getCurrentPageStyle(mAct);
        CheckedTextView textViewDefault = new CheckedTextView(mAct) ;
        defaultBgClr = textViewDefault.getDrawingCacheBackgroundColor();
        defaultTextClr = textViewDefault.getCurrentTextColor();

	    listView.setAdapter(new ListAdapter()
	    {
	        @Override
	        public int getCount() {
	            return originalAdapter.getCount();
	        }
	
	        @Override
	        public Object getItem(int id) {
	            return originalAdapter.getItem(id);
	        }
	
	        @Override
	        public long getItemId(int id) {
	            return originalAdapter.getItemId(id);
	        }
	
	        @Override
	        public int getItemViewType(int id) {
	            return originalAdapter.getItemViewType(id);
	        }
	
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            View view = originalAdapter.getView(position, convertView, parent);
	            //set CheckedTextView in order to change button color
	            CheckedTextView textView = (CheckedTextView)view;
	            if(mDbFolder.getPageTableId(position,true) == DB_page.getFocusPage_tableId())
	            {
		            textView.setTypeface(null, Typeface.BOLD_ITALIC);
		            textView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		            textView.setTextColor(ColorSet.mText_ColorArray[style]);
			        if(style%2 == 0)
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
			        else
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_light);

                    if(action == Page.MOVE_TO)
                        textView.setCheckMarkDrawable(null);
	            }
	            else
	            {
		        	textView.setTypeface(null, Typeface.NORMAL);
		            textView.setBackgroundColor(defaultBgClr);
		            textView.setTextColor(defaultTextClr);
		            textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
	            }
	            return view;
	        }

	        @Override
	        public int getViewTypeCount() {
	            return originalAdapter.getViewTypeCount();
	        }

	        @Override
	        public boolean hasStableIds() {
	            return originalAdapter.hasStableIds();
	        }
	
	        @Override
	        public boolean isEmpty() {
	            return originalAdapter.isEmpty();
	        }

	        @Override
	        public void registerDataSetObserver(DataSetObserver observer) {
	            originalAdapter.registerDataSetObserver(observer);
	
	        }
	
	        @Override
	        public void unregisterDataSetObserver(DataSetObserver observer) {
	            originalAdapter.unregisterDataSetObserver(observer);
	
	        }
	
	        @Override
	        public boolean areAllItemsEnabled() {
	            return originalAdapter.areAllItemsEnabled();
	        }
	
	        @Override
	        public boolean isEnabled(int position) {
	            return originalAdapter.isEnabled(position);
	        }
	    });
	}
	
	// get App default storage directory name
	static public String getStorageDirName(Context context)
	{
//		return context.getResources().getString(R.string.app_name);

		Resources currentResources = context.getResources();
		Configuration conf = new Configuration(currentResources.getConfiguration());
		conf.locale = Locale.ENGLISH; // apply English to avoid reading directory error
		Resources newResources = new Resources(context.getAssets(), 
											   currentResources.getDisplayMetrics(),
											   conf);
		String appName = newResources.getString(R.string.app_name);

		// restore locale
		new Resources(context.getAssets(), 
					  currentResources.getDisplayMetrics(), 
					  currentResources.getConfiguration());
		
//		System.out.println("Util / _getStorageDirName / appName = " + appName);
		return appName;		
	}
	
	// get style
	static public int getNewPageStyle(Context context)
	{
		SharedPreferences mPref_style;
		mPref_style = context.getSharedPreferences("style", 0);
		return mPref_style.getInt("KEY_STYLE",STYLE_DEFAULT);
	}
	
	
	// set button color
	private static String[] mItemArray = new String[]{"1","2","3","4","5","6","7","8","9","10"};
    public static void setButtonColor(RadioButton rBtn,int iBtnId)
    {
    	if(iBtnId%2 == 0)
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_dark);
    	else
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_light);
		rBtn.setBackgroundColor(ColorSet.mBG_ColorArray[iBtnId]);
		rBtn.setText(mItemArray[iBtnId]);
		rBtn.setTextColor(ColorSet.mText_ColorArray[iBtnId]);
    }
	
    // get current page style
	static public int getCurrentPageStyle(Context context)
	{
		return TabsHost.mDbFolder.getPageStyle(TabsHost.mNow_pageId, true);
	}

	// get style count
	static public int getStyleCount()
	{
		return ColorSet.mBG_ColorArray.length;
	}
	
	// set page table id of last time view
	public static void setPref_lastTimeView_folder_tableId(Activity act, int folderTableId )
	{
	  SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
	  String keyName = "KEY_LAST_TIME_VIEW_DRAWER_FOLDER_TABLE_ID";
      pref.edit().putInt(keyName, folderTableId).apply();
	}
	
	// get page table id of last time view
	public static int getPref_lastTimeView_folder_tableId(Context context)
	{
		SharedPreferences pref = context.getSharedPreferences("last_time_view", 0);
		String keyName = "KEY_LAST_TIME_VIEW_DRAWER_FOLDER_TABLE_ID";
		return pref.getInt(keyName, 1); // folder table Id: default is 1
	}	

	// set page table id of last time view
	public static void setPref_lastTimeView_page_tableId(Activity act, int pageTableId )
	{
	  SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
	  String keyPrefix = "KEY_FOLDER_TABLE_ID_";
	  int tableId = Util.getPref_lastTimeView_folder_tableId(act);
	  String keyName = keyPrefix.concat(String.valueOf(tableId));
      pref.edit().putInt(keyName, pageTableId).apply();
	}
	
	// get page table id of last time view
	public static int getPref_lastTimeView_page_tableId(Context context)
	{
		SharedPreferences pref = context.getSharedPreferences("last_time_view", 0);
		String keyPrefix = "KEY_FOLDER_TABLE_ID_";
		int tableId = Util.getPref_lastTimeView_folder_tableId(context);
		String keyName = keyPrefix.concat(String.valueOf(tableId));
		// page table Id: default is 1
		return pref.getInt(keyName, 1); //??? why table is not found sometimes?
//		return String.valueOf(6); //for testing Table not found issue
	}
	
	// remove key of last time view
	public static void removePref_lastTimeView_key(Activity act, int drawerFolderTableId)
	{
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyPrefix = "KEY_FOLDER_TABLE_ID_";
		String keyName = keyPrefix.concat(String.valueOf(drawerFolderTableId));
		pref.edit().remove(keyName).apply();
	}	
	
	// set scroll X of drawer of last time view
	public static void setPref_lastTimeView_scrollX_byFolderTableId(Activity act, int scrollX )
	{
	  SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
	  String keyPrefix = "KEY_FOLDER_TABLE_ID_";
	  int tableId = Util.getPref_lastTimeView_folder_tableId(act);
	  String keyName = keyPrefix.concat(String.valueOf(tableId));
	  keyName = keyName.concat("_SCROLL_X");
      pref.edit().putInt(keyName, scrollX).apply(); //??? it could be not updated soon enough?
	}
	
	// get scroll X of drawer of last time view
	public static Integer getPref_lastTimeView_scrollX_byFolderTableId(Activity act)
	{
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyPrefix = "KEY_FOLDER_TABLE_ID_";
		int tableId = Util.getPref_lastTimeView_folder_tableId(act);
		String keyName = keyPrefix.concat(String.valueOf(tableId));
		keyName = keyName.concat("_SCROLL_X");
		return pref.getInt(keyName, 0); // default scroll X is 0
	}	

	// Set list view first visible Index of last time view
	public static void setPref_lastTimeView_list_view_first_visible_index(Activity act, int index )
	{
//		System.out.println("Util / _setPref_lastTimeView_list_view_first_visible_index / index = " + index);
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX";
		String location = getCurrentListViewLocation(act);
		keyName = keyName.concat(location);
        pref.edit().putInt(keyName, index).apply(); //??? it could not updated soon enough?
	}
	
	// Get list view first visible Index of last time view
	public static Integer getPref_lastTimeView_list_view_first_visible_index(Activity act)
	{
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX";
		String location = getCurrentListViewLocation(act);
		keyName = keyName.concat(location);		
		return pref.getInt(keyName, 0); // default scroll X is 0
	}	
	
	// Set list view first visible index Top of last time view
	public static void setPref_lastTimeView_list_view_first_visible_index_top(Activity act, int top )
	{
//        System.out.println("Util / _setPref_lastTimeView_list_view_first_visible_index_top / top = " + top);
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX_TOP";
		String location = getCurrentListViewLocation(act);
		keyName = keyName.concat(location);
        pref.edit().putInt(keyName, top).apply(); //??? it could be not updated soon enough?
	}
	
	// Get list view first visible index Top of last time view
	public static Integer getPref_lastTimeView_list_view_first_visible_index_top(Activity act)
	{
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX_TOP";
		String location = getCurrentListViewLocation(act);
		keyName = keyName.concat(location);		
		return pref.getInt(keyName, 0);
	}
	
	// set has default import
	public static void setPref_has_default_import(Activity act, boolean has,int position )
	{
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyName = "KEY_HAS_DEFAULT_IMPORT"+position;
		pref.edit().putBoolean(keyName, has).apply();
	}
	
	// get has default import
	public static boolean getPref_has_default_import(Context context,int position)
	{
		SharedPreferences pref = context.getSharedPreferences("last_time_view", 0);
		String keyName = "KEY_HAS_DEFAULT_IMPORT"+position;
		return pref.getBoolean(keyName, false);
	}

//	static String strTitleEdit;
	
	// get Send String with XML tag
	public static String getStringWithXmlTag(List<Long> noteIdArray)
	{
        String PAGE_TAG_B = "<page>";
        String PAGE_NAME_TAG_B = "<page_name>";
        String PAGE_NAME_TAG_E = "</page_name>";
        String NOTE_ITEM_TAG_B = "<note>";
        String NOTE_ITEM_TAG_E = "</note>";
        String TITLE_TAG_B = "<title>";
        String TITLE_TAG_E = "</title>";
        String BODY_TAG_B = "<body>";
        String BODY_TAG_E = "</body>";
        String PICTURE_TAG_B = "<picture>";
        String PICTURE_TAG_E = "</picture>";
        String AUDIO_TAG_B = "<audio>";
        String AUDIO_TAG_E = "</audio>";
        String LINK_TAG_B = "<link>";
        String LINK_TAG_E = "</link>";
        String PAGE_TAG_E = "</page>";
        
        String sentString = NEW_LINE;

    	// when page has page name only, no notes
    	if(noteIdArray.size() == 0)
    	{
        	sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
	        sentString = sentString.concat(NEW_LINE + PAGE_NAME_TAG_B + mDbFolder.getCurrentPageTitle() + PAGE_NAME_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_B);
	    	sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + TITLE_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + BODY_TAG_B +  BODY_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + PICTURE_TAG_B + PICTURE_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + AUDIO_TAG_B + AUDIO_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + LINK_TAG_B + LINK_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + PAGE_TAG_E );
    		sentString = sentString.concat(NEW_LINE);
    	}
    	else
    	{
	        for(int i=0;i< noteIdArray.size();i++)
	        {
				String strTitleEdit;
				int pageTableId = DB_page.getFocusPage_tableId();
				DB_page db_page = new DB_page(MainAct.mAct,pageTableId);
                db_page.open();
		    	Cursor cursorNote = db_page.queryNote(noteIdArray.get(i));
		        strTitleEdit = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_TITLE));
		        strTitleEdit = replaceEscapeCharacter(strTitleEdit);
		        
		        String strBodyEdit = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_BODY));
		        strBodyEdit = replaceEscapeCharacter(strBodyEdit);

		        String strPictureUriStr = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_PICTURE_URI));
		        strPictureUriStr = replaceEscapeCharacter(strPictureUriStr);

		        String strAudioUriStr = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_AUDIO_URI));
		        strAudioUriStr = replaceEscapeCharacter(strAudioUriStr);

		        String strLinkUriStr = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_LINK_URI));

		        strLinkUriStr = replaceEscapeCharacter(strLinkUriStr);
		        
		        int mark = cursorNote.getInt(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_MARKING));
		        String srtMark = (mark == 1)? "[s]":"[n]";
                db_page.close();

                if(i==0)
		        {
                    DB_folder db_folder = new DB_folder(MainAct.mAct,getPref_lastTimeView_folder_tableId(MainAct.mAct));
                    sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
		        	sentString = sentString.concat(NEW_LINE + PAGE_NAME_TAG_B + db_folder.getCurrentPageTitle() + PAGE_NAME_TAG_E );
		        }
		        
		        sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_B);
	        	sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + srtMark + strTitleEdit + TITLE_TAG_E);
				sentString = sentString.concat(NEW_LINE + BODY_TAG_B + strBodyEdit + BODY_TAG_E);
				sentString = sentString.concat(NEW_LINE + PICTURE_TAG_B + strPictureUriStr + PICTURE_TAG_E);
				sentString = sentString.concat(NEW_LINE + AUDIO_TAG_B + strAudioUriStr + AUDIO_TAG_E);
				sentString = sentString.concat(NEW_LINE + LINK_TAG_B + strLinkUriStr + LINK_TAG_E);
				sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_E);
		        sentString = sentString.concat(NEW_LINE);
		    	if(i==noteIdArray.size()-1)
		        	sentString = sentString.concat(NEW_LINE +  PAGE_TAG_E);

	        }
    	}
    	return sentString;
	}

    // replace special character (e.q. amp sign) for avoiding XML paring exception 
	//      &   &amp;
	//      >   &gt;
	//      <   &lt;
	//      '   &apos;
	//      "   &quot;
	private static String replaceEscapeCharacter(String str)
	{
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll(">", "&gt;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll("'", "&apos;");
        str = str.replaceAll("\"", "&quot;");
        return str;
	}
	
	// add XML tag
	public static String addXmlTag(String str)
	{
		String ENCODING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String XML_TAG_B = NEW_LINE + "<LiteNote>";
        String XML_TAG_E = NEW_LINE + "</LiteNote>";
        
        String data = ENCODING + XML_TAG_B;
        
        data = data.concat(str);
		data = data.concat(XML_TAG_E);
		
		return data;
	}

	// trim XML tag
	public String trimXMLtag(String string) {
		string = string.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>","");
		string = string.replace("<LiteNote>","");
		string = string.replace("<page>","");
		string = string.replace("<page_name>","=== Page: ");
		string = string.replace("</page_name>"," ===");
		string = string.replace("<note>","--- note ---");
		string = string.replace("[s]","");
		string = string.replace("[n]","");
		string = string.replace("<title></title>"+NEW_LINE,"");
        string = string.replace("<body></body>"+NEW_LINE,"");
        string = string.replace("<picture></picture>"+NEW_LINE,"");
        string = string.replace("<audio></audio>"+NEW_LINE,"");
        string = string.replace("<link></link>"+NEW_LINE,"");
		string = string.replace("<title>","Title: ");
		string = string.replace("</title>","");
		string = string.replace("<body>","Body: ");
		string = string.replace("</body>","");
		string = string.replace("<picture>","Picture: ");
		string = string.replace("</picture>","");		
		string = string.replace("<audio>","Audio: ");
		string = string.replace("</audio>","");		
		string = string.replace("<link>","Link: ");
		string = string.replace("</link>","");		
		string = string.replace("</note>","");
		string = string.replace("</page>"," ");
		string = string.replace("</LiteNote>","");
		string = string.trim();
		return string;
	}
	
	
	// get local real path from URI
	public static String getLocalRealPathByUri(Context context, Uri contentUri) {
		  Cursor cursor = null;
		  try { 
		    String[] proj = { MediaStore.Images.Media.DATA };
		    cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
		    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);//???
		    cursor.moveToFirst();
		    return cursor.getString(column_index);
		  }
		  catch (Exception e){
			return null;  
		  }
		  finally {
		    if (cursor != null) {
		      cursor.close();
		    }
		  }
	}
	
	// get display name by URI string
	public static String getDisplayNameByUriString(String uriString, Activity activity)
	{
		String display_name = "";
		String scheme = getUriScheme(uriString);
		
		if(Util.isEmptyString(uriString) || Util.isEmptyString(scheme))
			return display_name;
		
		Uri uri = Uri.parse(uriString);
		//System.out.println("Uri string = " + uri.toString());
		//System.out.println("Uri last segment = " + uri.getLastPathSegment());
		if(scheme.equalsIgnoreCase("content"))
		{
	        String[] proj = { MediaStore.MediaColumns.DISPLAY_NAME };
	        Cursor cursor = null;
	        try{
	        	cursor = activity.getContentResolver().query(uri, proj, null, null, null);
	        }
	        catch (Exception e)
	        {
	        	Toast toast = Toast.makeText(activity, "Uri is not accessible", Toast.LENGTH_SHORT);
				toast.show();
	        }
	        
            if((cursor != null) && cursor.moveToFirst()) //reset the cursor
            {
                int col_index=-1;
                do
                {
                	col_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                	display_name = cursor.getString(col_index);
                }while(cursor.moveToNext());
                cursor.close();
            }
		}
		else if(scheme.equalsIgnoreCase("http") ||
				scheme.equalsIgnoreCase("https")   )
		{
            // if display name can not be displayed, then show last segment instead
          	display_name = uri.getLastPathSegment();
		}
		else if(scheme.equalsIgnoreCase("file")  )
		{
			if(UtilAudio.hasAudioExtension(uriString))
			{
				// Get MP3 title from MP3 file
				String audio_artist = null;
				String audio_title = null;
				MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//				System.out.println("Util / _getDisplayNameByUriString / uri = " + uri);
				if(!Util.isEmptyString(uriString))
				{
					try
					{
						mmr.setDataSource(activity,uri);
					}
					catch(Exception e)
					{
						
					}
					//??? 12-30 15:11:31.808: E/AndroidRuntime(8813): java.lang.IllegalArgumentException
					audio_title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
					audio_artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
					display_name = audio_title + " / " + audio_artist;
				}
				
				// add for video with mkv format 
				if(Util.isEmptyString(audio_title) && 
				   Util.isEmptyString(audio_artist)   )
				{
					display_name = uri.getLastPathSegment();
				}
			}
			else
				display_name = uri.getLastPathSegment();
		}
		//System.out.println("display_name = " + display_name);
                	
        return display_name;
	}
	
	// get scheme by Uri string
	public static String getUriScheme(String string)
	{
 		Uri uri = Uri.parse(string);
		return uri.getScheme();
	}
	
	
	// is URI existed for Activity
	public static boolean isUriExisted(String uriString, Activity activity)
	{
		boolean bFileExist = false;
		if(!Util.isEmptyString(uriString))
		{
			Uri uri = Uri.parse(uriString);

			// when scheme is content and check local file
			File file = null;
			try
			{
				file = new File(uri.getPath());
			}
			catch(Exception e)
			{
				System.out.println("Util / _isUriExisted / local file not found exception");
			}

			if(file != null)
			{
				if(file.exists()) //??? _exists has bug?
					bFileExist = true;
				else
                {
                    // for some file (eg. Universal Image Loader @#&=+-_.,!()~'%20.png ) ,_file.exists will return false,
                    // after _createNewFile, will create a file whose file size is zero, _file.exists will return true
                    try {
                        file.createNewFile();
//                        System.out.println("Util / _isUriExisted / 0 size file is created");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // check again after _createNewFile
                    bFileExist = file.exists() ? true: false;
                }
			}
			else
				bFileExist = false;

			// when scheme is content and check remote file
			if(!bFileExist)
			{
				try
				{
					ContentResolver cr = activity.getContentResolver();
					cr.openInputStream(uri); //??? why this could hang up system?
					bFileExist = true;
				}
				catch (FileNotFoundException exception)
				{
					System.out.println("Util / _isUriExisted / remote file not found exception");
			    }
				catch (SecurityException se)
				{
					System.out.println("Util / _isUriExisted / remote security exception");
				}
				catch (Exception e)
				{
					System.out.println("Util / _isUriExisted / remote exception");
				}
			}
//			System.out.println("Util / _isUriExisted / bFileExist (content)= " + bFileExist);

			// when scheme is https or http
			try
			{
				if(Patterns.WEB_URL.matcher(uriString).matches())//??? URL can check URI string?
					bFileExist = true;
			}
			catch (Exception e)
			{

		    }
//			System.out.println("Util / _isUriExisted / bFileExist (web url)= " + bFileExist);
		}
		return bFileExist;
	}
	
	// is Empty string
	public static boolean isEmptyString(String str)
	{
		boolean empty = true;
		if( str != null )
		{
			if(str.length() > 0 )
				empty = false;
		}
		return empty;
	}
	
	/***
	 * pictures directory or gallery directory
	 * 
	 * get: storage/emulated/0/
	 * with: Environment.getExternalStorageDirectory();
	 * 
	 * get: storage/emulated/0/Pictures
	 * with: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	 * 
	 * get: storage/emulated/0/DCIM
	 * with: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
	 * or with: Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM";  
	 *  
	 * get: storage/emulated/0/Android/data/com.cwc.litenote/files
	 * with: storageDir[0] got from File[] storageDir = context.getExternalFilesDirs(null);
	 * 
	 * get: storage/ext_sd/Android/data/com.cwc.litenote/files
	 * with: storageDir[1] got from File[] storageDir = context.getExternalFilesDirs(null);
	 *   
	 */
	public static File getPicturesDir(Context context)
    {
    	if(Define.PICTURE_PATH_BY_SYSTEM_DEFAULT)
    	{
    		// Notes: 
    		// 1 for Google Camera App: 
    		// 	 - default path is /storage/sdcard/DCIM/Camera
    		// 	 - Can not save file to external SD card
    		// 2 for hTC default camera App:
    		//   - default path is /storage/ext_sd/DCIM/100MEDIA
    		//   - Can save file to internal SD card and external SD card, it is decided by hTC App
    		
//    		// is saved to preference after taking picture
//    		SharedPreferences pref_takePicture = context.getSharedPreferences("takePicutre", 0);	
//    		String picDirPathPref = pref_takePicture.getString("KEY_SET_PICTURE_DIR","unknown");
//    		System.out.println("--- Util / _getPicturesDir / pictureDirPath = " + picDirPathPref);
    		
    		String dirString;
    		File dir = null;
    		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) 
        	{
    			dirString = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
        		// add App name for sub-directory
        		dirString = dirString.concat("/"+ Util.getStorageDirName(context));
        		System.out.println("Util / _getPicturesDir / dirString = " + dirString);
        		dir = new File(dirString);
        	}
    		return dir;
    	}
    	else
    	{
    		File[] storageDir = context.getExternalFilesDirs(null); 
    		for(File dir:storageDir)
    			System.out.println("storageDir[] = " + dir);
    		// for Kitkat: write permission is off for external SD card, 
    		// but App can freely access Android/data/com.example.foo/ 
    		// on external storage devices with no permissions. 
    		// i.e. 
        	//		storageDir[1] = file:///storage/ext_sd/Android/data/com.cwc.litenote/files
            File appPicturesDir = new File(storageDir[1]+"/"+"pictures");// 0: system 1:ext_sd    
    		System.out.println("Util / _getPicturesDir / appPicturesDir = " + appPicturesDir);
            return appPicturesDir;
        }
    }
    
    static boolean isValid = false;
    static String mStringUrl;
    public static int mResponseCode;
    static String mResponseMessage;
	public static int oneSecond = 1000;
    
	// check network connection
    public static boolean isNetworkConnected(Activity act)
    {
    	final ConnectivityManager conMgr = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
    	final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
    	if (activeNetwork != null && activeNetwork.isConnected()) {
    		System.out.println("network is connected");
    		return true;
    	} else {
    		System.out.println("network is NOT connected");
    		return false;
    	} 
    }
    
    // try Url connection
    protected static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    static public void tryUrlConnection(String strUrl, final Activity act) throws Exception 
    {
//    	mStringUrl = strUrl.replaceFirst("^https", "http");
    	mResponseCode = 0;
    	mStringUrl = strUrl;
    	Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() 
            {
        	    try
        	    {
        			String encodedUrl = Uri.encode(mStringUrl, ALLOWED_URI_CHARS);
        			HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
        			conn.setRequestMethod("HEAD");
        			conn.setConnectTimeout(oneSecond); // cause exception if connection error
        			conn.setReadTimeout(oneSecond*4);
        			mResponseCode = conn.getResponseCode();
        	        mResponseMessage = conn.getResponseMessage();
        	    } 
        	    catch (IOException exception) 
        	    {
        	    	mResponseCode = 0;
        	    	mResponseMessage = "io exception";
        	    	System.out.println("------------------ tryUrlConnection / io exception");
        	    	exception.printStackTrace();
				} 
        	    catch (Exception e) 
        	    {
        	    	System.out.println("------------------ tryUrlConnection / exception");
					e.printStackTrace();
				}
        	    System.out.println("Response Code : " + mResponseCode +
        	    				   " / Response Message: " + mResponseMessage );
            }
        });    	
    }    
    
    // location about drawer table Id and page table Id
    static String getCurrentListViewLocation(Activity act)
    {
    	String strLocation = "";
    	// folder
    	int folderTableId = getPref_lastTimeView_folder_tableId(act);
    	String strFolderTableId = String.valueOf(folderTableId);
    	// page
    	int pageTableId = getPref_lastTimeView_page_tableId(act);
        String strPageTableId = String.valueOf(pageTableId);
    	strLocation = "_" + strFolderTableId + "_" + strPageTableId;
    	return strLocation;
    }
    
	// get Url array of directory files
    public final static int AUDIO = 0;
    public final static int IMAGE = 1;
    public final static int VIDEO = 2;
    public static String[] getUrlsByFiles(File[] files,int type)
    {
        if(files == null)
        {
        	return null;
        }
        else
        {
        	String path[] = new String[files.length];
            int i=0;
            
	        for(File file : files)
	        {
		        if( ( (type == AUDIO) && (UtilAudio.hasAudioExtension(file)) ) ||
		        	( (type == IMAGE) && (UtilImage.hasImageExtension(file)) ) ||
		        	( (type == VIDEO) && (UtilVideo.hasVideoExtension(file)) )  )	
	            {
		            if(i< files.length)
		            {
//		            	path[i] = "file:///" + file.getPath();
		            	path[i] = "file://" + file.getAbsolutePath();
		            	System.out.println("path[i] = " + path[i]);
		            	i++;
		            }
	            }
	        }
	        return path;
        }
    }		    
    
	// show saved file name
	public static void showSavedFileToast(String string,Activity act)
	{
		Toast.makeText(act,
						string,
						Toast.LENGTH_SHORT)
						.show();
	}

	static public boolean isLandscapeOrientation(Activity act)
	{
		int currentOrientation = act.getResources().getConfiguration().orientation;

		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
			return true;
		else
			return false;
	}


	static public void lockOrientation(Activity act) {
//	    if (act.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//	        act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
//	    } else {
//	        act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
//	    }
	    
	    int currentOrientation = act.getResources().getConfiguration().orientation;
	    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
	    }
	    else {
//		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
	    }	    
	}

	static public void unlockOrientation(Activity act) {
	    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	// get time format string
	static public String getTimeFormatString(long duration)
	{
		long hour = TimeUnit.MILLISECONDS.toHours(duration);
		long min = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hour);
		long sec = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.HOURS.toSeconds(hour) - TimeUnit.MINUTES.toSeconds(min);
		String str = String.format(Locale.US,"%2d:%02d:%02d", hour, min, sec);
		return str;
	}
	
	public static boolean isYouTubeLink(String strLink)
	{
		boolean is = false;
		
		if(Util.isEmptyString(strLink))
			return is;
		
//		//check if single string
//		String strArr[] = strLink.split("\\s"); // \s: A whitespace character, short for [ \t\n\x0b\r\f]
//		int cnt = 0;
//		for(int i=0; i < strArr.length; i++ )
//		{
//			System.out.println("strArr [" + i + "] = " + strArr[i]);
//			cnt++;
//		}
//		
//		//check if youTube keyword
//		if( (cnt == 1) &&
//			(strLink.contains("youtube") ||
//			 strLink.contains("youtu.be")  )&&
//			strLink.contains("//")) 
//		{
//			is = true;
//		}
		
		//check if single string
		String strArr[] = strLink.split("/");
//		for(int i=0; i < strArr.length; i++ )
//		{
//			System.out.println("YouTube strArr [" + i + "] = " + strArr[i]);
//		}
		
		if(strArr.length >= 2)
		{
			if(	strArr[2].contains("youtube") ||
				strArr[2].contains("youtu.be")  ) 		
			{
				is = true;
			}		
		}
		
		return is;
	}
	
    @TargetApi(Build.VERSION_CODES.KITKAT)
	public static Intent chooseMediaIntentByType(Activity act,String type)
    {
	    // set multiple actions in Intent 
	    // Refer to: http://stackoverflow.com/questions/11021021/how-to-make-an-intent-with-multiple-actions
        PackageManager pm = act.getPackageManager();
        Intent getImageContentIntent = null;
        Intent openInChooser = null;
        List<ResolveInfo> resInfoSaf = null;
        Intent[] extraIntentsSaf = null;
        List<ResolveInfo> resInfo = null;
        Intent[] extraIntents = null;
        
        // SAF support starts from Kitkat
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
			// BEGIN_INCLUDE (use_open_document_intent)
	        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        	getImageContentIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
	        
	        // Filter to only show results that can be "opened", such as a file (as opposed to a list
	        // of contacts or time zones)
        	getImageContentIntent.addCategory(Intent.CATEGORY_OPENABLE);	        
        	getImageContentIntent.setType(type);

        	// get extra SAF intents
	        resInfoSaf = pm.queryIntentActivities(getImageContentIntent, 0);
//	        System.out.println("resInfoSaf size = " + resInfoSaf.size());
        	extraIntentsSaf = new Intent[resInfoSaf.size()];
	        for (int i = 0; i < resInfoSaf.size(); i++) 
	        {
	            // Extract the label, append it, and repackage it in a LabeledIntent
	            ResolveInfo ri = resInfoSaf.get(i);
	            String packageName = ri.activityInfo.packageName;
	            Intent intent = new Intent();
	            intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
	            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
	            intent.setType(type);
	            
		        Spannable saf_span = new SpannableString(" (CLOUD)");
		        saf_span.setSpan(new ForegroundColorSpan(android.graphics.Color.RED), 0, saf_span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        CharSequence newSafLabel = TextUtils.concat(ri.loadLabel(pm), saf_span);
//	        	System.out.println("SAF label " + i + " = " + newSafLabel );
	        	extraIntentsSaf[i] = new LabeledIntent(intent, packageName, newSafLabel, ri.icon);
	        } 
        }   
        
        // get extra non-SAF intents
        getImageContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getImageContentIntent.setType(type);
        resInfo = pm.queryIntentActivities(getImageContentIntent, 0);	        
        extraIntents = new Intent[resInfo.size()];	        
        for (int i = 0; i < resInfo.size(); i++) 
        {
            ResolveInfo ri = resInfo.get(i);
            String packageName = ri.activityInfo.packageName;
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType(type);
            CharSequence label = ri.loadLabel(pm);
        	extraIntents[i] = new LabeledIntent(intent, packageName, label, ri.icon);
        }
        
        // get all intents
        int safSize = (resInfoSaf == null)? 0:resInfoSaf.size(); 
        int sizeAll = safSize + resInfo.size();
        Intent[] extraIntentsAll = new Intent[sizeAll];
        int j =0;
        for(int i = 0; i < sizeAll; i++)
        {
        	if( i < safSize)
        		extraIntentsAll[i] = extraIntentsSaf[i];
        	else if( (i >= safSize) && (i < sizeAll ))
        	{
        		extraIntentsAll[i] = extraIntents[j];
        		j++;
        	}
        }
        
        // calculate the duplication number and remove duplication
        int len = extraIntentsAll.length;
    	int duplicatedNum = 0;
        System.out.println("extraIntentsAll size = " + len );
        for(int i=0; i<len ; i++)
        {
        	ComponentName component = null;
        	if(extraIntentsAll[i] !=  null)
        	{
        		component = extraIntentsAll[i].getComponent();
//	        		System.out.println("--- extraIntentsAll (" + i + ")= " + extraIntentsAll[i].toString());
//	        		System.out.println("--- cmp = " + component);
	        	for(int k=0; k< len; k++)
	        	{
	        		if((k != i) && (extraIntentsAll[k] !=  null) && (component.equals(extraIntentsAll[k].getComponent()) ))
	        		{
	        			duplicatedNum++;
	        			extraIntentsAll[k] = null;
	        		}
	        	}
        	}
        }
        
        // get final intents
        Intent[] extraIntentsFinal = new Intent[len-duplicatedNum];
        int count = 0;
        for(int i=0; i<len ; i++)
        {
        	if(extraIntentsAll[i] != null)
        	{
        		extraIntentsFinal[count] = extraIntentsAll[i];
	        	ComponentName component = extraIntentsFinal[count].getComponent();
	        	System.out.println("--- final cmp of ("+ count + ") = " + component);
        		count++;
        	}
        }
        
        // OK to put extra
        CharSequence charSeq = "";
        
        if(type.startsWith("image"))
        	charSeq = act.getResources().getText(R.string.add_new_chooser_image);
        else if(type.startsWith("video"))
        	charSeq = act.getResources().getText(R.string.add_new_chooser_video);
        else if(type.startsWith("audio"))
        	charSeq = act.getResources().getText(R.string.add_new_chooser_audio);
        	
	    openInChooser = Intent.createChooser(getImageContentIntent, charSeq);
        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntentsFinal);
                	
        return openInChooser;
    }

    public static void setScrollThumb(Context context, View view)
    {
		// Change scroll thumb by reflection
		// ref: http://stackoverflow.com/questions/21806852/change-the-color-of-scrollview-programmatically?lq=1
		try
		{
		    Field mScrollCacheField = View.class.getDeclaredField("mScrollCache");
		    mScrollCacheField.setAccessible(true);
		    Object mScrollCache = mScrollCacheField.get(view);

		    Field scrollBarField = mScrollCache.getClass().getDeclaredField("scrollBar");
		    scrollBarField.setAccessible(true);
		    Object scrollBar = scrollBarField.get(mScrollCache);

		    Method method = scrollBar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
		    method.setAccessible(true);
		    // Set drawable
		    method.invoke(scrollBar, context.getResources().getDrawable(R.drawable.fastscroll_thumb_default_holo));
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		}	    	
    }
    
    // Get YouTube Id
	public static String getYoutubeId(String url) {

	    String videoId = "";
	    
	    // 1st method: for https://www.youtube.com/watch?v=_sQSXwdtxlY format
	    if (url != null && url.trim().length() > 0 && url.startsWith("http")) {
	        String expression = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*";
	        CharSequence input = url;
	        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);//??? some Urls are NG
	        Matcher matcher = pattern.matcher(input);
	        if (matcher.matches()) {
	            String groupIndex1 = matcher.group(7);
	            if (groupIndex1 != null && groupIndex1.length() == 11)
	                videoId = groupIndex1;
	        }
	    }

	    // 2nd method: for https://youtu.be/V7MfPD7kZuQ format
	    if(Util.isEmptyString(videoId))
	    {
		    String reg = "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$";
		    Pattern pattern = Pattern.compile(reg ,Pattern.CASE_INSENSITIVE);
		    Matcher matcher = pattern.matcher(url);
		    if (matcher.matches()){
		        videoId = matcher.group(1);
		    }
	    }
	    System.out.println("Util / _getYoutubeId / video_id = " + videoId);
	    return videoId;		
	}	

	static JsonAsync jsonAsyncTask;
	// Get YouTube title
	public static String getYoutubeTitle(String youtubeUrl) 
	{
    		URL embeddedURL = null;
    		if (youtubeUrl != null) 
    		{
    			try {
					embeddedURL = new URL("http://www.youtube.com/oembed?url=" +
					youtubeUrl + "&format=json");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
    		}
	        
    		jsonAsyncTask = new JsonAsync();
    		jsonAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,embeddedURL);
    		isTimeUp = false;
			setupLongTimeout(1000);
    		
    		while(Util.isEmptyString(jsonAsyncTask.title) && !isTimeUp)
    		{
    			//??? add time out?
//    			System.out.print("?");
    		}
    		isTimeUp = true;
	        return jsonAsyncTask.title;
	}


	// Get Http title
	static String httpTitle;
	public static void getHttpTitle(String httpUrl,Activity act,final EditText editText)
	{
		if(!isEmptyString(httpUrl))
		{
			try
			{
				WebView wv = new WebView(act);
				wv.loadUrl(httpUrl);
				isTimeUp = false;
				setupLongTimeout(1000);

				//Add for non-stop showing of full screen web view
				wv.setWebViewClient(new WebViewClient() {
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url)
					{
						view.loadUrl(url);
						return true;
					}
				});

				wv.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        httpTitle = title;
                        editText.setHint(Html.fromHtml("<small style=\"text-color: gray;\"><i>" +
                                httpTitle +
                                "</i></small>"));

                        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (hasFocus) {
                                    ((EditText) v).setText(httpTitle);
                                    ((EditText) v).setSelection(httpTitle.length());
                                }
                            }
                        });
                    }
                });
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	static boolean isTimeUp;
	static Timer longTimer;
	synchronized static void setupLongTimeout(long timeout)
	{
	  if(longTimer != null) 
	  {
	    longTimer.cancel();
	    longTimer = null;
	  }
	  
	  if(longTimer == null) 
	  {
	    longTimer = new Timer();
	    longTimer.schedule(new TimerTask() 
	    {
	      public void run() 
	      {
	        longTimer.cancel();
	        longTimer = null;
	        //do your stuff, i.e. finishing activity etc.
	        isTimeUp = true;
	      }
	    }, timeout /*in milliseconds*/);
	  }
	}

	// set Immersive Navigator
	public static void setImmersiveNavigator(Activity act)
	{
		System.out.println("Util / _setImmersiveNavigator");
		if (Build.VERSION.SDK_INT > 19)
		{
			View decorView = act.getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
					| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
					| View.SYSTEM_UI_FLAG_IMMERSIVE;
			decorView.setSystemUiVisibility(uiOptions);
		}
	}

	// set full screen
	public static void setFullScreen(Activity act)
	{
//		System.out.println("Util / _setFullScreen");
		Window win = act.getWindow();
		
		if (Build.VERSION.SDK_INT < 16) 
		{ 
			win.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
						 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} 
		else 
		{ 
		    View decorView = act.getWindow().getDecorView();
		    
		    // set full screen to hide the status bar.
		    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		    decorView.setSystemUiVisibility(uiOptions);
		}
	}
	
	// set NOT full screen
	public static void setNotFullScreen(Activity act)
	{
//		System.out.println("Util / _setNotFullScreen");
        Window win = act.getWindow();
        
		if (Build.VERSION.SDK_INT < 16) 
		{ 
			win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			win.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
						 WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} 
		else 
		{ 
		    View decorView = act.getWindow().getDecorView();
		    // show the status bar.
		    int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
		    decorView.setSystemUiVisibility(uiOptions);
		}
	}

	// Create assets file
	public static File createAssetsFile(Activity act, String fileName)
	{
        File file = null;
		AssetManager am = act.getAssets();
		InputStream inputStream = null;
		try {
			inputStream = am.open(fileName);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// main directory
		String dirString = Environment.getExternalStorageDirectory().toString() +
				"/" + Util.getStorageDirName(act);

		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();

		String filePath = dirString + "/" + fileName;

        if((inputStream != null)) {
            try {
                file = new File(filePath);
                OutputStream outputStream = new FileOutputStream(file);
                byte buffer[] = new byte[1024];
                int length = 0;

                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                //Logging exception
            }
        }
		return file;
	}

	// Get external storage path for different devices
	public static String getDefaultExternalStoragePath(String path)
	{
		if(path.contains("/storage/emulated/0"))
			path = path.replace("/storage/emulated/0", Environment.getExternalStorageDirectory().getAbsolutePath() );
		else if( path.contains("/mnt/internal_sd"))
			path = path.replace("/mnt/internal_sd", Environment.getExternalStorageDirectory().getAbsolutePath());
		return path;
	}
}
