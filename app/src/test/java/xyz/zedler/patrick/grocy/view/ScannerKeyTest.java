package xyz.zedler.patrick.grocy.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;
import androidx.appcompat.view.ContextThemeWrapper;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ScannerKeyTest {
  @Test
  public void enterIsConsumedBeforeAutocompleteAndDispatchesExactlyOnceOnRelease() {
    CustomAutoCompleteTextView view = new CustomAutoCompleteTextView(new ContextThemeWrapper(
        RuntimeEnvironment.getApplication(), com.google.android.material.R.style.Theme_Material3_DayNight), null);
    view.setText("1234567890123");
    AtomicInteger calls = new AtomicInteger();
    view.setOnEnterPressListener(calls::incrementAndGet);
    assertTrue(view.onKeyDown(KeyEvent.KEYCODE_ENTER,
        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
    assertEquals(0, calls.get());
    assertEquals("1234567890123", view.getText().toString());
    assertTrue(view.onKeyUp(KeyEvent.KEYCODE_ENTER,
        new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)));
    assertEquals(1, calls.get());
  }
}
