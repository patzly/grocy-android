package xyz.zedler.patrick.grocy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class RestartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.exit(0);
    }

    public static void restartApp(Activity activity) {
        activity.startActivity(new Intent(activity.getApplicationContext(), MainActivity.class));
    }
}
