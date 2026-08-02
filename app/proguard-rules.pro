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
