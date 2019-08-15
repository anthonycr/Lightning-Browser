package net.i2p.util;

/*
 * public domain
 *
 */

/**
 * bridge to android logging
 *
 * @author zzz
 */
class AndroidLogWriter extends LogWriter {
    public AndroidLogWriter(LogManager manager) {
        super(manager);
    }

    public String currentFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeRecord(LogRecord rec, String s) {
        //noinspection ThrowableResultOfMethodCallIgnored
        if (rec.getThrowable() == null)
            log(rec.getPriority(), rec.getSource(), rec.getSourceName(), rec.getThreadName(), rec.getMessage());
        else
            log(rec.getPriority(), rec.getSource(), rec.getSourceName(), rec.getThreadName(), rec.getMessage(), rec.getThrowable());
    }

    @Override
    protected void writeRecord(int priority, String s) {
        android.util.Log.println(toAndroidLevel(priority), ANDROID_LOG_TAG, s);
    }

    @Override
    protected void flushWriter() {
        // nop
    }

    @Override
    protected void closeWriter() {
        // nop
    }

    private static final String ANDROID_LOG_TAG = "I2P";

    public void log(int priority, Class<?> src, String name, String threadName, String msg) {
            if (src != null) {
                String tag = src.getName();
                int dot = tag.lastIndexOf(".");
                if (dot >= 0)
                    tag = tag.substring(dot + 1);
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         tag +
                                         " [" + threadName + "] " + msg);
            } else if (name != null)
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         name +
                                         " ["  + threadName + "] " + msg);
            else
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         '[' + threadName + "] " + msg);
    }

    public void log(int priority, Class<?> src, String name, String threadName, String msg, Throwable t) {
            if (src != null) {
                String tag = src.getName();
                int dot = tag.lastIndexOf(".");
                if (dot >= 0)
                    tag = tag.substring(dot + 1);
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         tag +
                                         " [" + threadName + "] " + msg +
                                         ' ' + t.toString() + ' ' + android.util.Log.getStackTraceString(t));
            } else if (name != null)
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         name +
                                         " [" + threadName + "] " + msg +
                                         ' ' + t.toString() + ' ' + android.util.Log.getStackTraceString(t));
            else
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         '[' + threadName + "] " +
                                         msg + ' ' + t.toString() + ' ' + android.util.Log.getStackTraceString(t));
    }

    private static int toAndroidLevel(int level) {
        switch (level) {
        case Log.DEBUG:
            return android.util.Log.DEBUG;
        case Log.INFO:
            return android.util.Log.INFO;
        case Log.WARN:
            return android.util.Log.WARN;
        case Log.ERROR:
        case Log.CRIT:
        default:
            return android.util.Log.ERROR;
        }
    }
}
