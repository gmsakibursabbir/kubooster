# KuBooster R8 rules

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.kuduy.kubooster.**$$serializer { *; }
-keepclassmembers class com.kuduy.kubooster.** { *** Companion; }
-keepclasseswithmembers class com.kuduy.kubooster.** { kotlinx.serialization.KSerializer serializer(...); }

# Shizuku — keep binder interfaces
-keep class moe.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
