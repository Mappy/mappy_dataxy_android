package com.dataxy;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import androidx.test.platform.app.InstrumentationRegistry;

public class Junit4InstrumentationTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    public Junit4InstrumentationTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
