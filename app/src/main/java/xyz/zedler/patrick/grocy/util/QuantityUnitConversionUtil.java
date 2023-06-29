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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionPath;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;

public class QuantityUnitConversionUtil {

  // https://github.com/grocy/grocy/blob/master/migrations/0208.sql
  public static List<QuantityUnitConversionResolved> calculateConversions(
      List<QuantityUnitConversion> conversions,
      List<Product> products
  ) {
    List<QuantityUnitConversionPath> paths = new ArrayList<>();

    // First, determine conversions that are a single step.
    List<QuantityUnitConversion> conversionFactors;

    // Priority 1: Product specific QU overrides
    // Note that the quantity_unit_conversions table already contains both conversion directions for every conversion.
    conversionFactors = conversions.stream()
        .filter(c -> c.getProductId() != null)
        .collect(Collectors.toList());



    // Try to add stock<->purchase conversions (exist already on server version 4.0.0)
    for (Product product : products) {
      if (product.getQuIdStockInt() == product.getQuIdPurchaseInt()) continue;
      QuantityUnitConversion conversion = new QuantityUnitConversion();
      conversion.setId(-1);
      conversion.setProductId(String.valueOf(product.getId()));
      conversion.setFromQuId(product.getQuIdStockInt());
      conversion.setToQuId(product.getQuIdPurchaseInt());
      conversion.setFactor(product.getQuFactorPurchaseToStockDouble());

      boolean exists = conversionFactors.stream().anyMatch(existingConversion ->
          existingConversion.getProductId().equals(conversion.getProductId()) &&
              existingConversion.getFromQuId() == conversion.getFromQuId() &&
              existingConversion.getToQuId() == conversion.getToQuId()
      );

      if (!exists) {
        conversionFactors.add(conversion);
      }
    }

    // Priority 2: QU conversions with a factor of 1.0 from the stock unit to the stock unit
    for (Product product : products) {
      QuantityUnitConversion conversion = new QuantityUnitConversion();
      conversion.setId(-1);
      conversion.setProductId(String.valueOf(product.getId()));
      conversion.setFromQuId(product.getQuIdStockInt());
      conversion.setToQuId(product.getQuIdStockInt());
      conversion.setFactor(1.0);
      conversionFactors.add(conversion);
    }

    // Default QU conversions are handled later, as we can't determine yet, for which products they are applicable.
    List<QuantityUnitConversion> defaultConversions = conversions.stream()
        .filter(c -> c.getProductId() == null)
        .collect(Collectors.toList());

    // Now build the closure of possible conversions using a recursive function
    // As a base case, select the conversions that refer to a concrete product
    for (QuantityUnitConversion conversion : conversionFactors) {
      // create base path with two units from one conversion
      QuantityUnitConversionPath newPath = new QuantityUnitConversionPath(conversion);
      // call recursive function which finds all possible paths for this base path
      buildConversionChains(conversionFactors, defaultConversions, paths, newPath);
    }

    List<QuantityUnitConversionPath> pathsFiltered = paths.stream().distinct()
        .collect(Collectors.toList());

    // group conversions by product_id, from_qu_id und to_qu_id
    Map<List<Integer>, List<QuantityUnitConversionPath>> grouped = pathsFiltered.stream()
        .collect(Collectors.groupingBy((QuantityUnitConversionPath c) -> Arrays
            .asList(c.getProductIdInt(), c.getFromQuId(), c.getToQuId())));

    // create new list for storing sorted conversions
    List<QuantityUnitConversionPath> pathsSorted = new ArrayList<>();

    for (List<QuantityUnitConversionPath> group : grouped.values()) {
      Collections.sort(group, Comparator.comparing(QuantityUnitConversionPath::getDepth));
      pathsSorted.add(group.get(0));
    }

    // sort conversions by product_id, from_qu_id und to_qu_id
    Collections.sort(pathsSorted, Comparator.comparing(QuantityUnitConversionPath::getProductIdInt)
        .thenComparing(QuantityUnitConversionPath::getFromQuId)
        .thenComparing(QuantityUnitConversionPath::getToQuId));

    List<QuantityUnitConversionResolved> result = new ArrayList<>();
    int conversionId = 0;
    for (QuantityUnitConversionPath path : pathsSorted) {
      result.add(path.toConversion(conversionId));
      conversionId++;
    }
    return result;
  }

  private static void buildConversionChains(
      List<QuantityUnitConversion> conversionFactors,
      List<QuantityUnitConversion> defaultConversions,
      List<QuantityUnitConversionPath> paths,
      QuantityUnitConversionPath currentPath
  ) {
    if (currentPath.getDepth() >= 1) {
      paths.add(currentPath);
    }

    // Recursive case 1: Add a product-associated conversion to the chain
    for (QuantityUnitConversion conversion : conversionFactors) {
      if (conversion.getProductId().equals(currentPath.getProductId())
          && conversion.getFromQuId() == currentPath.getToQuId()
          && !currentPath.getPath().contains("/" + conversion.getToQuId() + "/")
      ) {
        QuantityUnitConversionPath newPath = new QuantityUnitConversionPath(
            currentPath.getDepth() + 1,
            currentPath.getProductId(),
            currentPath.getFromQuId(),
            conversion.getToQuId(),
            currentPath.getFactor() * conversion.getFactor(),
            currentPath.getPath() + conversion.getToQuId() + "/"
        );
        buildConversionChains(conversionFactors, defaultConversions, paths, newPath);
      }
    }

    // Recursive cases 2 and 3: Add a default unit conversion to the start/end of the conversion chain
    for (QuantityUnitConversion conversion : defaultConversions) {
      boolean isDefaultConversionNotOverridden = conversionFactors.stream().noneMatch(c ->
          c.getProductId().equals(currentPath.getProductId())
              && c.getFromQuId() == conversion.getFromQuId()
              && c.getToQuId() == conversion.getToQuId()
      );
      boolean isFromQuNotInPath = !currentPath.getPath().contains("/" + conversion.getFromQuId() + "/");
      boolean isToQuNotInPath = !currentPath.getPath().contains("/" + conversion.getToQuId() + "/");


      if (isDefaultConversionNotOverridden) {
        // Case 2: Add to the start
        if (conversion.getToQuId() == currentPath.getFromQuId() && isFromQuNotInPath) {
          QuantityUnitConversionPath newPath = new QuantityUnitConversionPath(
              currentPath.getDepth() + 1,
              currentPath.getProductId(),
              conversion.getFromQuId(),
              currentPath.getToQuId(),
              conversion.getFactor() * currentPath.getFactor(),
              "/" + conversion.getFromQuId() + currentPath.getPath()
          );
          buildConversionChains(conversionFactors, defaultConversions, paths, newPath);
        }

        // Case 3: Add to the end
        if (conversion.getFromQuId() == currentPath.getToQuId() && isToQuNotInPath) {
          QuantityUnitConversionPath newPath = new QuantityUnitConversionPath(
              currentPath.getDepth() + 1,
              currentPath.getProductId(),
              currentPath.getFromQuId(),
              conversion.getToQuId(),
              currentPath.getFactor() * conversion.getFactor(),
              currentPath.getPath() + conversion.getToQuId() + "/"
          );
          buildConversionChains(conversionFactors, defaultConversions, paths, newPath);
        }
      }
    }

    // Recursive case 4: Add the default unit conversions that are reachable by a given product
    // We cannot start with them directly, as we only want to add default conversions,
    // where at least one of the units is 'reachable' from the product's stock quantity unit
    // (and thus the conversion is sensible). Thus we add these cases here.
    for (QuantityUnitConversion conversion : defaultConversions) {
      boolean isDefaultConversionNotOverridden = conversionFactors.stream().noneMatch(c ->
          c.getProductId().equals(currentPath.getProductId())
              && c.getFromQuId() == conversion.getFromQuId()
              && c.getToQuId() == conversion.getToQuId()
      );
      boolean isConversionPartOfPath = currentPath.getPath().contains("/" + conversion.getFromQuId() + "/")
          && currentPath.getPath().contains("/" + conversion.getToQuId() + "/");

      String newPathString = "/" + conversion.getFromQuId() + "/" + conversion.getToQuId() + "/";
      boolean isPathAlreadyAdded = paths.stream().anyMatch(
          p -> p.getProductIdInt() == currentPath.getProductIdInt()
              && p.getFromQuId() == conversion.getFromQuId()
              && p.getToQuId() == conversion.getToQuId()
      );

      if (isDefaultConversionNotOverridden && isConversionPartOfPath && !isPathAlreadyAdded) {
        QuantityUnitConversionPath newPath = new QuantityUnitConversionPath(
            1,
            currentPath.getProductId(),
            conversion.getFromQuId(),
            conversion.getToQuId(),
            conversion.getFactor(),
            newPathString
        );
        buildConversionChains(conversionFactors, defaultConversions, paths, newPath);
      }
    }
  }

  public static HashMap<QuantityUnit, Double> getUnitFactors(
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversionResolved> unitConversions,
      Product product
  ) {
    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    for (QuantityUnitConversion conversion : unitConversions) {
      if (conversion.getProductIdInt() != product.getId()) continue;
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || unitFactors.containsKey(unit)) continue;
      unitFactors.put(unit, conversion.getFactor());
    }
    return unitFactors;
  }

  public static HashMap<QuantityUnit, Double> getUnitFactors(
      Context context,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversion> unitConversions,
      Product product,
      boolean relativeToStockUnit
  ) {
    QuantityUnit relativeToUnit = relativeToStockUnit
        ? quantityUnitHashMap.get(product.getQuIdStockInt())
        : quantityUnitHashMap.get(product.getQuIdPurchaseInt());
    QuantityUnit stockUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
    QuantityUnit purchaseUnit = quantityUnitHashMap.get(product.getQuIdPurchaseInt());

    if (relativeToUnit == null || stockUnit == null || purchaseUnit == null) {
      throw new IllegalArgumentException(context.getString(R.string.error_loading_qus));
    }

    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    unitFactors.put(relativeToUnit, (double) -1);
    if (relativeToStockUnit && !unitFactors.containsKey(purchaseUnit)) {
      unitFactors.put(purchaseUnit, product.getQuFactorPurchaseToStockDouble());
    } else if (!relativeToStockUnit && !unitFactors.containsKey(stockUnit)) {
      unitFactors.put(stockUnit, product.getQuFactorPurchaseToStockDouble());
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
          || relativeToUnit.getId() != conversion.getFromQuId()) {
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

  public static double getAmountRelativeToUnit(
      HashMap<QuantityUnit, Double> unitFactors,
      Product product,
      QuantityUnit quantityUnit,
      double inputAmount
  ) {
    if (quantityUnit == null || !unitFactors.containsKey(quantityUnit)) {
      return inputAmount;
    }
    Double factor = unitFactors.get(quantityUnit);
    assert factor != null;
    if (factor != -1 && quantityUnit.getId() == product.getQuIdPurchaseInt()) {
      return inputAmount / factor;
    } else if (factor != -1) {
      return inputAmount * factor;
    }
    return inputAmount;
  }
}
