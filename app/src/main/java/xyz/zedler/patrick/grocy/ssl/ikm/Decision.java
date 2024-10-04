package xyz.zedler.patrick.grocy.ssl.ikm;

/**
 * Source: github.com/stephanritscher/InteractiveKeyManager
 */
public class Decision {

  public final static int DECISION_INVALID = 0;
  public final static int DECISION_ABORT = 1;
  public final static int DECISION_KEYCHAIN = 2;
  public final static int DECISION_FILE = 3;

  public int state = DECISION_INVALID;
  public String param;
  public String hostname;
  public Integer port;
}