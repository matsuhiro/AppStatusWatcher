
package com.matsuhiro.android.appstatuswatcher.widget;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class SystemOverlayLinearLayout extends LinearLayout {
    private static final float THRESHOLD = 50;

    private WindowManager.LayoutParams mWMParams;

    private Context mContext;

    private float mFirstX, mFirstY;

    private OnClickItemListener mListener = null;

    private boolean mIsClicked;

    private int mOffsetX, mOffsetY;

    public SystemOverlayLinearLayout(Context context) {
        super(context);
        mContext = context;
    }

    public SystemOverlayLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void addWindow() {
        this.setClickable(true);
        this.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mIsClicked != false && mListener != null) {
                    mListener.onClick(SystemOverlayLinearLayout.this);
                }
            }

        });
        mWMParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(this, mWMParams);
    }

    public void removeWindow() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.removeView(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mOffsetX = x;
            mOffsetY = y;

            mFirstX = x;
            mFirstY = y;
            mIsClicked = true;
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            float abs = (x - mFirstX) * (x - mFirstX) + (y - mFirstY) * (y - mFirstY);
            if (abs > THRESHOLD) {
                mIsClicked = false;
            }
            int diffX = mOffsetX - x;
            int diffY = mOffsetY - y;

            mWMParams.x -= diffX;
            mWMParams.y -= diffY;

            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(getRootView(), mWMParams);
            mOffsetX = x;
            mOffsetY = y;
        }
        return super.dispatchTouchEvent(ev);
    }

    public interface OnClickItemListener {
        public void onClick(SystemOverlayLinearLayout rootview);
    }

    public void setOnClickItemListener(OnClickItemListener listener) {
        mListener = listener;
    }
}
