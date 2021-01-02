package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.IdRes;
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

import xyz.zedler.patrick.grocy.R;


/**
 * This is an extended BottomSheetDialogFragment class. The one overridden method fixes the
 * weird behavior of bottom sheets in landscape mode. All bottom sheets in this app should use this
 * extended class to apply the fix.
 */
public class BaseBottomSheet extends BottomSheetDialogFragment {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation != Configuration.ORIENTATION_LANDSCAPE) return;
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if(dialog == null) return;

            DisplayMetrics metrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

            BottomSheetBehavior<View> behavior = getBehavior();
            if(behavior == null) return;

            behavior.setPeekHeight(metrics.heightPixels / 2);
        });
    }

    @Nullable
    BottomSheetBehavior<View> getBehavior() {
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if(dialog == null) return null;
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if(sheet == null) return null;
        return BottomSheetBehavior.from(sheet);
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

    void navigate(@IdRes int destination) {
        navigate(destination, (Bundle) null);
    }

    void navigate(@IdRes int destination, Bundle arguments) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up)
                .setPopExitAnim(R.anim.slide_out_down)
                .setExitAnim(R.anim.slide_no);
        findNavController().navigate(destination, arguments, builder.build());
    }

    void navigate(@IdRes int destination, @NonNull NavOptions navOptions) {
        findNavController().navigate(destination, null, navOptions);
    }

    void navigateDeepLink(@NonNull String uri) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up)
                .setPopExitAnim(R.anim.slide_out_down)
                .setExitAnim(R.anim.slide_no);
        findNavController().navigate(Uri.parse(uri), builder.build());
    }
}
