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

package xyz.zedler.patrick.grocy.fragment;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.FAB;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipePositionAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeBinding;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.viewmodel.RecipeViewModel;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeFragment extends BaseFragment implements
    RecipePositionAdapter.RecipePositionsItemAdapterListener {

  private final static String TAG = RecipeFragment.class.getSimpleName();

  private static final String DIALOG_CONSUME_SHOWING = "dialog_consume_showing";
  private static final String DIALOG_SHOPPING_LIST_SHOWING = "dialog_shopping_list_showing";
  private static final String DIALOG_SHOPPING_LIST_NAMES = "dialog_shopping_list_names";
  private static final String DIALOG_SHOPPING_LIST_CHECKED = "dialog_shopping_list_checked";
  private static final String DIALOG_DELETE_SHOWING = "dialog_delete_showing";

  private MainActivity activity;
  private RecipeViewModel viewModel;
  private ClickUtil clickUtil;
  private FragmentRecipeBinding binding;
  private AlertDialog dialogConsume, dialogShoppingList, dialogDelete;
  private final HashMap<String, Boolean> dialogShoppingListMultiChoiceItems = new HashMap<>();

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentRecipeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (binding != null) {
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    RecipeFragmentArgs args = RecipeFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new RecipeViewModel
        .RecipeViewModelFactory(activity.getApplication(), args)
    ).get(RecipeViewModel.class);
    viewModel = new ViewModelProvider(this).get(RecipeViewModel.class);
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    clickUtil = new ClickUtil();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setToolbar(binding.toolbar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainer);
    systemBarBehavior.applyAppBarInsetOnContainer(false);
    systemBarBehavior.applyStatusBarInsetOnContainer(false);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    ColorRoles colorYellow = ResUtil.getHarmonizedRoles(requireContext(), R.color.yellow);
    binding.buttonFulfillmentInfo.setIconTint(ColorStateList.valueOf(colorYellow.getAccent()));

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    viewModel.getRecipeLive().observe(getViewLifecycleOwner(), recipe -> {
      if (recipe == null) return;
      loadRecipePicture(recipe);
      setupMenuButtons();
      updateDataWithServings();
    });
    viewModel.getServingsDesiredLive().observe(
        getViewLifecycleOwner(),
        servings -> viewModel.updateSaveDesiredServingsVisibility()
    );

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      }
    });

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

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_recipe, this::onMenuItemClick);
    activity.updateFab(
        R.drawable.ic_round_countertops,
        R.string.title_preparation_mode,
        FAB.TAG.PREPARATION,
        savedInstanceState == null,
        () -> viewModel.showMessage(R.string.msg_not_implemented_yet)
    );
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

  private void loadRecipePicture(Recipe recipe) {
    if (recipe.getPictureFileName() != null) {
      GrocyApi grocyApi = new GrocyApi(activity.getApplication());

      PictureUtil.loadPicture(
          binding.photoView,
          binding.photoViewCard,
          null,
          grocyApi.getRecipePictureServeLarge(recipe.getPictureFileName()),
          RequestHeaders.getGlideGrocyAuthHeaders(requireContext()),
          true
      );
    }
  }

  public void updateDataWithServings() {
    Recipe recipe = viewModel.getRecipeLive().getValue();
    RecipeFulfillment recipeFulfillment = viewModel.getRecipeFulfillment();
    List<RecipePosition> recipePositions = viewModel.getRecipePositions();
    if (recipe == null || recipeFulfillment == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }

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
      binding.imageFulfillment.setImageTintList(
          ColorStateList.valueOf(colorGreen.getAccent())
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
      binding.imageFulfillment.setImageTintList(
          ColorStateList.valueOf(colorYellow.getAccent())
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
      binding.imageFulfillment.setImageTintList(
          ColorStateList.valueOf(colorRed.getAccent())
      );
      binding.missing.setText(
          getResources().getQuantityString(R.plurals.msg_recipes_ingredients_missing,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount())
      );
      binding.missing.setVisibility(View.VISIBLE);
    }

    binding.textInputServings.setEndIconOnClickListener(v -> viewModel.saveDesiredServings());
    binding.textInputServings.setEndIconOnLongClickListener(v -> {
      activity.showToast(R.string.action_apply_desired_servings, true);
      return true;
    });
    binding.textInputServings.setHelperText(
        getString(
            R.string.property_servings_base_insert,
            NumUtil.trimAmount(recipe.getBaseServings(), viewModel.getMaxDecimalPlacesAmount())
        )
    );
    binding.textInputServings.setHelperTextColor(ColorStateList.valueOf(colorBlue.getAccent()));
    assert binding.textInputServings.getEditText() != null;
    binding.textInputServings.getEditText().setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        viewModel.saveDesiredServings();
        binding.editTextServings.clearFocus();
      }
      return false;
    });
    binding.calories.setText(
        getString(R.string.property_energy),
        NumUtil.trimAmount(recipeFulfillment.getCalories(), viewModel.getMaxDecimalPlacesAmount()),
        getString(R.string.subtitle_per_serving)
    );
    if (viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      binding.costs.setText(
          getString(R.string.property_costs),
          NumUtil.trimPrice(recipeFulfillment.getCosts(), viewModel.getDecimalPlacesPriceDisplay()) + " "
              + viewModel.getCurrency()
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
            viewModel.getProducts(),
            viewModel.getQuantityUnits(),
            viewModel.getQuantityUnitConversions(),
            viewModel.getStockItemHashMap(),
            viewModel.getShoppingListItems()
        );
      } else {
        binding.recycler.setAdapter(
            new RecipePositionAdapter(
                requireContext(),
                (LinearLayoutManager) binding.recycler.getLayoutManager(),
                recipe,
                recipePositions,
                viewModel.getProducts(),
                viewModel.getQuantityUnits(),
                viewModel.getQuantityUnitConversions(),
                viewModel.getStockItemHashMap(),
                viewModel.getShoppingListItems(),
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
      binding.preparationTitle.setVisibility(View.GONE);
      binding.preparation.setVisibility(View.GONE);
    } else {
      binding.preparationTitle.setVisibility(View.VISIBLE);
      binding.preparation.setHtml(description);
    }
  }

  @Override
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

  private void setupMenuButtons() {
    binding.chipConsume.setOnClickListener(v -> showConsumeConfirmationDialog());
    binding.chipShoppingList.setOnClickListener(v -> buildShoppingListConfirmationDialog());
  }

  private void showConsumeConfirmationDialog() {
    Recipe recipe = viewModel.getRecipeLive().getValue();
    if (recipe == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }
    dialogConsume = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog
    ).setTitle(R.string.title_confirmation)
        .setMessage(getString(R.string.msg_recipe_consume_ask, recipe.getName()))
        .setPositiveButton(R.string.action_consume_all, (dialog, which) -> {
          performHapticClick();
          viewModel.consumeRecipe(recipe.getId());
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogConsume.show();
  }

  private void buildShoppingListConfirmationDialog() {
    // Hashmap with all missing products for the dialog (at first all should be checked)
    // global variable for alert dialog management
    dialogShoppingListMultiChoiceItems.clear();
    if (binding.recycler.getAdapter() == null
        || !(binding.recycler.getAdapter() instanceof RecipePositionAdapter)) return;
    for (Product product : ((RecipePositionAdapter) binding.recycler.getAdapter()).getMissingProducts()) {
      dialogShoppingListMultiChoiceItems.put(product.getName(), true);
    }
    showShoppingListConfirmationDialog();
  }

  private void showShoppingListConfirmationDialog() {
    Recipe recipe = viewModel.getRecipeLive().getValue();
    if (recipe == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }
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
    builder.setPositiveButton(
        R.string.action_proceed,
        (dialog, which) -> viewModel
            .addNotFulfilledProductsToCartForRecipe(recipe.getId(), getExcludedProductIds())
    );
    builder.setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick());
    builder.setOnCancelListener(dialog -> performHapticClick());

    String[] names = dialogShoppingListMultiChoiceItems.keySet().toArray(new String[0]);
    Boolean[] namesChecked = dialogShoppingListMultiChoiceItems.values().toArray(new Boolean[0]);
    builder.setMultiChoiceItems(
        names,
        toPrimitiveBooleanArray(namesChecked),
        (dialog, which, isChecked) -> dialogShoppingListMultiChoiceItems.put(
            names[which], isChecked
        )
    );

    dialogShoppingList = builder.create();
    dialogShoppingList.show();
  }

  private void showDeleteConfirmationDialog() {
    Recipe recipe = viewModel.getRecipeLive().getValue();
    if (recipe == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            getString(
                R.string.msg_master_delete, getString(R.string.title_recipe), recipe.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          viewModel.deleteRecipe(recipe.getId());
          activity.navUtil.navigateUp();
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogDelete.show();
  }

  private boolean onMenuItemClick(MenuItem item) {
    Recipe recipe = viewModel.getRecipeLive().getValue();
    if (recipe == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return false;
    }
    if (item.getItemId() == R.id.action_edit_recipe) {
      activity.navUtil.navigateFragment(RecipeFragmentDirections
          .actionRecipeFragmentToRecipeEditFragment(ACTION.EDIT).setRecipe(recipe));
      return true;
    } else if (item.getItemId() == R.id.action_copy_recipe) {
      viewModel.copyRecipe(recipe.getId());
      return true;
    } else if (item.getItemId() == R.id.action_delete_recipe) {
      showDeleteConfirmationDialog();
      return true;
    }
    return false;
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false);
  }

  public void clearInputFocus() {
    binding.editTextServings.clearFocus();
  }

  private int[] getExcludedProductIds() {
    List<Integer> excludedIds = new ArrayList<>();
    for (String productName : dialogShoppingListMultiChoiceItems.keySet()) {
      if (Boolean.TRUE.equals(dialogShoppingListMultiChoiceItems.get(productName))) continue;
      Product product = Product.getProductFromName(viewModel.getProducts(), productName);
      if (product != null) excludedIds.add(product.getId());
    }
    return excludedIds.stream().mapToInt(i -> i).toArray();
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

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}