package com.dataxy.sample;

import android.app.Application;
import android.content.Context;

import com.dataxy.DataXY;

/**
 * Application DataXY
 */
public class DataXYApplication extends Application {

    // Constant
    public static final String DATAXY_ID = "your dataxy_id"; // fill it,  Contact Commercial support:** vosdonnees@adhslx.com

    @Override
    public void onCreate() {
        super.onCreate();

        // initialize DataXY
        final Context context = getApplicationContext();
        DataXY.initialize(context, DATAXY_ID);
        DataXY.activateDebugMode(true);
    }
}