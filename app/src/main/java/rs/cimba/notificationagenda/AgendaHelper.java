package rs.cimba.notificationagenda;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AgendaHelper {

    private static final String CHANNEL_ID = "agenda_channel";
    private static final int NOTIFICATION_ID = 1;

    private static int showAllDay = 1;
    public static void updateNotificationAgenda(Context context) {
        List<String> events = getTodaysEvents(context);
        if (events.isEmpty()) {
            events.add("No events today!");
        }

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notification Agenda",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Today's Agenda");
        for (String event : events) {
            inboxStyle.addLine(event);
        }

        String nowTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(nowTime + " Today's Agenda")
                .setContentText(events.get(0))
                .setStyle(inboxStyle)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private static List<String> getTodaysEvents(Context context) {
        List<String> events = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            return events;
        }

        long now = System.currentTimeMillis();
        long startOfDay = getStartOfDayMillis();
        long endOfDay = getEndOfDayMillis();

        // Query Instances table
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startOfDay);
        ContentUris.appendId(builder, endOfDay);

        String[] projection = new String[]{
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY
        };

        Cursor cursor = context.getContentResolver().query(
                builder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE));
                long startMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
                long endMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END));
                int allDay = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY));

                // Skip finished events
                if (endMillis < now) continue;


                if (allDay == 1 && showAllDay != 1) continue;

                Calendar calStart = Calendar.getInstance();
                calStart.setTimeInMillis(startMillis);
                Calendar calEnd = Calendar.getInstance();
                calEnd.setTimeInMillis(endMillis);
                String timeStr;
                if (allDay == 1) {
                    timeStr = "";
                } else {
                    timeStr = String.format("%02d:%02d - %02d:%02d", calStart.get(Calendar.HOUR_OF_DAY), calStart.get(Calendar.MINUTE), calEnd.get(Calendar.HOUR_OF_DAY), calEnd.get(Calendar.MINUTE));
                }

                String eventStr = timeStr + " " + title;
                events.add(eventStr);

                Log.d("CalendarDebug", "Event: " + eventStr + " | startMillis=" + startMillis);
            }
            cursor.close();
        }

        Log.d("CalendarDebug", "Total events today: " + events.size());
        return events;
    }

    private static long getStartOfDayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static long getEndOfDayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}
