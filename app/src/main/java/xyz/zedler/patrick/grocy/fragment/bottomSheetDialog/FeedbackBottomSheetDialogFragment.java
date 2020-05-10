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
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;

import xyz.zedler.patrick.grocy.R;

public class FeedbackBottomSheetDialogFragment extends BottomSheetDialogFragment {

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

		view.findViewById(R.id.linear_rate).setOnClickListener(v -> {
			startAnimatedIcon(view, R.id.image_feedback_rate);
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

		TextInputLayout textInputLayoutFeedback = view.findViewById(R.id.text_input_feedback);
		EditText editText = textInputLayoutFeedback.getEditText();
		assert editText != null;
		editText.setOnFocusChangeListener((View v, boolean hasFocus) -> {
			if(hasFocus) startAnimatedIcon(view, R.id.image_feedback_box);
		});

		view.findViewById(R.id.button_feedback_send).setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(
					Uri.parse(
							"mailto:"
									+ getString(R.string.app_mail)
									+ "?subject=" + Uri.encode("Feedback@Grocy")
									+ "&body=" + Uri.encode(editText.getText().toString())
					)
			);
			startActivity(Intent.createChooser(intent, getString(R.string.action_send_feedback)));
			dismiss();
		});

		return view;
	}

	private void startAnimatedIcon(View view, @IdRes int viewId) {
		try {
			((Animatable) ((ImageView) view.findViewById(viewId)).getDrawable()).start();
		} catch (ClassCastException cla) {
			Log.e(TAG, "startAnimatedIcon(ImageView) requires AVD!");
		}
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
