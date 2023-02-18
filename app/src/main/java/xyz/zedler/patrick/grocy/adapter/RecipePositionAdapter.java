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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.ColorRoles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowRecipePositionEntryBinding;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

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
  private final List<QuantityUnitConversion> quantityUnitConversions;
  private final HashMap<Integer, StockItem> stockItemHashMap;
  private final List<ShoppingListItem> shoppingListItems;
  private final RecipePositionsItemAdapterListener listener;

  private final PluralUtil pluralUtil;
  private final int maxDecimalPlacesAmount;
  private final ColorRoles colorBlue;
  private final ColorRoles colorGreen;
  private final ColorRoles colorYellow;
  private final ColorRoles colorRed;

  public RecipePositionAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      Recipe recipe,
      List<RecipePosition> recipePositions,
      List<Product> products,
      List<QuantityUnit> quantityUnits,
      List<QuantityUnitConversion> quantityUnitConversions,
      HashMap<Integer, StockItem> stockItemHashMap,
      List<ShoppingListItem> shoppingListItems,
      RecipePositionsItemAdapterListener listener
  ) {
    this.context = context;
    maxDecimalPlacesAmount = PreferenceManager.getDefaultSharedPreferences(context).getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    this.linearLayoutManager = linearLayoutManager;
    this.recipe = recipe;
    this.recipePositions = new ArrayList<>(recipePositions);
    this.products = new ArrayList<>(products);
    this.quantityUnits = new ArrayList<>(quantityUnits);
    this.quantityUnitConversions = new ArrayList<>(quantityUnitConversions);
    this.stockItemHashMap = stockItemHashMap != null ? new HashMap<>(stockItemHashMap) : new HashMap<>();
    this.shoppingListItems = shoppingListItems != null ? new ArrayList<>(shoppingListItems) : new ArrayList<>();
    this.listener = listener;
    this.pluralUtil = new PluralUtil(context);

    colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);
    colorGreen = ResUtil.getHarmonizedRoles(context, R.color.green);
    colorYellow = ResUtil.getHarmonizedRoles(context, R.color.yellow);
    colorRed = ResUtil.getHarmonizedRoles(context, R.color.red);
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

  public List<Product> getMissingProducts() {
    ArrayList<Product> missingProducts = new ArrayList<>();
    for (RecipePosition recipePosition : recipePositions) {
      Product product = Product.getProductFromId(products, recipePosition.getProductId());
      QuantityUnitConversion quantityUnitConversion = product != null
          ? QuantityUnitConversion.getFromTwoUnits(
          quantityUnitConversions,
          product.getQuIdStockInt(),
          recipePosition.getQuantityUnitId(),
          product.getId()
      ) : null;
      double amountStockUnit = recipePosition.getAmount() /
          recipe.getBaseServings() * recipe.getDesiredServings();
      double amountRecipeUnit = amountStockUnit;
      if (quantityUnitConversion != null && !recipePosition.isOnlyCheckSingleUnitInStock()) {
        amountRecipeUnit *= quantityUnitConversion.getFactor();
      }
      if (stockItemHashMap.isEmpty()) continue;
      StockItem stockItem = stockItemHashMap.get(recipePosition.getProductId());
      double amountMissing = getAmountMissing(recipePosition, stockItem, amountStockUnit, amountRecipeUnit);
      double amountShoppingList = getAmountOnShoppingList(recipePosition, quantityUnitConversion);
      if (amountMissing > 0 && amountShoppingList < amountMissing) missingProducts.add(product);
    }
    return missingProducts;
  }

  private double getAmountOnShoppingList(RecipePosition recipePosition, QuantityUnitConversion conversion) {
    double amountStockUnit = 0;
    for (ShoppingListItem shoppingListItem : shoppingListItems) {
      if (!shoppingListItem.hasProduct() || shoppingListItem.getProductIdInt() != recipePosition.getProductId()) continue;
      amountStockUnit += shoppingListItem.getAmountDouble();
    }
    return conversion != null ? amountStockUnit * conversion.getFactor() : amountStockUnit;
  }

  private double getAmountMissing(
      RecipePosition recipePosition,
      StockItem stockItem,
      double amountStockUnit,
      double amountRecipeUnit
  ) {
    double amountStock = stockItem != null ? stockItem.getAmountDouble() : 0;
    double amountMissing;
    if (recipePosition.isOnlyCheckSingleUnitInStock()) {
      amountMissing = amountStockUnit / amountRecipeUnit - amountStock;
    } else {
      amountMissing = amountStockUnit - amountStock;
    }
    return amountMissing >= 0 ? amountMissing : 0;
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();
    RecipePositionViewHolder holder = (RecipePositionViewHolder) viewHolder;

    RecipePosition recipePosition = recipePositions.get(position);
    Product product = Product.getProductFromId(products, recipePosition.getProductId());
    QuantityUnit quantityUnit = QuantityUnit.getFromId(
        quantityUnits, recipePosition.getQuantityUnitId()
    );
    QuantityUnitConversion quantityUnitConversion = product != null
        ? QuantityUnitConversion.getFromTwoUnits(
            quantityUnitConversions,
            product.getQuIdStockInt(),
            recipePosition.getQuantityUnitId(),
            product.getId()
        ) : null;

    // AMOUNT
    double amountStockUnit = recipePosition.getAmount() /
        recipe.getBaseServings() * recipe.getDesiredServings();
    double amountRecipeUnit = amountStockUnit;
    if (quantityUnitConversion != null && !recipePosition.isOnlyCheckSingleUnitInStock()) {
      amountRecipeUnit *= quantityUnitConversion.getFactor();
    }
    String amountString;
    if (recipePosition.getVariableAmount() == null
        || recipePosition.getVariableAmount().isEmpty()) {
      amountString = NumUtil.trimAmount(amountRecipeUnit, maxDecimalPlacesAmount);
      holder.binding.variableAmount.setVisibility(View.GONE);
    } else {
      amountString = recipePosition.getVariableAmount();
      holder.binding.variableAmount.setVisibility(View.VISIBLE);
    }

    if (product != null) {
      holder.binding.ingredient.setText(context.getString(
          R.string.title_ingredient_with_amount,
          amountString,
          pluralUtil.getQuantityUnitPlural(quantityUnit, amountRecipeUnit),
          product.getName()
      ));
    } else {
      holder.binding.ingredient.setText(R.string.error_undefined);
    }

    // FULFILLMENT
    StockItem stockItem = stockItemHashMap.get(recipePosition.getProductId());
    if (recipePosition.isNotCheckStockFulfillment() || stockItemHashMap.isEmpty()) {
      holder.binding.fulfillment.setVisibility(View.GONE);
    } else {
      holder.binding.fulfillment.setVisibility(View.VISIBLE);

      double amountMissing = getAmountMissing(recipePosition, stockItem, amountStockUnit, amountRecipeUnit);
      double amountShoppingList = getAmountOnShoppingList(recipePosition, quantityUnitConversion);
      if (amountMissing == 0) {
        holder.binding.fulfilled.setText(R.string.msg_recipes_enough_in_stock);
        holder.binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_check_circle_outline,
            null
        ));
        holder.binding.imageFulfillment.setColorFilter(
            colorGreen.getAccent(),
            android.graphics.PorterDuff.Mode.SRC_IN
        );
        holder.binding.missing.setVisibility(View.GONE);
      } else {
        holder.binding.fulfilled.setText(R.string.msg_recipes_not_enough);
        holder.binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            amountShoppingList >= amountMissing ? R.drawable.ic_round_error_outline : R.drawable.ic_round_highlight_off,
            null
        ));
        holder.binding.imageFulfillment.setColorFilter(
            amountShoppingList >= amountMissing ? colorYellow.getAccent() : colorRed.getAccent(),
            android.graphics.PorterDuff.Mode.SRC_IN
        );
        holder.binding.missing.setText(
            context.getString(
                R.string.msg_recipes_ingredient_fulfillment_info_list,
                NumUtil.trimAmount(amountMissing, maxDecimalPlacesAmount),
                NumUtil.trimAmount(amountShoppingList, maxDecimalPlacesAmount)
            )
        );
        holder.binding.missing.setVisibility(View.VISIBLE);
      }
    }

    // NOTE
    if (recipePosition.getNote() == null || recipePosition.getNote().trim().isEmpty()) {
      holder.binding.note.setVisibility(View.GONE);
    } else {
      holder.binding.note.setText(recipePosition.getNote());
      holder.binding.note.setVisibility(View.VISIBLE);
    }

    if (recipePosition.isChecked()) {
      holder.binding.linearRecipePositionContainer.setAlpha(0.5f);
      holder.binding.ingredient.setPaintFlags(
          holder.binding.ingredient.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
    } else {
      holder.binding.linearRecipePositionContainer.setAlpha(1f);
      holder.binding.ingredient.setPaintFlags(
          holder.binding.ingredient.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG
      );
    }

    // CONTAINER
    holder.binding.linearRecipePositionContainer.setBackground(
        ViewUtil.getRippleBgListItemSurface(context)
    );
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
      List<QuantityUnit> newQuantityUnits,
      List<QuantityUnitConversion> newQuantityUnitConversions,
      HashMap<Integer, StockItem> newStockItemHashMap,
      List<ShoppingListItem> newShoppingListItems
  ) {

    RecipePositionAdapter.DiffCallback diffCallback = new RecipePositionAdapter.DiffCallback(
        this.recipe,
        recipe,
        this.recipePositions,
        newList,
        this.products,
        newProducts,
        this.quantityUnits,
        newQuantityUnits,
        this.quantityUnitConversions,
        newQuantityUnitConversions,
        this.stockItemHashMap,
        newStockItemHashMap,
        this.shoppingListItems,
        newShoppingListItems
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipe = recipe;
    this.recipePositions.clear();
    this.recipePositions.addAll(newList);
    this.products.clear();
    this.products.addAll(newProducts);
    this.quantityUnits.clear();
    this.quantityUnits.addAll(newQuantityUnits);
    this.quantityUnitConversions.clear();
    this.quantityUnitConversions.addAll(newQuantityUnitConversions);
    this.stockItemHashMap.clear();
    this.stockItemHashMap.putAll(newStockItemHashMap);
    this.shoppingListItems.clear();
    this.shoppingListItems.addAll(newShoppingListItems);
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
    List<QuantityUnitConversion> oldQuantityUnitConversions;
    List<QuantityUnitConversion> newQuantityUnitConversions;
    HashMap<Integer, StockItem> oldStockItemHashMap;
    HashMap<Integer, StockItem> newStockItemHashMap;
    List<ShoppingListItem> oldShoppingListItems;
    List<ShoppingListItem> newShoppingListItems;

    public DiffCallback(
        Recipe oldRecipe,
        Recipe newRecipe,
        List<RecipePosition> oldItems,
        List<RecipePosition> newItems,
        List<Product> oldProducts,
        List<Product> newProducts,
        List<QuantityUnit> oldQuantityUnits,
        List<QuantityUnit> newQuantityUnits,
        List<QuantityUnitConversion> oldQuantityUnitConversions,
        List<QuantityUnitConversion> newQuantityUnitConversions,
        HashMap<Integer, StockItem> oldStockItemHashMap,
        HashMap<Integer, StockItem> newStockItemHashMap,
        List<ShoppingListItem> oldShoppingListItems,
        List<ShoppingListItem> newShoppingListItems
    ) {
      this.oldRecipe = oldRecipe;
      this.newRecipe = newRecipe;
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldProducts = oldProducts;
      this.newProducts = newProducts;
      this.oldQuantityUnits = oldQuantityUnits;
      this.newQuantityUnits = newQuantityUnits;
      this.oldQuantityUnitConversions = oldQuantityUnitConversions;
      this.newQuantityUnitConversions = newQuantityUnitConversions;
      this.oldStockItemHashMap = oldStockItemHashMap;
      this.newStockItemHashMap = newStockItemHashMap;
      this.oldShoppingListItems = oldShoppingListItems;
      this.newShoppingListItems = newShoppingListItems;
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
      QuantityUnit newQuantityUnit = QuantityUnit.getFromId(
          newQuantityUnits, newItem.getQuantityUnitId()
      );
      QuantityUnit oldQuantityUnit = QuantityUnit.getFromId(
          oldQuantityUnits, oldItem.getQuantityUnitId()
      );
      QuantityUnitConversion newQuantityUnitConversion = newItemProduct != null
          ? QuantityUnitConversion.getFromTwoUnits(
              newQuantityUnitConversions,
              newItemProduct.getQuIdStockInt(),
              newItem.getQuantityUnitId(),
              newItemProduct.getId()
          ) : null;
      QuantityUnitConversion oldQuantityUnitConversion = oldItemProduct != null
          ? QuantityUnitConversion.getFromTwoUnits(
              oldQuantityUnitConversions,
              oldItemProduct.getQuIdStockInt(),
              oldItem.getQuantityUnitId(),
              oldItemProduct.getId()
          ) : null;
      StockItem newStockItem = newStockItemHashMap.get(newItem.getProductId());
      StockItem oldStockItem = oldStockItemHashMap.get(oldItem.getProductId());

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      if (newItemProduct == null || !newItemProduct.equals(oldItemProduct)) {
        return false;
      }

      if (newQuantityUnit == null || !newQuantityUnit.equals(oldQuantityUnit)) {
        return false;
      }

      if (newQuantityUnitConversion == null
          || !newQuantityUnitConversion.equals(oldQuantityUnitConversion)) {
        return false;
      }

      if (oldStockItem == null && newStockItem != null
          || newStockItem == null || !newStockItem.equals(oldStockItem)) {
        return false;
      }

      if (oldShoppingListItems == null && newShoppingListItems != null
          || oldShoppingListItems != null
          && oldShoppingListItems.size() != newShoppingListItems.size()) return false;

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
