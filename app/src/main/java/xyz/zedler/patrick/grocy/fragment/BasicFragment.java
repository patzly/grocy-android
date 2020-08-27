package xyz.zedler.patrick.grocy.fragment;

import androidx.fragment.app.Fragment;

public class BasicFragment extends Fragment {

    private boolean isSearchVisible = false;

    public boolean isSearchVisible() {
        return isSearchVisible;
    }

    public void setIsSearchVisible(boolean visible) {
        isSearchVisible = visible;
    }

    public void dismissSearch() {}
}
