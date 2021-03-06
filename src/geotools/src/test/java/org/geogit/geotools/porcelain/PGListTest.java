/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.AbstractDataStoreFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.exceptions.base.MockitoException;

/**
 *
 */
public class PGListTest extends Assert {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogitCLI cli;

    private static AbstractDataStoreFactory factory;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        factory = TestHelper.createTestFactory();
    }

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        setUpGeogit(cli);
    }

    @Test
    public void testList() throws Exception {
        PGList listCommand = new PGList();
        listCommand.dataStoreFactory = factory;
        listCommand.run(cli);
    }

    @Test
    public void testListHelp() throws Exception {
        PGList listCommand = new PGList();
        listCommand.help = true;
        listCommand.run(cli);
    }

    @Test
    public void testInvalidDatabaseParams() throws Exception {
        PGList listCommand = new PGList();
        listCommand.commonArgs.host = "nonexistant";
        listCommand.run(cli);
    }

    @Test
    public void testNoRepository() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        PGList listCommand = new PGList();
        listCommand.dataStoreFactory = factory;
        listCommand.run(cli);
    }

    @Test
    public void testNullDataStore() throws Exception {
        PGList listCommand = new PGList();
        listCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        listCommand.run(cli);
    }

    @Test
    public void testEmptyDataStore() throws Exception {
        PGList listCommand = new PGList();
        listCommand.dataStoreFactory = TestHelper.createEmptyTestFactory();
        listCommand.run(cli);
    }

    @Test
    public void testGetNamesException() throws Exception {
        PGList listCommand = new PGList();
        listCommand.dataStoreFactory = TestHelper.createFactoryWithGetNamesException();
        listCommand.run(cli);
    }

    @Test
    public void testListException() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        GeogitCLI mockCli = spy(new GeogitCLI(consoleReader));

        setUpGeogit(mockCli);

        when(mockCli.getConsole()).thenThrow(new MockitoException("Exception"));
        PGList listCommand = new PGList();
        listCommand.dataStoreFactory = factory;
        exception.expect(MockitoException.class);
        listCommand.run(mockCli);
    }

    private void setUpGeogit(GeogitCLI cli) throws Exception {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        when(platform.getUserHome()).thenReturn(userhome);

        cli.setPlatform(platform);
    }

}
