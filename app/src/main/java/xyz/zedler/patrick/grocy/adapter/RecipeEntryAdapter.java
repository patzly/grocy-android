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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryBinding;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;

public class RecipeEntryAdapter extends
    RecyclerView.Adapter<RecipeEntryAdapter.ViewHolder> {

  private final static String TAG = RecipeEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LinearLayoutManager linearLayoutManager;
  private final ArrayList<Recipe> recipes;
  private final ArrayList<RecipeFulfillment> recipeFulfillments;
  private final RecipesItemAdapterListener listener;
  private String sortMode;
  private boolean sortAscending;

  public RecipeEntryAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      ArrayList<Recipe> recipes,
      ArrayList<RecipeFulfillment> recipeFulfillments,
      RecipesItemAdapterListener listener,
      String sortMode,
      boolean sortAscending
  ) {
    this.context = context;
    this.linearLayoutManager = linearLayoutManager;
    this.recipes = new ArrayList<>(recipes);
    this.recipeFulfillments = new ArrayList<>(recipeFulfillments);
    this.listener = listener;
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
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

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new RecipeViewHolder(RowRecipeEntryBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();

    Recipe recipe = recipes.get(position);
    RecipeFulfillment recipeFulfillment = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe.getId());
    RecipeViewHolder holder = (RecipeViewHolder) viewHolder;

    // NAME

    holder.binding.title.setText(recipe.getName());

    if (recipeFulfillment != null) {
      // CALORIES

      holder.binding.calories.setText(
              context.getString(
                      R.string.subtitle_recipe_calories,
                      String.valueOf(recipeFulfillment.getCalories())
              )
      );

      // DUE SCORE
      int due_score = recipeFulfillment.getDueScore();
      @ColorRes int color;

      holder.binding.dueScore.setTypeface(
              ResourcesCompat.getFont(context, R.font.jost_medium)
      );

      if (due_score == 0) {
        color = R.color.retro_green_fg;
      }
      else if (due_score <= 10) {
        color = R.color.retro_yellow_fg;
      }
      else {
        color = R.color.retro_red_fg;
      }
      holder.binding.dueScore.setTextColor(ContextCompat.getColor(context, color));

      holder.binding.dueScore.setText(
              context.getString(
                      R.string.subtitle_recipe_due_score,
                      String.valueOf(recipeFulfillment.getDueScore())
              )
      );
    }

    // CONTAINER

    holder.binding.linearRecipeEntryContainer.setOnClickListener(
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
      String sortMode,
      boolean sortAscending
  ) {

    RecipeEntryAdapter.DiffCallback diffCallback = new RecipeEntryAdapter.DiffCallback(
        this.recipes,
        newList,
        this.recipeFulfillments,
        newRecipeFulfillments,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipes.clear();
    this.recipes.addAll(newList);
    this.recipeFulfillments.clear();
    this.recipeFulfillments.addAll(newRecipeFulfillments);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Recipe> oldItems;
    ArrayList<Recipe> newItems;
    ArrayList<RecipeFulfillment> oldRecipeFulfillments;
    ArrayList<RecipeFulfillment> newRecipeFulfillments;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;

    public DiffCallback(
        ArrayList<Recipe> oldItems,
        ArrayList<Recipe> newItems,
        ArrayList<RecipeFulfillment> oldRecipeFulfillments,
        ArrayList<RecipeFulfillment> newRecipeFulfillments,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldRecipeFulfillments = oldRecipeFulfillments;
      this.newRecipeFulfillments = newRecipeFulfillments;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
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

      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }
      if (sortAscendingOld != sortAscendingNew) {
        return false;
      }

      RecipeFulfillment recipeFulfillmentOld = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(oldRecipeFulfillments, oldItem.getId());
      RecipeFulfillment recipeFulfillmentNew = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(newRecipeFulfillments, newItem.getId());
      if (recipeFulfillmentOld == null && recipeFulfillmentNew != null
          || recipeFulfillmentOld != null && recipeFulfillmentNew == null
          || recipeFulfillmentOld != null && !recipeFulfillmentOld.equals(recipeFulfillmentNew)) {
        return false;
      }

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final RecipeEntryAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull RecipeEntryAdapter adapter,
        LinearLayoutManager linearLayoutManager
    ) {
      this.mAdapter = adapter;
      this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      // workaround for https://github.com/patzly/grocy-android/issues/439
      // figure out the position of the first visible item
      int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
      int offsetTop = 0;
      if(firstPos >= 0) {
        View firstView = linearLayoutManager.findViewByPosition(firstPos);
        if (firstView != null) {
          offsetTop = linearLayoutManager.getDecoratedTop(firstView)
              - linearLayoutManager.getTopDecorationHeight(firstView);
        }
      }

      mAdapter.notifyItemMoved(fromPosition, toPosition);

      // reapply the saved position
      if(firstPos >= 0) {
        linearLayoutManager.scrollToPositionWithOffset(firstPos, offsetTop);
      }
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }
}
