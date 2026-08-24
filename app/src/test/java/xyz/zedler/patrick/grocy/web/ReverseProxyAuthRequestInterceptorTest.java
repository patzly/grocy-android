package xyz.zedler.patrick.grocy.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class ReverseProxyAuthRequestInterceptorTest {

  @Rule
  public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @After
  public void resetManager() {
    ReverseProxyAuthManager.configure(null);
    ReverseProxyAuthManager.consumeAuthenticationRequest();
  }

  @Test
  public void interceptedLoginResponseCompletesThroughErrorCallback() {
    ReverseProxyAuthManager.configure("https://grocy.example");
    AtomicInteger errors = new AtomicInteger();
    AtomicReference<VolleyError> deliveredError = new AtomicReference<>();

    boolean intercepted = ReverseProxyAuthRequestInterceptor.intercept(
        "https://grocy.example/api/system/info",
        "<html><title>Sign in - authentik</title></html>",
        error -> {
          errors.incrementAndGet();
          deliveredError.set(error);
        }
    );

    assertTrue(intercepted);
    assertEquals(1, errors.get());
    assertTrue(deliveredError.get() instanceof AuthFailureError);
  }

  @Test
  public void validGrocyResponseIsNotIntercepted() {
    ReverseProxyAuthManager.configure("https://grocy.example");
    AtomicInteger errors = new AtomicInteger();

    boolean intercepted = ReverseProxyAuthRequestInterceptor.intercept(
        "https://grocy.example/api/system/info",
        "{\"grocy_version\":{\"Version\":\"4.0.0\"}}",
        error -> errors.incrementAndGet()
    );

    assertFalse(intercepted);
    assertEquals(0, errors.get());
  }
}
