# Preserve generic and annotation metadata used by Retrofit, Gson and Room.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Keep useful obfuscated crash locations without revealing source filenames.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson fields have explicit wire names so R8 may safely rename Java members.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
