package fr.rhaz.os.android.rss;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ListListener implements AdapterView.OnItemClickListener {

    ProgressDialog mProgressDialog;
    List<PluginRSS> plugins;
    Activity activity;

    public ListListener(List<PluginRSS> plugins, Activity activity){
        this.plugins = plugins;
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final PluginRSS plugin = plugins.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        TextView textView = new TextView(activity);
        textView.setPadding(10, 10, 10, 10);
        textView.setLinksClickable(true);
        textView.setAutoLinkMask(Linkify.ALL);
        textView.setText(Html.fromHtml(plugin.getDescription()));
        builder.setView(textView);
        builder.setTitle(plugin.getTitle());
        builder.setPositiveButton("Download", (dialog, id12) -> {
            final DownloadTask downloadTask = new DownloadTask(activity);
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage("Downloading...");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(dialog1 -> downloadTask.cancel(true));
            downloadTask.execute(plugin.getLink());
        });
        builder.setNegativeButton("Cancel", (dialog, id1) -> {
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.e("Download", result);
            } else
                Toast.makeText(context,"File downloaded", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                File d = new File(Environment.getExternalStorageDirectory(), "RHazOS/plugins");
                if(!d.exists()) d.mkdir();
                File f = new File(d, sUrl[0].substring(sUrl[0].lastIndexOf('/')+1));
                if(!f.exists()) f.createNewFile();
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(f);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
    }
}
