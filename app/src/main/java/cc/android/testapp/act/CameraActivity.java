package cc.android.testapp.act;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cc.android.testapp.R;
import cc.android.testapp.util.RealPathFromUriUtils;
import cc.commons.util.IOUtil;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int REQUEST_IMAGE = 0;
    public static final int CAPTURE_IMAGE = 1;

    protected TextView mImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        this.mImagePath = (TextView) findViewById(R.id.textView_show_image_path);

        this.findViewById(R.id.btn_select_image).setOnClickListener(this);
        this.findViewById(R.id.btn_capture_image).setOnClickListener(this);

        try {
            Intent intent = getIntent();
            String str = intent.getAction();
            if (MediaStore.ACTION_IMAGE_CAPTURE.equals(str)) {
                str = intent.getExtras().getString("output");
                Log.i("233", String.valueOf(str));
            }
        } catch (Throwable exp) {
        }

    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            Button btn = (Button) v;
            if (btn.getId() == R.id.btn_select_image) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);

            } else if (btn.getId() == R.id.btn_capture_image) {
                Intent intent = getIntent();
                intent.setClassName("com.android.camera", "com.android.camera.Camera");
                startActivityForResult(intent, CAPTURE_IMAGE);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE:
                    if (data != null) {
                        String realPathFromUri = RealPathFromUriUtils.getRealPathFromUri(this, data.getData());
                        Log.e("MainActivity", realPathFromUri);
                        this.mImagePath.setText(realPathFromUri);
                        Intent tIntent = new Intent();
                        tIntent.setData(data.getData());
                        this.setResult(Activity.RESULT_OK, tIntent);

                        ClipData tClipData = getIntent().getClipData();
                        if (tClipData != null && tClipData.getItemCount() > 0) {
                            //BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                            ClipData.Item tItem = tClipData.getItemAt(0);
                            if (tItem.getUri() != null) {
                                InputStream tIStream = null;
                                OutputStream tOStream = null;
                                try {
                                    tIStream = getContentResolver().openInputStream(data.getData());
                                    tOStream = getContentResolver().openOutputStream(tItem.getUri());
                                    IOUtil.copy(tIStream,
                                            tOStream);
                                } catch (IOException e) {
                                    Toast.makeText(this, "无法存储文件到指定缓存", Toast.LENGTH_SHORT).show();
                                } finally {
                                    IOUtil.closeStream(tIStream, tOStream);
                                }
                            }
                        }


                        finish();
                    } else {
                        Toast.makeText(this, "图片损坏，请重新选择", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case CAPTURE_IMAGE:
                    this.setResult(Activity.RESULT_OK, this.getIntent());
                    finish();
                    break;
            }
        }
    }
}