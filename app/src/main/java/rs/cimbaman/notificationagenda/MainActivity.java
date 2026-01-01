package rs.cimbaman.notificationagenda;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Build;


import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;



import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final int REQUEST_CALENDAR_PERMISSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        fullRefresh();

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

        Button refreshBtn = findViewById(R.id.btn_refresh);

        refreshBtn.setOnClickListener(v -> {
            fullRefresh();

            Toast.makeText(this, "Dashboard Refreshed", Toast.LENGTH_SHORT).show();
        });

        TextView calText = findViewById(R.id.status_calendar);
        TextView notifyText = findViewById(R.id.status_notification);

        calText.setOnClickListener(v -> {
            checkAndRequestPermissions();
            fullRefresh();
        });

        notifyText.setOnClickListener(v -> {
            checkAndRequestPermissions();
            fullRefresh();
        });

        SwitchCompat allDaySwitch = findViewById(R.id.switch_all_day);

        allDaySwitch.setChecked(AgendaHelper.showAllDay == 1);

        allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) AgendaHelper.showAllDay = 1;
            else AgendaHelper.showAllDay = 0;

            fullRefresh();
        });


        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            TextView versionText = findViewById(R.id.version_display); // Add this ID to a TextView in XML
            versionText.setText("Version: " + version);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void checkAndRequestPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
                return;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // SUCCESS: Permission granted, move to the next step
            if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
                checkAndRequestPermissions(); // Now check for Calendar
            }
        } else {
            String name = (requestCode == REQUEST_NOTIFICATION_PERMISSION) ? "Notification" : "Calendar";
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                showPermissionDialog(name);
            } else {
                // User just clicked deny once - you can try requesting again or show a simpler toast
                Toast.makeText(this, name + " permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPermissionDialog(String permissionName) {
        new AlertDialog.Builder(this)
                .setTitle(permissionName + " Permission Needed")
                .setMessage("This app needs " + permissionName + " permission to work properly.")
                .setCancelable(false)
                .setPositiveButton("Settings", (dialog, which) -> {

                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Exit", (dialog, which) -> finishAffinity())
                .show();
    }

    private void showNotificationAgenda() {
        AgendaHelper.updateNotificationAgenda(this);
    }
    private void refreshDashboardUI() {

        TextView previewContent = findViewById(R.id.preview_content);
        previewContent.setText(AgendaHelper.nearestEvent);

        TextView fullList = findViewById(R.id.full_agenda_list);
        StringBuilder sb = new StringBuilder();
        for (String s : AgendaHelper.events) sb.append("• ").append(s).append("\n");
        fullList.setText(sb.length() > 0 ? sb.toString().trim() : "No events today!");

        TextView notifyStatus = findViewById(R.id.status_notification);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notifyStatus.setText("● Notification Access: Granted");
            notifyStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            notifyStatus.setText("● Notification Access: Required");
            notifyStatus.setTextColor(Color.parseColor("#F44336")); // Red
        }

        TextView calStatus = findViewById(R.id.status_calendar);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED) {
            calStatus.setText("● Calendar Access: Granted");
            calStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            calStatus.setText("● Calendar Access: Required");
            calStatus.setTextColor(Color.parseColor("#F44336")); // Red
        }
    }

     private void fullRefresh(){
        showNotificationAgenda();
        refreshDashboardUI();
     }


}