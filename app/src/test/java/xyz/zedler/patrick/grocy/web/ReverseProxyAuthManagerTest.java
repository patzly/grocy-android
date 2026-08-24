package xyz.zedler.patrick.grocy.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class ReverseProxyAuthManagerTest {

  @Rule
  public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @After
  public void resetConfiguration() {
    ReverseProxyAuthManager.configure(null);
  }

  @Test
  public void rejectsRequestsUntilServerIsConfigured() {
    ReverseProxyAuthManager.configure(null);

    assertFalse(ReverseProxyAuthManager.isConfiguredServerRequest(
        "https://grocy.example/api/system/info"
    ));
  }

  @Test
  public void usesNewServerImmediatelyAfterReconfiguration() {
    ReverseProxyAuthManager.configure("https://old.example");
    ReverseProxyAuthManager.configure("https://grocy.example");

    assertTrue(ReverseProxyAuthManager.isConfiguredServerRequest(
        "https://grocy.example/api/system/info"
    ));
    assertFalse(ReverseProxyAuthManager.isConfiguredServerRequest(
        "https://old.example/api/system/info"
    ));
  }

  @Test
  public void stillRejectsDifferentOrigins() {
    ReverseProxyAuthManager.configure("https://grocy.example");

    assertFalse(ReverseProxyAuthManager.isConfiguredServerRequest(
        "https://attacker.example/api/system/info"
    ));
  }

  @Test
  public void consumesAdditionalChallengeWhileAuthenticationIsOpen() {
    ReverseProxyAuthManager.configure("https://grocy.example");
    ReverseProxyAuthManager.handleResponse(
        "https://grocy.example/api/system/info",
        "<html><title>Sign in - authentik</title></html>"
    );

    String challenge = ReverseProxyAuthManager.getAuthenticationRequired().getValue();
    assertFalse(ReverseProxyAuthManager.shouldLaunchAuthentication(challenge, true));
    assertNull(ReverseProxyAuthManager.getAuthenticationRequired().getValue());
  }

  @Test
  public void launchesFirstAuthenticationChallenge() {
    assertTrue(ReverseProxyAuthManager.shouldLaunchAuthentication(
        "https://grocy.example",
        false
    ));
  }
}
