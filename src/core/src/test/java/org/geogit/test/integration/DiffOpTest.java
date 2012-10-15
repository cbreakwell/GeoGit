/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.plumbing.diff.DiffTreeWalk;
import org.geogit.api.porcelain.DiffOp;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Unit test suite for {@link DiffOp}, must cover {@link DiffTreeWalk} too.
 * 
 * @author groldan
 * 
 */
public class DiffOpTest extends RepositoryTestCase {

    private DiffOp diffOp;

    @Override
    protected void setUpInternal() throws Exception {
        this.diffOp = geogit.diff();
    }

    @Test
    public void testDiffPreconditions() throws Exception {
        Iterator<DiffEntry> difflist = geogit.diff().call();
        assertNotNull(difflist);
        assertFalse(difflist.hasNext());

        final ObjectId oid1 = insertAndAdd(points1);
        final RevCommit commit1_1 = geogit.commit().call();
        try {
            diffOp.setOldVersion(oid1.toString()).setNewVersion(Ref.HEAD).call();
            fail("Expected IAE as oldVersion is not a commit");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(oid1.toString()));
            assertTrue(e.getMessage(),
                    e.getMessage().contains("doesn't resolve to a tree-ish object"));
        }
        try {
            diffOp.setOldVersion(commit1_1.getId().toString()).setNewVersion(oid1.toString())
                    .call();
            fail("Expected IAE as newVersion is not a commit");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(oid1.toString()));
            assertTrue(e.getMessage(),
                    e.getMessage().contains("doesn't resolve to a tree-ish object"));
        }
    }

    @Test
    public void testEmptyRepo() throws Exception {
        Iterator<DiffEntry> difflist = diffOp.setOldVersion(ObjectId.NULL.toString()).call();
        assertNotNull(difflist);
        assertFalse(difflist.hasNext());
    }

    @Test
    public void testNoChangeSameCommit() throws Exception {

        insertAndAdd(points1);
        final RevCommit commit = geogit.commit().setAll(true).call();

        assertFalse(diffOp.setOldVersion(commit.getId().toString())
                .setNewVersion(commit.getId().toString()).call().hasNext());
    }

    @Test
    public void testSingleAddition() throws Exception {

        final ObjectId newOid = insertAndAdd(points1);
        geogit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(ObjectId.NULL)
                .setNewVersion(Ref.HEAD).call());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);

        assertNull(de.getOldObject());
        assertNotNull(de.getNewObject());

        String expectedPath = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        assertEquals(expectedPath, de.newPath());

        assertEquals(DiffEntry.ChangeType.ADDED, de.changeType());
        assertEquals(ObjectId.NULL, de.oldObjectId());

        assertEquals(newOid, de.newObjectId());
        assertFalse(de.getNewObject().getMetadataId().isNull());
    }

    @Test
    public void testSingleAdditionReverseOrder() throws Exception {

        final ObjectId newOid = insertAndAdd(points1);
        final RevCommit commit = geogit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(commit.getId())
                .setNewVersion(ObjectId.NULL).call());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);

        assertNull(de.getNewObject());
        assertNotNull(de.getOldObject());

        assertEquals(DiffEntry.ChangeType.REMOVED, de.changeType());
        assertEquals(ObjectId.NULL, de.newObjectId());

        assertEquals(newOid, de.oldObjectId());
        assertFalse(de.getOldObject().getMetadataId().isNull());
    }

    @Test
    public void testSingleDeletion() throws Exception {
        final ObjectId featureContentId = insertAndAdd(points1);
        final RevCommit addCommit = geogit.commit().setAll(true).call();

        assertTrue(deleteAndAdd(points1));
        final RevCommit deleteCommit = geogit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(addCommit.getId())
                .setNewVersion(deleteCommit.getId()).call());

        final String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);
        assertEquals(path, de.oldPath());

        assertEquals(DiffEntry.ChangeType.REMOVED, de.changeType());

        assertEquals(featureContentId, de.oldObjectId());

        assertEquals(ObjectId.NULL, de.newObjectId());
    }

    @Test
    public void testSingleDeletionReverseOrder() throws Exception {

        final ObjectId featureContentId = insertAndAdd(points1);
        final RevCommit addCommit = geogit.commit().setAll(true).call();

        assertTrue(deleteAndAdd(points1));
        final RevCommit deleteCommit = geogit.commit().setAll(true).call();

        // set old/new version in reverse order
        List<DiffEntry> difflist = toList(diffOp.setOldVersion(deleteCommit.getId())
                .setNewVersion(addCommit.getId()).call());

        final String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());

        // then the diff should report an ADD instead of a DELETE
        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);
        assertNull(de.oldPath());
        assertEquals(path, de.newPath());

        assertEquals(DiffEntry.ChangeType.ADDED, de.changeType());

        assertEquals(ObjectId.NULL, de.oldObjectId());

        assertEquals(featureContentId, de.newObjectId());
    }

    @Test
    public void testSingleModification() throws Exception {

        final ObjectId oldOid = insertAndAdd(points1);
        final RevCommit insertCommit = geogit.commit().setAll(true).call();

        final String featureId = points1.getIdentifier().getID();
        final Feature modifiedFeature = feature((SimpleFeatureType) points1.getType(), featureId,
                "changedProp", new Integer(1500), null);

        final ObjectId newOid = insertAndAdd(modifiedFeature);

        final RevCommit changeCommit = geogit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(insertCommit.getId())
                .setNewVersion(changeCommit.getId()).call());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);
        String expectedPath = NodeRef.appendChild(pointsName, featureId);
        assertEquals(expectedPath, de.newPath());

        assertEquals(DiffEntry.ChangeType.MODIFIED, de.changeType());
        assertEquals(oldOid, de.oldObjectId());

        assertEquals(newOid, de.newObjectId());
    }

    @Test
    public void testFilterNamespaceNoChanges() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = geogit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = geogit.commit().setAll(true).call();

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(pointsName);

        Iterator<DiffEntry> diffs = diffOp.call();
        assertFalse(diffs.hasNext());
    }

    @Test
    public void testFilterTypeNameNoChanges() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = geogit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = geogit.commit().setAll(true).call();

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(pointsName);

        Iterator<DiffEntry> diffs = diffOp.call();
        assertFalse(diffs.hasNext());
    }

    @Test
    public void testFilterDidntMatchAnything() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = geogit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = geogit.commit().setAll(true).call();

        // set a filter that doesn't produce any match

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(NodeRef.appendChild(pointsName, "nonExistentId"));

        Iterator<DiffEntry> diffs = diffOp.call();
        assertNotNull(diffs);
        assertFalse(diffs.hasNext());
    }

    @Test
    public void testFilterFeatureIdNoChanges() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = geogit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = geogit.commit().setAll(true).call();

        // filter on feature1_1, it didn't change between commit2 and commit1

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(NodeRef.appendChild(pointsName, points1.getIdentifier().getID()));

        Iterator<DiffEntry> diffs = diffOp.call();
        assertFalse(diffs.hasNext());
    }

    @Test
    public void testFilterMatchesSingleBlobChange() throws Exception {
        final ObjectId initialOid = insertAndAdd(points1);
        final RevCommit commit1 = geogit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = geogit.commit().setAll(true).call();

        ((SimpleFeature) points1).setAttribute("sp", "modified");
        final ObjectId modifiedOid = insertAndAdd(points1);
        final RevCommit commit3 = geogit.commit().setAll(true).call();

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit3.getId());
        diffOp.setFilter(NodeRef.appendChild(pointsName, points1.getIdentifier().getID()));

        List<DiffEntry> diffs;
        DiffEntry diff;

        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.MODIFIED, diff.changeType());
        assertEquals(initialOid, diff.oldObjectId());
        assertEquals(modifiedOid, diff.newObjectId());

        assertTrue(deleteAndAdd(points1));
        final RevCommit commit4 = geogit.commit().setAll(true).call();
        diffOp.setOldVersion(commit2.getId()).setNewVersion(commit4.getId());
        diffOp.setFilter(NodeRef.appendChild(pointsName, points1.getIdentifier().getID()));
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.REMOVED, diff.changeType());
        assertEquals(initialOid, diff.oldObjectId());
        assertEquals(ObjectId.NULL, diff.newObjectId());

        // invert the order of old and new commit
        diffOp.setOldVersion(commit4.getId()).setNewVersion(commit1.getId());
        diffOp.setFilter(NodeRef.appendChild(pointsName, points1.getIdentifier().getID()));
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.ADDED, diff.changeType());
        assertEquals(ObjectId.NULL, diff.oldObjectId());
        assertEquals(initialOid, diff.newObjectId());

        // different commit range
        diffOp.setOldVersion(commit4.getId()).setNewVersion(commit3.getId());
        diffOp.setFilter(NodeRef.appendChild(pointsName, points1.getIdentifier().getID()));
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.ADDED, diff.changeType());
        assertEquals(ObjectId.NULL, diff.oldObjectId());
        assertEquals(modifiedOid, diff.newObjectId());
    }

    // @Test
    // public void testFilterAddressesNamespaceTree() throws Exception {
    //
    // // two commits on different trees
    // final ObjectId oid11 = insertAndAdd(points1);
    // final ObjectId oid12 = insertAndAdd(points2);
    // final RevCommit commit1 = geogit.commit().setAll(true).call();
    //
    // final ObjectId oid21 = insertAndAdd(lines1);
    // final ObjectId oid22 = insertAndAdd(lines2);
    // final RevCommit commit2 = geogit.commit().setAll(true).call();
    //
    // List<DiffEntry> diffs;
    //
    // // filter on namespace1, no changes between commit1 and commit2
    // diffOp.setOldVersion(commit1.getId());
    // diffOp.setFilter(pointsNs);
    //
    // diffs = toList(diffOp.call());
    // assertEquals(0, diffs.size());
    //
    // // filter on namespace2, all additions between commit1 and commit2
    // diffOp.setOldVersion(commit1.getId());
    // diffOp.setFilter(linesNs);
    //
    // diffs = toList(diffOp.call());
    // assertEquals(2, diffs.size());
    // assertEquals(ChangeType.ADD, diffs.get(0).getType());
    // assertEquals(ChangeType.ADD, diffs.get(1).getType());
    //
    // assertEquals(ObjectId.NULL, diffs.get(0).getOldObjectId());
    // assertEquals(ObjectId.NULL, diffs.get(1).getOldObjectId());
    //
    // // don't care about order
    // Set<ObjectId> expected = new HashSet<ObjectId>();
    // expected.add(oid21);
    // expected.add(oid22);
    // Set<ObjectId> actual = new HashSet<ObjectId>();
    // actual.add(diffs.get(0).getNewObjectId());
    // actual.add(diffs.get(1).getNewObjectId());
    // assertEquals(expected, actual);
    // }

    @Test
    public void testMultipleDeletes() throws Exception {

        // two commits on different trees
        final ObjectId oid11 = insertAndAdd(points1);
        final ObjectId oid12 = insertAndAdd(points2);
        final ObjectId oid13 = insertAndAdd(points3);
        final RevCommit commit1 = geogit.commit().setAll(true).call();

        final ObjectId oid21 = insertAndAdd(lines1);
        final RevCommit commit2 = geogit.commit().setAll(true).call();

        deleteAndAdd(points1);
        deleteAndAdd(points3);
        final RevCommit commit3 = geogit.commit().setAll(true).call();

        List<DiffEntry> diffs;

        // filter on namespace1, no changes between commit1 and commit2
        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit3.getId());
        diffOp.setFilter(pointsName);

        diffs = toList(diffOp.call());
        assertEquals(2, diffs.size());
        assertEquals(ChangeType.REMOVED, diffs.get(0).changeType());
        assertEquals(ChangeType.REMOVED, diffs.get(1).changeType());

        assertEquals(oid11, diffs.get(0).oldObjectId());
        assertEquals(oid13, diffs.get(1).oldObjectId());
    }

    @Test
    public void testTreeDeletes() throws Exception {

        // two commits on different trees
        final ObjectId oid11 = insertAndAdd(points1);
        final ObjectId oid12 = insertAndAdd(points2);
        final ObjectId oid13 = insertAndAdd(points3);
        final RevCommit commit1 = geogit.commit().setAll(true).call();

        final ObjectId oid21 = insertAndAdd(lines1);
        final ObjectId oid22 = insertAndAdd(lines2);
        final RevCommit commit2 = geogit.commit().setAll(true).call();

        deleteAndAdd(points1);
        deleteAndAdd(points2);
        deleteAndAdd(points3);
        final RevCommit commit3 = geogit.commit().setAll(true).call();

        List<DiffEntry> diffs;

        // filter on namespace1, no changes between commit1 and commit2
        diffOp.setOldVersion(commit1.getId());
        diffOp.setNewVersion(Ref.HEAD);
        diffOp.setFilter(pointsName);

        diffs = toList(diffOp.call());
        assertEquals(3, diffs.size());
    }
}
