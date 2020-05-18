package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class TextBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static boolean DEBUG = false;
	private final static String TAG = "TextBottomSheet";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState
	) {
		View view = inflater.inflate(
				R.layout.fragment_bottomsheet_text,
				container,
				false
		);

		Context context = getContext();
		Bundle bundle = getArguments();
		assert context != null && bundle != null;

		String file = bundle.getString(Constants.BOTTOM_SHEET_TEXT.FILE) + ".txt";
		String fileLocalized = bundle.getString(Constants.BOTTOM_SHEET_TEXT.FILE)
				+ "-" + Locale.getDefault().getLanguage()
				+ ".txt";
		if(readFromFile(context, fileLocalized) != null) file = fileLocalized;

		((TextView) view.findViewById(R.id.text_text_title)).setText(
				bundle.getString(Constants.BOTTOM_SHEET_TEXT.TITLE)
		);

		FrameLayout frameLayoutLink = view.findViewById(R.id.frame_text_open_link);
		String link = bundle.getString(Constants.BOTTOM_SHEET_TEXT.LINK);
		if (link != null) {
			frameLayoutLink.setOnClickListener(v -> {
				IconUtil.start(view, R.id.image_text_open_link);
				new Handler().postDelayed(
						() -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))),
						500
				);
			});
		} else {
			frameLayoutLink.setVisibility(View.GONE);
		}

		((TextView) view.findViewById(R.id.text_text)).setText(readFromFile(context, file));
		if(bundle.getBoolean(Constants.BOTTOM_SHEET_TEXT.BIG_TEXT, false)) {
			((TextView) view.findViewById(R.id.text_text)).setTextSize(
					TypedValue.COMPLEX_UNIT_SP,
					14.5f
			);
		}

		return view;
	}

	private String readFromFile(Context context, String file) {
		StringBuilder text = new StringBuilder();
		try {
			InputStream inputStream = context.getAssets().open(file);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			for(String line; (line = bufferedReader.readLine()) != null;) {
				text.append(line).append('\n');
			}
			text.deleteCharAt(text.length() - 1);
			inputStream.close();
		} catch (FileNotFoundException e) {
			if(DEBUG) Log.e(TAG, "readFromFile: \"" + file + "\" not found!");
			return null;
		} catch (Exception e) {
			if(DEBUG) Log.e(TAG, "readFromFile: " + e.toString());
		}
		return text.toString();
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
