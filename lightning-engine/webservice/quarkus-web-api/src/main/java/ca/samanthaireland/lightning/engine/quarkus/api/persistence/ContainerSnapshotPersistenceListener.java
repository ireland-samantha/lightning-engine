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

import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.internal.TickListener;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Container-scoped snapshot persistence listener.
 *
 * <p>This listener is registered with each container's GameLoop to persist
 * snapshots for that specific container's matches to MongoDB.
 *
 * <p>Unlike the global MongoSnapshotPersistenceService, this listener:
 * <ul>
 *   <li>Only persists snapshots for matches within its container</li>
 *   <li>Uses the container's own SnapshotProvider</li>
 *   <li>Is registered per-container, not globally</li>
 * </ul>
 */
public class ContainerSnapshotPersistenceListener implements TickListener {
    private static final Logger log = LoggerFactory.getLogger(ContainerSnapshotPersistenceListener.class);

    private final long containerId;
    private final ExecutionContainer container;
    private final MongoClient mongoClient;
    private final String databaseName;
    private final String collectionName;
    private final int tickInterval;

    private volatile MongoCollection<Document> collection;

    /**
     * Create a new container-scoped snapshot persistence listener.
     *
     * @param containerId    the container ID
     * @param container      the container to persist snapshots for
     * @param mongoClient    the MongoDB client
     * @param databaseName   the database name
     * @param collectionName the collection name
     * @param tickInterval   the tick interval for persistence (1 = every tick)
     */
    public ContainerSnapshotPersistenceListener(long containerId,
                                                  ExecutionContainer container,
                                                  MongoClient mongoClient,
                                                  String databaseName,
                                                  String collectionName,
                                                  int tickInterval) {
        this.containerId = containerId;
        this.container = container;
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.tickInterval = tickInterval;

        log.info("Container {} snapshot persistence initialized: database={}, collection={}, tickInterval={}",
                containerId, databaseName, collectionName, tickInterval);
    }

    @Override
    public void onTickComplete(long tick) {
        // Check if we should persist this tick
        if (tickInterval > 1 && tick % tickInterval != 0) {
            return;
        }

        try {
            persistSnapshots(tick);
        } catch (Exception e) {
            log.error("Failed to persist snapshots for container {} at tick {}", containerId, tick, e);
        }
    }

    /**
     * Persist snapshots for all matches in this container.
     */
    private void persistSnapshots(long tick) {
        List<Match> matches = container.matches().all();
        if (matches.isEmpty()) {
            return;
        }

        MongoCollection<Document> coll = getCollection();
        List<Document> documents = new ArrayList<>();
        Instant now = Instant.now();

        for (Match match : matches) {
            try {
                Snapshot snapshot = container.snapshots().forMatch(match.id());
                if (snapshot != null && !snapshot.isEmpty()) {
                    Document doc = toDocument(containerId, match.id(), tick, now, snapshot);
                    documents.add(doc);
                }
            } catch (Exception e) {
                log.warn("Failed to create snapshot for container {} match {} at tick {}",
                        containerId, match.id(), tick, e);
            }
        }

        if (!documents.isEmpty()) {
            coll.insertMany(documents);
            log.debug("Container {}: Persisted {} snapshots for tick {}", containerId, documents.size(), tick);
        }
    }

    /**
     * Convert a snapshot to a MongoDB document.
     *
     * <p>Stores data in legacy format (without version strings) for MongoDB
     * POJO codec compatibility. Version information is not stored in MongoDB.
     */
    private Document toDocument(long containerId, long matchId, long tick, Instant timestamp, Snapshot snapshot) {
        Document doc = new Document();
        doc.append("containerId", containerId);
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
                    MongoDatabase database = mongoClient.getDatabase(databaseName);
                    collection = database.getCollection(collectionName);
                    log.debug("Container {}: Connected to MongoDB collection: {}.{}",
                            containerId, databaseName, collectionName);
                }
            }
        }
        return collection;
    }
}
