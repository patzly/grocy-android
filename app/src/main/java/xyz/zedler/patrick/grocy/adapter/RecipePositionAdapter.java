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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowRecipePositionEntryBinding;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class RecipePositionAdapter extends
    RecyclerView.Adapter<RecipePositionAdapter.ViewHolder> {

  private final static String TAG = RecipePositionAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LinearLayoutManager linearLayoutManager;
  private Recipe recipe;
  private final List<RecipePosition> recipePositions;
  private final List<Product> products;
  private final List<QuantityUnit> quantityUnits;
  private final RecipePositionsItemAdapterListener listener;

  private final PluralUtil pluralUtil;

  public RecipePositionAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      Recipe recipe,
      List<RecipePosition> recipePositions,
      List<Product> products,
      List<QuantityUnit> quantityUnits,
      RecipePositionsItemAdapterListener listener
  ) {
    this.context = context;
    this.linearLayoutManager = linearLayoutManager;
    this.recipe = recipe;
    this.recipePositions = new ArrayList<>(recipePositions);
    this.products = new ArrayList<>(products);
    this.quantityUnits = new ArrayList<>(quantityUnits);
    this.listener = listener;
    this.pluralUtil = new PluralUtil(context);
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

  public static class RecipePositionViewHolder extends ViewHolder {

    private final RowRecipePositionEntryBinding binding;

    public RecipePositionViewHolder(RowRecipePositionEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new RecipePositionViewHolder(RowRecipePositionEntryBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();
    RecipePositionViewHolder holder = (RecipePositionViewHolder) viewHolder;

    RecipePosition recipePosition = recipePositions.get(position);
    Product product = Product.getProductFromId(products, recipePosition.getProductId());
    QuantityUnit quantityUnit = QuantityUnit.getFromId(quantityUnits, recipePosition.getQuantityUnitId());

    // AMOUNT
    double amount = recipePosition.getAmount() / recipe.getBaseServings() * recipe.getDesiredServings();
    if (recipePosition.getVariableAmount() == null || recipePosition.getVariableAmount().isEmpty()) {
      holder.binding.amount.setText(NumUtil.trim(amount));
      holder.binding.variableAmount.setVisibility(View.GONE);
    } else {
      holder.binding.amount.setText(recipePosition.getVariableAmount());
      holder.binding.variableAmount.setVisibility(View.VISIBLE);
    }

    // QUANTITY UNIT
    holder.binding.quantityUnit.setText(pluralUtil.getQuantityUnitPlural(quantityUnit, amount));

    // NAME
    holder.binding.title.setText(product != null ? product.getName() : context.getString(R.string.error_undefined));

    // NOTE
    if (recipePosition.getNote() == null) {
      holder.binding.note.setVisibility(View.GONE);
    } else {
      holder.binding.note.setText(recipePosition.getNote());
      holder.binding.note.setVisibility(View.VISIBLE);
    }

    if (recipePosition.isChecked()) {
      holder.binding.linearRecipePositionContainer.setAlpha(0.5f);
      //holder.binding.title.setPaintFlags(holder.binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
      //holder.binding.title.setTextColor(ContextCompat.getColor(context, R.color.on_background_secondary));
    } else {
      holder.binding.linearRecipePositionContainer.setAlpha(1f);
      //holder.binding.title.setPaintFlags(holder.binding.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
      //holder.binding.title.setTextColor(ContextCompat.getColor(context, R.color.on_background));
    }

    // CONTAINER
    holder.binding.linearRecipePositionContainer.setOnClickListener(
        view -> listener.onItemRowClicked(recipePosition, position)
    );
  }

  @Override
  public int getItemCount() {
    return recipePositions.size();
  }

  public interface RecipePositionsItemAdapterListener {

    void onItemRowClicked(RecipePosition recipePosition, int position);
  }

  public void updateData(
      Recipe recipe,
      List<RecipePosition> newList,
      List<Product> newProducts,
      List<QuantityUnit> newQuantityUnits
  ) {

    RecipePositionAdapter.DiffCallback diffCallback = new RecipePositionAdapter.DiffCallback(
        this.recipe,
        recipe,
        this.recipePositions,
        newList,
        this.products,
        newProducts,
        this.quantityUnits,
        newQuantityUnits
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipe = recipe;
    this.recipePositions.clear();
    this.recipePositions.addAll(newList);
    this.products.clear();
    this.products.addAll(newProducts);
    this.quantityUnits.clear();
    this.quantityUnits.addAll(newQuantityUnits);
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    Recipe oldRecipe;
    Recipe newRecipe;
    List<RecipePosition> oldItems;
    List<RecipePosition> newItems;
    List<Product> oldProducts;
    List<Product> newProducts;
    List<QuantityUnit> oldQuantityUnits;
    List<QuantityUnit> newQuantityUnits;

    public DiffCallback(
        Recipe oldRecipe,
        Recipe newRecipe,
        List<RecipePosition> oldItems,
        List<RecipePosition> newItems,
        List<Product> oldProducts,
        List<Product> newProducts,
        List<QuantityUnit> oldQuantityUnits,
        List<QuantityUnit> newQuantityUnits
    ) {
      this.oldRecipe = oldRecipe;
      this.newRecipe = newRecipe;
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldProducts = oldProducts;
      this.newProducts = newProducts;
      this.oldQuantityUnits = oldQuantityUnits;
      this.newQuantityUnits = newQuantityUnits;
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
      if (!Objects.equals(newRecipe.getDesiredServings(), oldRecipe.getDesiredServings())) {
        return false;
      }
      RecipePosition newItem = newItems.get(newItemPos);
      RecipePosition oldItem = oldItems.get(oldItemPos);
      Product newItemProduct = Product.getProductFromId(newProducts, newItem.getProductId());
      Product oldItemProduct = Product.getProductFromId(oldProducts, oldItem.getProductId());
      QuantityUnit newQuantityUnit = QuantityUnit.getFromId(newQuantityUnits, newItem.getQuantityUnitId());
      QuantityUnit oldQuantityUnit = QuantityUnit.getFromId(oldQuantityUnits, oldItem.getQuantityUnitId());

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      if (newItemProduct == null || !newItemProduct.equals(oldItemProduct)) {
        return false;
      }

      if (newQuantityUnit == null || !newQuantityUnit.equals(oldQuantityUnit)) {
        return false;
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final RecipePositionAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull RecipePositionAdapter adapter,
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
