package com.sam_chordas.android.stockhawk.dagger;

import com.sam_chordas.android.stockhawk.service.StockTaskService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {


    void inject(StockTaskService stockTaskService);
}
