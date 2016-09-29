package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

public class StockIntentService extends IntentService {

  public StockIntentService(){
    super(StockIntentService.class.getName());
  }

  public StockIntentService(String name) {
    super(name);
  }

  @Override protected void onHandleIntent(Intent intent) {
    Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
    Bundle args = new Bundle();
    if (intent.getStringExtra("tag").equals("add")){
      args.putString("symbol", intent.getStringExtra("symbol"));
    }

    // We can call OnRunTask from the intent service to force it to run immediately instead of
    // scheduling a task.
    Task task = new OneoffTask.Builder()
            .setService(StockTaskService.class)
            .setTag(intent.getStringExtra("tag"))
            .setExtras(args)
            .setExecutionWindow(0, 1)
            .setUpdateCurrent(false)
            .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
            .setRequiresCharging(false)
            .build();

    GcmNetworkManager.getInstance(this).schedule(task);
  }
}
