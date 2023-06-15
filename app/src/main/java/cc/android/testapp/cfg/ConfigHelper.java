package cc.android.testapp.cfg;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cc.android.testapp.util.CLog;
import cc.commons.util.reflect.FieldUtil;
import de.robv.android.xposed.XposedHelpers;

public class ConfigHelper {
    public static String PATH_CONFIG = "/config";
    private static WeakReference<Context> mContext = null;
    private static Uri mConfigUri;
    private static boolean mInited;
    private static String mPreCfgStr = "";

    private static final ScheduledExecutorService mExec = Executors.newSingleThreadScheduledExecutor();

    public static void startSelf() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(Config.MODULE_PACKAGE_NAME, Config.MODULE_PACKAGE_NAME + ".act.NoneActivity"));
        intent.setAction(Intent.ACTION_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startSelfReflex(mContext.get(), intent);
        } catch (Throwable e) {
            Log.e(CLog.TAG, "Error on start app", e);
            mContext.get().startActivity(intent);
        }
    }

    private static String reslove(Uri uri) {
        return mContext.get().getContentResolver().getType(uri);
    }

    public static void startSelfReflex(Context context, Intent intent) {
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ((Context) XposedHelpers.getObjectField(context, "mBase")).startActivity(intent);
            return;
        }
        Instrumentation instrumentation = (Instrumentation) XposedHelpers.getObjectField(context, "mInstrumentation");
        IBinder iBinder = (IBinder) FieldUtil.getDeclaredFieldValue(Activity.class, "mToken", context);
        String str = (String) XposedHelpers.getObjectField(context, "mEmbeddedID");
        Object objectField = XposedHelpers.getObjectField(context, "mMainThread");
        Instrumentation.ActivityResult activityResult = (Instrumentation.ActivityResult)
                XposedHelpers.callMethod(instrumentation, "execStartActivity"
                        , new Object[]{context, XposedHelpers.callMethod(objectField
                                , "getApplicationThread"), iBinder, context, intent, -1, null});
        if (activityResult != null) {
            XposedHelpers.callMethod(objectField, "sendActivityResult", iBinder
                    , str, -1, activityResult.getResultCode(), activityResult.getResultData());
        }
    }

    private static void loadConfig() {
        Runnable tTask = () -> {
            try {
                String tCfgStr = resloveConfig();
                if (TextUtils.isEmpty(tCfgStr)) {
                    startSelf();
                    tCfgStr = resloveConfig();
                }
                if (!TextUtils.isEmpty(tCfgStr) && !mPreCfgStr.equals(tCfgStr)) {
                    Log.i(CLog.TAG, "load config :" + tCfgStr);
                    Config.setClientConfig(mPreCfgStr = TextUtils.isEmpty(tCfgStr) ? "{}" : tCfgStr);
                }
            } catch (Throwable e) {
                Log.e(CLog.TAG, "Error on load config", e);
            }
        };
        tTask.run();
        mExec.scheduleWithFixedDelay(tTask, 1, 4, TimeUnit.SECONDS);
    }

    public static String resloveConfig() {
        Uri.Builder tBuilder = mConfigUri.buildUpon();
        tBuilder.path(PATH_CONFIG);
        return reslove(tBuilder.build());
    }

    public static String resloveConfig(String pPath, String pKey, String pValue) {
        return resloveConfig(pPath, new String[]{pKey}, pValue);
    }

    public static String resloveConfig(String pPath, String[] pKeys, String... pValues) {
        Uri.Builder tUpon = mConfigUri.buildUpon();
        tUpon.path(pPath);
        for (int i = 0; i < pKeys.length; i++) {
            tUpon.appendQueryParameter(pKeys[i], pValues[i]);
        }

        return reslove(tUpon.build());
    }

    public static void init(Context pCon) {
        if (mInited) return;

        mInited = true;

        if (pCon instanceof Application) {
            mContext = new WeakReference<>(pCon);
        } else {
            mContext = new WeakReference<>(pCon.getApplicationContext());
        }

        mConfigUri = Uri.parse("content://" + Config.MODULE_PACKAGE_NAME + "?pn=" + mContext.get().getPackageName());

        loadConfig();
    }

}
