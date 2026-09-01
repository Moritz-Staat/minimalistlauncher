# R8 rules for the release build.
#
# The launcher is one module without reflection of its own, so almost nothing needs keeping;
# the libraries ship their own consumer rules. What is listed here is what would break
# silently rather than loudly.

# Enum names are persisted: settings store `entries.name` and read it back by comparing
# strings. A renamed constant would not crash, it would quietly reset every setting.
-keepclassmembers enum de.moritzstaat.launcher.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# WorkManager builds its workers from the class name stored in its database.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Line numbers make a stack trace from the device readable; the source file name itself is
# not interesting and stays hidden.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
