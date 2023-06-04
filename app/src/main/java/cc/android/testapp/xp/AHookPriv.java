package cc.android.testapp.xp;

import cc.android.testapp.util.CLog;
import cc.commons.util.reflect.ClassUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class AHookPriv extends XC_MethodHook {

    protected String mModName;
    protected String mPackageName;

    private boolean mHooked = false;

    public static void hook(XC_LoadPackage.LoadPackageParam pParam, Class<? extends AHookPriv> pClazz) {
        try {
            String tPackageName = XposedHelpers.getStaticObjectField(pClazz, "PACKAGE_NAME").toString();
            if (!pParam.packageName.equals(tPackageName)) return;

            Class<?> tClazz = XposedHelpers.findClass("android.app.ActivityThread", pParam.classLoader);
            AHookPriv tMod = ClassUtil.newInstance(pClazz);
            tMod.mModName = XposedHelpers.getStaticObjectField(pClazz, "MOD_NAME").toString();
            tMod.mPackageName = tPackageName;
            XposedBridge.hookAllMethods(tClazz, "performLaunchActivity", tMod);
            CLog.log(tMod.mModName + " hook point find success");
        } catch (Throwable exp) {
            CLog.log("hook point find error ->" + pClazz.getName(), exp);
        }
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        if (this.mHooked) return;

        Object tApp = XposedHelpers.getObjectField(param.thisObject, "mInitialApplication");
        ClassLoader tLoad = (ClassLoader) XposedHelpers.callMethod(tApp, "getClassLoader");
        try {
            hookToModel(tLoad);
            this.mHooked = true;
        } catch (Throwable e) {
            CLog.log("error on hook to " + this.mModName, e);
        }
    }

    abstract protected void hookToModel(ClassLoader pLoader) throws Throwable;
}
