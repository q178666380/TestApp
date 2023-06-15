package cc.android.testapp.xp.hooker.adblock.util;

import android.content.pm.PackageManager;

import cc.android.testapp.cfg.Config;
import cc.commons.util.reflect.FieldUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;

public class XposedHidden {

    public static boolean isSelfPackage(String str) {
        return str.contains(Config.MODULE_PACKAGE_NAME);
    }

    public static boolean hasXposedStr(String str) {
        return (str.contains(Config.MODULE_PACKAGE_NAME)
                || str.contains("de.robv.android.xposed.installer")
                || str.contains("org.meowcat.edxposed.manager"))
                && !str.contains("afterHookedMethod") && !str.contains("beforeHookedMethod");
    }

    public static void hiddenModule(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (isSelfPackage(loadPackageParam.packageName)
                || "com.google.android.webview".equals(loadPackageParam.packageName)
                || (loadPackageParam.appInfo.flags & 1) == 1) {
            return;
        }

        XC_MethodHook tHook = new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam pMParam) {
                String str = (String) pMParam.args[0];
                if (XposedHidden.isSelfPackage(str)) {
                    for (StackTraceElement sEle : Thread.currentThread().getStackTrace()) {
                        if (XposedHidden.hasXposedStr(sEle.toString())) return;
                    }
                    pMParam.setThrowable(new PackageManager.NameNotFoundException(str));
                }
            }
        };

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager"
                , loadPackageParam.classLoader, "getPackageInfo"
                , String.class, Integer.TYPE, tHook);

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager"
                , loadPackageParam.classLoader, "getApplicationInfo"
                , String.class, Integer.TYPE, tHook);

        tHook = new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam pMParam) {
                List<Object> list = (List) pMParam.getResult();
                ArrayList<Object> tNewResult = new ArrayList<>();
                boolean tCallFromSelf = false;
                for (StackTraceElement sEle : Thread.currentThread().getStackTrace()) {
                    if (XposedHidden.hasXposedStr(sEle.toString())) tCallFromSelf = true;
                }
                for (Object sApp : list) {
                    String tPName = String.valueOf(FieldUtil.getFieldValue(FieldUtil.getField(sApp.getClass(), "packageName"), sApp));
                    if (XposedHidden.isSelfPackage(tPName) && !tCallFromSelf)
                        continue;

                    tNewResult.add(sApp);
                }
                pMParam.setResult(tNewResult);
            }
        };
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager"
                , loadPackageParam.classLoader, "getInstalledApplications"
                , Integer.TYPE, tHook);
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager"
                , loadPackageParam.classLoader, "getInstalledPackages"
                , Integer.TYPE, tHook);
    }

}
