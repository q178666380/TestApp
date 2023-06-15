package cc.android.testapp.xp.hooker;

import android.app.Application;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import cc.android.testapp.cfg.Config;
import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.HookBase;
import cc.android.testapp.xp.hooker.adblock.core.BlockAdSDK;
import cc.commons.util.StringUtil;
import cc.commons.util.extra.CList;
import cc.commons.util.reflect.FieldUtil;
import cc.commons.util.reflect.MethodUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookStacktrace extends HookBase implements Runnable {

    public static final String MOD_NAME = "方法追踪";
    private static ClassLoader mLoader;
    private static String mCheckClass = null;
    private static String mCheckMethod = null;
    private final static ArrayList<Unhook> mMethodHook = new ArrayList<>();

    @Override
    public void run() {
        mCheckClass = Config.config.optString(Config.KEY_CHECK_CLASS, null);
        if (StringUtil.isBlank(mCheckClass)) mCheckClass = null;
        else mCheckClass = mCheckClass.replace('/', '.');

        mCheckMethod = Config.config.optString(Config.KEY_CHECK_METHOD, null);
        if (StringUtil.isBlank(mCheckMethod)) mCheckMethod = null;

        runHook();
    }

    @Override
    protected void hookToApp(XC_LoadPackage.LoadPackageParam pParam, Application pApp) throws Throwable {
        mLoader = pApp.getClassLoader();
        this.run();
    }

    @Override
    public boolean isTargetApp(XC_LoadPackage.LoadPackageParam pParam) {
        return true;
    }

    public static void runHook() {
        if (mLoader == null) return;

        if (!mMethodHook.isEmpty()) {
            for (XC_MethodHook.Unhook sUnhook : mMethodHook) sUnhook.unhook();
            mMethodHook.clear();
        }

        if (mCheckClass == null || mCheckMethod == null) return;

        if (BlockAdSDK.classExist(mLoader, mCheckClass)) {
            try {
                Class<?> tClazz = mLoader.loadClass(mCheckClass);
                CList<Member> tMethods = new CList<>();
                if (tClazz.getSimpleName().equals(mCheckMethod)) {
                    tMethods.addAll(Arrays.asList(tClazz.getDeclaredConstructors()));
                } else if (MethodUtil.isDeclaredMethodExist(tClazz, (pMethod) -> pMethod.getName().equals(mCheckMethod))) {
                    tMethods.addAll(MethodUtil.getMethodIgnoreParam(tClazz, mCheckMethod, true));
                }

                if (tMethods.isEmpty()) {
                    CLog.log("Method (" + mCheckMethod + ") in Class " + mCheckClass + " no found");
                    return;
                }

                for (Member sMethod : tMethods) {
                    XC_MethodHook.Unhook t = XposedBridge.hookMethod(sMethod, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            CLog.FORCE_FILE_OUTPUT = true;
                            CLog.log("Stack trace for " + sMethod);
                            StringBuilder tSBuilder;

                            if (param.thisObject != null) {
                                tSBuilder = new StringBuilder("this{ ");
                                CList<Field> tFields = FieldUtil.getDeclaredField(param.thisObject.getClass(), (pField) -> true);
                                for (Field sField : tFields) {
                                    tSBuilder.append("\"" + sField.getName() + "\"" + ": " + FieldUtil.getFieldValue(sField, param.thisObject));
                                    tSBuilder.append(",");
                                }
                                tSBuilder.append("}");
                                CLog.log(tSBuilder.toString());
                            }
                            tSBuilder = new StringBuilder("Params: ");
                            if (param.args.length != 0) {
                                int i = 0;
                                for (Object sObj : param.args) {
                                    if (sObj == null) {
                                        String tName = (sMethod instanceof Method) ? ((Method) sMethod).getParameterTypes()[i].getName()
                                                : ((Constructor<?>) sMethod).getParameterTypes()[i].getName();
                                        tSBuilder.append("(" + tName + ":null)");
                                    } else {
                                        tSBuilder.append("(" + sObj.getClass().getName() + ":" + sObj + ")");
                                    }
                                    i++;
                                }
                            } else {
                                tSBuilder.append("(none)");
                            }
                            CLog.log(tSBuilder.toString());
                            String tResult = String.valueOf(param.getResult());
//                            if (tResult.length() > 100) {
//                                CLog.log("return: ");
//                                int tStart = 0, tEnd = 100;
//                                while (true) {
//                                    CLog.log(tResult.substring(tStart, tEnd));
//                                    if (tEnd >= tResult.length()) break;
//                                    tStart = tEnd;
//                                    tEnd += 100;
//                                    if (tEnd > tResult.length()) tEnd = tResult.length();
//                                }
//                            } else
                            CLog.log("return: " + tResult);
                            CLog.log(new RuntimeException());
                            CLog.FORCE_FILE_OUTPUT = false;
                        }
                    });
                    mMethodHook.add(t);
                }
                CLog.log("Start track Method (" + mCheckMethod + ") in Class " + mCheckClass);
            } catch (Throwable e) {
                e.printStackTrace();
                CLog.log("Error on trace hook: " + e.getLocalizedMessage());
            }
        } else {
            CLog.log("Class no found: " + mCheckClass);
        }
    }

}
