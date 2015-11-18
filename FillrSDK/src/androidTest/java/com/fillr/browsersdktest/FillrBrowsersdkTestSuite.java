package com.fillr.browsersdktest;

import android.test.suitebuilder.TestSuiteBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by naveedzanoon on 11/06/15.
 */
public class FillrBrowsersdkTestSuite extends TestSuite {

    public static Test suite() {
        return new TestSuiteBuilder(FillrBrowsersdkTest.class).includeAllPackagesUnderHere().build();
    }

}
