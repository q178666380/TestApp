package cc.android.testapp.xp;

import android.app.Application;
import android.app.Instrumentation;

import cc.android.testapp.cfg.ConfigHelper;
import cc.android.testapp.util.CLog;
import cc.commons.util.reflect.ClassUtil;
import cc.commons.util.reflect.FieldUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class HookBase extends XC_MethodHook {

    protected String mPackageName = null;
    protected String mModName = null;
    private boolean mHooked = false;

    public static void hook(XC_LoadPackage.LoadPackageParam pParam, HookBase pHook) {
        if (!pHook.isTargetApp(pParam)) return;

        final HookBase tHookB = pHook.copyIfIneed();
        XC_MethodHook tHook = new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam pMParam) {
                try {
                    if (tHookB.mHooked) return;

                    Application tApp = (Application) ((pMParam.thisObject instanceof Application) ?
                            pMParam.thisObject : pMParam.args[0]);
                    ConfigHelper.init(tApp);
                    tHookB.hookToApp(pParam, tApp);
                    tHookB.mHooked = true;
                } catch (Throwable e) {
                    CLog.log("Error on (" + tHookB.getModuleName() + ") hook to " + pParam.packageName, e);
                }
            }
        };
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", tHook);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, tHook);
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam pMParam) throws Throwable {
    }

    protected abstract void hookToApp(XC_LoadPackage.LoadPackageParam pParam, Application pApp) throws Throwable;

    /**
     * 获取新的子类实例的以应对特殊的hook需求,默认不拷贝
     *
     * @return
     */
    protected HookBase copyIfIneed() {
        return this;
    }

    /**
     * 查询是否Hook此App
     * <p>
     * 默认的实现方式需要子类中存在名字为PACKAGE_NAME的静态字段,并比较该字段的值与XP中包的名字,
     * 否者将一律返回false
     * <p/>
     *
     * @param pParam app参数
     * @return
     */
    public boolean isTargetApp(XC_LoadPackage.LoadPackageParam pParam) {
        if (this.mPackageName == null) {
            if (FieldUtil.isFieldExist(this.getClass(), "PACKAGE_NAME")) {
                this.mPackageName = FieldUtil.getStaticFieldValue(this.getClass(), "PACKAGE_NAME").toString();
            }
        }
        return this.mPackageName != null && this.mPackageName.equals(pParam.packageName);
    }

    /**
     * 获取该Hook模块的名字
     * <p>
     * 一般用于日志输出,需要子类中存在名字为MOD_NAME的静态字段
     * <p/>
     *
     * @return
     */
    public String getModuleName() {
        if (this.mModName == null) {
            if (FieldUtil.isFieldExist(this.getClass(), "MOD_NAME")) {
                this.mModName = FieldUtil.getStaticFieldValue(this.getClass(), "MOD_NAME").toString();
            }
        }
        return this.mModName != null ? this.getClass().getName() : this.mModName;
    }
}
