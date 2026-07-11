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
import android.provider.Settings;

public final class PrayerReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID_PREFIX = "happy_prayer_daily_v5_";

    @Override
    public void onReceive(Context context, Intent intent) {
        int slotId = intent.getIntExtra("slot_id", 0);
        boolean explicitTest = intent.getBooleanExtra("explicit_test", false);
        if (!explicitTest && ReminderScheduler.isAirplanePauseEnabled(context) && isAirplaneModeOn(context)) {
            ReminderSlot pausedSlot = ReminderScheduler.getSlot(context, slotId);
            if (pausedSlot != null && ReminderScheduler.isEnabled(context, pausedSlot)) {
                ReminderScheduler.schedule(context, pausedSlot);
            }
            return;
        }
        int soundMode = ReminderScheduler.getSoundMode(context);
        String channelId = channelId(soundMode, ReminderScheduler.isPopupEnabled(context));
        createChannel(context, soundMode, channelId);

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
        popupIntent.putExtra("slot_id", slotId);
        popupIntent.putExtra("is_test", explicitTest);
        PendingIntent popupPendingIntent = PendingIntent.getActivity(
                context,
                3000 + slotId,
                popupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String message = ReminderScheduler.getMessage(context, slotId);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, channelId);
        } else {
            builder = new Notification.Builder(context);
        }
        builder.setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("שכולם יהיו מאושרים ושמחים")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_dialog_info, "פתח", popupPendingIntent)
                .setAutoCancel(true);
        // When the immersive popup is active it owns the audio. Playing the old notification
        // sample at the same time caused the harsh, doubled sound users could hear.
        if (!ReminderScheduler.isPopupEnabled(context)) {
            builder.setSound(soundUri(context, soundMode));
        } else {
            builder.setSound(null);
        }
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
                manager.notify(2000 + slotId, builder.build());
            }
        }

        if (ReminderScheduler.isPopupEnabled(context)) {
            try {
                context.startActivity(popupIntent);
            } catch (RuntimeException ignored) {
                // Some Android versions only allow the full-screen notification path from the background.
            }
        }

        ReminderSlot slot = ReminderScheduler.getSlot(context, slotId);
        if (slot != null && ReminderScheduler.isEnabled(context, slot)) {
            ReminderScheduler.schedule(context, slot);
        }
    }

    private static void createChannel(Context context, int soundMode, String channelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(channelId) != null) return;

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel channel = new NotificationChannel(
                channelId,
                "תזכורות תפילה - " + ReminderScheduler.SOUND_LABELS[soundMode],
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("תזכורות יומיות שכולם יהיו מאושרים ושמחים");
        if (ReminderScheduler.isPopupEnabled(context)) {
            channel.setSound(null, null);
        } else {
            channel.setSound(soundUri(context, soundMode), attributes);
        }
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    private static String channelId(int soundMode, boolean popupEnabled) {
        return CHANNEL_ID_PREFIX + soundMode + (popupEnabled ? "_immersive" : "_notification");
    }

    private static Uri soundUri(Context context, int soundMode) {
        int soundResource = R.raw.gentle_bell;
        if (soundMode == ReminderScheduler.SOUND_CALM_PAD) {
            soundResource = R.raw.calm_pad;
        } else if (soundMode == ReminderScheduler.SOUND_SOFT_CHIMES) {
            soundResource = R.raw.soft_chimes;
        } else if (soundMode == ReminderScheduler.SOUND_VOICE) {
            soundResource = R.raw.default_voice;
        } else if (soundMode == ReminderScheduler.SOUND_MANTRA) {
            soundResource = R.raw.mantra_voice;
        }
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundResource);
    }

    private static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
}
