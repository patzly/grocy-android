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

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;

public class SortUtil {

    // Source: https://medium.com/fme-developer-stories/how-to-sort-umlaute-in-java-correctly-13f3262f15a1
    private static final String EXT_GERMAN_RULES = "&< ' ' < '.'"+
            "<0<1<2<3<4<5<6<7<8<9<a,A<b,B<c,C<d,D<ð,Ð<e,E<f,F<g,G<h,H<i,I<j"+
            ",J<k,K<l,L<m,M<n,N<o,O<p,P<q,Q<r,R<s, S & SS,ß<t,T& TH, Þ &TH,"+
            "þ <u,U<v,V<w,W<x,X<y,Y<z,Z&AE,Æ&AE,æ&OE,Œ&OE,œ";

    private static void compareStockItemsWithExtGerman(List<StockItem> items, boolean asc) {
        Collections.sort(items, (item1, item2) -> Collator.getInstance(Locale.GERMAN).compare(
                (asc ? item1 : item2).getProduct().getName().toLowerCase(),
                (asc ? item2 : item1).getProduct().getName().toLowerCase()
        ));
    }

    public static void sortStockItemsByName(
            Context context, List<StockItem> stockItems, boolean ascending
    ) {
        if(stockItems == null) return;
        Locale locale = LocaleUtil.getUserLocale(context);
        Collections.sort(stockItems, (item1, item2) -> Collator.getInstance(locale).compare(
                (ascending ? item1 : item2).getProduct().getName().toLowerCase(),
                (ascending ? item2 : item1).getProduct().getName().toLowerCase())
        );
    }

    public static void sortStockItemsByBBD(List<StockItem> stockItems, boolean ascending) {
        if(stockItems == null) return;
        Collections.sort(
                stockItems,
                (item1, item2) -> {
                    String bbd1 = (ascending ? item1 : item2).getBestBeforeDate();
                    String bbd2 = (ascending ? item2 : item1).getBestBeforeDate();
                    if(bbd1 == null && bbd2 == null) {
                        return 0;
                    } else if(bbd1 == null) {
                        return -1; // or 1 when items without BBD should be last
                    } else if(bbd2 == null) {
                        return 1; // or -1 when items without BBD should be last
                    }
                    return DateUtil.getDate(bbd1).compareTo(DateUtil.getDate(bbd2));
                }
        );
    }

    public static void sortProductsByName(List<Product> products, boolean ascending) {
        if(products == null) return;
        Collections.sort(
                products,
                (item1, item2) -> (ascending ? item1 : item2).getName().toLowerCase().compareTo(
                        (ascending ? item2 : item1).getName().toLowerCase()
                )
        );
    }

    public static void sortStockLocationItemsByName(ArrayList<StockLocation> stockLocations) {
        if(stockLocations == null) return;
        Collections.sort(
                stockLocations,
                (item1, item2) -> item1.getLocationName().toLowerCase().compareTo(
                        item2.getLocationName().toLowerCase()
                )
        );
    }

    public static void sortLocationsByName(
            Context context, ArrayList<Location> locations, boolean ascending
    ) {
        if(locations == null) return;
        Locale locale = LocaleUtil.getUserLocale(context);
        Collections.sort(locations, (item1, item2) -> Collator.getInstance(locale).compare(
                (ascending ? item1 : item2).getName().toLowerCase(),
                (ascending ? item2 : item1).getName().toLowerCase()
        ));
    }

    public static void sortStoresByName(Context context, List<Store> stores, boolean ascending) {
        if(stores == null) return;
        Locale locale = LocaleUtil.getUserLocale(context);
        Collections.sort(stores, (item1, item2) -> Collator.getInstance(locale).compare(
                (ascending ? item1 : item2).getName().toLowerCase(),
                (ascending ? item2 : item1).getName().toLowerCase()
        ));
    }

    public static void sortProductGroupsByName(
            Context context,
            ArrayList<ProductGroup> productGroups,
            boolean ascending
    ) {
        if(productGroups == null || productGroups.isEmpty()) return;
        Locale locale = LocaleUtil.getUserLocale(context);
        Collections.sort(productGroups, (item1, item2) -> Collator.getInstance(locale).compare(
                (ascending ? item1 : item2).getName().toLowerCase(),
                (ascending ? item2 : item1).getName().toLowerCase()
        ));
    }

    public static void sortQuantityUnitsByName(
            Context context,
            ArrayList<QuantityUnit> quantityUnits,
            boolean ascending
    ) {
        if(quantityUnits == null) return;
        Locale locale = LocaleUtil.getUserLocale(context);
        Collections.sort(quantityUnits, (item1, item2) -> Collator.getInstance(locale).compare(
                (ascending ? item1 : item2).getName().toLowerCase(),
                (ascending ? item2 : item1).getName().toLowerCase()
        ));
    }

    public static void sortShoppingListItemsByName(
            Context context,
            List<ShoppingListItem> shoppingListItems,
            HashMap<Integer, String> productNamesHashMap,
            boolean ascending
    ) {
        if(shoppingListItems == null) return;
        Locale locale = LocaleUtil.getUserLocale(context);
        ArrayList<ShoppingListItem> itemsWithoutProduct = new ArrayList<>();
        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(!shoppingListItem.hasProduct()) itemsWithoutProduct.add(shoppingListItem);
        }
        Collections.sort(
                itemsWithoutProduct,
                (item1, item2) -> {
                    String noteA = (ascending ? item1 : item2).getNote();
                    String noteB = (ascending ? item2 : item1).getNote();
                    if(noteA != null && noteB != null) {
                        return Collator.getInstance(locale).compare(noteA, noteB);
                    }
                    else if(noteA == null && noteB != null) return -1;
                    else if(noteA != null) return 1;
                    else return 0;
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
                    if(nameA != null && nameB != null) {
                        return Collator.getInstance(locale).compare(nameA, nameB);
                    }
                    else if(nameA == null && nameB != null) return -1;
                    else if(nameA != null) return 1;
                    else return 0;
                }
        );
        shoppingListItems.addAll(itemsWithoutProduct);
    }

    public static void sortLanguagesByName(List<Language> languages) {
        if(languages == null) return;
        Collections.sort(languages, (item1, item2) -> item1.getName().toLowerCase()
                .compareTo(item2.getName().toLowerCase()));
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public static List<ShortcutInfo> sortShortcutsById(
            List<ShortcutInfo> shortcutInfos,
            List<String> shortcutIdsSorted
    ) {
        HashMap<String, ShortcutInfo> shortcutInfoHashMap = new HashMap<>();
        for(ShortcutInfo shortcutInfo : shortcutInfos) {
            shortcutInfoHashMap.put(shortcutInfo.getId(), shortcutInfo);
        }
        List<ShortcutInfo> sorted = new ArrayList<>();
        for(String id : shortcutIdsSorted) {
            ShortcutInfo info = shortcutInfoHashMap.get(id);
            if(info != null) sorted.add(info);
        }
        return sorted;
    }
}
