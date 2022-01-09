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

package xyz.zedler.patrick.grocy.helper;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class ShoppingListHelper {

  public static ArrayList<GroupedListItem> groupItemsShoppingMode(
      Context context,
      ArrayList<ShoppingListItem> shoppingListItems,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, String> productNamesHashMap,
      ArrayList<ProductGroup> productGroups,
      ArrayList<ShoppingList> shoppingLists,
      int selectedShoppingListId,
      boolean showDoneItems
  ) {
    HashMap<String, ProductGroup> productGroupHashMap = new HashMap<>();
    for (ProductGroup p : productGroups) {
      productGroupHashMap.put(String.valueOf(p.getId()), p);
    }
    HashMap<ProductGroup, Collection<ShoppingListItem>> sortedShoppingListItems = new HashMap<>();
    ArrayList<ShoppingListItem> doneItems = new ArrayList<>();
    ProductGroup ungrouped = new ProductGroup(
        -1,
        context.getString(R.string.property_ungrouped)
    );
    // sort displayedItems by productGroup
    for (ShoppingListItem shoppingListItem : shoppingListItems) {
      if (shoppingListItem.getDoneInt() == 1) {
        doneItems.add(shoppingListItem);
        continue;
      }
      String groupId = null;
      ProductGroup productGroup = null;
      Product product = null;
      if (shoppingListItem.hasProduct()) {
        product = productHashMap.get(shoppingListItem.getProductIdInt());
      }
      if (product != null) {
        groupId = product.getProductGroupId();
      }
      if (groupId != null && groupId.isEmpty()) {
        groupId = null;
      }
      if (groupId != null) {
        productGroup = productGroupHashMap.get(groupId);
      }
      if (groupId == null || productGroup == null) {
        productGroup = ungrouped;
      }
      Collection<ShoppingListItem> items = sortedShoppingListItems.get(productGroup);
      if (items == null) {
        items = new ArrayList<>();
        sortedShoppingListItems.put(productGroup, items);
      }
      items.add(shoppingListItem);
    }
    // sort product groups
    ArrayList<ProductGroup> sortedProductGroups = new ArrayList<>(sortedShoppingListItems.keySet());
    SortUtil.sortProductGroupsByName(context, sortedProductGroups, true);
    if (sortedProductGroups.contains(ungrouped)) {
      sortedProductGroups.remove(ungrouped);
      sortedProductGroups.add(ungrouped);
    }
    // create list with groups (headers) and entries
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    for (ProductGroup productGroup : sortedProductGroups) {
      ProductGroup clonedProductGroup = productGroup
          .getClone(); // clone is necessary because else adapter contains
      groupedListItems.add(
          clonedProductGroup);                  // same productGroup objects and could not calculate diff properly
      clonedProductGroup.setDisplayDivider(groupedListItems.get(0) != clonedProductGroup);
      Collection<ShoppingListItem> items = sortedShoppingListItems.get(productGroup);
      assert items != null;
      ArrayList<ShoppingListItem> itemsOneGroup = new ArrayList<>(items);
      SortUtil.sortShoppingListItemsByName(context, itemsOneGroup, productNamesHashMap, true);
      groupedListItems.addAll(itemsOneGroup);
    }
    // add bottom notes if they are not empty
    HashMap<Integer, ShoppingList> shoppingListHashMap = new HashMap<>();
    for (ShoppingList s : shoppingLists) {
      shoppingListHashMap.put(s.getId(), s);
    }
    ShoppingList shoppingList = shoppingListHashMap.get(selectedShoppingListId);
    Spanned notes = null;
    if (shoppingList != null && shoppingList.getNotes() != null) {
      Spanned spanned = Html.fromHtml(shoppingList.getNotes().trim());
      notes = (Spanned) TextUtil.trimCharSequence(spanned);
    }
    if (notes != null && notes.toString().trim().isEmpty()) {
      notes = null;
    }
    if (shoppingList != null && notes != null) {
      ProductGroup p = new ProductGroup(-1, context.getString(R.string.property_notes));
      groupedListItems.add(p);
      groupedListItems.add(new ShoppingListBottomNotes(notes));
    }
    if (!doneItems.isEmpty() && showDoneItems) {
      ProductGroup p = new ProductGroup(-2, context.getString(R.string.subtitle_done));
      groupedListItems.add(p);
      groupedListItems.addAll(doneItems);
    }
    return groupedListItems;
  }

  public static void changeAppBarTitle(
      TextView textTitle,
      ActionButton buttonLists,
      ShoppingList shoppingList
  ) {
    // change app bar title to shopping list name
    if (shoppingList == null) {
      return;
    }
    if (textTitle.getText().toString().equals(shoppingList.getName())) {
      return;
    }
    textTitle.animate().alpha(0).withEndAction(() -> {
      textTitle.setText(shoppingList.getName());
      textTitle.animate().alpha(1).setDuration(150).start();
    }).setDuration(150).start();
    buttonLists.animate().alpha(0).withEndAction(
        () -> buttonLists.animate().alpha(1).setDuration(150).start()
    ).setDuration(150).start();
  }
}
