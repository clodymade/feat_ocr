package com.mwkg.hiocr;

public class HiOCR {
    static {
        System.loadLibrary("native-lib");
    }

    public native void destroyNativeAnalyzer(long handle);
    public native long createNativeAnalyzer(long scanType, String licenseKey);
    public native String analyzeTextDataNative(long handle, HiTextInfo[] textInfoArray);
    public native String decryptionDataNative(long handle, String input);
}