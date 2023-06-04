package cc.android.testapp;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.zebra.util.Tea;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import cc.android.testapp.permission.PermissionHelper;
import cc.android.testapp.permission.PermissionInterface;
import cc.android.testapp.util.Config;
import cc.android.testapp.xp.hooker.edit_text.ETUtil;
import cc.commons.util.FileUtil;
import cc.commons.util.StringUtil;

public class MainActivity extends AppCompatActivity implements PermissionInterface, View.OnClickListener {

    private EditText mInputFile;

    private EditText mInstallUserId;
    private EditText mInstallApp;

    /**
     * 经度
     */
    private EditText mTE_JD;
    /**
     * 纬度
     */
    private EditText mTE_WD;
    private EditText mAddress;
    private EditText mDeviceId;

    private TextView mTV_HookResult;

    private Button mBTN_locSwitch;

    private File mStorage;

    private PermissionHelper mPermissionHelper;

    private static MainActivity mInstance = null;

    private Random mRan = new Random();

    /**
     * 0:经度,1:纬度
     *
     * @return
     */
    public double[] getRedirectLoc() {
        int i = 0;
        double[] tLoc = new double[2];
        for (TextView sTView : Arrays.asList(this.mTE_JD, this.mTE_WD)) {
            String tContent = sTView.getText().toString();
            if (StringUtil.isBlank(tContent)) {
                tLoc[i] = 0D;
            } else {
                tLoc[i] = Config.shakeNumber(Double.parseDouble(tContent), 1);
                sTView.setText(String.valueOf(tLoc[i]));
            }
            i++;
        }

        return tLoc;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInstance = this;

        this.mInputFile = (EditText) this.findViewById(R.id.editText_edit_file);

        this.mStorage = new File(Environment.getExternalStorageDirectory(), "Download");
        this.mPermissionHelper = new PermissionHelper(this, this);
        this.mPermissionHelper.requestPermissions();

        this.findViewById(R.id.btn_decrypt_file).setOnClickListener(this);
        this.findViewById(R.id.btn_encrypt_file).setOnClickListener(this);

        this.findViewById(R.id.btn_loc_confirm).setOnClickListener(this);
        this.findViewById(R.id.btn_loc_clear).setOnClickListener(this);

        this.findViewById(R.id.btn_class_check).setOnClickListener(this);
        this.findViewById(R.id.btn_method_confirm).setOnClickListener(this);

        (mBTN_locSwitch = (Button) this.findViewById(R.id.btn_loc_switch)).setOnClickListener(this);

        this.mTE_WD = (EditText) this.findViewById(R.id.editText_loc_latitude);
        this.mTE_JD = (EditText) this.findViewById(R.id.editText_loc_longitude);

        this.mAddress = (EditText) this.findViewById(R.id.editText_address);
        this.mDeviceId = (EditText) this.findViewById(R.id.editText_device_id);

        this.mTV_HookResult = (TextView) this.findViewById(R.id.textview_pdahook_result);

        this.updateButtonStat();

        this.updateHookStatus();
        loadConfig();
    }

    public void loadConfig() {
        String tWMCFile = Config.stringValue("WMC_FILE", null);
        if (tWMCFile != null) this.mInputFile.setText(tWMCFile);

        double tValue = Config.doubleValue(Config.KEY_LOCATION_JD);
        if (tValue != 0D) this.mTE_JD.setText(String.valueOf(tValue));
        tValue = Config.doubleValue(Config.KEY_LOCATION_WD);
        if (tValue != 0D) this.mTE_WD.setText(String.valueOf(tValue));
        String tStr = Config.stringValue(Config.KEY_ADDRESS, "");
        if (StringUtil.isNotEmpty(tStr)) this.mAddress.setText(tStr);
        tStr = Config.stringValue(Config.KEY_DEVICE_ID, "");
        if (StringUtil.isNotEmpty(tStr)) this.mDeviceId.setText(tStr);
    }

    public void updateHookStatus() {
        String tMsg = this.isModuleEnabled() ? "模块已经启用" : "模块未启用";
        this.mTV_HookResult.setText(tMsg);
        if (this.isModuleEnabled()) this.mTV_HookResult.setTextColor(Color.parseColor("#FF000000"));
        else this.mTV_HookResult.setTextColor(Color.parseColor("#FFFF0000"));
    }

    /**
     * 使用指定的后缀为输入文件格式化文件名字
     *
     * @param pSuffix 文件名后缀,如果为空则不添加
     * @return 添加了后缀与.wmc的文件名
     */
    public String getFormatSuffixFileName(String pSuffix) {
        String tFile = this.mInputFile.getText().toString().trim();
        if (tFile.isEmpty()) return tFile;

        if (!pSuffix.isEmpty()) pSuffix = "." + pSuffix;

        int tIndex = tFile.indexOf('.');
        if (tIndex != -1) {
            return tFile.substring(0, tIndex) + pSuffix + tFile.substring(tIndex);
        } else {
            return tFile + pSuffix + ".wmc";
        }
    }

    private Uri imageUri;

    public void onClick(View pView) {
        if (pView.getId() == R.id.btn_loc_confirm) {
            double[] tLoc = getRedirectLoc();
            Config.setProp(Config.KEY_LOCATION_JD, tLoc[0], false);
            Config.setProp(Config.KEY_LOCATION_WD, tLoc[1], false);
            Config.setProp(Config.KEY_ADDRESS, this.mAddress.getText(), false);
            Config.setProp(Config.KEY_DEVICE_ID, this.mDeviceId.getText(), true);

            Toast.makeText(this, "已经设置坐标", Toast.LENGTH_SHORT).show();
            return;
        } else if (pView.getId() == R.id.btn_loc_switch) {
            Config.setProp(Config.KEY_ENABLE_LOCATION_CHANGE, !Config.boolValue(Config.KEY_ENABLE_LOCATION_CHANGE), true);
            this.updateButtonStat();
            return;
        } else if (pView.getId() == R.id.btn_loc_clear) {
            this.mTE_JD.setText("119.");
            this.mTE_WD.setText("29.");
            return;
        } else if (pView.getId() == R.id.btn_decrypt_file || pView.getId() == R.id.btn_encrypt_file) {
            handleZebra(pView);
            return;
        } else if (pView.getId() == R.id.btn_class_check) {
            // 输出类所有的方法和字段
        } else if (pView.getId() == R.id.btn_method_confirm) {
            // 输出方法调用栈
        }
    }

    protected void updateButtonStat() {
        this.mBTN_locSwitch.setText("点击" + (Config.boolValue(Config.KEY_ENABLE_LOCATION_CHANGE) ? "禁用" : "启用"));
    }

    protected void handleZebra(View pView) {
        boolean tDecrypt = pView.getId() == R.id.btn_decrypt_file;
        String tInputFile = this.getFormatSuffixFileName(tDecrypt ? "" : "decrypt");
        if (tInputFile.isEmpty()) {
            Toast.makeText(this, "请输入文件名", Toast.LENGTH_LONG).show();
            return;
        }

        File tFile = new File(this.mStorage, tInputFile);
        if (!tFile.isFile()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_LONG).show();
            return;
        }

        byte[] tData;
        try {
            tData = FileUtil.readData(tFile);
        } catch (IOException e) {
            Toast.makeText(this, "读取文件出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        String tOutFile;
        if (tDecrypt) {
            tOutFile = getFormatSuffixFileName("decrypt");
            tData = Tea.decryptUsingTea(tData);
        } else {
            tOutFile = getFormatSuffixFileName("new");
            tData = Tea.encryptUsingTea(tData);
        }

        try {
            FileUtil.writeData(new File(this.mStorage, tOutFile), tData);
            Toast.makeText(this, "已经转储文件", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "写入文件出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }
    }

    @Override
    public int getPermissionsRequestCode() {
        return 0;
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
    }

    @Override
    public void requestPermissionsSuccess() {

    }

    @Override
    public void requestPermissionsFail() {

    }

    public boolean isModuleEnabled() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu pMenu) {
        getMenuInflater().inflate(R.menu.menu, pMenu);

        // 更新按钮状态
        if (Config.boolValue(Config.KEY_ENABLE_TEXTEDIT)) {
            Config.setProp(Config.KEY_ENABLE_TEXTEDIT, false, false);
            onOptionsItemSelected((MenuItem) pMenu.findItem(R.id.menu_enable_edit));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem pItem) {
        if (pItem.getItemId() == R.id.menu_enable_edit) {
            String tTile = null;
            boolean tEnable = ETUtil.isEnableEdit();
            if (tEnable) {
                pItem.setTitle("启用文字编辑");
            } else {
                pItem.setTitle("关闭文字编辑");
            }
            ETUtil.switchEditStatus(!tEnable);
        }
        return super.onOptionsItemSelected(pItem);
    }
}
