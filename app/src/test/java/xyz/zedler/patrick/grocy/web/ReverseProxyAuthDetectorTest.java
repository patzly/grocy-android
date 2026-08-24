package xyz.zedler.patrick.grocy.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReverseProxyAuthDetectorTest {

  @Test
  public void detectsHtmlLoginPages() {
    assertTrue(ReverseProxyAuthDetector.looksLikeLoginPage(
        "<!doctype html><html><title>Sign in</title></html>"
    ));
    assertTrue(ReverseProxyAuthDetector.looksLikeLoginPage(
        "https://grocy.example/outpost.goauthentik.io/start"
    ));
    assertTrue(ReverseProxyAuthDetector.looksLikeLoginPage(
        "https://auth.example/if/flow/default-authentication-flow/"
    ));
  }

  @Test
  public void doesNotClassifyGrocyJsonAsLogin() {
    assertFalse(ReverseProxyAuthDetector.looksLikeLoginPage(
        "{\"grocy_version\":{\"Version\":\"4.6.0\"}}"
    ));
    assertFalse(ReverseProxyAuthDetector.looksLikeLoginPage(null));
  }

  @Test
  public void scopesCookiesAndChallengesToSameOrigin() {
    assertTrue(ReverseProxyAuthDetector.isSameOrigin(
        "https://grocy.example", "https://GROCY.example:443/api/system/info"
    ));
    assertFalse(ReverseProxyAuthDetector.isSameOrigin(
        "https://grocy.example", "https://auth.example/api/system/info"
    ));
    assertFalse(ReverseProxyAuthDetector.isSameOrigin(
        "https://grocy.example", "http://grocy.example/api/system/info"
    ));
    assertFalse(ReverseProxyAuthDetector.isSameOrigin(
        "https://grocy.example:8443", "https://grocy.example/api/system/info"
    ));
  }

  @Test
  public void identifiesProviderCallbackPaths() {
    assertTrue(ReverseProxyAuthDetector.isAuthenticationPath(
        "/outpost.goauthentik.io/callback"
    ));
    assertTrue(ReverseProxyAuthDetector.isAuthenticationPath(
        "/if/flow/default-authentication-flow/"
    ));
    assertFalse(ReverseProxyAuthDetector.isAuthenticationPath("/api/system/info"));
  }
}
