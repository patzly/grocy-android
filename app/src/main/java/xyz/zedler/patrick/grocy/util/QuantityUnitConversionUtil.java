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

import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;

public class QuantityUnitConversionUtil {
  public static HashMap<QuantityUnit, Double> getUnitFactors(
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversionResolved> unitConversions,
      Product product,
      boolean useResolvedConversions
  ) {
    // useResolvedConversions is always the VersionUtil.isGrocyServerMin400() value because
    // starting with this version, transitive conversions are
    // calculated (see QuantityUnitConversionResolved class). For easier version compatibility
    // changes of this app in future versions, the QuantityUnitConversionsResolved table of this app
    // contains with earlier server versions just the simple conversions from the Grocy
    // server (not resolved). If 4.0.0 is the min. server version requirement, this behavior
    // can simply be removed and not all pages have to be edited.
    if (!useResolvedConversions) {
      return getUnitFactors(quantityUnitHashMap, unitConversions, product);
    }
    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    for (QuantityUnitConversion conversion : unitConversions) {
      if (conversion.getProductIdInt() != product.getId()) continue;
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || unitFactors.containsKey(unit)) continue;
      unitFactors.put(unit, conversion.getFactor());
    }
    return unitFactors;
  }

  private static HashMap<QuantityUnit, Double> getUnitFactors(
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversionResolved> unitConversions,
      Product product
  ) {
    QuantityUnit stockUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
    QuantityUnit purchaseUnit = quantityUnitHashMap.get(product.getQuIdPurchaseInt());

    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    if (stockUnit == null || purchaseUnit == null) {
      return unitFactors;
    }
    unitFactors.put(stockUnit, (double) 1);
    if (!unitFactors.containsKey(purchaseUnit)) {
      unitFactors.put(purchaseUnit, 1 / product.getQuFactorPurchaseToStockDouble());
    }
    for (QuantityUnitConversion conversion : unitConversions) {
      if (!NumUtil.isStringInt(conversion.getProductId())
          || product.getId() != conversion.getProductIdInt()) {
        continue;
      }
      // Only add product specific conversions
      // ("overriding" standard conversions which are added in the next step)
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || unitFactors.containsKey(unit)) {
        continue;
      }
      unitFactors.put(unit, conversion.getFactor());
    }
    for (QuantityUnitConversion conversion : unitConversions) {
      if (NumUtil.isStringInt(conversion.getProductId())
          || stockUnit.getId() != conversion.getFromQuId()) {
        continue;
      }
      // Only add standard unit conversions
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || unitFactors.containsKey(unit)) {
        continue;
      }
      unitFactors.put(unit, conversion.getFactor());
    }
    return unitFactors;
  }

  public static String getAmountStock(
      QuantityUnit stock,
      QuantityUnit current,
      String amountStr,
      HashMap<QuantityUnit, Double> quantityUnitsFactors,
      boolean onlyCheckSingleUnitInStock,
      int maxDecimalPlacesAmount
  ) {
    if (!NumUtil.isStringDouble(amountStr)
        || quantityUnitsFactors == null || onlyCheckSingleUnitInStock
    ) {
      return null;
    }
    if (stock != null && current != null) {
      double amount = NumUtil.toDouble(amountStr);
      Object currentFactor = quantityUnitsFactors.get(current);
      if (currentFactor == null) return null;
      return NumUtil.trimAmount(amount / (double) currentFactor, maxDecimalPlacesAmount);
    } else {
      return null;
    }
  }

  public static String getPriceStock(
      QuantityUnit current,
      String amountStr,
      String priceStr,
      HashMap<QuantityUnit, Double> quantityUnitsFactors,
      boolean isTareWeightEnabled,
      boolean isTotalPrice,
      int decimalPlacesPriceDisplay
  ) {
    if (!NumUtil.isStringDouble(priceStr) || !NumUtil.isStringDouble(amountStr)
        || current == null) {
      return null;
    }
    if (!NumUtil.isStringDouble(amountStr) || quantityUnitsFactors == null) {
      return null;
    }
    double amount = NumUtil.toDouble(amountStr);
    double price = NumUtil.toDouble(priceStr);
    Object currentFactor = quantityUnitsFactors.get(current);
    if (currentFactor == null) return null;

    double priceMultiplied = isTareWeightEnabled ? price : price * (double) currentFactor;
    if (isTotalPrice) {
      priceMultiplied /= amount;
    }
    return NumUtil.trimPrice(priceMultiplied, decimalPlacesPriceDisplay);
  }
}
