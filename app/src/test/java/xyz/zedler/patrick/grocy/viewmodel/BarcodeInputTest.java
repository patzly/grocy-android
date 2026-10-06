package xyz.zedler.patrick.grocy.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.app.Application;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.InventoryFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragmentArgs;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BarcodeInputTest {
  @Test
  public void typedUnknownNumericBarcodeUsesLookupInAllTransactionForms() throws Exception {
    Purchase purchase = new Purchase();
    Consume consume = new Consume();
    Inventory inventory = new Inventory();
    for (BaseViewModel model : Arrays.asList(purchase, consume, inventory)) {
      populate(model, new ArrayList<>());
      List<Event> events = new ArrayList<>();
      model.getEventHandler().observeForever(events::add);
      if (model instanceof Purchase) {
        purchase.getFormData().getProductNameLive().setValue(" 1234567890123 ");
        purchase.checkProductInput();
      } else if (model instanceof Consume) {
        consume.getFormData().getProductNameLive().setValue(" 1234567890123 ");
        consume.checkProductInput();
      } else {
        inventory.getFormData().getProductNameLive().setValue(" 1234567890123 ");
        inventory.checkProductInput();
      }
      Event event = events.get(events.size() - 1);
      assertEquals(Event.CHOOSE_PRODUCT, event.getType());
      assertEquals("1234567890123", event.getBundle().getString(ARGUMENT.BARCODE));
    }
  }

  @Test
  public void pastedGrocycodePreservesConsumeStockEntry() throws Exception {
    Consume model = new Consume();
    populate(model, new ArrayList<>());
    model.getFormData().getProductNameLive().setValue(" grcy:p:1:abc123 ");
    model.checkProductInput();
    assertEquals(1, model.selectedId);
    assertEquals("abc123", model.stockEntryId);
  }

  @Test
  public void pastedPendingBarcodeUsesPendingProductRoute() throws Exception {
    Purchase model = new Purchase();
    PendingProductBarcode barcode = new PendingProductBarcode();
    barcode.setBarcode("pending-123");
    barcode.setPendingProductId(7);
    populate(model, Arrays.asList(barcode));
    model.getFormData().getProductNameLive().setValue(" pending-123 ");
    model.checkProductInput();
    assertEquals(7, model.pendingId);
    assertSame(barcode, model.selectedBarcode);
  }

  @Test
  public void knownBarcodeAndProductNameStillSelectProducts() throws Exception {
    Inventory model = new Inventory();
    ProductBarcode barcode = new ProductBarcode();
    barcode.setBarcode("ABC-123");
    barcode.setProductId("1");
    populate(model, Arrays.asList(barcode));
    model.getFormData().getProductNameLive().setValue(" ABC-123 ");
    model.checkProductInput();
    assertEquals(1, model.selectedId);
    assertSame(barcode, model.selectedBarcode);
    model.getFormData().getProductNameLive().setValue("Milk");
    model.checkProductInput();
    assertEquals(1, model.selectedId);
    assertEquals(null, model.selectedBarcode);
  }

  @Test
  public void scannerAndTypedCodesBothTrimWhitespace() throws Exception {
    Purchase model = new Purchase();
    populate(model, new ArrayList<>());
    List<Event> events = new ArrayList<>();
    model.getEventHandler().observeForever(events::add);
    model.onBarcodeRecognized(" 1234567890123 ");
    assertEquals("1234567890123", events.get(0).getBundle().getString(ARGUMENT.BARCODE));
  }

  private void populate(BaseViewModel model, List<ProductBarcode> barcodes) throws Exception {
    Product product = new Product();
    product.setId(1);
    product.setName("Milk");
    Class<?> type = model.getClass().getSuperclass();
    set(type, model, "products", Arrays.asList(product));
    set(type, model, "barcodes", barcodes);
    if (model instanceof Purchase) {
      HashMap<Integer, Product> products = new HashMap<>();
      products.put(1, product);
      set(type, model, "productHashMap", products);
    }
  }

  private void set(Class<?> type, Object target, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Application app() { return RuntimeEnvironment.getApplication(); }

  private static class Purchase extends PurchaseViewModel {
    int selectedId;
    int pendingId;
    ProductBarcode selectedBarcode;
    Purchase() { super(app(), new PurchaseFragmentArgs.Builder().setStartWithScanner(false).setCloseWhenFinished(false).build()); }
    @Override public void setProduct(Integer id, ProductBarcode barcode, ShoppingListItem item) {
      selectedId = id;
      selectedBarcode = barcode;
    }
    @Override public void setPendingProduct(int id, PendingProductBarcode barcode) {
      pendingId = id;
      selectedBarcode = barcode;
    }
  }

  private static class Consume extends ConsumeViewModel {
    int selectedId;
    String stockEntryId;
    Consume() { super(app(), new ConsumeFragmentArgs.Builder().setStartWithScanner(false).setCloseWhenFinished(false).build()); }
    @Override public void setProduct(int id, ProductBarcode barcode, String entryId) {
      selectedId = id;
      stockEntryId = entryId;
    }
  }

  private static class Inventory extends InventoryViewModel {
    int selectedId;
    ProductBarcode selectedBarcode;
    Inventory() { super(app(), new InventoryFragmentArgs.Builder().setCloseWhenFinished(false).build()); }
    @Override public void setProduct(int id, ProductBarcode barcode) {
      selectedId = id;
      selectedBarcode = barcode;
    }
  }
}
