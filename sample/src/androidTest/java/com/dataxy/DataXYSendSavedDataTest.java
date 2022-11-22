package com.dataxy;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.dataxy.sample.DataXYSampleActivity;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.MethodSorters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.any;

@RunWith(BlockJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataXYSendSavedDataTest extends Junit4InstrumentationTestCase<DataXYSampleActivity> {

    private static final String LATITUDE_1_JSON = "48.850000";
    private static final String LONGITUDE_1_JSON = "2.330000";
    private static final double ALTITUDE_1 = 35;
    private static final double ACCURACY_1 = 22;
    private static final float SPEED_1 = 22F;
    private static final String DATE_1_JSON = "557798400.386";
    private static final String LATITUDE_2_JSON = "49.000000";
    private static final String LONGITUDE_2_JSON = "2.300000";
    private static final double ACCURACY_2 = 27;
    private static final float SPEED_2 = 27;
    private static final String DATE_2_JSON = "557798460.000";

    private static final String SERVER_TIER_HOST = "https://serverhost.com";
    private static final String SERVER_TIER_2_HOST = "https://serverhost2.com";

    private static final String LOCATION_PROVIDERS = "[gps]";

    private static final String JSON_1 = "\"" + DATE_1_JSON + "\":{\"lat\":" + LATITUDE_1_JSON + ",\"long\":" + LONGITUDE_1_JSON + ",\"alt\":" + ALTITUDE_1 + ",\"horizontal_accuracy\":" + ACCURACY_1 + ",\"speed\":" + SPEED_1 + "}";
    private static final String JSON_2 = "\"" + DATE_2_JSON + "\":{\"lat\":" + LATITUDE_2_JSON + ",\"long\":" + LONGITUDE_2_JSON + ",\"horizontal_accuracy\":" + ACCURACY_2 + ",\"speed\":" + SPEED_2 + "}";
    private static final String JSON_LIST = "{" + JSON_1 + "," + JSON_2 + "}";

    private static final String MOCK_PROVIDER_NAME = "test";
    private static final long PERMISSION_TIMEOUT = 5000;

    private LocationManager mLocationManager;
    private PendingIntent mPendingIntent;

    private Context mContext;
    private int sentData = 0;

    public DataXYSendSavedDataTest() {
        super(DataXYSampleActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        getActivity();
        DataXYLocationService.isRunning = true;
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXY.enableProfiling(mContext, true);
        DataXY.enableAnonymousStudiesLog(mContext, true);

        DataXYDataManager.clearSaveData(mContext);

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

        byte owners = (byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER | DataXYOwner.URL_SERVER_TIER_2);
        clearCache(owners);
        clearDataBase();
    }

    @Override
    public void tearDown() throws Exception {
        byte owners = (byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER | DataXYOwner.URL_SERVER_TIER_2);
        clearCache(owners);
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

    @Test
    public void test_sendSavedData_Huge() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.enableServerTier(context, true);
        DataXYPreferencesHelper.setUrlServerTier(context, SERVER_TIER_HOST);
        DataXYPreferencesHelper.setProfilingEnabled(context, false);
        DataXYPreferencesHelper.setStudyEnabled(context, false);
        DataXYPreferencesHelper.enableServerTier2(context, false);
        DataXYPreferencesHelper.setUrlServerTier2(context, null);
        test_sendSavedDataHuge(DataXYOwner.getOwners());
    }

    @Test
    public void test_sendSavedData_Dts() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.enableServerTier(context, true);
        DataXYPreferencesHelper.setUrlServerTier(context, SERVER_TIER_HOST);
        DataXYPreferencesHelper.setProfilingEnabled(context, false);
        DataXYPreferencesHelper.setStudyEnabled(context, false);
        DataXYPreferencesHelper.enableServerTier2(context, false);
        DataXYPreferencesHelper.setUrlServerTier2(context, null);
        test_sendSavedData(DataXYOwner.getOwners());
    }

    @Test
    public void test_sendSavedData_Mappy() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.enableServerTier2(context, false);
        DataXYPreferencesHelper.setUrlServerTier2(context, null);
        DataXYPreferencesHelper.setProfilingEnabled(context, true);
        DataXYPreferencesHelper.setStudyEnabled(context, true);
        DataXYPreferencesHelper.enableServerTier(context, false);
        DataXYPreferencesHelper.setUrlServerTier(context, SERVER_TIER_HOST);
        test_sendSavedData(DataXYOwner.getOwners());
    }

    @Test
    public void test_sendSavedData_UrlServer2() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.enableServerTier(context, false);
        DataXYPreferencesHelper.setUrlServerTier(context, null);
        DataXYPreferencesHelper.setProfilingEnabled(context, false);
        DataXYPreferencesHelper.setStudyEnabled(context, false);
        DataXYPreferencesHelper.enableServerTier2(context, true);
        DataXYPreferencesHelper.setUrlServerTier2(context, SERVER_TIER_2_HOST);
        test_sendSavedData(DataXYOwner.getOwners());
    }
    @Test
    public void test_sendSavedData_All() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.enableServerTier(context, true);
        DataXYPreferencesHelper.setUrlServerTier(context, SERVER_TIER_HOST);
        DataXYPreferencesHelper.setProfilingEnabled(context, true);
        DataXYPreferencesHelper.setStudyEnabled(context, true);
        DataXYPreferencesHelper.enableServerTier2(context, true);
        DataXYPreferencesHelper.setUrlServerTier2(context, SERVER_TIER_2_HOST);
        test_sendSavedData(DataXYOwner.getOwners());
    }

    @Test
    public void test_sendSavedData_Nothing() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYPreferencesHelper.enableServerTier(context, false);
        DataXYPreferencesHelper.enableServerTier2(context, false);

        DataXYPreferencesHelper.setProfilingEnabled(context, false);
        DataXYPreferencesHelper.setStudyEnabled(context, false);
        test_sendSavedData(DataXYOwner.getOwners());
    }

    @Test
    public void test_sendUrl() throws UnsupportedEncodingException {
        DataXYSender.sAdvertisingId = "toto";
        DataXYDeviceIdHelper.overrideForTest = false;
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        HashMap<String, Boolean> map = new HashMap<>();
        DataXYPreferencesHelper.setMAdvertiseEnabled(context, true);
        map.put("geo_test", false);
        map.put("geo_test2", true);
        String expectedLocationStatusParameters = "&statusloc=true&loc_providers=" + URLEncoder.encode(LOCATION_PROVIDERS, "UTF-8");
        setConsent(context, true, true, true, true, map);
        test_SendUrl(SERVER_TIER_HOST + "?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=true&cst_geo_study=true&cst_geo_par=true&cst_geo_test2=true&cst_geo_test=false" + expectedLocationStatusParameters,
                "https://xy.mappyrecette.net/log/1.0?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=true&cst_geo_study=true&cst_geo_par=true&cst_geo_test2=true&cst_geo_test=false" + expectedLocationStatusParameters,
                SERVER_TIER_2_HOST + "?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=true&cst_geo_study=true&cst_geo_par=true&cst_geo_test2=true&cst_geo_test=false" + expectedLocationStatusParameters);

        map.put("geo_test", true);
        map.put("geo_test2", false);

        DataXYPreferencesHelper.setMAdvertiseEnabled(context, false);
        setConsent(context, true, false, false, true, map);
        test_SendUrl(SERVER_TIER_HOST + "?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=false&cst_geo_study=false&cst_geo_par=false&cst_geo_test2=false&cst_geo_test=true" + expectedLocationStatusParameters,
                "https://xy.mappyrecette.net/log/1.0?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=false&cst_geo_study=false&cst_geo_par=false&cst_geo_test2=false&cst_geo_test=true" + expectedLocationStatusParameters,
                SERVER_TIER_2_HOST + "?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=false&cst_geo_study=false&cst_geo_par=false&cst_geo_test2=false&cst_geo_test=true" + expectedLocationStatusParameters);
        map.clear();

        setConsent(context, true, false, false, true, map);
        test_SendUrl(SERVER_TIER_HOST + "?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=false&cst_geo_study=false&cst_geo_par=false" + expectedLocationStatusParameters,
                "https://xy.mappyrecette.net/log/1.0?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=false&cst_geo_study=false&cst_geo_par=false" + expectedLocationStatusParameters,
                SERVER_TIER_2_HOST + "?sdk_version=" + BuildConfig.VERSION_NAME + "&operating_system=android&operating_system_version=" + Build.VERSION.RELEASE  + "&device_id=toto&id=your%20dataxy_id&application_id=mappy_dataxy_sample&application_version=2.0&cst_geo_perso=false&cst_geo_study=false&cst_geo_par=false" + expectedLocationStatusParameters);
        map.clear();
        DataXYDeviceIdHelper.overrideForTest = null;
    }

    @Test
    public void test_sendUrl_With_NoAdvertising() {
        DataXYSender.sAdvertisingId = null;
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        HashMap<String, Boolean> map = new HashMap<>();
        DataXYPreferencesHelper.setMAdvertiseEnabled(context, true);
        map.put("geo_test", false);
        map.put("geo_test2", true);
        setConsent(context, true, true, true, true, map);
        DataXYSender.sIsLocationAvailable = true;
        DataXYSender.sLocationProviders = LOCATION_PROVIDERS;
        DataXYSender.buildUrlMappy(context);
        Assert.assertNotNull("sAdvertisingId is null", DataXYSender.sAdvertisingId);

        DataXYSender.sAdvertisingId = "toto";
        DataXYDeviceIdHelper.overrideForTest = true;
        DataXYSender.buildUrlServerTier(context,  DataXYPreferencesHelper.getUrlServerTier());
        Assert.assertNotNull("sAdvertisingId is null", DataXYSender.sAdvertisingId);
        Assert.assertNotEquals("toto is not different -" + DataXYSender.sAdvertisingId + "-", "toto", DataXYSender.sAdvertisingId);
        DataXYDeviceIdHelper.overrideForTest = null;
    }

    private void test_SendUrl(String expectedDtsUrl, String expectedMappyUrl, String expectedTierServerUrl) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DataXYSender.sIsLocationAvailable = true;
        DataXYSender.sLocationProviders = LOCATION_PROVIDERS;
        String url = DataXYSender.buildUrlServerTier(context, DataXYPreferencesHelper.getUrlServerTier());
        Assert.assertEquals("Wrong URL for DTS", expectedDtsUrl, url);
        url = DataXYSender.buildUrlMappy(context);
        Assert.assertEquals("Wrong URL for Mappy", expectedMappyUrl, url);
        url = DataXYSender.buildUrlServerTier(context, DataXYPreferencesHelper.getUrlServerTier2());
        Assert.assertEquals("Wrong URL for tier server", expectedTierServerUrl, url);
    }

    private void setConsent(Context context, boolean dtsEnabled, boolean profiling, boolean study, boolean enableServerUrl, HashMap<String, Boolean> consent) {
        DataXYPreferencesHelper.enableServerTier(context, dtsEnabled);
        DataXYPreferencesHelper.setUrlServerTier(context, SERVER_TIER_HOST);
        DataXYPreferencesHelper.setProfilingEnabled(context, profiling);
        DataXYPreferencesHelper.setStudyEnabled(context, study);
        DataXYPreferencesHelper.enableServerTier2(context, enableServerUrl);
        DataXYPreferencesHelper.setUrlServerTier2(context, SERVER_TIER_2_HOST);
        DataXYSender.clearConsent();
        for (Map.Entry<String, Boolean> entry : consent.entrySet()) {
            DataXYSender.addConsent(entry.getKey(), entry.getValue());
        }
    }

    void test_sendSavedData(byte owner) {
        int savedDataCount = 0;
        boolean mappy = DataXYOwner.hasMappy(owner);
        if (mappy) {
            savedDataCount += saveData(DataXYOwner.MAPPY);
            savedDataCount += 1;// + 1 because new locations to send
        }
        boolean dtsServer = DataXYOwner.hasUrlServerTier(owner);
        if (dtsServer) {
            savedDataCount += saveData(DataXYOwner.URL_SERVER_TIER);
            savedDataCount += 1;// + 1 because new locations to send
        }
        boolean urlServer = DataXYOwner.hasUrlServerTier2(owner);
        if (urlServer) {
            savedDataCount += saveData(DataXYOwner.URL_SERVER_TIER_2);
            savedDataCount += 1;// + 1 because new locations to send
        }
        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String locations, @NonNull byte owners, @NonNull Callback callback) {
                ++sentData;
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };

        runSendTest(owner);
        waitWhileSending(owner);

        Assert.assertEquals(savedDataCount, sentData); // + 1 because new locations to send
    }

    void test_sendSavedDataHuge(byte owner) {
        int savedDataCount = 0;
        boolean mappy = DataXYOwner.hasMappy(owner);
        if (mappy) {
            savedDataCount += saveData(DataXYOwner.MAPPY, 1);
            savedDataCount += 1;// + 1 because new locations to send
        }
        boolean dtsServer = DataXYOwner.hasUrlServerTier(owner);
        if (dtsServer) {
            savedDataCount += saveData(DataXYOwner.URL_SERVER_TIER, 1);
            savedDataCount += 1;// + 1 because new locations to send
        }
        boolean urlServer = DataXYOwner.hasUrlServerTier2(owner);
        if (urlServer) {
            savedDataCount += saveData(DataXYOwner.URL_SERVER_TIER_2, 1);
            savedDataCount += 1;// + 1 because new locations to send
        }
        DataXYSender.INSTANCE = new IDataXYSender() {
            @Override
            public void send(Context context, @NonNull String locations, @NonNull byte owners, @NonNull Callback callback) {
                ++sentData;
                callback.onSuccess(owners);
            }

            @Override
            public void setOkHttpClient(OkHttpClient okHttpClient) {
            }
        };

        runSendTestHuge(owner, LOCATION_PROVIDERS, 100000);
        waitWhileSending(owner);

        Assert.assertEquals(savedDataCount, sentData); // + 1 because new locations to send
    }

    private int saveData(byte owners) {
        return saveData(owners, 20000);
    }

    private int saveData(byte owners, int nbFileToSend) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean enableDts = DataXYOwner.hasUrlServerTier(owners);
        boolean enableMappy = DataXYOwner.hasMappy(owners);
        boolean enableUrlServer = DataXYOwner.hasUrlServerTier2(owners);
        for (int i = 0; i < nbFileToSend; i++) {

            if (enableDts) {
                final String nameDts = i + "_dts";
                DataXYDataManager.saveData(context, nameDts, JSON_LIST, DataXYOwner.URL_SERVER_TIER);
            }
            if (enableMappy) {
                final String nameMappy = i + "_mappy";
                DataXYDataManager.saveData(context, nameMappy, JSON_LIST, DataXYOwner.MAPPY);
            }
            if (enableUrlServer) {
                final String nameUrlServer = i + "_url_server";
                DataXYDataManager.saveData(context, nameUrlServer, JSON_LIST, DataXYOwner.URL_SERVER_TIER_2);
            }
        }

        checkSavedData(context, nbFileToSend, owners);
        return nbFileToSend;
    }

    private void checkSavedData(Context context, int nbFileToSend, byte owners) {
        List<DataXYDataManager.DataXYDataHolder> dataDts = DataXYDataManager.getSavedData(context, DataXYOwner.URL_SERVER_TIER);
        if (DataXYOwner.hasUrlServerTier(owners)) {
            Assert.assertEquals(nbFileToSend, dataDts.size());
            for (DataXYDataManager.DataXYDataHolder holder : dataDts) {
                Assert.assertTrue(holder.linkedFileExists());
            }
        }

        List<DataXYDataManager.DataXYDataHolder> dataMappy = DataXYDataManager.getSavedData(context, DataXYOwner.MAPPY);
        if (DataXYOwner.hasMappy(owners)) {
            Assert.assertEquals(nbFileToSend, dataMappy.size());
            for (DataXYDataManager.DataXYDataHolder holder : dataMappy) {
                Assert.assertTrue(holder.linkedFileExists());
            }
        }

        List<DataXYDataManager.DataXYDataHolder> dataUrlServerMappy = DataXYDataManager.getSavedData(context, DataXYOwner.URL_SERVER_TIER_2);
        if (DataXYOwner.hasUrlServerTier2(owners)) {
            Assert.assertEquals(nbFileToSend, dataUrlServerMappy.size());
            for (DataXYDataManager.DataXYDataHolder holder : dataUrlServerMappy) {
                Assert.assertTrue(holder.linkedFileExists());
            }
        }
    }

    private void runSendTest(byte owners) {
        int previousMax = DataXYPreferencesHelper.getMinNbPointsReceiver();
        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext,  3);
        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        Assert.assertTrue(dataBase.getLocationCount(owners) == 0);
        dataBase.close();

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeInMillis(1123);
        mockLocation(48.3, 2.85, 42, 22, 2.0F, calendar);
        calendar.setTimeInMillis(3456);
        mockLocation(48.2, 2.84, 42, 22, 3.0F, calendar);
        calendar.setTimeInMillis(5789);
        mockLocation(48.1, 2.84, 42, 22, 2.1F, calendar);

        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext,  previousMax);
    }

    private void runSendTestHuge(byte owners, String locationProviders, int max) {
        int previousMax = DataXYPreferencesHelper.getMinNbPointsReceiver();
        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext,  max -1);
        final DataXYDataBase dataBase = new DataXYDataBase(getActivity())
                .open();

        Assert.assertTrue(dataBase.getLocationCount(owners) == 0);

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
        dataBase.insertLocationOwners(locations, owners, locationProviders);

        dataBase.close();

        calendar.setTimeInMillis(max * 1000);
        mockLocation(48.3 + max * 0.00001, 2.85 + max * 0.00001, 42, 22, 2.0F, calendar);

        DataXYPreferencesHelper.setMinNbPointsReceiver(mContext,  previousMax);
    }

    private void waitWhileSending(byte owners) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (DataXYOwner.hasMappy(owners)) {
            do {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            while (DataXYDataManager.getSavedData(context, DataXYOwner.MAPPY) != null);
        }
        if (DataXYOwner.hasUrlServerTier(owners)) {
            do {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            while (DataXYDataManager.getSavedData(context, DataXYOwner.URL_SERVER_TIER) != null);
        }
        if (DataXYOwner.hasUrlServerTier2(owners)) {
            do {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            while (DataXYDataManager.getSavedData(context, DataXYOwner.URL_SERVER_TIER_2) != null);
        }
    }

    private void clearCache(byte owners) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        List<DataXYDataManager.DataXYDataHolder> dtsSavedData = null;
        if (DataXYOwner.hasUrlServerTier(owners)) {
            dtsSavedData = DataXYDataManager.getSavedData(context, DataXYOwner.URL_SERVER_TIER);
        }
        List<DataXYDataManager.DataXYDataHolder> mappySavedData = null;
        if (DataXYOwner.hasMappy(owners)) {
           mappySavedData = DataXYDataManager.getSavedData(context, DataXYOwner.MAPPY);
        }
        List<DataXYDataManager.DataXYDataHolder> urlServerSavedData = null;
        if (DataXYOwner.hasUrlServerTier2(owners)) {
            urlServerSavedData = DataXYDataManager.getSavedData(context, DataXYOwner.URL_SERVER_TIER_2);
        }

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
    }
    private void clearDataBase() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        byte owners = DataXYOwner.getOwners();
        new DataXYDataBase(targetContext)
                .open()
                .deleteAllLocations(owners)
                .close();
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
        final long millis = 1000L;
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
