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
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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

		ArrayList<HelpAdapter.HelpSection> helpSections = getHelpSections();

		HashMap<Integer, String> sectionPositions = new HashMap<>();
		for(int pos=0; pos<helpSections.size(); pos++) {
			HelpAdapter.HelpSection helpSection = helpSections.get(pos);
			sectionPositions.put(helpSection.getId(), String.valueOf(pos));
		}

		HelpAdapter adapter = new HelpAdapter(helpSections);
		binding.recyclerHelp.setAdapter(adapter);

		// open section on startup if ID is given
		Intent intent = getIntent();
		String sectionId = null;
		if(intent != null) sectionId = intent.getStringExtra(Constants.ARGUMENT.SELECTED_ID);
		if(sectionId != null && sectionPositions.containsKey(Integer.parseInt(sectionId))) {
			String position;
			position = sectionPositions.get(Integer.parseInt(sectionId));
			if(position != null) {
				adapter.expandItem(Integer.parseInt(position));
				binding.recyclerHelp.scrollToPosition(Integer.parseInt(position));
			}
		}
	}

	private ArrayList<HelpAdapter.HelpSection> getHelpSections() {
		ArrayList<HelpAdapter.HelpSection> helpSections = new ArrayList<>();
		for(int i=0; i<=5; i++) {
			int headerRes = 0;
			int bodyRes = 0;
			switch(i) {
				case 0:
					headerRes = R.string.help_general_header;
					bodyRes = R.string.help_general_body;
					break;
				case 1:
					headerRes = R.string.help_key_invalid_header;
					bodyRes = R.string.help_key_invalid_body;
					break;
				case 2:
					headerRes = R.string.help_homeassistant_header;
					bodyRes = R.string.help_homeassistant_body;
					break;
				case 3:
					headerRes = R.string.help_consume_header;
					bodyRes = R.string.help_consume_body;
					break;
				case 4:
					headerRes = R.string.help_open_header;
					bodyRes = R.string.help_open_body;
					break;
				case 5:
					headerRes = R.string.help_other_question_header;
					bodyRes = R.string.help_other_question_body;
					break;
			}
			String header = getString(headerRes);
			String body = getString(bodyRes).replaceAll("\n[ ]+", "\n");
			helpSections.add(new HelpAdapter.HelpSection(header, body, headerRes));
		}
		return helpSections;
	}
}
