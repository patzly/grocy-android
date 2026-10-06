package xyz.zedler.patrick.grocy.form;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragmentArgs;
import xyz.zedler.patrick.grocy.model.ProductDetails;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class FormDataConsumeTest {
  @Test
  public void confirmationHandlesDeletedLocationForConsumeAndOpen() throws Exception {
    Application app = RuntimeEnvironment.getApplication();
    FormDataConsume form = populatedForm(app);
    assertTrue(form.getConfirmationText(false).contains(app.getString(R.string.subtitle_unknown)));
    assertTrue(form.getConfirmationText(true).contains(app.getString(R.string.subtitle_unknown)));
    assertFalse(form.getFilledJSONObject(false).has("location_id"));
  }

  @Test
  public void disabledLocationTrackingKeepsItsOwnDescription() throws Exception {
    Application app = RuntimeEnvironment.getApplication();
    FormDataConsume form = populatedForm(app);
    PreferenceManager.getDefaultSharedPreferences(app).edit()
        .putBoolean(PREF.FEATURE_STOCK_LOCATION_TRACKING, false).commit();
    assertTrue(form.getConfirmationText(false).contains(
        app.getString(R.string.subtitle_feature_disabled)));
  }

  private FormDataConsume populatedForm(Application app) throws Exception {
    PreferenceManager.getDefaultSharedPreferences(app).edit().clear().commit();
    FormDataConsume form = new FormDataConsume(app,
        PreferenceManager.getDefaultSharedPreferences(app), new ConsumeFragmentArgs.Builder().setStartWithScanner(false).build());
    ProductDetails details = new Gson().fromJson("{\"product\":{\"id\":1,\"name\":\"Milk\","
        + "\"enable_tare_weight_handling\":\"0\",\"tare_weight\":\"0\"},\"stock_amount\":\"5\","
        + "\"quantity_unit_stock\":{\"id\":1,\"name\":\"Bottle\",\"name_plural\":\"Bottles\"}}",
        ProductDetails.class);
    form.getProductDetailsLive().setValue(details);
    form.getProductNameLive().setValue("Milk");
    form.getQuantityUnitStockLive().setValue(details.getQuantityUnitStock());
    form.getQuantityUnitLive().setValue(details.getQuantityUnitStock());
    java.util.HashMap<xyz.zedler.patrick.grocy.model.QuantityUnit, Double> factors = new java.util.HashMap<>();
    factors.put(details.getQuantityUnitStock(), 1.0);
    form.getQuantityUnitsFactorsLive().setValue(factors);
    form.getAmountLive().setValue("1");
    for (Field field : FormDataConsume.class.getDeclaredFields()) {
      field.setAccessible(true);
      if (field.get(form) instanceof LiveData) {
        ((LiveData<?>) field.get(form)).observeForever(value -> {});
      }
    }
    return form;
  }
}
