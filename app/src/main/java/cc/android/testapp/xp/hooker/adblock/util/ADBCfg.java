package cc.android.testapp.xp.hooker.adblock.util;

import cc.android.testapp.cfg.Config;
import cc.android.testapp.cfg.ConfigHelper;

public class ADBCfg {

    public static String PATH_SAVE_RULE1 = "/save_rule";
    public static String PATH_READ_RULE1 = "/read_rule";
    public static String KEY_ENABLE_ADB = "enable_adb";
    public static String KEY_WHITLE_NAME = "white_name";
    public static String KEY_ENABLE_SKIP_TOAST = "enable_adb_toast";


    public static String getAppRule(String pAct) {
        return ConfigHelper.resloveConfig(PATH_READ_RULE1, "an", pAct);
    }

    public static String saveAppRule(String pAct, String pRule) {
        return ConfigHelper.resloveConfig(PATH_SAVE_RULE1, new String[]{"an", "data"}, pAct, pRule);
    }

    public static boolean isWhiteName() {
        return Config.getConfig().optBoolean(KEY_WHITLE_NAME, false);
    }

    public static boolean skipAd() {
        return Config.getConfig().optBoolean(KEY_ENABLE_ADB, true) && !isWhiteName();
    }

    public static boolean noticeOnSkipAd() {
        return Config.getConfig().optBoolean(KEY_ENABLE_SKIP_TOAST, true);
    }


}
