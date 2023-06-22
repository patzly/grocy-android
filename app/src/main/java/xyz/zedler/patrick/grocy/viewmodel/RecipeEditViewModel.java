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
import android.text.Html;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.form.FormDataRecipeEdit;
import xyz.zedler.patrick.grocy.fragment.RecipeEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputProductBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.repository.RecipeEditRepository;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class RecipeEditViewModel extends BaseViewModel {

  private static final String TAG = RecipeEditViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final RecipeEditRepository repository;
  private final FormDataRecipeEdit formData;
  private final RecipeEditFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;

  private List<Product> products;
  private List<ProductBarcode> productBarcodes;
  private Recipe recipe;

  private final boolean debug;
  private final MutableLiveData<Boolean> actionEditLive;
  private final int maxDecimalPlacesAmount;

  public RecipeEditViewModel(
      @NonNull Application application,
      @NonNull RecipeEditFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(application, TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(application);
    repository = new RecipeEditRepository(application);
    formData = new FormDataRecipeEdit(application, sharedPrefs, startupArgs);
    args = startupArgs;
    actionEditLive = new MutableLiveData<>();
    actionEditLive.setValue(args.getAction().equals(Constants.ACTION.EDIT));
    recipe = args.getRecipe();
    infoFullscreenLive = new MutableLiveData<>();
  }

  public FormDataRecipeEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.products = data.getProducts();
      this.productBarcodes = data.getProductBarcodes();

      formData.getProductsLive().setValue(Product.getActiveProductsOnly(products));
      if (downloadAfterLoading) {
        downloadData(false);
      } else {
        fillWithRecipeIfNecessary();
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else {
            fillWithRecipeIfNecessary();
          }
        },
        error -> onError(error, null),
        forceUpdate,
        false,
        Product.class,
        ProductBarcode.class
    );
  }

  public void setProduct(Product product) {
    formData.getProductProducedLive().setValue(product);
    formData.getProductProducedNameLive().setValue(product.getName());
    formData.isFormValid();
  }

  public void onBarcodeRecognized(String barcode) {
    if (formData.getProductProducedLive().getValue() != null) {
      formData.getBarcodeLive().setValue(barcode);
      return;
    }
    Product product = null;
    GrocycodeUtil.Grocycode grocycode = GrocycodeUtil.getGrocycode(barcode);
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
      for (ProductBarcode code : productBarcodes) {
        if (code.getBarcode().equals(barcode)) {
          product = Product.getProductFromId(products, code.getProductIdInt());
        }
      }
    }
    if (product != null) {
      setProduct(product);
    } else {
      Bundle bundle = new Bundle();
      bundle.putString(ARGUMENT.BARCODE, barcode);
      sendEvent(Event.CHOOSE_PRODUCT, bundle);
    }
  }

  public void checkProductInput() {
    formData.isProductNameValid();
    String input = formData.getProductProducedNameLive().getValue();
    if (input == null || input.isEmpty()) {
      return;
    }
    Product product = Product.getProductFromName(products, input);

    GrocycodeUtil.Grocycode grocycode = GrocycodeUtil.getGrocycode(input.trim());
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
      for (ProductBarcode code : productBarcodes) {
        if (code.getBarcode().equals(input.trim())) {
          product = Product.getProductFromId(products, code.getProductIdInt());
        }
      }
      if (product != null) {
        setProduct(product);
        return;
      }
    }

    Product currentProduct = formData.getProductProducedLive().getValue();
    if (currentProduct != null && product != null && currentProduct.getId() == product.getId()) {
      return;
    }

    if (product != null) {
      setProduct(product);
    } else {
      showInputProductBottomSheet(input);
    }
  }

  public void saveEntry(boolean withClosing) {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }

    Recipe recipeToSave = new Recipe();
    if (isActionEdit()) {
      recipeToSave = recipe;
    }
    recipeToSave = formData.fillRecipe(recipeToSave);
    JSONObject jsonObject = Recipe.getJsonFromRecipe(recipeToSave, debug, TAG);

    if (isActionEdit()) {
      dlHelper.put(
          grocyApi.getObject(ENTITY.RECIPES, recipeToSave.getId()),
          jsonObject,
          response -> navigateUp(),
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveEntry: " + error);
            }
          }
      );
    } else {
      Recipe finalRecipe = recipeToSave;
      dlHelper.post(
          grocyApi.getObjects(ENTITY.RECIPES),
          jsonObject,
          response -> {
            int objectId = -1;
            try {
              objectId = response.getInt("created_object_id");
              Log.i(TAG, "saveEntry: " + objectId);
            } catch (JSONException e) {
              if (debug) {
                Log.e(TAG, "saveEntry: " + e);
              }
            }
            if (withClosing) {
              if (objectId != -1) {
                Bundle bundle = new Bundle();
                bundle.putInt(Constants.ARGUMENT.RECIPE_ID, objectId);
                sendEvent(Event.SET_RECIPE_ID, bundle);
              }
              navigateUp();
            } else {
              actionEditLive.setValue(true);
              finalRecipe.setId(objectId);
              recipe = finalRecipe;
              sendEvent(Event.TRANSACTION_SUCCESS);
            }
          },
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveEntry: " + error);
            }
          }
      );
    }
  }

  private void fillWithRecipeIfNecessary() {
    if (!isActionEdit() || formData.isFilledWithRecipe()) {
      return;
    }
    formData.getNameLive().setValue(recipe.getName());
    formData.getBaseServingsLive().setValue(
        NumUtil.trimAmount(recipe.getBaseServings(), maxDecimalPlacesAmount)
    );
    formData.getNotCheckShoppingListLive().setValue(recipe.isNotCheckShoppingList());
    formData.getProductsLive().setValue(Product.getActiveProductsOnly(products));
    formData.getPreparationLive().setValue(recipe.getDescription());
    formData.getPreparationSpannedLive().setValue(recipe.getDescription() != null
        ? Html.fromHtml(recipe.getDescription()) : null);

    formData.setFilledWithRecipe(true);
  }

  public void deleteEntry() {
    if (!isActionEdit()) {
      return;
    }
    dlHelper.delete(
        grocyApi.getObject(
            ENTITY.RECIPES,
            recipe.getId()
        ),
        response -> navigateUp(),
        this::showNetworkErrorMessage
    );
  }

  public void showInputProductBottomSheet(@NonNull String input) {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.PRODUCT_INPUT, input);
    showBottomSheet(new InputProductBottomSheet(), bundle);
  }

  private void showMessageAndContinueScanning(String msg) {
    formData.clearForm();
    showMessage(msg);
    sendEvent(Event.CONTINUE_SCANNING);
  }

  private void showMessageAndContinueScanning(@StringRes int msg) {
    showMessageAndContinueScanning(getString(msg));
  }

  public boolean isActionEdit() {
    assert actionEditLive.getValue() != null;
    return actionEditLive.getValue();
  }

  public MutableLiveData<Boolean> getActionEditLive() {
    return actionEditLive;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
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

  public String getAction() {
    return args.getAction();
  }

  public Recipe getRecipe() {
    return recipe;
  }

  public static class RecipeEditViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final RecipeEditFragmentArgs args;

    public RecipeEditViewModelFactory(
        Application application,
        RecipeEditFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new RecipeEditViewModel(application, args);
    }
  }
}
