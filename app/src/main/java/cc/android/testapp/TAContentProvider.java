package cc.android.testapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cc.android.testapp.util.CLog;
import cc.android.testapp.cfg.Config;
import cc.android.testapp.cfg.ConfigHelper;
import cc.android.testapp.xp.hooker.adblock.util.ADBCfg;
import cc.commons.util.FileUtil;

public class TAContentProvider extends ContentProvider {

    public static final String SAVEDIR = "/data/data/" + Config.MODULE_PACKAGE_NAME + "/files/AdBlock/";
    private static long mLastReadTime = -1;
    private static String mLastReadConfig = "";

    private String readData(String pPath, String pDir, String pFile) {
        File file = new File(pPath, pDir + "/" + pFile);
        try {
            if (file.exists()) {
                return FileUtil.readContent(file, "UTF-8");
            }
            if (file.getParentFile().mkdirs()) file.createNewFile();
            return "";
        } catch (IOException e) {
            CLog.log("Error on read file: " + file.getAbsolutePath(), e);
            return "";
        }
    }

    private List<String> getWhiteList() {
        File file = new File(SAVEDIR, "white_list");
        ArrayList<String> tList = new ArrayList<>();
        try {
            if (file.exists()) {
                JSONArray tJson = new JSONArray(FileUtil.readContent(file, "UTF-8"));
                for (int i = tJson.length() - 1; i >= 0; i--) {
                    tList.add(String.valueOf(tJson.get(i)));
                }
            } else {
                if (file.getParentFile().mkdirs()) file.createNewFile();
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        tList.add("com.guoshi.httpcanary");
        return tList;
    }

    /* renamed from: ˏ */
    private void saveData(String str, String str2, String str3, String str4) {
        try {
            FileUtil.writeData(new File(str, str2 + "/" + str3), str4.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getType(Uri uri) {
        if (uri.getPath() == null) return null;

        String tSData;
        String tPackName = uri.getQueryParameter("pn");
        String tActName = uri.getQueryParameter("an");
        String tType = uri.getPath();
        if (tType.equals(ADBCfg.PATH_SAVE_RULE1)) {
            tSData = uri.getQueryParameter("data");
            if (tSData != null)
                saveData(SAVEDIR + "rule/", tPackName, tActName, tSData);

        } else if (tType.equals(ADBCfg.PATH_READ_RULE1)) {
            return readData(SAVEDIR + "rule/", tPackName, tActName);
        } else if (tType.equals(ConfigHelper.PATH_CONFIG)) {
            File tFile = Config.getConfigFile();
            try {
                if (tFile.exists()) {
                    if (tFile.lastModified() != mLastReadTime) {
                        mLastReadTime = tFile.lastModified();
                        JSONObject tJson = new JSONObject(FileUtil.readContent(tFile, "UTF-8"));
                        if (tJson.optBoolean(ADBCfg.KEY_ENABLE_ADB, true) && getWhiteList().contains(tPackName)) {
                            tJson.put(ADBCfg.KEY_WHITLE_NAME, true);
                        }
                        return mLastReadConfig = tJson.toString();
                    } else return mLastReadConfig;
                }
                return "{}";
            } catch (JSONException | IOException e) {
                CLog.log("Error on read config for remote", e);
            }
        }

        return null;
    }


    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String
            selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[]
            selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String
            selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
