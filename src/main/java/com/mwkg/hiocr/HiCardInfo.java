package com.mwkg.hiocr;

public class HiCardInfo {
    private final String cardNumber;
    private final String expiryDate;
    private final String holderName;
    private final String networkInfo;

    public HiCardInfo(String cardNumber, String expiryDate, String holderName, String networkInfo) {
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.holderName = holderName;
        this.networkInfo = networkInfo;
    }

    public String getCardNumber() {
        return cardNumber;
    }
    public String getExpiryDate() {
        return expiryDate;
    }
    public String getHolderName() {
        return holderName;
    }
    public String getNetworkInfo() {
        return networkInfo;
    }
}
