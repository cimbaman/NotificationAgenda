package rs.cimba.notificationagenda;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;


import android.provider.CalendarContract;
import android.database.Cursor;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final int REQUEST_CALENDAR_PERMISSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

//        createNotificationChannel();

        showNotificationAgenda();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(
                        AgendaWorker.class,
                        15, TimeUnit.MINUTES
                ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "agenda_refresh",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );


    }

    private void checkAndRequestPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
                return; // wait until permission is handled
            }
        }

        // Calendar permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR},
                    REQUEST_CALENDAR_PERMISSION);
        }
    }

//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    "agenda_channel",
//                    "Notification Agenda",
//                    NotificationManager.IMPORTANCE_LOW
//            );
//            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//
//            NotificationManager manager =
//                    getSystemService(NotificationManager.class);
//            manager.createNotificationChannel(channel);
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted → continue
                checkAndRequestPermissions(); // check calendar next
            } else {
                // Denied → ask again politely
                showPermissionDialog("Notification");
            }
        }

        if (requestCode == REQUEST_CALENDAR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted → continue
            } else {
                // Denied → ask again politely
                showPermissionDialog("Calendar");
            }
        }
    }

    private void showPermissionDialog(String permissionName) {
        new AlertDialog.Builder(this)
                .setTitle(permissionName + " Permission Needed")
                .setMessage("This app needs " + permissionName + " permission to work properly.")
                .setCancelable(true)
                .setPositiveButton("Grant", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("Exit", (dialog, which) -> finishAffinity())
                .show();
    }

    private void showNotificationAgenda() {
        AgendaHelper.updateNotificationAgenda(this);
    }


}