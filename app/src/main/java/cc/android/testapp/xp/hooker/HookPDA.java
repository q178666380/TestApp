package cc.android.testapp.xp.hooker;

import android.app.Application;

import java.lang.reflect.Method;

import cc.android.testapp.cfg.Config;
import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.HookBase;
import cc.commons.util.StringUtil;
import cc.commons.util.reflect.FieldUtil;
import cc.commons.util.reflect.MethodUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookPDA extends HookBase {

    public static final String PACKAGE_NAME = "com.cmcc.zjpda";
    public static final String MOD_NAME = "网络运维";

    @Override
    protected void hookToApp(XC_LoadPackage.LoadPackageParam pParam, Application pApp) throws Throwable {
        ClassLoader tLoader = pApp.getClassLoader();

        CLog.FORCE_FILE_OUTPUT = true;
        try {
            hookRootCheck(tLoader);
            hookSignLocation(tLoader);
            hookDeviceId(tLoader);
            hookPhotoLocation(tLoader);

            CLog.log("Hook pda success");
        } finally {
            CLog.FORCE_FILE_OUTPUT = true;
        }
    }

    private void hookPhotoLocation(ClassLoader pLoader) throws ClassNotFoundException {
        // 更改拍照定位
        final Class<?> tClazz;
        CLog.log("Prepare hook to pda_camera");
        tClazz = pLoader.loadClass(PACKAGE_NAME + ".photo.TakePhotoActivity");

        Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "onReceiveLocation", true).oneGet();
        XposedBridge.hookMethod(tTarget, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                if (Config.config.optBoolean(Config.KEY_ENABLE_LOCATION_CHANGE)) {
                    try {
                        double tValue = Config.config.optDouble(Config.KEY_LOCATION_JD);
                        if (tValue != 0D) {
                            FieldUtil.setDeclaredFieldValue(tClazz, "mLon", param.thisObject, Config.shakeNumber(tValue, 10));
                        }
                        tValue = Config.config.optDouble(Config.KEY_LOCATION_WD);
                        if (tValue != 0D) {
                            FieldUtil.setDeclaredFieldValue(tClazz, "mLat", param.thisObject, Config.shakeNumber(tValue, 10));
                        }
                    } catch (IllegalStateException exp) {
                        CLog.log(exp);
                    }
                }
            }
        });
    }

    private void hookDeviceId(ClassLoader pLoader) throws ClassNotFoundException {
        //更改设备机型ID
        final Class<?> tClazz;
        CLog.log("Prepare hook to pda_device_id");
        tClazz = pLoader.loadClass(PACKAGE_NAME + ".frame.utils.DeviceIdUtil");
        Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "getDeviceId", true).oneGet();
        XposedBridge.hookMethod(tTarget, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                if (!Config.config.optBoolean(Config.KEY_ENABLE_LOCATION_CHANGE)) return;

                String tStr = Config.config.optString(Config.KEY_DEVICE_ID, "");
                if (StringUtil.isNotEmpty(tStr)) {
                    param.setResult(tStr);
                }
            }
        });
        CLog.log("hook pda_device_id end !");
    }

    private void hookSignLocation(ClassLoader pLoader) throws ClassNotFoundException {
        // 更改签到定位
        final Class<?> tClazz;
        CLog.log("Prepare hook to pda_sign");
        tClazz = pLoader.loadClass(PACKAGE_NAME + ".activity.sign.SignActivity");
        Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "onReceiveLocation", true).oneGet();
        XposedBridge.hookMethod(tTarget, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam param) {
                if (!Config.config.optBoolean(Config.KEY_ENABLE_LOCATION_CHANGE)) return;

                Object tBDLoc = param.args[0];
                Class<?> clazz_BDLoc = tBDLoc.getClass();
                try {
                    CLog.log("[pda_sign]: change location start");
                    double tValue = Config.config.optDouble(Config.KEY_LOCATION_JD);
                    if (tValue != 0D) {
                        MethodUtil.invokeDeclaredMethod(clazz_BDLoc,
                                "setLongitude", double.class, tBDLoc, Config.shakeNumber(tValue, 25));
                    }
                    tValue = Config.config.optDouble(Config.KEY_LOCATION_WD);
                    if (tValue != 0D) {
                        MethodUtil.invokeDeclaredMethod(clazz_BDLoc,
                                "setLatitude", double.class, tBDLoc, Config.shakeNumber(tValue, 25));
                    }
                    String tStr = Config.config.optString(Config.KEY_ADDRESS, "");
                    if (StringUtil.isNotEmpty(tStr)) {
                        Object tAddress = MethodUtil.invokeDeclaredMethod(clazz_BDLoc, "getAddress", tBDLoc);
                        Class<?> clazz_Address = tAddress.getClass();
                        FieldUtil.setFinalFieldValue(FieldUtil.getDeclaredField(clazz_Address, "address"), tAddress, tStr);
                        FieldUtil.setFinalFieldValue(FieldUtil.getDeclaredField(clazz_Address, "city"), tAddress, "金华市");
                        tStr = String.valueOf(MethodUtil.invokeMethod(MethodUtil.getDeclaredMethod(clazz_BDLoc, "getCity"), tBDLoc));
                        CLog.log("change city to \"" + tStr + "\"");
                        FieldUtil.setFinalFieldValue(FieldUtil.getDeclaredField(clazz_Address, "district"), tAddress, "金东区");
                        tStr = String.valueOf(MethodUtil.invokeMethod(MethodUtil.getDeclaredMethod(clazz_BDLoc, "getDistrict"), tBDLoc));
                        CLog.log("change city to \"" + tStr + "\"");
                    }
                    CLog.log("[pda_sign]: change location end");
                } catch (IllegalStateException exp) {
                    CLog.log("[pda_sign]: change location error", exp);
                }
            }
        });
        CLog.log("hook pda_sign end !");
    }

    private void hookRootCheck(ClassLoader pLoader) throws ClassNotFoundException {
        Class<?> tClazz;
        CLog.log("Prepare hook to pda_root");
        tClazz = pLoader.loadClass(PACKAGE_NAME + ".util.CheckRoot");

        Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "isDeviceRooted", true).oneGet();
        XposedBridge.hookMethod(tTarget, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult(false);
            }
        });

        CLog.log("hook to pda_root end !");
    }
}
