package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.text.TextUtils;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.data.api.model.QuoteResponse;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static boolean showPercent = true;

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(List<QuoteResponse> quotes) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();

        if (quotes != null) {
            for (QuoteResponse quote : quotes) {
                ContentProviderOperation op = buildBatchOperation(quote);
                if (op != null) {
                    batchOperations.add(op);
                }
            }

        }

        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        String formattedBidPrice = null;
        try {
            formattedBidPrice = String.format("%.2f", Float.parseFloat(bidPrice));

        } catch (NumberFormatException ex) {
            //
        }

        return formattedBidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String formattedChange = null;

        if (!TextUtils.isEmpty(change) && change.length() > 1) {
            try {
                String weight = change.substring(0, 1);
                String ampersand = "";
                if (isPercentChange) {
                    ampersand = change.substring(change.length() - 1, change.length());
                    change = change.substring(0, change.length() - 1);
                }
                change = change.substring(1, change.length());
                double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
                change = String.format("%.2f", round);
                StringBuffer changeBuffer = new StringBuffer(change);
                changeBuffer.insert(0, weight);
                changeBuffer.append(ampersand);
                formattedChange = changeBuffer.toString();

            } catch (NumberFormatException ex) {
                //
            }

        }

        return formattedChange;
    }

    public static ContentProviderOperation buildBatchOperation(QuoteResponse quote) {
        ContentProviderOperation op = null;

        if (quote.isValid()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                    QuoteProvider.Quotes.CONTENT_URI);

            builder.withValue(QuoteColumns.SYMBOL, quote.getSymbol());
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(quote.getBid()));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(quote.getPercentChange(), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(quote.getChange(), false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            builder.withValue(QuoteColumns.ISUP, quote.isUp() ? 1 : 0);

            op = builder.build();

        }

        return op;
    }
}
