package com.sam_chordas.android.stockhawk.service;

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
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.StocksApplication;

import java.io.IOException;
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
    public static final List<String> DEFAULT_SYMBOLS
            = Arrays.asList(new String[]{"\"YHOO\"", "\"AAPL\"", "\"GOOG\"", "\"MSFT\""});

    private String LOG_TAG = StockTaskService.class.getSimpleName();
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

        boolean isUpdate = false;
        Set<String> symbols = new HashSet<>();

        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
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

        int result = GcmNetworkManager.RESULT_FAILURE;

        if (symbols.isEmpty()) {
            symbols.addAll(DEFAULT_SYMBOLS);
        }

        String query = getSymbolsQuery(symbols);

        if (!TextUtils.isEmpty(query)) {

            try {

                QuoteQueryResponse queryResponse = stocksApi.getQuoteList(query).execute().body();

                result = GcmNetworkManager.RESULT_SUCCESS;

                updateQuotes(queryResponse.getQuotes(), isUpdate);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error fetching quotes", e);

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


    public void updateQuotes(List<QuoteResponse> quotes, boolean isUpdate){
        try {
            // update ISCURRENT to 0 (false) so new data is current
            if (isUpdate) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(QuoteColumns.ISCURRENT, 0);
                mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                        null, null);

            }

            mContext.getContentResolver()
                    .applyBatch(QuoteProvider.AUTHORITY, Utils.quoteJsonToContentVals(quotes));


        } catch (RemoteException | OperationApplicationException e) {
            Log.e(LOG_TAG, "Error applying batch insert", e);
        }
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

}
