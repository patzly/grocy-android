package xyz.zedler.patrick.grocy.fragment;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import xyz.zedler.patrick.grocy.R;

public class BasicFragment extends Fragment {

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
}
