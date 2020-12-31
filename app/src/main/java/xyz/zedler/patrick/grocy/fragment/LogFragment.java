package xyz.zedler.patrick.grocy.fragment;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentLogBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class LogFragment extends BaseFragment {

    private final static String TAG = LogFragment.class.getSimpleName();

    private FragmentLogBinding binding;
    private MainActivity activity;
    private boolean showInfo;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        showInfo = sharedPrefs.getBoolean(Constants.PREF.SHOW_INFO_LOGS, false);

        if(activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
            activity.showHideDemoIndicator(this, true);
            activity.getScrollBehavior().setUpScroll(R.id.scroll_log);
            activity.getScrollBehavior().setHideOnScroll(false);
            activity.updateBottomAppBar(
                    Constants.FAB.POSITION.GONE,
                    R.menu.menu_log,
                    true,
                    this::setUpBottomMenu
            );
        }

        setLog(showInfo);
    }

    private void setLog(boolean showInfo) {
        StringBuilder log = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(getLogcatCommand(showInfo));
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line).append('\n');
            }
            log.deleteCharAt(log.length() - 1);
        } catch (IOException ignored) {}
        binding.textLog.setText(log.toString());
    }

    private String getLogcatCommand(boolean showInfo) {
        return "logcat -d " +
                (showInfo ? "*:I " : "*:E ") +
                (showInfo ? "-t 150 " : "-t 300 ") +
                "AdrenoGLES:S " +
                "ActivityThread:S " +
                "RenderThread:S " +
                "Gralloc3:S " +
                "OpenGLRenderer:S " +
                "Choreographer:S ";
    }

    public void setUpBottomMenu() {
        MenuItem menuItemRefresh, menuItemFeedback;
        menuItemRefresh = activity.getBottomMenu().findItem(R.id.action_refresh);
        menuItemFeedback = activity.getBottomMenu().findItem(R.id.action_feedback);
        if(menuItemRefresh == null || menuItemFeedback == null) return;

        menuItemRefresh.setOnMenuItemClickListener(item -> {
            setLog(showInfo);
            return true;
        });
        menuItemFeedback.setOnMenuItemClickListener(item -> {
            IconUtil.start(menuItemFeedback);
            activity.showBottomSheet(new FeedbackBottomSheet(), null);
            return true;
        });
    }
}
