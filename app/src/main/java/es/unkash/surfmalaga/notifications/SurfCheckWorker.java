package es.unkash.surfmalaga.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import es.unkash.surfmalaga.R;
import es.unkash.surfmalaga.data.AlertConfig;
import es.unkash.surfmalaga.data.AlertStorage;
import es.unkash.surfmalaga.data.OpenMeteoRepository;
import es.unkash.surfmalaga.data.SurfData;
import es.unkash.surfmalaga.ui.MainActivity;

public class SurfCheckWorker extends Worker {

    public static final String CHANNEL_ID = "surf_alerts";
    public static final String TAG = "SurfCheckWorker";

    public SurfCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AlertConfig config = AlertStorage.load(getApplicationContext());
        if (!config.enabled) return Result.success();

        // Llamada síncrona (estamos en hilo background de WorkManager)
        final String[] alertMessage = {null};
        final boolean[] done = {false};

        new OpenMeteoRepository().fetchData(new OpenMeteoRepository.Callback() {
            @Override
            public void onSuccess(SurfData data) {
                alertMessage[0] = config.evaluate(data);
                done[0] = true;
            }
            @Override
            public void onError(String message) {
                done[0] = true;
            }
        });

        // Esperar máx 15 segundos
        long start = System.currentTimeMillis();
        while (!done[0] && System.currentTimeMillis() - start < 15000) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        if (alertMessage[0] != null) {
            sendNotification(alertMessage[0]);
        }

        return Result.success();
    }

    private void sendNotification(String message) {
        Context ctx = getApplicationContext();
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal (Android 8+)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alertas de surf",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificaciones de condiciones de surf");
        nm.createNotificationChannel(channel);

        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wave)
                .setContentTitle("🏄 Surf Málaga")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(1, builder.build());
    }
}
