package cc.android.testapp.xp.hooker.textmodify;

import android.app.Application;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.HookBase;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEditText extends HookBase {

    public static final String MOD_NAME = "文本编辑";

    @Override
    protected void hookToApp(XC_LoadPackage.LoadPackageParam pParam, Application pApp) throws Throwable {
        CLog.log("EditText model hook to->" + pParam.packageName);
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent"
                , MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam pParam) {
                        if (!(pParam.thisObject instanceof TextView)) return;
                        TextView tView = (TextView) pParam.thisObject;
                        if (!ETCfg.isEnableEdit() || !ETCfg.canEdit(tView)) return;

                        if (((MotionEvent) pParam.args[0]).getAction() == MotionEvent.ACTION_DOWN) {
                            pParam.setResult(true);
                            new ETDiaLog(tView.getContext(), tView, ETCfg.getListener(tView)).show();
                        }
                    }
                });
        XposedHelpers.findAndHookMethod(TextView.class, "setText"
                , CharSequence.class, TextView.BufferType.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam pParam) throws Throwable {
                        TextView tView = (TextView) pParam.thisObject;
                        if (!ETCfg.isEnableEdit() || !ETCfg.canEdit(tView)) return;
                        // 锁定文字
                        if (ETCfg.isTextLocked(tView)) pParam.args[0] = ETCfg.getLockText(tView);
                        // 高亮
                        if (pParam.args[0] != null && ETCfg.canHighLight(tView)) {
                            String tText = pParam.args[0].toString();
                            SpannableString tSpaStr = new SpannableString(tText);
                            tSpaStr.setSpan(new BackgroundColorSpan(Color.RED), 0, tText.length(), 18);
                            pParam.args[0] = tSpaStr;
                        }
                    }
                });
    }

    @Override
    public boolean isTargetApp(XC_LoadPackage.LoadPackageParam pParam) {
        return true;
    }
}
