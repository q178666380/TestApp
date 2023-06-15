package cc.android.testapp.xp.hooker.adblock.core;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import cc.android.testapp.util.CLog;
import cc.android.testapp.xp.hooker.adblock.track.SkipType;
import cc.commons.util.StringUtil;

public class AdRule {
    public static final AdRule NoAD = new AdRule();
    private int mViewId;
    private String mViewClazz;
    public SkipType mSType = SkipType.NO_OP;

    public String mStartIntent = null;

    protected AdRule() {
        this(View.NO_ID, "");
    }

    public AdRule(int pViewId, String pView) {
        this.mViewId = pViewId;
        this.mViewClazz = pView;
        if (this.mViewId != View.NO_ID) {
            mSType = SkipType.CLICK_BUTTON;
        }
    }

    public AdRule(@Nonnull View pView) {
        this(pView.getId(), pView.getClass().getName());
    }


    public int getViewId() {
        return this.mViewId;
    }

    public String getViewClass() {
        return this.mViewClazz;
    }

    public Intent getIntent() {
        try {
            return TextUtils.isEmpty(this.mStartIntent) ? null : Intent.parseUri(this.mStartIntent, 0);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    public String paserToJson() {
        JSONObject tJson = new JSONObject();
        try {
            tJson.put("view_id", this.mViewId);
            tJson.put("skip_type", mSType.name());
            if (!TextUtils.isEmpty(this.mStartIntent)) tJson.put("intent", mStartIntent);
            tJson.put("view_class", this.mViewClazz);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return tJson.toString();
    }

    public static AdRule parseJson(String pJsonStr) {
        try {
            if (StringUtil.isBlank(pJsonStr)) pJsonStr = "{}";
            JSONObject tJson = new JSONObject(pJsonStr);
            AdRule tRule = new AdRule(tJson.optInt("view_id", View.NO_ID)
                    , tJson.optString("view_class", ""));

            tRule.mSType = SkipType.valueOf(tJson.optString("skip_type", tRule.mSType.name()));
            tRule.mStartIntent = tJson.optString("intent", tRule.mStartIntent);
            return tRule;
        } catch (JSONException e) {
            CLog.log("Error on paser config " + e.getMessage());
            return NoAD;
        }
    }

    @Override
    public String toString() {
        return paserToJson();
    }


    @Override
    public boolean equals(Object pObj) {
        if (!(pObj instanceof AdRule)) return false;
        AdRule tRule = (AdRule) pObj;
        return tRule.toString().equals(this.toString());
    }

    public boolean isAd() {
        return this.getViewId() != View.NO_ID;
    }

    public boolean isDirectSkip() {
        return this.mSType == SkipType.START_ACT || this.mSType == SkipType.FINISH_ACT;
    }

    public AdRule copy() {
        return parseJson(this.paserToJson());
    }
}
