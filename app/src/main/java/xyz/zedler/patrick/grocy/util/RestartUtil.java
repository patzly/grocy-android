package xyz.zedler.patrick.grocy.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import xyz.zedler.patrick.grocy.MainActivity;

public class RestartUtil {

    public static void restartApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
        if(context instanceof Activity) {
            ((Activity) context).finish();
        }
        Runtime.getRuntime().exit(0);
    }
}
