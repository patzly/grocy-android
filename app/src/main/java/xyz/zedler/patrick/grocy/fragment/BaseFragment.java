package xyz.zedler.patrick.grocy.fragment;

import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;

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

    public void editObject(Object object) {}

    public void deleteObjectSafely(Object object) {}

    public void deleteObject(int objectId) {}

    public void updateConnectivity(boolean isOnline) {}

    public void selectShoppingList(int id) {}

    public void enableLoginButtons() {}

    public void requestLogin(String server, String key, boolean checkVersion, boolean isDemo) {}

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if(nextAnim == 0) return null;

        Animation anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);

        anim.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                if(enter) BaseFragment.this.onAnimationEnd();
            }
        });

        return anim;
    }

    void onAnimationEnd() {}

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
     * Returns data from last fragment (which was in backStack on the top of the current one)
     * immediately. The last fragment stored this data with <code>setForPreviousFragment</code>.
     * @param key (String): identifier for value
     * @return Object: the value or null, if no data was set
     */
    @Nullable
    Object getFromLastFragmentNow(String key) {
        NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
        assert backStackEntry != null;
        return backStackEntry.getSavedStateHandle().get(key);
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

    /**
     * Set data for this fragment (which is on top of the backStack)
     * @param key (String): identifier for value
     * @param value (Object): the value to store
     */
    void setForThisFragment(String key, Object value) {
        NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().set(key, value);
    }

    /**
     * Remove set data of this fragment (which is on top of the backStack)
     * @param key (String): identifier for value
     */
    void removeForThisFragment(String key) {
        NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().remove(key);
    }

    @Nullable
    public Animation setStatusBarColor(
            int transit,
            boolean enter,
            int nextAnim,
            MainActivity activity,
            @ColorRes int color
    ) {
        if(!enter) return super.onCreateAnimation(transit, false, nextAnim);
        if(nextAnim == 0) {
            // set color of statusBar immediately after popBackStack, when previous fragment appears
            activity.setStatusBarColor(color);
            return super.onCreateAnimation(transit, true, nextAnim);
        }
        // set color of statusBar after transition is finished (when shown)
        Animation anim = AnimationUtils.loadAnimation(requireActivity(), nextAnim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                activity.setStatusBarColor(color);
            }
        });
        return anim;
    }

    Drawable getDrawable(@DrawableRes int drawable) {
        return ContextCompat.getDrawable(requireContext(), drawable);
    }

    public void setOption(Object value, String option) {}

    interface ObserverListener {
        void onChange(Object value);
    }
}
