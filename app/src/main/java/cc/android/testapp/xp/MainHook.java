package cc.android.testapp.xp;

import cc.android.testapp.xp.hooker.edit_text.HookEditText;
import cc.android.testapp.xp.hooker.HookFCDM;
import cc.android.testapp.xp.hooker.HookPDA;
import cc.android.testapp.util.Config;
import cc.android.testapp.util.CLog;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.content.res.Resources;
import android.content.res.XModuleResources;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pParam) throws Throwable {
        if (!pParam.isFirstApplication) return;

        try {
            if (pParam.packageName.equals(Config.MODULE_PACKAGE_NAME)) {
                CLog.log("Hook to self, change xposed model enable stats");
                findAndHookMethod(Config.MODULE_PACKAGE_NAME + ".MainActivity", pParam.classLoader
                        , "isModuleEnabled", XC_MethodReplacement.returnConstant(true));
                return;
            }

            HookPDA.hook(pParam, HookPDA.class);// PDA
            HookFCDM.hook(pParam, HookFCDM.class);// 风车动漫
            HookEditText.hook(pParam);
        } catch (Throwable exp) {
            CLog.log("error on hook main stage", exp);
        }
    }

    public static Resources TAResource;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        TAResource = XModuleResources.createInstance(startupParam.modulePath, null);
    }

}
