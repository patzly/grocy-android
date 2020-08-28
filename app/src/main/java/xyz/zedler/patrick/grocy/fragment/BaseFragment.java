package xyz.zedler.patrick.grocy.fragment;

import android.app.Activity;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;

public class BaseFragment extends Fragment {

    private boolean isSearchVisible = false;

    public boolean isSearchVisible() {
        return isSearchVisible;
    }

    public void setIsSearchVisible(boolean visible) {
        isSearchVisible = visible;
    }

    public void dismissSearch() {}

    public NavOptions getNavOptions() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_up)
                .setPopExitAnim(R.anim.slide_out_down)
                .build();
    }

    public static void navigateUp(Fragment fragment, Activity activity) {
        if(fragment == null || activity == null) return;
        NavHostFragment.findNavController(fragment).navigateUp();
        if(activity instanceof MainActivity) ((MainActivity) activity).binding.bottomAppBar.show();
    }
}
