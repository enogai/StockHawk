package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.data.api.YahooApi;
import com.sam_chordas.android.stockhawk.data.api.model.QuoteQueryResponse;
import com.sam_chordas.android.stockhawk.data.api.model.QuoteResponse;
import com.sam_chordas.android.stockhawk.ui.StocksApplication;
import com.sam_chordas.android.stockhawk.ui.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String TAG = StockTaskService.class.getSimpleName();

    public static final List<String> DEFAULT_SYMBOLS
            = Arrays.asList(new String[]{"\"YHOO\"", "\"AAPL\"", "\"GOOG\"", "\"MSFT\""});

    private Context mContext;

    @Inject
    YahooApi stocksApi;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    public void onCreate(){
        ((StocksApplication) getApplication()).getAppComponent().inject(this);
    }

    @Override
    public int onRunTask(TaskParams params) {
        if (mContext == null) {
            mContext = this;
        }

        int result = GcmNetworkManager.RESULT_FAILURE;

        if(Utils.isConnected(mContext)) {

            boolean isUpdate = false;
            Set<String> symbols = new HashSet<>();

            if (params.getTag().equals("periodic")) {
                isUpdate = true;

                Set<String> storedSymbols = getStoredSymbols();

                symbols.addAll(storedSymbols);

            } else if (params.getTag().equals("add")) {
                // get symbol from params.getExtra and build query
                String symbol = params.getExtras().getString("symbol");
                if (!TextUtils.isEmpty(symbol)) {
                    symbols.add(String.format("\"%s\"", symbol));

                }
            }

            String query = getSymbolsQuery(symbols);

            if (!TextUtils.isEmpty(query)) {

                try {

                    QuoteQueryResponse queryResponse = stocksApi.getQuoteList(query).execute().body();

                    if(queryResponse!=null){
                        ArrayList<ContentProviderOperation> ops = getQuoteListOps(queryResponse.getQuotes(), isUpdate);
                        if(ops!=null && !ops.isEmpty()){
                            mContext.getContentResolver()
                                    .applyBatch(QuoteProvider.AUTHORITY, ops);

                        }

                    }

                    result = GcmNetworkManager.RESULT_SUCCESS;

                } catch (IOException e) {
                    Log.e(TAG, "Error fetching quotes", e);

                }catch (RemoteException | OperationApplicationException e) {
                    Log.e(TAG, "Error updating quotes", e);
                }
            }

        }

        return result;
    }

    public Set<String> getStoredSymbols() {
        Set<String> symbols = new HashSet<>();
        Cursor cursor = null;

        try {
            cursor = mContext
                    .getContentResolver()
                    .query(QuoteProvider.Quotes.CONTENT_URI,
                            new String[]{"Distinct " + QuoteColumns.SYMBOL},
                            null, null, null);

            while (cursor.moveToNext()) {
                symbols.add(String.format("\"%s\"",
                        cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL))));

            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return symbols;
    }


    public ArrayList<ContentProviderOperation> getQuoteListOps(List<QuoteResponse> quotes, boolean isUpdate){
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        if(quotes!=null && !quotes.isEmpty()){
            if (isUpdate) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(QuoteColumns.ISCURRENT, 0);
                ops.add(ContentProviderOperation
                        .newUpdate(QuoteProvider.Quotes.CONTENT_URI)
                        .withValues(contentValues)
                        .build());

            }

            for (QuoteResponse quote : quotes) {
                ContentProviderOperation op = getQuoteOp(quote);
                if (op != null) {
                    ops.add(op);
                }
            }

        }

        return ops;
    }

    public ContentProviderOperation getQuoteOp(QuoteResponse quote) {
        ContentProviderOperation op = null;

        if (quote.isValid()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                    QuoteProvider.Quotes.CONTENT_URI);

            builder.withValue(QuoteColumns.SYMBOL, quote.getSymbol().trim().toLowerCase());
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(quote.getBid()));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(quote.getPercentChange(), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(quote.getChange(), false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            builder.withValue(QuoteColumns.ISUP, quote.isUp() ? 1 : 0);

            op = builder.build();

        }

        return op;
    }

    public String getSymbolsQuery(Set<String> symbols) {
        String query = null;
        if (symbols != null && symbols.size() > 0) {
            String symbolsListText = symbols.toString();
            query = String.format("select * from yahoo.finance.quotes where symbol in ( %s )",
                    symbolsListText.substring(1, symbolsListText.length() - 1).trim());

        }

        return query;
    }



    public String truncateBidPrice(String bidPrice) {
        String formattedBidPrice = null;
        try {
            formattedBidPrice = String.format("%.2f", Float.parseFloat(bidPrice));

        } catch (NumberFormatException ex) {
            //
        }

        return formattedBidPrice;
    }

    public String truncateChange(String change, boolean isPercentChange) {
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


}
