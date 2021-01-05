package xyz.zedler.patrick.grocy.fragment;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;

public class BaseFragment extends Fragment {

    private boolean isSearchVisible = false;
    private final MutableLiveData<Boolean> workflowEnabled = new MutableLiveData<>(false);

    public boolean isSearchVisible() {
        return isSearchVisible;
    }

    public void setIsSearchVisible(boolean visible) {
        isSearchVisible = visible;
    }

    public void dismissSearch() {}

    public boolean isWorkflowEnabled() {
        return workflowEnabled.getValue();
    }

    public MutableLiveData<Boolean> getWorkflowEnabled() {
        return workflowEnabled;
    }

    public void setWorkflowEnabled(boolean enabled) {
        workflowEnabled.setValue(enabled);
    }

    public boolean toggleWorkflowEnabled() {
        workflowEnabled.setValue(!isWorkflowEnabled());
        return true;
    }

    public void onBottomSheetDismissed() {}

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

    public void deleteShoppingList(ShoppingList shoppingList) {}

    @Nullable
    public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
        return null;
    }

    public int getSelectedQuantityUnitId() {
        return -1;
    }

    public void updateConnectivity(boolean isOnline) {}

    public void selectShoppingList(ShoppingList shoppingList) {}

    public void selectQuantityUnit(QuantityUnit quantityUnit) {}

    public void updateShortcuts() {}

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

    void navigate(NavDirections directions, @NonNull NavOptions navOptions) {
        findNavController().navigate(directions, navOptions);
    }

    public void navigate(@IdRes int destination) {
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

    @SuppressLint("RestrictedApi")
    int getBackStackSize() {
        return findNavController().getBackStack().size();
    }

    /**
     * Get data from last fragment (which was in backStack on the top of the current one).
     * The last fragment stored this data with <code>setForPreviousFragment</code>.
     * @param key (String): identifier for value
     * @param observerListener (ObserverListener): observer for callback after value was received
     */
    void getFromThisDestination(String key, ObserverListener observerListener) {
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
     * Returns data from last destination (which was in backStack on the top of the current one)
     * immediately. The last destination stored this data with <code>setForPreviousDestination</code>.
     * @param key (String): identifier for value
     * @return Object: the value or null, if no data was set
     */
    @Nullable
    Object getFromThisDestinationNow(String key) {
        NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
        assert backStackEntry != null;
        return backStackEntry.getSavedStateHandle().get(key);
    }

    /**
     * Set data for previous destination (which is in backStack below the current one)
     * @param key (String): identifier for value
     * @param value (Object): the value to store
     */
    void setForPreviousDestination(String key, Object value) {
        NavBackStackEntry backStackEntry = findNavController().getPreviousBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().set(key, value);
    }

    /**
     * Set data for this destination (which is on top of the backStack)
     * @param key (String): identifier for value
     * @param value (Object): the value to store
     */
    void setForThisDestination(String key, Object value) {
        NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().set(key, value);
    }

    /**
     * Set data for any destination (if multiple instances are in backStack, the topmost one)
     * @param destinationId (int): identifier for destination
     * @param key (String): identifier for value
     * @param value (Object): the value to store
     */
    void setForDestination(@IdRes int destinationId, String key, Object value) {
        NavBackStackEntry backStackEntry = findNavController().getBackStackEntry(destinationId);
        if(backStackEntry == null) return;
        backStackEntry.getSavedStateHandle().set(key, value);
    }

    /**
     * Remove set data of this destination (which is on top of the backStack)
     * @param key (String): identifier for value
     */
    void removeForThisDestination(String key) {
        NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().remove(key);
    }

    NavDestination getPreviousDestination() {
        NavBackStackEntry backStackEntry = findNavController().getPreviousBackStackEntry();
        assert backStackEntry != null;
        return backStackEntry.getDestination();
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
