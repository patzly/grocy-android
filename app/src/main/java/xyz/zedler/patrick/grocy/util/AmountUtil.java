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

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;

public class AmountUtil {

  public static Double getShoppingListItemAmount(
      ShoppingListItem item,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversionResolved> unitConversions
  ) {
    if (!item.hasProduct()) {
      return null;
    }
    Product product = productHashMap.get(item.getProductIdInt());
    if (product == null) return null;

    QuantityUnit stock = quantityUnitHashMap.get(product.getQuIdStockInt());
    QuantityUnit purchase = quantityUnitHashMap.get(product.getQuIdPurchaseInt());
    if (stock == null || purchase == null) {
      return null;
    }
    HashMap<QuantityUnit, Double> unitFactors = QuantityUnitConversionUtil
        .getUnitFactors(quantityUnitHashMap, unitConversions, product);
    Double factor = unitFactors.get(quantityUnitHashMap.get(item.getQuIdInt()));
    return factor != null ? item.getAmountDouble() * factor : item.getAmountDouble();
  }

  public static void addStockAmountNormalInfo(
      @NonNull Context context,
      @NonNull PluralUtil pluralUtil,
      @NonNull StringBuilder stringBuilder,
      @Nullable StockItem stockItem,
      @Nullable QuantityUnit quantityUnit,
      int maxDecimalPlacesAmount
  ) {
    if (stockItem == null) return;
    String unitStr = "";
    if (quantityUnit != null) {
      unitStr = pluralUtil.getQuantityUnitPlural(quantityUnit, stockItem.getAmountDouble());
    }
    stringBuilder.append(
        context.getString(
            R.string.subtitle_amount,
            NumUtil.trimAmount(stockItem.getAmountDouble(), maxDecimalPlacesAmount),
            unitStr
        )
    );
    if (stockItem.getAmountOpenedDouble() > 0) {
      stringBuilder.append(" ");
      stringBuilder.append(
          context.getString(
              R.string.subtitle_amount_opened,
              NumUtil.trimAmount(stockItem.getAmountOpenedDouble(), maxDecimalPlacesAmount)
          )
      );
    }
  }

  public static void addStockEntryAmountNormalInfo(
      @NonNull Context context,
      @NonNull PluralUtil pluralUtil,
      @NonNull StringBuilder stringBuilder,
      @Nullable StockEntry stockEntry,
      @Nullable QuantityUnit quantityUnit,
      int maxDecimalPlacesAmount
  ) {
    if (stockEntry == null) return;
    String unitStr = "";
    if (quantityUnit != null) {
      unitStr = pluralUtil.getQuantityUnitPlural(quantityUnit, stockEntry.getAmount());
    }
    stringBuilder.append(
        context.getString(
            R.string.subtitle_amount,
            NumUtil.trimAmount(stockEntry.getAmount(), maxDecimalPlacesAmount),
            unitStr
        )
    );
    if (stockEntry.getOpen() == 1) {
      stringBuilder.append(" ");
      stringBuilder.append(context.getString(R.string.subtitle_opened));
    }
  }

  public static void addStockAmountAggregatedInfo(
      @NonNull Context context,
      @NonNull PluralUtil pluralUtil,
      @NonNull StringBuilder stringBuilder,
      @Nullable StockItem stockItem,
      @Nullable QuantityUnit quantityUnit,
      int maxDecimalPlacesAmount,
      boolean addSpace
  ) {
    if (stockItem == null) return;
    if (stockItem.getIsAggregatedAmountInt() == 0) return;
    String unitAggregated = "";
    if (quantityUnit != null) {
      unitAggregated = pluralUtil.getQuantityUnitPlural(
          quantityUnit,
          stockItem.getAmountAggregatedDouble()
      );
    }
    if (addSpace) stringBuilder.append("  ");
    stringBuilder.append("∑ ");
    stringBuilder.append(
        context.getString(
            R.string.subtitle_amount,
            NumUtil.trimAmount(stockItem.getAmountAggregatedDouble(), maxDecimalPlacesAmount),
            unitAggregated
        )
    );
    if (stockItem.getAmountOpenedAggregatedDouble() > 0) {
      stringBuilder.append(" ");
      stringBuilder.append(
          context.getString(
              R.string.subtitle_amount_opened,
              NumUtil.trimAmount(stockItem.getAmountOpenedAggregatedDouble(), maxDecimalPlacesAmount)
          )
      );
    }
  }

  public static String getStockAmountInfo(
      @NonNull Context context,
      @NonNull PluralUtil pluralUtil,
      @Nullable StockItem stockItem,
      @Nullable QuantityUnit quantityUnit,
      int maxDecimalPlacesAmount
  ) {
    if (stockItem == null) return null;
    StringBuilder stringBuilder = new StringBuilder();
    boolean addSpaceBetween = false;
    if (!stockItem.getProduct().getNoOwnStockBoolean()) {
      addStockAmountNormalInfo(context, pluralUtil, stringBuilder, stockItem, quantityUnit, maxDecimalPlacesAmount);
      addSpaceBetween = true;
    }
    addStockAmountAggregatedInfo(context, pluralUtil, stringBuilder, stockItem, quantityUnit, maxDecimalPlacesAmount, addSpaceBetween);
    return stringBuilder.toString();
  }

  public static String getStockAmountInfo(
      @NonNull Context context,
      @NonNull PluralUtil pluralUtil,
      @Nullable ProductDetails productDetails,
      int maxDecimalPlacesAmount
  ) {
    if (productDetails == null) return null;
    return getStockAmountInfo(
        context,
        pluralUtil,
        new StockItem(productDetails),
        productDetails.getQuantityUnitStock(),
        maxDecimalPlacesAmount
    );
  }

  public static String getStockEntryAmountInfo(
      @NonNull Context context,
      @NonNull PluralUtil pluralUtil,
      @Nullable StockEntry stockEntry,
      @Nullable QuantityUnit quantityUnit,
      int maxDecimalPlacesAmount
  ) {
    if (stockEntry == null) return null;
    StringBuilder stringBuilder = new StringBuilder();
    addStockEntryAmountNormalInfo(context, pluralUtil, stringBuilder, stockEntry, quantityUnit, maxDecimalPlacesAmount);
    return stringBuilder.toString();
  }
}
