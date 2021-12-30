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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

public abstract class GroupedListItem {

  public static final int TYPE_HEADER = 0;
  public static final int TYPE_ENTRY = 1;
  public static final int TYPE_BOTTOM_NOTES = 2;

  public static final String CONTEXT_SHOPPING_LIST = "shopping_list";
  public static final String CONTEXT_STOCK_OVERVIEW = "stock_overview";

  public static int getType(GroupedListItem groupedListItem, String context) {
    if (context.equals(CONTEXT_SHOPPING_LIST)) {
      if (groupedListItem instanceof ShoppingListItem) {
        return GroupedListItem.TYPE_ENTRY;
      } else if (groupedListItem instanceof ProductGroup) {
        return GroupedListItem.TYPE_HEADER;
      } else {
        return GroupedListItem.TYPE_BOTTOM_NOTES;
      }
    } else if (context.equals(CONTEXT_STOCK_OVERVIEW)) {
      if (groupedListItem instanceof StockItem) {
        return GroupedListItem.TYPE_ENTRY;
      } else {
        return GroupedListItem.TYPE_HEADER;
      }
    } else {
      return TYPE_ENTRY;
    }
  }

  public int getType(String context) {
    return getType(this, context);
  }
}
