-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.foldnav.app.** { *; }
-keep class org.maplibre.android.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-dontwarn androidx.window.**
