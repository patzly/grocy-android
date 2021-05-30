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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;

public class AmountUtil {

  public static Double getShoppingListItemAmount(
      ShoppingListItem item,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, ArrayList<QuantityUnitConversion>> unitConversionHashMap
  ) {
    if (!item.hasProduct()) {
      return null;
    }
    Product product = productHashMap.get(item.getProductIdInt());
    ArrayList<QuantityUnitConversion> unitConversions
        = unitConversionHashMap.get(item.getProductIdInt());
    if (product == null) {
      return null;
    }
    if (unitConversions == null) {
      unitConversions = new ArrayList<>();
    }

    QuantityUnit stock = quantityUnitHashMap.get(product.getQuIdStockInt());
    QuantityUnit purchase = quantityUnitHashMap.get(product.getQuIdPurchaseInt());
    if (stock == null || purchase == null) {
      return null;
    }
    HashMap<Integer, Double> unitFactors = new HashMap<>();
    ArrayList<Integer> quIdsInHashMap = new ArrayList<>();
    unitFactors.put(stock.getId(), (double) -1);
    quIdsInHashMap.add(stock.getId());
    if (!quIdsInHashMap.contains(purchase.getId())) {
      unitFactors.put(purchase.getId(), product.getQuFactorPurchaseToStockDouble());
    }
    for (QuantityUnitConversion conversion : unitConversions) {
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || quIdsInHashMap.contains(unit.getId())) {
        continue;
      }
      unitFactors.put(unit.getId(), conversion.getFactor());
    }
    if (!unitFactors.containsKey(item.getQuIdInt())) {
      return null;
    }
    Double factor = unitFactors.get(item.getQuIdInt());
    assert factor != null;
    if (factor != -1 && item.getQuIdInt() == product.getQuIdPurchaseInt()) {
      return item.getAmountDouble() / factor;
    } else if (factor != -1) {
      return item.getAmountDouble() * factor;
    } else {
      return null;
    }
  }
}
