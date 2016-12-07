package com.cw.litenote.config;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.R;
import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Import_fromSDCardFragment extends ListFragment
{
    private List<String> filePathArray = null;
    List<String> fileNames = null;
    public static View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.sd_file_list, container, false);

        View view = rootView.findViewById(R.id.view_back_btn_bg);
        view.setBackgroundColor(ColorSet.getBarColor(getActivity()));

        // back button
        Button backButton = (Button) rootView.findViewById(R.id.view_back);
        backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

        // do cancel
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        ((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(getActivity()));

        return rootView;
    }

    @Override
    public void onCreate(Bundle bundle) 
    {
        super.onCreate(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        String dirString = Environment.getExternalStorageDirectory().toString() +
                "/" +
                Util.getStorageDirName(getActivity());
        getFiles(new File(dirString).listFiles());
    }

    // on list item click
    @Override
    public void onListItemClick(ListView l, View v, int position, long rowId)
    {
        int selectedRow = (int)rowId;
        if(selectedRow == 0)
        {
        	//root
            getFiles(new File("/").listFiles());
        }
        else
        {
            final String filePath = filePathArray.get(selectedRow);
            final File file = new File(filePath);
            if(file.isDirectory())
            {
            	//directory
                getFiles(file.listFiles());
            }
            else
            {
            	// view the selected file's content
            	if( file.isFile() &&
                   (file.getName().contains("XML") ||
                    file.getName().contains("xml")     ))
            	{
		           	Intent i = new Intent(getActivity(), Import_selectedFileAct.class);
		           	i.putExtra("FILE_PATH", filePath);
		           	startActivity(i);
            	}
            	else
            	{
            		Toast.makeText(getActivity(),R.string.file_not_found,Toast.LENGTH_SHORT).show();
            		String dirString = Environment.getExternalStorageDirectory().toString() + 
					          "/" + 
					          Util.getStorageDirName(getActivity());
            		getFiles(new File(dirString).listFiles());            		
            	}
            }
        }
    }
    
    private void getFiles(File[] files)
    {
        if(files == null)
        {
        	Toast.makeText(getActivity(),R.string.toast_import_SDCard_no_file,Toast.LENGTH_SHORT).show();
        	getActivity().finish();
        }
        else
        {
//        	System.out.println("files length = " + files.length);
            filePathArray = new ArrayList<String>();
            fileNames = new ArrayList<String>();
            filePathArray.add("");
            fileNames.add("ROOT");
            
	        for(File file : files)
	        {
                filePathArray.add(file.getPath());
                fileNames.add(file.getName());
	        }
	        ArrayAdapter<String> fileList = new ArrayAdapter<String>(getActivity(),
	        														R.layout.sd_file_list_row,
	        														fileNames);
	        setListAdapter(fileList);
        }
    }
}