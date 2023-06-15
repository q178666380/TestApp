package cc.android.testapp.xp;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.util.Log;

import java.util.ArrayList;

import cc.android.testapp.util.CLog;
import cc.android.testapp.cfg.Config;
import cc.android.testapp.xp.hooker.HookPDA;
import cc.android.testapp.xp.hooker.HookStacktrace;
import cc.android.testapp.xp.hooker.adblock.HookADBlock;
import cc.android.testapp.xp.hooker.textmodify.HookEditText;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private final ArrayList<HookBase> mSubHook = new ArrayList<>();

    public MainHook() {
        HookStacktrace tLis;
        this.mSubHook.add(new HookPDA());
        this.mSubHook.add(new HookEditText());
        this.mSubHook.add(new HookADBlock());
        this.mSubHook.add(tLis = new HookStacktrace());
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pParam) {
        if (!pParam.isFirstApplication || !pParam.processName.equals(pParam.packageName)) return;

        try {
            if (pParam.packageName.equals(Config.MODULE_PACKAGE_NAME)) {
                CLog.log("Hook to self, change xposed model enable stats");
                findAndHookMethod(Config.MODULE_PACKAGE_NAME + ".MainActivity", pParam.classLoader
                        , "isModuleEnabled", XC_MethodReplacement.returnConstant(true));
                return;
            }

            Config.AllowSave = false;
            for (HookBase sHook : this.mSubHook) {
                try {
                    HookBase.hook(pParam, sHook);
                } catch (Throwable e) {
                    Log.e(CLog.TAG, "Error on prepare hook to " + sHook.getModuleName(), e);
                }
            }
        } catch (Throwable exp) {
            Log.e(CLog.TAG, "error on hook main stage", exp);
        }
    }

    public static Resources TAResource;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        TAResource = XModuleResources.createInstance(startupParam.modulePath, null);
    }

}
