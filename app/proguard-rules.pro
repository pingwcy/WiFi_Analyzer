# 保留 Kotlin 标准库的集合类层次，防止 R8 破坏继承关系
-keep class kotlin.collections.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.text.** { *; }

# 保留所有继承 Kotlin 集合的类
-keep,allowobfuscation class * extends kotlin.collections.AbstractList
-keep,allowobfuscation class * extends kotlin.collections.AbstractCollection
-keep,allowobfuscation class * extends kotlin.collections.EmptyList

# AndroidX Startup 相关
-keep class androidx.startup.** { *; }
-keep class androidx.lifecycle.ProcessLifecycleInitializer { *; }