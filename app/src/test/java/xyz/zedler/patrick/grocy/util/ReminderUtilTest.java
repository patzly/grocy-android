package xyz.zedler.patrick.grocy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.preference.PreferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.notification.BootReceiver;
import xyz.zedler.patrick.grocy.notification.ChoresNotificationReceiver;
import xyz.zedler.patrick.grocy.notification.StockNotificationReceiver;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ReminderUtilTest {
  private Application application;
  private ReminderUtil reminders;
  private ShadowAlarmManager alarms;

  @Before
  public void setUp() {
    application = RuntimeEnvironment.getApplication();
    PreferenceManager.getDefaultSharedPreferences(application).edit().clear().commit();
    reminders = new ReminderUtil(application);
    alarms = shadowOf((AlarmManager) application.getSystemService(Context.ALARM_SERVICE));
  }

  @Test
  public void disablingChoresKeepsStockAndBootRecovery() {
    reminders.setReminderEnabled(ReminderUtil.STOCK_TYPE, true);
    reminders.setReminderEnabled(ReminderUtil.CHORES_TYPE, true);
    reminders.setReminderEnabled(ReminderUtil.CHORES_TYPE, false);
    assertEquals(1, alarms.getScheduledAlarms().size());
    assertAlarm(alarms.getScheduledAlarms().get(0), StockNotificationReceiver.class,
        NOTIFICATIONS.STOCK_ID);
    assertBootEnabled(true);
  }

  @Test
  public void disablingStockKeepsChoresAndBootRecovery() {
    reminders.setReminderEnabled(ReminderUtil.STOCK_TYPE, true);
    reminders.setReminderEnabled(ReminderUtil.CHORES_TYPE, true);
    reminders.setReminderEnabled(ReminderUtil.STOCK_TYPE, false);
    assertEquals(1, alarms.getScheduledAlarms().size());
    assertAlarm(alarms.getScheduledAlarms().get(0), ChoresNotificationReceiver.class,
        NOTIFICATIONS.CHORES_ID);
    assertBootEnabled(true);
    reminders.setReminderEnabled(ReminderUtil.CHORES_TYPE, false);
    assertTrue(alarms.getScheduledAlarms().isEmpty());
    assertBootEnabled(false);
  }

  @Test
  public void bootReschedulesStockWhenChoresAreDisabled() {
    PreferenceManager.getDefaultSharedPreferences(application).edit()
        .putBoolean(NOTIFICATIONS.STOCK_ENABLE, true)
        .putBoolean(NOTIFICATIONS.CHORES_ENABLE, false).commit();
    reminders.rescheduleReminders();
    assertEquals(1, alarms.getScheduledAlarms().size());
    assertAlarm(alarms.getScheduledAlarms().get(0), StockNotificationReceiver.class,
        NOTIFICATIONS.STOCK_ID);
    assertBootEnabled(true);
  }

  @Test
  public void enablingChoresReplacesAlarmFromOlderVersions() {
    addLegacyChoresAlarm();
    reminders.setReminderEnabled(ReminderUtil.CHORES_TYPE, true);
    assertEquals(1, alarms.getScheduledAlarms().size());
    assertAlarm(alarms.getScheduledAlarms().get(0), ChoresNotificationReceiver.class,
        NOTIFICATIONS.CHORES_ID);
  }

  @Test
  public void disablingChoresRemovesLegacyAlarmAndKeepsStockAlarm() {
    reminders.setReminderEnabled(ReminderUtil.STOCK_TYPE, true);
    addLegacyChoresAlarm();
    reminders.setReminderEnabled(ReminderUtil.CHORES_TYPE, false);
    assertEquals(1, alarms.getScheduledAlarms().size());
    assertAlarm(alarms.getScheduledAlarms().get(0), StockNotificationReceiver.class,
        NOTIFICATIONS.STOCK_ID);
  }

  private void addLegacyChoresAlarm() {
    AlarmManager manager = (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
    manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3_600_000,
        PendingIntent.getBroadcast(application, NOTIFICATIONS.CHORES_ID,
            new Intent(application, StockNotificationReceiver.class), PendingIntent.FLAG_IMMUTABLE));
  }

  @Test
  public void nullTimeUsesValidDefault() {
    reminders.scheduleReminder(ReminderUtil.CHORES_TYPE, NOTIFICATIONS.CHORES_ID, null,
        ChoresNotificationReceiver.class);
    assertEquals(1, alarms.getScheduledAlarms().size());
    assertTrue(alarms.getScheduledAlarms().get(0).triggerAtTime >= System.currentTimeMillis());
  }

  private void assertAlarm(ShadowAlarmManager.ScheduledAlarm alarm, Class<?> receiver, int id) {
    assertEquals(receiver.getName(), shadowOf(alarm.operation).getSavedIntent()
        .getComponent().getClassName());
    assertEquals(id, shadowOf(alarm.operation).getRequestCode());
  }

  private void assertBootEnabled(boolean enabled) {
    assertEquals(enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        application.getPackageManager().getComponentEnabledSetting(
            new ComponentName(application, BootReceiver.class)));
  }
}
