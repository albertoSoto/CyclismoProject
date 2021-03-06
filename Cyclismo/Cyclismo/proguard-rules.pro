# 
# Section 1
# Default eclipse project proguard settings.
#
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

-dontpreverify

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
# change from 5 to 2. With 5 passes, the CursorLoader in the android.support.v4 will not compile.
# See http://stackoverflow.com/questions/6605971/android-sdk-tools-revision-12-has-problem-with-proguard-error-conversion-to
-optimizationpasses 2
-allowaccessmodification

-keepattributes *Annotation*
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgent
-keep public class * extends android.preference.Preference
-keep public class * extends android.support.v4.app.Fragment
-keep public class * extends android.app.Fragment
-keep public class com.android.vending.licensing.ILicensingService

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

#
# Section 2
# Google APIs Client Library for Java proguard settings.
#

# Needed by google-api-client to keep generic types and @Key annotations accessed via reflection
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# Needed by Guava
-dontwarn sun.misc.Unsafe

# See https://groups.google.com/forum/#!topic/guava-discuss/YCZzeCiIVoI
-dontwarn com.google.common.collect.MinMaxPriorityQueue

# For Google Play Services
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

# For Google Maps API library
-keepclassmembers class * implements java.io.Serializable {
    private static final long serialVersionUID;
}
-keep public class com.google.googlenav.capabilities.CapabilitiesController*

# 
# Section 3
# My Tracks specific proguard settings.
#

# *Probably* ok to ignore
-dontwarn com.android.common.**
-dontwarn com.google.android.googleapps.**
-dontwarn com.google.android.gsf.**
-dontwarn javax.servlet.**
-dontwarn org.apache.avalon.framework.logger.**
-dontwarn org.apache.log.**
-dontwarn org.apache.log4j.**
-dontwarn org.junit.**
-dontwarn org.mockito.**
-dontwarn net.vidageek.mirror.**
-dontwarn org.codehaus.jackson.**
-dontwarn org.cowboycoders.ant.interfaces.AntTransceiver
-dontwarn org.fluxoid.utils.DrivingDirectionsUtils
-dontwarn android.test.*
-dontwarn org.cowboycoders.ant.utils.UsbUtils
-dontwarn android.test.suitebuilder.*
-dontwarn org.cowboycoders.ant.interfaces.AntTransceiver$UsbReader

-dontwarn org.apache.logging.log4j.**
-keepattributes Signature
-keep class org.apache.logging.log4j.** { *; }

# Make our stack traces useful
# Line numbers will be correct, file names will be replaced by "MT" since the
# class name is enough to get the file name.
-renamesourcefileattribute MT
-keepattributes SourceFile,LineNumberTable

# Keep services needed by ANT+ protocol
-keep public class com.dsi.ant.IAnt_6
-keep public class com.dsi.ant.IAnt
-keep public class com.dsi.ant.IServiceSettings

-keep public class org.cowboycoders.turbotrainers.**

# Only rendering is used for Mapsforge
# See here: https://groups.google.com/forum/#!topic/mapsforge-dev/m6oHx78FPLU
-dontwarn com.caverock.androidsvg.SVGImageView

-dontwarn org.xmlpull.v1.**
