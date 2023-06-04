package cc.android.testapp.xp.hooker;

import java.io.File;
import java.util.Objects;

import cc.android.testapp.util.Config;
import cc.commons.util.StringUtil;
import de.robv.android.xposed.XC_MethodHook;

public class HookThread extends Thread {

    private static HookThread INSTANCE = null;

    private long mPreFileTime = 0;
    private ClassLoader mLoader;

    private String mCheckClass = null;
    private String mPreCheckMethod = null;
    private String mCheckMethod = null;
    private XC_MethodHook mMethodHook = null;

    private HookThread(ClassLoader pLoader) {
        this.mLoader = pLoader;
    }

    synchronized public static void runIfNoStart(ClassLoader pLoader) {
        if (INSTANCE == null) {
            INSTANCE = new HookThread(pLoader);
            INSTANCE.start();
        }
    }

    @Override
    public void run() {
        super.run();

        try {
            File tFile = Config.getConfigFile();
            if (tFile.exists() && this.mPreFileTime != tFile.lastModified()) {
                this.mPreFileTime = tFile.lastModified();

                this.mCheckClass = Config.stringValue(Config.KEY_CHECK_CLASS, null);
                if (StringUtil.isBlank(this.mCheckClass)) this.mCheckClass = null;

                this.mCheckMethod = Config.stringValue(Config.KEY_CHECK_METHOD, null);
                if (StringUtil.isBlank(this.mCheckMethod)) this.mCheckMethod = null;
            }

            if (this.mCheckClass != null) {

                this.mCheckClass = null;
            }

            if (!Objects.equals(this.mCheckMethod, this.mPreCheckMethod)) {
                if (this.mCheckMethod != null) {
                    if (this.mMethodHook != null) {
                    }//清除hook

                    this.mPreCheckMethod = this.mCheckMethod;
                } else {
                    // 清除Hook
                }
            }


            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
