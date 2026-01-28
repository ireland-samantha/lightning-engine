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

import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.internal.TickListener;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB-based snapshot persistence service.
 *
 * <p>This service implements {@link TickListener} to capture and persist
 * snapshots to MongoDB after each tick (or at configured intervals).
 *
 * <p>Snapshots are stored in a collection with the following structure:
 * <pre>{@code
 * {
 *   "_id": ObjectId(...),
 *   "matchId": 1,
 *   "tick": 42,
 *   "timestamp": ISODate(...),
 *   "data": {
 *     "ModuleName": {
 *       "COMPONENT_NAME": [1.0, 2.0, 3.0],
 *       ...
 *     }
 *   }
 * }
 * }</pre>
 */
public class MongoSnapshotPersistenceService implements TickListener {
    private static final Logger log = LoggerFactory.getLogger(MongoSnapshotPersistenceService.class);

    private final MongoClient mongoClient;
    private final SnapshotProvider snapshotProvider;
    private final MatchService matchService;
    private final SnapshotPersistenceConfig config;

    private volatile MongoCollection<Document> collection;

    /**
     * Create a new MongoDB snapshot persistence service.
     *
     * @param mongoClient the MongoDB client
     * @param snapshotProvider the snapshot provider for capturing state
     * @param matchService the match service for getting active matches
     * @param config the persistence configuration
     */
    public MongoSnapshotPersistenceService(MongoClient mongoClient,
                                           SnapshotProvider snapshotProvider,
                                           MatchService matchService,
                                           SnapshotPersistenceConfig config) {
        this.mongoClient = mongoClient;
        this.snapshotProvider = snapshotProvider;
        this.matchService = matchService;
        this.config = config;
        log.info("MongoDB snapshot persistence initialized: database={}, collection={}, tickInterval={}",
                config.database(), config.collection(), config.tickInterval());
    }

    @Override
    public void onTickComplete(long tick) {
        // Check if we should persist this tick
        if (config.tickInterval() > 1 && tick % config.tickInterval() != 0) {
            return;
        }

        try {
            persistSnapshots(tick);
        } catch (Exception e) {
            log.error("Failed to persist snapshots for tick {}", tick, e);
        }
    }

    /**
     * Persist snapshots for all active matches.
     *
     * @param tick the current tick number
     */
    private void persistSnapshots(long tick) {
        List<Match> matches = matchService.getAllMatches();
        if (matches.isEmpty()) {
            return;
        }

        MongoCollection<Document> coll = getCollection();
        List<Document> documents = new ArrayList<>();
        Instant now = Instant.now();

        for (Match match : matches) {
            try {
                Snapshot snapshot = snapshotProvider.createForMatch(match.id());
                if (snapshot != null && !snapshot.isEmpty()) {
                    Document doc = toDocument(match.id(), tick, now, snapshot);
                    documents.add(doc);
                }
            } catch (Exception e) {
                log.warn("Failed to create snapshot for match {} at tick {}", match.id(), tick, e);
            }
        }

        if (!documents.isEmpty()) {
            coll.insertMany(documents);
            log.debug("Persisted {} snapshots for tick {}", documents.size(), tick);
        }
    }

    /**
     * Convert a snapshot to a MongoDB document.
     *
     * <p>Stores data in legacy format (without version strings) for MongoDB
     * POJO codec compatibility. Version information is not stored in MongoDB.
     */
    private Document toDocument(long matchId, long tick, Instant timestamp, Snapshot snapshot) {
        Document doc = new Document();
        doc.append("matchId", matchId);
        doc.append("tick", tick);
        doc.append("timestamp", timestamp);

        // Convert module data to a Document in legacy format for POJO compatibility
        Document dataDoc = new Document();
        for (var module : snapshot.modules()) {
            Document moduleDoc = new Document();
            for (var component : module.components()) {
                moduleDoc.append(component.name(), component.values());
            }
            dataDoc.append(module.name(), moduleDoc);
        }
        doc.append("data", dataDoc);

        return doc;
    }

    /**
     * Get or create the MongoDB collection.
     */
    private MongoCollection<Document> getCollection() {
        if (collection == null) {
            synchronized (this) {
                if (collection == null) {
                    MongoDatabase database = mongoClient.getDatabase(config.database());
                    collection = database.getCollection(config.collection());
                    log.debug("Connected to MongoDB collection: {}.{}",
                            config.database(), config.collection());
                }
            }
        }
        return collection;
    }
}
