package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetFeedbackBinding;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FeedbackBottomSheet extends BaseBottomSheet {

  private final static String TAG = "FeedbackBottomSheet";

  private FragmentBottomsheetFeedbackBinding binding;

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
    binding = FragmentBottomsheetFeedbackBinding.inflate(inflater, container, false);

    binding.linearFeedbackIssue.setOnClickListener(v -> {
      Intent intent = new Intent(
          Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_github_issues))
      );
      startActivity(intent);
      dismiss();
    });

    binding.linearFeedbackRate.setOnClickListener(v -> {
      ViewUtil.startIcon(binding.imageFeedbackRate);
      Uri uri = Uri.parse(
          "market://details?id=" + requireContext().getApplicationContext().getPackageName()
      );
      Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
      goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
          Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
          Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
          Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        try {
          startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
              "http://play.google.com/store/apps/details?id="
                  + requireContext().getApplicationContext().getPackageName()
          )));
        }
        dismiss();
      }, 300);
    });

    binding.linearFeedbackShare.setOnClickListener(v -> {
      ResUtil.share(requireContext(), R.string.msg_share);
      dismiss();
    });

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
