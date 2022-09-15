/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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

public class LogFragment extends BaseFragment {

  private final static String TAG = LogFragment.class.getSimpleName();

  private FragmentLogBinding binding;
  private MainActivity activity;
  private boolean showInfo = false;

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
    binding.setActivity(activity);

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      activity.getScrollBehavior().setUpScroll(R.id.scroll_log);
      activity.getScrollBehavior().setHideOnScroll(false);
      activity.updateBottomAppBar(false, R.menu.menu_log, this::onMenuItemClick);
    }

    String server = PreferenceManager.getDefaultSharedPreferences(requireContext())
        .getString(Constants.PREF.SERVER_URL, null);
    if (server == null || server.isEmpty()) {
      showInfo = true;
    } else if (savedInstanceState != null && savedInstanceState.containsKey("show_info")) {
      showInfo = savedInstanceState.getBoolean("show_info");
    }

    new Handler().postDelayed(
        () -> new loadAsyncTask(
            getLogcatCommand(),
            log -> binding.textLog.setText(log)
        ).execute(),
        300
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putBoolean("show_info", showInfo);
    super.onSaveInstanceState(outState);
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, String> {

    private final String logcatCommand;
    private final LogLoadedListener listener;

    loadAsyncTask(String logcatCommand, LogLoadedListener listener) {
      this.logcatCommand = logcatCommand;
      this.listener = listener;
    }

    @Override
    protected final String doInBackground(Void... params) {
      StringBuilder log = new StringBuilder();
      try {
        Process process = Runtime.getRuntime().exec(logcatCommand);
        BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          log.append(line).append('\n');
        }
        if (log.length() > 0) log.deleteCharAt(log.length() - 1);
      } catch (IOException ignored) {
      }
      return log.toString();
    }

    @Override
    protected void onPostExecute(String log) {
      if (listener != null) {
        listener.onLogLoaded(log);
      }
    }

    private interface LogLoadedListener {

      void onLogLoaded(String log);
    }
  }

  private String getLogcatCommand() {
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

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_refresh) {
      new loadAsyncTask(getLogcatCommand(), log -> binding.textLog.setText(log)).execute();
      return true;
    } else if (item.getItemId() == R.id.action_feedback) {
      activity.showBottomSheet(new FeedbackBottomSheet(), null);
      return true;
    } else if (item.getItemId() == R.id.action_log_level) {
      if (showInfo) {
        item.getSubMenu().findItem(R.id.action_info_logs).setChecked(true);
      } else {
        item.getSubMenu().findItem(R.id.action_error_logs).setChecked(true);
      }
      return true;
    } else if (item.getItemId() == R.id.action_error_logs) {
      showInfo = false;
      new loadAsyncTask(getLogcatCommand(), log -> binding.textLog.setText(log)).execute();
      return true;
    } else if (item.getItemId() == R.id.action_info_logs) {
      showInfo = true;
      new loadAsyncTask(getLogcatCommand(), log -> binding.textLog.setText(log)).execute();
      return true;
    }
    return false;
  }
}
