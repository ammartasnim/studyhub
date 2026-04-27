package com.dsi.studyhub.gamification;

public class XpConfig {
    public static final int POST_CREATED     = 30;
    public static final int COMMENT_CREATED  = 10;
    public static final int LIKE_RECEIVED    = 5;
    public static final int LIKE_REMOVED     = -5;
    public static final int DAILY_LOGIN      = 5;
    public static final int DAILY_POST_BONUS = 10;

    // Daily XP cap to prevent spam
//    public static final int DAILY_XP_CAP = 150;
}
