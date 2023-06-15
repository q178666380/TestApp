package cc.android.testapp.xp.hooker.adblock.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.hooker.adblock.HookADBlock;
import cc.android.testapp.xp.hooker.adblock.track.SkipType;
import cc.android.testapp.xp.hooker.adblock.track.TrackHelper;
import cc.android.testapp.xp.hooker.adblock.util.ADBCfg;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BlockAdAct {

    private static final ArrayList<Rect> mSkipButtonArea = new ArrayList<>();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    /**
     * 缓存的广告规则
     * <p>一般情下,只允许AdRule.isAd()为true的时候,AdRule才能被加入缓存</p>
     */
    private static final HashMap<String, AdRule> mActRuleCache = new HashMap<>();
    private static final HashMap<String, ViewListener> mActOpMap = new HashMap<>();
    private static final List<String> mInhandleAct = new ArrayList<>();

    private static final String[][] mKnewAdId = {new String[]{"com.sankuai.meituan.takeoutnew.ui.page.boot.WelcomeActivity", "ll_skip"}, new String[]{"com.sinovatech.unicom.basic.ui.MainActivity", "splash_advertise_close"}};
    private static final String[] mIgnoreRes = {"action", "gallery", "edit", "add", "sign", "title", "qrcode", "query", "search", "icon", "message", "setting", "scan", "notice", "menu", "tab", "manager", "mine", "home", "more"};


    public static void onHook(XC_LoadPackage.LoadPackageParam pParam) {
        hookActOnCreate(pParam);
        hookActOnResume(pParam);
        hookActOnPause(pParam);
    }

    private static void hookActOnResume(XC_LoadPackage.LoadPackageParam pParam) {
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam pMParam) {
                if (!ADBCfg.skipAd()) return;

                Activity tAct = (Activity) pMParam.thisObject;
                CLog.log("onResume: " + toStr(tAct));
                if (execCacheRule(tAct)) return;

                ViewGroup pViewG = getViewGroup(tAct);
                if (pViewG == null) return;

                performClick(tAct, pViewG);
            }
        });
    }

    public static void hookActOnCreate(XC_LoadPackage.LoadPackageParam pParam) {
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam pMParam) {
                if (!ADBCfg.skipAd()) return;

                Activity tAct = (Activity) pMParam.thisObject;
                CLog.log("onCreate: " + toStr(tAct));
                ViewGroup pViewG = getViewGroup(tAct);
                if (pViewG == null) return;

                AdRule tCfgRule = getRuleFromCfg(tAct);
                if (tCfgRule.isAd()) {
                    mActRuleCache.put(tAct.getClass().getName(), tCfgRule);
                    if (execCacheRule(tAct)) return;
                }

                performClick(tAct, pViewG);
            }
        });
    }

    private static boolean execCacheRule(Activity pAct) {
        AdRule tCacheR = mActRuleCache.get(pAct.getClass().getName());
        if (tCacheR == null || !tCacheR.isAd() || tCacheR.mSType == SkipType.BAN) return false;
        boolean tFinish = false;
        if (tCacheR.mSType == SkipType.FINISH_ACT) {
            pAct.finish();
            CLog.log("通过执行finish()方法来跳过广告" + toStr(pAct));
            tFinish = true;
        } else if (tCacheR.mSType == SkipType.START_ACT) {
            Intent tIntent = tCacheR.getIntent();
            if (tIntent != null) {
                pAct.startActivity(tIntent);
                CLog.log("通过执行startActivity方法来跳过广告" + toStr(pAct));
                tFinish = true;
            }
        }
        if (tFinish) {
            noticeAdSkip(pAct, "direct exec");
            removeFromOpList(pAct);
        }

        return tFinish;
    }

    private static AdRule getRuleFromCfg(Context pCon) {
        String tName = pCon.getClass().getName();

        AdRule tCfgR = AdRule.parseJson(ADBCfg.getAppRule(tName));
        if (!tCfgR.isAd()) {
            for (String[] sArr : mKnewAdId) {
                if (sArr[0].equals(tName)) {
                    tCfgR = new AdRule(pCon.getResources().getIdentifier(sArr[1], "id", pCon.getPackageName()), tName);
                    break;
                }
            }
        }
        CLog.log("读取配置->" + tCfgR + toStr(pCon));
        return tCfgR;
    }

    private static @Nullable AdRule findNonameButton(Context context, ViewGroup pViewG) {
        if (ignoreAct(context)) {
            CLog.log("关键字activity跳过");
            return null;
        }

        CLog.log("尝试检索无文字按钮:(" + context.getClass().getName() + ")");
        ViewCache tViews = new ViewCache();
        searchNoBlockView(tViews, pViewG);
        CLog.log("view个数: " + context + " View: " + tViews.getViewNum() + " TextView: " + tViews.getTextNum());
        if (tViews.isOverflow()) {
            CLog.log("复杂界面跳过: " + context);
        } else {
            Rect tRect = new Rect();
            ArrayList<View> tMatchViews = new ArrayList<>();
            for (View sView : tViews.getViews()) {
                int tId = sView.getId();
                if (!sView.isShown() || !sView.isClickable() || tId == View.NO_ID)
                    continue;

                try {
                    String resourceEntryName = sView.getResources().getResourceEntryName(tId);
                    for (String sStr : mIgnoreRes) {
                        if (resourceEntryName.contains(sStr)) continue;
                    }
                } catch (Exception ignored) {
                    continue;
                }
                if ((tId & ViewCompat.MEASURED_STATE_MASK) != ViewCompat.MEASURED_STATE_TOO_SMALL) {
                    sView.getGlobalVisibleRect(tRect);
                    for (Rect sRect : mSkipButtonArea)
                        if (sRect.contains(tRect)) {
                            tMatchViews.add(sView);
                        }
                }
            }

            View tView = getBestAdButton(tMatchViews);
            if (tView != null) return new AdRule(tView);
        }
        return null;
    }


    private static View getBestAdButton(List<View> pViews) {
        if (pViews.size() > 1) {
            Collections.sort(pViews, (view, view2) -> {
                if (view.isShown() != view2.isShown()) return view.isShown() ? -1 : 1;
                if (view.isClickable() != view2.isClickable())
                    return view.isClickable() ? -1 : 1;

                if (view.getId() == View.NO_ID) return 1;
                return view2.getId() == View.NO_ID ? 1 : 0;
            });

        }

        return pViews.isEmpty() ? null : pViews.get(0);
    }


    private static void searchNoBlockView(ViewCache pViews, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childAt = viewGroup.getChildAt(i);
            if (!isViewCovered(i, viewGroup)) {
                pViews.addView(childAt);
                if (childAt instanceof ViewGroup) {
                    searchNoBlockView(pViews, (ViewGroup) childAt);
                }
                if (pViews.isOverflow()) {
                    return;
                }
            }
        }
    }

    public static String STR = "";

    private static boolean isViewCovered(int pTargetIndex, ViewGroup pViewG) {
        View tChild = pViewG.getChildAt(pTargetIndex);
        if (!tChild.isShown()) {
            return true;
        }
        if ((tChild instanceof ViewGroup) && ((ViewGroup) tChild).getChildCount() == 0) {
            return true;
        }

        STR = toStr(tChild, pViewG.getContext());
        Rect tRect = new Rect();
        boolean tResult = tChild.getGlobalVisibleRect(tRect);
        if (!(pViewG instanceof ScrollView)) {
            tResult = tResult && (tRect.bottom - tRect.top >= tChild.getMeasuredHeight());
            tResult = tResult && (tRect.bottom - tRect.top >= tChild.getMeasuredHeight());
            tResult = tResult && (tRect.right - tRect.left >= tChild.getMeasuredWidth());
            if (!tResult) CLog.log(STR + "被父容器遮挡" + toStr(tChild, pViewG.getContext()));
            if (!tResult) return true;
        }
        Rect tR2 = new Rect();
        return isViewCovered(pTargetIndex + 1, pViewG, tRect);
    }

    private static boolean isViewCovered(int pStartPos, ViewGroup pViewG, Rect pBox) {
        Rect tR2 = new Rect();
        int tCount = pViewG.getChildCount();
        for (int i = pStartPos; i < tCount; i++) {
            View sChild = pViewG.getChildAt(i);
            if (sChild.isShown() && sChild.getWidth() != 0 && sChild.getHeight() != 0) {
                sChild.getGlobalVisibleRect(tR2);
                if (tR2.contains(pBox))
                    CLog.log(STR + "被兄弟View遮挡" + toStr(sChild, pViewG.getContext()));
                if (tR2.contains(pBox)) return true;


                if (sChild instanceof ViewGroup) {
                    ViewGroup viewGroup2 = (ViewGroup) sChild;
                    if (viewGroup2.getChildCount() == 0) {
                        continue;
                    } else if (isViewCovered(0, viewGroup2, pBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private static boolean ignoreAct(Context context) {
        String tStr = context.getClass().getName().toLowerCase();
        return tStr.contains("gallery") || tStr.contains("scan") || tStr.contains("picture") || tStr.contains("unity") || tStr.contains("image") || tStr.contains("webview");
    }


    private static void performClick(Context pContext, ViewGroup pViewG) {
        initClickButtonArea(pContext);
        ViewListener tLis = new ViewListener(pContext, pViewG);
        ViewListener tOld = mActOpMap.put(pContext.getClass().getName(), tLis);
        if (tOld != null) tOld.setExpired();
        pViewG.getViewTreeObserver().addOnGlobalLayoutListener(tLis);
    }

    private static String toStr(Context pContext) {
        return " (Activity: " + pContext.getClass().getName() + ")";
    }

    private static String toStr(View pView, Context pContext) {
        String tStr = " (View: " + pView.getClass().getName();
        if (pView.getId() != View.NO_ID) {
            try {
                tStr += ", id: " + pContext.getResources().getResourceEntryName(pView.getId());
            } catch (Resources.NotFoundException ignored) {
            }
        } else {
            tStr += "(no id)";
        }
        if (pView instanceof TextView) {
            tStr += "(Text:" + ((TextView) pView).getText() + ")";
        }
        return tStr + ")";
    }

    public static void noticeAdSkip(Context context, String pExtra) {
        if (ADBCfg.noticeOnSkipAd())
            Toast.makeText(context, "TA-消灭了一个广告(" + pExtra + ") ", Toast.LENGTH_SHORT).show();

    }

    public static @Nullable View findSkipButton(List<View> pViews) {
        ArrayList<View> tViews = new ArrayList<>();
        for (View sView : pViews) {
            if (sView.isShown() && getSkipButton(sView) != null) tViews.add(sView);
        }
        return getBestAdButton(tViews);
    }

    private static void findSkipButton(ViewGroup pViewG, ViewCache pResult) {
        // 通过遮挡判定来收集可能存在的按钮元素
        View tView;
        for (int i = pViewG.getChildCount() - 1; i >= 0; i--) {
            View sChild = pViewG.getChildAt(i);
            if (sChild == null) continue;
            if (sChild instanceof ViewGroup) {
                findSkipButton((ViewGroup) sChild, pResult);
                if (pResult.isOverflow()) return;
            } else if (sChild.isShown()) {
                if ((tView = getSkipButton(sChild)) != null) pResult.addButton(tView);
            }
        }
    }

    public static @Nullable View getSkipButton(View pView) {
        if (!(pView instanceof TextView)) return null;
        TextView textView = (TextView) pView;

        if (isSkipButton(textView))
            return getClickabelParent(pView, false);
        return null;
    }

    public static boolean isSkipButton(TextView pView) {
        //CLog.log(toStr(pView, pView.getContext()) + "判定开始");
        String tText = pView.getText().toString().trim().replace(" ", "");
        if (tText.length() > 7) {
            //CLog.log(toStr(pView, pView.getContext()) + "未通过长度判定,结束");
            return false;
        }

        boolean tbutton = false;
        for (String sStr : HookADBlock.STR_SKIP)
            if (tText.contains(sStr)) {
                //CLog.log(toStr(pView, pView.getContext()) + "通过文本判定");
                tbutton = true;
                break;
            }

        if (!tbutton) CLog.log(toStr(pView, pView.getContext()) + "未通过文本判定");
        if ((!tbutton) && (!HookADBlock.STR_TIME_SKIP.matcher(tText).matches())) {
            //CLog.log(toStr(pView, pView.getContext()) + "未通过时间文本判定,结束");
            return false;
        }

        Rect tRect = new Rect();
        pView.getGlobalVisibleRect(tRect);
        for (Rect sRect : mSkipButtonArea) {
            if (tRect.left > mAreaRightTop.x && tRect.bottom < mAreaRightTop.y) {
                //CLog.log(toStr(pView, pView.getContext()) + "通过区域判定1");
                return true;
            }
            if (tRect.left > mAreaRightBottom.x && tRect.top > mAreaRightBottom.y) {
                //CLog.log(toStr(pView, pView.getContext()) + "通过区域判定2");
                return true;
            }
            if (tRect.right < mAreaLeftBottom.x && tRect.top > mAreaLeftBottom.y) {
                //CLog.log(toStr(pView, pView.getContext()) + "通过区域判定3");
                return true;
            }
        }
        //CLog.log(toStr(pView, pView.getContext()) + "未通过区域判定_结束");
        return false;
    }

    private static View getClickabelParent(View pView, boolean pAllowInvisible) {
        if (pView == null) return null;
        if (pView.isClickable()) return pView;

        if (pView.isShown() || pAllowInvisible) {
            ViewParent pParent = pView.getParent();
            if (pParent instanceof View) {
                return getClickabelParent((View) pParent, pAllowInvisible);
            }
        }
        return null;
    }

    public static ViewGroup getViewGroup(Activity activity) {
        return (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
    }

    private static Point mAreaRightTop;
    private static Point mAreaLeftBottom;
    private static Point mAreaRightBottom;

    private static void initClickButtonArea(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int tW = displayMetrics.widthPixels;
        int tH = displayMetrics.heightPixels;


        Rect tRect;
        mSkipButtonArea.add(tRect = new Rect((int) (tW * 0.6f), 0, tW, (int) (tH * 0.25f))); //right top
        mAreaRightTop = new Point(tRect.left, tRect.bottom);
        mSkipButtonArea.add(tRect = new Rect(0, (int) (tH * 0.75), (int) (tW * 0.4), tH)); //left bottom
        mAreaLeftBottom = new Point(tRect.right, tRect.top);
        mSkipButtonArea.add(tRect = new Rect((int) (tW * 0.6f), (int) (tH * 0.75), tW, tH)); //right bottom
        mAreaRightBottom = new Point(tRect.left, tRect.top);

    }

    public static void removeFromOpList(Context context) {
        mActOpMap.remove(context.getClass().getName());
    }


    private static void hookActOnPause(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam pMParam) {
                if (!ADBCfg.skipAd()) return;

                Activity activity = (Activity) pMParam.thisObject;
                String name = activity.getClass().getName();
                ViewListener tOld = mActOpMap.remove(name);
                if (tOld != null) tOld.setExpired();

                //CLog.log("onPause:" + toStr(activity));
            }
        });
    }

    static class ViewListener implements ViewTreeObserver.OnGlobalLayoutListener {

        private Context mCon;
        private final ViewGroup mViewG;
        public final long mAddTime = System.currentTimeMillis();
        private boolean mExpired = false;

        public ViewListener(Context pCon, ViewGroup pViewG) {
            this.mCon = pCon;
            this.mViewG = pViewG;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - this.mAddTime > 15000 || this.mExpired;
        }

        public void setExpired() {
            this.mExpired = true;
        }

        @Override
        public void onGlobalLayout() {
            if (this.isExpired()) {
                //CLog.log("over time：" + toStr(mCon));
                mViewG.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mCon = null;
                return;
            }

            final AdRule tCacheR = mActRuleCache.get(mCon.getClass().getName());
            int pViewId = tCacheR == null ? View.NO_ID : tCacheR.getViewId();
            View tButton = null;
            if (pViewId != View.NO_ID) {
                tButton = mViewG.findViewById(pViewId);
                if (tButton != null) {
                    CLog.log("从缓存规则获取到按钮: " + toStr(tButton, mCon));
                }
            }

            if (tButton == null) {
                ViewCache tCacheV = new ViewCache();
                findSkipButton(mViewG, tCacheV);
                if (!tCacheV.getButtonViews().isEmpty()) {
//                    CLog.log("收集到的按钮: ");
//                    for(View sView : tCacheV.getButtonViews()){
//                        CLog.log(toStr(sView,mCon));
//                    }
                }
                tButton = getBestAdButton(tCacheV.getButtonViews());
                if (tButton != null) {
                    CLog.log("实时搜索获取到了按钮:" + toStr(tButton, mCon));
                }
            }
            if (tButton == null) {
                //else CLog.log("未点击, View is null" + toStr(mCon));
                return;
            }

            boolean tSuccess;
            AdRule tTraceR;
            boolean tTrack = tCacheR == null || tCacheR.mSType != SkipType.BAN;
            if (tTrack) {
                TrackHelper.startTrack(mCon, new AdRule(tButton), (pRule) -> replaceRule(tCacheR, pRule));
            }

            try {
                tSuccess = tButton.performClick();
            } finally {
                if (tTrack) TrackHelper.finishTrack();
            }

            CLog.log("点击" + toStr(tButton, mCon) + " Result: " + tSuccess + toStr(mCon));
            if (tSuccess) {
                mHandler.postDelayed(tButton::performClick, 250L); //防止检测多点两次
                noticeAdSkip(mCon, "click");
                removeFromOpList(mCon);
                mViewG.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!tTrack && tButton.getId() != View.NO_ID) {
                    replaceRule(tCacheR, new AdRule(tButton));
                }
            }
        }

        private void replaceRule(AdRule pCache, AdRule pNew) {
            CLog.log("Cache: " + pCache + "|New: " + pNew);
            AdRule tSaveT = null;
            if (pCache == null) {
                tSaveT = pNew;
            } else {
                if (pCache.mSType == SkipType.BAN)
                    pNew.mSType = SkipType.BAN;

                if (!pCache.equals(pNew)) tSaveT = pNew;
            }

            if (tSaveT != null) {
                CLog.log("保存新配置: " + tSaveT + toStr(mCon));
                String tKey = mCon.getClass().getName();
                mActRuleCache.put(tKey, tSaveT);
                ADBCfg.saveAppRule(tKey, tSaveT.paserToJson());
            }
        }
    }

}
