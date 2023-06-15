package cc.android.testapp.act;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class NoneActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();

    }
}
