/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Manipulates the index (staging area) by setting the unstaged changes that match this operation
 * criteria as staged.
 * 
 * @see WorkingTree
 * @see StagingArea
 */
public class AddOp extends AbstractGeoGitOp<WorkingTree> {

    private Set<String> patterns;

    private boolean updateOnly;

    private WorkingTree workTree;

    private StagingArea index;

    /**
     * Constructs a new {@code AddOp} with the given parameters.
     * 
     * @param workTree the working tree to add stages from
     * @param index the staging area to stage changes to
     */
    @Inject
    public AddOp(final WorkingTree workTree, final StagingArea index) {
        this.index = index;
        this.workTree = workTree;
        patterns = new HashSet<String>();
    }

    /**
     * Executes the add command, staging unstaged changes that match the provided patterns.
     * 
     * @return the modified {@link WorkingTree working tree}.
     */
    public WorkingTree call() {
        // this is add all, TODO: implement partial adds
        String path = null;
        if (patterns.size() == 1) {
            path = patterns.iterator().next();
        }
        stage(getProgressListener(), path);
        return workTree;
    }

    /**
     * Stages the object addressed by {@code pathFilter}, or all unstaged objects if
     * {@code pathFilter == null} to be added, if it is/they are marked as an unstaged change. Does
     * nothing otherwise.
     * <p>
     * To stage changes not yet staged, a diff tree walk is performed using the current staged
     * {@link RevTree} as the old object and the current unstaged {@link RevTree} as the new object.
     * Then all the differences are traversed and the staged tree is updated with the changes
     * reported by the diff walk (neat).
     * </p>
     * 
     * @param progress the progress listener for this process
     * @param pathFilter the filter to use
     */
    public void stage(final ProgressListener progress, final @Nullable String pathFilter) {

        // short cut for the case where the index is empty and we're staging all changes in the
        // working tree, so it's just a matter of updating the index ref to working tree RevTree id
        if (null == pathFilter && !index.getStaged(null).hasNext()) {
            progress.started();
            Optional<ObjectId> workHead = command(RevParse.class).setRefSpec(Ref.WORK_HEAD).call();
            if (workHead.isPresent()) {
                command(UpdateRef.class).setName(Ref.STAGE_HEAD).setNewValue(workHead.get()).call();
            }
            progress.progress(100f);
            progress.complete();
            return;
        }

        final long numChanges = workTree.countUnstaged(pathFilter);

        Iterator<DiffEntry> unstaged = workTree.getUnstaged(pathFilter);

        index.stage(progress, unstaged, numChanges);
    }

    /**
     * @param pattern a regular expression to match what content to be staged
     * @return {@code this}
     */
    public AddOp addPattern(final String pattern) {
        patterns.add(pattern);
        return this;
    }

    /**
     * @param updateOnly if {@code true}, only add already tracked features (either for modification
     *        or deletion), but do not stage any newly added one.
     * @return {@code this}
     */
    public AddOp setUpdateOnly(final boolean updateOnly) {
        this.updateOnly = updateOnly;
        return this;
    }

}
