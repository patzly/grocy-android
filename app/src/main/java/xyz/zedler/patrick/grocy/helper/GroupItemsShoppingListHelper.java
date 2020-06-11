package xyz.zedler.patrick.grocy.helper;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

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

public class GroupItemsShoppingListHelper {

    public static ArrayList<GroupedListItem> groupItems(
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ProductGroup> productGroups,
            ArrayList<ShoppingList> shoppingLists,
            int selectedShoppingListId,
            Activity activity
    ) {
        HashMap<String, ProductGroup> productGroupHashMap = new HashMap<>();
        for(ProductGroup p : productGroups) productGroupHashMap.put(String.valueOf(p.getId()), p);
        HashMap<ProductGroup, Collection<ShoppingListItem>> sortedShoppingListItems = new HashMap<>();
        ProductGroup ungrouped = new ProductGroup(
                -1,
                activity.getString(R.string.title_shopping_list_ungrouped)
        );
        // sort displayedItems by productGroup
        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            String groupId = null;
            ProductGroup productGroup = null;
            Product product = shoppingListItem.getProduct();
            if(product != null) groupId = product.getProductGroupId();
            if(groupId != null && groupId.isEmpty()) groupId = null;
            if(groupId != null) productGroup = productGroupHashMap.get(groupId);
            if(groupId == null || productGroup == null) productGroup = ungrouped;
            Collection<ShoppingListItem> items = sortedShoppingListItems.get(productGroup);
            if(items == null) {
                items = new ArrayList<>();
                sortedShoppingListItems.put(productGroup, items);
            }
            items.add(shoppingListItem);
        }
        // sort product groups
        ArrayList<ProductGroup> sortedProductGroups = new ArrayList<>(sortedShoppingListItems.keySet());
        SortUtil.sortProductGroupsByName(sortedProductGroups, true);
        if(sortedProductGroups.contains(ungrouped)) {
            sortedProductGroups.remove(ungrouped);
            sortedProductGroups.add(ungrouped);
        }
        // create list with groups (headers) and entries
        ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
        for(ProductGroup productGroup : sortedProductGroups) {
            groupedListItems.add(productGroup);
            Collection<ShoppingListItem> items = sortedShoppingListItems.get(productGroup);
            assert items != null;
            ArrayList<ShoppingListItem> itemsOneGroup = new ArrayList<>(items);
            SortUtil.sortShoppingListItemsByName(itemsOneGroup, true);
            groupedListItems.addAll(itemsOneGroup);
        }
        // add bottom notes if they are not empty
        HashMap<Integer, ShoppingList> shoppingListHashMap = new HashMap<>();
        for(ShoppingList s : shoppingLists) shoppingListHashMap.put(s.getId(), s);
        ShoppingList shoppingList = shoppingListHashMap.get(selectedShoppingListId);
        Spanned notes = null;
        if(shoppingList != null && shoppingList.getNotes() != null) {
            Spanned spanned = Html.fromHtml(shoppingList.getNotes().trim());
            notes = (Spanned) TextUtil.trimCharSequence(spanned);
        }
        if(notes != null && notes.toString().trim().isEmpty()) notes = null;
        if(shoppingList != null && notes != null) {
            ProductGroup p = new ProductGroup(-1, activity.getString(R.string.property_notes));
            groupedListItems.add(p);
            groupedListItems.add(new ShoppingListBottomNotes(notes));
        }
        return groupedListItems;
    }
}
