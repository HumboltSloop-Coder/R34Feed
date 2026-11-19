# Add project specific ProGuard rules here.
# Keep Retrofit/Kotlinx serialization models if using reflection-based serialization.
-keepattributes *Annotation*,Signature,Exceptions
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.example.r34viewer.**$$serializer { *; }
-keepclassmembers class com.example.r34viewer.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.r34viewer.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn java.lang.invoke.StringConcatFactory
