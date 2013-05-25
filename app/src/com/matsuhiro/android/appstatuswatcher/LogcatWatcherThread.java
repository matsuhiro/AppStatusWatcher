
package com.matsuhiro.android.appstatuswatcher;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.text.TextUtils;

public class LogcatWatcherThread extends Thread {

    private String mPackageName;

    private ActivityManager mActivityManager;

    private OnWatcherListener mListener;

    public interface OnWatcherListener {
        boolean onMatch(int pid, int uid);
    }

    public LogcatWatcherThread(String packageName, ActivityManager activityManager,
            OnWatcherListener listener) {
        super("LogcatWatcherThread");
        mPackageName = packageName;
        mActivityManager = activityManager;
        mListener = listener;
    }

    @Override
    public void run() {
        if (TextUtils.isEmpty(mPackageName) || mActivityManager == null || mListener == null) {
            return;
        }
        while (true) {
            List<RunningAppProcessInfo> runningAppProcesses = mActivityManager
                    .getRunningAppProcesses();
            for (RunningAppProcessInfo processInfo : runningAppProcesses) {
                if (processInfo.processName.equals(mPackageName)) {
                    mListener.onMatch(processInfo.pid, processInfo.uid);
                    return;
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
