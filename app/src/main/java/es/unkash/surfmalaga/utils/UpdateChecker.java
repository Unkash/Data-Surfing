package es.unkash.surfmalaga.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import es.unkash.surfmalaga.BuildConfig;
import es.unkash.surfmalaga.R;

/**
 * Comprueba si hay una nueva versión disponible en GitHub
 * y gestiona la descarga e instalación en background.
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final String VERSION_JSON_URL =
        "https://raw.githubusercontent.com/Unkash/Data-Surfing/main/version.json";

    public static void check(Activity activity) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(VERSION_JSON_URL).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) return;

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                String remoteVersion = json.getString("version");
                String apkUrl = json.getString("apk_url");
                String currentVersion = BuildConfig.VERSION_NAME;

                Log.d(TAG, "Versión actual: " + currentVersion + " — Remota: " + remoteVersion);

                if (isNewer(remoteVersion, currentVersion)) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        showUpdateDialog(activity, remoteVersion, apkUrl));
                }

            } catch (Exception e) {
                Log.d(TAG, "Sin actualización disponible o sin conexión: " + e.getMessage());
            }
        }).start();
    }

    private static boolean isNewer(String remote, String current) {
        try {
            String[] r = remote.split("\\.");
            String[] c = current.split("\\.");
            int len = Math.max(r.length, c.length);
            for (int i = 0; i < len; i++) {
                int rv = i < r.length ? Integer.parseInt(r[i]) : 0;
                int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
                if (rv > cv) return true;
                if (rv < cv) return false;
            }
        } catch (Exception e) { /* ignorar */ }
        return false;
    }

    private static void showUpdateDialog(Activity activity, String version, String apkUrl) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
            .setTitle("🏄 Nueva versión disponible")
            .setMessage("Surf Málaga v" + version + " está disponible.\n\n¿Descargar e instalar ahora?")
            .setPositiveButton("Descargar", (d, w) -> startDownload(activity, version, apkUrl))
            .setNegativeButton("Ahora no", null)
            .show();
    }

    private static void startDownload(Activity activity, String version, String apkUrl) {
        // Diálogo de progreso
        View progressView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_download_progress, null);
        ProgressBar progressBar = progressView.findViewById(R.id.downloadProgressBar);
        TextView tvStatus = progressView.findViewById(R.id.tvDownloadStatus);

        AlertDialog progressDialog = new AlertDialog.Builder(activity)
                .setTitle("Descargando v" + version)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Descargar con DownloadManager
        String fileName = "SurfMalaga-v" + version + ".apk";
        DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Surf Málaga v" + version)
                .setDescription("Descargando actualización...")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        long downloadId = dm.enqueue(request);

        // Actualizar barra de progreso cada 500ms
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable progressUpdater = new Runnable() {
            @Override public void run() {
                DownloadManager.Query query = new DownloadManager.Query()
                        .setFilterById(downloadId);
                Cursor cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int downloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                    int status = statusCol >= 0 ? cursor.getInt(statusCol) : -1;
                    long downloaded = downloadedCol >= 0 ? cursor.getLong(downloadedCol) : 0;
                    long total = totalCol >= 0 ? cursor.getLong(totalCol) : -1;
                    cursor.close();

                    if (total > 0) {
                        int percent = (int) (downloaded * 100 / total);
                        progressBar.setProgress(percent);
                        tvStatus.setText(String.format("%.1f / %.1f MB",
                                downloaded / 1048576f, total / 1048576f));
                    }

                    if (status == DownloadManager.STATUS_RUNNING ||
                        status == DownloadManager.STATUS_PENDING) {
                        handler.postDelayed(this, 500);
                    }
                }
            }
        };
        handler.post(progressUpdater);

        // Receiver para cuando termine la descarga
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != downloadId) return;

                handler.removeCallbacks(progressUpdater);
                progressDialog.dismiss();
                activity.unregisterReceiver(this);

                // Verificar que descargó bien
                DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
                Cursor cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = statusCol >= 0 ? cursor.getInt(statusCol) : -1;
                    int localUriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    String localUri = localUriCol >= 0 ? cursor.getString(localUriCol) : null;
                    cursor.close();

                    if (status == DownloadManager.STATUS_SUCCESSFUL && localUri != null) {
                        installApk(activity, localUri, fileName);
                    } else {
                        new Handler(Looper.getMainLooper()).post(() ->
                            new AlertDialog.Builder(activity)
                                .setTitle("Error")
                                .setMessage("No se pudo descargar la actualización.")
                                .setPositiveButton("OK", null)
                                .show());
                    }
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private static void installApk(Activity activity, String localUri, String fileName) {
        try {
            File apkFile = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName);

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".provider", apkFile);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            Intent installIntent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(installIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error instalando APK: " + e.getMessage());
        }
    }
}
