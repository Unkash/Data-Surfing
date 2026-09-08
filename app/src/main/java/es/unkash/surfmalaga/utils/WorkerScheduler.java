package es.unkash.surfmalaga.utils;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import es.unkash.surfmalaga.notifications.SurfCheckWorker;

public class WorkerScheduler {

    public static void schedule(Context ctx, int intervalHours) {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(SurfCheckWorker.TAG);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SurfCheckWorker.class, intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(SurfCheckWorker.TAG)
                .build();

        WorkManager.getInstance(ctx).enqueue(request);
    }

    public static void cancel(Context ctx) {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(SurfCheckWorker.TAG);
    }
}
