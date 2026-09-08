# kotlinx.serialization ------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
# Keep generated serializers referenced reflectively by the runtime.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers, allowshrinking class **$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class dev.abhinav.tenniscalendar.** { *; }

# Retrofit / OkHttp ----------------------------------------------------------
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Coil -----------------------------------------------------------------------
-dontwarn coil3.**

# WorkManager ----------------------------------------------------------------
# The default WorkerFactory instantiates workers reflectively via this constructor.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
# WorkManager uses Room internally. Room's generated *_Impl database classes are created
# reflectively via their no-arg constructor; without this R8 strips it and the app crashes at
# startup (NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init>).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
-dontwarn androidx.room.paging.**
