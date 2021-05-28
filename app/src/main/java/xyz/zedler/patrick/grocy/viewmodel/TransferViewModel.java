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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.TransferFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetNew;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuickModeConfirmBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockEntriesBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockLocationsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataTransfer;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.repository.TransferRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class TransferViewModel extends BaseViewModel {

  private static final String TAG = TransferViewModel.class.getSimpleName();
  private final SharedPreferences sharedPrefs;
  private final boolean debug;

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final TransferRepository repository;
  private final FormDataTransfer formData;

  private ArrayList<Product> products;
  private ArrayList<QuantityUnit> quantityUnits;
  private ArrayList<QuantityUnitConversion> unitConversions;
  private ArrayList<ProductBarcode> barcodes;
  private ArrayList<Location> locations;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> quickModeEnabled;

  private Runnable queueEmptyAction;

  public TransferViewModel(@NonNull Application application, TransferFragmentArgs args) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new TransferRepository(application);
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
    repository.loadFromDatabase((products, barcodes, locations, qUs, conversions) -> {
      this.products = products;
      this.barcodes = barcodes;
      this.locations = locations;
      this.quantityUnits = qUs;
      this.unitConversions = conversions;
      formData.getProductsLive().setValue(getActiveProductsOnly(products));
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData(@Nullable String dbChangedTime) {
        /*if(isOffline()) { // skip downloading
            isLoadingLive.setValue(false);
            return;
        }*/
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateProducts(dbChangedTime, products -> {
          this.products = products;
          formData.getProductsLive().setValue(getActiveProductsOnly(products));
        }), dlHelper.updateProductBarcodes(
            dbChangedTime, barcodes -> this.barcodes = barcodes
        ), dlHelper.updateLocations(
            dbChangedTime, locations -> this.locations = locations
        ), dlHelper.updateQuantityUnitConversions(
            dbChangedTime, conversions -> this.unitConversions = conversions
        ), dlHelper.updateQuantityUnits(
            dbChangedTime, quantityUnits -> this.quantityUnits = quantityUnits
        )
    );
    if (queue.isEmpty()) {
      if (queueEmptyAction != null) {
        queueEmptyAction.run();
        queueEmptyAction = null;
      }
      return;
    }

    //currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    repository.updateDatabase(products, barcodes, locations,
        quantityUnits, unitConversions, () -> {
        });
    if (queueEmptyAction != null) {
      queueEmptyAction.run();
      queueEmptyAction = null;
    }
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
  }

  public void setProduct(int productId, ProductBarcode barcode) {
    DownloadHelper.OnQueueEmptyListener onQueueEmptyListener = () -> {
      ProductDetails productDetails = formData.getProductDetailsLive().getValue();
      assert productDetails != null;
      Product product = productDetails.getProduct();

      if (productDetails.getStockAmountAggregated() == 0) {
        String name = product.getName();
        showMessage(getApplication().getString(R.string.msg_not_in_stock, name));
        formData.clearForm();
        return;
      }
      if (productDetails.getProduct().getEnableTareWeightHandlingBoolean()) {
        showMessage(getApplication().getString(R.string.msg_transfer_tare_weight));
        formData.clearForm();
        return;
      }

      formData.getProductDetailsLive().setValue(productDetails);
      formData.getProductNameLive().setValue(product.getName());

      // stock location (from location)
      ArrayList<StockLocation> stockLocations = formData.getStockLocations();
      StockLocation stockLocation = getStockLocation(
          stockLocations,
          product.getLocationIdInt()
      );
      if (stockLocation == null && !stockLocations.isEmpty()) {
        stockLocation = stockLocations.get(stockLocations.size() - 1);
      }
      formData.getFromLocationLive().setValue(stockLocation);

      // quantity unit
      try {
        setProductQuantityUnitsAndFactors(product, barcode);
      } catch (IllegalArgumentException e) {
        showMessage(e.getMessage());
        formData.clearForm();
        return;
      }

      // amount
      if (barcode != null && barcode.hasAmount()) {
        // if barcode contains amount, take this
        // quick mode status doesn't matter
        formData.getAmountLive().setValue(NumUtil.trim(barcode.getAmountDouble()));
      } else if (!isQuickModeEnabled()) {
        String defaultAmount = sharedPrefs.getString(
            Constants.SETTINGS.STOCK.DEFAULT_CONSUME_AMOUNT,
            Constants.SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT
        );
        if (NumUtil.isStringDouble(defaultAmount)) {
          defaultAmount = NumUtil.trim(Double.parseDouble(defaultAmount));
        }
        if (NumUtil.isStringDouble(defaultAmount)
            && Double.parseDouble(defaultAmount) > 0) {
          formData.getAmountLive().setValue(defaultAmount);
        }
      } else {
        // if quick mode enabled, always fill with amount 1
        formData.getAmountLive().setValue(NumUtil.trim(1));
      }

      // stock entry
      formData.getUseSpecificLive().setValue(false);
      formData.getSpecificStockEntryLive().setValue(null);

      formData.isFormValid();
      if (isQuickModeEnabled()) {
        sendEvent(Event.FOCUS_INVALID_VIEWS);
      }
    };

    dlHelper.newQueue(onQueueEmptyListener, error -> {
      showMessage(getString(R.string.error_no_product_details));
      formData.clearForm();
    }).append(
        dlHelper.getProductDetails(
            productId,
            productDetails -> formData.getProductDetailsLive().setValue(productDetails)
        ), dlHelper.getStockLocations(
            productId,
            formData::setStockLocations
        ), dlHelper.getStockEntries(
            productId,
            formData::setStockEntries
        )
    ).start();
  }

  private void setProductQuantityUnitsAndFactors(
      Product product,
      ProductBarcode barcode
  ) {
    QuantityUnit stock = getQuantityUnit(product.getQuIdStockInt());
    QuantityUnit purchase = getQuantityUnit(product.getQuIdPurchaseInt());

    if (stock == null || purchase == null) {
      throw new IllegalArgumentException(getString(R.string.error_loading_qus));
    }

    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    ArrayList<Integer> quIdsInHashMap = new ArrayList<>();
    unitFactors.put(stock, (double) -1);
    quIdsInHashMap.add(stock.getId());
    if (!quIdsInHashMap.contains(purchase.getId())) {
      unitFactors.put(purchase, product.getQuFactorPurchaseToStockDouble());
    }
    for (QuantityUnitConversion conversion : unitConversions) {
      if (product.getId() != conversion.getProductId()) {
        continue;
      }
      QuantityUnit unit = getQuantityUnit(conversion.getToQuId());
      if (unit == null || quIdsInHashMap.contains(unit.getId())) {
        continue;
      }
      unitFactors.put(unit, conversion.getFactor());
    }
    formData.getQuantityUnitsFactorsLive().setValue(unitFactors);

    QuantityUnit barcodeUnit = null;
    if (barcode != null && barcode.hasQuId()) {
      barcodeUnit = getQuantityUnit(barcode.getQuIdInt());
    }
    if (barcodeUnit != null && unitFactors.containsKey(barcodeUnit)) {
      formData.getQuantityUnitLive().setValue(barcodeUnit);
    } else {
      formData.getQuantityUnitLive().setValue(stock);
    }
  }

  public void onBarcodeRecognized(String barcode) {
    ProductBarcode productBarcode = null;
    Product product = null;
    for (ProductBarcode code : barcodes) {
      if (code.getBarcode().equals(barcode)) {
        productBarcode = code;
        product = getProduct(code.getProductId());
      }
    }
    if (product != null) {
      setProduct(product.getId(), productBarcode);
    } else {
      formData.getBarcodeLive().setValue(barcode);
      formData.isFormValid();
      sendEvent(Event.FOCUS_INVALID_VIEWS);
    }
  }

  public void checkProductInput() {
    formData.isProductNameValid();
    String input = formData.getProductNameLive().getValue();
    if (input == null || input.isEmpty()) {
      return;
    }
    Product product = getProductFromName(input);

    if (product == null) {
      ProductBarcode productBarcode = null;
      for (ProductBarcode code : barcodes) {
        if (code.getBarcode().equals(input.trim())) {
          productBarcode = code;
          product = getProduct(code.getProductId());
        }
      }
      if (product != null) {
        setProduct(product.getId(), productBarcode);
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
      setProduct(product.getId(), null);
    } else {
      showInputProductBottomSheet(input);
    }
  }

  public void addBarcodeToExistingProduct(String barcode) {
    formData.getBarcodeLive().setValue(barcode);
    formData.getProductNameLive().setValue(null);
  }

  public void transferProduct() {
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
            snackbarMessage.setDurationSecs(20);
          }
          showSnackbar(snackbarMessage);
          sendEvent(Event.CONSUME_SUCCESS);
        },
        error -> {
          showErrorMessage();
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
        error -> showErrorMessage()
    );
  }

  private void uploadProductBarcode(Runnable onSuccess) {
    ProductBarcode productBarcode = formData.fillProductBarcode();
    JSONObject body = productBarcode.getJsonFromProductBarcode(debug, TAG);
    dlHelper.addProductBarcode(body, () -> {
      formData.getBarcodeLive().setValue(null);
      barcodes.add(productBarcode); // add to list so it will be found on next scan without reload
      if (onSuccess != null) {
        onSuccess.run();
      }
    }, error -> showMessage(R.string.error_failed_barcode_upload)).perform(dlHelper.getUuid());
  }

  @Nullable
  public Product getProductFromName(@Nullable String name) {
    if (name == null) {
      return null;
    }
    for (Product product : products) {
      if (product.getName().equals(name)) {
        return product;
      }
    }
    return null;
  }

  public Product getProduct(int id) {
    for (Product product : products) {
      if (product.getId() == id) {
        return product;
      }
    }
    return null;
  }

  private ArrayList<Product> getActiveProductsOnly(ArrayList<Product> allProducts) {
    ArrayList<Product> activeProductsOnly = new ArrayList<>();
    for (Product product : allProducts) {
      if (product.isActive()) {
        activeProductsOnly.add(product);
      }
    }
    return activeProductsOnly;
  }

  private QuantityUnit getQuantityUnit(int id) {
    for (QuantityUnit quantityUnit : quantityUnits) {
      if (quantityUnit.getId() == id) {
        return quantityUnit;
      }
    }
    return null;
  }

  private StockLocation getStockLocation(ArrayList<StockLocation> locations, int locationId) {
    for (StockLocation stockLocation : locations) {
      if (stockLocation.getLocationId() == locationId) {
        return stockLocation;
      }
    }
    return null;
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
    showBottomSheet(new QuantityUnitsBottomSheetNew(), bundle);
  }

  public void showStockEntriesBottomSheet() {
    if (!formData.isProductNameValid()) {
      return;
    }
    ArrayList<StockEntry> stockEntries = formData.getStockEntries();
    StockEntry currentStockEntry = formData.getSpecificStockEntryLive().getValue();
    String selectedId = currentStockEntry != null ? currentStockEntry.getStockId() : null;
    ArrayList<StockEntry> filteredStockEntries = new ArrayList<>();
    StockLocation stockLocation = formData.getFromLocationLive().getValue();
    assert stockLocation != null;
    int locationId = stockLocation.getLocationId();
    for (StockEntry stockEntry : stockEntries) {
      if (stockEntry.getLocationId() == locationId) {
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
    ArrayList<StockLocation> stockLocations = formData.getStockLocations();
    StockLocation currentStockLocation = formData.getFromLocationLive().getValue();
    int selectedId = currentStockLocation != null ? currentStockLocation.getLocationId() : -1;
    ProductDetails productDetails = formData.getProductDetailsLive().getValue();
    QuantityUnit quantityUnitStock = formData.getQuantityUnitStockLive().getValue();
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.STOCK_LOCATIONS, stockLocations);
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedId);
    bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
    bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnitStock);
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
    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedId);
    showBottomSheet(new LocationsBottomSheet(), bundle);
  }

  public void showConfirmationBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.TEXT, formData.getConfirmationText());
    showBottomSheet(new QuickModeConfirmBottomSheet(), bundle);
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

  public boolean getUseFrontCam() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.FRONT_CAM,
        Constants.SETTINGS_DEFAULT.SCANNER.FRONT_CAM
    );
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
