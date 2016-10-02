package com.sam_chordas.android.stockhawk.data;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AppPreferences {

    public static final String SHOW_PERCENT_KEY = "SHOW_PERCENT";
    public static final String LAST_UPDATED_KEY = "LAST_UPDATED";
    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("");

    private final SharedPreferences prefs;

    public AppPreferences(SharedPreferences prefs){
        this.prefs = prefs;
    }

    public void setShowPercent(boolean showPercent){
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putBoolean(SHOW_PERCENT_KEY, showPercent);
        editor.apply();
    }

    public boolean isShowPercent(){
       return this.prefs.getBoolean(SHOW_PERCENT_KEY, true);
    }

    public void setUpdatedAt(Date date){
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putLong(LAST_UPDATED_KEY, date.getTime());
        editor.apply();

    }

    public Date getUpdatedAt(){
        Date date = null;
        long updatedMillis = this.prefs.getLong(LAST_UPDATED_KEY, -1);
        if(updatedMillis > 0){
            date = new Date(updatedMillis);
        }

        return date;
    }

    public boolean isUpdated(){
        Date updatedDate = getUpdatedAt();
        Date now = new Date();

        // at least 5 min
        return intervalGreaterThan(updatedDate, now, 5*60*1000);
    }

    public boolean intervalGreaterThan(Date start, Date end, long interval){
        boolean greater = false;

        if(start!=null && end!=null &&
                start.before(end)){
            greater = (end.getTime() - start.getTime() - interval) > 0;
        }

        return greater;
    }

}
