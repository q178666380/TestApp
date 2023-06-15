package cc.android.testapp.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import cc.android.testapp.cfg.Config;
import cc.commons.util.FileUtil;
import cc.commons.util.IOUtil;

public class CLog {

    private static File share_log_file = new File(Environment.getExternalStorageDirectory(),
            "Download/config/" + Config.MODULE_PACKAGE_NAME + "/log.txt");

    private static SimpleDateFormat mTimeF = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINESE);
    private static int mCount = 0;

    /**
     * 日志Tag
     */
    public static final String TAG = "TestApp";

    /**
     * 强制输出日志到文件,用于调试用,用完即关
     */
    public static boolean FORCE_FILE_OUTPUT = false;

    public static void log(String pMsg, Throwable exp) {
        log(pMsg);
        log(exp);
    }

    public static void log(Throwable exp) {
        StringWriter tSW = new StringWriter();
        PrintWriter tPW = new PrintWriter(tSW);
        tPW.println("ERROR : " + exp.getLocalizedMessage());
        exp.printStackTrace(tPW);
        tPW.flush();
        log(tSW.toString());
    }


    synchronized public static void log(String pLog) {
        if (FORCE_FILE_OUTPUT || Config.enableFilelog()) {
            FileOutputStream tFOStream = null;
            try {
                if (share_log_file.exists() && (System.currentTimeMillis() - share_log_file.lastModified()) / 1000 > 1200)
                    share_log_file.delete();
                tFOStream = FileUtil.openOutputStream(share_log_file, true);
                byte[] tB = String.format(Locale.CHINESE, "%s|%03d: [%s]%s\n",
                        mTimeF.format(System.currentTimeMillis()), ++mCount, TAG, pLog).getBytes(StandardCharsets.UTF_8);
                tFOStream.write(tB, 0, tB.length);
            } catch (IOException e) {
                Log.e(CLog.TAG, "Error on write log" + e.getLocalizedMessage());
            } finally {
                IOUtil.closeStream(tFOStream);
            }
        }
        Log.i(TAG, pLog);
    }
}
