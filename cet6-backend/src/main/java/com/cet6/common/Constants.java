package com.cet6.common;

public class Constants {

    public static final String ROLE_VISITOR = "visitor";
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ADMIN = "admin";

    /** 记忆等级：1熟练 2模糊 3遗忘 */
    public static final int MEMORY_MASTER = 1;
    public static final int MEMORY_FUZZY = 2;
    public static final int MEMORY_FORGET = 3;

    /** 题型：1听力 2阅读 3翻译 4写作 */
    public static final int TYPE_LISTENING = 1;
    public static final int TYPE_READING = 2;
    public static final int TYPE_TRANSLATION = 3;
    public static final int TYPE_WRITING = 4;

    /** 计划类型：1背单词 2刷题 */
    public static final int PLAN_WORD = 1;
    public static final int PLAN_QUESTION = 2;

    private Constants() {}
}
