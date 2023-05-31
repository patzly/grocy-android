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

import android.content.pm.ShortcutInfo;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;

public class SortUtil {

  // Source: https://medium.com/fme-developer-stories/how-to-sort-umlaute-in-java-correctly-13f3262f15a1
  private static final String EXT_GERMAN_RULES = "&< ' ' < '.'" +
      "<0<1<2<3<4<5<6<7<8<9<a,A<b,B<c,C<d,D<ð,Ð<e,E<f,F<g,G<h,H<i,I<j" +
      ",J<k,K<l,L<m,M<n,N<o,O<p,P<q,Q<r,R<s, S & SS,ß<t,T& TH, Þ &TH," +
      "þ <u,U<v,V<w,W<x,X<y,Y<z,Z&AE,Æ&AE,æ&OE,Œ&OE,œ";

  private static void compareStockItemsWithExtGerman(List<StockItem> items, boolean asc) {
    Collections.sort(items, (item1, item2) -> Collator.getInstance(Locale.GERMAN).compare(
        (asc ? item1 : item2).getProduct().getName().toLowerCase(),
        (asc ? item2 : item1).getProduct().getName().toLowerCase()
    ));
  }

  public static void sortStockItemsByName(List<StockItem> stockItems, boolean ascending) {
    if (stockItems == null) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(stockItems, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getProduct().getName().toLowerCase(),
        (ascending ? item2 : item1).getProduct().getName().toLowerCase())
    );
  }

  public static void sortStockItemsByBBD(List<StockItem> stockItems, boolean ascending) {
    if (stockItems == null) {
      return;
    }
    Collections.sort(
        stockItems,
        (item1, item2) -> {
          String bbd1 = (ascending ? item1 : item2).getBestBeforeDate();
          String bbd2 = (ascending ? item2 : item1).getBestBeforeDate();
          if (bbd1 == null && bbd2 == null) {
            return 0;
          } else if (bbd1 == null) {
            return -1; // or 1 when items without BBD should be last
          } else if (bbd2 == null) {
            return 1; // or -1 when items without BBD should be last
          }
          return DateUtil.getDate(bbd1).compareTo(DateUtil.getDate(bbd2));
        }
    );
  }

  public static void sortStockEntriesByDueDate(List<StockEntry> stockEntries, boolean ascending) {
    if (stockEntries == null) {
      return;
    }
    Collections.sort(
        stockEntries,
        (item1, item2) -> {
          String bbd1 = (ascending ? item1 : item2).getBestBeforeDate();
          String bbd2 = (ascending ? item2 : item1).getBestBeforeDate();
          if (bbd1 == null && bbd2 == null) {
            return 0;
          } else if (bbd1 == null) {
            return -1; // or 1 when items without BBD should be last
          } else if (bbd2 == null) {
            return 1; // or -1 when items without BBD should be last
          }
          return DateUtil.getDate(bbd1).compareTo(DateUtil.getDate(bbd2));
        }
    );
  }

  public static void sortStockEntriesByName(
      List<StockEntry> stockEntries, HashMap<Integer, Product> productHashMap, boolean ascending
  ) {
    if (stockEntries == null || productHashMap == null) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(stockEntries, (entry1, entry2) -> {
      int productId1 = (ascending ? entry1 : entry2).getProductId();
      int productId2 = (ascending ? entry2 : entry1).getProductId();
      Product product1 = productHashMap.get(productId1);
      Product product2 = productHashMap.get(productId2);
      if (product1 == null && product2 == null) {
        return 0;
      } else if (product1 == null) {
        return -1;
      } else if (product2 == null) {
        return 1;
      }
      return Collator.getInstance(locale).compare(
          product1.getName().toLowerCase(),
          product2.getName().toLowerCase()
      );
    });
  }

  public static void sortProductsByName(List<Product> products, boolean ascending) {
    if (products == null) {
      return;
    }
    Collections.sort(
        products,
        (item1, item2) -> (ascending ? item1 : item2).getName().toLowerCase().compareTo(
            (ascending ? item2 : item1).getName().toLowerCase()
        )
    );
  }

  public static void sortStockLocationItemsByName(ArrayList<StockLocation> stockLocations) {
    if (stockLocations == null) {
      return;
    }
    Collections.sort(
        stockLocations, Comparator.comparing(item -> item.getLocationName().toLowerCase())
    );
  }

  public static void sortTasksByName(List<Task> tasks, boolean ascending) {
    if (tasks == null || tasks.isEmpty()) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(tasks, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getName().toLowerCase(),
        (ascending ? item2 : item1).getName().toLowerCase()
    ));
  }

  public static void sortTasksByDueDate(List<Task> tasks, boolean ascending) {
    if (tasks == null || tasks.isEmpty()) {
      return;
    }
    Collections.sort(
        tasks,
        (item1, item2) -> {
          String bbd1 = (ascending ? item1 : item2).getDueDate();
          String bbd2 = (ascending ? item2 : item1).getDueDate();
          if (bbd1 == null && bbd2 == null
              || bbd1 != null && bbd1.isEmpty() && bbd2 != null && bbd2.isEmpty()) {
            return 0;
          } else if (bbd1 == null || bbd1.isEmpty()) {
            return -1; // or 1 when items without BBD should be last
          } else if (bbd2 == null || bbd2.isEmpty()) {
            return 1; // or -1 when items without BBD should be last
          }
          return DateUtil.getDate(bbd1).compareTo(DateUtil.getDate(bbd2));
        }
    );
  }

  public static void sortTaskCategoriesByName(
      ArrayList<TaskCategory> taskCategories, boolean ascending
  ) {
    if (taskCategories == null || taskCategories.isEmpty()) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(taskCategories, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getName().toLowerCase(),
        (ascending ? item2 : item1).getName().toLowerCase()
    ));
  }

  public static void sortChoreEntriesByNextExecution(
      List<ChoreEntry> choreEntries, boolean ascending
  ) {
    if (choreEntries == null || choreEntries.isEmpty()) {
      return;
    }
    Collections.sort(
        choreEntries,
        (item1, item2) -> {
          String time1 = (ascending ? item1 : item2).getNextEstimatedExecutionTime();
          String time2 = (ascending ? item2 : item1).getNextEstimatedExecutionTime();
          if (time1 == null && time2 == null
              || time1 != null && time1.isEmpty() && time2 != null && time2.isEmpty()) {
            return 0;
          } else if (time1 == null || time1.isEmpty()) {
            return -1; // or 1 when items without BBD should be last
          } else if (time2 == null || time2.isEmpty()) {
            return 1; // or -1 when items without BBD should be last
          }
          Date date1 = DateUtil.getDate(time1);
          Date date2 = DateUtil.getDate(time2);
          if (date1 == null || date2 == null) {
            return 0;
          }
          return DateUtil.getDate(time1).compareTo(DateUtil.getDate(time2));
        }
    );
  }

  public static void sortChoreEntriesByName(ArrayList<ChoreEntry> choreEntries, boolean ascending) {
    if (choreEntries == null || choreEntries.isEmpty()) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(choreEntries, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getChoreName().toLowerCase(),
        (ascending ? item2 : item1).getChoreName().toLowerCase()
    ));
  }

  public static void sortUsersByName(ArrayList<User> users, boolean ascending) {
    if (users == null || users.isEmpty()) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(users, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getDisplayName().toLowerCase(),
        (ascending ? item2 : item1).getDisplayName().toLowerCase()
    ));
  }

  public static void sortStringsByName(List<String> strings, boolean ascending) {
    if (strings == null || strings.isEmpty()) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(strings, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).toLowerCase(),
        (ascending ? item2 : item1).toLowerCase()
    ));
  }

  public static void sortStringsByValue(List<String> strings) {
    if (strings == null || strings.isEmpty()) {
      return;
    }
    Collections.sort(strings, (item1, item2) -> {
      if (!NumUtil.isStringDouble(item1) && !NumUtil.isStringDouble(item2)) {
        return 0;
      } else if (NumUtil.isStringDouble(item1) && !NumUtil.isStringDouble(item2)) {
        return 1;
      } else if (!NumUtil.isStringDouble(item1) && NumUtil.isStringDouble(item2)) {
        return -1;
      } else {
        double item1Double = NumUtil.toDouble(item1);
        double item2Double = NumUtil.toDouble(item2);
        return Double.compare(item1Double, item2Double);
      }
    });
  }

  public static void sortLocationsByName(List<Location> locations, boolean ascending) {
    if (locations == null) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(locations, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getName().toLowerCase(),
        (ascending ? item2 : item1).getName().toLowerCase()
    ));
  }

  public static void sortStoresByName(List<Store> stores, boolean ascending) {
    if (stores == null) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(stores, (item1, item2) -> Collator.getInstance(locale).compare(
            (ascending ? item1 : item2).getName().toLowerCase(),
            (ascending ? item2 : item1).getName().toLowerCase()));
  }

  public static void sortProductGroupsByName(List<ProductGroup> productGroups, boolean ascending) {
    if (productGroups == null || productGroups.isEmpty()) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(productGroups, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getName().toLowerCase(),
        (ascending ? item2 : item1).getName().toLowerCase()
    ));
  }

  public static void sortQuantityUnitsByName(
      ArrayList<QuantityUnit> quantityUnits, boolean ascending
  ) {
    if (quantityUnits == null) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(quantityUnits, (item1, item2) -> Collator.getInstance(locale).compare(
            (ascending ? item1 : item2).getName().toLowerCase(),
            (ascending ? item2 : item1).getName().toLowerCase()
    ));
  }

  public static void sortShoppingListItemsByName(
      List<ShoppingListItem> shoppingListItems,
      HashMap<Integer, String> productNamesHashMap,
      boolean ascending
  ) {
    if (shoppingListItems == null) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    ArrayList<ShoppingListItem> itemsWithoutProduct = new ArrayList<>();
    for (ShoppingListItem shoppingListItem : shoppingListItems) {
      if (!shoppingListItem.hasProduct()) {
        itemsWithoutProduct.add(shoppingListItem);
      }
    }
    Collections.sort(
        itemsWithoutProduct,
        (item1, item2) -> {
          String noteA = (ascending ? item1 : item2).getNote();
          String noteB = (ascending ? item2 : item1).getNote();
          if (noteA != null && noteB != null) {
            return Collator.getInstance(locale).compare(noteA, noteB);
          } else if (noteA == null && noteB != null) {
            return -1;
          } else if (noteA != null) {
            return 1;
          } else {
            return 0;
          }
        }
    );
    shoppingListItems.removeAll(itemsWithoutProduct);
    Collections.sort(
        shoppingListItems,
        (item1, item2) -> {
          String nameA = productNamesHashMap.get((ascending ? item1 : item2)
              .getProductIdInt());
          String nameB = productNamesHashMap.get((ascending ? item2 : item1)
              .getProductIdInt());
          if (nameA != null && nameB != null) {
            return Collator.getInstance(locale).compare(nameA, nameB);
          } else if (nameA == null && nameB != null) {
            return -1;
          } else if (nameA != null) {
            return 1;
          } else {
            return 0;
          }
        }
    );
    shoppingListItems.addAll(itemsWithoutProduct);
  }

  public static void sortUsersByName(List<User> users, boolean ascending) {
    if (users == null || users.isEmpty()) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(users, (item1, item2) -> Collator.getInstance(locale).compare(
        (ascending ? item1 : item2).getUserName().toLowerCase(),
        (ascending ? item2 : item1).getUserName().toLowerCase()
    ));
  }

  public static void sortLanguagesByName(List<Language> languages) {
    if (languages == null) {
      return;
    }
    Collections.sort(languages, Comparator.comparing(item -> item.getName().toLowerCase()));
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  public static List<ShortcutInfo> sortShortcutsById(
      List<ShortcutInfo> shortcutInfos,
      List<String> shortcutIdsSorted
  ) {
    HashMap<String, ShortcutInfo> shortcutInfoHashMap = new HashMap<>();
    for (ShortcutInfo shortcutInfo : shortcutInfos) {
      shortcutInfoHashMap.put(shortcutInfo.getId(), shortcutInfo);
    }
    List<ShortcutInfo> sorted = new ArrayList<>();
    for (String id : shortcutIdsSorted) {
      ShortcutInfo info = shortcutInfoHashMap.get(id);
      if (info != null) {
        sorted.add(info);
      }
    }
    return sorted;
  }

  public static void sortRecipesByName(List<Recipe> recipes, boolean ascending) {
    if (recipes == null) {
      return;
    }
    Locale locale = LocaleUtil.getLocale();
    Collections.sort(recipes, (item1, item2) -> Collator.getInstance(locale).compare(
            (ascending ? item1 : item2).getName().toLowerCase(),
            (ascending ? item2 : item1).getName().toLowerCase()));
  }

  public static void sortRecipesByCalories(List<Recipe> recipes, List<RecipeFulfillment> recipeFulfillments, boolean ascending) {
    if (recipes == null || recipeFulfillments == null) {
      return;
    }
    Collections.sort(recipes, (recipe1, recipe2) -> {
      RecipeFulfillment recipeFulfillment1 = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe1.getId());
      RecipeFulfillment recipeFulfillment2 = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe2.getId());

      double recipe1Calories = recipeFulfillment1 != null ? recipeFulfillment1.getCalories() : 0;
      double recipe2Calories = recipeFulfillment2 != null ? recipeFulfillment2.getCalories() : 0;

      return (int)(ascending ? recipe1Calories : recipe2Calories) - (int)(ascending ? recipe2Calories : recipe1Calories);
    });
  }

  public static void sortRecipesByDueScore(List<Recipe> recipes, List<RecipeFulfillment> recipeFulfillments, boolean ascending) {
    if (recipes == null || recipeFulfillments == null) {
      return;
    }
    Collections.sort(recipes, (recipe1, recipe2) -> {
      RecipeFulfillment recipeFulfillment1 = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe1.getId());
      RecipeFulfillment recipeFulfillment2 = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe2.getId());

      int recipe1DueScore = recipeFulfillment1 != null ? recipeFulfillment1.getDueScore() : 0;
      int recipe2DueScore = recipeFulfillment2 != null ? recipeFulfillment2.getDueScore() : 0;

      return (ascending ? recipe1DueScore : recipe2DueScore) - (ascending ? recipe2DueScore : recipe1DueScore);
    });
  }
}
