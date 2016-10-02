package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import android.support.design.widget.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.AppPreferences;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.adapter.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import javax.inject.Inject;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = MyStocksActivity.class.getName();

    public static final String KEY = TAG;
    public static final String SHOW_PERCENT_KEY = String.format("%s_%s", KEY, "SHOW_PERCENT");

    private static final int CURSOR_LOADER_ID = 0;

    private ItemTouchHelper mItemTouchHelper;
    private QuoteCursorAdapter mCursorAdapter;
    private TextView noDataTextView;

    @Inject
    AppPreferences appPreferences;
    private CoordinatorLayout mCoordinatorLayout;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stocks);

        ((StocksApplication)getApplication()).getAppComponent().inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        noDataTextView = (TextView) findViewById(R.id.no_data);
        noDataTextView.setVisibility(View.GONE);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        //TODO:
                        // do something on item click
                    }
                }));
        mRecyclerView.setAdapter(mCursorAdapter);
        mRecyclerView.setVisibility(View.VISIBLE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab!=null){
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MaterialDialog.Builder(MyStocksActivity.this).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    onAddSymbol(input.toString());
                                }
                            })
                            .show();

                }
            });
        }


        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        schedulePeriodicUpdate();
    }

    private void schedulePeriodicUpdate() {
        long period = 3600L;
        long flex = 10L;
        String periodicTag = "periodic";

        // create a periodic task to pull stocks once every hour after the app has been opened. This
        // is so Widget data stays up to date.
        PeriodicTask periodicTask = new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(periodicTag)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();
        // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
        // are updated.
        GcmNetworkManager.getInstance(this).schedule(periodicTask);

    }

    private void onAddSymbol(String symbol) {
        if (!TextUtils.isEmpty(symbol)) {
            symbol = symbol.trim().toLowerCase();

            Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                    new String[]{symbol}, null);
            if (c.getCount() != 0) {
                Toast toast =
                        Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                                Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                toast.show();

            } else {
                if (Utils.isConnected(this)) {
                    // Add the stock to DB
                    Intent intent = new Intent(this, StockIntentService.class);
                    intent.putExtra("tag", "add");
                    intent.putExtra("symbol", symbol);
                    startService(intent);

                } else {
                    showNoConnection();
                }

            }

        }

    }

    @Override
    public void onResume() {
        super.onResume();

        reload();
    }

    public void reload(){
        boolean showPercent = appPreferences.isShowPercent();

        mCursorAdapter.setShowPercent(showPercent);
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

    }

    public void showNoConnection() {
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.no_connection, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        reload();

                    }
                });

        snackbar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            boolean showPercent = appPreferences.isShowPercent();
            setShowPercent(!showPercent);

        }

        return super.onOptionsItemSelected(item);
    }

    private void setShowPercent(boolean showPercent) {
        appPreferences.setShowPercent(showPercent);
        mCursorAdapter.setShowPercent(showPercent);

        this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);

    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);

        int count = data.getCount();
        boolean connected = Utils.isConnected(this);
        boolean updated = appPreferences.isUpdated();

        if(!connected){
            showNoConnection();
        }else{
            if(count == 0){
                noDataTextView.setVisibility(View.VISIBLE);

            }else if(!updated){
                // show not up to date
            }

        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void initData(){
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        if (Utils.isConnected(this)) {
            Intent mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra("tag", "init");
            startService(mServiceIntent);
        } else {
            showNoConnection();
        }

    }

}
