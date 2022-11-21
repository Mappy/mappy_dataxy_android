package com.dataxy;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import com.dataxy.sample.DataXYApplication;
import com.dataxy.sample.DataXYSampleActivity;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.dataxy.ViewActionUtils.waitAtLeast;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.any;

@RunWith(BlockJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataXYSampleTest extends Junit4InstrumentationTestCase<DataXYSampleActivity> {

    private static final String MOCK_PROVIDER_NAME = "test";
    private static final long PERMISSION_TIMEOUT = 5000;

    private LocationManager mLocationManager;
    private PendingIntent mPendingIntent;

    @Nullable
    private DataXYSenderIdlingResource mIdlingResource;

    private Context mContext;
    private int countWait = 0;

    public DataXYSampleTest() {
        super(DataXYSampleActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        getActivity();
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXY.configure(mContext, new Configuration.Builder()
                .enable(false)
                .build());

        DataXYLocationService.isRunning = true;
        DataXY.enableProfiling(mContext, false);
        DataXY.enableAnonymousStudiesLog(mContext, false);
        DataXY.enableServerTier(mContext, false);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, false);
        DataXY.setUrlServerTier2(mContext, null);
        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext, 120);
        DataXYPreferencesHelper.setIntervalTimeReceiver(mContext, TimeUnit.MINUTES.toMillis(30));

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onView(withText("Ask for permission")).perform(click());

            UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());

            long start = System.currentTimeMillis();

            UiObject authorizePermission = null;
            while ((authorizePermission == null || !authorizePermission.exists()) && (System.currentTimeMillis() - start < PERMISSION_TIMEOUT)) {
                authorizePermission = uiDevice.findObject(new UiSelector().clickable(true).checkable(false).index(1));
                if (authorizePermission != null && authorizePermission.exists()) {
                    authorizePermission.clickAndWaitForNewWindow();
                } else {
                    sleep(300);
                }
            }
            if (authorizePermission == null) {
                Assert.fail();
            }
        }

        Intent intent = new Intent(mContext, DataXYReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1664, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (mLocationManager.getProvider(MOCK_PROVIDER_NAME) != null) {
                mLocationManager.clearTestProviderEnabled(MOCK_PROVIDER_NAME);
                mLocationManager.removeTestProvider(MOCK_PROVIDER_NAME);
            }
        } catch (Exception e) {
            Log.e("TAG", Objects.requireNonNull(e.getMessage()));
        } finally {
            mLocationManager.removeUpdates(mPendingIntent);
        }
        mLocationManager.addTestProvider(MOCK_PROVIDER_NAME, false, false, false, false, true, false, false, 1, 2);
        mLocationManager.setTestProviderEnabled(MOCK_PROVIDER_NAME, true);

        mLocationManager.requestLocationUpdates(MOCK_PROVIDER_NAME, 1L, 0.00001F, mPendingIntent);
        clearDataBase();
    }

    @Override
    public void tearDown() throws Exception {
        DataXYSender.switchPlatform(false, null);
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
            mIdlingResource = null;
        }

        List<DataXYDataManager.DataXYDataHolder> mappySavedData = DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY);
        List<DataXYDataManager.DataXYDataHolder> dtsSavedData = DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER);
        List<DataXYDataManager.DataXYDataHolder> urlServerSavedData = DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2);

        if (dtsSavedData != null) {
            for (DataXYDataManager.DataXYDataHolder holder : dtsSavedData) {
                DataXYDataManager.delete(holder);
            }
        }
        if (mappySavedData != null) {
            for (DataXYDataManager.DataXYDataHolder holder : mappySavedData) {
                DataXYDataManager.delete(holder);
            }
        }
        if (urlServerSavedData != null) {
            for (DataXYDataManager.DataXYDataHolder holder : urlServerSavedData) {
                DataXYDataManager.delete(holder);
            }
        }

        clearDataBase();
        try {
            if (mLocationManager != null) {
                mLocationManager.clearTestProviderEnabled(MOCK_PROVIDER_NAME);
                mLocationManager.removeTestProvider(MOCK_PROVIDER_NAME);
            }
        } catch (Exception e) {
            Log.e("TAG", Objects.requireNonNull(e.getMessage()));
        } finally {
            mLocationManager.removeUpdates(mPendingIntent);
            mLocationManager = null;
        }
        DataXYSender.INSTANCE = new DataXYSender();

        super.tearDown();
    }

    private void clearDataBase() {
        new DataXYDataBase(mContext)
                .open()
                .deleteAllLocations((byte) (DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER))
                .close();
    }

    @Test
    public void test_0_initialization() {
        assertNotNull("must have advertising id to work properly", DataXYSender.sAdvertisingId);
        assertEquals("clientId", "mappy_dataxy_sample", DataXYSender.sClientId);
    }

    @Test
    public void test_0_mockLocation() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);
        final DataXYDataBase dataBase = new DataXYDataBase(mContext)
                .open();

        byte owners = DataXYOwner.getOwners();
        Assert.assertEquals(0, dataBase.getLocationCount(owners));
        dataBase.close();

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);

        dataBase.open();
        Assert.assertEquals("Incorrect received locations count,", 1, dataBase.getLocationCount(owners));

        dataBase.close();
    }

    @Test
    public void test_1_aggregate() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);

        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        byte owners = DataXYOwner.getOwners();
        Assert.assertEquals(0, dataBase.getLocationCount(owners));
        dataBase.close();

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);
        calendar.setTimeInMillis(3456);
        mockLocation(48.2, 2.84, 42, 22, 3.0F, calendar);
        final Calendar calendar2 = Calendar.getInstance();
        calendar2.set(Calendar.MILLISECOND, 0);
        calendar2.setTimeInMillis(5789);
        mockLocation(48.1, 2.84, 42, 22, 2.1F, calendar2);

        dataBase.open();
        Assert.assertEquals("Incorrect received locations count,", 3, dataBase.getLocationCount(owners));

        dataBase.close();
    }

    @Test
    public void test_2_aggregate_exceedMax() {

        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);
        int previousMax = DataXYReceiver.MAX_LOCATIONS;
        int newMax = 2;

        byte owners = DataXYOwner.getOwners();
        DataXYReceiver.MAX_LOCATIONS = newMax;

        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        Assert.assertEquals(0, dataBase.getLocationCount(owners));

        dataBase.close();

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);
        calendar.setTimeInMillis(3456);
        mockLocation(48.2, 2.84, 42, 22, 3.0F, calendar);
        calendar.setTimeInMillis(5789);
        mockLocation(48.1, 2.84, 42, 22, 2.1F, calendar);

        dataBase.open();
        Assert.assertEquals("Incorrect received locations count,", newMax, dataBase.getLocationCount(owners));

        dataBase.close();

        DataXYReceiver.MAX_LOCATIONS = previousMax;
    }

    @Test
    public void test_2_aggregate_close_beforeAMinute() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);
        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        byte owners = DataXYOwner.getOwners();
        Assert.assertEquals(0, dataBase.getLocationCount(owners));

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);
        calendar.setTimeInMillis(3456);
        mockLocation(48.3, 2.85001, 42, 22, 3.0F, calendar);

        Assert.assertEquals("Incorrect received locations count,", 1, dataBase.getLocationCount(owners));

        dataBase.close();
    }

    @Test
    public void test_2_aggregate_close_afterAMinute() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);
        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext, 120);
        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        byte owners = DataXYOwner.getOwners();
        Assert.assertEquals(0, dataBase.getLocationCount(owners));
        dataBase.close();

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);
        calendar.setTimeInMillis(63456);
        mockLocation(48.3, 2.85001, 42, 22, 3.0F, calendar);

        dataBase.open();
        Assert.assertEquals("Incorrect received locations count,", 2, dataBase.getLocationCount(owners));

        dataBase.close();
    }

    @Test
    public void test_2_aggregate_exceedTime() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);
        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String data, byte owners, @NonNull Callback callback) {
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };
        long previousMax = DataXYPreferencesHelper.getIntervalTimeReceiver();
        DataXYPreferencesHelper.setIntervalTimeReceiver(mContext, 4000); // 4 seco

        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        byte owners = DataXYOwner.getOwners();

        Assert.assertEquals(0, dataBase.getLocationCount(owners));

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);
        calendar.setTimeInMillis(3456);
        mockLocation(48.2, 2.84, 42, 22, 3.0F, calendar);
        calendar.setTimeInMillis(5789);
        mockLocation(48.1, 2.84, 42, 22, 2.1F, calendar);

        Assert.assertEquals("Incorrect received locations count,", 0, dataBase.getLocationCount(owners));

        dataBase.close();
        DataXYPreferencesHelper.setIntervalTimeReceiver(mContext, previousMax);
    }

    private int runSendTestHuge(byte owners, String providers, int max) {
        int previousMax = DataXYPreferencesHelper.getMinNbPointsReceiver();
        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext,  max - 1);
        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        Assert.assertEquals(0, dataBase.getLocationCount(owners));

        final Calendar calendar = Calendar.getInstance();
        List<Location> locations = new ArrayList<>();
        calendar.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < max; ++i) {
            calendar.setTimeInMillis(i * 1000);
            Location location = new Location("test");
            location.setAccuracy(22);
            location.setAltitude(i + 42);
            location.setSpeed(2.0f);
            location.setLatitude(48.3 + i * 0.00001);
            location.setLongitude(2.85 + i * 0.00001);
            location.setTime(calendar.getTimeInMillis());
            locations.add(location);
        }
        dataBase.insertLocationOwners(locations, owners, providers);
        calendar.setTimeInMillis(max * 1000);
        mockLocation(48.3 + max * 0.00001, 2.85 + max * 0.00001, 42, 22, 2.0F, calendar);
        int locationCount = dataBase.getLocationCount(owners);
        dataBase.close();

        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext,  previousMax);
        return locationCount;
    }

    private int runSendTest(byte owner, boolean overrideMinLocationsToSend) {
        int previousMax = DataXYPreferencesHelper.getMinNbPointsReceiver();
        if (overrideMinLocationsToSend) {
            DataXYPreferencesHelper.setMinNbPointsReceiver(mContext, 3);
        }
        final DataXYDataBase dataBase = new DataXYDataBase(InstrumentationRegistry.getInstrumentation().getTargetContext())
                .open();

        Assert.assertEquals(0, dataBase.getLocationCount(owner));

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);
        calendar.setTimeInMillis(3456);
        mockLocation(48.2, 2.84, 42, 22, 3.0F, calendar);
        calendar.setTimeInMillis(5789);
        mockLocation(48.1, 2.84, 42, 22, 2.1F, calendar);

        int locationCount = dataBase.getLocationCount(owner);
        if (overrideMinLocationsToSend) {
            DataXYPreferencesHelper.setMinNbPointsReceiver(mContext, previousMax);
        }
        dataBase.close();

        return locationCount;
    }

    @Test
    public void test_3_send_success() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);
        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String locations, byte owners, @NonNull Callback callback) {
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };

        byte owners = DataXYOwner.getOwners();
        int locationCount = runSendTest(owners, true);

        Assert.assertEquals("Incorrect received locations count,", 0, locationCount);
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER));
    }

    @Test
    public void test_3_send_huge_success() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, true);
        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String locations, byte owners, @NonNull Callback callback) {
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };

        byte owners = DataXYOwner.getOwners();
        int locationCount = runSendTestHuge(owners, "[gps]", 5000);

        Assert.assertEquals("Incorrect received locations count,", 0, locationCount);
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER));
    }

    @Test
    public void test_3_send_failure_mappy() {
        DataXYController.enableServerTier(mContext, false);
        DataXYController.setUrlServerTier(mContext, null);

        DataXYController.enableServerTier2(mContext, false);
        DataXYController.setUrlServerTier2(mContext, null);

        DataXYController.enableAnonymousStudiesLog(mContext, true);

        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String locations, byte owner, @NonNull Callback callback) {
                callback.onFailure(owner, new Exception());
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };

        byte owners = DataXYOwner.getOwners();
        int locationCount = runSendTest(owners, true);

        Assert.assertEquals("actual : " + locationCount + ", expected : " + 0, 0, locationCount);
        Assert.assertNotNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER));
    }

    @Test
    public void test_3_send_failure_dts() {
        DataXY.enableServerTier(mContext, true);
        DataXY.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXY.enableServerTier2(mContext, false);
        DataXY.setUrlServerTier2(mContext, null);
        DataXYController.enableAnonymousStudiesLog(mContext, false);
        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String locations, byte owners, @NonNull Callback callback) {
                callback.onFailure(owners, new Exception());
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };

        byte owners = DataXYOwner.getOwners();
        int locationCount = runSendTest(owners, true);

        Assert.assertEquals("Incorrect received locations count,", 0, locationCount);
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY));
        Assert.assertNotNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2));
    }

    @Test
    public void test_3_send_failure_url_server() {
        DataXY.enableServerTier(mContext, false);
        DataXY.setUrlServerTier(mContext, null);
        DataXY.enableServerTier2(mContext, true);
        DataXY.setUrlServerTier2(mContext, "https://testservercoucou.com");
        DataXYController.enableAnonymousStudiesLog(mContext, false);

        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String locations, byte owners, @NonNull Callback callback) {
                callback.onFailure(owners, new Exception());
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };

        byte owners = DataXYOwner.getOwners();
        int locationCount = runSendTest(owners, true);

        Assert.assertEquals("Incorrect received locations count,", 0, locationCount);
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER));
        Assert.assertNotNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2));
    }

    @Test
    public void test_4_send_all() throws Throwable {
        send(true, true, true, "https://testservercoucou.com");
    }

    @Test
    public void test_4_send_dts() throws Throwable {
        send(false, true, false, null);
    }

    @Test
    public void test_4_send_mappy_server() throws Throwable {
        send(true, false, false, null);
    }

    @Test
    public void test_4_send_url_server() throws Throwable {
        send(false, false, true, "https://testservercoucou.com");
    }

    @Test
    public void test_4_send_url_server_not_ok() throws Throwable {
        send(false, false, false, "http://testservercoucou.com");
    }

    @Test
    public void test_4_send_url_server_not_ok_1() throws Throwable {
        send(false, false, false, "test.com");
    }

    @Test
    public void test_4_send_url_server_ok_1() throws Throwable {
        send(false, false, true, "https://testservercoucou.com/fdsfs?");
    }

    @Test
    public void test_4_send_url_server_ok_2() throws Throwable {
        send(false, false, true, "https://testservercoucou.com/");
    }

    @Test
    public void test_4_send_url_server_ok_3() throws Throwable {
        send(false, false, true, "https://testservercoucou.com/dfsdf");
    }

    @Test
    public void test_4_send_nothing() throws Throwable {
        send(false, false, false, null);
    }

    @Test
    public void test_5_huge_send_all() throws Throwable {
        sendHuge(true, true, true);
    }

    @Test
    public void test_5_huge_send_dts() throws Throwable {
        sendHuge(false, true, false);
    }

    @Test
    public void test_5_huge_send_mappy_server() throws Throwable {
        sendHuge(true, false, false);
    }

    @Test
    public void test_6_send_all_scheduler() throws Throwable {
        sendScheduler( 3, true, true, true, "https://test.com");
    }

    @Test
    public void test_6_send_url_server_scheduler() throws Throwable {
        sendScheduler( 3, false, false, true, "https://test.com");
    }

    @Test
    public void test_6_send_nothing_scheduler() throws Throwable {
        sendScheduler( 3, false, false, false, null);
    }

    @Test
    public void test_6_send_mappy_scheduler() throws Throwable {
        sendScheduler( 3, true, false, false, null);
    }

    @Test
    public void test_6_send_dts_scheduler() throws Throwable {
        sendScheduler( 3, false, true, false, null);
    }

    @Test
    public void test_6_send_all_scheduler_2() throws Throwable {
        sendScheduler( 3, true, true, true, "https://test.com");
    }

    @Test
    public void test_6_send_url_server_scheduler_2() throws Throwable {
        sendScheduler(3, false, false, true, "https://test.com");
    }

    @Test
    public void test_6_not_enough_points_to_send_scheduler() throws Throwable {
        sendScheduler(20, true, true, false, null, false, false);
    }

    @Test
    public void test_6_send_after_max_interval_scheduler() throws Throwable {
        sendScheduler(3, true, true, false, null, true, true);
    }

    private void sendScheduler(int minimumNbPoints, boolean enableMappyServer, boolean enableDts, boolean enableUrlServer, String urlServer) throws Throwable {
        sendScheduler(minimumNbPoints, enableMappyServer, enableDts, enableUrlServer, urlServer, true, true);
    }

    private void sendScheduler(int minimumNbPoints, boolean enableMappyServer, boolean enableDts,boolean enableUrlServer, String urlServer, boolean counter, boolean expectHasPoints) throws Throwable {

        androidx.work.Configuration config = new androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, config);

        Configuration.Builder builder = new Configuration.Builder()
                                            .setMinimumPoints(minimumNbPoints)
                                            .enable(true);
        DataXYController.configure(mContext, builder.build());
        DataXYPreferencesHelper.sLastDeliveryTime = System.currentTimeMillis();

        DataXYController.enableServerTier(mContext, enableDts);
        DataXYController.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXYController.enableServerTier2(mContext, enableUrlServer);
        DataXYController.setUrlServerTier2(mContext, urlServer);
        DataXYController.enableAnonymousStudiesLog(mContext, enableMappyServer);

        int nbCounter = 0;
        if (counter) {
            if (enableDts) {
                nbCounter += 1;
            }

            if (enableMappyServer) {
                nbCounter += 1;
            }

            if (enableUrlServer) {
                nbCounter += 1;
            }
        }

        countWait = 0;
        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String data, byte owners, @NonNull Callback callback) {
                countWait++;
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {}
        };
        byte owners = DataXYOwner.getOwners();
        runSendTest(owners, false);

        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DataXYWorker.class)
                .build();
        WorkManager.getInstance(targetContext).enqueue(request);

        waitResult(nbCounter);

        Assert.assertEquals(nbCounter, countWait);

        DataXYController.configure(mContext, new Configuration.Builder()
                .enable(false)
                .build());
    }

    private void waitResult(int nbCounter) {
        long now = System.currentTimeMillis();
        while (countWait != nbCounter && System.currentTimeMillis() - now < 10000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test_7_send_application_version() throws Throwable {
        DataXYController.setApplicationVersion(null); //reset to null
        DataXY.initialize(mContext, "your dts_id"); //reinitialize to get default value
        PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        String version = pInfo.versionName;
        send(true, false, false, null, version);

        DataXY.initialize(mContext, DataXYApplication.DATAXY_ID); //reinitialize to get default value
    }

    @Test
    public void test_7_send_application_version_set() throws Throwable {
        DataXY.setApplicationVersion("new_version");
        send(true, false, false, null, "new_version");
    }

    @Test
    public void test_7_send_application_version_set_no_advertisingId() throws Throwable {
        DataXY.setApplicationVersion("new_version");
        while (DataXYSender.sAdvertisingId == null) {
            Thread.sleep(1L);
        }
        DataXYSender.sAdvertisingId = null;
        send(true, false, false, null, "new_version");

        Assert.assertNotNull("Advertising null", DataXYSender.sAdvertisingId);
    }

    @Test
    public void test_setServerUrl() {
        Assert.assertTrue("set Server https://test.com", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.com"));
        Assert.assertTrue("set Server https://test.com/test", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.com/test"));
        Assert.assertTrue("set Server https://test.com/test?blabla", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.com/test?blabla"));
        Assert.assertTrue("set Server https://test.com/test?blabla=2", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.com/test?blabla=2"));
        Assert.assertTrue("set Server https://test.com/test?bl@+-abla=2", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.com/test?bl@+-abla=2"));
        Assert.assertTrue("set Server https://test.com/test?blabla=https://test.com", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.com/test?blabla=https://test.com"));
        Assert.assertTrue("set Server https://test.com/test?blabla=test&bla=test2", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.com/test?blabla=test&bla=test2"));
        Assert.assertFalse("set Server http://test.com", DataXYPreferencesHelper.setUrlServerTier(mContext, "http://test.com"));
        Assert.assertFalse("set Server test.com", DataXYPreferencesHelper.setUrlServerTier(mContext, "test.com"));
        Assert.assertTrue("set Server https://test.come", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://test.come"));
        Assert.assertFalse("set Server https://.com", DataXYPreferencesHelper.setUrlServerTier(mContext, "https://.com"));
        Assert.assertTrue("set Server null", DataXYPreferencesHelper.setUrlServerTier(mContext, null));
        Assert.assertFalse("set Server empty", DataXYPreferencesHelper.setUrlServerTier(mContext, ""));

    }

    private void send(boolean enableMappyServer, boolean enableDts, boolean enableUrlServer, String urlServer) throws Throwable {
        send(enableMappyServer, enableDts, enableUrlServer, urlServer, true, null, true, true);
    }

    private void send(boolean enableMappyServer, boolean enableDts, boolean enableUrlServer, String urlServer, String version)
            throws Throwable {
        send(enableMappyServer, enableDts, enableUrlServer, urlServer, true, version, true, true);
    }

    private void send(boolean enableMappyServer, boolean enableDts, boolean enableUrlServer, String urlServer, boolean counter, String version, boolean overrideMinimumNbPoints, boolean expectHasPoints)
            throws Throwable {

        DataXYController.enableServerTier(mContext, enableDts);
        DataXYController.setUrlServerTier(mContext, "https://testserverdts.com");
        DataXYController.enableServerTier2(mContext, enableUrlServer);
        DataXYController.setUrlServerTier2(mContext, urlServer);
        DataXYController.enableAnonymousStudiesLog(mContext, enableMappyServer);

        String provider = DataXYLocationServiceUtil.getProvidersList(mContext);

        int nbCounter = 0;
        if (counter) {
            if (enableDts) {
                nbCounter += 1;
            }

            if (enableMappyServer) {
                nbCounter += 1;
            }

            if (enableUrlServer) {
                nbCounter += 1;
            }
        }
        mIdlingResource = new DataXYSenderIdlingResource(nbCounter);

        List<String> errors = new ArrayList<>();
        MockWebServer mockWebServer = new MockWebServer();
        // Schedule some responses.
        mockWebServer.setDispatcher(new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) {

                String path = request.getPath();

                if (version != null) {
                    String partToFind = "application_version=" + version;
                    if (!path.contains(partToFind)) {
                        errors.add("path must contains `" + partToFind + "` but is : " + path);
                    }
                }

                if (path.contains("application_id=mappy_dataxy_sample")) {
                    String expectedBody = expectHasPoints ?
                            "{\"1.123\":{\"lat\":48.300000,\"long\":2.850000,\"loc_providers\":\"" + provider + "\",\"alt\":42.0,\"horizontal_accuracy\":22.0,\"speed\":2.0},\"3.456\":{\"lat\":48.200000,\"long\":2.840000,\"loc_providers\":\"" + provider + "\",\"alt\":42.0,\"horizontal_accuracy\":22.0,\"speed\":3.0},\"5.789\":{\"lat\":48.100000,\"long\":2.840000,\"loc_providers\":\""+ provider + "\",\"alt\":42.0,\"horizontal_accuracy\":22.0,\"speed\":2.1}}"
                            :
                            "{}";

                    String locationJson = readBody(request);

                    if (locationJson.equals(expectedBody)) {
                        mIdlingResource.increaseCounter();
                        return new MockResponse().setResponseCode(200);
                    } else {
                        errors.add("body must be\n" + expectedBody + "\nbut is \n " + locationJson);
                    }
                    return new MockResponse().setResponseCode(204);

                }
                errors.add("unknown request : " + path);
                return new MockResponse().setResponseCode(404);
            }
        });
        mockWebServer.start();

        DataXYSender.switchPlatform(false, mockWebServer.url("/").toString());
        byte owners = DataXYOwner.getOwners();
        runSendTest(owners, true);

        IdlingRegistry.getInstance().register(mIdlingResource);

        onView(isRoot()).perform(waitAtLeast(1000L));
        IdlingRegistry.getInstance().unregister(mIdlingResource);
        mIdlingResource = null;

        Assert.assertTrue(dumpList(errors), errors.isEmpty());

        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER));

        mockWebServer.shutdown();
    }

    private String dumpList(List<String> errors) {
        StringBuilder builder = new StringBuilder("\n");
        for (String error : errors) {
            builder.append(error).append("\n");
        }
        return builder.toString();
    }

    private void sendHuge(boolean enableMappyServer, boolean enableDts, boolean enableUrlServer) throws Throwable {
        DataXYController.enableServerTier(mContext, enableDts);
        DataXYController.setUrlServerTier(mContext, "https://testserverdts.com");

        DataXYController.enableProfiling(mContext, enableMappyServer);
        DataXYController.enableAnonymousStudiesLog(mContext, enableMappyServer);

        DataXYController.enableServerTier2(mContext, enableUrlServer);
        DataXYController.setUrlServerTier2(mContext, enableUrlServer ? "https://testservercoucou.com" : null);

        int nbCounter = 0;
        if (enableDts) {
            nbCounter += 1;
        }

        if (enableMappyServer) {
            nbCounter += 1;
        }

        if (enableUrlServer) {
            nbCounter += 1;
        }
        int max = 100000;
        mIdlingResource = new DataXYSenderIdlingResource(nbCounter);

        MockWebServer mockWebServer = new MockWebServer();
        // Schedule some responses.
        mockWebServer.setDispatcher(new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                mIdlingResource.increaseCounter();
                if (path.contains("application_id=mappy_dataxy_sample")) {
                    String locationJson = readBody(request);
                    Assert.assertEquals(locationJson.split("long").length, (max + 1));
                    return new MockResponse().setResponseCode(204);
                }
                Assert.fail("unknown request : " + path);
                return new MockResponse().setResponseCode(404);
            }
        });
        mockWebServer.start();

        DataXYSender.switchPlatform(false, mockWebServer.url("/").toString());
        byte owners = DataXYOwner.getOwners();
        runSendTestHuge(owners, "[gps]", max);

        IdlingRegistry.getInstance().register(mIdlingResource);

        onView(isRoot()).perform(waitAtLeast(1000));
        IdlingRegistry.getInstance().unregister(mIdlingResource);
        mIdlingResource = null;

        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.MAPPY));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER_2));
        Assert.assertNull(DataXYDataManager.getSavedData(mContext, DataXYOwner.URL_SERVER_TIER));

        mockWebServer.shutdown();
        DataXYSender.switchPlatform(false, null);
    }

    private String readBody(RecordedRequest request) {
        try {
            final InputStream inputStream = request.getBody().inputStream();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void mockLocation(double latitude, double longitude, double altitude, float accuracy, float speed, Calendar calendar) {
        Location location = new Location(MOCK_PROVIDER_NAME);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(altitude);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);
        location.setTime(calendar.getTimeInMillis());
        location.setElapsedRealtimeNanos(calendar.getTimeInMillis() * 1000000);

        mLocationManager.setTestProviderLocation(MOCK_PROVIDER_NAME, location);
        final long millis = 300L;
        onView(isRoot()).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return any(View.class);
            }

            @Override
            public String getDescription() {
                return "wait for at least " + millis + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                uiController.loopMainThreadForAtLeast(millis);
            }
        });
    }
}
