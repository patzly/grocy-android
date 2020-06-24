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
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.adapter.HelpAdapter;
import xyz.zedler.patrick.grocy.databinding.ActivityHelpBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class HelpActivity extends AppCompatActivity {

	private final static boolean DEBUG = false;
	private final static String TAG = HelpActivity.class.getSimpleName();

	private ClickUtil clickUtil = new ClickUtil();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActivityHelpBinding binding = ActivityHelpBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		AppCompatDelegate.setDefaultNightMode(
				sharedPrefs.getBoolean(Constants.PREF.DARK_MODE,false)
						? AppCompatDelegate.MODE_NIGHT_YES
						: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		);

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

		/*(new AppBarScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_help,
				R.id.linear_help_app_bar,
				R.id.scroll_help,
				true
		);*/

		binding.recyclerHelp.setLayoutManager(new LinearLayoutManager(this));
		binding.recyclerHelp.setItemAnimator(new DefaultItemAnimator());
		binding.recyclerHelp.setHasFixedSize(true);

		String[] sections = getHelpSections();

		ArrayList<HelpAdapter.HelpSection> helpSections = new ArrayList<>();
		for(String section : sections) {
			String[] sectionParts = null;
			String sectionId = null;
			String header = null;
			String body = null;
			if(section.startsWith("id=")) {
				sectionParts = section.split("\n", 3);
				sectionId = sectionParts[0].substring(3);
				header = sectionParts[1].substring(1).trim();
				body = sectionParts[2];
			} else if(section.startsWith("#")) {
				sectionParts = section.split("\n", 2);
				header = sectionParts[0].substring(1).trim();
				body = sectionParts[1];
			}
			if(sectionParts == null) continue;


			helpSections.add(new HelpAdapter.HelpSection(header, body, sectionId));
		}

		HashMap<String, String> sectionPositions = new HashMap<>();
		for(int pos=0; pos<helpSections.size(); pos++) {
			HelpAdapter.HelpSection helpSection = helpSections.get(pos);
			if(helpSection.getId() == null) continue;
			sectionPositions.put(helpSection.getId(), String.valueOf(pos));
		}

		HelpAdapter adapter = new HelpAdapter(helpSections);
		binding.recyclerHelp.setAdapter(adapter);

		// open section on startup if ID is given
		Intent intent = getIntent();
		String sectionId = null;
		if(intent != null) sectionId = intent.getStringExtra(Constants.ARGUMENT.SELECTED_ID);
		if(sectionId != null && sectionPositions.containsKey(sectionId)) {
			String position;
			position = sectionPositions.get(sectionId);
			if(position != null) {
				adapter.expandItem(Integer.parseInt(position));
				binding.recyclerHelp.scrollToPosition(Integer.parseInt(position));
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
		return text.toString().split("\n[-][-][-]+\n");
	}
}
