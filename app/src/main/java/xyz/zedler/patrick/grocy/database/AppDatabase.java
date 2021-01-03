package xyz.zedler.patrick.grocy.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import xyz.zedler.patrick.grocy.dao.LocationDao;
import xyz.zedler.patrick.grocy.dao.MissingItemDao;
import xyz.zedler.patrick.grocy.dao.ProductBarcodeDao;
import xyz.zedler.patrick.grocy.dao.ProductDao;
import xyz.zedler.patrick.grocy.dao.ProductGroupDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitConversionDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.dao.StoreDao;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;

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
                ProductBarcode.class
        },
        version = 8
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
