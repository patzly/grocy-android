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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.FAB;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipePositionAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipePositionResolvedAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.RecipePositionResolved;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.util.ChipUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.viewmodel.RecipeViewModel;
import xyz.zedler.patrick.grocy.viewmodel.RecipeViewModel.RecipeViewModelFactory;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeFragment extends BaseFragment implements
    RecipePositionAdapter.RecipePositionsItemAdapterListener,
    RecipePositionResolvedAdapter.RecipePositionsItemAdapterListener
{

  private final static String TAG = RecipeFragment.class.getSimpleName();

  private static final String DIALOG_CONSUME_SHOWING = "dialog_consume_showing";
  private static final String DIALOG_SHOPPING_LIST_SHOWING = "dialog_shopping_list_showing";
  private static final String DIALOG_SHOPPING_LIST_NAMES = "dialog_shopping_list_names";
  private static final String DIALOG_SHOPPING_LIST_CHECKED = "dialog_shopping_list_checked";
  private static final String DIALOG_DELETE_SHOWING = "dialog_delete_showing";

  private MainActivity activity;
  private RecipeViewModel viewModel;
  private FragmentRecipeBinding binding;
  private GrocyApi grocyApi;
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
    viewModel = new ViewModelProvider(this, new RecipeViewModelFactory(activity.getApplication(), args)
    ).get(RecipeViewModel.class);
    viewModel = new ViewModelProvider(this).get(RecipeViewModel.class);
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setContainer(binding.linearContainer);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainer);
    systemBarBehavior.applyAppBarInsetOnContainer(false);
    systemBarBehavior.applyStatusBarInsetOnContainer(false);
    systemBarBehavior.applyStatusBarInsetOnAppBar(false);
    systemBarBehavior.applyStatusBarInsetOnToolBar(false);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    int colorOnBg = ResUtil.getColor(activity, R.attr.colorOnSurface);
    binding.appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
      if (binding.collapsingToolbarLayout.getHeight() + verticalOffset
          < binding.collapsingToolbarLayout.getScrimVisibleHeightTrigger()) {
        if (binding.toolbar.getNavigationIcon() != null) {
          binding.toolbar.getNavigationIcon().setColorFilter(colorOnBg, PorterDuff.Mode.SRC_IN);
        }
        UiUtil.setLightStatusBar(
            activity.getWindow().getDecorView(), !UiUtil.isDarkModeActive(activity)
        );
      } else {
        if (binding.toolbar.getNavigationIcon() != null) {
          binding.toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }
        UiUtil.setLightStatusBar(activity.getWindow().getDecorView(), false);
      }
    });

    grocyApi = new GrocyApi(activity.getApplication());
    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());
    binding.imageView.setOnClickListener(v -> {
      Recipe recipe = viewModel.getRecipeLive().getValue();
      if (recipe == null) {
        activity.showSnackbar(R.string.error_undefined, false);
        return;
      }
      if (recipe.getPictureFileName() == null || recipe.getPictureFileName().isEmpty()) {
        return;
      }
      Bundle argsPhotoViewer = new PhotoViewerFragmentArgs.Builder(
          grocyApi.getRecipePictureServeLarge(recipe.getPictureFileName()),
          true
      ).build().toBundle();
      activity.navUtil.navigate(R.id.photoViewerFragment, argsPhotoViewer);
    });

    binding.buttonFulfillmentInfo.setIconTint(ColorStateList.valueOf(
        ResUtil.getColor(activity, R.attr.colorCustomYellow)
    ));

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    viewModel.getRecipeLive().observe(getViewLifecycleOwner(), recipe -> {
      if (recipe == null) return;
      binding.collapsingToolbarLayout.setTitle(recipe.getName());
      if (recipe.getName().length() > 30) {
        binding.collapsingToolbarLayout.setExpandedTitleTextAppearance(
            R.style.TextAppearance_Grocy_HeadlineSmall
        );
        binding.collapsingToolbarLayout.setCollapsedTitleTextAppearance(
            R.style.TextAppearance_Grocy_TitleMedium
        );
      } else {
        binding.collapsingToolbarLayout.setExpandedTitleTextAppearance(
            R.style.TextAppearance_Grocy_HeadlineLarge
        );
        binding.collapsingToolbarLayout.setCollapsedTitleTextAppearance(
            R.style.TextAppearance_Grocy_TitleLarge
        );
      }
      binding.collapsingToolbarLayout.setExpandedTitleColor(Color.WHITE);
      loadRecipePicture(recipe);
      setupMenuButtons();
      updateDataWithServings();
    });
    viewModel.getServingsDesiredLive().observe(
        getViewLifecycleOwner(),
        servings -> binding.titleServings.setText(getString(
            R.string.property_servings_desired_insert,
            servings
        ))
    );

    viewModel.getFilterChipLiveDataRecipeInfoFields().observe(
        getViewLifecycleOwner(),
        data -> binding.recipeInfoMenuButton.setOnClickListener(v -> {
          PopupMenu popupMenu = new PopupMenu(requireContext(), binding.recipeInfoMenuButton);
          data.populateMenu(popupMenu.getMenu());
          popupMenu.show();
        })
    );
    viewModel.getFilterChipLiveDataIngredientFields().observe(getViewLifecycleOwner(), data -> {
      if (!VersionUtil.isGrocyServerMin400(viewModel.getSharedPrefs())) {
        viewModel.showMessageLongDuration("Please update your Grocy server to version 4.0.0 or"
            + " newer to display extra fields on ingredients in this app");
        return;
      }
      binding.ingredientsMenuButton.setOnClickListener(v -> {
        PopupMenu popupMenu = new PopupMenu(requireContext(), binding.ingredientsMenuButton);
        data.populateMenu(popupMenu.getMenu());
        popupMenu.show();
      });
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
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
        binding.appBar, false, binding.scroll, true, false
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

  @Override
  public void saveInput(String text, Bundle argsBundle) {
    viewModel.getServingsDesiredLive().setValue(text);
    viewModel.saveDesiredServings();
  }

  private void loadRecipePicture(Recipe recipe) {
    if (recipe.getPictureFileName() != null) {
      PictureUtil.loadPicture(
          binding.imageView,
          null,
          null,
          grocyApi.getRecipePictureServeLarge(recipe.getPictureFileName()),
          RequestHeaders.getGlideGrocyAuthHeaders(requireContext()),
          false
      );
    }
  }

  public void updateDataWithServings() {
    Recipe recipe = viewModel.getRecipeLive().getValue();
    RecipeFulfillment recipeFulfillment = viewModel.getRecipeFulfillment();
    List<RecipePosition> recipePositions = viewModel.getRecipePositions();
    List<RecipePositionResolved> recipePositionsResolved = viewModel.getRecipePositionsResolved();
    if (recipe == null || recipeFulfillment == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }

    binding.titleServingsBase.setText(
        getString(
            R.string.property_servings_base_insert,
            NumUtil.trimAmount(recipe.getBaseServings(), viewModel.getMaxDecimalPlacesAmount())
        )
    );

    ChipUtil chipUtil = new ChipUtil(requireContext());
    List<String> activeFields = viewModel.getFilterChipLiveDataRecipeInfoFields().getActiveFields();
    binding.infoContainer.removeAllViews();
    if (activeFields.contains(RecipeViewModel.FIELD_FULFILLMENT)) {
      binding.infoContainer.addView(chipUtil.createRecipeFulfillmentChip(recipeFulfillment));
    }
    if (activeFields.contains(RecipeViewModel.FIELD_ENERGY)) {
      binding.infoContainer.addView(chipUtil.createTextChip(NumUtil.trimAmount(
          recipeFulfillment.getCalories(), viewModel.getMaxDecimalPlacesAmount()
      ) + " " + viewModel.getEnergyUnit()));
    }
    if (activeFields.contains(RecipeViewModel.FIELD_PRICE)) {
      binding.infoContainer.addView(chipUtil.createTextChip(NumUtil.trimPrice(
          recipeFulfillment.getCosts(), viewModel.getDecimalPlacesPriceDisplay()
      ) + " " + viewModel.getCurrency()));
    }
    for (String activeField : activeFields) {
      if (activeField.startsWith(Userfield.NAME_PREFIX)) {
        String userfieldName = activeField.substring(
            Userfield.NAME_PREFIX.length()
        );
        Userfield userfield = viewModel.getUserfieldHashMap().get(userfieldName);
        if (userfield == null) continue;
        Chip chipUserfield = chipUtil.createTextChip("");
        Chip chipFilled = Userfield.fillChipWithUserfield(
            chipUserfield,
            userfield,
            recipe.getUserfields().get(userfieldName)
        );
        if (chipFilled != null) binding.infoContainer.addView(chipFilled);
      }
    }
    binding.infoContainer.setVisibility(
        binding.infoContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE
    );

    if (!recipePositionsResolved.isEmpty()) {
      if (binding.recycler.getAdapter() instanceof RecipePositionResolvedAdapter) {
        ((RecipePositionResolvedAdapter) binding.recycler.getAdapter()).updateData(
            recipe,
            recipePositionsResolved,
            viewModel.getProducts(),
            viewModel.getQuantityUnits(),
            viewModel.getQuantityUnitConversions(),
            viewModel.getFilterChipLiveDataIngredientFields().getActiveFields()
        );
      } else {
        binding.recycler.setAdapter(
            new RecipePositionResolvedAdapter(
                requireContext(),
                (LinearLayoutManager) binding.recycler.getLayoutManager(),
                recipe,
                recipePositionsResolved,
                viewModel.getProducts(),
                viewModel.getQuantityUnits(),
                viewModel.getQuantityUnitConversions(),
                viewModel.getFilterChipLiveDataIngredientFields().getActiveFields(),
                this
            )
        );
      }
      binding.ingredientContainer.setVisibility(View.VISIBLE);
    } else if (!recipePositions.isEmpty()) {
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
      binding.ingredientContainer.setVisibility(View.VISIBLE);
    } else {
      binding.ingredientContainer.setVisibility(View.GONE);
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

  @Override
  public void onItemRowClicked(RecipePositionResolved recipePosition, int position) {
    if (recipePosition == null) {
      return;
    }

    recipePosition.toggleChecked();
    RecipePositionResolvedAdapter adapter = (RecipePositionResolvedAdapter) binding.recycler
        .getAdapter();
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
    if (binding.recycler.getAdapter() == null) return;
    if (binding.recycler.getAdapter() instanceof RecipePositionAdapter) {
      for (Product product : ((RecipePositionAdapter) binding.recycler.getAdapter()).getMissingProducts()) {
        dialogShoppingListMultiChoiceItems.put(product.getName(), true);
      }
    } else {
      for (Product product : ((RecipePositionResolvedAdapter) binding.recycler.getAdapter()).getMissingProducts()) {
        dialogShoppingListMultiChoiceItems.put(product.getName(), true);
      }
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
    Typeface jost = ResourcesCompat.getFont(activity, R.font.google_sans_flex);

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
        .setMessage(getString(R.string.msg_master_delete, getString(R.string.title_recipe), recipe.getName()))
        .setPositiveButton(R.string.action_delete, (dialog, which) -> {
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
      activity.navUtil.navigate(RecipeFragmentDirections
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

  private int[] getExcludedProductIds() {
    List<Integer> excludedIds = new ArrayList<>();
    for (String productName : dialogShoppingListMultiChoiceItems.keySet()) {
      if (Boolean.TRUE.equals(dialogShoppingListMultiChoiceItems.get(productName))) continue;
      Product product = Product.getProductFromName(viewModel.getProducts(), productName);
      if (product != null) excludedIds.add(product.getId());
    }
    return excludedIds.stream().mapToInt(i -> i).toArray();
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