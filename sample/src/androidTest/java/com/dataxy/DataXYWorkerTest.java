package com.dataxy;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;
import okhttp3.OkHttpClient;

public class DataXYWorkerTest {

    private DataXYDataBase mDataBase;

    private static final double LATITUDE_1 = 48.85;
    private static final String LATITUDE_1_JSON = "48.850000";
    private static final double LONGITUDE_1 = 2.33;
    private static final String LONGITUDE_1_JSON = "2.330000";
    private static final double ALTITUDE_1 = 35;
    private static final long DATE_1 = 557798400386L;
    private static final String DATE_1_JSON = "557798400.386";
    private static final float ACCURACY_1 = 22;
    private static final String PROVIDERS_1 = "[compas,boussole]";
    private static final String PROVIDERS_2 = "[compas]";
    private static final String PROVIDERS_3 = "[boussole]";

    private static final String JSON_1 = "{\"" + DATE_1_JSON + "\":{\"lat\":" + LATITUDE_1_JSON + ",\"long\":" + LONGITUDE_1_JSON + ",\"loc_providers\":\"" + PROVIDERS_1 + "\",\"alt\":" + ALTITUDE_1 + ",\"horizontal_accuracy\":" + ACCURACY_1 + "}}";
    private static final String JSON_2 = "{\"" + DATE_1_JSON + "\":{\"lat\":" + LATITUDE_1_JSON + ",\"long\":" + LONGITUDE_1_JSON + ",\"loc_providers\":\"" + PROVIDERS_2 + "\",\"alt\":" + ALTITUDE_1 + ",\"horizontal_accuracy\":" + ACCURACY_1 + "}}";

    private static final Location LOCATION_1 = new Location(PROVIDERS_1);

    static {
        LOCATION_1.setLatitude(LATITUDE_1);
        LOCATION_1.setLongitude(LONGITUDE_1);
        LOCATION_1.setAltitude(ALTITUDE_1);
        LOCATION_1.setAccuracy(ACCURACY_1);
        LOCATION_1.setTime(DATE_1);
    }

    private String resultData = null;

    @Before
    public void setUp() {
        mDataBase = new DataXYDataBase(InstrumentationRegistry.getInstrumentation().getTargetContext());
        mDataBase.open();
        byte owners = (byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER);
        mDataBase.deleteAllLocations(owners);
        Assert.assertEquals("database not empty ", 0, mDataBase.getLocationCount(owners));

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        androidx.work.Configuration config = new androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
    }

    @Test
    public void test_sendLocations() {
        DataXYConfiguration configuration = new DataXYConfiguration();
        configuration.mMinimumNbPoints = 1;

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYController.enableAnonymousStudiesLog(context, true);
        DataXYController.enableServerTier(context, false);
        DataXYController.setUrlServerTier(context, null);

        DataXYController.enableServerTier2(context, false);
        DataXYController.setUrlServerTier2(context, null);

        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String data, byte owners, @NonNull Callback callback) {
                Assert.assertEquals(DataXYOwner.MAPPY, owners);
                resultData = data;
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {}
        };

        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.configure(targetContext, configuration);
        mDataBase.insertLocationOwners(LOCATION_1, DataXYOwner.MAPPY, PROVIDERS_1);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DataXYWorker.class)
                .build();
        WorkManager.getInstance(targetContext).enqueue(request);

        waitResultData();

        Assert.assertEquals(JSON_1, resultData);
    }


    @Test
    public void test_sendLocations_2() {
        DataXYConfiguration configuration = new DataXYConfiguration();
        configuration.mMinimumNbPoints = 1;
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        DataXYController.enableServerTier(context, false);
        DataXYController.setUrlServerTier(context, null);
        DataXYController.enableAnonymousStudiesLog(context, true);
        DataXYController.enableServerTier2(context, true);
        DataXYController.setUrlServerTier2(context, "http://test.com");

        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String data, byte owners, @NonNull Callback callback) {
                // TODO here this data is  send to DTS here , it's a bug or not
               if (owners == DataXYOwner.MAPPY) {
                   resultData = data;
               }
               callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {}
        };

        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.configure(targetContext, configuration);
        mDataBase.insertLocationOwners(LOCATION_1, DataXYOwner.MAPPY, PROVIDERS_2);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DataXYWorker.class)
                .build();
        WorkManager.getInstance(targetContext).enqueue(request);

        waitResultData();

        Assert.assertEquals(JSON_2, resultData);
    }


    @Test
    public void test_sendLocations_3() {
        DataXYConfiguration configuration = new DataXYConfiguration();
        configuration.mMinimumNbPoints = 1;

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYController.enableAnonymousStudiesLog(context, false);
        DataXYController.enableServerTier(context, true);
        DataXYController.setUrlServerTier(context, "http://test.com");
        DataXYController.enableServerTier2(context, true);
        DataXYController.setUrlServerTier2(context, "http://test.com");

        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String data, byte owners, @NonNull Callback callback) {
                Assert.assertEquals(DataXYOwner.MAPPY, owners);
                resultData = data;
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {}
        };

        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.configure(targetContext, configuration);
        mDataBase.insertLocationOwners(LOCATION_1, DataXYOwner.MAPPY, PROVIDERS_3);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DataXYWorker.class)
                .build();
        WorkManager.getInstance(targetContext).enqueue(request);

        waitResultData();

        Assert.assertEquals(null, resultData);
    }

    private void waitResultData() {
        long now = System.currentTimeMillis();
        while (resultData == null && System.currentTimeMillis() - now < 10000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
