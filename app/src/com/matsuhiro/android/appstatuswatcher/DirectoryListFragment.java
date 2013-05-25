
package com.matsuhiro.android.appstatuswatcher;

import com.matsuhiro.android.appstatuswatcher.chart.WatchedDataChart;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class DirectoryListFragment extends Fragment {

    private LayoutInflater mInflater;

    private ListView mListView = null;

    private DirectoryListAdapter mTaskListAdapter = null;

    private static final String KEY_DIRECTORY = "DIRECTORY";

    private File mDirectory;

    private FragmentActivity mActivity;

    private ArrayList<DirInfo> mDirInfos;

    private Stack<ArrayList<DirInfo>> mDirInfoStack;
    
    private PackageManager mPm;

    public static DirectoryListFragment newInstance(String directory) {
        DirectoryListFragment fragment = new DirectoryListFragment();
        Bundle args = new Bundle();
        args.putString(KEY_DIRECTORY, directory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = getActivity();
        mPm = mActivity.getPackageManager();
        String dir = getArguments().getString(KEY_DIRECTORY);
        mDirectory = new File(dir);
        mDirInfos = new ArrayList<DirInfo>();
        mDirInfoStack = new Stack<ArrayList<DirInfo>>();
        mInflater = inflater;
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.directory_list_fragment,
                container, false);
        mListView = (ListView) rootView.findViewById(R.id.directoryList);
        mTaskListAdapter = new DirectoryListAdapter();
        mListView.setAdapter(mTaskListAdapter);
        mListView.setFastScrollEnabled(true);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final DirInfo info = mDirInfos.get(position);
                if (info.mIsDirectory) {
                    mDirInfoStack.push(mDirInfos);
                    mDirInfos = new ArrayList<DirInfo>();
                    mDirectory = new File(mDirectory, info.mName);
                    LoadDirTask task = new LoadDirTask(mDirectory, mDirInfos, mListView);
                    task.execute("");
                } else {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
                    final CharSequence[] types = { "RAM", "CPU", "Traffic" };
                    alertDialogBuilder.setTitle("select type");
                    alertDialogBuilder.setItems(types, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            WatchedDataChart.TYPE type = WatchedDataChart.TYPE.RAM;
                            switch (which) {
                                case 0:
                                    type = WatchedDataChart.TYPE.RAM;
                                    break;
                                case 1:
                                    type = WatchedDataChart.TYPE.CPU;
                                    break;
                                case 2:
                                    type = WatchedDataChart.TYPE.TRAFFIC;
                                    break;
                            }
                            File f = new File(mDirectory, info.mName);
                            Intent intent = new WatchedDataChart(f.getAbsolutePath(),
                                    type).execute(mActivity);
                            mActivity.startActivity(intent);
                            
                        }
                    });
                    alertDialogBuilder.create().show();
                }
            }
        });
        LoadDirTask task = new LoadDirTask(mDirectory, mDirInfos, mListView);
        task.execute("");
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private static class DirInfo {
        String mName;

        boolean mIsDirectory;

        DirInfo(String name, boolean isDirectory) {
            mName = name;
            mIsDirectory = isDirectory;
        }
    }

    private static class LoadDirTask extends AsyncTask<String, Void, Void> {
        private File mDirectory;

        private ArrayList<DirInfo> mDirInfos;

        private ListView mList;

        public LoadDirTask(File dir, ArrayList<DirInfo> dirInfos, ListView list) {
            mDirectory = dir;
            mDirInfos = dirInfos;
            mList = list;
        }

        @Override
        protected Void doInBackground(String... params) {
            File[] files = mDirectory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    mDirInfos.add(new DirInfo(file.getName(), true));
                } else {
                    mDirInfos.add(new DirInfo(file.getName(), false));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mList.invalidateViews();
        }

    }

    private class DirectoryListAdapter extends BaseAdapter {

        public int getCount() {
            if (mDirInfos != null) {
                return mDirInfos.size();
            }
            return 0;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.directory_list_item, null);
            }
            DirInfo info = mDirInfos.get(position);

            TextView taskNameTextView = (TextView) convertView.findViewById(R.id.dir_name);
            taskNameTextView.setText(info.mName);

            ImageView icon = (ImageView) convertView.findViewById(R.id.dir_icon);
            try {
                if (info.mIsDirectory) {
                    icon.setImageDrawable(mPm.getApplicationIcon(info.mName));
                } else {
                    icon.setImageResource(android.R.color.transparent);
                }
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            return convertView;
        }

    }
}
