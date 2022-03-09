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

package xyz.zedler.patrick.grocy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;

public class ArrayUtil {

  public static HashMap<Integer, Product> getProductsHashMap(ArrayList<Product> products) {
    HashMap<Integer, Product> hashMap = new HashMap<>();
    for (Product p : products) {
      hashMap.put(p.getId(), p);
    }
    return hashMap;
  }

  public static HashMap<Integer, String> getProductNamesHashMap(ArrayList<Product> products) {
    if (products == null) {
      return null;
    }
    HashMap<Integer, String> productNamesHashMap = new HashMap<>();
    for (Product product : products) {
      productNamesHashMap.put(product.getId(), product.getName());
    }
    return productNamesHashMap;
  }

  public static ArrayList<Integer> getMissingProductsIds(ArrayList<MissingItem> missingItems) {
    ArrayList<Integer> missingProductIds = new ArrayList<>();
    for (MissingItem missingItem : missingItems) {
      missingProductIds.add(missingItem.getId());
    }
    return missingProductIds;
  }

  public static HashMap<Integer, Location> getLocationsHashMap(ArrayList<Location> locations) {
    HashMap<Integer, Location> hashMap = new HashMap<>();
    for (Location l : locations) {
      hashMap.put(l.getId(), l);
    }
    return hashMap;
  }

  public static HashMap<Integer, ProductGroup> getProductGroupsHashMap(
      List<ProductGroup> productGroups
  ) {
    HashMap<Integer, ProductGroup> hashMap = new HashMap<>();
    for (ProductGroup p : productGroups) {
      hashMap.put(p.getId(), p);
    }
    return hashMap;
  }

  public static HashMap<Integer, Store> getStoresHashMap(ArrayList<Store> stores) {
    HashMap<Integer, Store> hashMap = new HashMap<>();
    for (Store s : stores) {
      hashMap.put(s.getId(), s);
    }
    return hashMap;
  }

  public static HashMap<Integer, QuantityUnit> getQuantityUnitsHashMap(
      ArrayList<QuantityUnit> quantityUnits
  ) {
    HashMap<Integer, QuantityUnit> hashMap = new HashMap<>();
    for (QuantityUnit q : quantityUnits) {
      hashMap.put(q.getId(), q);
    }
    return hashMap;
  }

  public static HashMap<Integer, ArrayList<QuantityUnitConversion>> getUnitConversionsHashMap(
      ArrayList<QuantityUnitConversion> unitConversions
  ) {
    HashMap<Integer, ArrayList<QuantityUnitConversion>> hashMap = new HashMap<>();
    for (QuantityUnitConversion unitConversion : unitConversions) {
      ArrayList<QuantityUnitConversion> unitConversionArrayList
          = hashMap.get(unitConversion.getProductId());
      if (unitConversionArrayList == null) {
        unitConversionArrayList = new ArrayList<>();
        hashMap.put(unitConversion.getProductId(), unitConversionArrayList);
      }
      unitConversionArrayList.add(unitConversion);
    }
    return hashMap;
  }

  public static HashMap<Integer, ShoppingListItem> getShoppingListItemHashMap(
      ArrayList<ShoppingListItem> shoppingListItems
  ) {
    if (shoppingListItems == null) {
      return null;
    }
    HashMap<Integer, ShoppingListItem> hashMap = new HashMap<>();
    for (ShoppingListItem item : shoppingListItems) {
      hashMap.put(item.getId(), item);
    }
    return hashMap;
  }
}
