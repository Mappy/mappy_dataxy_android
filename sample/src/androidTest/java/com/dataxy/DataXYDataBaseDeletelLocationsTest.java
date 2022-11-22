package com.dataxy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class DataXYDataBaseDeletelLocationsTest extends BaseDataXYDataBaseTests {

    private static byte[] POSSIBLE_OWNERS = {DataXYOwner.MAPPY, DataXYOwner.URL_SERVER_TIER_2, DataXYOwner.URL_SERVER_TIER};
    private static byte[] TWO_OWNERS = {DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER, DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2, DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER};
    private static byte ALL_OWNERS = DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> combinations = new ArrayList<>();
        for (Byte pointsOwner : POSSIBLE_OWNERS) {
            for (Byte requestOwner : POSSIBLE_OWNERS) {
                combinations.add(new Byte[]{pointsOwner, requestOwner});
            }
        }
        for (Byte pointsOwner : POSSIBLE_OWNERS) {
            for (Byte requestOwner : TWO_OWNERS) {
                combinations.add(new Byte[]{pointsOwner, requestOwner});
            }
        }
        for (Byte pointsOwner : POSSIBLE_OWNERS) {
            combinations.add(new Byte[]{pointsOwner, ALL_OWNERS});
        }

        for (Byte pointsOwner : TWO_OWNERS) {
            for (Byte requestOwner : POSSIBLE_OWNERS) {
                combinations.add(new Byte[]{pointsOwner, requestOwner});
            }
        }
        for (Byte pointsOwner : TWO_OWNERS) {
            for (Byte requestOwner : TWO_OWNERS) {
                combinations.add(new Byte[]{pointsOwner, requestOwner});
            }
        }
        for (Byte pointsOwner : TWO_OWNERS) {
            combinations.add(new Byte[]{pointsOwner, ALL_OWNERS});
        }

        for (Byte requestOwner : POSSIBLE_OWNERS) {
            combinations.add(new Byte[]{ALL_OWNERS, requestOwner});
        }
        for (Byte requestOwner : TWO_OWNERS) {
            combinations.add(new Byte[]{ALL_OWNERS, requestOwner});
        }
        combinations.add(new Byte[]{ALL_OWNERS, ALL_OWNERS});

        return combinations;
    }

    private byte pointsOwner;
    private byte requestOwner;

    public DataXYDataBaseDeletelLocationsTest(byte pointsOwner, byte requestOwner) {
        this.pointsOwner = pointsOwner;
        this.requestOwner = requestOwner;
    }

    @Test
    public void test_deleteLocations() {
        checkDeleteCondition();
        mDataBase.insertLocationOwners(LOCATION_1, pointsOwner, LOCATION_PROVIDERS);
        mDataBase.insertLocationOwners(LOCATION_2, pointsOwner, LOCATION_PROVIDERS);

        mDataBase.deleteAllLocations(requestOwner);

        int expectedPoints = shouldDeletePoints(pointsOwner, requestOwner) ? 0 : 2;

        runTest(expectedPoints, DataXYOwner.ALL_OWNERS, "Incorrect locations count for " + pointsOwner + "-" + requestOwner + " ;");
    }

    @Test
    public void test_deleteOldLocations() {
        mDataBase.deleteAllLocations(DataXYOwner.ALL_OWNERS);
        checkDeleteCondition();
        mDataBase.insertLocationOwners(LOCATION_1, pointsOwner, LOCATION_PROVIDERS);
        mDataBase.insertLocationOwners(LOCATION_2, pointsOwner, LOCATION_PROVIDERS);
        mDataBase.insertLocationOwners(LOCATION_3, pointsOwner, LOCATION_PROVIDERS);

        mDataBase.deleteOldLocations(1, requestOwner);

        boolean shouldDeletePoints = shouldDeletePoints(pointsOwner, requestOwner);
        int expectedPoints = shouldDeletePoints ? 2 : 3;

        runTest(expectedPoints, DataXYOwner.ALL_OWNERS, "Incorrect locations count for " + pointsOwner + "-" + requestOwner + " ;");

        if (shouldDeletePoints) {
            String message = "Incorrect date for " + pointsOwner + "-" + requestOwner + " ;";
            final List<DataXYLocation> locations = mDataBase.getAllLocations(DataXYOwner.ALL_OWNERS);
            Assert.assertEquals(message, DATE_2, locations.get(0).mDate);
            Assert.assertEquals(message, DATE_3, locations.get(1).mDate);
        }
    }

    @Test
    public void test_deleteManyOldLocations() {
        checkDeleteCondition();
        mDataBase.insertLocationOwners(LOCATION_1, pointsOwner, LOCATION_PROVIDERS);
        mDataBase.insertLocationOwners(LOCATION_2, pointsOwner, LOCATION_PROVIDERS);
        mDataBase.insertLocationOwners(LOCATION_3, pointsOwner, LOCATION_PROVIDERS);

        mDataBase.deleteOldLocations(20, requestOwner);

        int expectedPoints = shouldDeletePoints(pointsOwner, requestOwner) ? 0 : 3;

        runTest(expectedPoints, DataXYOwner.ALL_OWNERS, "Incorrect locations count for " + pointsOwner + "-" + requestOwner + " ;");
    }

    // Just to be sure
    private void checkDeleteCondition() {
        Assert.assertTrue(shouldDeletePoints(DataXYOwner.MAPPY, DataXYOwner.MAPPY));
        Assert.assertFalse(shouldDeletePoints(DataXYOwner.MAPPY, DataXYOwner.URL_SERVER_TIER));
        Assert.assertTrue(shouldDeletePoints(DataXYOwner.MAPPY, (byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER)));
        Assert.assertFalse(shouldDeletePoints(DataXYOwner.MAPPY, (byte) (DataXYOwner.URL_SERVER_TIER | DataXYOwner.URL_SERVER_TIER_2)));
        Assert.assertFalse(shouldDeletePoints((byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER), DataXYOwner.MAPPY));
        Assert.assertFalse(shouldDeletePoints((byte) (DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER), DataXYOwner.MAPPY));
    }

    private boolean shouldDeletePoints(byte pointsOwner, byte requestOwner) {
        return (pointsOwner & requestOwner) == pointsOwner;
    }
}
