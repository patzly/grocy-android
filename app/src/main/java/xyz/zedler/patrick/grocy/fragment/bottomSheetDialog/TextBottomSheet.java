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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.Context;
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
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetTextBinding;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;

public class TextBottomSheet extends BaseBottomSheet {

  private final static String TAG = TextBottomSheet.class.getSimpleName();
  private boolean debug;

  private FragmentBottomsheetTextBinding binding;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {

    binding = FragmentBottomsheetTextBinding.inflate(
        inflater, container, false
    );

    Context context = requireContext();
    Bundle bundle = requireArguments();

    debug = PrefsUtil.isDebuggingEnabled(requireActivity());

    binding.textTextTitle.setText(
        bundle.getString(Constants.ARGUMENT.TITLE)
    );

    String link = bundle.getString(Constants.ARGUMENT.LINK);
    if (link != null) {
      binding.frameTextOpenLink.setOnClickListener(v -> {
        IconUtil.start(binding.imageTextOpenLink);
        new Handler().postDelayed(
            () -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))),
            500
        );
      });
    } else {
      binding.frameTextOpenLink.setVisibility(View.GONE);
    }

    int file = bundle.getInt(Constants.ARGUMENT.FILE);
    binding.textText.setText(ResUtil.getRawText(context, file));

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
