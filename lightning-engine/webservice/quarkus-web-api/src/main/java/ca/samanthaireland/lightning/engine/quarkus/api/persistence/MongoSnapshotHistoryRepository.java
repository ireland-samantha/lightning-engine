/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package ca.samanthaireland.lightning.engine.quarkus.api.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB implementation of {@link SnapshotHistoryRepository}.
 *
 * <p>Stores snapshots in a MongoDB collection with indexes on matchId and tick.
 */
public class MongoSnapshotHistoryRepository implements SnapshotHistoryRepository {
    private static final Logger log = LoggerFactory.getLogger(MongoSnapshotHistoryRepository.class);

    private final MongoClient mongoClient;
    private final String databaseName;
    private final String collectionName;
    private volatile MongoCollection<Document> collection;

    public MongoSnapshotHistoryRepository(MongoClient mongoClient, String databaseName, String collectionName) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.collectionName = collectionName;
    }

    // =========================================================================
    // CONTAINER-SCOPED METHODS
    // =========================================================================

    @Override
    public Optional<SnapshotDocument> findByContainerAndMatchIdAndTick(long containerId, long matchId, long tick) {
        Document doc = getCollection()
                .find(Filters.and(
                        Filters.eq("containerId", containerId),
                        Filters.eq("matchId", matchId),
                        Filters.eq("tick", tick)))
                .first();
        return Optional.ofNullable(doc).map(this::toSnapshotDocument);
    }

    @Override
    public List<SnapshotDocument> findByContainerAndMatchIdAndTickBetween(long containerId, long matchId, long fromTick, long toTick, int limit) {
        List<SnapshotDocument> result = new ArrayList<>();
        getCollection()
                .find(Filters.and(
                        Filters.eq("containerId", containerId),
                        Filters.eq("matchId", matchId),
                        Filters.gte("tick", fromTick),
                        Filters.lte("tick", toTick)))
                .sort(Sorts.ascending("tick"))
                .limit(limit)
                .forEach(doc -> result.add(toSnapshotDocument(doc)));
        return result;
    }

    @Override
    public List<SnapshotDocument> findLatestByContainerAndMatchId(long containerId, long matchId, int limit) {
        List<SnapshotDocument> result = new ArrayList<>();
        getCollection()
                .find(Filters.and(
                        Filters.eq("containerId", containerId),
                        Filters.eq("matchId", matchId)))
                .sort(Sorts.descending("tick"))
                .limit(limit)
                .forEach(doc -> result.add(toSnapshotDocument(doc)));
        return result;
    }

    @Override
    public Optional<SnapshotDocument> findFirstByContainerAndMatchId(long containerId, long matchId) {
        Document doc = getCollection()
                .find(Filters.and(
                        Filters.eq("containerId", containerId),
                        Filters.eq("matchId", matchId)))
                .sort(Sorts.ascending("tick"))
                .first();
        return Optional.ofNullable(doc).map(this::toSnapshotDocument);
    }

    @Override
    public Optional<SnapshotDocument> findLastByContainerAndMatchId(long containerId, long matchId) {
        Document doc = getCollection()
                .find(Filters.and(
                        Filters.eq("containerId", containerId),
                        Filters.eq("matchId", matchId)))
                .sort(Sorts.descending("tick"))
                .first();
        return Optional.ofNullable(doc).map(this::toSnapshotDocument);
    }

    @Override
    public long countByContainerAndMatchId(long containerId, long matchId) {
        return getCollection().countDocuments(Filters.and(
                Filters.eq("containerId", containerId),
                Filters.eq("matchId", matchId)));
    }

    @Override
    public long countByContainerId(long containerId) {
        return getCollection().countDocuments(Filters.eq("containerId", containerId));
    }

    @Override
    public List<Long> findDistinctMatchIdsByContainerId(long containerId) {
        List<Long> matchIds = new ArrayList<>();
        getCollection()
                .distinct("matchId", Filters.eq("containerId", containerId), Long.class)
                .forEach(matchIds::add);
        return matchIds;
    }

    @Override
    public long deleteByContainerAndMatchId(long containerId, long matchId) {
        return getCollection().deleteMany(Filters.and(
                Filters.eq("containerId", containerId),
                Filters.eq("matchId", matchId))).getDeletedCount();
    }

    @Override
    public long deleteByContainerAndMatchIdAndTickLessThan(long containerId, long matchId, long olderThanTick) {
        return getCollection().deleteMany(
                Filters.and(
                        Filters.eq("containerId", containerId),
                        Filters.eq("matchId", matchId),
                        Filters.lt("tick", olderThanTick)))
                .getDeletedCount();
    }

    // =========================================================================
    // LEGACY METHODS
    // =========================================================================

    @Override
    public Optional<SnapshotDocument> findByMatchIdAndTick(long matchId, long tick) {
        Document doc = getCollection()
                .find(Filters.and(
                        Filters.eq("matchId", matchId),
                        Filters.eq("tick", tick)))
                .first();
        return Optional.ofNullable(doc).map(this::toSnapshotDocument);
    }

    @Override
    public List<SnapshotDocument> findByMatchIdAndTickBetween(long matchId, long fromTick, long toTick, int limit) {
        List<SnapshotDocument> result = new ArrayList<>();
        getCollection()
                .find(Filters.and(
                        Filters.eq("matchId", matchId),
                        Filters.gte("tick", fromTick),
                        Filters.lte("tick", toTick)))
                .sort(Sorts.ascending("tick"))
                .limit(limit)
                .forEach(doc -> result.add(toSnapshotDocument(doc)));
        return result;
    }

    @Override
    public List<SnapshotDocument> findByMatchIdAndTimestampBetween(long matchId, Instant from, Instant to, int limit) {
        List<SnapshotDocument> result = new ArrayList<>();
        getCollection()
                .find(Filters.and(
                        Filters.eq("matchId", matchId),
                        Filters.gte("timestamp", from),
                        Filters.lte("timestamp", to)))
                .sort(Sorts.ascending("timestamp"))
                .limit(limit)
                .forEach(doc -> result.add(toSnapshotDocument(doc)));
        return result;
    }

    @Override
    public List<SnapshotDocument> findLatestByMatchId(long matchId, int limit) {
        List<SnapshotDocument> result = new ArrayList<>();
        getCollection()
                .find(Filters.eq("matchId", matchId))
                .sort(Sorts.descending("tick"))
                .limit(limit)
                .forEach(doc -> result.add(toSnapshotDocument(doc)));
        return result;
    }

    @Override
    public Optional<SnapshotDocument> findFirstByMatchId(long matchId) {
        Document doc = getCollection()
                .find(Filters.eq("matchId", matchId))
                .sort(Sorts.ascending("tick"))
                .first();
        return Optional.ofNullable(doc).map(this::toSnapshotDocument);
    }

    @Override
    public Optional<SnapshotDocument> findLastByMatchId(long matchId) {
        Document doc = getCollection()
                .find(Filters.eq("matchId", matchId))
                .sort(Sorts.descending("tick"))
                .first();
        return Optional.ofNullable(doc).map(this::toSnapshotDocument);
    }

    @Override
    public long countByMatchId(long matchId) {
        return getCollection().countDocuments(Filters.eq("matchId", matchId));
    }

    @Override
    public long countAll() {
        return getCollection().countDocuments();
    }

    @Override
    public List<Long> findDistinctMatchIds() {
        List<Long> matchIds = new ArrayList<>();
        getCollection().distinct("matchId", Long.class).forEach(matchIds::add);
        return matchIds;
    }

    @Override
    public SnapshotDocument save(SnapshotDocument snapshot) {
        Document doc = toDocument(snapshot);
        if (snapshot.id() == null) {
            getCollection().insertOne(doc);
            ObjectId id = doc.getObjectId("_id");
            return new SnapshotDocument(id, snapshot.containerId(), snapshot.matchId(), snapshot.tick(),
                    snapshot.timestamp(), snapshot.data());
        } else {
            doc.append("_id", snapshot.id());
            getCollection().replaceOne(Filters.eq("_id", snapshot.id()), doc);
            return snapshot;
        }
    }

    @Override
    public long deleteByMatchId(long matchId) {
        return getCollection().deleteMany(Filters.eq("matchId", matchId)).getDeletedCount();
    }

    @Override
    public long deleteByMatchIdAndTickLessThan(long matchId, long olderThanTick) {
        return getCollection().deleteMany(
                Filters.and(
                        Filters.eq("matchId", matchId),
                        Filters.lt("tick", olderThanTick)))
                .getDeletedCount();
    }

    private MongoCollection<Document> getCollection() {
        if (collection == null) {
            synchronized (this) {
                if (collection == null) {
                    MongoDatabase database = mongoClient.getDatabase(databaseName);
                    collection = database.getCollection(collectionName);
                    log.debug("Connected to MongoDB collection: {}.{}", databaseName, collectionName);
                }
            }
        }
        return collection;
    }

    private Document toDocument(SnapshotDocument snapshot) {
        Document doc = new Document();
        doc.append("containerId", snapshot.containerId());
        doc.append("matchId", snapshot.matchId());
        doc.append("tick", snapshot.tick());
        doc.append("timestamp", snapshot.timestamp());

        Document dataDoc = new Document();
        if (snapshot.data() != null) {
            for (Map.Entry<String, Map<String, List<Float>>> moduleEntry : snapshot.data().entrySet()) {
                Document moduleDoc = new Document();
                for (Map.Entry<String, List<Float>> componentEntry : moduleEntry.getValue().entrySet()) {
                    moduleDoc.append(componentEntry.getKey(), componentEntry.getValue());
                }
                dataDoc.append(moduleEntry.getKey(), moduleDoc);
            }
        }
        doc.append("data", dataDoc);

        return doc;
    }

    @SuppressWarnings("unchecked")
    private SnapshotDocument toSnapshotDocument(Document doc) {
        Document dataDoc = doc.get("data", Document.class);
        Map<String, Map<String, List<Float>>> data = new LinkedHashMap<>();

        if (dataDoc != null) {
            for (String moduleName : dataDoc.keySet()) {
                Document moduleDoc = dataDoc.get(moduleName, Document.class);
                Map<String, List<Float>> moduleData = new LinkedHashMap<>();
                if (moduleDoc != null) {
                    for (String componentName : moduleDoc.keySet()) {
                        List<Number> values = moduleDoc.getList(componentName, Number.class);
                        if (values != null) {
                            moduleData.put(componentName,
                                    values.stream().map(Number::floatValue).toList());
                        }
                    }
                }
                data.put(moduleName, moduleData);
            }
        }

        // Handle legacy documents that may not have containerId (default to 0)
        Long containerId = doc.getLong("containerId");
        if (containerId == null) {
            containerId = 0L;
        }

        // MongoDB stores timestamps as java.util.Date, convert to Instant
        Date timestampDate = doc.getDate("timestamp");
        Instant timestamp = timestampDate != null ? timestampDate.toInstant() : Instant.now();

        return new SnapshotDocument(
                doc.getObjectId("_id"),
                containerId,
                doc.getLong("matchId"),
                doc.getLong("tick"),
                timestamp,
                data
        );
    }
}
