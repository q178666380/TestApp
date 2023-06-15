package cc.android.testapp.xp.hooker.textmodify;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import cc.android.testapp.R;
import cc.android.testapp.xp.MainHook;
import cc.commons.util.StringUtil;

public final class ETDiaLog extends AlertDialog implements View.OnClickListener
        , CompoundButton.OnCheckedChangeListener {

    public final TextView mOwner;
    public final View.OnClickListener mListener;

    public Button mBTN_Apply;
    public Button mBTN_OriginClick;
    public EditText mET_Content;
    public CheckBox mCB_HighLight;
    public CheckBox mCB_LockText;
    public final Context mContent;

    private static final Drawable BG = new ColorDrawable(Color.argb(150, 255, 255, 255));

    public ETDiaLog(Context pContent, TextView pOwner, View.OnClickListener pListener) {
        super(pContent);
        this.mContent = pContent;
        this.mOwner = pOwner;
        this.mListener = pListener;
    }

    public void highLightView(ViewGroup viewGroup) {
        int tIndex = -1;
        while (++tIndex < viewGroup.getChildCount()) {
            View tChild = viewGroup.getChildAt(tIndex);
            if (tChild == null) continue;
            if (tChild instanceof ViewGroup) {
                highLightView((ViewGroup) tChild);
            } else if (tChild instanceof TextView) {
                TextView tView = (TextView) tChild;
                tView.setText(tView.getText().toString());
            }
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        View tView = this.getLayoutInflater().inflate(
                MainHook.TAResource.getLayout(R.layout.edit_text), null);
        if (this.getWindow() != null) this.getWindow().setBackgroundDrawable(BG);
        this.setContentView(tView);

        this.mET_Content = ETCfg.ignoreEdit((EditText) findViewById(R.id.et_edittext_text));
        this.mBTN_Apply = ETCfg.ignoreEdit((Button) findViewById(R.id.btn_edittext_apply));
        this.mBTN_OriginClick = ETCfg.ignoreEdit((Button) findViewById(R.id.btn_edittext_origin_click));
        this.mCB_HighLight = ETCfg.ignoreEdit((CheckBox) findViewById(R.id.cb_edittext_highlight));
        this.mCB_LockText = ETCfg.ignoreEdit((CheckBox) findViewById(R.id.cb_edittext_lock_text));

        this.mCB_HighLight.setChecked(ETCfg.canHighLight(this.mOwner));
        this.mCB_HighLight.setOnCheckedChangeListener(this);
        this.mCB_LockText.setChecked(ETCfg.isTextLocked(this.mOwner));
        this.mBTN_Apply.setOnClickListener(this);

        if (this.mListener == null) this.mBTN_OriginClick.setEnabled(false);
        else this.mBTN_OriginClick.setOnClickListener(this);

        this.mET_Content.setText(this.mOwner.getText().toString());
    }

    @Override
    public void onClick(View pView) {
        if (pView == this.mBTN_Apply) {
            String tStr = this.mET_Content.getText().toString();
            if (StringUtil.isEmpty(tStr)) {
                Toast.makeText(this.getContext(), "未更改", LENGTH_SHORT).show();
                ETDiaLog.this.dismiss();
                return;
            }
            if (mCB_LockText.isChecked()) ETCfg.lockText(this.mOwner, tStr);
            else ETCfg.lockText(this.mOwner, null);
            this.mOwner.setText(new SpannableString(tStr));
            Toast.makeText(this.getContext(), "已应用", LENGTH_SHORT).show();
            ETDiaLog.this.dismiss();
        } else if (pView == this.mBTN_OriginClick) {
            this.mOwner.callOnClick();
            ETDiaLog.this.dismiss();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean pChecked) {
        ETCfg.setHighLight(pChecked);
        this.highLightView((ViewGroup) this.mOwner.getRootView());
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }
}