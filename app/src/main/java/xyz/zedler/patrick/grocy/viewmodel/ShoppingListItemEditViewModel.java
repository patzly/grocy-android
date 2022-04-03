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
import java.util.List;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.ShoppingListItemEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FormDataShoppingListItemEdit;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.repository.ShoppingListItemEditRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class ShoppingListItemEditViewModel extends BaseViewModel {

  private static final String TAG = ShoppingListItemEditViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ShoppingListItemEditRepository repository;
  private final FormDataShoppingListItemEdit formData;
  private final ShoppingListItemEditFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;

  private List<ShoppingList> shoppingLists;
  private List<Product> products;
  private List<ProductBarcode> barcodes;
  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversion> unitConversions;

  private DownloadHelper.Queue currentQueueLoading;
  private Runnable queueEmptyAction;
  private final boolean debug;
  private final boolean isActionEdit;

  public ShoppingListItemEditViewModel(
      @NonNull Application application,
      @NonNull ShoppingListItemEditFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new ShoppingListItemEditRepository(application);
    formData = new FormDataShoppingListItemEdit(application);
    args = startupArgs;
    isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
  }

  public FormDataShoppingListItemEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.shoppingLists = data.getShoppingLists();
      this.products = data.getProducts();
      this.barcodes = data.getBarcodes();
      this.quantityUnits = data.getQuantityUnits();
      this.unitConversions = data.getQuantityUnitConversions();
      formData.getProductsLive().setValue(Product.getActiveProductsOnly(products));
      if (!isActionEdit) {
        formData.getShoppingListLive().setValue(getLastShoppingList());
      }
      fillWithShoppingListItemIfNecessary();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateShoppingLists(dbChangedTime, shoppingLists -> {
          this.shoppingLists = shoppingLists;
          if (!isActionEdit) {
            formData.getShoppingListLive().setValue(getLastShoppingList());
          }
        }), dlHelper.updateProducts(dbChangedTime, products -> {
          this.products = products;
          formData.getProductsLive().setValue(Product.getActiveProductsOnly(products));
        }), dlHelper.updateQuantityUnitConversions(
            dbChangedTime, conversions -> this.unitConversions = conversions
        ), dlHelper.updateProductBarcodes(
            dbChangedTime, barcodes -> this.barcodes = barcodes
        ), dlHelper.updateQuantityUnits(
            dbChangedTime, quantityUnits -> this.quantityUnits = quantityUnits
        )
    );
    if (queue.isEmpty()) {
      onQueueEmpty();
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    if (queueEmptyAction != null) {
      queueEmptyAction.run();
      queueEmptyAction = null;
      return;
    }
    fillWithShoppingListItemIfNecessary();
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
  }

  public void saveItem() {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }

    ShoppingListItem item = null;
    if (isActionEdit) {
      item = args.getShoppingListItem();
    }
    item = formData.fillShoppingListItem(item);
    JSONObject jsonObject = ShoppingListItem.getJsonFromShoppingListItem(item, false,
        debug, TAG);

    if (isActionEdit) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, item.getId()),
          jsonObject,
          response -> saveProductBarcodeAndNavigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveItem: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
          jsonObject,
          response -> saveProductBarcodeAndNavigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveItem: " + error);
            }
          }
      );
    }
  }

  private void saveProductBarcodeAndNavigateUp() {
    ProductBarcode productBarcode = formData.fillProductBarcode(null);
    if (productBarcode.getBarcode() == null) {
      navigateUp();
      return;
    }
    dlHelper.addProductBarcode(
        ProductBarcode.getJsonFromProductBarcode(productBarcode, debug, TAG),
        this::navigateUp,
        error -> navigateUp()
    ).perform(dlHelper.getUuid());
  }

  private void fillWithShoppingListItemIfNecessary() {
    if (!isActionEdit || formData.isFilledWithShoppingListItem()) {
      return;
    }

    ShoppingListItem item = args.getShoppingListItem();
    assert item != null;

    ShoppingList shoppingList = getShoppingList(item.getShoppingListIdInt());
    formData.getShoppingListLive().setValue(shoppingList);

    double amount = item.getAmountDouble();
    QuantityUnit quantityUnit = null;

    Product product = item.getProductId() != null ? getProduct(item.getProductIdInt()) : null;
    if (product != null) {
      formData.getProductLive().setValue(product);
      formData.getProductNameLive().setValue(product.getName());
      HashMap<QuantityUnit, Double> unitFactors = setProductQuantityUnitsAndFactors(product);

      quantityUnit = getQuantityUnit(item.getQuIdInt());
      if (unitFactors != null && quantityUnit != null && unitFactors.containsKey(quantityUnit)) {
        Double factor = unitFactors.get(quantityUnit);
        assert factor != null;
        if (factor != -1 && quantityUnit.getId() == product.getQuIdPurchaseInt()) {
          amount = amount / factor;
        } else if (factor != -1) {
          amount = amount * factor;
        }
      }
    }
    formData.getAmountLive().setValue(NumUtil.trim(amount));
    formData.getQuantityUnitLive().setValue(quantityUnit);

    formData.getNoteLive().setValue(item.getNote());
    formData.setFilledWithShoppingListItem(true);
  }

  private HashMap<QuantityUnit, Double> setProductQuantityUnitsAndFactors(Product product) {
    QuantityUnit stock = getQuantityUnit(product.getQuIdStockInt());
    QuantityUnit purchase = getQuantityUnit(product.getQuIdPurchaseInt());

    if (stock == null || purchase == null) {
      showMessage(getString(R.string.error_loading_qus));
      return null;
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

    if (!isActionEdit) {
      formData.getQuantityUnitLive().setValue(purchase);
    }
    return unitFactors;
  }

  public void setProduct(Product product) {
    if (product == null) {
      return;
    }
    formData.getProductLive().setValue(product);
    formData.getProductNameLive().setValue(product.getName());
    setProductQuantityUnitsAndFactors(product);
    formData.isFormValid();
  }

  public void setProduct(int productId) {
    if (products == null) {
      return;
    }
    Product product = getProduct(productId);
    if (product == null) {
      return;
    }
    setProduct(product);
  }

  public void onBarcodeRecognized(String barcode) {
    Product product = null;
    Grocycode grocycode = GrocycodeUtil.getGrocycode(barcode);
    if (grocycode != null && grocycode.isProduct()) {
      product = Product.getProductFromId(products, grocycode.getObjectId());
      if (product == null) {
        formData.clearForm();
        showMessage(R.string.msg_not_found);
        return;
      }
    } else if (grocycode != null) {
      formData.clearForm();
      showMessage(R.string.error_wrong_grocycode_type);
      return;
    }
    if (product == null) {
      product = Product.getProductFromBarcode(products, barcodes, barcode);
    }
    if (product != null) {
      setProduct(product);
    } else {
      formData.getBarcodeLive().setValue(barcode);
      formData.isFormValid();
    }
  }

  public void showProductDetailsBottomSheet() {
    Product product = checkProductInput();
    if (product == null) {
      return;
    }
    dlHelper.getProductDetails(product.getId(), details -> showBottomSheet(
        new ProductOverviewBottomSheet(),
        new ProductOverviewBottomSheetArgs.Builder()
            .setProductDetails(details).build().toBundle()
    )).perform(dlHelper.getUuid());
  }

  public void deleteItem() {
    if (!isActionEdit()) {
      return;
    }
    ShoppingListItem shoppingListItem = args.getShoppingListItem();
    assert shoppingListItem != null;
    dlHelper.delete(
        grocyApi.getObject(
            GrocyApi.ENTITY.SHOPPING_LIST,
            shoppingListItem.getId()
        ),
        response -> navigateUp(),
        this::showErrorMessage
    );
  }

  public Product checkProductInput() {
    formData.isProductNameValid();
    String input = formData.getProductNameLive().getValue();
    if (input == null || input.isEmpty()) {
      return null;
    }
    Product product = getProductFromName(input);

    Product currentProduct = formData.getProductLive().getValue();
    if (currentProduct != null && product != null && currentProduct.getId() == product.getId()) {
      return product;
    }

    if (product != null) {
      setProduct(product);
    } else {
      Bundle bundle = new Bundle();
      bundle.putString(Constants.ARGUMENT.PRODUCT_INPUT, input);
      showBottomSheet(new InputProductBottomSheet(), bundle);
    }
    return product;
  }

  private QuantityUnit getQuantityUnit(int id) {
    for (QuantityUnit quantityUnit : quantityUnits) {
      if (quantityUnit.getId() == id) {
        return quantityUnit;
      }
    }
    return null;
  }

  private ShoppingList getLastShoppingList() {
    int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
    return getShoppingList(lastId);
  }

  private ShoppingList getShoppingList(int id) {
    for (ShoppingList shoppingList : shoppingLists) {
      if (shoppingList.getId() == id) {
        return shoppingList;
      }
    }
    return null;
  }

  @Nullable
  public Product getProduct(int id) {
    for (Product product : products) {
      if (product.getId() == id) {
        return product;
      }
    }
    return null;
  }

  private Product getProductFromName(String name) {
    for (Product product : products) {
      if (product.getName().equals(name)) {
        return product;
      }
    }
    return null;
  }

  public boolean isActionEdit() {
    return isActionEdit;
  }

  public boolean isFeatureMultiShoppingListsEnabled() {
    return sharedPrefs.getBoolean(
        Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true
    );
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
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

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
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

  public static class ShoppingListItemEditViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final ShoppingListItemEditFragmentArgs args;

    public ShoppingListItemEditViewModelFactory(
        Application application,
        ShoppingListItemEditFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new ShoppingListItemEditViewModel(application, args);
    }
  }
}
