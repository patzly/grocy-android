package xyz.zedler.patrick.grocy.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.MasterProductCatOptionalFragmentArgs;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.test.TestServer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ProductCopyTest {
  private static final String CONVERSIONS = "[{\"id\":1,\"product_id\":\"8\",\"from_qu_id\":2,"
      + "\"to_qu_id\":1,\"factor\":13.5},{\"id\":2,\"product_id\":\"8\",\"from_qu_id\":1,"
      + "\"to_qu_id\":2,\"factor\":0.07407407407407407}]";
  private static final String TARGET = "[{\"id\":10,\"product_id\":\"40\",\"from_qu_id\":2,"
      + "\"to_qu_id\":1,\"factor\":1}]";

  @Test
  public void copyingDoesNotChangeSourceProduct() throws Exception {
    Application app = RuntimeEnvironment.getApplication();
    Product source = source(app);
    MasterProductViewModel model = new MasterProductViewModel(app,
        new MasterProductFragmentArgs.Builder(Constants.ACTION.CREATE).setProduct(source).build());
    assertEquals("Original", source.getName());
    assertNotSame(source, model.getFormData().getProductLive().getValue());
    assertEquals("shared.jpg", model.getFormData().getProductLive().getValue().getPictureFileName());
  }

  @Test
  public void conversionFailureRetryUpdatesSameProductAndDoesNotDuplicateInverseRows() throws Exception {
    AtomicBoolean fail = new AtomicBoolean(true);
    try (TestServer server = new TestServer(request -> {
      if (request.method.equals("GET")) {
        request.respond(200, request.query.contains("product_id=8") ? CONVERSIONS : TARGET);
      } else if (request.path.endsWith("/products") && request.method.equals("POST")) {
        request.respond(200, "{\"created_object_id\":40}");
      } else if (request.path.endsWith("/quantity_unit_conversions/10") && fail.get()) {
        request.respond(403, "{}");
      } else {
        request.respond(200, "{}");
      }
    })) {
      Application app = configure(server);
      MasterProductViewModel model = model(app);
      model.saveProduct(false);
      model.saveProduct(false);
      TestServer.await(() -> !isSaving(model));
      assertEquals(1, productCreates(server));
      Product createdProduct = source(app);
      createdProduct.setId(40);
      createdProduct.setName("Copy");
      java.lang.reflect.Method namesMethod = MasterProductViewModel.class.getDeclaredMethod(
          "getProductNames", java.util.List.class, String.class);
      namesMethod.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.ArrayList<String> names = (java.util.ArrayList<String>) namesMethod.invoke(model,
          java.util.Arrays.asList(source(app), createdProduct), null);
      assertTrue(!names.contains("Copy"));
      model.getFormData().getProductNamesLive().setValue(names);
      fail.set(false);
      model.saveProduct(false);
      TestServer.await(() -> !isSaving(model));
      assertEquals(1, productCreates(server));
      assertTrue(model.isActionEdit());
      assertEquals(40, model.getFormData().getProductLive().getValue().getId());
      assertTrue(server.requests.stream().filter(request ->
          request.path.endsWith("/quantity_unit_conversions/10") && request.method.equals("PUT")).count() >= 2);
      assertEquals(0, server.requests.stream().filter(request ->
          request.path.endsWith("/quantity_unit_conversions") && request.method.equals("POST")).count());
      TestServer.Request update = server.requests.stream().filter(request ->
          request.path.endsWith("/quantity_unit_conversions/10")).findFirst().get();
      assertEquals(13.5, new JSONObject(update.body).getDouble("factor"), 0);
      assertEquals(40, new JSONObject(update.body).getInt("product_id"));
    }
  }

  @Test
  public void lostConversionResponseDoesNotRepeatPostAndRetryFindsCreatedRow() throws Exception {
    AtomicBoolean created = new AtomicBoolean(false);
    try (TestServer server = new TestServer(request -> {
      if (request.method.equals("GET")) {
        request.respond(200, request.query.contains("product_id=8") ? CONVERSIONS
            : created.get() ? TARGET.replace("\"id\":10", "\"id\":77") : "[]");
      } else if (request.method.equals("POST") && request.path.endsWith("/quantity_unit_conversions")) {
        created.set(true);
        try {
          Thread.sleep(1500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        // The server applied the insert, but the response arrived after the timeout.
      } else {
        request.respond(200, "{\"created_object_id\":40}");
      }
    })) {
      Application app = configure(server);
      PreferenceManager.getDefaultSharedPreferences(app).edit()
          .putInt(Constants.SETTINGS.NETWORK.LOADING_TIMEOUT, 1).commit();
      MasterProductViewModel model = model(app);
      model.saveProduct(false);
      TestServer.await(() -> !isSaving(model));
      model.saveProduct(false);
      TestServer.await(() -> !isSaving(model));
      assertTrue(model.isActionEdit());
      assertEquals(1, productCreates(server));
      assertEquals(1, server.requests.stream().filter(request -> request.method.equals("POST")
          && request.path.endsWith("/quantity_unit_conversions")).count());
      assertTrue(server.requests.stream().anyMatch(request -> request.method.equals("PUT")
          && request.path.endsWith("/quantity_unit_conversions/77")));
    }
  }

  @Test
  public void sourceConversionDownloadFailureDoesNotCreateProduct() throws Exception {
    try (TestServer server = new TestServer(request -> request.respond(403, "{}"))) {
      MasterProductViewModel model = model(configure(server));
      model.saveProduct(false);
      TestServer.await(() -> !isSaving(model));
      assertEquals(0, productCreates(server));
    }
  }

  @Test
  public void legacyServersKeepPurchaseFactorInProductPayload() throws Exception {
    try (TestServer server = new TestServer(request -> request.respond(200,
        request.method.equals("GET") ? "[]" : "{\"created_object_id\":40}"))) {
      Application app = configure(server);
      PreferenceManager.getDefaultSharedPreferences(app).edit()
          .putString(Constants.PREF.GROCY_VERSION, "3.3.2").commit();
      MasterProductViewModel model = model(app);
      model.saveProduct(false);
      TestServer.await(() -> !isSaving(model));
      TestServer.Request create = server.requests.stream().filter(request ->
          request.method.equals("POST") && request.path.endsWith("/products")).findFirst().get();
      assertEquals(20, new JSONObject(create.body).getDouble("qu_factor_purchase_to_stock"), 0);
    }
  }

  @Test
  public void clearingSharedPhotoUnlinksProductAndNeverDeletesFile() throws Exception {
    verifyPhotoRemoval(200, "");
  }

  @Test
  public void failedPhotoUnlinkKeepsExistingPhoto() throws Exception {
    verifyPhotoRemoval(403, "shared.jpg");
  }

  @Test
  public void replacingSharedPhotoUpdatesReferenceAfterUploadWithoutDeletingOldFile() throws Exception {
    try (TestServer server = new TestServer(request -> request.respond(200, "{}"))) {
      Application app = configure(server);
      MasterProductCatOptionalViewModel model = new MasterProductCatOptionalViewModel(app,
          new MasterProductCatOptionalFragmentArgs.Builder(Constants.ACTION.EDIT)
              .setProduct(source(app)).build());
      model.getFormData().getPictureFilenameLive().setValue("shared.jpg");
      model.uploadPicture(new byte[]{1, 2, 3});
      TestServer.await(() -> !"shared.jpg".equals(model.getFormData().getPictureFilenameLive().getValue()));
      assertEquals(2, server.requests.size());
      assertTrue(server.requests.stream().noneMatch(request -> request.method.equals("DELETE")));
      assertEquals(model.getFormData().getPictureFilenameLive().getValue(),
          new JSONObject(server.requests.get(1).body).getString("picture_file_name"));
    }
  }

  private void verifyPhotoRemoval(int status, String expected) throws Exception {
    try (TestServer server = new TestServer(request -> request.respond(status, "{}"))) {
      Application app = configure(server);
      MasterProductCatOptionalViewModel model = new MasterProductCatOptionalViewModel(app,
          new MasterProductCatOptionalFragmentArgs.Builder(Constants.ACTION.EDIT)
              .setProduct(source(app)).build());
      model.getFormData().getPictureFilenameLive().setValue("shared.jpg");
      model.deleteCurrentPicture(null);
      TestServer.await(() -> !model.getIsLoadingLive().getValue());
      assertEquals(expected, model.getFormData().getPictureFilenameLive().getValue());
      assertTrue(!server.requests.isEmpty());
      assertTrue(server.requests.stream().allMatch(request -> request.method.equals("PUT")
          && request.path.endsWith("/objects/products/8")));
      assertEquals("", new JSONObject(server.requests.get(0).body).getString("picture_file_name"));
    }
  }

  private Application configure(TestServer server) {
    Application app = RuntimeEnvironment.getApplication();
    PreferenceManager.getDefaultSharedPreferences(app).edit().clear()
        .putString(Constants.PREF.SERVER_URL, server.url())
        .putString(Constants.PREF.GROCY_VERSION, "4.0.3").commit();
    return app;
  }

  private Product source(Application app) {
    Product product = new Product(PreferenceManager.getDefaultSharedPreferences(app));
    product.setId(8);
    product.setName("Original");
    product.setLocationId("1");
    product.setQuIdStock(1);
    product.setQuIdPurchase(2);
    product.setQuIdConsume(1);
    product.setQuIdPrice(1);
    product.setPictureFileName("shared.jpg");
    product.setQuFactorPurchaseToStock("20");
    return product;
  }

  private MasterProductViewModel model(Application app) throws Exception {
    MasterProductViewModel model = new MasterProductViewModel(app,
        new MasterProductFragmentArgs.Builder(Constants.ACTION.CREATE)
            .setProduct(source(app)).setProductName("Copy").build());
    Object form = model.getFormData();
    for (Field field : form.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      if (field.get(form) instanceof LiveData) {
        ((LiveData<?>) field.get(form)).observeForever(value -> {});
      }
    }
    assertTrue(model.getFormData().isWholeFormValid());
    return model;
  }

  private boolean isSaving(MasterProductViewModel model) {
    try {
      Field field = MasterProductViewModel.class.getDeclaredField("saveInProgress");
      field.setAccessible(true);
      return field.getBoolean(model);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private long productCreates(TestServer server) {
    return server.requests.stream().filter(request ->
        request.path.endsWith("/objects/products") && request.method.equals("POST")).count();
  }
}
