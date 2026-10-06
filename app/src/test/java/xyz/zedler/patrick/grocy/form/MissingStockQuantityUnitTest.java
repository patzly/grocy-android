package xyz.zedler.patrick.grocy.form;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.InventoryFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragmentArgs;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MissingStockQuantityUnitTest {
  @Test
  public void consumeCannotSubmitWithoutStockUnitEvenWhenAnotherUnitIsSelected() throws Exception {
    Application app = RuntimeEnvironment.getApplication();
    SharedPreferences prefs = preferences(app);
    FormDataConsume form = new FormDataConsume(app, prefs,
        new ConsumeFragmentArgs.Builder().setStartWithScanner(false).build());
    populate(form);
    assertTrue(form.isFormValid());
    form.getQuantityUnitStockLive().setValue(null);
    assertFalse(form.isFormValid());
    assertTrue(form.getQuantityUnitErrorLive().getValue());
  }

  @Test
  public void purchaseCannotSubmitWithoutStockUnitEvenWhenAnotherUnitIsSelected() throws Exception {
    Application app = RuntimeEnvironment.getApplication();
    FormDataPurchase form = new FormDataPurchase(app, preferences(app),
        new PurchaseFragmentArgs.Builder().setStartWithScanner(false).setCloseWhenFinished(false).build());
    populate(form);
    assertTrue(form.isFormValid());
    form.getQuantityUnitStockLive().setValue(null);
    assertFalse(form.isFormValid());
    assertTrue(form.getQuantityUnitErrorLive().getValue());
  }

  @Test
  public void inventoryCannotSubmitWithoutStockUnitEvenWhenAnotherUnitIsSelected() throws Exception {
    Application app = RuntimeEnvironment.getApplication();
    FormDataInventory form = new FormDataInventory(app, preferences(app),
        new InventoryFragmentArgs.Builder().setCloseWhenFinished(false).build());
    populate(form);
    assertTrue(form.isFormValid());
    form.getQuantityUnitStockLive().setValue(null);
    assertFalse(form.isFormValid());
    assertTrue(form.getQuantityUnitErrorLive().getValue());
  }

  private SharedPreferences preferences(Application app) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
    prefs.edit().clear().putBoolean(PREF.FEATURE_STOCK_LOCATION_TRACKING, false)
        .putBoolean(PREF.FEATURE_STOCK_PRICE_TRACKING, false)
        .putBoolean(PREF.FEATURE_STOCK_BBD_TRACKING, false).commit();
    return prefs;
  }

  private void populate(Object form) throws Exception {
    Gson gson = new Gson();
    ProductDetails details = gson.fromJson("{\"product\":{\"id\":1,\"name\":\"Milk\","
        + "\"enable_tare_weight_handling\":\"0\",\"tare_weight\":\"0\"},\"stock_amount\":\"5\","
        + "\"quantity_unit_stock\":{\"id\":2,\"name\":\"Piece\",\"name_plural\":\"Pieces\"}}",
        ProductDetails.class);
    QuantityUnit unit = gson.fromJson("{\"id\":3,\"name\":\"Pack\",\"name_plural\":\"Packs\"}", QuantityUnit.class);
    HashMap<QuantityUnit, Double> factors = new HashMap<>();
    factors.put(unit, 1.0);
    set(form, "productDetailsLive", details);
    set(form, "productNameLive", "Milk");
    set(form, "quantityUnitStockLive", details.getQuantityUnitStock());
    set(form, "quantityUnitLive", unit);
    set(form, "quantityUnitsFactorsLive", factors);
    set(form, "amountLive", "1");
    for (Field field : form.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      if (field.get(form) instanceof LiveData) {
        ((LiveData<?>) field.get(form)).observeForever(value -> {});
      }
    }
  }

  private void set(Object form, String name, Object value) throws Exception {
    Field field = form.getClass().getDeclaredField(name);
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    androidx.lifecycle.MutableLiveData<Object> data = (androidx.lifecycle.MutableLiveData<Object>) field.get(form);
    data.setValue(value);
  }
}
