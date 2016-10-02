package com.sam_chordas.android.stockhawk.ui;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.sam_chordas.android.stockhawk.BuildConfig;
import com.sam_chordas.android.stockhawk.dagger.AppComponent;
import com.sam_chordas.android.stockhawk.dagger.AppModule;
import com.sam_chordas.android.stockhawk.dagger.DaggerAppComponent;

public class StocksApplication extends Application{

    public static final String YAHOO_API_BASE_URL = "https://query.yahooapis.com";
    private AppComponent mComponent;


    @Override
    public void onCreate() {
        super.onCreate();

        if(BuildConfig.DEBUG){
            Stetho.initializeWithDefaults(this);
        }

        mComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this, YAHOO_API_BASE_URL))
                .build();

    }

    public AppComponent getAppComponent() {
        return mComponent;
    }


}
