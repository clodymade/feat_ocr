package com.mwkg.hiocr;

public class HiTextInfo {
    private final String text;
    private final HiRect bbox;

    public HiTextInfo(String text, HiRect bbox) {
        this.text = text;
        this.bbox = bbox;
    }

    public String getText() {
        return text;
    }
    public HiRect getBBox() {
        return bbox;
    }
}