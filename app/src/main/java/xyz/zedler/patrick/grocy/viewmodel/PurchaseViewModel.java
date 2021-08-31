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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.DateBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetNew;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuickModeConfirmBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnJSONArrayResponseListener;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataPurchase;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.PurchaseRepository;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class PurchaseViewModel extends BaseViewModel {

  private static final String TAG = PurchaseViewModel.class.getSimpleName();
  private final SharedPreferences sharedPrefs;
  private final boolean debug;

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final PurchaseRepository repository;
  private final FormDataPurchase formData;

  private ArrayList<Product> products;
  private HashMap<Integer, Product> productHashMap;
  private ArrayList<QuantityUnit> quantityUnits;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private ArrayList<QuantityUnitConversion> unitConversions;
  private HashMap<Integer, ArrayList<QuantityUnitConversion>> unitConversionHashMap;
  private HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private ArrayList<ProductBarcode> barcodes;
  private ArrayList<Store> stores;
  private ArrayList<Location> locations;
  private ArrayList<ShoppingListItem> shoppingListItems;
  private HashMap<Integer, ShoppingListItem> shoppingListItemHashMap;
  private ArrayList<Integer> batchShoppingListItemIds;

  private final LiveData<List<PendingProduct>> pendingProductsLive;
  private final LiveData<List<PendingProductBarcode>> pendingProductBarcodesLive;
  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> quickModeEnabled;

  private Runnable queueEmptyAction;

  public PurchaseViewModel(@NonNull Application application, PurchaseFragmentArgs args) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new PurchaseRepository(application);
    formData = new FormDataPurchase(application, sharedPrefs, args);
    AppDatabase appDatabase = AppDatabase.getAppDatabase(application);
    pendingProductsLive = appDatabase.pendingProductDao().getAllLive();
    pendingProductBarcodesLive = appDatabase.pendingProductBarcodeDao().getAllLive();

    if (args.getShoppingListItems() != null) {
      batchShoppingListItemIds = new ArrayList<>(args.getShoppingListItems().length);
      for (int i : args.getShoppingListItems()) {
        batchShoppingListItemIds.add(i);
      }
    }

    infoFullscreenLive = new MutableLiveData<>();
    boolean quickModeStart;
    if (args.getStartWithScanner()) {
      quickModeStart = true;
    } else if (!args.getCloseWhenFinished()) {
      quickModeStart = sharedPrefs.getBoolean(
          Constants.PREF.QUICK_MODE_ACTIVE_PURCHASE,
          false
      );
    } else {
      quickModeStart = false;
    }
    quickModeEnabled = new MutableLiveData<>(quickModeStart);
  }

  public FormDataPurchase getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase((products, barcodes, qUs, conversions, stores,
        locations, shoppingListItems) -> {
      this.products = products;
      formData.getProductsLive().setValue(getActiveProductsOnly(products));
      productHashMap = ArrayUtil.getProductsHashMap(products);
      this.barcodes = barcodes;
      this.quantityUnits = qUs;
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits);
      this.unitConversions = conversions;
      unitConversionHashMap = ArrayUtil.getUnitConversionsHashMap(unitConversions);
      this.stores = stores;
      this.locations = locations;
      this.shoppingListItems = shoppingListItems;
      shoppingListItemHashMap = ArrayUtil.getShoppingListItemHashMap(shoppingListItems);
      fillShoppingListItemAmountsHashMap();
      if (downloadAfterLoading) {
        downloadData();
      }
    }, batchShoppingListItemIds != null);
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

    DownloadHelper.Queue queue = dlHelper.newQueue(() -> onQueueEmpty(true), this::onDownloadError);
    queue.append(
        dlHelper.updateProducts(dbChangedTime, products -> {
          this.products = products;
          productHashMap = ArrayUtil.getProductsHashMap(products);
          formData.getProductsLive().setValue(getActiveProductsOnly(products));
        }), dlHelper.updateQuantityUnitConversions(dbChangedTime, conversions -> {
          this.unitConversions = conversions;
          unitConversionHashMap = ArrayUtil.getUnitConversionsHashMap(unitConversions);
        }), dlHelper.updateProductBarcodes(
            dbChangedTime, barcodes -> this.barcodes = barcodes
        ), dlHelper.updateQuantityUnits(dbChangedTime, quantityUnits -> {
          this.quantityUnits = quantityUnits;
          quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits);
        }), dlHelper.updateStores(
            dbChangedTime, stores -> this.stores = stores
        ), dlHelper.updateLocations(
            dbChangedTime, locations -> this.locations = locations
        )
    );
    if (batchShoppingListItemIds != null) {
      dlHelper.updateShoppingListItems(dbChangedTime, items -> {
        this.shoppingListItems = items;
        shoppingListItemHashMap = ArrayUtil.getShoppingListItemHashMap(shoppingListItems);
      });
    }
    if (queue.isEmpty()) {
      onQueueEmpty(false);
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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty(boolean offlineDataUpdated) {
    fillShoppingListItemAmountsHashMap();
    if (offlineDataUpdated) {
      repository.updateDatabase(products, barcodes,
          quantityUnits, unitConversions, stores, locations, shoppingListItems);
    }
    if (queueEmptyAction != null) {
      queueEmptyAction.run();
      queueEmptyAction = null;
      return;
    }
    if (batchShoppingListItemIds != null) {
      fillWithShoppingListItem();
    }
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
  }

  public void setProduct(
      @Nullable Integer productId,
      @Nullable ProductBarcode barcode,
      @Nullable ShoppingListItem shoppingListItem
  ) {
    if (productId == null && barcode == null && shoppingListItem == null) {
      return;
    }
    if (productId == null && barcode != null) {
      return;
    }

    DownloadHelper.OnProductDetailsResponseListener listener = productDetails -> {
      Product updatedProduct = productDetails.getProduct();
      formData.getProductDetailsLive().setValue(productDetails);
      formData.getProductNameLive().setValue(updatedProduct.getName());

      // quantity unit
      double initialUnitFactor;
      try {
        Integer forcedQuId = null;
        if (barcode != null && barcode.hasQuId()) {
          forcedQuId = barcode.getQuIdInt();
        } else if (shoppingListItem != null && shoppingListItem.hasQuId()) {
          forcedQuId = shoppingListItem.getQuIdInt();
        }
        initialUnitFactor = setProductQuantityUnitsAndFactors(updatedProduct, forcedQuId);
      } catch (IllegalArgumentException e) {
        showMessageAndContinueScanning(e.getMessage());
        return;
      }

      // amount
      boolean isTareWeightEnabled = formData.isTareWeightEnabled();
      if (!isTareWeightEnabled && barcode != null && barcode.hasAmount()) {
        // if barcode contains amount, take this (with tare weight handling off)
        // quick mode status doesn't matter
        formData.getAmountLive().setValue(NumUtil.trim(barcode.getAmountDouble()));
      } else if (!isTareWeightEnabled && shoppingListItem != null) {
        Double amountInUnit = AmountUtil.getShoppingListItemAmount(
            shoppingListItem, productHashMap, quantityUnitHashMap, unitConversionHashMap
        );
        if (amountInUnit != null) {
          formData.getAmountLive().setValue(NumUtil.trim(amountInUnit));
        } else {
          formData.getAmountLive().setValue(NumUtil.trim(shoppingListItem.getAmountDouble()));
        }
      } else if (!isTareWeightEnabled && !isQuickModeEnabled()) {
        String defaultAmount = sharedPrefs.getString(
            Constants.SETTINGS.STOCK.DEFAULT_PURCHASE_AMOUNT,
            Constants.SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT
        );
        if (NumUtil.isStringDouble(defaultAmount)) {
          defaultAmount = NumUtil.trim(Double.parseDouble(defaultAmount));
        }
        if (NumUtil.isStringDouble(defaultAmount)
            && Double.parseDouble(defaultAmount) > 0) {
          formData.getAmountLive().setValue(defaultAmount);
        }
      } else if (!isTareWeightEnabled) {
        // if quick mode enabled, always fill with amount 1
        formData.getAmountLive().setValue(NumUtil.trim(1));
      }

      // purchased date
      if (formData.getPurchasedDateEnabled()) {
        formData.getPurchasedDateLive().setValue(DateUtil.getDateStringToday());
      }

      // due days
      int dueDays = productDetails.getProduct().getDefaultDueDaysInt();
      if (dueDays < 0) {
        formData.getDueDateLive().setValue(Constants.DATE.NEVER_OVERDUE);
      } else if (dueDays == 0) {
        formData.getDueDateLive().setValue(null);
      } else {
        formData.getDueDateLive()
            .setValue(DateUtil.getTodayWithDaysAdded(dueDays));
      }

      // price
      String lastPrice;
      if (barcode != null && barcode.hasLastPrice()) {
        // if barcode contains last price, take this
        lastPrice = barcode.getLastPrice();
      } else {
        lastPrice = productDetails.getLastPrice();
      }
      if (lastPrice != null && !lastPrice.isEmpty()) {
        lastPrice = NumUtil.trimPrice(Double.parseDouble(lastPrice) * initialUnitFactor);
      }
      formData.getPriceLive().setValue(lastPrice);

      // store
      String storeId;
      if (barcode != null && barcode.hasStoreId()) {
        // if barcode contains store, take this
        storeId = barcode.getStoreId();
      } else {
        storeId = productDetails.getLastShoppingLocationId();
      }
      if (!NumUtil.isStringInt(storeId)) {
        storeId = productDetails.getDefaultShoppingLocationId();
      }
      Store store = NumUtil.isStringInt(storeId)
          ? getStore(Integer.parseInt(storeId)) : null;
      formData.getStoreLive().setValue(store);
      formData.getShowStoreSection().setValue(store != null || !stores.isEmpty());

      // location
      formData.getLocationLive().setValue(productDetails.getLocation());

      formData.isFormValid();
      if (isQuickModeEnabled()) {
        sendEvent(Event.FOCUS_INVALID_VIEWS);
      }
    };

    if (productId == null && shoppingListItem.hasProduct()) {
      productId = shoppingListItem.getProductIdInt();
    } else if (productId == null) {
      formData.getAmountLive().setValue(NumUtil.trim(shoppingListItem.getAmountDouble()));
      return;
    }
    dlHelper.getProductDetails(
        productId,
        listener,
        error -> showMessageAndContinueScanning(getString(R.string.error_no_product_details))
    ).perform(dlHelper.getUuid());
  }

  private double setProductQuantityUnitsAndFactors( // returns factor for unit which was set
      Product product,
      Integer forcedQuId
  ) {
    QuantityUnit stock = quantityUnitHashMap.get(product.getQuIdStockInt());
    QuantityUnit purchase = quantityUnitHashMap.get(product.getQuIdPurchaseInt());

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
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || quIdsInHashMap.contains(unit.getId())) {
        continue;
      }
      unitFactors.put(unit, conversion.getFactor());
    }
    formData.getQuantityUnitsFactorsLive().setValue(unitFactors);

    QuantityUnit forcedUnit = null;
    if (forcedQuId != null) {
      forcedUnit = quantityUnitHashMap.get(forcedQuId);
    }
    Double factor;
    if (forcedUnit != null && unitFactors.containsKey(forcedUnit)) {
      formData.getQuantityUnitLive().setValue(forcedUnit);
      factor = unitFactors.get(forcedUnit);
    } else {
      formData.getQuantityUnitLive().setValue(purchase);
      factor = unitFactors.get(purchase);
    }
    return factor != null && factor != -1 ? factor : 1;
  }

  public void onBarcodeRecognized(String barcode) {
    Product product = null;
    Grocycode grocycode = GrocycodeUtil.getGrocycode(barcode);
    if (grocycode != null && grocycode.isProduct()) {
      product = productHashMap.get(grocycode.getObjectId());
      if (product == null) {
        showMessageAndContinueScanning(R.string.msg_not_found);
        return;
      }
    } else if (grocycode != null) {
      showMessageAndContinueScanning(R.string.error_wrong_grocycode_type);
      return;
    }
    ProductBarcode productBarcode = null;
    if (product == null) {
      for (ProductBarcode code : barcodes) {
        if (code.getBarcode().equals(barcode)) {
          productBarcode = code;
          product = productHashMap.get(code.getProductId());
        }
      }
    }
    if (product != null) {
      setProduct(product.getId(), productBarcode, null);
    } else {
      if (isQuickModeEnabled()) {
        PendingProduct pendingProduct = PendingProduct
            .getFromBarcode(pendingProductsLive, pendingProductBarcodesLive, barcode);
        if (pendingProduct != null) {
          pendingProduct.addAmount(1);
          repository.insertPendingProduct(pendingProduct);
          sendEvent(Event.TRANSACTION_SUCCESS);
          showMessage("Amount + 1 for " + pendingProduct.getProductName());
          return;
        } else if(isOpenFoodFactsEnabled()) {
          dlHelper.getOpenFoodFactsProductName(barcode, productName -> {
            PendingProduct pendingProduct1 = PendingProduct
                .getFromName(pendingProductsLive, productName);
            if (pendingProduct1 == null) {
              repository.insertPendingProduct(
                  new PendingProduct(productName, true),
                  id -> {
                    repository.insertPendingProductBarcode(new PendingProductBarcode(id, barcode));
                    showMessageMainThread("New pending product: " + productName);
                  }, () -> showMessageMainThread("Error, could not store pending product: " + productName)
              );
            } else {
              repository.insertPendingProductBarcode(
                  new PendingProductBarcode(pendingProduct1.getId(), barcode)
              );
              showMessage("Store new barcode for " + productName);
            }
            sendEvent(Event.TRANSACTION_SUCCESS);
          }, error-> {
            formData.getBarcodeLive().setValue(barcode);
            formData.isFormValid();
            sendEvent(Event.FOCUS_INVALID_VIEWS);
          });
          return;
        }
      }
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
    Product product = Product.getProductFromName(products, input);

    if (product == null) {
      ProductBarcode productBarcode = null;
      for (ProductBarcode code : barcodes) {
        if (code.getBarcode().equals(input.trim())) {
          productBarcode = code;
          product = productHashMap.get(code.getProductId());
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

  public void fillWithShoppingListItem() {
    if (batchShoppingListItemIds == null) {
      return;
    }
    if (formData.getBatchModeItemIndexLive().getValue() == null) {
      return;
    }
    int index = formData.getBatchModeItemIndexLive().getValue();
    if (index >= batchShoppingListItemIds.size()) {
      return;
    }
    int currentItemId = batchShoppingListItemIds.get(index);
    if (shoppingListItemHashMap == null) {
      return;
    }
    ShoppingListItem currentItem = shoppingListItemHashMap.get(currentItemId);
    if (currentItem == null) {
      return;
    }
    formData.getShoppingListItemLive().setValue(currentItem);
    setProduct(null, null, currentItem);
  }

  public boolean batchModeNextItem() {  // also returns whether there was a next item
    if (batchShoppingListItemIds == null) {
      return false;
    }
    if (formData.getBatchModeItemIndexLive().getValue() == null) {
      return false;
    }
    int index = formData.getBatchModeItemIndexLive().getValue();
    if (index >= batchShoppingListItemIds.size() - 1) {
      return false;
    }
    formData.getBatchModeItemIndexLive().setValue(index + 1);
    fillWithShoppingListItem();
    return true;
  }

  public void purchaseProduct() {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }
    if (formData.getBarcodeLive().getValue() != null) {
      uploadProductBarcode(this::purchaseProduct);
      return;
    }

    assert formData.getProductDetailsLive().getValue() != null;
    Product product = formData.getProductDetailsLive().getValue().getProduct();
    JSONObject body = formData.getFilledJSONObject();

    OnJSONArrayResponseListener onResponse = response -> {
      // UNDO OPTION
      String transactionId = null;
      double amountPurchased = 0;
      try {
        transactionId = response.getJSONObject(0)
            .getString("transaction_id");
        for (int i = 0; i < response.length(); i++) {
          amountPurchased += response.getJSONObject(i).getDouble("amount");
        }
      } catch (JSONException e) {
        if (debug) {
          Log.e(TAG, "purchaseProduct: " + e);
        }
      }
      if (debug) {
        Log.i(TAG, "purchaseProduct: transaction successful");
      }

      SnackbarMessage snackbarMessage = new SnackbarMessage(
          formData.getTransactionSuccessMsg(amountPurchased)
      );
      if (transactionId != null) {
        String transId = transactionId;
        ShoppingListItem shoppingListItem = formData.getShoppingListItemLive().getValue();
        snackbarMessage.setAction(
            getString(R.string.action_undo),
            v -> undoTransaction(transId, shoppingListItem)
        );
        snackbarMessage.setDurationSecs(20);
      }
      showSnackbar(snackbarMessage);
      sendEvent(Event.TRANSACTION_SUCCESS);
    };

    dlHelper.postWithArray(
        grocyApi.purchaseProduct(product.getId()),
        body,
        response -> {
          ShoppingListItem shoppingListItem = formData.getShoppingListItemLive().getValue();
          if (batchShoppingListItemIds != null && shoppingListItem != null) {
            deleteShoppingListItem(shoppingListItem.getId(), () -> onResponse.onResponse(response));
          } else {
            onResponse.onResponse(response);
          }
        },
        error -> {
          showErrorMessage();
          if (debug) {
            Log.i(TAG, "purchaseProduct: " + error);
          }
        }
    );
  }

  private void undoTransaction(String transactionId, @Nullable ShoppingListItem shoppingListItem) {
    dlHelper.post(
        grocyApi.undoStockTransaction(transactionId),
        success -> {
          showMessage(getString(R.string.msg_undone_transaction));
          if (shoppingListItem != null) undoDeleteShoppingListItem(shoppingListItem);
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

  private void deleteShoppingListItem(int itemId, @NonNull Runnable onFinish) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, itemId),
        response -> onFinish.run(),
        error -> {
          if (debug) {
            Log.e(TAG, "deleteShoppingListItem: " + error);
          }
          onFinish.run();
        }
    );
  }

  private void undoDeleteShoppingListItem(@NonNull ShoppingListItem item) {
    dlHelper.post(
        grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
        item.getJsonFromShoppingListItem(true, debug, TAG),
        response -> {},
        error -> {
          if (debug) {
            Log.e(TAG, "undoDeleteShoppingListItem: " + error);
          }
        }
    );
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
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

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  private Store getStore(int id) {
    for (Store store : stores) {
      if (store.getId() == id) {
        return store;
      }
    }
    return null;
  }

  private void fillShoppingListItemAmountsHashMap() {
    shoppingListItemAmountsHashMap = new HashMap<>();
    if (shoppingListItems == null) {
      return;
    }
    for (ShoppingListItem item : shoppingListItems) {
      Double amount = AmountUtil.getShoppingListItemAmount(
          item, productHashMap, quantityUnitHashMap, unitConversionHashMap
      );
      if (amount != null) {
        shoppingListItemAmountsHashMap.put(item.getId(), amount);
      }
    }
  }

  public HashMap<Integer, Double> getShoppingListItemAmountsHashMap() {
    return shoppingListItemAmountsHashMap;
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

  public void showPurchasedDateBottomSheet() {
    if (!formData.isProductNameValid()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.DEFAULT_DAYS_FROM_NOW, String.valueOf(0));
    bundle.putString(
        Constants.ARGUMENT.SELECTED_DATE,
        formData.getPurchasedDateLive().getValue()
    );
    bundle.putInt(DateBottomSheet.DATE_TYPE, DateBottomSheet.PURCHASED_DATE);
    showBottomSheet(new DateBottomSheet(), bundle);
  }

  public void showDueDateBottomSheet(boolean hasFocus) {
    ProductDetails productDetails = formData.getProductDetailsLive().getValue();
    if (!hasFocus || !formData.isProductNameValid() || productDetails == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(
        Constants.ARGUMENT.DEFAULT_DAYS_FROM_NOW,
        String.valueOf(productDetails.getProduct().getDefaultDueDaysInt())
    );
    bundle.putString(
        Constants.ARGUMENT.SELECTED_DATE,
        formData.getDueDateLive().getValue()
    );
    bundle.putInt(DateBottomSheet.DATE_TYPE, DateBottomSheet.DUE_DATE);
    showBottomSheet(new DateBottomSheet(), bundle);
  }

  public void showStoresBottomSheet() {
    if (!formData.isProductNameValid() || stores == null || stores.isEmpty()) {
      return;
    }
    Bundle bundle = new Bundle();
    if (stores.get(0).getId() != -1) {
      stores.add(0, new Store(-1, getString(R.string.subtitle_none_selected)));
    }
    bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, stores);
    bundle.putInt(
        Constants.ARGUMENT.SELECTED_ID,
        formData.getStoreLive().getValue() != null
            ? formData.getStoreLive().getValue().getId()
            : -1
    );
    showBottomSheet(new StoresBottomSheet(), bundle);
  }

  public void showLocationsBottomSheet() {
    if (!formData.isProductNameValid()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
    bundle.putInt(
        Constants.ARGUMENT.SELECTED_ID,
        formData.getLocationLive().getValue() != null
            ? formData.getLocationLive().getValue().getId()
            : -1
    );
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

  public LiveData<List<PendingProduct>> getPendingProductsLive() {
    return pendingProductsLive;
  }

  public LiveData<List<PendingProductBarcode>> getPendingProductBarcodesLive() {
    return pendingProductBarcodesLive;
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
        .putBoolean(Constants.PREF.QUICK_MODE_ACTIVE_PURCHASE, isQuickModeEnabled())
        .apply();
    return true;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class PurchaseViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final PurchaseFragmentArgs args;

    public PurchaseViewModelFactory(Application application, PurchaseFragmentArgs args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new PurchaseViewModel(application, args);
    }
  }
}
