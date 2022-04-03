/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;
import com.android.volley.VolleyError;
import java.net.URLEncoder;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;

@SuppressWarnings("EmptyMethod")
public class BaseFragment extends Fragment {

  String getErrorMessage(VolleyError volleyError) {
    // similar method is also in BaseViewmodel
    if (volleyError != null && volleyError.networkResponse != null) {
      if (volleyError.networkResponse.statusCode == 403) {
        return getString(R.string.error_permission);
      }
    }
    return getString(R.string.error_undefined);
  }

  public boolean isSearchVisible() {
    return false;
  }

  public void dismissSearch() {
  }

  public void onBottomSheetDismissed() {
  }

  public boolean onBackPressed() {
    return false;
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return false;
  }

  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return false;
  }

  public void clearFields() {
  }

  public void editObject(Object object) {
  }

  public void copyProduct(Product product) {
  }

  public void deleteObjectSafely(Object object) {
  }

  public void deleteObject(int objectId) {
  }

  public void deleteShoppingList(ShoppingList shoppingList) {
  }

  public void clearShoppingList(ShoppingList shoppingList, boolean onlyDoneItems) {
  }

  public void toggleDoneStatus(ShoppingListItem shoppingListItem) {
  }

  public void purchaseItem(ShoppingListItem shoppingListItem) {
  }

  public void deleteItem(ShoppingListItem shoppingListItem) {
  }

  public void editItem(ShoppingListItem shoppingListItem) {
  }

  public void toggleDoneStatus(Task task) {
  }

  public void deleteTask(Task task) {
  }

  public void editTask(Task task) {
  }

  @Nullable
  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return null;
  }

  public void updateConnectivity(boolean isOnline) {
  }

  public void selectShoppingList(ShoppingList shoppingList) {
  }

  public void selectQuantityUnit(QuantityUnit quantityUnit) {
  }

  public void selectQuantityUnit(QuantityUnit quantityUnit, Bundle argsBundle) {
  }

  public void selectPurchasedDate(String purchasedDate) {
  }

  public void selectDueDate(String dueDate) {
  }

  public void selectStockLocation(StockLocation stockLocation) {
  }

  public void selectStockEntry(StockEntry stockEntry) {
  }

  public void selectProductGroup(ProductGroup productGroup) {
  }

  public void selectLocation(Location location) {
  }

  public void selectStore(Store store) {
  }

  public void selectTaskCategory(TaskCategory taskCategory) {
  }

  public void selectUser(User user) {
  }

  public void setLanguage(Language language) {
  }

  public void addBarcodeToNewProduct(String barcode) {
  }

  public void addBarcodeToExistingProduct(String barcode) {
  }

  public void saveText(Spanned spanned) {
  }

  public void saveInput(String text, Bundle argsBundle) {
  }

  public void performAction(String action, StockItem stockItem) {
  }

  public void updateShortcuts() {
  }

  public void updateBarcodeFormats() {
  }

  public void startTransaction() {
  }

  public void interruptCurrentProductFlow() {
  }

  public void enableLoginButtons() {
  }

  public void login(boolean checkVersion) {
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    if (nextAnim == 0) {
      return null;
    }

    Animation anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);

    anim.setAnimationListener(new Animation.AnimationListener() {

      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        if (enter) {
          BaseFragment.this.onEnterAnimationEnd();
        }
      }
    });

    return anim;
  }

  void onEnterAnimationEnd() {
  }

  @NonNull
  NavController findNavController() {
    return NavHostFragment.findNavController(this);
  }

  public void navigate(NavDirections directions) {
    findNavController().navigate(directions);
  }

  void navigate(NavDirections directions, @NonNull Navigator.Extras navigatorExtras) {
    findNavController().navigate(directions, navigatorExtras);
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

  public void navigateDeepLink(@NonNull String uri) {
    navigateDeepLink(Uri.parse(uri));
  }

  void navigateDeepLink(@StringRes int uri, @NonNull Bundle args) {
    navigateDeepLink(getUriWithArgs(getString(uri), args));
  }

  void navigateDeepLinkSlideStartEnd(@StringRes int uri, @NonNull Bundle args) {
    navigateDeepLinkSlideStartEnd(getUriWithArgs(getString(uri), args));
  }

  private void navigateDeepLink(@NonNull Uri uri) {
    NavOptions.Builder builder = new NavOptions.Builder();
    builder.setEnterAnim(R.anim.slide_in_up)
        .setPopExitAnim(R.anim.slide_out_down)
        .setExitAnim(R.anim.slide_no);
    findNavController().navigate(uri, builder.build());
  }

  private void navigateDeepLinkSlideStartEnd(@NonNull Uri uri) {
    NavOptions.Builder builder = new NavOptions.Builder();
    builder.setEnterAnim(R.anim.slide_from_end)
        .setPopExitAnim(R.anim.slide_to_end)
        .setPopEnterAnim(R.anim.slide_from_start)
        .setExitAnim(R.anim.slide_to_start);
    findNavController().navigate(uri, builder.build());
  }

  private Uri getUriWithArgs(@NonNull String uri, @NonNull Bundle argsBundle) {
    String[] parts = uri.split("\\?");
    if (parts.length == 1) {
      return Uri.parse(uri);
    }
    String linkPart = parts[0];
    String argsPart = parts[parts.length - 1];
    String[] pairs = argsPart.split("&");
    StringBuilder finalDeepLink = new StringBuilder(linkPart + "?");
    for (int i = 0; i <= pairs.length - 1; i++) {
      String pair = pairs[i];
      String key = pair.split("=")[0];
      Object valueBundle = argsBundle.get(key);
      if (valueBundle == null) {
        continue;
      }
      try {
        finalDeepLink.append(key).append("=")
            .append(URLEncoder.encode(valueBundle.toString(), "UTF-8"));
      } catch (Throwable ignore) {
      }
      if (i != pairs.length - 1) {
        finalDeepLink.append("&");
      }
    }
    return Uri.parse(finalDeepLink.toString());
  }

  /**
   * Get data from last fragment (which was in backStack on the top of the current one). The last
   * fragment stored this data with <code>setForPreviousFragment</code>.
   *
   * @param key              (String): identifier for value
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
   *
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
   *
   * @param key   (String): identifier for value
   * @param value (Object): the value to store
   */
  void setForPreviousDestination(String key, Object value) {
    NavBackStackEntry backStackEntry = findNavController().getPreviousBackStackEntry();
    assert backStackEntry != null;
    backStackEntry.getSavedStateHandle().set(key, value);
  }

  /**
   * Set data for this destination (which is on top of the backStack)
   *
   * @param key   (String): identifier for value
   * @param value (Object): the value to store
   */
  void setForThisDestination(String key, Object value) {
    NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
    assert backStackEntry != null;
    backStackEntry.getSavedStateHandle().set(key, value);
  }

  /**
   * Set data for any destination (if multiple instances are in backStack, the topmost one)
   *
   * @param destinationId (int): identifier for destination
   * @param key           (String): identifier for value
   * @param value         (Object): the value to store
   */
  void setForDestination(@IdRes int destinationId, String key, Object value) {
    NavBackStackEntry backStackEntry;
    try {
      backStackEntry = findNavController().getBackStackEntry(destinationId);
    } catch (IllegalArgumentException e) {
      backStackEntry = null;
    }
    if (backStackEntry == null) {
      return;
    }
    backStackEntry.getSavedStateHandle().set(key, value);
  }

  /**
   * Remove set data of this destination (which is on top of the backStack)
   *
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
      @ColorRes int color,
      Runnable onAnimationEnd
  ) {
    if (!enter) {
      return super.onCreateAnimation(transit, false, nextAnim);
    }
    if (nextAnim == 0) {
      // set color of statusBar immediately after popBackStack, when previous fragment appears
      activity.setStatusBarColor(color);
      return super.onCreateAnimation(transit, true, nextAnim);
    }
    // set color of statusBar after transition is finished (when shown)
    Animation anim = AnimationUtils.loadAnimation(requireActivity(), nextAnim);
    anim.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        activity.setStatusBarColor(color);
        if (onAnimationEnd != null) {
          onAnimationEnd.run();
        }
        BaseFragment.this.onEnterAnimationEnd();
      }
    });
    return anim;
  }

  @Nullable
  public Animation setStatusBarColor(
      int transit,
      boolean enter,
      int nextAnim,
      MainActivity activity,
      @ColorRes int color
  ) {
    return setStatusBarColor(transit, enter, nextAnim, activity, color, null);
  }

  public void setOption(Object value, String option) {
  }

  interface ObserverListener {

    void onChange(Object value);
  }
}
