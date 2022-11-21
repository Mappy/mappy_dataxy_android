package com.dataxy;

import android.location.Location;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class BaseDataXYDataBaseTests {

    DataXYDataBase mDataBase;

    private static final double LATITUDE_1 = 48.85;
    private static final double LONGITUDE_1 = 2.33;
    private static final double ALTITUDE_1 = 35;
    private static final long   DATE_1 = 557798400000L;
    private static final double LATITUDE_2 = 49;
    private static final double LONGITUDE_2 = 2.3;
    private static final double ALTITUDE_2 = 30;
    static final long           DATE_2 = 557798460000L;
    private static final double LATITUDE_3 = 49.15;
    private static final double LONGITUDE_3 = 2.27;
    private static final double ALTITUDE_3 = 25;
    static final long           DATE_3 = 557798520000L;

    static final Location LOCATION_1 = new Location("test");
    static final Location LOCATION_2 = new Location("test");
    static final Location LOCATION_3 = new Location("test");

    static final String LOCATION_PROVIDERS = "[gps]";

    static {
        LOCATION_1.setLatitude(LATITUDE_1);
        LOCATION_1.setLongitude(LONGITUDE_1);
        LOCATION_1.setAltitude(ALTITUDE_1);
        LOCATION_1.setTime(DATE_1);

        LOCATION_2.setLatitude(LATITUDE_2);
        LOCATION_2.setLongitude(LONGITUDE_2);
        LOCATION_2.setAltitude(ALTITUDE_2);
        LOCATION_2.setTime(DATE_2);

        LOCATION_3.setLatitude(LATITUDE_3);
        LOCATION_3.setLongitude(LONGITUDE_3);
        LOCATION_3.setAltitude(ALTITUDE_3);
        LOCATION_3.setTime(DATE_3);
    }

    @Before
    public void setUp() {
        mDataBase = new DataXYDataBase(InstrumentationRegistry.getInstrumentation().getTargetContext());
        mDataBase.open();
        byte owners = (byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER);
        mDataBase.deleteAllLocations(owners);

        Assert.assertEquals("database not empty ", 0, mDataBase.getLocationCount(owners));
    }

    @After
    public void tearDown() {
        byte owners = (byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER);
        mDataBase.deleteAllLocations(owners);
        mDataBase.close();
        mDataBase = null;
    }

    void runTest(int expected, byte owner, String failureMessage) {
        final int locationCount = mDataBase.getLocationCount(owner);
        final List<DataXYLocation> locations = mDataBase.getAllLocations(owner);
        Assert.assertEquals(failureMessage, expected, locationCount);
        Assert.assertEquals(failureMessage, expected, locations.size());
    }
}
