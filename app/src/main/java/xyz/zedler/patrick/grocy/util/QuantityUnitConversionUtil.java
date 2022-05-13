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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;

public class QuantityUnitConversionUtil {

  public static HashMap<QuantityUnit, Double> getUnitFactors(
      Context context,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversion> unitConversions,
      Product product
  ) {
    QuantityUnit stock = quantityUnitHashMap.get(product.getQuIdStockInt());
    QuantityUnit purchase = quantityUnitHashMap.get(product.getQuIdPurchaseInt());

    if (stock == null || purchase == null) {
      throw new IllegalArgumentException(context.getString(R.string.error_loading_qus));
    }

    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    unitFactors.put(stock, (double) -1);
    if (!unitFactors.containsKey(purchase)) {
      unitFactors.put(purchase, product.getQuFactorPurchaseToStockDouble());
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
          || stock.getId() != conversion.getFromQuId()) {
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

}
