package es.unkash.surfmalaga.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import es.unkash.surfmalaga.data.AlertConfig;
import es.unkash.surfmalaga.data.AlertStorage;
import es.unkash.surfmalaga.utils.WorkerScheduler;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlertConfig config = AlertStorage.load(context);
            if (config.enabled) {
                WorkerScheduler.schedule(context, config.checkInterval);
            }
        }
    }
}
