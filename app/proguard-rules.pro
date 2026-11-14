# ============================================================================
# EdgeCOS Android ProGuard / R8 规则
# ============================================================================
# 目标：
# 1. release 构建移除所有 android.util.Log 调用，避免敏感信息泄露到 Logcat
# 2. 保留涉及反射的库（JJWT、Moshi）所需类，防止混淆后失效
# 3. 保留崩溃堆栈行号，便于线上问题排查
# ============================================================================

# ---- 保留崩溃堆栈行号 ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- 保留泛型签名（Moshi / Retrofit 反射需要） ----
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations

# ----------------------------------------------------------------------------
# 移除 Log 调用：release 包中所有 android.util.Log 的调试日志都被 R8 删除
# （assumenosideeffects 假设方法无副作用，可安全删除返回值未被使用的调用）
# ----------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** println(...);
}

# ----------------------------------------------------------------------------
# JJWT (io.jsonwebtoken)：使用反射解析 JWT，必须保留
# 参考：https://github.com/jwtk/jjwt#reflection
# ----------------------------------------------------------------------------
-keep class io.jsonwebtoken.** { *; }
-keep class io.jsonwebtoken.impl.** { *; }
-keep class io.jsonwebtoken.impl.compression.** { *; }
-keep class io.jsonwebtoken.impl.crypto.** { *; }
-keep class io.jsonwebtoken.impl.security.** { *; }
-keep class io.jsonwebtoken.security.** { *; }
-keep class io.jsonwebtoken.jackson.** { *; }
# JJWT 通过 ServiceLoader 加载实现类，保留 META-INF/services
-keep class META-INF.services.** { *; }
-keepclassmembers class * {
    @io.jsonwebtoken.** *;
}

# ----------------------------------------------------------------------------
# Moshi：KotlinJsonAdapterFactory 使用 Kotlin 反射
# 保留所有 @JsonClass 标注的 DTO 类及其字段
# ----------------------------------------------------------------------------
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.squareup.moshi.** { *; }
-keep class com.squareup.moshi.kotlin.reflect.** { *; }
# 保留 data class 的所有字段（Retrofit + Moshi 反序列化需要）
-keepclassmembers class com.edgecos.mobileapp.data.remote.dto.** { *; }
-keep class com.edgecos.mobileapp.data.remote.dto.** { *; }

# ----------------------------------------------------------------------------
# Retrofit / OkHttp：库自带 consumer-rules，但保险起见保留关键类
# ----------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# ----------------------------------------------------------------------------
# OkHttp 平台相关（Platform$Android10 等）
# ----------------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ----------------------------------------------------------------------------
# Kotlin 元数据：保留 @Metadata 以支持 Kotlin 反射
# ----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# ----------------------------------------------------------------------------
# Room：生成的实现类（已通过 KSP 添加 keep 规则，此处兜底）
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ----------------------------------------------------------------------------
# Compose：默认配置已足够，无需额外规则
# ----------------------------------------------------------------------------
