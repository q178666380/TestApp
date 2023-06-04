package cc.android.testapp.xp.hooker;

import java.lang.reflect.Method;

import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.AHookPriv;
import cc.commons.util.reflect.MethodUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HookFCDM extends AHookPriv {

    public static final String PACKAGE_NAME = "qiaoba.fcdm.yy";
    public static final String MOD_NAME = "风车动漫";

    @Override
    protected void hookToModel(ClassLoader pLoader) {
        CLog.log("Prepare hook to FFDM AD");
        Class<?> tClazz = XposedHelpers.findClassIfExists(
                "com.bytedance.sdk.openadsdk.stub.activity.Stub_Standard_Portrait_Activity", pLoader);

        Method tTarget = MethodUtil.getMethodIgnoreParam(tClazz, "onCreate", false).first();
        XposedBridge.hookMethod(tTarget, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                MethodUtil.invokeMethod(param.thisObject.getClass(), "finish", param.thisObject);
            }
        });
    }
}
