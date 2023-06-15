package cc.android.testapp.xp.hooker.adblock;

import android.app.Application;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import cc.android.testapp.cfg.ConfigHelper;
import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.HookBase;
import cc.android.testapp.xp.hooker.HookPDA;
import cc.android.testapp.xp.hooker.adblock.core.BlockAdAct;
import cc.android.testapp.xp.hooker.adblock.core.BlockAdSDK;
import cc.android.testapp.xp.hooker.adblock.util.ADBCfg;
import cc.android.testapp.xp.hooker.adblock.util.XposedHidden;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookADBlock extends HookBase {

    public static final String MOD_NAME = "广告屏蔽";
    public static final Set<String> STR_SKIP = new HashSet<>(Arrays.asList("跳过", "跳转", "进入"));
    public static final Pattern STR_TIME_SKIP = Pattern.compile("\\d[s秒]$");
    public static final HashSet<String> WHITE_LIST_DEF = new HashSet<>(Arrays.asList(
            "com.tencent.mm", "com.xunmeng.pinduoduo", "com.eg.android.AlipayGphone",
            HookPDA.PACKAGE_NAME
    ));

    @Override
    protected void hookToApp(XC_LoadPackage.LoadPackageParam pParam, Application pApp) throws Throwable {
        if (!ADBCfg.skipAd()) return;

        try {
            XposedHidden.hiddenModule(pParam);
        } catch (Throwable e) {
            CLog.log("Error on hidden self from app", e);
        }

        try {
            hookAdDialog();
        } catch (Throwable e) {
            CLog.log("Error on hook AdDialog.show()", e);
        }

        try {
            hookSkipButton(pParam, "com.tencent.qqlive"); //qqlite
            hookSkipButton(pParam, "com.sina.weibo"); //sina
        } catch (Throwable e) {
            CLog.log("Error on hook skip button for qq or sina", e);
        }

        zhihuEnableXP(pApp.getPackageName());
        BlockAdSDK.onHook(pApp);
        BlockAdAct.onHook(pParam);
    }

    @Override
    public boolean isTargetApp(XC_LoadPackage.LoadPackageParam pParam) {
        // system app
        if ((pParam.appInfo.flags & 1) == 1 && pParam.packageName.contains("com.android."))
            return false;

        // 已知的无需关闭的app
        for (String sPack : WHITE_LIST_DEF) {
            if (sPack.equals(pParam.packageName)) return false;
        }

        return true;
    }

    public static void hookSkipButton(XC_LoadPackage.LoadPackageParam pParam, String pPackName) {
        if (pParam.packageName.equals(pPackName)) {
            XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, TextView.BufferType.class
                    , Boolean.TYPE, Integer.TYPE, new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam pMParam) {
                            if (pMParam.args[0] == null) return;
                            if (HookADBlock.STR_SKIP.contains(String.valueOf(pMParam.args[0]))) {
                                final View view = (View) pMParam.thisObject;
                                view.postDelayed(() -> fakeClick(view), 10L);
                            }
                        }
                    });
        }
    }

    public static void hookAdDialog() {
        XposedHelpers.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam pMParam) {
                Dialog tDialog = (Dialog) pMParam.thisObject;
                if (tDialog.getClass().getName().toLowerCase().contains("addialog")) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        tDialog.dismiss();
                        CLog.log("Closed AdDialog(" + tDialog.getClass().getName() + ")");
                        BlockAdAct.noticeAdSkip(tDialog.getContext(), "AdDialog");
                    }, 1);
                }
            }
        });
    }

    public static void fakeClick(View view) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        final int i = rect.left + 5;
        final int i2 = rect.top + 5;
        final View rootView = view.getRootView();
        final long nanoTime = System.nanoTime();
        rootView.dispatchTouchEvent(MotionEvent.obtain(nanoTime, nanoTime, MotionEvent.ACTION_DOWN, i, i2, 0));
        rootView.postDelayed(() -> rootView.dispatchTouchEvent(MotionEvent.obtain(nanoTime, System.nanoTime()
                , MotionEvent.ACTION_MOVE, i, i2, 0)), 5L);
        rootView.postDelayed(() -> rootView.dispatchTouchEvent(MotionEvent.obtain(nanoTime, System.nanoTime()
                , MotionEvent.ACTION_UP, i, i2, 0)), 10L);
    }

    private static void zhihuEnableXP(String str) {
        if (str.equals("com.zhihu.android")) {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "zhihu/.allowXposed");
            try {
                if (file.exists()) {
                    return;
                }
                if (file.getParentFile().mkdirs()) file.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

}
