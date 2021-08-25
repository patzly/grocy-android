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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Arrays;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BarcodeFormatsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.RestartBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShortcutsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.APPEARANCE;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SCANNER;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SHOPPING_MODE;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.UnlockUtil;

public class SettingsViewModel extends BaseViewModel {

  public static final int THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
  public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
  public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;

  private static final String TAG = SettingsViewModel.class.getSimpleName();
  private final SharedPreferences sharedPrefs;

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;

  private MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<Boolean> getExternalScannerEnabledLive;
  private final MutableLiveData<Boolean> needsRestartLive;
  private final MutableLiveData<Boolean> torEnabledLive;
  private final MutableLiveData<Boolean> proxyEnabledLive;
  private final MutableLiveData<String> shoppingModeUpdateIntervalTextLive;
  private ArrayList<Location> locations;
  private final MutableLiveData<String> presetLocationTextLive;
  private ArrayList<ProductGroup> productGroups;
  private final MutableLiveData<String> presetProductGroupTextLive;
  private ArrayList<QuantityUnit> quantityUnits;
  private final MutableLiveData<String> presetQuantityUnitTextLive;
  private final MutableLiveData<String> dueSoonDaysTextLive;
  private final MutableLiveData<String> defaultPurchaseAmountTextLive;
  private final MutableLiveData<String> defaultConsumeAmountTextLive;
  private final MutableLiveData<Boolean> showBarcodeScannerZXingInfo;
  private final MutableLiveData<Boolean> showMLKitCropStreamLive;

  public SettingsViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

    dlHelper = new DownloadHelper(
        getApplication(),
        TAG,
        isLoading -> isLoadingLive.setValue(isLoading)
    );
    grocyApi = new GrocyApi(getApplication());

    isLoadingLive = new MutableLiveData<>(false);
    getExternalScannerEnabledLive = new MutableLiveData<>(getExternalScannerEnabled());
    needsRestartLive = new MutableLiveData<>(false);
    torEnabledLive = new MutableLiveData<>(getTorEnabled());
    proxyEnabledLive = new MutableLiveData<>(getProxyEnabled());
    shoppingModeUpdateIntervalTextLive = new MutableLiveData<>(getShoppingModeUpdateIntervalText());
    presetLocationTextLive = new MutableLiveData<>(getString(R.string.setting_loading));
    presetProductGroupTextLive = new MutableLiveData<>(getString(R.string.setting_loading));
    presetQuantityUnitTextLive = new MutableLiveData<>(getString(R.string.setting_loading));
    dueSoonDaysTextLive = new MutableLiveData<>(getDueSoonDaysText());
    defaultPurchaseAmountTextLive = new MutableLiveData<>(getDefaultPurchaseAmountText());
    defaultConsumeAmountTextLive = new MutableLiveData<>(getDefaultConsumeAmountText());
    showBarcodeScannerZXingInfo = new MutableLiveData<>(true);
    showMLKitCropStreamLive = new MutableLiveData<>(getUseMlKitScanner());
  }

  public boolean isDemo() {
    String server = getServerUrl();
    return server != null && server.contains("grocy.info");
  }

  public boolean isVersionCompatible() {
    return getSupportedVersions().contains(
        sharedPrefs.getString(
            Constants.PREF.GROCY_VERSION,
            getString(R.string.date_unknown)
        )
    );
  }

  public void showCompatibilityBottomSheet() {
    if (isVersionCompatible()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.SERVER, sharedPrefs.getString(
        Constants.PREF.SERVER_URL,
        getString(R.string.date_unknown)
    ));
    bundle.putString(Constants.ARGUMENT.KEY, sharedPrefs.getString(
        Constants.PREF.API_KEY,
        getString(R.string.date_unknown)
    ));
    bundle.putString(Constants.ARGUMENT.VERSION, sharedPrefs.getString(
        Constants.PREF.GROCY_VERSION,
        getString(R.string.date_unknown)
    ));
    bundle.putBoolean(Constants.ARGUMENT.DEMO_CHOSEN, isDemo());
    bundle.putStringArrayList(
        Constants.ARGUMENT.SUPPORTED_VERSIONS,
        getSupportedVersions()
    );
    CompatibilityBottomSheet bottomSheet = new CompatibilityBottomSheet();
    showBottomSheet(bottomSheet, bundle);
  }

  public void reloadConfiguration() {
    ConfigUtil.loadInfo(
        dlHelper,
        grocyApi,
        sharedPrefs,
        () -> showBottomSheet(new RestartBottomSheet(), null),
        error -> showErrorMessage()
    );
  }

  public void showLogoutBottomSheet() {
    showBottomSheet(new LogoutBottomSheet());
  }

  public void showShortcutsBottomSheet() {
    showBottomSheet(new ShortcutsBottomSheet(), null);
  }

  public String getServerUrl() {
    return sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
  }

  public int getTheme() {
    return sharedPrefs.getInt(APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME);
  }

  public boolean isThemeActive(int theme) {
    return getTheme() == theme;
  }

  public void setTheme(int theme) {
    sharedPrefs.edit().putInt(APPEARANCE.THEME, theme).apply();
  }

  public String getLanguage() {
    return sharedPrefs.getString(
        Constants.SETTINGS.APPEARANCE.LANGUAGE,
        Constants.SETTINGS_DEFAULT.APPEARANCE.LANGUAGE
    );
  }

  public void showLoadingTimeoutBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.NUMBER, getLoadingTimeout());
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.property_seconds));
    bundle.putString(ARGUMENT.TYPE, NETWORK.LOADING_TIMEOUT);
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public int getLoadingTimeout() {
    return sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public void setLoadingTimeout(int seconds) {
    sharedPrefs.edit().putInt(Constants.SETTINGS.NETWORK.LOADING_TIMEOUT, seconds).apply();
  }

  public boolean getLoggingEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
        Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING
    );
  }

  public void setLoggingEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING, enabled).apply();
  }

  public boolean getBeginnerModeEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    );
  }

  public void setBeginnerModeEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE, enabled).apply();
  }

  public boolean getUseOpenFoodFactsEnabled() {
    return sharedPrefs.getBoolean(
        BEHAVIOR.FOOD_FACTS,
        SETTINGS_DEFAULT.BEHAVIOR.FOOD_FACTS
    );
  }

  public void setUseOpenFoodFactsEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.FOOD_FACTS, enabled).apply();
  }

  public boolean getExpandBottomSheetsEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.EXPAND_BOTTOM_SHEETS
    );
  }

  public void setExpandBottomSheetsEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS, enabled).apply();
  }

  public boolean getSpeedUpStartEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.SPEED_UP_START,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.SPEED_UP_START
    );
  }

  public void setSpeedUpStartEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.SPEED_UP_START, enabled).apply();
  }

  public MutableLiveData<Boolean> getShowBarcodeScannerZXingInfo() {
    return showBarcodeScannerZXingInfo;
  }

  public boolean isAppUnlocked() {
    return UnlockUtil.isKeyInstalled(getApplication());
  }

  public boolean getUseMlKitScanner() {
    return sharedPrefs.getBoolean(
        SCANNER.USE_ML_KIT,
        SETTINGS_DEFAULT.SCANNER.USE_ML_KIT
    );
  }

  public void setUseMlKitScanner(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.USE_ML_KIT, enabled).apply();
    assert showBarcodeScannerZXingInfo.getValue() != null;
    showBarcodeScannerZXingInfo.setValue(!enabled);
    showMLKitCropStreamLive.setValue(enabled);
  }

  public String getUseScannerToolString(boolean isMlKitButton) {
    if (isMlKitButton) {
      return getApplication().getString(R.string.title_use_scanner_tool, "ML Kit");
    } else {
      return getApplication().getString(R.string.title_use_scanner_tool, "ZXing");
    }
  }

  public boolean getCropCameraStream() {
    return sharedPrefs.getBoolean(
        SCANNER.CROP_CAMERA_STREAM,
        SETTINGS_DEFAULT.SCANNER.CROP_CAMERA_STREAM
    );
  }

  public void setCropCameraStream(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.CROP_CAMERA_STREAM, enabled).apply();
  }

  public MutableLiveData<Boolean> getShowMLKitCropStreamLive() {
    return showMLKitCropStreamLive;
  }

  public boolean getFrontCamEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.FRONT_CAM,
        Constants.SETTINGS_DEFAULT.SCANNER.FRONT_CAM
    );
  }

  public void setFrontCamEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.FRONT_CAM, enabled).apply();
  }

  public boolean getScannerFormat2dEnabled() {
    return sharedPrefs.getBoolean(
        SCANNER.SCANNER_FORMAT_2D,
        Constants.SETTINGS_DEFAULT.SCANNER.SCANNER_FORMAT_2D
    );
  }

  public void setScannerFormat2dEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.SCANNER_FORMAT_2D, enabled).apply();
  }

  public void showBarcodeFormatsBottomSheet() {
    showBottomSheet(new BarcodeFormatsBottomSheet());
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
  }

  public MutableLiveData<Boolean> getGetExternalScannerEnabledLive() {
    return getExternalScannerEnabledLive;
  }

  public void setExternalScannerEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER, enabled).apply();
    getExternalScannerEnabledLive.setValue(enabled);
  }

  public void showShoppingModeUpdateIntervalBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(ARGUMENT.NUMBER, sharedPrefs.getInt(
        SHOPPING_MODE.UPDATE_INTERVAL,
        SETTINGS_DEFAULT.SHOPPING_MODE.UPDATE_INTERVAL
    ));
    bundle.putString(ARGUMENT.TYPE, SHOPPING_MODE.UPDATE_INTERVAL);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_seconds));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getShoppingModeUpdateIntervalText() {
    return getApplication().getResources().getQuantityString(
        R.plurals.property_seconds_num,
        sharedPrefs.getInt(
            SHOPPING_MODE.UPDATE_INTERVAL,
            SETTINGS_DEFAULT.SHOPPING_MODE.UPDATE_INTERVAL
        ),
        sharedPrefs.getInt(
            SHOPPING_MODE.UPDATE_INTERVAL,
            SETTINGS_DEFAULT.SHOPPING_MODE.UPDATE_INTERVAL
        )
    );
  }

  public MutableLiveData<String> getShoppingModeUpdateIntervalTextLive() {
    return shoppingModeUpdateIntervalTextLive;
  }

  public void setShoppingModeUpdateInterval(String text) {
    int interval = 10;
    if (NumUtil.isStringInt(text)) {
      interval = Integer.parseInt(text);
      if (interval < 0) {
        interval = 10;
      }
    }
    sharedPrefs.edit().putInt(SHOPPING_MODE.UPDATE_INTERVAL, interval).apply();
    shoppingModeUpdateIntervalTextLive.setValue(
        getApplication().getResources().getQuantityString(
            R.plurals.property_seconds_num,
            interval,
            interval
        )
    );
  }

  public boolean getKeepScreenOnEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SHOPPING_MODE.KEEP_SCREEN_ON,
        Constants.SETTINGS_DEFAULT.SHOPPING_MODE.KEEP_SCREEN_ON
    );
  }

  public void setKeepScreenOnEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SHOPPING_MODE.KEEP_SCREEN_ON, enabled)
        .apply();
  }

  public boolean getShowDoneItemsEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SHOPPING_MODE.SHOW_DONE_ITEMS,
        Constants.SETTINGS_DEFAULT.SHOPPING_MODE.SHOW_DONE_ITEMS
    );
  }

  public void setShowDoneItemsEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SHOPPING_MODE.SHOW_DONE_ITEMS, enabled)
        .apply();
  }

  public boolean getListIndicatorEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.STOCK.DISPLAY_DOTS_IN_STOCK,
        Constants.SETTINGS_DEFAULT.STOCK.DISPLAY_DOTS_IN_STOCK
    );
  }

  public void setListIndicatorEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(STOCK.DISPLAY_DOTS_IN_STOCK, enabled)
        .apply();
    dlHelper.uploadSetting(STOCK.DISPLAY_DOTS_IN_STOCK, enabled, this::showMessage);
  }

  public MutableLiveData<String> getPresetLocationTextLive() {
    return presetLocationTextLive;
  }

  public void setPresetLocation(Location location) {
    sharedPrefs.edit().putInt(STOCK.LOCATION, location.getId()).apply();
    dlHelper.uploadSetting(STOCK.LOCATION, location.getId(), this::showMessage);
    presetLocationTextLive.setValue(location.getName());
  }

  public MutableLiveData<String> getPresetProductGroupTextLive() {
    return presetProductGroupTextLive;
  }

  public void setPresetProductGroup(ProductGroup productGroup) {
    sharedPrefs.edit().putInt(STOCK.PRODUCT_GROUP, productGroup.getId()).apply();
    dlHelper.uploadSetting(STOCK.PRODUCT_GROUP, productGroup.getId(), this::showMessage);
    presetProductGroupTextLive.setValue(productGroup.getName());
  }

  public MutableLiveData<String> getPresetQuantityUnitTextLive() {
    return presetQuantityUnitTextLive;
  }

  public void setPresetQuantityUnit(QuantityUnit quantityUnit) {
    sharedPrefs.edit().putInt(STOCK.QUANTITY_UNIT, quantityUnit.getId()).apply();
    dlHelper.uploadSetting(STOCK.QUANTITY_UNIT, quantityUnit.getId(), this::showMessage);
    presetQuantityUnitTextLive.setValue(quantityUnit.getName());
  }

  public void loadProductPresets() {
    int locationId = sharedPrefs.getInt(STOCK.LOCATION, SETTINGS_DEFAULT.STOCK.LOCATION);
    int groupId = sharedPrefs.getInt(STOCK.PRODUCT_GROUP, SETTINGS_DEFAULT.STOCK.PRODUCT_GROUP);
    int unitId = sharedPrefs.getInt(STOCK.QUANTITY_UNIT, SETTINGS_DEFAULT.STOCK.QUANTITY_UNIT);
    dlHelper.getLocations(
        locations -> {
          this.locations = locations;
          this.locations.add(
              0,
              new Location(-1, getString(R.string.subtitle_none_selected))
          );
          Location location = getLocation(locationId);
          presetLocationTextLive.setValue(location != null ? location.getName()
              : getString(R.string.subtitle_none_selected));
        }, error -> presetLocationTextLive.setValue(getString(R.string.setting_not_loaded))
    ).perform(dlHelper.getUuid());
    dlHelper.getProductGroups(
        productGroups -> {
          this.productGroups = productGroups;
          this.productGroups.add(
              0,
              new ProductGroup(-1, getString(R.string.subtitle_none_selected))
          );
          ProductGroup productGroup = getProductGroup(groupId);
          presetProductGroupTextLive.setValue(productGroup != null ? productGroup.getName()
              : getString(R.string.subtitle_none_selected));
        }, error -> presetProductGroupTextLive.setValue(getString(R.string.setting_not_loaded))
    ).perform(dlHelper.getUuid());
    dlHelper.getQuantityUnits(
        quantityUnits -> {
          this.quantityUnits = quantityUnits;
          this.quantityUnits.add(
              0,
              new QuantityUnit(-1, getString(R.string.subtitle_none_selected))
          );
          QuantityUnit quantityUnit = getQuantityUnit(unitId);
          presetQuantityUnitTextLive.setValue(quantityUnit != null ? quantityUnit.getName()
              : getString(R.string.subtitle_none_selected));
        }, error -> presetQuantityUnitTextLive.setValue(getString(R.string.setting_not_loaded))
    ).perform(dlHelper.getUuid());
  }

  public void showLocationsBottomSheet() {
    if (locations == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.LOCATIONS, locations);
    bundle.putInt(
        ARGUMENT.SELECTED_ID,
        sharedPrefs.getInt(STOCK.LOCATION, SETTINGS_DEFAULT.STOCK.LOCATION)
    );
    showBottomSheet(new LocationsBottomSheet(), bundle);
  }

  public void showProductGroupsBottomSheet() {
    if (productGroups == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.PRODUCT_GROUPS, productGroups);
    bundle.putInt(
        ARGUMENT.SELECTED_ID,
        sharedPrefs.getInt(STOCK.PRODUCT_GROUP, SETTINGS_DEFAULT.STOCK.PRODUCT_GROUP)
    );
    showBottomSheet(new ProductGroupsBottomSheet(), bundle);
  }

  public void showQuantityUnitsBottomSheet() {
    if (quantityUnits == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.QUANTITY_UNITS, quantityUnits);
    bundle.putInt(
        ARGUMENT.SELECTED_ID,
        sharedPrefs.getInt(STOCK.QUANTITY_UNIT, SETTINGS_DEFAULT.STOCK.QUANTITY_UNIT)
    );
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  public void showDueSoonDaysBottomSheet() {
    Bundle bundle = new Bundle();
    String days = sharedPrefs.getString(STOCK.DUE_SOON_DAYS, SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    if (NumUtil.isStringInt(days)) {
      bundle.putInt(ARGUMENT.NUMBER, Integer.parseInt(days));
    } else {
      bundle.putInt(ARGUMENT.NUMBER, Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS));
    }
    bundle.putString(ARGUMENT.TYPE, STOCK.DUE_SOON_DAYS);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_days));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getDueSoonDaysText() {
    String days = sharedPrefs.getString(STOCK.DUE_SOON_DAYS, SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    int daysInt;
    if (NumUtil.isStringInt(days)) {
      daysInt = Integer.parseInt(days);
    } else {
      daysInt = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    }
    return getApplication().getResources().getQuantityString(R.plurals.date_days, daysInt, daysInt);
  }

  public MutableLiveData<String> getDueSoonDaysTextLive() {
    return dueSoonDaysTextLive;
  }

  public void setDueSoonDays(String text) {
    int interval = 5;
    if (NumUtil.isStringInt(text)) {
      interval = Integer.parseInt(text);
      if (interval < 1) {
        interval = 5;
      }
    }
    sharedPrefs.edit().putString(STOCK.DUE_SOON_DAYS, String.valueOf(interval)).apply();
    dueSoonDaysTextLive.setValue(
        getApplication().getResources().getQuantityString(R.plurals.date_days, interval, interval)
    );
    dlHelper.uploadSetting(STOCK.DUE_SOON_DAYS, String.valueOf(interval), this::showMessage);
  }

  public void showDefaultPurchaseAmountBottomSheet() {
    Bundle bundle = new Bundle();
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_PURCHASE_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT
    );
    if (NumUtil.isStringDouble(amount)) {
      bundle.putDouble(ARGUMENT.NUMBER, Double.parseDouble(amount));
    } else {
      bundle.putDouble(
          ARGUMENT.NUMBER,
          Double.parseDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT)
      );
    }
    bundle.putString(ARGUMENT.TYPE, STOCK.DEFAULT_PURCHASE_AMOUNT);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_amount));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getDefaultPurchaseAmountText() {
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_PURCHASE_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT
    );
    double amountDouble;
    if (NumUtil.isStringDouble(amount)) {
      amountDouble = Double.parseDouble(amount);
    } else {
      amountDouble = Double.parseDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT);
    }
    return NumUtil.trim(amountDouble);
  }

  public MutableLiveData<String> getDefaultPurchaseAmountTextLive() {
    return defaultPurchaseAmountTextLive;
  }

  public void setDefaultPurchaseAmount(String text) {
    double amount = 0;
    if (NumUtil.isStringDouble(text)) {
      amount = Double.parseDouble(text);
      if (amount < 0) {
        amount = 0;
      }
    }
    sharedPrefs.edit().putString(STOCK.DEFAULT_PURCHASE_AMOUNT, NumUtil.trim(amount)).apply();
    defaultPurchaseAmountTextLive.setValue(NumUtil.trim(amount));
    dlHelper.uploadSetting(STOCK.DEFAULT_PURCHASE_AMOUNT, NumUtil.trim(amount), this::showMessage);
  }

  public boolean getPurchasedDateEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.STOCK.SHOW_PURCHASED_DATE,
        Constants.SETTINGS_DEFAULT.STOCK.SHOW_PURCHASED_DATE
    );
  }

  public void setPurchasedDateEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.STOCK.SHOW_PURCHASED_DATE, enabled)
        .apply();
    dlHelper.uploadSetting(STOCK.SHOW_PURCHASED_DATE, enabled, this::showMessage);
  }

  public void showDefaultConsumeAmountBottomSheet() {
    Bundle bundle = new Bundle();
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_CONSUME_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT
    );
    if (NumUtil.isStringDouble(amount)) {
      bundle.putDouble(ARGUMENT.NUMBER, Double.parseDouble(amount));
    } else {
      bundle.putDouble(
          ARGUMENT.NUMBER,
          Double.parseDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT)
      );
    }
    bundle.putString(ARGUMENT.TYPE, STOCK.DEFAULT_CONSUME_AMOUNT);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_amount));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getDefaultConsumeAmountText() {
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_CONSUME_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT
    );
    double amountDouble;
    if (NumUtil.isStringDouble(amount)) {
      amountDouble = Double.parseDouble(amount);
    } else {
      amountDouble = Double.parseDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT);
    }
    return NumUtil.trim(amountDouble);
  }

  public MutableLiveData<String> getDefaultConsumeAmountTextLive() {
    return defaultConsumeAmountTextLive;
  }

  public void setDefaultConsumeAmount(String text) {
    double amount = 0;
    if (NumUtil.isStringDouble(text)) {
      amount = Double.parseDouble(text);
      if (amount < 0) {
        amount = 0;
      }
    }
    sharedPrefs.edit().putString(STOCK.DEFAULT_CONSUME_AMOUNT, NumUtil.trim(amount)).apply();
    defaultConsumeAmountTextLive.setValue(NumUtil.trim(amount));
    dlHelper.uploadSetting(STOCK.DEFAULT_CONSUME_AMOUNT, NumUtil.trim(amount), this::showMessage);
  }

  public boolean getUseQuickConsumeAmountEnabled() {
    return sharedPrefs.getBoolean(
        STOCK.USE_QUICK_CONSUME_AMOUNT,
        SETTINGS_DEFAULT.STOCK.USE_QUICK_CONSUME_AMOUNT
    );
  }

  public void setUseQuickConsumeAmountEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.STOCK.USE_QUICK_CONSUME_AMOUNT, enabled)
        .apply();
    dlHelper.uploadSetting(STOCK.USE_QUICK_CONSUME_AMOUNT, enabled, this::showMessage);
  }

  public boolean getLoadingCircleEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.NETWORK.LOADING_CIRCLE,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_CIRCLE
    );
  }

  public void setLoadingCircleEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.NETWORK.LOADING_CIRCLE, enabled).apply();
  }

  public MutableLiveData<Boolean> getNeedsRestartLive() {
    return needsRestartLive;
  }

  public MutableLiveData<Boolean> getTorEnabledLive() {
    return torEnabledLive;
  }

  public boolean getTorEnabled() {
    return sharedPrefs.getBoolean(NETWORK.TOR, SETTINGS_DEFAULT.NETWORK.TOR);
  }

  public void setTorEnabled(boolean enabled) {
    if (enabled != getTorEnabled()) needsRestartLive.setValue(true);
    sharedPrefs.edit().putBoolean(NETWORK.TOR, enabled).apply();
  }

  public MutableLiveData<Boolean> getProxyEnabledLive() {
    return proxyEnabledLive;
  }

  public boolean getProxyEnabled() {
    return sharedPrefs.getBoolean(NETWORK.PROXY, SETTINGS_DEFAULT.NETWORK.PROXY);
  }

  public void setProxyEnabled(boolean enabled) {
    if (enabled != getProxyEnabled()) needsRestartLive.setValue(true);
    sharedPrefs.edit().putBoolean(NETWORK.PROXY, enabled).apply();
  }

  public String getProxyHost() {
    return sharedPrefs.getString(NETWORK.PROXY_HOST, SETTINGS_DEFAULT.NETWORK.PROXY_HOST);
  }

  public void setProxyHost(String host) {
    sharedPrefs.edit().putString(NETWORK.PROXY_HOST, host).apply();
    needsRestartLive.setValue(true);
  }

  public void showProxyHostBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putString(ARGUMENT.TEXT, getProxyHost());
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.setting_proxy_host));
    bundle.putString(ARGUMENT.TYPE, NETWORK.PROXY_HOST);
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public int getProxyPort() {
    return sharedPrefs.getInt(NETWORK.PROXY_PORT, SETTINGS_DEFAULT.NETWORK.PROXY_PORT);
  }

  public void setProxyPort(int port) {
    sharedPrefs.edit().putInt(NETWORK.PROXY_PORT, port).apply();
    needsRestartLive.setValue(true);
  }

  public void showProxyPortBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.NUMBER, getProxyPort());
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.setting_proxy_port));
    bundle.putString(ARGUMENT.TYPE, NETWORK.PROXY_PORT);
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public ArrayList<String> getSupportedVersions() {
    return new ArrayList<>(Arrays.asList(
        getApplication().getResources().getStringArray(R.array.compatible_grocy_versions)
    ));
  }

  public boolean getIsDemoInstance() {
    String server = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
    return server != null && server.contains("grocy.info");
  }

  private Location getLocation(int id) {
    if (id == -1) {
      return null;
    }
    for (Location location : locations) {
      if (location.getId() == id) {
        return location;
      }
    }
    return null;
  }

  private ProductGroup getProductGroup(int id) {
    if (id == -1) {
      return null;
    }
    for (ProductGroup productGroup : productGroups) {
      if (productGroup.getId() == id) {
        return productGroup;
      }
    }
    return null;
  }

  private QuantityUnit getQuantityUnit(int id) {
    if (id == -1) {
      return null;
    }
    for (QuantityUnit quantityUnit : quantityUnits) {
      if (quantityUnit.getId() == id) {
        return quantityUnit;
      }
    }
    return null;
  }

  public DownloadHelper getDownloadHelper() {
    return dlHelper;
  }

  public GrocyApi getGrocyApi() {
    return grocyApi;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
