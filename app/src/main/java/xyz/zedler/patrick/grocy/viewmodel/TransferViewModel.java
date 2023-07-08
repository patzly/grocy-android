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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.form.FormDataTransfer;
import xyz.zedler.patrick.grocy.fragment.TransferFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuickModeConfirmBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockEntriesBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockLocationsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.repository.InventoryRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.QuantityUnitConversionUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class TransferViewModel extends BaseViewModel {

  private static final String TAG = TransferViewModel.class.getSimpleName();
  private final SharedPreferences sharedPrefs;
  private final boolean debug;

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final InventoryRepository repository;
  private final FormDataTransfer formData;

  private List<Product> products;
  private List<QuantityUnitConversionResolved> unitConversions;
  private List<ProductBarcode> barcodes;
  private List<Location> locations;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> quickModeEnabled;

  private Runnable queueEmptyAction;
  private boolean productWillBeFilled;
  private final int maxDecimalPlacesAmount;

  public TransferViewModel(@NonNull Application application, TransferFragmentArgs args) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new InventoryRepository(application);
    formData = new FormDataTransfer(application, sharedPrefs, args);

    infoFullscreenLive = new MutableLiveData<>();
    boolean quickModeStart;
    if (!args.getCloseWhenFinished()) {
      quickModeStart = sharedPrefs.getBoolean(
          PREF.QUICK_MODE_ACTIVE_TRANSFER,
          false
      );
    } else {
      quickModeStart = false;
    }
    quickModeEnabled = new MutableLiveData<>(quickModeStart);

    barcodes = new ArrayList<>();
  }

  public FormDataTransfer getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.products = data.getProducts();
      this.barcodes = data.getBarcodes();
      this.locations = data.getLocations();
      this.quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      this.unitConversions = data.getQuantityUnitConversionsResolved();
      formData.getProductsLive().setValue(Product.getActiveAndStockEnabledProductsOnly(products));
      if (downloadAfterLoading) {
        downloadData(false);
      } else if (queueEmptyAction != null) {
        queueEmptyAction.run();
        queueEmptyAction = null;
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else if (queueEmptyAction != null) {
            queueEmptyAction.run();
            queueEmptyAction = null;
          }
        },
        error -> onError(error, TAG),
        forceUpdate,
        false,
        Product.class,
        ProductBarcode.class,
        Location.class,
        QuantityUnit.class,
        QuantityUnitConversionResolved.class
    );
  }

  public void setProduct(int productId, ProductBarcode barcode, String stockEntryId) {
    Runnable onQueueEmptyListener = () -> {
      ProductDetails productDetails = formData.getProductDetailsLive().getValue();
      assert productDetails != null;
      Product product = productDetails.getProduct();

      if (productDetails.getStockAmountAggregated() == 0) {
        String name = product.getName();
        showMessageAndContinueScanning(getApplication().getString(R.string.msg_not_in_stock, name));
        return;
      }
      if (productDetails.getProduct().getEnableTareWeightHandlingBoolean()) {
        showMessageAndContinueScanning(getApplication().getString(R.string.msg_transfer_tare_weight));
        return;
      }

      formData.getProductDetailsLive().setValue(productDetails);
      formData.getProductNameLive().setValue(product.getName());

      // stock location (from location)
      List<StockLocation> stockLocations = formData.getStockLocations();
      StockLocation stockLocation = StockLocation.getFromId(
          stockLocations,
          product.getLocationIdInt()
      );
      if (stockLocation == null && !stockLocations.isEmpty()) {
        stockLocation = stockLocations.get(stockLocations.size() - 1);
      }
      formData.getFromLocationLive().setValue(stockLocation);

      // quantity unit
      HashMap<QuantityUnit, Double> unitFactors= QuantityUnitConversionUtil.getUnitFactors(
          quantityUnitHashMap,
          unitConversions,
          product,
          VersionUtil.isGrocyServerMin400(sharedPrefs)
      );
      formData.getQuantityUnitsFactorsLive().setValue(unitFactors);
      QuantityUnit stock = quantityUnitHashMap.get(product.getQuIdStockInt());
      formData.getQuantityUnitStockLive().setValue(stock);

      QuantityUnit barcodeUnit = null;
      if (barcode != null && barcode.hasQuId()) {
        barcodeUnit = quantityUnitHashMap.get(barcode.getQuIdInt());
      }
      if (barcodeUnit != null && unitFactors.containsKey(barcodeUnit)) {
        formData.getQuantityUnitLive().setValue(barcodeUnit);
      } else {
        formData.getQuantityUnitLive().setValue(stock);
      }

      // amount
      if (barcode != null && barcode.hasAmount()) {
        // if barcode contains amount, take this
        // quick mode status doesn't matter
        formData.getAmountLive().setValue(NumUtil.trimAmount(barcode.getAmountDouble(), maxDecimalPlacesAmount));
      } else if (!isQuickModeEnabled()) {
        String defaultAmount = sharedPrefs.getString(
            Constants.SETTINGS.STOCK.DEFAULT_CONSUME_AMOUNT,
            Constants.SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT
        );
        if (NumUtil.isStringDouble(defaultAmount)) {
          defaultAmount = NumUtil.trimAmount(NumUtil.toDouble(defaultAmount), maxDecimalPlacesAmount);
        }
        if (NumUtil.isStringDouble(defaultAmount)
            && NumUtil.toDouble(defaultAmount) > 0) {
          formData.getAmountLive().setValue(defaultAmount);
        }
      } else {
        // if quick mode enabled, always fill with amount 1
        formData.getAmountLive().setValue(NumUtil.trimAmount(1, maxDecimalPlacesAmount));
      }

      // stock entry
      StockEntry stockEntry = null;
      if (stockEntryId != null) {
        stockEntry = StockEntry.getStockEntryFromId(formData.getStockEntries(), stockEntryId);
      }
      if (stockEntryId != null && stockEntry == null) {
        showMessage(R.string.error_stock_entry_grocycode);
      }
      if (stockEntry != null) {
        formData.getUseSpecificLive().setValue(true);
        formData.getSpecificStockEntryLive().setValue(stockEntry);
      } else {
        formData.getUseSpecificLive().setValue(false);
        formData.getSpecificStockEntryLive().setValue(null);
      }

      formData.isFormValid();
      if (isQuickModeEnabled()) {
        sendEvent(Event.FOCUS_INVALID_VIEWS);
      }
    };

    dlHelper.newQueue(
        updated -> onQueueEmptyListener.run(),
        error -> showMessageAndContinueScanning(getString(R.string.error_no_product_details))
    ).append(
        ProductDetails.getProductDetails(
            dlHelper,
            productId,
            productDetails -> formData.getProductDetailsLive().setValue(productDetails)
        ), StockLocation.getStockLocations(
            dlHelper,
            productId,
            formData::setStockLocations
        ), StockEntry.getStockEntries(
            dlHelper,
            productId,
            formData::setStockEntries
        )
    ).start();
  }

  public void onBarcodeRecognized(String barcode) {
    if (formData.getProductDetailsLive().getValue() != null) {
      if (ProductBarcode.getFromBarcode(barcodes, barcode) == null) {
        formData.getBarcodeLive().setValue(barcode);
      } else {
        showMessage(R.string.msg_clear_form_first);
      }
      return;
    }
    Product product = null;
    String stockEntryId = null;
    Grocycode grocycode = GrocycodeUtil.getGrocycode(barcode);
    if (grocycode != null && grocycode.isProduct()) {
      product = Product.getProductFromId(products, grocycode.getObjectId());
      if (product == null) {
        showMessageAndContinueScanning(R.string.msg_not_found);
        return;
      }
      stockEntryId = grocycode.getProductStockEntryId();
    } else if (grocycode != null) {
      showMessageAndContinueScanning(R.string.error_wrong_grocycode_type);
      return;
    }
    ProductBarcode productBarcode = null;
    if (product == null) {
      productBarcode = ProductBarcode.getFromBarcode(barcodes, barcode);
      product = productBarcode != null
          ? Product.getProductFromId(products, productBarcode.getProductIdInt()) : null;
    }
    if (product != null) {
      setProduct(product.getId(), productBarcode, stockEntryId);
    } else {
      Bundle bundle = new Bundle();
      bundle.putString(ARGUMENT.BARCODE, barcode);
      sendEvent(Event.CHOOSE_PRODUCT, bundle);
    }
  }

  public void checkProductInput() {
    formData.isProductNameValid();
    String input = formData.getProductNameLive().getValue();
    if (input == null || input.isEmpty()) {
      return;
    }
    Product product = Product.getProductFromName(products, input);

    Grocycode grocycode = GrocycodeUtil.getGrocycode(input.trim());
    if (grocycode != null && grocycode.isProduct()) {
      product = Product.getProductFromId(products, grocycode.getObjectId());
      if (product == null) {
        showMessageAndContinueScanning(R.string.msg_not_found);
        return;
      }
    } else if (grocycode != null) {
      showMessageAndContinueScanning(R.string.error_wrong_grocycode_type);
      return;
    }
    if (product == null) {
      ProductBarcode productBarcode = null;
      for (ProductBarcode code : barcodes) {
        if (code.getBarcode().equals(input.trim())) {
          productBarcode = code;
          product = Product.getProductFromId(products, code.getProductIdInt());
        }
      }
      if (product != null) {
        setProduct(product.getId(), productBarcode, null);
        return;
      }
    }

    ProductDetails currentProductDetails = formData.getProductDetailsLive().getValue();
    Product currentProduct = currentProductDetails != null
        ? currentProductDetails.getProduct() : null;
    if (currentProduct != null && product != null && currentProduct.getId() == product.getId()) {
      return;
    }

    if (product != null) {
      setProduct(product.getId(), null, null);
    } else {
      showInputProductBottomSheet(input);
    }
  }

  public void addBarcodeToExistingProduct(String barcode) {
    formData.getBarcodeLive().setValue(barcode);
    formData.getProductNameLive().setValue(null);
  }

  public void transferProduct() {
    transferProduct(false);
  }

  public void transferProduct(boolean confirmed) {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }
    if (formData.getBarcodeLive().getValue() != null) {
      uploadProductBarcode(this::transferProduct);
      return;
    }

    assert formData.getProductDetailsLive().getValue() != null;
    Product product = formData.getProductDetailsLive().getValue().getProduct();
    JSONObject body = formData.getFilledJSONObject();

    if (!confirmed && product.getShouldNotBeFrozenBoolean()
        && formData.getToLocationLive().getValue() != null
        && formData.getToLocationLive().getValue().getIsFreezerInt() == 1) {
      sendEvent(Event.CONFIRM_FREEZING);
      return;
    }

    dlHelper.postWithArray(grocyApi.transferProduct(product.getId()),
        body,
        response -> {
          // UNDO OPTION
          String transactionId = null;
          double amountTransferred = 0;
          try {
            transactionId = response.getJSONObject(0)
                .getString("transaction_id");
            for (int i = 0; i < response.length(); i++) {
              if (response.getJSONObject(i).getString("transaction_type")
                  .equals("transfer_from")) {
                continue;
              }
              amountTransferred += response.getJSONObject(i).getDouble("amount");
            }
          } catch (JSONException e) {
            if (debug) {
              Log.e(TAG, "transferProduct: " + e);
            }
          }
          if (debug) {
            Log.i(TAG, "transferProduct: transaction successful");
          }

          SnackbarMessage snackbarMessage = new SnackbarMessage(
              formData.getTransactionSuccessMsg(amountTransferred)
          );
          if (transactionId != null) {
            String transId = transactionId;
            snackbarMessage.setAction(
                getString(R.string.action_undo),
                v -> undoTransaction(transId)
            );
            snackbarMessage.setDurationSecs(sharedPrefs.getInt(
                    Constants.SETTINGS.BEHAVIOR.MESSAGE_DURATION,
                    Constants.SETTINGS_DEFAULT.BEHAVIOR.MESSAGE_DURATION));
          }
          showSnackbar(snackbarMessage);
          sendEvent(Event.CONSUME_SUCCESS);
        },
        error -> {
          showNetworkErrorMessage(error);
          if (debug) {
            Log.i(TAG, "transferProduct: " + error);
          }
        }
    );
  }

  private void undoTransaction(String transactionId) {
    dlHelper.post(
        grocyApi.undoStockTransaction(transactionId),
        success -> {
          showMessage(getString(R.string.msg_undone_transaction));
          if (debug) {
            Log.i(TAG, "undoTransaction: undone");
          }
        },
        this::showNetworkErrorMessage
    );
  }

  private void uploadProductBarcode(Runnable onSuccess) {
    ProductBarcode productBarcode = formData.fillProductBarcode();
    JSONObject body = productBarcode.getJsonFromProductBarcode(debug, TAG);
    ProductBarcode.addProductBarcode(dlHelper, body, () -> {
      formData.getBarcodeLive().setValue(null);
      barcodes.add(productBarcode); // add to list so it will be found on next scan without reload
      if (onSuccess != null) {
        onSuccess.run();
      }
    }, error -> showMessage(R.string.error_failed_barcode_upload)).perform(dlHelper.getUuid());
  }

  public void showInputProductBottomSheet(@NonNull String input) {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.PRODUCT_INPUT, input);
    showBottomSheet(new InputProductBottomSheet(), bundle);
  }

  public void showQuantityUnitsBottomSheet(boolean hasFocus) {
    if (!hasFocus) {
      return;
    }
    HashMap<QuantityUnit, Double> unitsFactors = getFormData()
        .getQuantityUnitsFactorsLive().getValue();
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.QUANTITY_UNITS,
        unitsFactors != null ? new ArrayList<>(unitsFactors.keySet()) : null
    );
    QuantityUnit quantityUnit = formData.getQuantityUnitLive().getValue();
    bundle.putInt(ARGUMENT.SELECTED_ID, quantityUnit != null ? quantityUnit.getId() : -1);
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  public void showStockEntriesBottomSheet() {
    if (!formData.isProductNameValid()) {
      return;
    }
    List<StockEntry> stockEntries = formData.getStockEntries();
    StockEntry currentStockEntry = formData.getSpecificStockEntryLive().getValue();
    String selectedId = currentStockEntry != null ? currentStockEntry.getStockId() : null;
    ArrayList<StockEntry> filteredStockEntries = new ArrayList<>();
    StockLocation stockLocation = formData.getFromLocationLive().getValue();
    assert stockLocation != null;
    int locationId = stockLocation.getLocationId();
    for (StockEntry stockEntry : stockEntries) {
      if (stockEntry.getLocationIdInt() == locationId) {
        filteredStockEntries.add(stockEntry);
      }
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.STOCK_ENTRIES,
        filteredStockEntries
    );
    bundle.putString(Constants.ARGUMENT.SELECTED_ID, selectedId);
    showBottomSheet(new StockEntriesBottomSheet(), bundle);
  }

  public void showStockLocationsBottomSheet() {  // from location
    if (!formData.isProductNameValid()) {
      return;
    }
    List<StockLocation> stockLocations = formData.getStockLocations();
    StockLocation currentStockLocation = formData.getFromLocationLive().getValue();
    int selectedId = currentStockLocation != null ? currentStockLocation.getLocationId() : -1;
    ProductDetails productDetails = formData.getProductDetailsLive().getValue();
    QuantityUnit quantityUnitStock = formData.getQuantityUnitStockLive().getValue();
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.STOCK_LOCATIONS,
        new ArrayList<>(stockLocations)
    );
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedId);
    bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
    bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnitStock);
    bundle.putString(ARGUMENT.TITLE, getString(R.string.title_location_from));
    showBottomSheet(new StockLocationsBottomSheet(), bundle);
  }

  public void showLocationsBottomSheet(boolean hasFocus) {  // to location
    if (!hasFocus) {
      return;
    }
    if (!formData.isProductNameValid()) {
      return;
    }
    Location currentToLocation = formData.getToLocationLive().getValue();
    int selectedId = currentToLocation != null ? currentToLocation.getId() : -1;
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, new ArrayList<>(locations));
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedId);
    bundle.putString(ARGUMENT.TITLE, getString(R.string.title_location_to));
    showBottomSheet(new LocationsBottomSheet(), bundle);
  }

  public void showConfirmationBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.TEXT, formData.getConfirmationText());
    showBottomSheet(new QuickModeConfirmBottomSheet(), bundle);
  }

  private void showMessageAndContinueScanning(String msg) {
    formData.clearForm();
    showMessage(msg);
    sendEvent(Event.CONTINUE_SCANNING);
  }

  private void showMessageAndContinueScanning(@StringRes int msg) {
    showMessageAndContinueScanning(getString(msg));
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setQueueEmptyAction(Runnable queueEmptyAction) {
    this.queueEmptyAction = queueEmptyAction;
  }

  public void setProductWillBeFilled(boolean productWillBeFilled) {
    this.productWillBeFilled = productWillBeFilled;
  }

  public boolean isProductWillBeFilled() {
    return productWillBeFilled;
  }

  public boolean isQuickModeEnabled() {
    if (quickModeEnabled.getValue() == null) {
      return false;
    }
    return quickModeEnabled.getValue();
  }

  public MutableLiveData<Boolean> getQuickModeEnabled() {
    return quickModeEnabled;
  }

  public boolean toggleQuickModeEnabled() {
    quickModeEnabled.setValue(!isQuickModeEnabled());
    sendEvent(isQuickModeEnabled() ? Event.QUICK_MODE_ENABLED : Event.QUICK_MODE_DISABLED);
    sharedPrefs.edit()
        .putBoolean(Constants.PREF.QUICK_MODE_ACTIVE_TRANSFER, isQuickModeEnabled())
        .apply();
    return true;
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

  public static class TransferViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final TransferFragmentArgs args;

    public TransferViewModelFactory(Application application, TransferFragmentArgs args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new TransferViewModel(application, args);
    }
  }
}
