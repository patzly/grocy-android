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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.TransitionManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.color.ColorRoles;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipePositionAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetRecipeBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.repository.RecipesRepository;
import xyz.zedler.patrick.grocy.util.AlertDialogUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil.TouchProgressBarUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeBottomSheet extends BaseBottomSheetDialogFragment implements
        RecipePositionAdapter.RecipePositionsItemAdapterListener {

  private final static String TAG = RecipeBottomSheet.class.getSimpleName();

  private SharedPreferences sharedPrefs;
  private MainActivity activity;
  private FragmentBottomsheetRecipeBinding binding;
  private ViewUtil.TouchProgressBarUtil touchProgressBarUtil;
  private RecipesRepository recipesRepository;
  private DownloadHelper dlHelper;

  private Recipe recipe;
  private RecipeFulfillment recipeFulfillment;
  private List<Recipe> recipes;
  private List<RecipeFulfillment> recipeFulfillments;
  private List<RecipePosition> recipePositions;
  private List<Product> products;
  private List<QuantityUnit> quantityUnits;

  private MutableLiveData<Boolean> networkLoadingLive;
  private MutableLiveData<String> servingsDesiredLive;
  private MutableLiveData<Boolean> servingsDesiredSaveEnabledLive;

  private boolean servingsDesiredChanged;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    activity = (MainActivity) getActivity();
    assert activity != null;

    binding = FragmentBottomsheetRecipeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onStart() {
    super.onStart();
    keepScreenOnIfNecessary(true);
  }

  @Override
  public void onStop() {
    super.onStop();
    keepScreenOnIfNecessary(false);
  }

  @Override
  public void onDestroyView() {
    if (touchProgressBarUtil != null) {
      touchProgressBarUtil.onDestroy();
      touchProgressBarUtil = null;
    }
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding.recycler.setAdapter(null);
      binding = null;
    }

    super.onDestroyView();
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);
    if (servingsDesiredChanged) {
      activity.getCurrentFragment().updateData();
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.setLifecycleOwner(getViewLifecycleOwner());
    binding.setBottomSheet(this);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplication());
    networkLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(activity.getApplication(), TAG,
        isLoading -> networkLoadingLive.setValue(isLoading));
    recipesRepository = new RecipesRepository(activity.getApplication());

    binding.recycler.setLayoutManager(
            new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
    }

    Bundle bundle = getArguments();

    if (bundle == null) {
      dismiss();
      return;
    }

    recipe = bundle.getParcelable(ARGUMENT.RECIPE);
    recipeFulfillment = bundle.getParcelable(ARGUMENT.RECIPE_FULFILLMENT);
    recipePositions = bundle.getParcelableArrayList(ARGUMENT.RECIPE_POSITIONS);
    products = bundle.getParcelableArrayList(ARGUMENT.PRODUCTS);
    quantityUnits = bundle.getParcelableArrayList(ARGUMENT.QUANTITY_UNITS);

    if (
        recipe == null ||
            recipeFulfillment == null ||
            recipePositions == null ||
            products == null ||
            quantityUnits == null
    ) {
      dismiss();
      return;
    }

    binding.toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_copy_recipe) {
        activity.getCurrentFragment().copyRecipe(recipe.getId());
        dismiss();
        return true;
      }
      return false;
    });

    servingsDesiredLive = new MutableLiveData<>(NumUtil.trim(recipe.getDesiredServings()));
    servingsDesiredSaveEnabledLive = new MutableLiveData<>(false);

    loadRecipePicture();
    setupMenuButtons();
    updateDataWithServings();
  }

  private void loadDataFromDatabase() {
    recipesRepository.loadFromDatabase(data -> {
      recipes = data.getRecipes();
      recipeFulfillments = data.getRecipeFulfillments();
      recipePositions = RecipePosition.getRecipePositionsFromRecipeId(data.getRecipePositions(), recipe.getId());
      products = data.getProducts();
      quantityUnits = data.getQuantityUnits();

      recipe = Recipe.getRecipeFromId(recipes, recipe.getId());
      recipeFulfillment = recipe != null
          ? RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe.getId())
          : null;
      if (recipe == null || recipeFulfillment == null) {
        showToast(R.string.error_undefined);
        return;
      }

      updateDataWithServings();
    });
  }

  public void onItemRowClicked(RecipePosition recipePosition, int position) {
    if (recipePosition == null) {
      return;
    }

    recipePosition.toggleChecked();
    RecipePositionAdapter adapter = (RecipePositionAdapter) binding.recycler.getAdapter();
    if (adapter != null) {
      adapter.notifyItemChanged(position, recipePosition);
    }
  }

  private void keepScreenOnIfNecessary(boolean keepOn) {
    if (activity == null) {
      activity = (MainActivity) requireActivity();
    }
    if (sharedPrefs == null) {
      sharedPrefs = PreferenceManager
              .getDefaultSharedPreferences(activity);
    }
    boolean necessary = sharedPrefs.getBoolean(
            Constants.SETTINGS.RECIPES.KEEP_SCREEN_ON,
            Constants.SETTINGS_DEFAULT.RECIPES.KEEP_SCREEN_ON
    );
    if (necessary && keepOn) {
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  public void changeAmount(boolean more) {
    if (!NumUtil.isStringDouble(servingsDesiredLive.getValue())) {
      servingsDesiredLive.setValue(String.valueOf(1));
    } else {
      double servings = Double.parseDouble(servingsDesiredLive.getValue());
      double servingsNew = more ? servings + 1 : servings - 1;
      if (servingsNew <= 0) servingsNew = 1;
      servingsDesiredLive.setValue(NumUtil.trim(servingsNew));
    }
  }

  public void updateDataWithServings() {
    TransitionManager.beginDelayedTransition(binding.recipeBottomsheet);

    ColorRoles colorBlue = ResUtil.getHarmonizedRoles(activity, R.color.blue);
    ColorRoles colorGreen = ResUtil.getHarmonizedRoles(activity, R.color.green);
    ColorRoles colorYellow = ResUtil.getHarmonizedRoles(activity, R.color.yellow);
    ColorRoles colorRed = ResUtil.getHarmonizedRoles(activity, R.color.red);

    // REQUIREMENTS FULFILLED
    if (recipeFulfillment.isNeedFulfilled()) {
      setMenuButtonState(binding.menuItemConsume, true);
      setMenuButtonState(binding.menuItemShoppingList, false);
      binding.fulfilled.setText(R.string.msg_recipes_enough_in_stock);
      binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
          getResources(),
          R.drawable.ic_round_done,
          null
      ));
      binding.imageFulfillment.setColorFilter(
          colorGreen.getAccent(),
          android.graphics.PorterDuff.Mode.SRC_IN
      );
      binding.missing.setVisibility(View.GONE);
    } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
      setMenuButtonState(binding.menuItemConsume, false);
      setMenuButtonState(binding.menuItemShoppingList, false);
      binding.fulfilled.setText(R.string.msg_recipes_not_enough);
      binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
          getResources(),
          R.drawable.ic_round_priority_high,
          null
      ));
      binding.imageFulfillment.setColorFilter(
          colorYellow.getAccent(),
          android.graphics.PorterDuff.Mode.SRC_IN
      );
      binding.missing.setText(
          getResources()
              .getQuantityString(R.plurals.msg_recipes_ingredients_missing_but_on_shopping_list,
                  recipeFulfillment.getMissingProductsCount(),
                  recipeFulfillment.getMissingProductsCount())
      );
      binding.missing.setVisibility(View.VISIBLE);
    } else {
      setMenuButtonState(binding.menuItemConsume, false);
      setMenuButtonState(binding.menuItemShoppingList, true);
      binding.fulfilled.setText(R.string.msg_recipes_not_enough);
      binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
          getResources(),
          R.drawable.ic_round_close,
          null
      ));
      binding.imageFulfillment.setColorFilter(
          colorRed.getAccent(),
          android.graphics.PorterDuff.Mode.SRC_IN
      );
      binding.missing.setText(
          getResources().getQuantityString(R.plurals.msg_recipes_ingredients_missing,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount())
      );
      binding.missing.setVisibility(View.VISIBLE);
    }

    binding.name.setText(recipe.getName());
    binding.textInputServings.setHelperText(
        getString(R.string.property_servings_base_insert, NumUtil.trim(recipe.getBaseServings()))
    );
    binding.textInputServings.setHelperTextColor(ColorStateList.valueOf(colorBlue.getAccent()));
    assert binding.textInputServings.getEditText() != null;
    binding.textInputServings.getEditText().setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        saveDesiredServings();
      }
      return false;
    });
    binding.calories.setText(
        getString(R.string.property_energy),
        NumUtil.trim(recipeFulfillment.getCalories()),
        getString(R.string.subtitle_per_serving)
    );
    boolean isPriceTrackingEnabled = sharedPrefs.getBoolean(
        Constants.PREF.FEATURE_STOCK_PRICE_TRACKING, true
    );
    if (isPriceTrackingEnabled) {
      binding.costs.setText(
          getString(R.string.property_costs),
          NumUtil.trimPrice(recipeFulfillment.getCosts()) + " "
              + sharedPrefs.getString(Constants.PREF.CURRENCY, "")
      );
    } else {
      binding.costs.setVisibility(View.GONE);
    }

    if (recipePositions.isEmpty()) {
      binding.ingredientContainer.setVisibility(View.GONE);
    } else {
      if (binding.recycler.getAdapter() instanceof RecipePositionAdapter) {
        ((RecipePositionAdapter) binding.recycler.getAdapter()).updateData(
            recipe,
            recipePositions,
            products,
            quantityUnits
        );
      } else {
        binding.recycler.setAdapter(
            new RecipePositionAdapter(
                requireContext(),
                (LinearLayoutManager) binding.recycler.getLayoutManager(),
                recipe,
                recipePositions,
                products,
                quantityUnits,
                this
            )
        );
      }

      for (RecipePosition recipePosition: recipePositions) {
        if (recipePosition.isChecked())
          recipePosition.setChecked(false);
      }
    }

    CharSequence trimmedDescription = TextUtil.trimCharSequence(recipe.getDescription());
    String description = trimmedDescription != null ? trimmedDescription.toString() : null;
    if (description == null || description.isEmpty()) {
      binding.preparation.setVisibility(View.GONE);
    } else {
      binding.preparation.setDialogTitle(R.string.property_preparation);
      binding.preparation.setHtml(description);
    }
  }

  public void clearServingsFieldAndFocusIt() {
    servingsDesiredLive.setValue(null);
    binding.textInputServings.requestFocus();
  }

  public void clearInputFocus() {
    binding.textInputServings.clearFocus();
    binding.dummyFocusView.requestFocus();
    hideSoftKeyboardBottomSheet(binding.recipeBottomsheet);
  }

  public void updateSaveDesiredServingsVisibility() {
    if (NumUtil.isStringDouble(servingsDesiredLive.getValue())) {
      double servings = NumUtil.toDouble(servingsDesiredLive.getValue());
      servingsDesiredSaveEnabledLive.setValue(servings != recipe.getDesiredServings());
    } else {
      servingsDesiredSaveEnabledLive.setValue(1 != recipe.getDesiredServings());
    }
  }

  public void saveDesiredServings() {
    servingsDesiredChanged = true;
    clearInputFocus();
    double servingsDesired;
    if (NumUtil.isStringDouble(servingsDesiredLive.getValue())) {
      servingsDesired = NumUtil.toDouble(servingsDesiredLive.getValue());
    } else {
      servingsDesired = 1;
      servingsDesiredLive.setValue(NumUtil.trim(servingsDesired));
    }
    servingsDesiredSaveEnabledLive.setValue(false);

    JSONObject body = new JSONObject();
    try {
      body.put("desired_servings", NumUtil.trim(servingsDesired));
    } catch (JSONException e) {
      showToast(R.string.error_undefined);
      servingsDesiredSaveEnabledLive.setValue(true);
      return;
    }

    dlHelper.editRecipe(
        recipe.getId(),
        body,
        response -> dlHelper.updateData(
            () -> {
              servingsDesiredSaveEnabledLive.setValue(false);
              loadDataFromDatabase();
            },
            volleyError -> {
              showToast(R.string.error_undefined);
              servingsDesiredSaveEnabledLive.setValue(true);
            },
            Recipe.class,
            RecipeFulfillment.class,
            RecipePosition.class
        ),
        error -> {
          showToast(R.string.error_undefined);
          servingsDesiredSaveEnabledLive.setValue(true);
        }
    ).perform(dlHelper.getUuid());
  }

  private void loadRecipePicture() {
    if (recipe.getPictureFileName() != null) {
      GrocyApi grocyApi = new GrocyApi(activity.getApplication());
      binding.picture.layout(0, 0, 0, 0);
      Glide
          .with(requireContext())
          .load(new GlideUrl(
              grocyApi.getRecipePicture(recipe.getPictureFileName()),
              RequestHeaders.getGlideGrocyAuthHeaders(requireContext()))
          )
          .transform(new CenterCrop(), new RoundedCorners(UiUtil.dpToPx(requireContext(), 12)))
          .transition(DrawableTransitionOptions.withCrossFade())
          .listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(
                @Nullable @org.jetbrains.annotations.Nullable GlideException e, Object model,
                Target<Drawable> target, boolean isFirstResource) {
              binding.picture.setVisibility(View.GONE);
              LinearLayout.LayoutParams params = (LayoutParams) binding.headerTextContainer.getLayoutParams();
              params.weight = 4f;
              binding.headerTextContainer.setLayoutParams(params);
              return false;
            }
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                DataSource dataSource, boolean isFirstResource) {
              binding.picture.setVisibility(View.VISIBLE);
              return false;
            }
          })
          .into(binding.picture);
    } else {
      binding.picture.setVisibility(View.GONE);
      LinearLayout.LayoutParams params = (LayoutParams) binding.headerTextContainer.getLayoutParams();
      params.weight = 4f;
      binding.headerTextContainer.setLayoutParams(params);
    }
  }

  private void setupMenuButtons() {
    binding.menuItemConsume.setOnClickListener(v -> AlertDialogUtil.showConfirmationDialog(
        requireContext(),
        getString(R.string.msg_recipe_consume_ask, recipe.getName()),
        () -> {
          activity.getCurrentFragment().consumeRecipe(recipe.getId());
          dismiss();
        })
    );
    HashMap<String, Boolean> missingIngredients = new HashMap<>();
    for (Product product : products) {
      missingIngredients.put(product.getName(), true);
    }
    binding.menuItemShoppingList.setOnClickListener(v -> AlertDialogUtil.showConfirmationDialog(
        requireContext(),
        getString(R.string.msg_recipe_shopping_list_ask, recipe.getName()),
        missingIngredients,
        ingredientsToAdd -> {
          Log.i(TAG, "setupMenuButtons: " + ingredientsToAdd);
          activity.getCurrentFragment().addNotFulfilledProductsToCartForRecipe(recipe.getId());
          dismiss();
        })
    );
    binding.menuItemEdit.setOnClickListener(v -> {
      activity.getCurrentFragment().editRecipe(recipe);
      dismiss();
    });
    touchProgressBarUtil = new TouchProgressBarUtil(
        binding.progressConfirmation,
        binding.menuItemDelete,
        2000,
        object -> {
          activity.getCurrentFragment().deleteRecipe(recipe.getId());
          dismiss();
        }
    );
  }

  public void openPreparationMode() {
    showToast(R.string.msg_coming_soon);
  }

  public MutableLiveData<String> getServingsDesiredLive() {
    return servingsDesiredLive;
  }

  public MutableLiveData<Boolean> getServingsDesiredSaveEnabledLive() {
    return servingsDesiredSaveEnabledLive;
  }

  public MutableLiveData<Boolean> getNetworkLoadingLive() {
    return networkLoadingLive;
  }

  private void hideSoftKeyboardBottomSheet(View view) {
    ((InputMethodManager) Objects.requireNonNull(activity.getSystemService(
        Context.INPUT_METHOD_SERVICE))).hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  private void setMenuButtonState(Button button, boolean enabled) {
    button.setEnabled(enabled);
    button.setAlpha(enabled ? 1f : 0.5f);
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainer.setPadding(
        binding.linearContainer.getPaddingLeft(),
        binding.linearContainer.getPaddingTop(),
        binding.linearContainer.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
