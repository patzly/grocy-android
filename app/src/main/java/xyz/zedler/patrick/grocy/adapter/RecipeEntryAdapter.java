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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryBinding;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryGridBinding;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.ChipUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.viewmodel.RecipesViewModel;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeEntryAdapter extends
    RecyclerView.Adapter<RecipeEntryAdapter.ViewHolder> {

  private final static String TAG = RecipeEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LayoutManager layoutManager;
  private final ArrayList<Recipe> recipes;
  private final ArrayList<RecipeFulfillment> recipeFulfillments;
  private final HashMap<String, Userfield> userfieldHashMap;
  private final RecipesItemAdapterListener listener;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private String sortMode;
  private boolean sortAscending;
  private final List<String> activeFields;
  private final int maxDecimalPlacesAmount;
  private final String energyUnit;
  private boolean containsPictures;

  public RecipeEntryAdapter(
      Context context,
      LayoutManager layoutManager,
      RecipesItemAdapterListener listener
  ) {
    this.context = context;
    this.layoutManager = layoutManager;
    this.recipes = new ArrayList<>();
    this.recipeFulfillments = new ArrayList<>();
    this.userfieldHashMap = new HashMap<>();
    this.listener = listener;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
    this.activeFields = new ArrayList<>();
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    energyUnit = sharedPrefs.getString(PREF.ENERGY_UNIT, PREF.ENERGY_UNIT_DEFAULT);
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class RecipeViewHolder extends ViewHolder {

    private final RowRecipeEntryBinding binding;

    public RecipeViewHolder(RowRecipeEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class RecipeGridViewHolder extends ViewHolder {

    private final RowRecipeEntryGridBinding binding;
    StaggeredGridLayoutManager.LayoutParams layoutParams;

    public RecipeGridViewHolder(RowRecipeEntryGridBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      layoutParams = (StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams();
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (layoutManager instanceof LinearLayoutManager) {
      return new RecipeViewHolder(RowRecipeEntryBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    } else {
      return new RecipeGridViewHolder(RowRecipeEntryGridBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    }
  }

  @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {
    int position = viewHolder.getAbsoluteAdapterPosition();

    Recipe recipe = recipes.get(position);
    RecipeFulfillment recipeFulfillment = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(
        recipeFulfillments, recipe.getId()
    );

    ViewGroup container;
    TextView title;
    ImageView picture;
    MaterialCardView picturePlaceholder = null;
    FlexboxLayout chips;

    if (viewHolder instanceof RecipeViewHolder) {
      container = ((RecipeViewHolder) viewHolder).binding.container;
      title = ((RecipeViewHolder) viewHolder).binding.title;
      chips = ((RecipeViewHolder) viewHolder).binding.flexboxLayout;
      picture = ((RecipeViewHolder) viewHolder).binding.picture;
      picturePlaceholder = ((RecipeViewHolder) viewHolder).binding.picturePlaceholder;
    } else {
      container = ((RecipeGridViewHolder) viewHolder).binding.container;
      title = ((RecipeGridViewHolder) viewHolder).binding.title;
      chips = ((RecipeGridViewHolder) viewHolder).binding.flexboxLayout;
      picture = ((RecipeGridViewHolder) viewHolder).binding.picture;
    }

    // NAME

    title.setText(recipe.getName());

    ChipUtil chipUtil = new ChipUtil(context);
    chips.removeAllViews();

    if (activeFields.contains(RecipesViewModel.FIELD_DUE_SCORE)
        && recipeFulfillment != null) {
      chips.addView(chipUtil.createRecipeDueScoreChip(recipeFulfillment.getDueScore()));
    }
    if (activeFields.contains(RecipesViewModel.FIELD_FULFILLMENT)
        && recipeFulfillment != null) {
      chips.addView(chipUtil.createRecipeFulfillmentChip(recipeFulfillment));
    }
    if (activeFields.contains(RecipesViewModel.FIELD_CALORIES)
        && recipeFulfillment != null) {
      chips.addView(chipUtil.createTextChip(NumUtil.trimAmount(
          recipeFulfillment.getCalories(), maxDecimalPlacesAmount
      ) + " " + energyUnit));
    }
    if (activeFields.contains(RecipesViewModel.FIELD_DESIRED_SERVINGS)
        && recipeFulfillment != null) {
      chips.addView(chipUtil.createTextChip(
          context.getString(R.string.property_servings_desired_insert, NumUtil.trimAmount(
          recipe.getDesiredServings(), maxDecimalPlacesAmount
          ))
      ));
    }
    for (String activeField : activeFields) {
      if (activeField.startsWith(Userfield.NAME_PREFIX)) {
        String userfieldName = activeField.substring(
            Userfield.NAME_PREFIX.length()
        );
        Userfield userfield = userfieldHashMap.get(userfieldName);
        if (userfield == null) continue;
        Chip chipUserfield = chipUtil.createUserfieldChip(
            userfield,
            recipe.getUserfields().get(userfieldName)
        );
        if (chipUserfield != null) chips.addView(chipUserfield);
      }
    }

    chips.setVisibility(chips.getChildCount() > 0 ? View.VISIBLE : View.GONE);

    String pictureFileName = recipe.getPictureFileName();
    if (activeFields.contains(RecipesViewModel.FIELD_PICTURE)
        && pictureFileName != null && !pictureFileName.isEmpty()) {
      picture.layout(0, 0, 0, 0);

      PictureUtil.loadPicture(
          picture,
          null,
          picturePlaceholder,
          grocyApi.getRecipePictureServeSmall(pictureFileName),
          grocyAuthHeaders,
          viewHolder instanceof RecipeGridViewHolder
      );
    } else if (activeFields.contains(RecipesViewModel.FIELD_PICTURE)
        && containsPictures && viewHolder instanceof RecipeViewHolder) {
      picture.setVisibility(View.GONE);
      picturePlaceholder.setVisibility(View.VISIBLE);
    } else {
      picture.setVisibility(View.GONE);
      if (picturePlaceholder != null) picturePlaceholder.setVisibility(View.GONE);
    }

    // CONTAINER

    container.setOnClickListener(
        view -> listener.onItemRowClicked(recipe)
    );
  }

  @Override
  public int getItemCount() {
    return recipes.size();
  }

  public interface RecipesItemAdapterListener {

    void onItemRowClicked(Recipe recipe);
  }

  public void updateData(
      ArrayList<Recipe> newList,
      ArrayList<RecipeFulfillment> newRecipeFulfillments,
      HashMap<String, Userfield> newUserfieldHashMap,
      String sortMode,
      boolean sortAscending,
      List<String> activeFields,
      Runnable onListFilled
  ) {
    RecipeEntryAdapter.DiffCallback diffCallback = new RecipeEntryAdapter.DiffCallback(
        this.recipes,
        newList,
        this.recipeFulfillments,
        newRecipeFulfillments,
        this.userfieldHashMap,
        newUserfieldHashMap,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending,
        this.activeFields,
        activeFields
    );

    if (onListFilled != null && !newList.isEmpty() && recipes.isEmpty()) {
      onListFilled.run();
    }

    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipes.clear();
    this.recipes.addAll(newList);
    this.recipeFulfillments.clear();
    this.recipeFulfillments.addAll(newRecipeFulfillments);
    this.userfieldHashMap.clear();
    this.userfieldHashMap.putAll(newUserfieldHashMap);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.activeFields.clear();
    this.activeFields.addAll(activeFields);
    diffResult.dispatchUpdatesTo(this);

    containsPictures = false;
    for (Recipe recipe : recipes) {
      String pictureFileName = recipe.getPictureFileName();
      if (pictureFileName != null && !pictureFileName.isEmpty()) {
        containsPictures = true;
        break;
      }
    }
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Recipe> oldItems;
    ArrayList<Recipe> newItems;
    ArrayList<RecipeFulfillment> oldRecipeFulfillments;
    ArrayList<RecipeFulfillment> newRecipeFulfillments;
    HashMap<String, Userfield> oldUserfieldHashMap;
    HashMap<String, Userfield> newUserfieldHashMap;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;
    List<String> activeFieldsOld;
    List<String> activeFieldsNew;

    public DiffCallback(
        ArrayList<Recipe> oldItems,
        ArrayList<Recipe> newItems,
        ArrayList<RecipeFulfillment> oldRecipeFulfillments,
        ArrayList<RecipeFulfillment> newRecipeFulfillments,
        HashMap<String, Userfield> oldUserfieldHashMap,
        HashMap<String, Userfield> newUserfieldHashMap,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew,
        List<String> activeFieldsOld,
        List<String> activeFieldsNew
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldRecipeFulfillments = oldRecipeFulfillments;
      this.newRecipeFulfillments = newRecipeFulfillments;
      this.oldUserfieldHashMap = oldUserfieldHashMap;
      this.newUserfieldHashMap = newUserfieldHashMap;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
      this.activeFieldsOld = activeFieldsOld;
      this.activeFieldsNew = activeFieldsNew;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      Recipe newItem = newItems.get(newItemPos);
      Recipe oldItem = oldItems.get(oldItemPos);

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }
      if (!ArrayUtil.areListsEqualIgnoreOrder(activeFieldsOld, activeFieldsNew)) {
        return false;
      }
      if (!oldUserfieldHashMap.equals(newUserfieldHashMap)) {
        return false;
      }

      RecipeFulfillment recipeFulfillmentOld = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(oldRecipeFulfillments, oldItem.getId());
      RecipeFulfillment recipeFulfillmentNew = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(newRecipeFulfillments, newItem.getId());
      if (recipeFulfillmentOld == null && recipeFulfillmentNew != null
          || recipeFulfillmentOld != null && recipeFulfillmentNew == null
          || recipeFulfillmentOld != null && !recipeFulfillmentOld.equals(recipeFulfillmentNew)) {
        return false;
      }

      return newItem.equalsForListDiff(oldItem);
    }
  }
}
