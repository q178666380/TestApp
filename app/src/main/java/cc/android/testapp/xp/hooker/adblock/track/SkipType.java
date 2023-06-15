package cc.android.testapp.xp.hooker.adblock.track;

public enum SkipType {
    NO_OP,
    CLICK_BUTTON,
    FINISH_ACT,
    START_ACT,
    /**
     * 禁止执行此类操作,只允许按钮点击
     */
    BAN
}
