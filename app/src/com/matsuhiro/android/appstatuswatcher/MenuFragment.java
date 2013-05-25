
package com.matsuhiro.android.appstatuswatcher;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MenuFragment extends ListFragment {

    private String TRACE_FILES_DIR;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TRACE_FILES_DIR = Environment.getExternalStorageDirectory().toString() + "/Android/data/"
                + this.getActivity().getPackageName().toString() + "/observational_result";

        return inflater.inflate(R.layout.list, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String[] screenNames = getResources().getStringArray(R.array.screen_names);
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, screenNames);
        setListAdapter(colorAdapter);
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        Fragment newContent = null;
        String[] screenNames = getResources().getStringArray(R.array.screen_names);
        switch (position) {
            case 0:
                newContent = new RunningTaskListFragment();
                break;
            case 1:
                newContent = new NotRunningTaskListFragment();
                break;
            case 2:
                newContent = DirectoryListFragment.newInstance(TRACE_FILES_DIR);
                break;
        }
        if (newContent != null)
            switchFragment(newContent, screenNames[position]);
    }

    // the meat of switching the above fragment
    private void switchFragment(Fragment fragment, String title) {
        if (getActivity() == null)
            return;

        if (getActivity() instanceof FragmentChangeActivity) {
            FragmentChangeActivity fca = (FragmentChangeActivity) getActivity();
            fca.setTitle(title);
            fca.switchContent(fragment);
        }
    }

}
