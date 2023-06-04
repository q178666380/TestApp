package cc.android.testapp.xp.hooker.edit_text;

import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import cc.android.testapp.util.CLog;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEditText extends XC_MethodHook {

    public static void hook(XC_LoadPackage.LoadPackageParam pParam) {
        CLog.log("edit text model hook to->" + pParam.packageName);
        HookEditText tHook = new HookEditText();
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent"
                , MotionEvent.class, tHook);
        XposedHelpers.findAndHookMethod(TextView.class, "setText"
                , CharSequence.class, TextView.BufferType.class, tHook);

    }

    protected void beforeHookedMethod(MethodHookParam pParam) throws Throwable {
        if (!(pParam.thisObject instanceof TextView)) return;
        TextView tView = (TextView) pParam.thisObject;
        String tMName = pParam.method.getName();

        if (tMName.equals("setText")) {
            // 锁定文字
            if (ETUtil.isTextLocked(tView)) pParam.args[0] = ETUtil.getLockText(tView);

            // 高亮
            if (pParam.args[0] != null && ETUtil.canHighLight(tView)) {
                String tText = pParam.args[0].toString();
                SpannableString tSpaStr = new SpannableString(tText);
                tSpaStr.setSpan(new BackgroundColorSpan(-65536), 0, tText.length(), 18);
                pParam.args[0] = tSpaStr;
            }
        }


        if (ETUtil.isEnableEdit() && ETUtil.canEdit(tView) && tMName.equals("dispatchTouchEvent")) {
            if (((MotionEvent) pParam.args[0]).getAction() == MotionEvent.ACTION_DOWN) {
                pParam.setResult(true);
                new ETDiaLog(tView.getContext(), tView, ETUtil.getListener(tView)).show();
            }
        }

    }

}
