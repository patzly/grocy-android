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
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.form.FormDataMasterProduct;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class MasterProductViewModel extends BaseViewModel {

  private static final String TAG = MasterProductViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MasterProductRepository repository;
  private final FormDataMasterProduct formData;

  private final MutableLiveData<List<PendingProductBarcode>> pendingProductBarcodesLive;
  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;

  private List<Product> products;
  private List<ProductBarcode> productBarcodes;
  private List<PendingProductBarcode> pendingProductBarcodes;

  private NetworkQueue.QueueItem extraQueueItem;
  private final boolean debug;
  private final MutableLiveData<Boolean> actionEditLive;
  private final MasterProductFragmentArgs args;
  private final boolean forceSaveWithClose;

  public MasterProductViewModel(
      @NonNull Application application,
      @NonNull MasterProductFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    args = startupArgs;
    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new MasterProductRepository(application);
    formData = new FormDataMasterProduct(application, getBeginnerModeEnabled());
    actionEditLive = new MutableLiveData<>();
    actionEditLive.setValue(args.getAction().equals(Constants.ACTION.EDIT));
    forceSaveWithClose = !isActionEdit() && args.getProductName() != null;

    pendingProductBarcodesLive = new MutableLiveData<>();
    infoFullscreenLive = new MutableLiveData<>();

    if (isActionEdit()) {
      if (args.getProduct() != null) {
        setCurrentProduct(args.getProduct());
      } else {
        assert args.getProductId() != null;
        int productId = Integer.parseInt(args.getProductId());
        extraQueueItem = ProductDetails.getProductDetails(dlHelper, productId, productDetails -> {
          extraQueueItem = null;
          formData.getProductNamesLive().setValue(
              getProductNames(products, productDetails.getProduct().getName())
          );
          setCurrentProduct(productDetails.getProduct());
        });
      }
    } else if (args.getProduct() != null || NumUtil.isStringInt(args.getProductId())) {  // on clone
      if (args.getProduct() != null) {
        Product product = args.getProduct();
        formData.getMessageCopiedFromLive()
            .setValue(getString(R.string.msg_data_copied_from_product, product.getName()));
        if (args.getProductName() != null) {
          product.setName(args.getProductName());
        } else {
          product.setName(null);
          sendEvent(Event.FOCUS_INVALID_VIEWS);
        }
        setCurrentProduct(product);
      } else {
        assert args.getProductId() != null;
        int productId = Integer.parseInt(args.getProductId());
        extraQueueItem = ProductDetails.getProductDetails(dlHelper, productId, productDetails -> {
          extraQueueItem = null;
          Product product = productDetails.getProduct();
          formData.getMessageCopiedFromLive()
              .setValue(getString(R.string.msg_data_copied_from_product, product.getName()));
          if (args.getProductName() != null) {
            product.setName(args.getProductName());
          } else {
            product.setName(null);
            sendEvent(Event.FOCUS_INVALID_VIEWS);
          }
          setCurrentProduct(product);
        });
      }
    } else {
      Product product = new Product(sharedPrefs);
      if (args.getProductName() != null) {
        product.setName(args.getProductName());
      } else {
        sendEvent(Event.FOCUS_INVALID_VIEWS);
      }
      setCurrentProduct(product);
    }
  }

  public boolean isActionEdit() {
    assert actionEditLive.getValue() != null;
    return actionEditLive.getValue();
  }

  public MutableLiveData<Boolean> getActionEditLive() {
    return actionEditLive;
  }

  public String getAction() {
    return isActionEdit() ? ACTION.EDIT : ACTION.CREATE;
  }

  public FormDataMasterProduct getFormData() {
    return formData;
  }

  public void setCurrentProduct(Product product) {
    formData.getProductLive().setValue(product);
    formData.isFormValid();
  }

  public Product getFilledProduct() {
    return formData.fillProduct(formData.getProductLive().getValue());
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.products = data.getProducts();
      this.productBarcodes = data.getBarcodes();
      this.pendingProductBarcodes = data.getPendingProductBarcodes();
      formData.getProductNamesLive().setValue(getProductNames(this.products, null));

      if (downloadAfterLoading) {
        downloadData(false);
      } else {
        onQueueEmpty();
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else {
            onQueueEmpty();
          }
        }, error -> onError(error, TAG),
        null,
        forceUpdate,
        false,
        extraQueueItem,
        Product.class,
        ProductBarcode.class
    );
  }

  private void onQueueEmpty() {
    if (args.getPendingProductBarcodes() != null) {
      ArrayList<PendingProductBarcode> filteredBarcodes = new ArrayList<>();
      String[] barcodeIds = args.getPendingProductBarcodes().split(",");
      for (PendingProductBarcode barcode : pendingProductBarcodes) {
        if (ArrayUtil.contains(barcodeIds, String.valueOf(barcode.getId()))) {
          filteredBarcodes.add(barcode);
        }
      }
      if (!filteredBarcodes.isEmpty()) {
        this.pendingProductBarcodesLive.setValue(filteredBarcodes);
      }
      removeBarcodesWhichExistOnline(productBarcodes);
    }
  }

  private ArrayList<String> getProductNames(List<Product> products, @Nullable String nameToRemove) {
    ArrayList<String> names = new ArrayList<>();
    for (Product product : products) {
      names.add(product.getName());
    }
    if (isActionEdit() && (formData.getProductLive().getValue() != null || nameToRemove != null)) {
      names.remove(nameToRemove != null
          ? nameToRemove
          : formData.getProductLive().getValue().getName());
    }
    return names;
  }

  public void saveProduct(boolean withClosing) {
    if (!formData.isWholeFormValid()) {
      showMessage(getString(R.string.error_missing_information));
      return;
    }

    Product product = getFilledProduct();
    JSONObject jsonObject = product.getJsonFromProduct(sharedPrefs, debug, TAG);

    if (isActionEdit()) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, product.getId()),
          jsonObject,
          response -> {
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.ARGUMENT.PRODUCT_ID, product.getId());
            sendEvent(Event.SET_PRODUCT_ID, bundle);
            sendEvent(Event.NAVIGATE_UP);
          },
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveProduct: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
          jsonObject,
          response -> {
            int objectId = -1;
            try {
              objectId = response.getInt("created_object_id");
              Log.i(TAG, "saveProduct: " + objectId);
            } catch (JSONException e) {
              if (debug) {
                Log.e(TAG, "saveProduct: " + e);
              }
            }
            if (withClosing) {
              if (objectId != -1) {
                Bundle bundle = new Bundle();
                bundle.putInt(Constants.ARGUMENT.PRODUCT_ID, objectId);
                sendEvent(Event.SET_PRODUCT_ID, bundle);
              }
              uploadBarcodesIfNecessary(objectId, () -> sendEvent(Event.NAVIGATE_UP));
            } else {
              int finalObjectId = objectId;
              uploadBarcodesIfNecessary(objectId, () -> {
                actionEditLive.setValue(true);
                product.setId(finalObjectId);
                setCurrentProduct(product);
                sendEvent(Event.TRANSACTION_SUCCESS);
              });
            }
          },
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveProduct: " + error);
            }
          }
      );
    }
  }

  private void uploadBarcodesIfNecessary(int productId, Runnable onFinished) {
    List<PendingProductBarcode> pendingProductBarcodes = pendingProductBarcodesLive.getValue();
    if (pendingProductBarcodes == null || pendingProductBarcodes.isEmpty() || productId < 0) {
      onFinished.run();
      return;
    }
    NetworkQueue queue = dlHelper.newQueue(
        updated -> {
          pendingProductBarcodesLive.setValue(null);
          onFinished.run();
        }, error -> onFinished.run()
    );
    for (PendingProductBarcode pendingProductBarcode : pendingProductBarcodes) {
      pendingProductBarcode.setPendingProductId(productId);
      queue.append(ProductBarcode.addProductBarcode(
          dlHelper,
          pendingProductBarcode.getJsonFromProductBarcode(debug, TAG),
          null, null
      ));
    }
    if (queue.getSize() == 0) {
      onFinished.run();
      return;
    }
    queue.start();
  }

  public void deleteProduct(int productId) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, productId),
        response -> sendEvent(Event.NAVIGATE_UP),
        error -> showMessage(getString(R.string.error_undefined))
    );
  }

  public MutableLiveData<List<PendingProductBarcode>> getPendingProductBarcodesLive() {
    return pendingProductBarcodesLive;
  }

  private void removeBarcodesWhichExistOnline(List<ProductBarcode> productBarcodes) {
    if (pendingProductBarcodesLive.getValue() == null) return;
    ArrayList<String> barcodeStrings = new ArrayList<>();
    for (ProductBarcode barcode : productBarcodes) {
      barcodeStrings.add(barcode.getBarcode());
    }
    ArrayList<PendingProductBarcode> filteredBarcodes = new ArrayList<>();
    for (PendingProductBarcode pendingProductBarcode : pendingProductBarcodesLive.getValue()) {
      if (barcodeStrings.contains(pendingProductBarcode.getBarcode())) continue;
      filteredBarcodes.add(pendingProductBarcode);
    }
    if (filteredBarcodes.isEmpty()) {
      pendingProductBarcodesLive.setValue(null);
    } else {
      pendingProductBarcodesLive.setValue(filteredBarcodes);
    }
  }

  public boolean isForceSaveWithClose() {
    return forceSaveWithClose;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class MasterProductViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductFragmentArgs args;

    public MasterProductViewModelFactory(
        Application application,
        MasterProductFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterProductViewModel(application, args);
    }
  }
}
