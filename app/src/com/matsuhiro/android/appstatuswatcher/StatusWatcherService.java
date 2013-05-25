
package com.matsuhiro.android.appstatuswatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.matsuhiro.android.appstatuswatcher.widget.SystemOverlayLinearLayout;

public class StatusWatcherService extends Service {
    private static final String TAG = StatusWatcherService.class.getSimpleName();

    private static final int POLLING_INTERVAL = 200;

    private String TRACE_FILES_DIR;

    private LinearLayout mLinearLayout;

    private ConcurrentHashMap<Integer, Monitor> mMonitorMap = null;

    private boolean mCanDetectTraffic = false;

    final CountDownLatch signal = new CountDownLatch(1);

    @Override
    public IBinder onBind(Intent intent) {
        // nothing to do
        return null;
    }

    @Override
    public void onCreate() {
        try {
            TrafficStats.getUidRxBytes(0);
            mCanDetectTraffic = true;
        } catch (Throwable e) {
            mCanDetectTraffic = false;
        }
        TRACE_FILES_DIR = Environment.getExternalStorageDirectory().toString() + "/Android/data/"
                + this.getPackageName().toString() + "/observational_result";
        super.onCreate();
        mMonitorMap = new ConcurrentHashMap<Integer, Monitor>();
        final Handler handler = new Handler();
        final Timer timer = new Timer(false);
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    if (mMonitorMap == null || !(mMonitorMap.size() > 0)) {
                        try {
                            signal.await();
                        } catch (InterruptedException e) {
                            timer.cancel();
                            StatusWatcherService.this.stopSelf();
                            e.printStackTrace();
                        }
                    }
                    Collection<Integer> keys = mMonitorMap.keySet();
                    int[] pids = new int[keys.size()];
                    int i = 0;
                    for (Integer key : keys) {
                        pids[i] = key.intValue();
                        i++;
                    }
                    i = 0;
                    ActivityManager activityManager = (ActivityManager) StatusWatcherService.this
                            .getSystemService(ACTIVITY_SERVICE);
                    android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager
                            .getProcessMemoryInfo(pids);
                    for (android.os.Debug.MemoryInfo pidMemoryInfo : memoryInfoArray) {
                        int pid = pids[i];
                        Monitor monitor = mMonitorMap.get(pid);
                        if (monitor == null) {
                            continue;
                        }
                        final String pss = String.format(Locale.US, "%.3f",
                                (float) pidMemoryInfo.getTotalPss() / (float) 1024);
                        long procTick = getTick(pid);
                        long totalTick = getTick();
                        long delta = procTick - monitor.mProcTick;
                        long totalDelta = totalTick - monitor.mTotalTick;
                        monitor.mProcTick = procTick;
                        monitor.mTotalTick = totalTick;
                        final long cpu = delta * 100 / totalDelta;

                        long traffic = 0;
                        if (mCanDetectTraffic) {
                            traffic = TrafficStats.getUidRxBytes(monitor.mUid);
                        }
                        long deltaTrafifcByte = 0;
                        if (monitor.mTraffic > 0) {
                            deltaTrafifcByte = traffic - monitor.mTraffic;
                        }
                        monitor.mTraffic = traffic;
                        final long time = monitor.mTime;
                        monitor.mTime += POLLING_INTERVAL;
                        BufferedWriter bw = monitor.mTraceBF;
                        StringBuffer sb = new StringBuffer();
                        sb.append(time).append(",").append(pss).append(",").append(cpu).append(",")
                                .append(deltaTrafifcByte).append("\n");
                        try {
                            bw.write(sb.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        final TextView ramView = (TextView) monitor.mLayout
                                .findViewById(R.id.overlay_app_ram);
                        final TextView cpuView = (TextView) monitor.mLayout
                                .findViewById(R.id.overlay_app_cpu);
                        final TextView timeView = (TextView) monitor.mLayout
                                .findViewById(R.id.overlay_app_time);
                        handler.post(new Runnable() {
                            public void run() {
                                if (mMonitorMap == null || ramView == null || cpuView == null
                                        || timeView == null) {
                                    return;
                                }
                                ramView.setText("RAM " + pss + " MB");
                                cpuView.setText("CPU " + cpu + " %");
                                timeView.setText("time " + time + " msec");
                            }
                        });
                        i++;
                    }
                } catch (NullPointerException e) {
                    timer.cancel();
                }
            }
        }, 0, POLLING_INTERVAL);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLinearLayout = (LinearLayout) inflater.inflate(R.layout.status_watcher_overlay, null);
        SystemOverlayLinearLayout layout = (SystemOverlayLinearLayout) mLinearLayout
                .findViewById(R.id.overlay);
        layout.setOnClickItemListener(new SystemOverlayLinearLayout.OnClickItemListener() {
            public void onClick(SystemOverlayLinearLayout rootview) {
                rootview.removeWindow();
                Collection<Integer> keys = mMonitorMap.keySet();
                for (Integer key : keys) {
                    Monitor monitor = mMonitorMap.get(key);
                    if (rootview.equals(monitor.mLayout)) {
                        mMonitorMap.remove(key);
                        rootview.removeAllViews();
                        BufferedWriter bw = monitor.mTraceBF;
                        try {
                            bw.flush();
                            bw.close();
                            monitor.mFos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(getApplicationContext(),
                                "watched : " + monitor.mPackageName, Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            }
        });

        if (intent == null) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        Monitor monitor = new Monitor();
        monitor.mAppName = bundle.getString("appName");
        monitor.mPackageName = bundle.getString("packageName");
        monitor.mPid = bundle.getInt("pid");
        monitor.mUid = bundle.getInt("uid");
        monitor.mLayout = layout;
        if (mMonitorMap.containsKey(monitor.mPid)) {
            return;
        }
        if (!createTraceFile(monitor)) {
            return;
        }
        mMonitorMap.put(monitor.mPid, monitor);
        signal.countDown();

        TextView appNameView = (TextView) mLinearLayout.findViewById(R.id.overlay_app_name);
        appNameView.setText(monitor.mAppName);

        layout.addWindow();
        Log.d(TAG, "added : " + monitor.mAppName);
        Log.d(TAG, "added : " + monitor.mPackageName);
    }

    @Override
    public void onDestroy() {
        Collection<Integer> keys = mMonitorMap.keySet();
        for (Integer key : keys) {
            Monitor monitor = mMonitorMap.get(key);
            SystemOverlayLinearLayout layout = monitor.mLayout;
            layout.removeWindow();
            layout.removeAllViews();
            BufferedWriter bw = monitor.mTraceBF;
            try {
                bw.flush();
                bw.close();
                monitor.mFos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mMonitorMap = null;
    }

    @SuppressWarnings("resource")
    private boolean createTraceFile(final Monitor monitor) {
        String time = getTime();
        String filePath = TRACE_FILES_DIR + "/" + monitor.mPackageName + "/" + time + ".txt";
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, true);
            monitor.mFos = fos;
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write("Time(msec),RAM(MB),CPU(%),Traffic(byte)\n");
            monitor.mTraceBF = bw;
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getTime() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new Date());
        return utcTime;
    }

    private class Monitor {
        public String mPackageName;

        public String mAppName;

        public int mPid;

        public int mUid;

        public SystemOverlayLinearLayout mLayout;

        public long mProcTick;

        public long mTotalTick;

        public long mTraffic = 0;

        public long mTime = 0;

        public FileOutputStream mFos;

        public BufferedWriter mTraceBF;
    }

    static private long getTick(int pid) {
        try {
            String filename = "/proc/" + pid + "/stat";
            String stat = load(filename);
            // Log.i(TAG, "stat="+stat);
            String[] split = stat.split(" +");
            return Integer.parseInt(split[13]) + Long.parseLong(split[14]);
        } catch (Exception e) {
            return 0;
        }
    }

    static private long getTick() {
        try {
            String filename = "/proc/stat";
            String stat = load(filename);
            String[] split = stat.split(" +");
            long time = 0;
            for (int i = 1; i < split.length; i++)
                time += Long.parseLong(split[i]);
            return time;
        } catch (Exception e) {
            return 0;
        }
    }

    static private String load(String filename) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String str = reader.readLine();
            if (str == null)
                return null;
            return str;
        } catch (Exception e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
