package xyz.zedler.patrick.grocy.util;

import android.os.SystemClock;

public class ClickUtil {

    private long idle = 500;
    private long lastClick;

    public ClickUtil() {
        lastClick = 0;
    }

    public ClickUtil(long idle) {
        lastClick = 0;
        this.idle = idle;
    }

    public void update() {
        lastClick = SystemClock.elapsedRealtime();
    }

    public boolean isDisabled() {
        if(SystemClock.elapsedRealtime() - lastClick < idle) return true;
        update();
        return false;
    }
}
