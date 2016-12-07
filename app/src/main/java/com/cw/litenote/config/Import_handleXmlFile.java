package com.cw.litenote.config;

import java.io.FileInputStream;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.main.TabsHost;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.Util;

import android.content.Context;

class Import_handleXmlFile {

   private String pageName,title,body,picture,audio,link;
   private DB_folder mDb_folder;
   private DB_page mDb_page;

   private Context mContext;
   
   private FileInputStream fileInputStream = null;
   volatile boolean parsingComplete = true;
   String fileBody = "";
   private String strSplitter;
   private boolean mEnableInsertDB = true;
   
   Import_handleXmlFile(FileInputStream fileInputStream,Context context)
   {
	   mContext = context;
	   this.fileInputStream = fileInputStream;

       int folderTableId = Util.getPref_lastTimeView_folder_tableId(mContext);
	   mDb_folder = new DB_folder(MainAct.mAct, folderTableId);

       int pageTableId = Util.getPref_lastTimeView_page_tableId(mContext);
	   mDb_page = new DB_page(MainAct.mAct,pageTableId);

   }
   
   public String getTitle()
   {
      return title;
   }
   
   public String getBody()
   {
      return body;
   }
   
   public String getPicture()
   {
      return picture;
   }   
   
   public String getAudio()
   {
      return audio;
   }   
   
   public String getPage()
   {
      return pageName;
   }
   
   public void parseXMLAndInsertDB(XmlPullParser myParser) 
   {
	  
      int event;
      String text=null;
      try 
      {
         event = myParser.getEventType();
         while (event != XmlPullParser.END_DOCUMENT) 
         {
        	 String name = myParser.getName(); //name: null, link, item, title, description
//        	 System.out.println("Import_handleXmlFile / _parseXMLAndInsertDB / name = " + name);
        	 switch (event)
	         {
	            case XmlPullParser.START_TAG:
	            if(name.equals("note"))
                {
	            	strSplitter = "--- note ---";
                }	
		        break;
		        
	            case XmlPullParser.TEXT:
			       text = myParser.getText();
	            break;
	            
	            case XmlPullParser.END_TAG:
		           if(name.equals("page_name"))
		           {
	                  pageName = text.trim();
	                  
	                  if(mEnableInsertDB)
	                  {
			        	  int style = Util.getNewPageStyle(mContext);
			        	  
			        	  // style is not set in XML file, so insert default style instead
			        	  mDb_folder.insertPage(DB_folder.getFocusFolder_tableName(),
								  				pageName,
			        			  				TabsHost.getLastExist_TabId() + 1,
			        			  				style );
			        		
			        	  // insert table for new tab
						  mDb_folder.insertPageTable(mDb_folder,DB_folder.getFocusFolder_tableId(), TabsHost.getLastExist_TabId() + 1, false );
			        	  // update last tab Id after Insert
			        	  TabsHost.setLastExist_tabId(TabsHost.getLastExist_TabId() + 1);
	                  }
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "=== " + "Page:" + " " + pageName + " ===");
	               }
	               else if(name.equals("title"))
	               {
		              text = text.replace("[n]"," ");
		              text = text.replace("[s]"," ");
		              title = text.trim();
		           }
	               else if(name.equals("body"))
	               { 	
	            	  body = text.trim();
	               }
	               else if(name.equals("picture"))
	               { 	
	            	  picture = text.trim();
					  picture = Util.getDefaultExternalStoragePath(picture);
	               }		           
	               else if(name.equals("audio"))
	               { 	
	            	  audio = text.trim();
					  audio = Util.getDefaultExternalStoragePath(audio);
				   }
	               else if(name.equals("link"))
	               { 	
	            	  link = text.trim();
	            	  if(mEnableInsertDB)
	            	  {
		            	  DB_page.setFocusPage_tableId(TabsHost.getLastExist_TabId());
		            	  if(title.length() !=0 || body.length() != 0 || picture.length() !=0 || audio.length() !=0 ||link.length() !=0)
		            	  {
		            		  if((!Util.isEmptyString(picture)) || (!Util.isEmptyString(audio)))
		            		      mDb_page.insertNote(title, picture, audio, "", link, body,1, (long) 0); // add mark for media
		            		  else
		            			  mDb_page.insertNote(title, picture, audio, "", link, body,0, (long) 0);
		            	  }
	            	  }
		              fileBody = fileBody.concat(Util.NEW_LINE + strSplitter);
		              fileBody = fileBody.concat(Util.NEW_LINE + "title:" + " " + title);
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "body:" + " " + body);
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "picture:" + " " + picture);
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "audio:" + " " + audio);
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "link:" + " " + link);
	            	  fileBody = fileBody.concat(Util.NEW_LINE);
	               }	               
	               break;
	         }		 
        	 event = myParser.next();
         }
         
         parsingComplete = false;
      } 
      catch (Exception e) 
      {
         e.printStackTrace();
      }
   }

   void handleXML()
   {
	   Thread thread = new Thread(new Runnable()
	   {
		   @Override
		   public void run() 
		   {
		      try 
		      {
		         InputStream stream = fileInputStream;
		         XmlPullParser myParser = XmlPullParserFactory.newInstance().newPullParser();
		         myParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		         myParser.setInput(stream, null);
		         parseXMLAndInsertDB(myParser);
		         stream.close();
		      } 
		      catch (Exception e) 
		      { }
		  }
	  });
	  thread.start(); 
   }
   
   void enableInsertDB(boolean en)
   {
	   mEnableInsertDB = en;
   }
}