package com.sam_chordas.android.stockhawk.data.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class QuoteQueryResponse {

    @SerializedName("query")
    private Query query;

    static class Results{
        @SerializedName("quote")
        private List<QuoteResponse> quotes;

    }

    static class Query{
        @SerializedName("results")
        private Results results;

    }

    public List<QuoteResponse> getQuotes(){
        List<QuoteResponse> quotes = null;
        if(query!=null && query.results!=null && query.results.quotes!=null){
            quotes = query.results.quotes;
        }else{
            quotes = new ArrayList<>();
        }

        return quotes;
    }

}
