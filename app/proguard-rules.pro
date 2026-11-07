# OSMDroid
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.osmdroid.** *;
}
-dontwarn org.osmdroid.**

# Coroutines and serialization
-dontwarn kotlinx.coroutines.**
