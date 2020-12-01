package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


/**
 * This is an extended BottomSheetDialogFragment class. The one overridden method fixes the
 * weird behavior of bottom sheets in landscape mode. All bottom sheets in this app should use this
 * extended class to apply the fix.
 */
public class CustomBottomSheet extends BottomSheetDialogFragment {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation != Configuration.ORIENTATION_LANDSCAPE) return;
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if(dialog == null) return;

            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if(sheet == null) return;

            DisplayMetrics metrics = new DisplayMetrics();
            ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);

            BottomSheetBehavior.from(sheet).setPeekHeight(metrics.heightPixels / 2);
        });
    }

    @NonNull
    NavController findNavController() {
        return NavHostFragment.findNavController(this);
    }

    void navigate(NavDirections directions) {
        findNavController().navigate(directions);
    }

    void navigate(NavDirections directions, @NonNull Navigator.Extras navigatorExtras) {
        findNavController().navigate(directions, navigatorExtras);
    }

    void navigate(NavDirections directions, @NonNull NavOptions navOptions) {
        findNavController().navigate(directions, navOptions);
    }
}
