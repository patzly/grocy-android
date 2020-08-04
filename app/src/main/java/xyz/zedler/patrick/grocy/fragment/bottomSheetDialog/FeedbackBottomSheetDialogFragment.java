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

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class FeedbackBottomSheetDialogFragment extends CustomBottomSheetDialogFragment {

	private final static String TAG = "FeedbackBottomSheet";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
	}

	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState
	) {
		View view = inflater.inflate(
				R.layout.fragment_bottomsheet_feedback,
				container,
				false
		);

		Activity activity = getActivity();
		assert activity != null;

		view.findViewById(R.id.linear_feedback_rate).setOnClickListener(v -> {
			IconUtil.start(view, R.id.image_feedback_rate);
			Uri uri = Uri.parse(
					"market://details?id=" + activity.getApplicationContext().getPackageName()
			);
			Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
			goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
					Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
					Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
					Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			new Handler().postDelayed(() -> {
				try {
					startActivity(goToMarket);
				} catch (ActivityNotFoundException e) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
							"http://play.google.com/store/apps/details?id="
									+ activity.getApplicationContext().getPackageName()
					)));
				}
				dismiss();
			}, 300);
		});

		view.findViewById(R.id.linear_feedback_issue).setOnClickListener(v -> {
			Intent intent = new Intent(
					Intent.ACTION_VIEW,
					Uri.parse(activity.getString(R.string.url_github_new_issue))
			);
			startActivity(intent);
			dismiss();
		});

		view.findViewById(R.id.linear_feedback_email).setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(
					Uri.parse(
							"mailto:"
									+ getString(R.string.app_mail)
									+ "?subject=" + Uri.encode("Feedback@Grocy")
					)
			);
			startActivity(Intent.createChooser(intent, getString(R.string.action_send_feedback)));
			dismiss();
		});

		return view;
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
