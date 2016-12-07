package com.cw.litenote.main;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

/**
 * Created by CW on 2016/8/23.
 */
public class Folder
{
    public DragSortListView listView;
    Adapter adapter;
    DragSortController controller;
    FragmentActivity act;

    Folder(FragmentActivity act)
    {
        this.act = act;
        listView = (DragSortListView) act.findViewById(R.id.left_drawer);
    }

    // initialize folder list view
    void initFolder()
    {

        // set Folder title
        if(MainAct.mDb_drawer.getFoldersCount() == 0)
        {
            for(int i = 0; i< Define.ORIGIN_FOLDERS_COUNT; i++)
            {
                // insert folder
                System.out.println("Folder/ _initFolder / insert folder "+ i) ;
                String folderTitle = Define.getFolderTitle(act,i);
                MainAct.mFolderTitles.add(folderTitle);
                MainAct.mDb_drawer.insertFolder(i+1, folderTitle );
            }
        }
        else
        {
            for(int i = 0; i< MainAct.mDb_drawer.getFoldersCount(); i++)
            {
                MainAct.mFolderTitles.add(""); // init only
                MainAct.mFolderTitles.set(i, MainAct.mDb_drawer.getFolderTitle(i));
            }
        }

        // check DB
//        DB_drawer.listFolders();

        // set adapter
        MainAct.mDb_drawer.open();
        Cursor cursor = DB_drawer.mCursor_folder;

        String[] from = new String[] { DB_drawer.KEY_FOLDER_TITLE};
        int[] to = new int[] { R.id.folderText};

        adapter = new Adapter(
                act,
                R.layout.folder_row,
                cursor,
                from,
                to,
                0
        );

        MainAct.mDb_drawer.close();

        listView.setAdapter(adapter);

        // set up click listener
        MainUi.addFolderListeners();//??? move to resume?
        listView.setOnItemClickListener(MainUi.folderClick);
        // set up long click listener
        listView.setOnItemLongClickListener(MainUi.folderLongClick);

        controller = buildController(listView);
        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);

        // init folder dragger
        MainAct.mPref_show_note_attribute = act.getSharedPreferences("show_note_attribute", 0);
        if(MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                .equalsIgnoreCase("yes"))
            listView.setDragEnabled(true);
        else
            listView.setDragEnabled(false);

        listView.setDragListener(onDrag);
        listView.setDropListener(onDrop);
    }

    // list view listener: on drag
    DragSortListView.DragListener onDrag = new DragSortListView.DragListener()
    {
        @Override
        public void drag(int startPosition, int endPosition) {
        }
    };

    // list view listener: on drop
    DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
    {
        @Override
        public void drop(int startPosition, int endPosition) {
            //reorder data base storage
            int loop = Math.abs(startPosition-endPosition);
            for(int i=0;i< loop;i++)
            {
                MainUi.swapFolderRows(startPosition,endPosition);
                if((startPosition-endPosition) >0)
                    endPosition++;
                else
                    endPosition--;
            }

            DB_drawer db_drawer = new DB_drawer(act);
            // update audio playing drawer index
            int drawerCount = db_drawer.getFoldersCount();
            for(int i=0;i<drawerCount;i++)
            {
                if(db_drawer.getFolderTableId(i) == MainAct.mPlaying_folderTableId)
                    MainAct.mPlaying_folderPos = i;
            }
            adapter.notifyDataSetChanged();
            MainUi.updateFocus_folderPosition();
        }
    };

    // List all folder tables
    static void listAllFolderTables(FragmentActivity act)
    {
        // list all folder tables
        int foldersCount = MainAct.mDb_drawer.getFoldersCount();
        for(int folderPos=0; folderPos<foldersCount; folderPos++)
        {
            String folderTitle = MainAct.mDb_drawer.getFolderTitle(folderPos);
            MainAct.mFocus_folderPos = folderPos;

            // list all folder tables
            int folderTableId = MainAct.mDb_drawer.getFolderTableId(folderPos);
            System.out.println("--- folder table Id = " + folderTableId +
                               ", folder title = " + folderTitle);

            DB_folder db_folder = new DB_folder(act,folderTableId);

            int pagesCount = db_folder.getPagesCount(true);

            for(int pagePos=0; pagePos<pagesCount; pagePos++)
            {
                TabsHost.mNow_pageId = pagePos;
                int pageId = db_folder.getPageId(pagePos, true);
                int pageTableId = db_folder.getPageTableId(pagePos, true);
                String pageTitle = db_folder.getPageTitle(pagePos, true);
                System.out.println("   --- page Id = " + pageId);
                System.out.println("   --- page table Id = " + pageTableId);
                System.out.println("   --- page title = " + pageTitle);

                MainAct.mLastOkTabId = pageId;

                try {
                    DB_page db_page = new DB_page(act,pageTableId);
                    db_page.open();
                    db_page.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    private static DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DOWN); // click
        controller.setDragHandleId(R.id.folder_dragger);// handler
        controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging

        return controller;
    }

    static class Adapter extends SimpleDragSortCursorAdapter
    {
        Adapter(Context context, int layout, Cursor c,
                       String[] from, int[] to, int flags)
        {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public int getCount() {
            DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
            int count = db_drawer.getFoldersCount();
            return count;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder viewHolder; // holds references to current item's GUI

            // if convertView is null, inflate GUI and create ViewHolder;
            // otherwise, get existing ViewHolder
            if (convertView == null)
            {
                convertView = MainAct.mAct.getLayoutInflater().inflate(R.layout.folder_row, parent, false);

                // set up ViewHolder for this ListView item
                viewHolder = new ViewHolder();
                viewHolder.drawerTitle = (TextView) convertView.findViewById(R.id.folderText);
                viewHolder.imageDragger = (ImageView) convertView.findViewById(R.id.folder_dragger);
                convertView.setTag(viewHolder); // store as View's tag
            }
            else // get the ViewHolder from the convertView's tag
                viewHolder = (ViewHolder) convertView.getTag();

            // set highlight of selected drawer
            if((AudioPlayer.mMediaPlayer != null) &&
                  (MainAct.mPlaying_folderPos == position) )
                viewHolder.drawerTitle.setTextColor(ColorSet.getHighlightColor(MainAct.mAct));
            else
                viewHolder.drawerTitle.setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));

            DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
            viewHolder.drawerTitle.setText(db_drawer.getFolderTitle(position));

              // dragger
              if(MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                                                         .equalsIgnoreCase("yes"))
                  viewHolder.imageDragger.setVisibility(View.VISIBLE);
              else
                  viewHolder.imageDragger.setVisibility(View.GONE);

            return convertView;
        }
    }

    private static class ViewHolder
    {
        TextView drawerTitle; // refers to ListView item's ImageView
        ImageView imageDragger;
    }

    /**
     * Listeners for folder ListView
     *
     */
    // click
    static class FolderListener_click implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            System.out.println("Folder / _onItemClickListener");
            MainAct.mFocus_folderPos = position;

            DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
            Util.setPref_lastTimeView_folder_tableId(MainAct.mAct,db_drawer.getFolderTableId(position) );

            MainUi.selectFolder(position);
            MainAct.setFolderTitle(MainAct.mFolderTitle);
        }
    }

    // long click
    static class FolderListener_longClick implements DragSortListView.OnItemLongClickListener
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
        {
            MainUi.editFolder(position);
            return true;
        }
    }
}