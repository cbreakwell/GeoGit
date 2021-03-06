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
package org.geotools.data.versioning.decorator;

import java.io.IOException;
import java.util.Iterator;

import org.geogit.api.GeoGIT;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.hessian.GeoToolsRevFeatureType;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public class QueryFeatureCollector implements Iterable<Feature> {

    private final GeoGIT geogit;

    private final FeatureType featureType;

    private Query query;

    public QueryFeatureCollector(final GeoGIT repository, final FeatureType featureType, Query query) {
        this.geogit = repository;
        this.featureType = featureType;
        this.query = query;
    }

    @Override
    public Iterator<Feature> iterator() {

        VersionQuery versionQuery = new VersionQuery(geogit, featureType.getName());
        Iterator<Node> featureNodes;
        try {
            featureNodes = versionQuery.getByQuery(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Iterator<Feature> features = Iterators.transform(featureNodes, new NodeToFeature(
                geogit, featureType));

        return features;
    }

    private final class NodeToFeature implements Function<Node, Feature> {

        private final GeoGIT geogit;

        private final FeatureType type;

        public NodeToFeature(final GeoGIT repo, final FeatureType type) {
            this.geogit = repo;
            this.type = type;
        }

        @Override
        public Feature apply(final Node featureNode) {
            String featureId = featureNode.getPath();
            ObjectId contentId = featureNode.getObjectId();
            StagingDatabase database = geogit.getRepository().getIndex().getDatabase();
            RevFeature feature;
            try {
                ObjectReader<RevFeature> featureReader = geogit.getRepository().newFeatureReader(
                        new GeoToolsRevFeatureType(type), featureId);
                feature = database.get(contentId, featureReader);
                if (!feature.getType().equals(type)) {
                    throw new IOException("Invalid feature type returned.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return VersionedFeatureWrapper.wrap((Feature) feature.feature(), featureNode
                    .getObjectId().toString());
        }

    }

}
