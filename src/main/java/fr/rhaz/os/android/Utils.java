package fr.rhaz.os.android;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

public class Utils {

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (!info.service.getClassName().equals(serviceClass.getName())) continue;
            return true;
        }

        return false;
    }

    public static void openFolder(Context context, File path) {
        Uri uri = Uri.fromFile(path);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Please install a file manager", Toast.LENGTH_LONG).show();
        }
    }

    public static String toString(List<String> list) {
        String str = "";
        for (String line : list) {
            str = str + line + "\n";
        }
        return str;
    }

    public static String[] toStringArray(List<String> list) {
        return (String[]) list.toArray(new String[list.size()]);
    }

    public static Configuration loadConfig(Context context) {
        try {
            File folder = new File(Environment.getExternalStorageDirectory(), "RHazOS");
            if (!folder.exists()) {
                folder.mkdir();
            }
            File file = new File(folder, "config.yml");
            if (!file.exists()) {
                copyInputStreamToFile(context.getResources().openRawResource(R.raw.config), file);
            }
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            Log.e("RHazOS", Log.getStackTraceString(e));
            return null;
        }
    }

    public static String getHTML(String link) throws Exception {
        StringBuilder content = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(link).openConnection().getInputStream()));
        while (true) {
            String line = bufferedReader.readLine();
            if (line != null) {
                content.append(line + "\n");
            } else {
                bufferedReader.close();
                return content.toString();
            }
        }
    }

    private static void copyInputStreamToFile(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            while (true) {
                int len = in.read(buf);
                if (len > 0) {
                    out.write(buf, 0, len);
                } else {
                    out.close();
                    in.close();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
