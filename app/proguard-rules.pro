# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# --- General R8/ProGuard Best Practices ---
# Keep attributes for debugging stack traces. You can uncomment this if you need
# more detailed crash reports in your release builds.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute

# Allow shrinking, optimization, and obfuscation.
# IMPORTANT: In your final release build, remove the '#' from the following lines
# to enable full optimization, shrinking, and obfuscation.
-dontoptimize
-dontshrink
-dontobfuscate
-allowaccessmodification                               # To allow access modification

# Suppress warnings for missing classes/members that are not critical for runtime.
# It's generally better to address specific warnings than to ignore all.
-ignorewarnings
-dontwarn android.webkit.** # WebView warnings
-dontwarn com.google.android.gms.** # Google Play Services warnings
-dontwarn com.bumptech.glide.** # Glide warnings
-dontwarn org.json.** # JSON related warnings
-dontwarn java.lang.invoke.MethodHandle                # Java 8+ method handle warnings
-dontwarn org.apache.http.conn.ssl.BrowserCompatHostnameVerifier # Apache HTTP Client warnings
-dontwarn okio.** # Okio library warnings (often used by network libraries)
-dontwarn sun.misc.Unsafe                               # Warnings about Unsafe operations

# Keep all attributes for serialization/deserialization, important for Gson and Firebase
-keepattributes InnerClasses, Signature, Exceptions

# --- Keep Android Framework Components ---
# Keep all public activities, fragments, services, broadcast receivers, and content providers
# as they are often instantiated by the system or via reflection.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep the R and BuildConfig classes
-keep class **.R$* { *; }
-keep class **.R { *; }
-keep class **.BuildConfig { *; }

# --- Keep specific Activities and their Aliases ---
# Replace 'luminous.organisation.Miya' with your actual package name if different
-keep class luminous.organisation.webgen.MainActivity { *; }
-keep class luminous.organisation.webgen.HomeActivity { *; }

# Keep the Activity Aliases used for dynamic launcher icons
-keep class luminous.organisation.Miya.MainActivityAlias.** { *; }

# --- Gson Library Rules ---
# Keep all fields and methods of classes used with Gson's TypeToken
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements java.lang.reflect.Type { *; } # Broad rule, consider refining if specific issues arise

# If you are using GSON with custom deserializers/serializers, you might need more specific rules
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# --- Firebase Library Rules ---
# General Firebase Database rules
#-keep class com.google.firebase.database.GenericTypeIndicator { *; }
-keep class com.google.firebase.database.** { *; }
#-keep class * extends com.google.firebase.database.GenericTypeIndicator { *; }

# General Firebase core and common modules (including Auth and Messaging)
-keep public class com.google.android.gms.** { public protected *; } # Covers Google Play Services, including Firebase GMS dependencies
-keep public class com.google.firebase.** { *; } # General Firebase classes

# --- Glide Library Rules ---
# Keep Glide-generated classes (usually internal implementations)
-keep class com.bumptech.glide.Glide { *; }
-keep class com.bumptech.glide.request.RequestOptions { *; }
-keep class com.bumptech.glide.load.resource.bitmap.CircleCrop { *; } # For CircleImageView support

# If you use custom Glide modules or app-level RequestOptions
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule
-keep class * extends com.bumptech.glide.GeneratedAppGlideModule

# --- AdMob (Google Mobile Ads) rules ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }

# --- Picasso Library Rules ---
# Rules for com.squareup.picasso:picasso
-keep class com.squareup.picasso.** { *; }
-keep interface com.squareup.picasso.** { *; }

# --- Jsoup Library Rules ---
# Rules for org.jsoup:jsoup
-keep class org.jsoup.** { *; }

# --- OkHttp Library Rules ---
# Rules for com.squareup.okhttp3:okhttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# --- Google API Client Library Rules ---
# Rules for com.google.api-client:google-api-client
# This is a general rule; if you know specific classes you're using,
# you might refine it later to be more granular.
-keep class com.google.api.client.** { *; }
-keep class com.google.api.client.googleapis.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.http.** { *; }

# --- Apache Commons Text (Optional but good to keep) ---
# Rules for org.apache.commons:commons-text
-keep class org.apache.commons.text.** { *; }
