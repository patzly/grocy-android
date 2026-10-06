package xyz.zedler.patrick.grocy.viewmodel;

import static org.junit.Assert.assertEquals;
import android.app.Application;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import xyz.zedler.patrick.grocy.fragment.StockOverviewFragmentArgs;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.SortUtil;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class StockOverviewSearchTest {
  @Test
  public void searchIgnoresMissingNamesAndFindsHealthyProducts() throws Exception {
    Application app = RuntimeEnvironment.getApplication();
    StockOverviewViewModel model = new StockOverviewViewModel(app,
        new StockOverviewFragmentArgs.Builder().build());
    Gson gson = new Gson();
    Product unnamed = gson.fromJson("{\"id\":1,\"name\":null}", Product.class);
    Product milk = gson.fromJson("{\"id\":2,\"name\":\"Milk\"}", Product.class);
    StockItem bad = gson.fromJson("{\"product_id\":1,\"product\":{\"id\":1,\"name\":null}}", StockItem.class);
    StockItem good = gson.fromJson("{\"product_id\":2,\"product\":{\"id\":2,\"name\":\"Milk\"}}", StockItem.class);
    set(model, "products", Arrays.asList(unnamed, milk));
    set(model, "stockItems", Arrays.asList(bad, good));
    set(model, "productHashMap", new HashMap<>());
    set(model, "productBarcodeHashMap", new HashMap<>());
    set(model, "productIdsMissingItems", new HashMap<>());
    set(model, "stockLocationsHashMap", new HashMap<>());
    model.updateSearchInput("Milk");
    assertEquals(1, model.getFilteredStockItemsLive().getValue().size());
    assertEquals(2, model.getFilteredStockItemsLive().getValue().get(0).getProductId());
  }

  @Test
  public void sortingStockAndProductsHandlesMissingNames() {
    Gson gson = new Gson();
    Product unnamed = gson.fromJson("{\"id\":1,\"name\":null}", Product.class);
    Product milk = gson.fromJson("{\"id\":2,\"name\":\"Milk\"}", Product.class);
    StockItem bad = gson.fromJson("{\"product_id\":1,\"product\":{\"id\":1,\"name\":null}}", StockItem.class);
    StockItem good = gson.fromJson("{\"product_id\":2,\"product\":{\"id\":2,\"name\":\"Milk\"}}", StockItem.class);
    java.util.List<Product> products = Arrays.asList(milk, unnamed);
    SortUtil.sortProductsByName(products, true);
    assertEquals(1, products.get(0).getId());
    java.util.List<StockItem> stock = Arrays.asList(good, bad);
    SortUtil.sortStockItemsByName(stock, true);
    assertEquals(1, stock.get(0).getProductId());
  }

  @Test
  public void searchBeforeDatabaseLoadsDoesNotCrash() {
    StockOverviewViewModel model = new StockOverviewViewModel(RuntimeEnvironment.getApplication(),
        new StockOverviewFragmentArgs.Builder().build());
    model.updateSearchInput("Milk");
  }

  private void set(Object target, String name, Object value) throws Exception {
    Field field = StockOverviewViewModel.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
