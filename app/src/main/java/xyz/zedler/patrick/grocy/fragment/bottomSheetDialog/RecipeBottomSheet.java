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
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipePositionAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetRecipeBinding;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil.TouchProgressBarUtil;

public class RecipeBottomSheet extends BaseBottomSheet implements
        RecipePositionAdapter.RecipePositionsItemAdapterListener {

  private final static String TAG = RecipeBottomSheet.class.getSimpleName();

  private SharedPreferences sharedPrefs;
  private MainActivity activity;
  private FragmentBottomsheetRecipeBinding binding;
  private ViewUtil.TouchProgressBarUtil touchProgressBarUtil;
  private Recipe recipe;

  private MutableLiveData<String> servingsDesiredLive;

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

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.setLifecycleOwner(getViewLifecycleOwner());
    binding.setBottomSheet(this);

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

    for (RecipePosition recipePosition: recipePositions) {
      if (recipePosition.isChecked())
        recipePosition.setChecked(false);
    }

    binding.name.setText(recipe.getName());
    servingsDesiredLive = new MutableLiveData<>(NumUtil.trim(recipe.getDesiredServings()));
    binding.textInputServings.setHelperText(getString(R.string.property_servings_base_insert, NumUtil.trim(recipe.getBaseServings())));
    binding.calories.setText(getString(R.string.property_energy), NumUtil.trim(recipeFulfillment.getCalories()), getString(R.string.subtitle_per_serving));
    binding.costs.setText(getString(R.string.property_costs), NumUtil.trimPrice(recipeFulfillment.getCosts()) + " " + sharedPrefs.getString(Constants.PREF.CURRENCY, ""));

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

    binding.menuItemConsume.setOnClickListener(v -> {
      activity.getCurrentFragment().consumeRecipe(recipe.getId());
      dismiss();
    });
    binding.menuItemShoppingList.setOnClickListener(v -> {
      activity.getCurrentFragment().addNotFulfilledProductsToCartForRecipe(recipe.getId());
      dismiss();
    });
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

    if (recipe.getPictureFileName() != null) {
      GrocyApi grocyApi = new GrocyApi(activity.getApplication());
      binding.picture.layout(0, 0, 0, 0);
      Glide
          .with(requireContext())
          .load(grocyApi.getRecipePicture(recipe.getPictureFileName()))
          .transform(new CenterCrop(), new RoundedCorners(UnitUtil.dpToPx(requireContext(), 12)))
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

  public void onItemRowClicked(RecipePosition recipePosition, int position) {
    if (recipePosition == null) {
      return;
    }

    recipePosition.toggleChecked();
    RecipePositionAdapter adapter = (RecipePositionAdapter)binding.recycler.getAdapter();
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

  }

  public void updateDataWithServings() {

  }

  public void clearServingsFieldAndFocusIt() {

  }

  public void clearInputFocus() {

  }

  public MutableLiveData<String> getServingsDesiredLive() {
    return servingsDesiredLive;
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
