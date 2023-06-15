package cc.android.testapp.xp.hooker.adblock.track;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.IdentityHashMap;

import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.hooker.adblock.core.AdRule;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TrackHelper {

    private final static IdentityHashMap<Message, Handler> mMsgs = new IdentityHashMap<>();
    private static ActTrack mTrack = null;
    private static CallB mCallB = null;

    private static XC_MethodHook.Unhook MSG_HOOK = null;

    public static void startTrack(Context pAct, AdRule pRule, CallB pCall) {
        CLog.log("开始跟踪");
        mTrack = new ActTrack(pAct, pRule);
        mMsgs.clear();
        mCallB = pCall;
        MSG_HOOK = XposedHelpers.findAndHookMethod(Handler.class, "sendMessageDelayed"
                , Message.class, long.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam pParam) {
                        if ((boolean) pParam.getResult() && !pParam.thisObject.getClass().getName().startsWith("android")) {
                            mMsgs.put((Message) pParam.args[0], (Handler) pParam.thisObject);
                            // CLog.log("Msg拦截" + pParam.args[0]);
                        }
                    }
                });
        mTrack.enable = true;
    }

    public static void finishTrack() {
        mTrack.stopTrack();

        if (mTrack.rule.isDirectSkip() || mMsgs.isEmpty()) {
            mCallB.call(mTrack.rule);
            mMsgs.clear();
            CLog.log("跟踪结束");
            return;
        }

        mTrack = new ActTrack(mTrack.act.get(), mTrack.rule.copy());
        long tOverTIme = System.currentTimeMillis() + 5000;
        HashSet<Class<?>> tHandlerC = new HashSet<>();
        for (Handler sHandler : mMsgs.values()) {
            if (tHandlerC.contains(sHandler.getClass())) continue;

            Method tM = XposedHelpers.findMethodExactIfExists(sHandler.getClass(), "handleMessage", Message.class);
            if (tM == null) continue;

            tHandlerC.add(sHandler.getClass());
            XC_MethodHook.Unhook tUHook = XposedBridge.hookMethod(tM, new XC_MethodHook() {
                Message mNow = null;

                @Override
                protected void beforeHookedMethod(MethodHookParam pParam) {
                    if (mMsgs.isEmpty() || System.currentTimeMillis() > tOverTIme) {
                        mMsgs.clear();
                        mTrack.stopTrack();
                        if (mTrack.rule.isDirectSkip()) mCallB.call(mTrack.rule);
                        CLog.log("Msg拦截结束或超时退出");
                        return;
                    }

                    Message tMsg = (Message) pParam.args[0];
                    Handler tHandler = mMsgs.remove(tMsg);
                    if (tHandler != null) {
                        CLog.log("Msg拦截(2_start)" + pParam.args[0] + " method: " + pParam.method);
                        mNow = tMsg;
                        mTrack.enable = true;
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam pParam) {
                    if (pParam.args[0] == mNow) {
                        CLog.log("Msg拦截(2_end)" + pParam.args[0] + " method: " + pParam.method);
                        mTrack.enable = false;
                    }
                }
            });
            mTrack.hooks.add(tUHook);
        }
        mTrack.hooks.add(MSG_HOOK);

    }

    public interface CallB {
        void call(AdRule pRule);
    }
}
