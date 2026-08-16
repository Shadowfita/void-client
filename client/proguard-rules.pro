# Balanced: obfuscate default-package (project) classes, leave packaged libs intact.

-dontoptimize
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,Exceptions
-renamesourcefileattribute SourceFile

# Entry points
-keep public class Loader {
    public static void main(java.lang.String[]);
}

# Applet lifecycle on any default-package subclass
-keep class * extends java.applet.Applet {
    public void init();
    public void start();
    public void stop();
    public void destroy();
    <init>();
}

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Guice injection targets in project classes
-keepclasseswithmembers class * {
    @com.google.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @com.google.inject.Inject *;
}

# Class373_Sub1 is created reflectively by Class348_Sub18.method2941.
# Preserve the wheel-capable AWT mouse handler and its constructor so ProGuard
# cannot shrink it and silently force the non-wheel Class373_Sub2 fallback.
-keep class Class373_Sub1 {
    <init>(java.awt.Component, boolean);
}

# All packaged classes — treat as untouchable libs
-keep class **.** { *; }
-keepclassmembers class **.** { *; }

# Silence unresolved references inside third-party jars
-dontwarn **
