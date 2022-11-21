package com.dataxy;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

@RunWith(Parameterized.class)
public class DataXYDataBaseInsertLocationsTest extends BaseDataXYDataBaseTests {

    private static byte[] POSSIBLE_OWNERS = {DataXYOwner.MAPPY, DataXYOwner.URL_SERVER_TIER_2, DataXYOwner.URL_SERVER_TIER};
    private static byte[] TWO_OWNERS = {DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2, DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER, DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER};
    private static byte ALL_OWNERS = DataXYOwner.MAPPY | DataXYOwner.URL_SERVER_TIER_2 | DataXYOwner.URL_SERVER_TIER;

    private static Location[][] POSSIBLE_POINTS_SETS = new Location[][]{
            {}, {LOCATION_1}, {LOCATION_1, LOCATION_2}
    };

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> combinations = new ArrayList<>();
        for (Location[] points : POSSIBLE_POINTS_SETS) {
            for (Byte pointsOwner : POSSIBLE_OWNERS) {
                combinations.add(new Object[]{pointsOwner, points});
            }
            for (Byte pointsOwner : TWO_OWNERS) {
                combinations.add(new Object[]{pointsOwner, points});
            }
            combinations.add(new Object[]{ALL_OWNERS, points});
        }

        return combinations;
    }

    private byte pointsOwner;
    private Location[] pointsSet;

    public DataXYDataBaseInsertLocationsTest(byte pointsOwner, Location[] pointsSet) {
        this.pointsOwner = pointsOwner;
        this.pointsSet = pointsSet;
    }

    @Test
    public void test_insertLocations() {
        for (Location loc : pointsSet) {
            mDataBase.insertLocationOwners(loc, pointsOwner, LOCATION_PROVIDERS);
        }
        runTest(pointsSet.length, pointsOwner, "Incorrect locations count for "+pointsOwner+" ;");
    }
}
