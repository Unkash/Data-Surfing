package es.unkash.surfmalaga.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import es.unkash.surfmalaga.data.SpotStorage;
import es.unkash.surfmalaga.utils.WorkerScheduler;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (SpotStorage.isGlobalEnabled(context)) {
                WorkerScheduler.schedule(context, SpotStorage.getCheckInterval(context));
            }
        }
    }
}
