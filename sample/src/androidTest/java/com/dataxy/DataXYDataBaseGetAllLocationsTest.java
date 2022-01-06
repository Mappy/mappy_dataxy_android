package com.dataxy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class DataXYDataBaseGetAllLocationsTest extends BaseDataXYDataBaseTests {

    private static byte[] POSSIBLE_OWNERS = { DataXYOwner.MAPPY, DataXYOwner.URL_SERVER_TIER_2, DataXYOwner.URL_SERVER_TIER };
    private static byte[] TWO_OWNERS = { DataXYOwner.MAPPY |  DataXYOwner.URL_SERVER_TIER_2, DataXYOwner.MAPPY |  DataXYOwner.URL_SERVER_TIER, DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER };
    private static byte ALL_OWNERS = DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> combinations = new ArrayList<>();
        for(Byte pointsOwner : POSSIBLE_OWNERS) {
            for (Byte requestOwner : POSSIBLE_OWNERS) {
                combinations.add( new Byte[]{pointsOwner, requestOwner} );
            }
        }
        for(Byte pointsOwner : POSSIBLE_OWNERS) {
            for (Byte requestOwner : TWO_OWNERS) {
                combinations.add( new Byte[]{pointsOwner, requestOwner} );
            }
        }
        for(Byte pointsOwner : POSSIBLE_OWNERS) {
            combinations.add( new Byte[]{pointsOwner, ALL_OWNERS} );
        }

        for(Byte pointsOwner : TWO_OWNERS) {
            for (Byte requestOwner : POSSIBLE_OWNERS) {
                combinations.add( new Byte[]{pointsOwner, requestOwner} );
            }
        }
        for(Byte pointsOwner : TWO_OWNERS) {
            for (Byte requestOwner : TWO_OWNERS) {
                combinations.add( new Byte[]{pointsOwner, requestOwner} );
            }
        }
        for(Byte pointsOwner : TWO_OWNERS) {
            combinations.add( new Byte[]{pointsOwner, ALL_OWNERS} );
        }

        for(Byte requestOwner : POSSIBLE_OWNERS) {
            combinations.add( new Byte[]{ALL_OWNERS, requestOwner} );
        }
        for(Byte requestOwner : TWO_OWNERS) {
            combinations.add( new Byte[]{ALL_OWNERS, requestOwner} );
        }
        combinations.add( new Byte[]{ALL_OWNERS, ALL_OWNERS} );

        return combinations;
    }

    private byte pointsOwner;
    private byte requestOwner;

    public DataXYDataBaseGetAllLocationsTest(byte pointsOwner, byte requestOwner) {
        this.pointsOwner = pointsOwner;
        this.requestOwner = requestOwner;
    }

    @Test
    public void test_getAllLocations() {
        checkExpectCondition();
        checkGetAllLocations(shouldExpectPoints(pointsOwner, requestOwner));
    }

    // Just to be sure
    private void checkExpectCondition() {
        Assert.assertTrue(shouldExpectPoints(DataXYOwner.MAPPY, DataXYOwner.MAPPY));
        Assert.assertFalse(shouldExpectPoints(DataXYOwner.MAPPY, DataXYOwner.URL_SERVER_TIER_2));
        Assert.assertTrue(shouldExpectPoints(DataXYOwner.MAPPY, (byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2)));
        Assert.assertFalse(shouldExpectPoints(DataXYOwner.MAPPY, (byte) (DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER)));
        Assert.assertFalse(shouldExpectPoints((byte) (DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2), DataXYOwner.MAPPY));
        Assert.assertFalse(shouldExpectPoints((byte) (DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER), DataXYOwner.MAPPY));
    }

    private boolean shouldExpectPoints(byte pointsOwner, byte requestOwner) {
        return (pointsOwner & requestOwner) == pointsOwner;
    }

    private void checkGetAllLocations(boolean expectPoints) {
        mDataBase.insertLocationOwners(LOCATION_1, pointsOwner, LOCATION_PROVIDERS);

        String errorMessage = "Unexpected result for owners "+pointsOwner+" "+requestOwner+";";
        final List<DataXYLocation> locations = mDataBase.getAllLocations(requestOwner);
        if(expectPoints) {
            Assert.assertEquals(errorMessage, 1, locations.size());
            final DataXYLocation location_1 = locations.get(0);
            Assert.assertEquals(errorMessage, LOCATION_1.getTime(), location_1.mDate);
            Assert.assertEquals(errorMessage, LOCATION_1.getLatitude(), location_1.mLatitude, 0.0);
            Assert.assertEquals(errorMessage, LOCATION_1.getLongitude(), location_1.mLongitude, 0.0);
            Assert.assertEquals(errorMessage, LOCATION_1.getAltitude(), location_1.mAltitude, 0.0);
            Assert.assertEquals(errorMessage, LOCATION_PROVIDERS, location_1.mLocationProviders);
        } else {
            Assert.assertEquals(errorMessage, 0, locations.size());
        }
    }
}
