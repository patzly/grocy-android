package xyz.zedler.patrick.grocy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;

public class SortUtil {

    public static void sortStockItemsByName(List<StockItem> stockItems, boolean ascending) {
        if(stockItems == null) return;
        Collections.sort(
                stockItems,
                (item1, item2) -> (ascending ? item1 : item2).getProduct()
                        .getName()
                        .toLowerCase().compareTo(
                                (ascending ? item2 : item1).getProduct().getName().toLowerCase()
                )
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

    public static void sortLocationsByName(ArrayList<Location> locations, boolean ascending) {
        if(locations == null) return;
        Collections.sort(
                locations,
                (item1, item2) -> (ascending ? item1 : item2).getName().toLowerCase().compareTo(
                        (ascending ? item2 : item1).getName().toLowerCase()
                )
        );
    }

    public static void sortStoresByName(List<Store> stores, boolean ascending) {
        if(stores == null) return;
        Collections.sort(
                stores,
                (item1, item2) -> (ascending ? item1 : item2).getName().toLowerCase().compareTo(
                        (ascending ? item2 : item1).getName().toLowerCase()
                )
        );
    }

    public static void sortProductGroupsByName(
            ArrayList<ProductGroup> productGroups,
            boolean ascending
    ) {
        if(productGroups == null || productGroups.isEmpty()) return;
        Collections.sort(
                productGroups,
                (item1, item2) -> (ascending ? item1 : item2).getName().toLowerCase().compareTo(
                        (ascending ? item2 : item1).getName().toLowerCase()
                )
        );
    }

    public static void sortQuantityUnitsByName(
            ArrayList<QuantityUnit> quantityUnits,
            boolean ascending
    ) {
        if(quantityUnits == null) return;
        Collections.sort(
                quantityUnits,
                (item1, item2) -> (ascending ? item1 : item2).getName().toLowerCase().compareTo(
                        (ascending ? item2 : item1).getName().toLowerCase()
                )
        );
    }

    public static void sortShoppingListItemsByName(
            List<ShoppingListItem> shoppingListItems,
            boolean ascending
    ) {
        if(shoppingListItems == null) return;
        Collections.sort(
                shoppingListItems,
                (item1, item2) -> (ascending ? item1 : item2).getProduct()
                        .getName()
                        .toLowerCase().compareTo(
                                (ascending ? item2 : item1).getProduct().getName().toLowerCase()
                        )
        );
    }
}
