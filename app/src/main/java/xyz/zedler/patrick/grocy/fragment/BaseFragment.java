package xyz.zedler.patrick.grocy.fragment;

import android.view.KeyEvent;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;

public class BaseFragment extends Fragment {

    private boolean isSearchVisible = false;

    public boolean isSearchVisible() {
        return isSearchVisible;
    }

    public void setIsSearchVisible(boolean visible) {
        isSearchVisible = visible;
    }

    public void dismissSearch() {}

    public boolean onBackPressed() {
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public void pauseScan() {}

    public void resumeScan() {}

    public void addInputAsBarcode() {}

    public void clearFields() {}

    void getFromPreviousFragment(String key, ObserverListener observerListener) {
        NavBackStackEntry backStackEntry = NavHostFragment.findNavController(this)
                .getCurrentBackStackEntry();
        assert backStackEntry != null;
        if(backStackEntry.getSavedStateHandle().getLiveData(key).hasObservers()) {
            backStackEntry.getSavedStateHandle().getLiveData(key).removeObservers(
                    getViewLifecycleOwner()
            );
        }
        backStackEntry.getSavedStateHandle().getLiveData(key).observe(
                getViewLifecycleOwner(),
                value -> {
                    observerListener.onChange(value);
                    backStackEntry.getSavedStateHandle().remove(key);
                }
        );
    }

    interface ObserverListener {
        void onChange(Object value);
    }
}
