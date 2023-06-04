package cc.android.testapp.xp.hooker.edit_text;

import android.view.View;
import android.widget.TextView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cc.android.testapp.util.Config;
import de.robv.android.xposed.XposedHelpers;

public class ETUtil {

    public static View.OnClickListener getListener(View pView) {
        Object tMethod = XposedHelpers.callMethod(pView, "getListenerInfo");
        return (View.OnClickListener) XposedHelpers.getObjectField(tMethod, "mOnClickListener");
    }

    public static void setHighLight(boolean pEnable) {
        XposedHelpers.setAdditionalStaticField(View.class, Config.KEY_HIGH_LIGHT, pEnable);
    }

    public static boolean canHighLight(@Nullable TextView pView) {
        if (pView != null && Config.KEY_IGNORE.equals(pView.getTag()))
            return false;

        return boolValue(XposedHelpers.getAdditionalStaticField(View.class, Config.KEY_HIGH_LIGHT));
    }

    public static boolean isEnableEdit() {
        return Config.boolValue(Config.KEY_ENABLE_TEXTEDIT);
    }

    public static void switchEditStatus(boolean pEnable) {
        Config.setProp(Config.KEY_ENABLE_TEXTEDIT, pEnable, true);
    }

    public static boolean canEdit(View pView) {
        return pView != null && !Config.KEY_IGNORE.equals(pView.getTag());
    }

    public static <T extends TextView> T ignoreEdit(T pView) {
        if (pView != null) pView.setTag(Config.KEY_IGNORE);
        return pView;
    }

    public static boolean isTextLocked(TextView pView) {
        if (pView != null) {
            Object tLockV = XposedHelpers.getAdditionalInstanceField(pView, Config.KEY_LOCK_TEXTEDIT);
            return tLockV != null;
        }
        return false;
    }

    public static void lockText(TextView pView, @Nullable String pText) {
        if (!canEdit(pView)) return;
        XposedHelpers.setAdditionalInstanceField(pView, Config.KEY_LOCK_TEXTEDIT, pText);
    }

    public static @Nonnull String getLockText(TextView pView) {
        if (pView != null) {
            Object tLockV = XposedHelpers.getAdditionalInstanceField(pView, Config.KEY_LOCK_TEXTEDIT);
            return tLockV == null ? "" : String.valueOf(tLockV);
        }
        return "";
    }

    public static boolean boolValue(Object pV) {
        return (pV instanceof Boolean) && ((Boolean) pV);
    }

}
