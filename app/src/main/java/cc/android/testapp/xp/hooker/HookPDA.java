package cc.android.testapp.xp.hooker;

import java.lang.reflect.Method;

import cc.android.testapp.util.CLog;
import cc.android.testapp.util.Config;
import cc.android.testapp.xp.AHookPriv;
import cc.commons.util.StringUtil;
import cc.commons.util.reflect.FieldUtil;
import cc.commons.util.reflect.MethodUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class HookPDA extends AHookPriv {

    public static final String PACKAGE_NAME = "com.cmcc.zjpda";
    public static final String MOD_NAME = "网络运维";

    @Override
    protected void hookToModel(ClassLoader pLoader) throws Throwable {
        {
            // root检查
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
        {
            // 更改签到定位
            final Class<?> tClazz;
            CLog.log("Prepare hook to pda_sign");
            tClazz = pLoader.loadClass(PACKAGE_NAME + ".activity.sign.SignActivity");
            Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "onReceiveLocation", true).oneGet();
            XposedBridge.hookMethod(tTarget, new XC_MethodHook() {

                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!Config.boolValue(Config.KEY_ENABLE_LOCATION_CHANGE)) return;

                    Object tBDLoc = param.args[0];
                    Class<?> clazz_BDLoc = tBDLoc.getClass();
                    try {
                        CLog.log("[pda_sign]: change location start");
                        double tValue = Config.doubleValue(Config.KEY_LOCATION_JD);
                        if (tValue != 0D) {
                            MethodUtil.invokeDeclaredMethod(clazz_BDLoc,
                                    "setLongitude", double.class, tBDLoc, Config.shakeNumber(tValue, 25));
                        }
                        tValue = Config.doubleValue(Config.KEY_LOCATION_WD);
                        if (tValue != 0D) {
                            MethodUtil.invokeDeclaredMethod(clazz_BDLoc,
                                    "setLatitude", double.class, tBDLoc, Config.shakeNumber(tValue, 25));
                        }
                        String tStr = Config.stringValue(Config.KEY_ADDRESS, "");
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
        {
            //更改设备机型ID
            final Class<?> tClazz;
            CLog.log("Prepare hook to pda_device_id");
            tClazz = pLoader.loadClass(PACKAGE_NAME + ".frame.utils.DeviceIdUtil");
            Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "getDeviceId", true).oneGet();
            XposedBridge.hookMethod(tTarget, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!Config.boolValue(Config.KEY_ENABLE_LOCATION_CHANGE)) return;

                    String tStr = Config.stringValue(Config.KEY_DEVICE_ID, "");
                    if (StringUtil.isNotEmpty(tStr)) {
                        param.setResult(tStr);
                    }
                }
            });
            CLog.log("hook pda_device_id end !");
        }
        {
            // 更改拍照定位
            final Class<?> tClazz;
            CLog.log("Prepare hook to pda_camera");
            tClazz = pLoader.loadClass(PACKAGE_NAME + ".photo.TakePhotoActivity");

            Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "onReceiveLocation", true).oneGet();
            XposedBridge.hookMethod(tTarget, new XC_MethodHook() {

                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    if (Config.boolValue(Config.KEY_ENABLE_LOCATION_CHANGE)) {
                        try {
                            double tValue = Config.doubleValue(Config.KEY_LOCATION_JD);
                            if (tValue != 0D) {
                                FieldUtil.setDeclaredFieldValue(tClazz, "mLon", param.thisObject, Config.shakeNumber(tValue, 10));
                            }
                            tValue = Config.doubleValue(Config.KEY_LOCATION_WD);
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

        CLog.log("Hook pda success");
    }
}
