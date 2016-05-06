package com.watch.pulltorefresh.util;

import android.util.Log;

import java.util.Locale;

/**
 * 日志记录类.
 */
@SuppressWarnings("unused")
public class L {

    public static final int LEVEL_VERBOSE = 0;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_INFO = 2;
    public static final int LEVEL_WARNING = 3;
    public static final int LEVEL_ERROR = 4;
    public static final int LEVEL_FATAL = 5;
    private static final String TAG = "wzy-test";

    private static int sLevel = LEVEL_VERBOSE;

    /**
     * Send a VERBOSE log message.
     */
    public static void v(String msg) {
        if (sLevel > LEVEL_VERBOSE) {
            return;
        }
        Log.v(TAG, msg);
    }

    public static void v(String TAG, String msg) {
        if (sLevel > LEVEL_VERBOSE) {
            return;
        }
        Log.v(TAG, msg);
    }

    public static void v(String format, Object... args) {
        if (sLevel > LEVEL_VERBOSE) {
            return;
        }

        if (args.length > 0) {
            format = String.format(Locale.getDefault(), format, args);
        }

        Log.v(TAG, format);
    }

    /**
     * Send a DEBUG log message
     */
    public static void d(String msg) {
        if (sLevel > LEVEL_DEBUG) {
            return;
        }
        Log.d(TAG, msg);
    }

    public static void d(String format, Object... args) {
        if (sLevel > LEVEL_DEBUG) {
            return;
        }

        if (args.length > 0) {
            format = String.format(Locale.getDefault(), format, args);
        }
        Log.d(TAG, format);
    }

    /**
     * Send an INFO log message
     */
    public static void i(String msg) {
        if (sLevel > LEVEL_INFO) {
            return;
        }
        Log.i(TAG, msg);
    }

    public static void i(String format, Object... args) {
        if (sLevel > LEVEL_INFO) {
            return;
        }

        if (args.length > 0) {
            format = String.format(Locale.getDefault(), format, args);
        }
        Log.i(TAG, format);
    }

    /**
     * Send a WARNING log message
     */
    public static void w(String msg) {
        if (sLevel > LEVEL_WARNING) {
            return;
        }
        Log.w(TAG, msg);
    }

    public static void w(String format, Object... args) {
        if (sLevel > LEVEL_WARNING) {
            return;
        }

        if (args.length > 0) {
            format = String.format(Locale.getDefault(), format, args);
        }
        Log.w(TAG, format);
    }

    /**
     * Send an ERROR log message
     */
    public static void e(String msg) {
        if (sLevel > LEVEL_ERROR) {
            return;
        }
        Log.e(TAG, msg);
    }

    public static void e(String format, Object... args) {
        if (sLevel > LEVEL_ERROR) {
            return;
        }

        if (args.length > 0) {
            format = String.format(Locale.getDefault(), format, args);
        }
        Log.e(TAG, format);
    }

    /**
     * Send a FATAL ERROR log message
     */
    public static void wtf(String msg) {
        if (sLevel > LEVEL_FATAL) {
            return;
        }
        Log.wtf(TAG, msg);
    }

    public static void wtf(String format, Object... args) {
        if (sLevel > LEVEL_FATAL) {
            return;
        }

        if (args.length > 0) {
            format = String.format(Locale.getDefault(), format, args);
        }
        Log.wtf(TAG, format);
    }
}
