package cc.android.testapp.xp.hooker.adblock.core;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;

import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.hooker.adblock.util.XHook;
import cc.commons.util.extra.CList;
import cc.commons.util.reflect.MethodUtil;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class BlockAdSDK {

    private static final List<ClassLoader> mHandledLoader = new ArrayList<>();

    public static void onHook(Application pAdd) {
        ClassLoader tLoader = pAdd.getClassLoader();
        if (mHandledLoader.contains(tLoader)) {
            return;
        }
        mHandledLoader.add(tLoader);

        blockQQAD(tLoader);
        blockMSDKAD(tLoader);
        blockFaceBookAD(tLoader);
        blockBytedanceAD(tLoader);
        blockUnity3DAD(tLoader);
        blockWindAD(tLoader);
        blockKuaiShouAD(tLoader);
        blockAdvlibAD(tLoader);
        blockAndroidAD(tLoader);
        blockBaiduAD(tLoader);
        blockIronSourceAD(tLoader);
        blockCQADSDK(tLoader);
    }

    private static void blockAndroidAD(ClassLoader pLoader) {
        try {
            pLoader.loadClass("com.google.android.gms.ads.MobileAds");
            XHook.with(pLoader)
                    .atClass("com.google.android.gms.ads.MobileAds")
                    .match("initialize(").hookBefore((pMParam, obj, objArr)
                            -> pMParam.setResult((Object) null));
            XHook.with(pLoader)
                    .atClass("com.google.android.gms.ads.AdLoader")
                    .match("loadAd(").hookBefore((pMParam, obj, objArr)
                            -> pMParam.setResult((Object) null));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void blockQQAD(ClassLoader pLoader) {
        ArrayList<Boolean> tResult = new ArrayList<>();
        boolean tHasHook = false;
        String tPack = "com.qq.e.";

        for (String tStr : new String[]{"com.qq.e.ads.splash.SplashAD"
                , tPack + "ads.splash.TGSplashAD"
                , tPack + "ads.contentad.ContentAD"
                , tPack + "ads.nativ.NativeAD"
                , tPack + "ads.interstitial.InterstitialAD"
                , tPack + "ads.interstitial2.UnifiedInterstitialAD"
                , tPack + "ads.banner2.UnifiedBannerAD"
                , tPack + "ads.banner.BannerView"}) {
            try {
                Class<?> tClazz = pLoader.loadClass("com.qq.e.ads.splash.SplashAD");
                XposedBridge.hookAllConstructors(tClazz, clearParamStr(true));
                tHasHook = true;
                tResult.add(true);
            } catch (Throwable ignored) {
                tResult.add(false);
            }
        }

        try {
            String tClazz = tPack + "comm.managers.GDTADManager";
            if (classExist(pLoader, tClazz)) {
                XposedHelpers.findAndHookMethod(tClazz, pLoader, "initWith",
                        Context.class, String.class, clearParamStr(true));
            } else {
                tClazz = tPack + "comm.managers.GDTAdSdk";
                XposedHelpers.findAndHookMethod(tClazz, pLoader, "init",
                        Context.class, String.class, clearParamStr(true));
            }
            tHasHook = true;
            tResult.add(true);
        } catch (Throwable ignored) {
            tResult.add(false);
        }

        //com.qq.e.a.AdDialog
        try {
            String tClazz = tPack + "a.AdDialog";
            if (classExist(pLoader, tClazz)) {
                XposedHelpers.findAndHookMethod(tClazz, pLoader, "show", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ((Dialog) param.thisObject).dismiss();
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
            tHasHook = true;
            tResult.add(true);
        } catch (Throwable ignored) {
            tResult.add(false);
        }
        if (tHasHook) CLog.log("[ADBSDK] QQGDTSDK hooked: " + tResult.toString());
    }

    private static void blockMSDKAD(ClassLoader pLoader) {
        try {
            Class<?> loadClass = pLoader.loadClass("com.mbridge.msdk.system.a");
            XHook.with(pLoader).atClass(loadClass).match("init(").hookBefore((pMParam, obj, objArr)
                    -> pMParam.setResult((Object) null));
            XHook.with(pLoader).atClass(loadClass).match("initAsync(").hookBefore((pMParam, obj, objArr)
                    -> pMParam.setResult((Object) null));
            XposedBridge.hookAllConstructors(pLoader.loadClass(
                    "com.mbridge.msdk.out.MBRewardVideoHandler"), clearParamStr(false));
        } catch (Throwable ignored) {
        }
    }

    private static void blockUnity3DAD(ClassLoader pLoader) {
        try {
            XHook.with(pLoader)
                    .atClass(pLoader.loadClass("com.unity3d.ads.UnityAds"))
                    .match("initialize(").hookBefore((pMParam, obj, objArr)
                            -> pMParam.setResult((Object) null));
        } catch (Throwable ignored) {
        }
    }

    private static void blockWindAD(ClassLoader pLoader) {
        try {
            pLoader.loadClass("com.sigmob.windad.WindAdOptions");
            XposedHelpers.findAndHookConstructor("com.sigmob.windad.WindAdOptions"
                    , pLoader, String.class, String.class, new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam pMParam) {
                            pMParam.args[0] = "";
                            pMParam.args[1] = "";
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    /**
     * 快手SDK
     *
     * @param pLoader
     */
    private static void blockKuaiShouAD(ClassLoader pLoader) {
        ArrayList<Boolean> tResult = new ArrayList<>();
        boolean tHasHook = false;
        String tPacket = "com.kwad.sdk.api.";
        XC_MethodHook tHook = clearParamStr(true);

        try {
            BlockAdSDK.clearAdBuilder(pLoader.loadClass(tPacket + "SdkConfig$Builder"), false);
            tResult.add(true);
            tHasHook = true;
        } catch (Throwable ignored) {
            tResult.add(false);
        }

        try {
            Class<?> tClazz = pLoader.loadClass(tPacket + "KsAdSDK");
            CList<Method> tMs = MethodUtil.getMethodIgnoreParam(tClazz, "init", true);
            if (!tMs.isEmpty()) {
                XposedBridge.hookMethod(tMs.first(), clearParamStr(false));
                tResult.add(true);
                tHasHook = true;
            }
        } catch (Throwable ignored) {
            tResult.add(false);
        }
        if (tHasHook) CLog.log("[ADBSDK] KuaiShouSDK hooked: " + tResult.toString());
    }

    private static void blockBaiduAD(ClassLoader pLoader) {
        ArrayList<Boolean> tResult = new ArrayList<>();
        boolean tHasHook = false;
        String tPacket = "com.baidu.mobads.sdk.api.";
        XC_MethodHook tHook = clearParamStr(true);

        try {
            BlockAdSDK.clearAdBuilder(pLoader.loadClass(tPacket + "BDAdConfig$Builder"), false);
            tResult.add(true);
            tHasHook = true;
        } catch (Throwable ignored) {
            tResult.add(false);
        }

        if (tHasHook) CLog.log("[ADBSDK] BaiDuSDK hooked: " + tResult.toString());
    }

    private static void blockAdvlibAD(ClassLoader pLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.iclicash.advlib.ui.banner.ADBanner"
                    , pLoader, "UpdateView"
                    , pLoader.loadClass("com.iclicash.advlib.core.ICliBundle")
                    , new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam pMParam) {
                            pMParam.setResult((Object) null);
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private static XC_MethodHook clearParamStr(boolean pClearAct) {
        return new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam pParam) {
                if (pParam.args == null) {
                    return;
                }
                for (int i = 0; i < pParam.args.length; i++) {
                    if (pParam.args[i] instanceof String) {
                        pParam.args[i] = "233";
                    } else if (pClearAct && pParam.args[i] instanceof Context) {
                        pParam.args[i] = null;
                    }
                }
            }
        };
    }

    private static void clearAdBuilder(Class<?> tClazz, boolean pClearCon) {
        XC_MethodHook tHook = clearParamStr(pClearCon);
        for (Method sM : tClazz.getDeclaredMethods()) {
            if (sM.getParameterTypes().length == 1
                    && sM.getParameterTypes()[0] == String.class
                    && sM.getReturnType() == tClazz) {
                XposedHelpers.findAndHookMethod(tClazz
                        , sM.getName(), String.class, tHook);
            }
        }
    }

    private static void blockFaceBookAD(ClassLoader pLoader) {
        try {
            pLoader.loadClass("com.facebook.ads.AudienceNetworkAds");
            XposedHelpers.findAndHookMethod("com.facebook.ads.AudienceNetworkAds", pLoader, "initialize", Context.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam pMParam) {
                    pMParam.args[0] = null;
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void blockIronSourceAD(ClassLoader pLoader) {
        try {
            Class<?> loadClass = pLoader.loadClass("com.ironsource.mediationsdk.IronSource");
            XHook.with(pLoader).atClass(loadClass).match("init(").hookBefore(new XHook.HookMethod() {
                @Override
                public void onHook(XC_MethodHook.MethodHookParam pMParam, Object obj, Object... pParams) {
                    pMParam.setResult((Object) null);
                }
            });
            XHook.with(pLoader).atClass(loadClass).match("initISDemandOnly(").hookBefore(new XHook.HookMethod() {
                @Override
                public void onHook(XC_MethodHook.MethodHookParam pMParam, Object obj, Object... pParams) {
                    pMParam.setResult((Object) null);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void blockBytedanceAD(ClassLoader pLoader) {
        ArrayList<Boolean> tResult = new ArrayList<>();
        boolean tHasHook = false;
        String tPacket = "com.bytedance.sdk.openadsdk.";
        try {
            XC_MethodHook tHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam pParam) throws Throwable {
                    if (pParam.method.getName().equals("setCodeId")) {
                        pParam.args[0] = "233";
                    } else if (pParam.method.getName().equals("setExpressViewAcceptedSize")) {
                        pParam.args[0] = 0f;
                        pParam.args[1] = 0f;
                    }
                }
            };
            XposedHelpers.findAndHookMethod(tPacket + "AdSlot$Builder", pLoader
                    , "setCodeId", String.class, tHook);
            XposedHelpers.findAndHookMethod(tPacket + "AdSlot$Builder", pLoader
                    , "setExpressViewAcceptedSize", float.class, float.class, tHook);
            tResult.add(true);
            tHasHook = true;
        } catch (Throwable ignored) {
            tResult.add(false);
        }

        try {
            BlockAdSDK.clearAdBuilder(pLoader.loadClass(tPacket + "TTAdConfig$Builder"), false);
            tResult.add(true);
            tHasHook = true;
        } catch (Throwable ignored) {
            tResult.add(false);
        }
        if (tHasHook) CLog.log("[ADBSDK] ByteDanceSDK hooked: " + tResult.toString());
    }

    private static void blockCQADSDK(ClassLoader pLoader) {
        ArrayList<Boolean> tResult = new ArrayList<>();
        boolean tHasHook = false;
        String tPacket = "com.cqyh.cqadsdk.";

        try {
            BlockAdSDK.clearAdBuilder(pLoader.loadClass(tPacket + "CQAdSDKConfig$Builder"), false);
            tResult.add(true);
            tHasHook = true;
        } catch (Throwable ignored) {
            tResult.add(false);
        }
        if (tHasHook) CLog.log("[ADBSDK] CQADSDK hooked: " + tResult.toString());
    }

    public static boolean classExist(ClassLoader pLoader, String pClazz) {
        try {
            pLoader.loadClass(pClazz);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

}
