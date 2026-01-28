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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for creating {@link SnapshotParser} instances.
 *
 * <p>This factory provides a way to create SnapshotParser instances for
 * library usage without direct dependency on the DI container. For Quarkus
 * production usage, inject SnapshotParser directly as a CDI bean.
 */
public final class SnapshotParserFactory {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private SnapshotParserFactory() {
        // Utility class
    }

    /**
     * Create a new SnapshotParser with the default ObjectMapper.
     *
     * @return a new SnapshotParser instance
     */
    public static SnapshotParser create() {
        return new SnapshotParser(DEFAULT_OBJECT_MAPPER);
    }

    /**
     * Create a new SnapshotParser with a custom ObjectMapper.
     *
     * @param objectMapper the ObjectMapper to use for JSON parsing
     * @return a new SnapshotParser instance
     */
    public static SnapshotParser create(ObjectMapper objectMapper) {
        return new SnapshotParser(objectMapper);
    }
}
