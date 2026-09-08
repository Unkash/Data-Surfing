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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import es.unkash.surfmalaga.R;
import es.unkash.surfmalaga.data.SpotRepository;
import es.unkash.surfmalaga.data.SpotStorage;
import es.unkash.surfmalaga.data.SurfSpot;
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
        Context ctx = getApplicationContext();

        if (!SpotStorage.isGlobalEnabled(ctx)) return Result.success();

        SpotRepository repo = SpotRepository.getInstance();
        List<SurfSpot> spots = repo.getSpots();
        SpotStorage.loadAll(ctx, spots);

        // Solo spots con alerta activada
        List<SurfSpot> activeSpots = new java.util.ArrayList<>();
        for (SurfSpot s : spots) {
            if (s.alertEnabled) activeSpots.add(s);
        }
        if (activeSpots.isEmpty()) return Result.success();

        // Cargar datos de cada spot en paralelo
        CountDownLatch latch = new CountDownLatch(activeSpots.size());

        for (SurfSpot spot : activeSpots) {
            repo.fetchSpotData(spot, new SpotRepository.Callback() {
                @Override public void onSuccess(es.unkash.surfmalaga.data.SurfData data) {
                    latch.countDown();
                }
                @Override public void onError(String message) {
                    latch.countDown();
                }
            });
        }

        try { latch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        // Evaluar y notificar
        StringBuilder sb = new StringBuilder();
        int notifId = 100;
        for (SurfSpot spot : activeSpots) {
            String msg = spot.evaluateAlert();
            if (msg != null) {
                sendNotification(ctx, notifId++, spot.name, msg);
            }
        }

        return Result.success();
    }

    private void sendNotification(Context ctx, int id, String spotName, String message) {
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Alertas de surf", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);

        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wave)
                .setContentTitle("🏄 " + spotName)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(id, builder.build());
    }
}
