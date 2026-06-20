package com.dor.happyprayer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

public final class PrayerReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "happy_prayer_daily";

    @Override
    public void onReceive(Context context, Intent intent) {
        createChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                1000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent popupIntent = new Intent(context, ReminderPopupActivity.class);
        popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent popupPendingIntent = PendingIntent.getActivity(
                context,
                3000 + intent.getIntExtra("slot_id", 0),
                popupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String message = ReminderScheduler.getMessage(context);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("שכולם יהיו מאושרים ושמחים")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_dialog_info, "פתח", popupPendingIntent)
                .setAutoCancel(true)
                .setSound(soundUri(context));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        if (ReminderScheduler.isPopupEnabled(context)) {
            builder.setFullScreenIntent(popupPendingIntent, true);
        }

        if (Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(2000 + intent.getIntExtra("slot_id", 0), builder.build());
            }
        }

        if (ReminderScheduler.isPopupEnabled(context)) {
            try {
                context.startActivity(popupIntent);
            } catch (RuntimeException ignored) {
                // Some Android versions only allow the full-screen notification path from the background.
            }
        }

        int slotId = intent.getIntExtra("slot_id", 0);
        for (ReminderSlot slot : ReminderScheduler.SLOTS) {
            if (slot.id == slotId && ReminderScheduler.isEnabled(context, slot)) {
                ReminderScheduler.schedule(context, slot);
                break;
            }
        }
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "תזכורות תפילה",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("תזכורות יומיות שכולם יהיו מאושרים ושמחים");
        channel.setSound(soundUri(context), attributes);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    private static Uri soundUri(Context context) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.gentle_bell);
    }
}
