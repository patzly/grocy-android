package xyz.zedler.patrick.grocy.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBroadcastPendingResult;
import androidx.preference.PreferenceManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.test.TestServer;
import xyz.zedler.patrick.grocy.util.ReminderUtil;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ChoresNotificationReceiverTest {
  @Test
  public void successSchedulesChoresAndNotifiesOnlyWhenDue() throws Exception {
    verifySuccess("[{\"next_estimated_execution_time\":\"2000-01-01 00:00:00\"}]", 1);
  }

  @Test
  public void emptyChoresDoNotNotify() throws Exception {
    verifySuccess("[]", 0);
  }

  @Test
  public void futureChoresDoNotNotify() throws Exception {
    verifySuccess("[{\"next_estimated_execution_time\":\"2999-01-01 00:00:00\"}]", 0);
  }

  @Test
  public void failedChoresDownloadRetriesChoresAndKeepsStockAlarm() throws Exception {
    try (TestServer server = new TestServer(request -> request.respond(403, "{}"))) {
      Application app = configure(server);
      ReminderUtil reminders = new ReminderUtil(app);
      reminders.setReminderEnabled(ReminderUtil.STOCK_TYPE, true);
      long now = System.currentTimeMillis();
      ShadowBroadcastPendingResult result = deliver(app);
      ShadowAlarmManager alarms = shadowOf((AlarmManager) app.getSystemService(Context.ALARM_SERVICE));
      TestServer.await(() -> result.getFuture().isDone());
      TestServer.await(() -> alarms.getScheduledAlarms().stream().anyMatch(alarm ->
          shadowOf(alarm.operation).getRequestCode() == NOTIFICATIONS.CHORES_ID
              && alarm.triggerAtTime >= now + 599_000 && alarm.triggerAtTime <= now + 610_000));
      assertEquals(2, alarms.getScheduledAlarms().size());
      for (ShadowAlarmManager.ScheduledAlarm alarm : alarms.getScheduledAlarms()) {
        String receiver = shadowOf(alarm.operation).getSavedIntent().getComponent().getClassName();
        assertEquals(shadowOf(alarm.operation).getRequestCode() == NOTIFICATIONS.CHORES_ID
            ? ChoresNotificationReceiver.class.getName() : StockNotificationReceiver.class.getName(), receiver);
      }
    }
  }

  private void verifySuccess(String response, int notifications) throws Exception {
    try (TestServer server = new TestServer(request -> request.respond(200, response))) {
      Application app = configure(server);
      ShadowBroadcastPendingResult result = deliver(app);
      TestServer.await(() -> result.getFuture().isDone());
      ShadowAlarmManager alarms = shadowOf((AlarmManager) app.getSystemService(Context.ALARM_SERVICE));
      assertEquals(1, alarms.getScheduledAlarms().size());
      assertEquals(ChoresNotificationReceiver.class.getName(), shadowOf(alarms.getScheduledAlarms()
          .get(0).operation).getSavedIntent().getComponent().getClassName());
      assertEquals(notifications, shadowOf((NotificationManager) app.getSystemService(
          Context.NOTIFICATION_SERVICE)).getAllNotifications().size());
      assertTrue(server.requests.get(0).path.endsWith("/chores"));
    }
  }

  private ShadowBroadcastPendingResult deliver(Application app) {
    ChoresNotificationReceiver receiver = new ChoresNotificationReceiver();
    app.registerReceiver(receiver, new IntentFilter("test.CHORES"), Context.RECEIVER_NOT_EXPORTED);
    app.sendBroadcast(new Intent("test.CHORES"));
    shadowOf(android.os.Looper.getMainLooper()).idle();
    assertTrue(shadowOf(receiver).wentAsync());
    return Shadow.extract(shadowOf(receiver).getOriginalPendingResult());
  }

  private Application configure(TestServer server) {
    Application app = RuntimeEnvironment.getApplication();
    PreferenceManager.getDefaultSharedPreferences(app).edit().clear()
        .putString(Constants.PREF.SERVER_URL, server.url()).commit();
    return app;
  }
}
