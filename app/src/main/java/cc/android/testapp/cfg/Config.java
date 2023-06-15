package cc.android.testapp.cfg;

import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import cc.android.testapp.util.CLog;
import cc.commons.util.FileUtil;
import cc.commons.util.IOUtil;

public class Config {
    public static String MODULE_PACKAGE_NAME = "cc.android.testapp";
    public static String KEY_LOCATION_JD = "KEY_LOCATION_JD";
    public static String KEY_LOCATION_WD = "KEY_LOCATION_WD";
    public static String KEY_ADDRESS = "KEY_ADDRESS";
    public static String KEY_DEVICE_ID = "KEY_DEVICE_ID";
    public static String KEY_ENABLE_LOCATION_CHANGE = "KEY_ENABLE_LOCATION_CHANGE";

    public static String KEY_CHECK_CLASS = "KEY_CHECK_CLASS";
    public static String KEY_CHECK_METHOD = "KEY_CHECK_METHOD";
    public static String KEY_ENABLE_FILELOG = "KEY_ENABLE_FILELOG";


    private static final File share_config_file = new File(Environment.getExternalStorageDirectory(),
            "Download/config/" + Config.MODULE_PACKAGE_NAME + "/config.prop");

    public static JSONObject config = new JSONObject();
    private static long read_time = -1;


    /** 以下配置为客户端配置 */
    /**
     * hook的app端,设置不允许保存
     */
    public static boolean AllowSave = true;

    public static File getConfigFile() {
        return share_config_file;
    }

    private static final Random mRan = new Random();

    public static void updateConfig() {
        synchronized (Config.class) {
            if (read_time != getConfigFile().lastModified()) {
                FileInputStream tFIStream = null;
                FileLock lock = null;
                try {
                    tFIStream = FileUtil.openInputStream(share_config_file);
                    lock = tFIStream.getChannel().lock(0, Long.MAX_VALUE, true);
                    config = new JSONObject(IOUtil.readContent(tFIStream, "UTF-8"));
                    read_time = getConfigFile().lastModified();
                } catch (IOException | JSONException e) {
                    String tStr = e.getLocalizedMessage();
                    Log.e(CLog.TAG, "Error on read config: " + tStr);
                } finally {
                    IOUtil.closeStream(lock, tFIStream);
                }

            }
        }
    }

    public static JSONObject getConfig() {
        return config;
    }

    public static void setClientConfig(String pJson) throws JSONException {
        config = new JSONObject(pJson);
    }

    public static void saveConfig() {
        if (!AllowSave) return;
        synchronized (Config.class) {

            FileOutputStream tFOStream = null;
            FileLock lock = null;
            try {
                tFOStream = FileUtil.openOutputStream(getConfigFile(), false);
                lock = tFOStream.getChannel().lock();
                tFOStream.write(config.toString(4).getBytes(StandardCharsets.UTF_8));
                read_time = getConfigFile().lastModified();
            } catch (IOException | JSONException e) {
                Log.e(CLog.TAG, "Error on save config: " + e.getLocalizedMessage());
            } finally {
                IOUtil.closeStream(lock, tFOStream);
            }
        }
    }

    public static double shakeNumber(double pValue, int pRange) {
        return pValue + (mRan.nextDouble() - 0.5) * 2 / (100000.0 / pRange);
    }

    public static void setProp(String pKey, Object pValue, boolean pSave) {
        try {
            config.put(pKey, pValue);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if (pSave) saveConfig();
    }

    public static boolean enableFilelog() {
        return config.optBoolean(KEY_ENABLE_FILELOG, false);
    }

}
