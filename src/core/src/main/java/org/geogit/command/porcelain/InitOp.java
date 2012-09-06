/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.command.porcelain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;
import org.geogit.command.plumbing.ResolveGeogitDir;
import org.geogit.repository.Repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Creates or "initializes" a repository in the {@link Platform#pwd() working directory}.
 * <p>
 * This command tries to find an existing {@code .geogit} repository directory in the current
 * directory's hierarchy. It is safe to call it from inside a directory that's a child of a
 * repository.
 * <p>
 * If no repository directory is found, then a new one is created on the current directory.
 * 
 * @see ResolveGeogitDir
 */
public class InitOp extends AbstractGeoGitOp<Repository> {

    private Platform platform;

    private Injector injector;

    /**
     * @param platform where to get the current directory from
     * @param injector where to get the repository from (with auto-wired dependencies) once ensured
     *        the {@code .geogit} repository directory is found or created.
     */
    @Inject
    public InitOp(Platform platform, Injector injector) {
        super(null);
        checkNotNull(platform);
        checkNotNull(injector);
        this.platform = platform;
        this.injector = injector;
    }

    /**
     * @return the repository _if_ it was newly created, {@code null} if an existing repository was
     *         reinitialized (NOTE so far reinitialized is a loose term, we're not reinitializing
     *         anything, like copying template config files to the repo directory or so)
     * @throws IllegalStateException if a repository cannot be created on the current directory or
     *         re-initialized in the current dir or one if its parents as determined by
     *         {@link ResolveGeogitDir}
     */
    @Override
    public Repository call() {
        final File workingDirectory = platform.pwd();
        checkState(workingDirectory != null, "working directory is null");
        final URL repoUrl = new ResolveGeogitDir(platform).call();

        boolean repoExisted = false;
        File envHome;
        if (repoUrl == null) {
            envHome = new File(workingDirectory, ".geogit");
            envHome.mkdirs();
            if (!envHome.exists()) {
                throw new RuntimeException("Unable to create geogit environment at '"
                        + envHome.getAbsolutePath() + "'");
            }
        } else {
            // we're at either the repo working dir or a subdirectory of it
            try {
                envHome = new File(repoUrl.toURI());
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
            repoExisted = true;
        }

        try {
            Preconditions.checkState(envHome.toURI().toURL()
                    .equals(new ResolveGeogitDir(platform).call()));
        } catch (MalformedURLException e) {
            Throwables.propagate(e);
        }

        Repository repository;
        try {
            repository = injector.getInstance(Repository.class);
            repository.create();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Can't access repository at '"
                    + envHome.getAbsolutePath() + "'", e);
        }

        return repoExisted ? null : repository;
    }

}