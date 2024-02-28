
package xyz.zedler.patrick.grocy.ssl.ikm;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import xyz.zedler.patrick.grocy.R;

/**
 * Source: github.com/stephanritscher/InteractiveKeyManager
 */
public class SelectKeyStoreActivity extends Activity
    implements OnClickListener, OnCancelListener, KeyChainAliasCallback {

  private final static String TAG = "SelectKeyStoreActivity";
  private final static int KEYSTORE_INTENT = 1380421;
  private final static int PERMISSIONS_REQUEST_EXTERNAL_STORAGE_BEFORE_FILE_CHOOSER = 1001;

  private int decisionId;
  private int state = Decision.DECISION_INVALID;
  private String param = null;
  private String hostname = null;
  private Integer port = null;

  private AlertDialog decisionDialog;
  private EditText hostnamePortInput;
  private Handler toastHandler;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Initialize widgets
    hostnamePortInput = new EditText(this);
    hostnamePortInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    decisionDialog = new AlertDialog.Builder(this).setTitle(R.string.ikm_select_cert)
        .setMessage(getString(R.string.ikm_client_cert))
        .setView(hostnamePortInput)
        .setPositiveButton(R.string.ikm_decision_file, this)
        .setNeutralButton(R.string.ikm_decision_keychain, this)
        .setNegativeButton(R.string.ikm_decision_abort, this)
        .setOnCancelListener(this)
        .create();
    final Context context = this;
    toastHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(@NonNull Message message) {
        Toast.makeText(context, (String) message.obj, Toast.LENGTH_SHORT).show();
      }
    };
  }

  @Override
  public void onResume() {
    super.onResume();
    // Load data from intent
    Intent i = getIntent();
    decisionId = i.getIntExtra(InteractiveKeyManager.DECISION_INTENT_ID, Decision.DECISION_INVALID);
    String hostnamePort = i.getStringExtra(InteractiveKeyManager.DECISION_INTENT_HOSTNAME_PORT);
    Log.d(TAG, "onResume() with " + i.getExtras() + " decId=" + decisionId + " data=" + i.getData());
    hostnamePortInput.setText(hostnamePort);
    decisionDialog.show();
  }

  @Override
  protected void onPause() {
    Intent i = getIntent();
    i.putExtra(InteractiveKeyManager.DECISION_INTENT_HOSTNAME_PORT, hostnamePortInput.getText().toString());
    decisionDialog.dismiss();
    super.onPause();
  }

  /**
   * Stop the user interaction and send result to invoking InteractiveKeyManager.
   * @param state type of the result as defined in IKMDecision
   * @param param keychain alias respectively keystore filename
   * @param hostname hostname of connection
   * @param port port of connection
   */
  void sendDecision(int state, String param, String hostname, Integer port) {
    Log.d(TAG, "sendDecision(" + state + ", " + param + ", " + hostname + ", " + port + ")");
    decisionDialog.dismiss();
    InteractiveKeyManager.interactResult(decisionId, state, param, hostname, port);
    finish();
  }

  @Override
  public void onClick(DialogInterface dialog, int btnId) {
    if (dialog == decisionDialog) {
      if (btnId == DialogInterface.BUTTON_NEGATIVE) { // Cancel
        sendDecision(Decision.DECISION_ABORT, null, null, null);
      }
      // Parse hostname and port
      hostname = null;
      port = null;
      try {
        String[] parts = hostnamePortInput.getText().toString().split(":");
        if (parts.length > 2) {
          throw new IllegalArgumentException("To many separating colons");
        }
        hostname = parts.length > 0 && !TextUtils.isEmpty(parts[0]) ? parts[0] : null;
        if (parts.length > 1) {
          port = Integer.valueOf(parts[1]);
        }
      } catch (IllegalArgumentException e) {
        // hostname:port invalid; show toast and ignore button click
        Log.e(TAG, "onClick: could not parse hostname " + hostnamePortInput.getText());
        toastHandler.obtainMessage(0, getString(R.string.ikm_store_aliasMapping)).sendToTarget();
        return;
      }
      // User selected how to provide key
      switch (btnId) {
        case DialogInterface.BUTTON_POSITIVE: // keystore file
          // Check permission to read external storage and request it/simulate successful request
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
              Log.d(TAG, "Requesting permission READ_EXTERNAL_STORAGE");
              requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                  PERMISSIONS_REQUEST_EXTERNAL_STORAGE_BEFORE_FILE_CHOOSER);
            } else {
              Log.d(TAG, "Verified permission READ_EXTERNAL_STORAGE");
              /* Permission callback invokes file chooser */
              onRequestPermissionsResult(PERMISSIONS_REQUEST_EXTERNAL_STORAGE_BEFORE_FILE_CHOOSER,
                  new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                  new int[]{PackageManager.PERMISSION_GRANTED});
            }
          }
          break;
        case DialogInterface.BUTTON_NEUTRAL: // keychain alias
          KeyChain.choosePrivateKeyAlias(this, this, null, null, null, -1, null);
          break;
      }
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    sendDecision(Decision.DECISION_ABORT, null, null, null);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // Handle result of request for permission to read external storage
    if (requestCode == PERMISSIONS_REQUEST_EXTERNAL_STORAGE_BEFORE_FILE_CHOOSER) {
      for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
        if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
          // Read external storage permission granted
          Log.d(TAG, "onRequestPermissionsResult(): Permission READ_EXTERNAL_STORAGE was granted.");
          // Start file chooser to select keystore
          Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
          intent.setType("file/*");
          intent.addCategory(Intent.CATEGORY_OPENABLE);
          startActivityForResult(Intent.createChooser(intent, this.getString(R.string.ikm_select_keystore)), KEYSTORE_INTENT);
          return;
        }
      }
      // Permission denied
      Log.w(TAG, "onRequestPermissionsResult(): Permission READ_EXTERNAL_STORAGE was denied.");
      sendDecision(Decision.DECISION_ABORT, null, null, null);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Handle result of keystore file chooser
    if (requestCode == KEYSTORE_INTENT) {
      if (resultCode == Activity.RESULT_OK) {
        if (data.getData() == null) {
          Log.w(TAG, "Keystore file chooser returned with OK, but file was null.");
          sendDecision(Decision.DECISION_ABORT, null, null, null);
        } else {
          state = Decision.DECISION_FILE;
          param = data.getData().getPath();
          sendDecision(state, param, hostname, port);
        }
      } else {
        sendDecision(Decision.DECISION_ABORT, null, null, null);
      }
    }
  }

  @Override
  public void alias(final String alias) {
    // Handle result of keychain alias chooser
    Log.d(TAG, "alias(" + alias + ")");
    if (alias != null) {
      state = Decision.DECISION_KEYCHAIN;
      param = alias;
      sendDecision(state, param, hostname, port);
    } else {
      sendDecision(Decision.DECISION_ABORT, null, null, null);
    }
  }
}