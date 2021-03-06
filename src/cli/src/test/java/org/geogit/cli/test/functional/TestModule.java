/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.test.functional;

import org.geogit.api.Platform;

import com.google.inject.AbstractModule;

/**
 *
 */
public class TestModule extends AbstractModule {

    private Platform testPlatform;

    /**
     * @param testPlatform
     */
    public TestModule(Platform testPlatform) {
        this.testPlatform = testPlatform;
    }

    @Override
    protected void configure() {
        if (testPlatform != null) {
            bind(Platform.class).toInstance(testPlatform);
        }
    }

}
