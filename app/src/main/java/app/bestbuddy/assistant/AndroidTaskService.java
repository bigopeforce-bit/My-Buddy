package app.bestbuddy.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/**
 * User-visible execution anchor for active BestBuddy runs. This does not grant screen access:
 * phone interaction still requires the user-enabled Accessibility Service.
 */
public final class AndroidTaskService extends Service {
    private static final String CHANNEL = "task_runtime";
    private static final int NOTIFICATION_ID = 31;
    private static final String ACTION_START = "app.bestbuddy.assistant.TASK_START";
    private static final String ACTION_STOP = "app.bestbuddy.assistant.TASK_STOP";

    static void start(Context context) {
        Intent intent = new Intent(context, AndroidTaskService.class).setAction(ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (IllegalStateException | SecurityException ignored) {
            // Android may reject a background FGS start. The in-app task and existing
            // accessibility runtime remain available; never attempt to bypass the OS.
        }
    }

    static void stop(Context context) {
        try {
            context.stopService(new Intent(context, AndroidTaskService.class));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "BestBuddy task runtime", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows when BestBuddy is keeping an active task running.");
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            // Stop every active chat through the same controller used by the in-app Stop control.
            // Keep this foreground anchor alive until the agent reports each run has ended.
            ChatHub.get(this).stopAll();
            return START_NOT_STICKY;
        }
        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 31, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop = new Intent(this, AndroidTaskService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 32, stop,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("BestBuddy is working")
                .setContentText("An AI task is active. Tap to return, or use Stop to end active tasks.")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pending)
                .addAction(new Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_launcher_monochrome),
                        "Stop", stopPending).build())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (SecurityException | IllegalStateException e) {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
