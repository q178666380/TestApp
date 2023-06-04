package cc.android.testapp.util;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.Random;

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

    public static String KEY_HIGH_LIGHT = "FIELD_BOOLEAN_TEXTVIEW_HIGHLIGHT";
    public static String KEY_IGNORE = "IGNORE_HOOK";
    public static String KEY_ENABLE_TEXTEDIT = "KEY_ENABLE_TEXTEDIT";
    public static String KEY_LOCK_TEXTEDIT = "KEY_LOCK_TEXTEDIT";
    public static String KEY_VIEW_INFO = "KEY_VIEW_INFO";


    private static final File share_config_file = new File(Environment.getExternalStorageDirectory(),
            "Download/config/" + Config.MODULE_PACKAGE_NAME + "/config.prop");

    private static final Properties CONFIG_PROP = new Properties();
    private static long read_time = -1;

    public static File getConfigFile() {
        return share_config_file;
    }

    private static final Random mRan = new Random();

    public static Properties getProps() {
        if (read_time != getConfigFile().lastModified()) {
            FileInputStream tFIStream = null;
            FileLock lock = null;
            try {
                tFIStream = FileUtil.openInputStream(share_config_file);
                lock = tFIStream.getChannel().lock(0, Long.MAX_VALUE, true);
                CONFIG_PROP.load(tFIStream);
                read_time = getConfigFile().lastModified();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtil.closeStream(lock, tFIStream);
            }
        }

        return CONFIG_PROP;
    }

    public static void saveProps() {
        FileOutputStream tFOStream = null;
        FileLock lock = null;
        try {
            tFOStream = FileUtil.openOutputStream(getConfigFile(), false);
            lock = tFOStream.getChannel().lock();
            CONFIG_PROP.store(tFOStream, "");
            read_time = getConfigFile().lastModified();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeStream(lock, tFOStream);
        }
    }

    public static String stringValue(String pKey, String pDef) {
        String tStr = getProps().getProperty(pKey);
        return tStr == null ? pDef : tStr;
    }

    public static boolean boolValue(String pKey) {
        String tStr = getProps().getProperty(pKey);
        return tStr != null && Boolean.parseBoolean(tStr);
    }

    public static long longValue(String pKey) {
        try {
            String tStr = getProps().getProperty(pKey);
            return tStr == null ? 0L : Long.parseLong(tStr);
        } catch (NumberFormatException ignore) {
            return 0L;
        }
    }

    public static double doubleValue(String pKey) {
        try {
            String tStr = getProps().getProperty(pKey);
            return tStr == null ? 0D : Double.parseDouble(tStr);
        } catch (NumberFormatException ignore) {
            return 0D;
        }
    }

    public static <T> void setProp(String pKey, T pValue, boolean pSave) {
        if (pValue == null) {
            getProps().remove(pKey);
        } else {
            getProps().setProperty(pKey, pValue.toString());
        }
        if (pSave) saveProps();
    }

    public static double shakeNumber(double pValue, int pRange) {
        return pValue + (mRan.nextDouble() - 0.5) * 2 / (100000.0 / pRange);
    }
}
