package com.sam_chordas.android.stockhawk.data.api.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuoteListDeserializer implements JsonDeserializer<List<QuoteResponse>>{
    @Override
    public List<QuoteResponse> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<QuoteResponse> quotes = new ArrayList<>();
        if(json.isJsonArray()){
            JsonArray list = json.getAsJsonArray();
            for(JsonElement element:list){
                quotes.add((QuoteResponse) context.deserialize(element, QuoteResponse.class));
            }
        }else if(json.isJsonObject()){
            quotes.add((QuoteResponse) context.deserialize(json, QuoteResponse.class));
        }else{
            throw new IllegalStateException("Unexpected json type: "+json.getClass());
        }
        return quotes;
    }
}
