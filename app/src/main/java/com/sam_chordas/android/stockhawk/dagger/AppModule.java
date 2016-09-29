package com.sam_chordas.android.stockhawk.dagger;


import android.app.Application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sam_chordas.android.stockhawk.data.api.YahooApi;
import com.sam_chordas.android.stockhawk.data.api.model.QuoteListDeserializer;
import com.sam_chordas.android.stockhawk.data.api.model.QuoteResponse;

import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class AppModule {
    public static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Application application;
    private String baseUrl;

    public AppModule(Application application, String baseUrl) {
        this.application = application;
        this.baseUrl = baseUrl;
    }

    @Provides
    @Singleton
    Application providesApplication() {
        return application;
    }

    @Provides
    @Singleton
    Gson provideGson() {
        Type quoteListType = new TypeToken<List<QuoteResponse>>() {}.getType();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(quoteListType, new QuoteListDeserializer())
                .create();

        return gson;
    }

    @Provides
    @Singleton
    OkHttpClient provideYahooHttpClient(Application application) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .cache(new Cache(application.getCacheDir(), HTTP_CACHE_SIZE))
                .build();

    }

    @Provides
    @Singleton
    YahooApi provideYahooApi(Gson gson, OkHttpClient okHttpClient) {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .build();

        return retrofit.create(YahooApi.class);
    }
}
