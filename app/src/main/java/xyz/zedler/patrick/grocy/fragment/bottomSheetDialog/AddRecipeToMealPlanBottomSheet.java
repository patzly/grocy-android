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
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetAddRecipeToMealPlanBinding;
import xyz.zedler.patrick.grocy.fragment.MealPlanFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class AddRecipeToMealPlanBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = AddRecipeToMealPlanBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetAddRecipeToMealPlanBinding binding;
  private DownloadHelper dlHelper;

  private MutableLiveData<String> recipeLive;
  private MutableLiveData<String> servingsLive;
  private MutableLiveData<String> dateLive;
  private MutableLiveData<String> sectionLive;
  private MutableLiveData<Boolean> canSaveLive;

  private Recipe selectedRecipe;
  private LocalDate selectedDate;
  private ArrayList<Recipe> recipes;
  private ArrayList<xyz.zedler.patrick.grocy.model.MealPlanSection> sections;
  private xyz.zedler.patrick.grocy.model.MealPlanSection selectedSection;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetAddRecipeToMealPlanBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    activity = (MainActivity) requireActivity();
    binding.setBottomsheet(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    dlHelper = new DownloadHelper(activity, TAG);

    recipeLive = new MutableLiveData<>(getString(R.string.subtitle_none_selected));
    servingsLive = new MutableLiveData<>("1");
    dateLive = new MutableLiveData<>();
    sectionLive = new MutableLiveData<>(getString(R.string.subtitle_none));
    canSaveLive = new MutableLiveData<>(false);

    // Get arguments
    Bundle bundle = requireArguments();
    if (bundle.containsKey(ARGUMENT.SELECTED_DATE)) {
      selectedDate = (LocalDate) bundle.getSerializable(ARGUMENT.SELECTED_DATE);
      if (selectedDate != null) {
        dateLive.setValue(selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
      }
    }

    if (bundle.containsKey(ARGUMENT.RECIPES)) {
      recipes = bundle.getParcelableArrayList(ARGUMENT.RECIPES);
    } else {
      recipes = new ArrayList<>();
    }

    if (bundle.containsKey(ARGUMENT.MEAL_PLAN_SECTIONS)) {
      sections = bundle.getParcelableArrayList(ARGUMENT.MEAL_PLAN_SECTIONS);
    } else {
      sections = new ArrayList<>();
    }

    // Observe servings changes to update save button state
    servingsLive.observeForever(this::updateCanSave);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (dlHelper != null) {
      dlHelper.destroy();
    }
    binding = null;
  }

  public MutableLiveData<String> getRecipeLive() {
    return recipeLive;
  }

  public MutableLiveData<String> getServingsLive() {
    return servingsLive;
  }

  public MutableLiveData<String> getDateLive() {
    return dateLive;
  }

  public MutableLiveData<String> getSectionLive() {
    return sectionLive;
  }

  public MutableLiveData<Boolean> getCanSaveLive() {
    return canSaveLive;
  }

  public void showRecipeBottomSheet() {
    if (recipes == null || recipes.isEmpty()) {
      activity.showSnackbar(R.string.error_no_recipes, false);
      return;
    }

    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.RECIPES, recipes);
    if (selectedRecipe != null) {
      bundle.putInt(ARGUMENT.SELECTED_ID, selectedRecipe.getId());
    }
    activity.showBottomSheet(new RecipesBottomSheet(), bundle);
  }

  public void showSectionBottomSheet() {
    if (sections == null || sections.isEmpty()) {
      // No sections available, keep it as "None"
      return;
    }

    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.MEAL_PLAN_SECTIONS, sections);
    if (selectedSection != null) {
      bundle.putString(ARGUMENT.SELECTED_ID, String.valueOf(selectedSection.getId()));
    }
    activity.showBottomSheet(new MealPlanSectionsSelectionBottomSheet(), bundle);
  }

  public void selectSection(xyz.zedler.patrick.grocy.model.MealPlanSection section) {
    selectedSection = section;
    if (section == null || section.getId() == -1) {
      sectionLive.setValue(getString(R.string.subtitle_none));
      selectedSection = null;
    } else {
      sectionLive.setValue(section.getName());
    }
  }

  public void selectRecipe(Recipe recipe) {
    selectedRecipe = recipe;
    recipeLive.setValue(recipe.getName());

    // Set default servings from recipe base servings if available
    if (recipe.getBaseServings() != null && recipe.getBaseServings() > 0) {
      servingsLive.setValue(NumUtil.trimAmount(recipe.getBaseServings(), 2));
    }

    updateCanSave(servingsLive.getValue());
  }

  private void updateCanSave(String servings) {
    boolean canSave = selectedRecipe != null
        && servings != null
        && !servings.trim().isEmpty()
        && NumUtil.isStringNum(servings)
        && NumUtil.toDouble(servings) > 0;
    canSaveLive.setValue(canSave);
  }

  public void saveRecipe() {
    if (selectedRecipe == null || selectedDate == null) {
      return;
    }

    String servings = servingsLive.getValue();
    if (servings == null || servings.trim().isEmpty() || !NumUtil.isStringNum(servings)) {
      activity.showSnackbar(R.string.error_invalid_servings, false);
      return;
    }

    // Disable save button while saving
    canSaveLive.setValue(false);

    // Create JSON object for API request
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("recipe_id", String.valueOf(selectedRecipe.getId()));
      jsonObject.put("recipe_servings", servings);
      jsonObject.put("day", selectedDate.toString());
      jsonObject.put("type", "recipe");
      if (selectedSection != null && selectedSection.getId() != -1) {
        jsonObject.put("section_id", String.valueOf(selectedSection.getId()));
      } else {
        // Explicitly set to null if no section or "None" selected
        jsonObject.put("section_id", JSONObject.NULL);
      }
    } catch (JSONException e) {
      activity.showSnackbar(R.string.error_undefined, false);
      canSaveLive.setValue(true);
      return;
    }

    // Make API request
    dlHelper.post(
        dlHelper.grocyApi.getObjects(xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY.MEAL_PLAN),
        jsonObject,
        response -> {
          activity.showSnackbar(R.string.msg_recipe_added_to_meal_plan, false);
          // Refresh meal plan data
          if (activity.getCurrentFragment() instanceof MealPlanFragment) {
            ((MealPlanFragment) activity.getCurrentFragment()).performAction(Constants.ACTION.REFRESH);
          }
          dismiss();
        },
        error -> {
          activity.showSnackbar(R.string.error_undefined, false);
          canSaveLive.setValue(true);
        }
    );
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainer.setPadding(
        binding.linearContainer.getPaddingLeft(),
        binding.linearContainer.getPaddingTop(),
        binding.linearContainer.getPaddingRight(),
        UiUtil.dpToPx(activity, 12) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
