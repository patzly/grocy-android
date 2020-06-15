package xyz.zedler.patrick.grocy;

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

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import xyz.zedler.patrick.grocy.behavior.AppBarScrollBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityLogBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class LogActivity extends AppCompatActivity {

	private final static boolean DEBUG = false;
	private final static String TAG = LogActivity.class.getSimpleName();

	private ActivityLogBinding binding;
	private ClickUtil clickUtil = new ClickUtil();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_log);

		findViewById(R.id.frame_log_close).setOnClickListener(v -> {
			if(clickUtil.isDisabled()) return;
			finish();
		});

		Toolbar toolbar = findViewById(R.id.toolbar_log);
		toolbar.setOnMenuItemClickListener((MenuItem item) -> {
			if(clickUtil.isDisabled()) return false;
			if (item.getItemId() == R.id.action_feedback) {
				IconUtil.start(item);
				BottomSheetDialogFragment sheet = new FeedbackBottomSheetDialogFragment();
				getSupportFragmentManager()
						.beginTransaction()
						.add(sheet, sheet.toString())
						.commit();
			}
			return true;
		});

		(new AppBarScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_log,
				R.id.linear_log_app_bar,
				R.id.scroll_log,
				true
		);

		TextView textView = findViewById(R.id.text_log_logs);
		textView.setText(getLogs());
	}

	private String getLogs() {
		StringBuilder log = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec("logcat *:I -d -t 150");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream())
			);
			String line;
			while ((line = bufferedReader.readLine()) != null) log.append(line).append('\n');
			log.deleteCharAt(log.length() - 1);
		} catch (IOException e) {}
		return log.toString();
	}
}
