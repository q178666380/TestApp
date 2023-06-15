package cc.android.testapp.xp.hooker.adblock.util;

import android.text.TextUtils;

import cc.android.testapp.util.CLog;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class XHook {

    private String mMethodName;

    private Class<?>[] pParams;

    private String pMethodPartStr;

    private ClassLoader pLoader;

    private boolean mStop;

    private Class<?> mOwnClazz;

    public interface HookMethod {
        void onHook(XC_MethodHook.MethodHookParam pParam, Object pThis, Object... pParams);
    }

    private XHook() {
    }

    public static XHook with() {
        return with(XHook.class.getClassLoader());
    }

    public static XHook with(ClassLoader classLoader) {
        XHook xHook = new XHook();
        xHook.pLoader = classLoader;
        return xHook;
    }

    private void collMethods(Class<?> cls, Set<Method> set) {
        if (cls.equals(Object.class)) {
            return;
        }
        set.addAll(Arrays.asList(cls.getDeclaredMethods()));
        collMethods(cls.getSuperclass(), set);
    }

    public XHook args(Class<?>... clsArr) {
        this.pParams = clsArr;
        return this;
    }

    public XHook atClass(Class<?> cls) {
        this.mOwnClazz = cls;
        return this;
    }

    public XHook atClass(String pClass) {
        try {
            this.mOwnClazz = this.pLoader.loadClass(pClass);
            return this;
        } catch (ClassNotFoundException e) {
            CLog.log("XHook.atClass load class fail", e);
            return this;
        }
    }

    public void hook(final HookMethod pBefore, final HookMethod pAfter) {
        if (this.mStop || this.mOwnClazz == null) {
            return;
        }
        XC_MethodHook xC_MethodHook = new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam pMParam) {
                if (pAfter != null) {
                    pAfter.onHook(pMParam, pMParam.thisObject, pMParam.args);
                }
            }

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam pMParam) {
                if (pBefore != null) {
                    pBefore.onHook(pMParam, pMParam.thisObject, pMParam.args);
                }
            }
        };
        int i = 0;
        if (!TextUtils.isEmpty(this.pMethodPartStr)) {
            HashSet<Method> tMethods = new HashSet<>();
            collMethods(this.mOwnClazz, tMethods);
            for (Method method : tMethods) {
                if (TextUtils.isEmpty(this.mMethodName) || method.getName().equals(this.mMethodName)) {
                    if (method.toString().contains(this.pMethodPartStr)) {
                        CLog.log("[ADBSDK] BLOCK hook to " + method.toString());
                        if (!method.toString().contains("abstract")) {
                            XposedBridge.hookMethod(method, xC_MethodHook);
                        }
                    }
                }
            }
        } else if (TextUtils.isEmpty(this.mMethodName)) {
            throw new RuntimeException("method not null");
        } else {
            Class<?>[] clsArr = this.pParams;
            if (clsArr == null) {
                XposedHelpers.findAndHookMethod(this.mOwnClazz, this.mMethodName, xC_MethodHook);
                return;
            }
            Object[] objArr = new Object[clsArr.length + 1];
            while (true) {
                Class<?>[] clsArr2 = this.pParams;
                if (i >= clsArr2.length) {
                    objArr[objArr.length - 1] = xC_MethodHook;
                    XposedHelpers.findAndHookMethod(this.mOwnClazz, this.mMethodName, objArr);
                    return;
                }
                objArr[i] = clsArr2[i];
                i++;
            }
        }
    }

    public void hookAfter(HookMethod hookMethod) {
        hook(null, hookMethod);
    }

    public void hookBefore(HookMethod hookMethod) {
        hook(hookMethod, null);
    }

    public XHook match(String pMethodPart) {
        this.pMethodPartStr = pMethodPart;
        return this;
    }

    public XHook method(String pMethod) {
        this.mMethodName = pMethod;
        return this;
    }

    public XHook stop() {
        this.mStop = true;
        return this;
    }
}
