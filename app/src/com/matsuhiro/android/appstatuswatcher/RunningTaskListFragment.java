
package com.matsuhiro.android.appstatuswatcher;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;

public class RunningTaskListFragment extends Fragment {

    private LayoutInflater mInflater;

    private ListView mListView = null;

    private TaskListAdapter mTaskListAdapter = null;

    private FragmentActivity mActivity;

    List<AppInfo> mAppInfos = new ArrayList<AppInfo>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = getActivity();
        mInflater = inflater;
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.task_list_fragment, container,
                false);
        mListView = (ListView) rootView.findViewById(R.id.taskList);
        mTaskListAdapter = new TaskListAdapter();
        mListView.setAdapter(mTaskListAdapter);
        mListView.setFastScrollEnabled(true);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo info = mAppInfos.get(position);
                Toast.makeText(mActivity, "pid=" + info.mPid, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("packageName", info.mPackageName);
                bundle.putString("appName", info.mAppName);
                bundle.putInt("pid", info.mPid);
                bundle.putInt("uid", info.mUid);
                intent.putExtras(bundle);
                intent.setClass(mActivity, StatusWatcherService.class);
                mActivity.startService(intent);
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityManager activityManager = (ActivityManager) mActivity
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();

        PackageManager pm = mActivity.getPackageManager();
        List<ApplicationInfo> list = pm.getInstalledApplications(0);
        for (ApplicationInfo ai : list) {
            String pname = ai.packageName;
            if (pname.startsWith("com.android") && !pname.contains("browser")) {
                continue;
            }
            for (RunningAppProcessInfo processInfo : runningAppProcesses) {
                if (processInfo.processName.equals(pname)) {
                    AppInfo info = new AppInfo();
                    info.mAppName = pm.getApplicationLabel(ai).toString();
                    ;
                    try {
                        info.mIconDrawable = pm.getApplicationIcon(pname);
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    info.mPackageName = pname;
                    info.mPid = processInfo.pid;
                    info.mUid = processInfo.uid;
                    mAppInfos.add(info);
                }
            }
        }
        mTaskListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAppInfos.clear();
    }

    private class TaskListAdapter extends BaseAdapter {

        public int getCount() {
            if (mAppInfos != null) {
                return mAppInfos.size();
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
                convertView = mInflater.inflate(R.layout.task_list_item, null);
            }
            AppInfo info = mAppInfos.get(position);
            TextView taskNameTextView = (TextView) convertView.findViewById(R.id.task_name);
            taskNameTextView.setText(info.mAppName);

            TextView packNameTextView = (TextView) convertView.findViewById(R.id.task_package_name);
            packNameTextView.setText(info.mPackageName);

            ImageView icon = (ImageView) convertView.findViewById(R.id.task_icon);
            icon.setImageDrawable(info.mIconDrawable);
            return convertView;
        }

    }

    private class AppInfo {
        public String mPackageName;

        public String mAppName;

        public Drawable mIconDrawable;

        public int mPid;

        public int mUid;
    }
}
