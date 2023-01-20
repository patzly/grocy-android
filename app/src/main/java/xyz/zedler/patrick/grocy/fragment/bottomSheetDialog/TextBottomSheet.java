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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetTextBinding;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class TextBottomSheet extends BaseBottomSheetDialogFragment {

  private static final String TAG = "TextBottomSheet";

  private FragmentBottomsheetTextBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
    binding = FragmentBottomsheetTextBinding.inflate(inflater, container, false);

    TextBottomSheetArgs args = TextBottomSheetArgs.fromBundle(requireArguments());

    binding.toolbarText.setTitle(getString(args.getTitle()));

    String link = args.getLink() != 0 ? getString(args.getLink()) : null;
    if (link != null) {
      binding.toolbarText.inflateMenu(R.menu.menu_link);
      ResUtil.tintMenuItemIcon(
          requireContext(), binding.toolbarText.getMenu().findItem(R.id.action_open_link)
      );
      binding.toolbarText.setOnMenuItemClickListener(item -> {
        int id = item.getItemId();
        if (id == R.id.action_open_link && getViewUtil().isClickEnabled(id)) {
          performHapticClick();
          ViewUtil.startIcon(item.getIcon());
          new Handler(Looper.getMainLooper()).postDelayed(
              () -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))), 500
          );
          return true;
        } else {
          return false;
        }
      });
    } else {
      binding.toolbarText.setTitleCentered(true);
    }

    String[] highlights = args.getHighlights();
    if (highlights == null) {
      highlights = new String[]{};
    }
    binding.formattedText.setText(ResUtil.getRawText(requireContext(), args.getFile()), highlights);

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.formattedText.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
