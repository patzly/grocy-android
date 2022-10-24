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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;
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
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.repository.RecipesRepository;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeBottomSheet extends BaseBottomSheetDialogFragment implements
        RecipePositionAdapter.RecipePositionsItemAdapterListener {

  private final static String TAG = RecipeBottomSheet.class.getSimpleName();

  private static final String DIALOG_CONSUME_SHOWING = "dialog_consume_showing";
  private static final String DIALOG_SHOPPING_LIST_SHOWING = "dialog_shopping_list_showing";
  private static final String DIALOG_SHOPPING_LIST_NAMES = "dialog_shopping_list_names";
  private static final String DIALOG_SHOPPING_LIST_CHECKED = "dialog_shopping_list_checked";
  private static final String DIALOG_DELETE_SHOWING = "dialog_delete_showing";

  private SharedPreferences sharedPrefs;
  private MainActivity activity;
  private FragmentBottomsheetRecipeBinding binding;
  private ViewUtil.TouchProgressBarUtil touchProgressBarUtil;
  private RecipesRepository recipesRepository;
  private DownloadHelper dlHelper;
  private AlertDialog dialogConsume, dialogShoppingList, dialogDelete;
  private final HashMap<String, Boolean> dialogShoppingListMultiChoiceItems = new HashMap<>();

  private Recipe recipe;
  private RecipeFulfillment recipeFulfillment;
  private List<Recipe> recipes;
  private List<RecipeFulfillment> recipeFulfillments;
  private List<RecipePosition> recipePositions;
  private List<Product> products;
  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversion> quantityUnitConversions;

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
    if (dialogConsume != null) {
      // Else it throws an leak exception because the context is somehow from the activity
      dialogConsume.dismiss();
    } else if (dialogShoppingList != null) {
      dialogShoppingList.dismiss();
    } else if (dialogDelete != null) {
      dialogDelete.dismiss();
    }
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
    quantityUnitConversions = bundle.getParcelableArrayList(ARGUMENT.QUANTITY_UNIT_CONVERSIONS);

    if (
        recipe == null ||
            recipeFulfillment == null ||
            recipePositions == null ||
            products == null ||
            quantityUnits == null ||
            quantityUnitConversions == null
    ) {
      dismiss();
      return;
    }

    binding.toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_preparation_mode) {
        openPreparationMode();
        return true;
      } else if (item.getItemId() == R.id.action_edit_recipe) {
        activity.getCurrentFragment().editRecipe(recipe);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_copy_recipe) {
        activity.getCurrentFragment().copyRecipe(recipe.getId());
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_delete_recipe) {
        showDeleteConfirmationDialog();
        return true;
      }
      return false;
    });

    servingsDesiredLive = new MutableLiveData<>(NumUtil.trim(recipe.getDesiredServings()));
    servingsDesiredSaveEnabledLive = new MutableLiveData<>(false);

    servingsDesiredSaveEnabledLive.observe(
        getViewLifecycleOwner(), value -> binding.textInputServings.setEndIconVisible(value)
    );

    loadRecipePicture();
    setupMenuButtons();
    updateDataWithServings();

    ColorStateList colorSurface3 = ColorStateList.valueOf(
        SurfaceColors.SURFACE_3.getColor(activity)
    );
    binding.chipConsume.setChipBackgroundColor(colorSurface3);
    binding.chipShoppingList.setChipBackgroundColor(colorSurface3);

    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean(DIALOG_CONSUME_SHOWING)) {
        new Handler(Looper.getMainLooper()).postDelayed(
            this::showConsumeConfirmationDialog, 1
        );
      } else if (savedInstanceState.getBoolean(DIALOG_SHOPPING_LIST_SHOWING)) {
        dialogShoppingListMultiChoiceItems.clear();
        String[] names = savedInstanceState.getStringArray(DIALOG_SHOPPING_LIST_NAMES);
        boolean[] checked = savedInstanceState.getBooleanArray(DIALOG_SHOPPING_LIST_CHECKED);
        if (names != null && checked != null) {
          for (int i = 0; i < names.length; i++) {
            dialogShoppingListMultiChoiceItems.put(names[i], checked[i]);
          }
        }
        new Handler(Looper.getMainLooper()).postDelayed(
            this::showShoppingListConfirmationDialog, 1
        );
      } else if (savedInstanceState.getBoolean(DIALOG_DELETE_SHOWING)) {
        new Handler(Looper.getMainLooper()).postDelayed(
            this::showDeleteConfirmationDialog, 1
        );
      }
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(DIALOG_CONSUME_SHOWING, dialogConsume != null && dialogConsume.isShowing());
    outState.putBoolean(
        DIALOG_SHOPPING_LIST_SHOWING, dialogShoppingList != null && dialogShoppingList.isShowing()
    );
    outState.putStringArray(
        DIALOG_SHOPPING_LIST_NAMES,
        dialogShoppingListMultiChoiceItems.keySet().toArray(new String[0])
    );
    outState.putBooleanArray(
        DIALOG_SHOPPING_LIST_CHECKED,
        toPrimitiveBooleanArray(dialogShoppingListMultiChoiceItems.values().toArray(new Boolean[0]))
    );
    outState.putBoolean(DIALOG_DELETE_SHOWING, dialogDelete != null && dialogDelete.isShowing());
  }

  private void loadDataFromDatabase() {
    recipesRepository.loadFromDatabase(data -> {
      recipes = data.getRecipes();
      recipeFulfillments = data.getRecipeFulfillments();
      recipePositions = RecipePosition.getRecipePositionsFromRecipeId(
          data.getRecipePositions(), recipe.getId()
      );
      products = data.getProducts();
      quantityUnits = data.getQuantityUnits();
      quantityUnitConversions = data.getQuantityUnitConversions();

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
      setMenuButtonState(binding.chipConsume, true);
      setMenuButtonState(binding.chipShoppingList, false);
      binding.fulfilled.setText(R.string.msg_recipes_enough_in_stock);
      binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
          getResources(),
          R.drawable.ic_round_check_circle_outline,
          null
      ));
      binding.imageFulfillment.setColorFilter(
          colorGreen.getAccent(),
          android.graphics.PorterDuff.Mode.SRC_IN
      );
      binding.missing.setVisibility(View.GONE);
    } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
      setMenuButtonState(binding.chipConsume, false);
      setMenuButtonState(binding.chipShoppingList, false);
      binding.fulfilled.setText(R.string.msg_recipes_not_enough);
      binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
          getResources(),
          R.drawable.ic_round_error_outline,
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
      setMenuButtonState(binding.chipConsume, false);
      setMenuButtonState(binding.chipShoppingList, true);
      binding.fulfilled.setText(R.string.msg_recipes_not_enough);
      binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
          getResources(),
          R.drawable.ic_round_highlight_off,
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

    binding.textInputServings.setEndIconOnClickListener(v -> saveDesiredServings());
    binding.textInputServings.setEndIconOnLongClickListener(v -> {
      activity.showToastTextLong(R.string.action_apply_desired_servings, true);
      return true;
    });
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
            quantityUnits,
            quantityUnitConversions
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
                quantityUnitConversions,
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
          .listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                Target<Drawable> target, boolean isFirstResource) {
              binding.picture.setVisibility(View.GONE);
              LinearLayout.LayoutParams params
                  = (LayoutParams) binding.headerTextContainer.getLayoutParams();
              params.weight = 4;
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
      LinearLayout.LayoutParams params
          = (LayoutParams) binding.headerTextContainer.getLayoutParams();
      params.weight = 4;
      binding.headerTextContainer.setLayoutParams(params);
    }
  }

  private void setupMenuButtons() {
    binding.chipConsume.setOnClickListener(v -> showConsumeConfirmationDialog());
    // Hashmap with all missing products for the dialog (at first all should be checked)
    // global variable for alert dialog management
    dialogShoppingListMultiChoiceItems.clear();
    for (Product product : products) {
      dialogShoppingListMultiChoiceItems.put(product.getName(), true);
    }
    binding.chipShoppingList.setOnClickListener(v -> showShoppingListConfirmationDialog());
  }

  private void showConsumeConfirmationDialog() {
    dialogConsume = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog
    ).setTitle(R.string.title_confirmation)
        .setMessage(getString(R.string.msg_recipe_consume_ask, recipe.getName()))
        .setPositiveButton(R.string.action_consume_all, (dialog, which) -> {
          performHapticClick();
          activity.getCurrentFragment().consumeRecipe(recipe.getId());
          dismiss();
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogConsume.show();
  }

  private void showShoppingListConfirmationDialog() {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog
    );
    Typeface jost = ResourcesCompat.getFont(activity, R.font.jost_book);

    TextView title = new TextView(activity);
    title.setTextAppearance(activity, R.style.Widget_Grocy_TextView_HeadlineSmall);
    int padding = UiUtil.dpToPx(activity, 8);
    title.setPadding(padding * 3, padding * 3, padding * 3, padding * 2);
    title.setText(R.string.title_confirmation);
    title.setTextSize(24);
    title.setTypeface(jost);

    TextView text = new TextView(activity);
    text.setTextAppearance(activity, R.style.Widget_Grocy_TextView_BodyMedium);
    text.setPadding(padding * 3, 0, padding * 3, 0);
    text.setText(getString(R.string.msg_recipe_shopping_list_ask, recipe.getName()));
    text.setTextSize(14);
    text.setTypeface(jost);

    LinearLayout container = new LinearLayout(activity);
    container.setOrientation(LinearLayout.VERTICAL);
    container.addView(title);
    container.addView(text);

    builder.setCustomTitle(container);
    builder.setPositiveButton(R.string.action_proceed, (dialog, which) -> {
      Log.i(TAG, "setupMenuButtons: " + dialogShoppingListMultiChoiceItems);
      activity.getCurrentFragment().addNotFulfilledProductsToCartForRecipe(recipe.getId());
      dialog.dismiss();
    });
    builder.setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick());
    builder.setOnCancelListener(dialog -> performHapticClick());

    String[] names = dialogShoppingListMultiChoiceItems.keySet().toArray(new String[0]);
    Boolean[] namesChecked = dialogShoppingListMultiChoiceItems.values().toArray(new Boolean[0]);
    builder.setMultiChoiceItems(
        names,
        toPrimitiveBooleanArray(namesChecked),
        (dialog, which, isChecked) -> dialogShoppingListMultiChoiceItems.put(
            (String) names[which], isChecked
        )
    );

    dialogShoppingList = builder.create();
    dialogShoppingList.show();
  }

  private void showDeleteConfirmationDialog() {
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            getString(
                R.string.msg_master_delete, getString(R.string.title_recipe), recipe.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          activity.getCurrentFragment().deleteRecipe(recipe.getId());
          dismiss();
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogDelete.show();
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

  private static boolean[] toPrimitiveBooleanArray(Boolean... booleanList) {
    final boolean[] primitives = new boolean[booleanList.length];
    int index = 0;
    for (Boolean object : booleanList) {
      primitives[index++] = object;
    }
    return primitives;
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
