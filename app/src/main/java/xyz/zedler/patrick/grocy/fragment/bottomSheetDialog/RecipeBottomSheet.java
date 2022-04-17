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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipeEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipePositionAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetRecipeBinding;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetTaskEntryBinding;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.repository.RecipesRepository;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class RecipeBottomSheet extends BaseBottomSheet implements
        RecipePositionAdapter.RecipePositionsItemAdapterListener {

  private final static int DELETE_CONFIRMATION_DURATION = 1000;
  private final static String TAG = RecipeBottomSheet.class.getSimpleName();

  private SharedPreferences sharedPrefs;
  private MainActivity activity;
  private FragmentBottomsheetRecipeBinding binding;
  private ProgressBar progressConfirm;
  private ValueAnimator confirmProgressAnimator;
  private Recipe recipe;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

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
    if (confirmProgressAnimator != null) {
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding.recycler.setAdapter(null);
      binding = null;
    }
    super.onDestroyView();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplication());

    binding.recycler.setLayoutManager(
            new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
    }

    Bundle bundle = getArguments();
    RecipeFulfillment recipeFulfillment;
    ArrayList<RecipePosition> recipePositions;
    ArrayList<Product> products;
    ArrayList<QuantityUnit> quantityUnits;

    if (bundle == null) {
      dismiss();
      return;
    } else {
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
    }

    binding.name.setText(getString(R.string.property_name), recipe.getName());
    binding.calories.setText(getString(R.string.property_calories), NumUtil.trim(recipeFulfillment.getCalories()));
    binding.costs.setText(getString(R.string.property_costs), NumUtil.trimPrice(recipeFulfillment.getCosts()) + " " + sharedPrefs.getString(Constants.PREF.CURRENCY, ""));
    binding.baseServings.setText(getString(R.string.property_base_servings), NumUtil.trim(recipe.getBaseServings()));
    binding.dueScore.setText(getString(R.string.property_due_score), String.valueOf(recipeFulfillment.getDueScore()));

    if (recipePositions.isEmpty()) {
      binding.ingredientContainer.setVisibility(View.GONE);
    } else {
      binding.ingredientsHeadline.setText(getText(R.string.property_ingredients));
      if (binding.recycler.getAdapter() instanceof RecipePositionAdapter) {
        ((RecipePositionAdapter) binding.recycler.getAdapter()).updateData(recipePositions, products, quantityUnits);
      } else {
        binding.recycler.setAdapter(
                new RecipePositionAdapter(
                        requireContext(),
                        (LinearLayoutManager) binding.recycler.getLayoutManager(),
                        recipePositions,
                        products,
                        quantityUnits,
                        this
                )
        );
      }
    }

    if (recipe.getDescription() == null || recipe.getDescription().trim().isEmpty()) {
      binding.description.setVisibility(View.GONE);
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        binding.description.setText(Html.fromHtml(recipe.getDescription(), Html.FROM_HTML_MODE_LEGACY));
      } else {
        binding.description.setText(Html.fromHtml(recipe.getDescription()));
      }
    }

    binding.toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_consume) {
        activity.getCurrentFragment().consumeRecipe(recipe.getId());
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_add_not_fulfilled_products_to_cart_for_recipe) {
        activity.getCurrentFragment().addNotFulfilledProductsToCartForRecipe(recipe.getId());
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_edit) {
        activity.getCurrentFragment().editRecipe(recipe);
        dismiss();
        return true;
      }
      return false;
    });
    binding.delete.setOnTouchListener((v, event) -> {
      onTouchDelete(v, event);
      return true;
    });
    progressConfirm = binding.progressConfirmation;
  }

  public void onItemRowClicked(RecipePosition recipePosition) {
    if (recipePosition == null) {
      return;
    }

    activity.showMessage("Not implemented yet :(");
  }

  public void onTouchDelete(View view, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      showAndStartProgress(view);
    } else if (event.getAction() == MotionEvent.ACTION_UP
        || event.getAction() == MotionEvent.ACTION_CANCEL) {
      hideAndStopProgress();
    }
  }

  private void showAndStartProgress(View buttonView) {
    assert getView() != null;
    TransitionManager.beginDelayedTransition((ViewGroup) getView());
    progressConfirm.setVisibility(View.VISIBLE);
    int startValue = 0;
    if (confirmProgressAnimator != null) {
      startValue = progressConfirm.getProgress();
      if (startValue == 100) {
        startValue = 0;
      }
      confirmProgressAnimator.removeAllListeners();
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    confirmProgressAnimator = ValueAnimator.ofInt(startValue, progressConfirm.getMax());
    confirmProgressAnimator.setDuration((long) DELETE_CONFIRMATION_DURATION
        * (progressConfirm.getMax() - startValue) / progressConfirm.getMax());
    confirmProgressAnimator.addUpdateListener(
        animation -> progressConfirm.setProgress((Integer) animation.getAnimatedValue())
    );
    confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        int currentProgress = progressConfirm.getProgress();
        if (currentProgress == progressConfirm.getMax()) {
          TransitionManager.beginDelayedTransition((ViewGroup) requireView());
          progressConfirm.setVisibility(View.GONE);
          ImageView buttonImage = buttonView.findViewById(R.id.image_action_button);
          ((Animatable) buttonImage.getDrawable()).start();
          activity.getCurrentFragment().deleteRecipe(recipe.getId());
          dismiss();
          return;
        }
        confirmProgressAnimator = ValueAnimator.ofInt(currentProgress, 0);
        confirmProgressAnimator.setDuration((long) (DELETE_CONFIRMATION_DURATION / 2)
            * currentProgress / progressConfirm.getMax());
        confirmProgressAnimator.setInterpolator(new FastOutSlowInInterpolator());
        confirmProgressAnimator.addUpdateListener(
            anim -> progressConfirm.setProgress((Integer) anim.getAnimatedValue())
        );
        confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            TransitionManager.beginDelayedTransition((ViewGroup) requireView());
            progressConfirm.setVisibility(View.GONE);
          }
        });
        confirmProgressAnimator.start();
      }
    });
    confirmProgressAnimator.start();
  }

  private void hideAndStopProgress() {
    if (confirmProgressAnimator != null) {
      confirmProgressAnimator.cancel();
    }

    if (progressConfirm.getProgress() != 100) {
      Toast.makeText(requireContext(), R.string.msg_press_hold_confirm, Toast.LENGTH_LONG).show();
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

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
