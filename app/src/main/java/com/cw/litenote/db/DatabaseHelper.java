package com.cw.litenote.db;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.preference.Define;

// Data Base Helper 
class DatabaseHelper extends SQLiteOpenHelper
{  
    static final String DB_NAME = "litenote.db";
    private static int DB_VERSION = 1;
    
    DatabaseHelper(Context context)
    {  
        super(context, DB_NAME , null, DB_VERSION);
    }

    @Override
    //Called when the database is created ONLY for the first time.
    public void onCreate(SQLiteDatabase sqlDb)
    {   
    	String tableCreated;
    	String DB_CREATE;
    	
    	// WritableDatabase(i.e. sqlDb) is created
    	DB_drawer.mSqlDb = sqlDb;
		DB_folder.mSqlDb = sqlDb; // add for DB_drawer.insertFolderTable below
		DB_page.mSqlDb = sqlDb; // add for DB_folder.insertPageTable below

    	System.out.println("DatabaseHelper / _onCreate");

		// Create Drawer table
		tableCreated = DB_drawer.DB_DRAWER_TABLE_NAME;
		DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
				DB_drawer.KEY_FOLDER_ID + " INTEGER PRIMARY KEY," +
				DB_drawer.KEY_FOLDER_TABLE_ID + " INTEGER," +
				DB_drawer.KEY_FOLDER_TITLE + " TEXT," +
				DB_drawer.KEY_FOLDER_CREATED + " INTEGER);";
		sqlDb.execSQL(DB_CREATE);

		// Create Folder tables
		for(int i = 1; i<= Define.ORIGIN_FOLDERS_COUNT; i++)
		{
			DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
			db_drawer.insertFolderTable(db_drawer, i, true);
		}

		// Create Page tables
    	if(!Define.HAS_PREFERENCE)
    	{
	    	for(int i = 1; i<= Define.ORIGIN_FOLDERS_COUNT; i++)
	    	{
	        	System.out.println("DatabaseHelper / _onCreate / will insert folder table " + i);
	        	for(int j = 1; j<= Define.ORIGIN_PAGES_COUNT; j++)
	        	{
	            	System.out.println("DatabaseHelper / _onCreate / will insert note table " + j);
					DB_folder db_folder = new DB_folder(MainAct.mAct,i);
					db_folder.insertPageTable(db_folder, i, j, true);
	        	}
	    	}
    	}

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    { //??? how to upgrade?
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onUpgrade / drop DB / DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }
    
    @Override
    public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion)
    { 
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onDowngrade / drop DB / DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }
}
