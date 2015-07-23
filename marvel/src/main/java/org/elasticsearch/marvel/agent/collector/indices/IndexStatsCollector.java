/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.marvel.agent.collector.indices;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettingsService;

import java.util.Collection;

/**
 * Collector for indices statistics.
 * <p/>
 * This collector runs on the master node only and collect a {@link IndexStatsMarvelDoc} document
 * for each existing index in the cluster.
 */
public class IndexStatsCollector extends AbstractCollector<IndexStatsCollector> {

    public static final String NAME = "index-stats-collector";
    public static final String TYPE = "marvel_index_stats";

    private final ClusterName clusterName;
    private final Client client;
    private final MarvelSettingsService marvelSettings;

    @Inject
    public IndexStatsCollector(Settings settings, ClusterService clusterService, ClusterName clusterName, Client client, MarvelSettingsService marvelSettings) {
        super(settings, NAME, clusterService);
        this.client = client;
        this.clusterName = clusterName;
        this.marvelSettings = marvelSettings;
    }

    @Override
    protected boolean masterOnly() {
        return true;
    }

    @Override
    protected Collection<MarvelDoc> doCollect() throws Exception {
        ImmutableList.Builder<MarvelDoc> results = ImmutableList.builder();

        IndicesStatsResponse indicesStats = client.admin().indices().prepareStats()
                .setStore(true)
                .setIndexing(true)
                .setDocs(true)
                .setIndices(marvelSettings.indices())
                .get(marvelSettings.indexStatsTimeout());

        long timestamp = System.currentTimeMillis();
        for (IndexStats indexStats : indicesStats.getIndices().values()) {
            results.add(buildMarvelDoc(clusterName.value(), TYPE, timestamp, indexStats));
        }
        return results.build();
    }

    protected MarvelDoc buildMarvelDoc(String clusterName, String type, long timestamp, IndexStats indexStats) {
        return IndexStatsMarvelDoc.createMarvelDoc(clusterName, type, timestamp, indexStats);
    }
}
