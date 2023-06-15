package cc.android.testapp.xp.hooker.adblock.track;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.hooker.adblock.core.AdRule;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ActTrack extends XC_MethodHook {
    public boolean enable = false;
    public WeakReference<Context> act;
    public AdRule rule;

    public ArrayList<XC_MethodHook.Unhook> hooks = new ArrayList<>();


    public ActTrack(Context pCon, AdRule pRule) {
        this.act = new WeakReference<>(pCon);
        this.rule = pRule;

        this.init();
    }

    public void stopTrack() {
        for (XC_MethodHook.Unhook sHook : hooks) {
            sHook.unhook();
        }
    }

    private void init() {
        hooks.add(XposedHelpers.findAndHookMethod(Activity.class, "finish", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pParam) {
                if (checkTarget(pParam.thisObject)) {
                    CLog.log("finish call_2");
                    if (rule.mSType == SkipType.CLICK_BUTTON) {
                        rule.mSType = SkipType.FINISH_ACT;
                    }
                }
            }
        }));

        hooks.add(XposedHelpers.findAndHookMethod(Activity.class, "startActivityForResult", Intent.class, int.class
                , Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam pParam) {
                        CLog.log("startActivityForResult call_1");
                        if (checkTarget(pParam.thisObject)) {
                            CLog.log("startActivityForResult call_2");
                            rule.mSType = SkipType.START_ACT;
                            rule.mStartIntent = ((Intent) pParam.args[0]).toUri(0);
                        }
                    }
                }));
    }

    private boolean checkTarget(Object pAct) {
        return enable && (act.get() == pAct);
    }

}
