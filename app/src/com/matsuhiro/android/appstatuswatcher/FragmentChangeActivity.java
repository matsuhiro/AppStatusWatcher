
package com.matsuhiro.android.appstatuswatcher;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

public class FragmentChangeActivity extends BaseActivity {

    private Fragment mContent;

    public FragmentChangeActivity() {
        super(R.string.app_name);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set the Above View
        if (savedInstanceState != null)
            mContent = getSupportFragmentManager().getFragment(savedInstanceState, "mContent");
        if (mContent == null) {
            mContent = new RunningTaskListFragment();
            this.setTitle("Ruuning Apps");
        }

        // set the Above View
        setContentView(R.layout.content_frame);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, mContent)
                .commit();

        // set the Behind View
        setBehindContentView(R.layout.menu_frame);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.menu_frame, new MenuFragment()).commit();

        // customize the SlidingMenu
        getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, "mContent", mContent);
    }

    public void switchContent(Fragment fragment) {
        mContent = fragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment)
                .commit();
        getSlidingMenu().showContent();
    }

}
