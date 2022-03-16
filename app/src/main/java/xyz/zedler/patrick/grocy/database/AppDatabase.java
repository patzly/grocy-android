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

package xyz.zedler.patrick.grocy.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import xyz.zedler.patrick.grocy.dao.LocationDao;
import xyz.zedler.patrick.grocy.dao.MissingItemDao;
import xyz.zedler.patrick.grocy.dao.ProductAveragePriceDao;
import xyz.zedler.patrick.grocy.dao.ProductBarcodeDao;
import xyz.zedler.patrick.grocy.dao.ProductDao;
import xyz.zedler.patrick.grocy.dao.ProductGroupDao;
import xyz.zedler.patrick.grocy.dao.ProductLastPurchasedDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitConversionDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.dao.StockItemDao;
import xyz.zedler.patrick.grocy.dao.StockLocationDao;
import xyz.zedler.patrick.grocy.dao.StoreDao;
import xyz.zedler.patrick.grocy.dao.TaskCategoryDao;
import xyz.zedler.patrick.grocy.dao.TaskDao;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductAveragePrice;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;

@Database(
    entities = {
        ShoppingList.class,
        ShoppingListItem.class,
        Product.class,
        ProductGroup.class,
        QuantityUnit.class,
        Store.class,
        Location.class,
        MissingItem.class,
        QuantityUnitConversion.class,
        ProductBarcode.class,
        StockItem.class,
        StockLocation.class,
        Task.class,
        TaskCategory.class,
        ProductLastPurchased.class,
        ProductAveragePrice.class
    },
    version = 23
)
public abstract class AppDatabase extends RoomDatabase {

  private static AppDatabase INSTANCE;

  public abstract ShoppingListDao shoppingListDao();

  public abstract ShoppingListItemDao shoppingListItemDao();

  public abstract ProductDao productDao();

  public abstract ProductGroupDao productGroupDao();

  public abstract QuantityUnitDao quantityUnitDao();

  public abstract StoreDao storeDao();

  public abstract LocationDao locationDao();

  public abstract MissingItemDao missingItemDao();

  public abstract QuantityUnitConversionDao quantityUnitConversionDao();

  public abstract ProductBarcodeDao productBarcodeDao();

  public abstract StockItemDao stockItemDao();

  public abstract StockLocationDao stockLocationDao();

  public abstract TaskDao taskDao();

  public abstract TaskCategoryDao taskCategoryDao();

  public abstract ProductLastPurchasedDao productLastPurchasedDao();

  public abstract ProductAveragePriceDao productAveragePriceDao();

  public static AppDatabase getAppDatabase(Context context) {
    if (INSTANCE == null) {
      INSTANCE = Room.databaseBuilder(
          context.getApplicationContext(),
          AppDatabase.class,
          "app_database"
      ).fallbackToDestructiveMigration().build();
    }
    return INSTANCE;
  }

  public static void destroyInstance() {
    INSTANCE = null;
  }
}
