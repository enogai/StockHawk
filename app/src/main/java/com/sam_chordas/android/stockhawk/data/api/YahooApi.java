package com.sam_chordas.android.stockhawk.data.api;

import com.sam_chordas.android.stockhawk.data.api.model.QuoteQueryResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YahooApi {

    @GET("/v1/public/yql?format=json&diagnostics=true&env=store://datatables.org/alltableswithkeys&callback=")
    Call<QuoteQueryResponse> getQuoteList(@Query("q") String query);

}
