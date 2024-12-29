# Keep all public classes and methods in the feat_ble package
-keep class com.mwkg.ocr.** { *; }

# Keep specific classes and their public members (if applicable)
-keep class com.mwkg.ocr.model.HiOcrResult { *; }
-keep class com.mwkg.ocr.model.HiCardNumber { *; }
-keep class com.mwkg.ocr.util.HiCardScanner { *; }
-keep class com.mwkg.ocr.view.HiCardNumberListActivity { *; }
-keep class com.mwkg.ocr.viewmodel.HiCardNumberListViewModel { *; }

# Keep all annotations in the library
-keepattributes *Annotation*

# Keep the method parameters and signatures
-keepattributes Signature, MethodParameters

# Preserve Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    private void readObjectNoData();
}