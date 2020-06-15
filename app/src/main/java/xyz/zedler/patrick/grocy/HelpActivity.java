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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import xyz.zedler.patrick.grocy.behavior.AppBarScrollBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityHelpBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;
import xyz.zedler.patrick.grocy.view.ExpandableCard;

public class HelpActivity extends AppCompatActivity {

	private final static boolean DEBUG = false;
	private final static String TAG = HelpActivity.class.getSimpleName();

	private ActivityHelpBinding binding;
	private ClickUtil clickUtil = new ClickUtil();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		AppCompatDelegate.setDefaultNightMode(
				sharedPrefs.getBoolean(Constants.PREF.DARK_MODE,false)
						? AppCompatDelegate.MODE_NIGHT_YES
						: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		);
		setContentView(R.layout.activity_help);

		findViewById(R.id.frame_help_close).setOnClickListener(v -> {
			if(clickUtil.isDisabled()) return;
			finish();
		});

		Toolbar toolbar = findViewById(R.id.toolbar_help);
		toolbar.setOnMenuItemClickListener((MenuItem item) -> {
			if(clickUtil.isDisabled()) return false;
			switch (item.getItemId()) {
				case R.id.action_feedback:
					IconUtil.start(item);
					BottomSheetDialogFragment sheet = new FeedbackBottomSheetDialogFragment();
					getSupportFragmentManager()
							.beginTransaction()
							.add(sheet, sheet.toString())
							.commit();
					break;
				case R.id.action_about:
					IconUtil.start(item);
					startActivity(new Intent(this, AboutActivity.class));
					break;
			}
			return true;
		});

		(new AppBarScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_help,
				R.id.linear_help_app_bar,
				R.id.scroll_help,
				true
		);

		LinearLayout container = findViewById(R.id.linear_help_container);

		String[] sections = getHelpSections();

		for(String section : sections) {
			if(section.startsWith("#")) {
				String[] h = section.split(" ");
				container.addView(newTitle(section.substring(h[0].length() + 1)));
			} else if(!section.startsWith("§")) {
				container.addView(newCard(section));
			}
		}
	}

	private String[] getHelpSections() {
		StringBuilder text = new StringBuilder();
		try {
			InputStream inputStream = getAssets().open("HELP.txt");
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			for(String line; (line = bufferedReader.readLine()) != null;) {
				text.append(line).append('\n');
			}
			text.deleteCharAt(text.length() - 1);
			inputStream.close();
		} catch (Exception e) {
			if(DEBUG) Log.e(TAG, "getHelpSections: " + e);
		}
		return text.toString().split("\n–\n");
	}

	private TextView newTitle(String title) {
		TextView textView = new TextView(this);
		textView.setText(title);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
		textView.setTypeface(ResourcesCompat.getFont(
				this, R.font.roboto_mono_medium
		));
		textView.setTextColor(ContextCompat.getColor(this, R.color.on_background));
		textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		layoutParams.setMargins(
				0,
				UnitUtil.getDp(this, 8),
				0,
				UnitUtil.getDp(this, 16)
		);
		textView.setLayoutParams(layoutParams);
		return textView;
	}

	private ExpandableCard newCard(String text) {
		ExpandableCard card = new ExpandableCard(this);
		card.setText(text);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		layoutParams.setMargins(
				0, 0, 0,
				UnitUtil.getDp(this, 8)
		);
		card.setLayoutParams(layoutParams);
		return card;
	}
}
