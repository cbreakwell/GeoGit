package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.BranchDeleteOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CloneOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.FetchOp;
import org.geogit.api.porcelain.LogOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class FetchOpTest extends RemoteRepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    LinkedList<RevCommit> expectedMaster;

    LinkedList<RevCommit> expectedBranch;

    @Override
    protected void setUpInternal() throws Exception {
    }

    private void prepareForFetch() throws Exception {
        // clone the repository
        CloneOp clone = clone();
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).call();

        // Commit several features to the remote

        expectedMaster = new LinkedList<RevCommit>();
        expectedBranch = new LinkedList<RevCommit>();

        insertAndAdd(remoteGeogit.geogit, points1);
        RevCommit commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);
        expectedBranch.addFirst(commit);

        // Create and checkout branch1
        remoteGeogit.geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("Branch1")
                .call();

        // Commit some changes to branch1
        insertAndAdd(remoteGeogit.geogit, points2);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedBranch.addFirst(commit);

        insertAndAdd(remoteGeogit.geogit, points3);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedBranch.addFirst(commit);

        // Make sure Branch1 has all of the commits
        Iterator<RevCommit> logs = remoteGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedBranch, logged);

        // Checkout master and commit some changes
        remoteGeogit.geogit.command(CheckoutOp.class).setSource("master").call();

        insertAndAdd(remoteGeogit.geogit, lines1);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);

        insertAndAdd(remoteGeogit.geogit, lines2);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);

        // Make sure master has all of the commits
        logs = remoteGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedMaster, logged);
    }

    private void verifyFetch() throws Exception {
        // Make sure the local repository got all of the commits from master
        localGeogit.geogit.command(CheckoutOp.class).setSource("refs/remotes/origin/master").call();
        Iterator<RevCommit> logs = localGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedMaster, logged);

        // Make sure the local repository got all of the commits from Branch1
        localGeogit.geogit.command(CheckoutOp.class).setSource("refs/remotes/origin/Branch1")
                .call();
        logs = localGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedBranch, logged);
    }

    private void verifyPrune() throws Exception {
        // Make sure the local repository got all of the commits from master
        localGeogit.geogit.command(CheckoutOp.class).setForce(true)
                .setSource("refs/remotes/origin/master").call();
        Iterator<RevCommit> logs = localGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedMaster, logged);

        // Make sure the local repository no longer has Branch1
        Optional<Ref> missing = localGeogit.geogit.command(RefParse.class)
                .setName("refs/remotes/origin/Branch1").call();

        assertFalse(missing.isPresent());
    }

    @Test
    public void testFetch() throws Exception {

        prepareForFetch();

        // fetch from the remote
        FetchOp fetch = fetch();
        fetch.call();

        verifyFetch();
    }

    @Test
    public void testFetchAll() throws Exception {
        prepareForFetch();

        // fetch from the remote
        FetchOp fetch = fetch();
        fetch.setAll(true).call();

        verifyFetch();
    }

    @Test
    public void testFetchSpecificRemote() throws Exception {
        prepareForFetch();

        // fetch from the remote
        FetchOp fetch = fetch();
        fetch.addRemote("origin").call();

        verifyFetch();
    }

    @Test
    public void testFetchSpecificRemoteAndAll() throws Exception {
        prepareForFetch();

        // fetch from the remote
        FetchOp fetch = fetch();
        fetch.addRemote("origin").setAll(true).call();

        verifyFetch();
    }

    @Test
    public void testFetchNoRemotes() throws Exception {
        FetchOp fetch = fetch();
        exception.expect(IllegalStateException.class);
        fetch.call();
    }

    @Test
    public void testFetchNoChanges() throws Exception {
        prepareForFetch();

        // fetch from the remote
        FetchOp fetch = fetch();
        fetch.addRemote("origin").setAll(true).call();

        verifyFetch();

        // fetch again
        fetch.call();

        verifyFetch();
    }

    @Test
    public void testFetchWithPrune() throws Exception {
        prepareForFetch();

        // fetch from the remote
        FetchOp fetch = fetch();
        fetch.addRemote("origin").setAll(true).call();

        verifyFetch();

        // Remove a branch from the remote
        remoteGeogit.geogit.command(BranchDeleteOp.class).setName("Branch1").call();

        // fetch again
        fetch = fetch();
        fetch.setPrune(true).call();

        verifyPrune();
    }

    @Test
    public void testFetchWithPruneAndBranchAdded() throws Exception {
        prepareForFetch();

        // fetch from the remote
        FetchOp fetch = fetch();
        fetch.addRemote("origin").setAll(true).call();

        verifyFetch();

        // Remove a branch from the remote
        remoteGeogit.geogit.command(BranchDeleteOp.class).setName("Branch1").call();

        // Add another branch
        remoteGeogit.geogit.command(BranchCreateOp.class).setName("Branch2").call();

        // fetch again
        fetch = fetch();
        fetch.setPrune(true).call();

        verifyPrune();

        // Make sure the local repository has Branch2
        Optional<Ref> missing = localGeogit.geogit.command(RefParse.class)
                .setName("refs/remotes/origin/Branch2").call();

        assertTrue(missing.isPresent());
    }

}
