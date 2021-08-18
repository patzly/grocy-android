package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class UnlockUtil {

  public static boolean isKeyInstalled(Context context) {
    try {
      context.getPackageManager().getPackageInfo(
          "xyz.zedler.patrick.grocy.unlock", PackageManager.GET_ACTIVITIES
      );
      return true;
    } catch (NameNotFoundException ignored) {
    }
    return false;
  }
}
