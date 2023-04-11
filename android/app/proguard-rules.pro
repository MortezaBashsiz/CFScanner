# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# this line use for prevent obfscate permission library
-keep public class kotlin.reflect.jvm.internal.impl.** { public *; }

# prevent obfscate enum class in retrofit cuse make crash in gson library
-keepclassmembers enum ir.filternet.cfscanner.model.** { *; }
-keepclassmembers enum ir.filternet.cfscanner.db.entity.** { *; }
-keep class ir.filternet.cfscanner.** { *; }
-keep class ir.filternet.cfscanner.db.entity.** { *; }

# keep Yandex class for report
-keep class com.yandex.** { *; }
-keepclassmembers class com.yandex.** { *; }