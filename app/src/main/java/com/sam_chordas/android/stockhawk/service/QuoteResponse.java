package com.sam_chordas.android.stockhawk.service;


import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class QuoteResponse {

    @SerializedName("symbol")
    private String symbol;

    @SerializedName("Bid")
    private String bid;

    @SerializedName("Change")
    private String change;

    @SerializedName("ChangeinPercent")
    private String percentChange;

    public boolean isValid(){
        return !TextUtils.isEmpty(symbol) &&
                !TextUtils.isEmpty(bid) &&
                !TextUtils.isEmpty(change) &&
                !TextUtils.isEmpty(percentChange);
    }

    public boolean isUp(){
        return change.charAt(0) != '-';

    }

    public String getSymbol() {
        return symbol;
    }

    public String getBid() {
        return bid;
    }

    public String getChange() {
        return change;
    }

    public String getPercentChange() {
        return percentChange;
    }

    @Override
    public String toString() {
        return "{" +
                "symbol='" + symbol + '\'' +
                ", bid='" + bid + '\'' +
                ", change='" + change + '\'' +
                ", percentChange='" + percentChange + '\'' +
                '}';
    }
}
