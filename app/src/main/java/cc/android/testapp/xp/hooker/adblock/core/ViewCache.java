package cc.android.testapp.xp.hooker.adblock.core;

import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cc.android.testapp.util.CLog;

public class ViewCache {
    private int mViewCount;
    private int mTextNum;
    private final List<View> mViews = new ArrayList<>();
    private final List<View> mBtnViews = new ArrayList<>();
    private final List<View> mTViews = new ArrayList<>();

    private static boolean instanceofClass(String str, Class<?> cls) {
        if (cls.equals(Object.class)) {
            return false;
        }
        if (cls.getName().equals(str)) {
            return true;
        }
        return instanceofClass(str, cls.getSuperclass());
    }

    private static boolean isNotADView(View view) {
        if (view.isShown()) {
            return view.getClass().getName().equals("com.tencent.widget.XListView")
                    || view.getClass().getName().equals("com.tencent.widget.Gallery")
                    || (view instanceof EditText) || (view instanceof AdapterView) || (view instanceof CheckBox)
                    || instanceofClass("android.support.v4.view.ViewPager", view.getClass())
                    || instanceofClass("android.support.v7.widget.RecyclerView", view.getClass())
                    || instanceofClass("androidx.viewpager2.widget.ViewPager2", view.getClass())
                    || instanceofClass("androidx.recyclerview.widget.RecyclerView", view.getClass())
                    || instanceofClass("com.google.android.material.floatingactionbutton.FloatingActionButton", view.getClass())
                    || instanceofClass("android.support.design.widget.FloatingActionButton", view.getClass());
        }
        return false;
    }

    public void addView(View view) {
        this.mViews.add(view);
        this.mViewCount++;
        if (isNotADView(view)) {
            this.mViewCount += 20;
        } else if (view instanceof TextView) {
            this.mTextNum++;
            CLog.log("Found TextView: " + ((TextView) view).getText() + "(" + view.getClass().getName() + ")");
        }
    }

    public void addButton(View pView) {
        this.mBtnViews.add(pView);
    }

    public int getViewNum() {
        return this.mViewCount;
    }

    public int getTextNum() {
        return this.mTextNum;
    }

    public List<View> getViews() {
        return this.mViews;
    }

    public List<View> getButtonViews() {
        return this.mBtnViews;
    }

    public boolean isOverflow() {
        return getViewNum() >= 20 || getTextNum() >= 5;
    }
}
