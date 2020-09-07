package xyz.zedler.patrick.grocy.fragment;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
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

    public void addBarcode(String barcode) {}

    public void createProductFromBarcode(String barcode) {}

    public void clearFields() {}

    @NonNull
    NavController findNavController() {
        return NavHostFragment.findNavController(this);
    }

    void navigate(NavDirections directions) {
        findNavController().navigate(directions);
    }

    /**
     * Get data from last fragment (which was in backStack on the top of the current one).
     * The last fragment stored this data with <code>setForPreviousFragment</code>.
     * @param key (String): identifier for value
     * @param observerListener (ObserverListener): observer for callback after value was received
     */
    void getFromLastFragment(String key, ObserverListener observerListener) {
        NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().getLiveData(key).removeObservers(
                getViewLifecycleOwner()
        );
        backStackEntry.getSavedStateHandle().getLiveData(key).observe(
                getViewLifecycleOwner(),
                value -> {
                    observerListener.onChange(value);
                    backStackEntry.getSavedStateHandle().remove(key);
                    backStackEntry.getSavedStateHandle().getLiveData(key).removeObservers(
                            getViewLifecycleOwner()
                    );
                }
        );
    }

    /**
     * Set data for previous fragment (which is in backStack below the current one)
     * @param key (String): identifier for value
     * @param value (Object): the value to store
     */
    void setForPreviousFragment(String key, Object value) {
        NavBackStackEntry backStackEntry = findNavController().getPreviousBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().set(key, value);
    }

    interface ObserverListener {
        void onChange(Object value);
    }
}
