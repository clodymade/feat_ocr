//
//  native-lib.cpp
//  HiOCR
//
//  Created by netcanis on 4/30/24.
//

#include <jni.h>
#include <string>
#include <vector>
#include <iostream>
#include "HiOCRAnalyzer.h"
#include "HiTextInfo.h"
#include "HiCardInfo.h"



extern "C" JNIEXPORT void JNICALL
Java_com_mwkg_hiocr_HiOCR_destroyNativeAnalyzer(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) {
        std::cerr << "Handle is null" << std::endl;
        return;
    }
    HiOCRAnalyzer* analyzer = reinterpret_cast<HiOCRAnalyzer*>(handle);
    delete analyzer;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mwkg_hiocr_HiOCR_createNativeAnalyzer(JNIEnv* env, jobject thiz, jlong scanType, jstring licenseKey) {
    const char* licenseKeyCStr = env->GetStringUTFChars(licenseKey, nullptr);
    std::string licenseKeyStr(licenseKeyCStr);
    env->ReleaseStringUTFChars(licenseKey, licenseKeyCStr);
    HiOCRAnalyzer* analyzer = new HiOCRAnalyzer(scanType, licenseKeyStr);
    return reinterpret_cast<jlong>(analyzer);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mwkg_hiocr_HiOCR_analyzeTextDataNative(JNIEnv *env,
                                                jobject thiz,
                                                jlong handle,
                                                jobjectArray textInfoArray) {
    if (handle == 0) {
        std::cerr << "Handle is null" << std::endl;
        return NULL;
    }
    if (textInfoArray == NULL) {
        std::cerr << "Array is null" << std::endl;
        return NULL;
    }

    jclass hiTextInfoClass = env->FindClass("com/mwkg/hiocr/HiTextInfo");
    jclass hiRectClass = env->FindClass("com/mwkg/hiocr/HiRect");
    if (!hiTextInfoClass || !hiRectClass || env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return NULL;
    }

    jmethodID getTextMethod = env->GetMethodID(hiTextInfoClass, "getText", "()Ljava/lang/String;");
    jmethodID getBBoxMethod = env->GetMethodID(hiTextInfoClass, "getBBox", "()Lcom/mwkg/hiocr/HiRect;");
    if (!getTextMethod || !getBBoxMethod) {
        std::cerr << "Method ID is null" << std::endl;
        return NULL;
    }

    // Prepare vector of HiTextInfo pointers
    std::vector<HiTextInfo*> textPointers;
    int count = env->GetArrayLength(textInfoArray);
    for (int i = 0; i < count; ++i) {
        jobject hiTextInfoObj = env->GetObjectArrayElement(textInfoArray, i);
        if (!hiTextInfoObj) {
            std::cerr << "Failed to retrieve HiTextInfo object at index " << i << std::endl;
            continue;
        }

        jobject bboxObj = env->CallObjectMethod(hiTextInfoObj, getBBoxMethod);
        if (!bboxObj || env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            env->DeleteLocalRef(hiTextInfoObj);
            continue;
        }

        jstring text = (jstring) env->CallObjectMethod(hiTextInfoObj, getTextMethod);
        if (!text || env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            env->DeleteLocalRef(hiTextInfoObj);
            env->DeleteLocalRef(bboxObj);
            continue;
        }

        const char* textChars = env->GetStringUTFChars(text, NULL);
        double x = env->GetDoubleField(bboxObj, env->GetFieldID(hiRectClass, "x", "D"));
        double y = env->GetDoubleField(bboxObj, env->GetFieldID(hiRectClass, "y", "D"));
        double width = env->GetDoubleField(bboxObj, env->GetFieldID(hiRectClass, "width", "D"));
        double height = env->GetDoubleField(bboxObj, env->GetFieldID(hiRectClass, "height", "D"));

        // Create HiTextInfo and add to vector
        HiRect rect = {x, y, width, height};
        HiTextInfo* textInfo = new HiTextInfo(std::string(textChars), rect);
        textPointers.push_back(textInfo);

        env->ReleaseStringUTFChars(text, textChars);
        env->DeleteLocalRef(hiTextInfoObj);
        env->DeleteLocalRef(bboxObj);
    }

    // Analyze text data
    HiOCRAnalyzer* analyzer = reinterpret_cast<HiOCRAnalyzer*>(handle);
    std::string result = analyzer->analyzeTextData(textPointers);

    // Clean up dynamically allocated memory
    for (HiTextInfo* textInfo : textPointers) {
        delete textInfo;
    }

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mwkg_hiocr_HiOCR_decryptionDataNative(JNIEnv *env, jobject thiz, jlong handle, jstring input) {
    if (handle == 0) {
        std::cerr << "Handle is null" << std::endl;
        return NULL;
    }
    const char *pcInput = env->GetStringUTFChars(input, nullptr);
    std::string inputStr(pcInput);
    env->ReleaseStringUTFChars(input, pcInput);

    //HiOCRAnalyzer analyzer;
    HiOCRAnalyzer* analyzer = reinterpret_cast<HiOCRAnalyzer*>(handle);
    std::string decrypted = analyzer->decryptionData(inputStr);

    return env->NewStringUTF(decrypted.c_str());
}

