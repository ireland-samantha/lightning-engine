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


package ca.samanthaireland.lightning.engine.core.entity;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

/**
 * Core components required by the engine infrastructure.
 *
 * <p>These components are fundamental to engine operation and are
 * automatically managed by the engine. Modules should not modify
 * these components directly.
 */
public final class CoreComponents {

    private CoreComponents() {
        // Utility class
    }

    /**
     * Component that stores the match ID an entity belongs to.
     *
     * <p>Entities created via {@code EntityComponentStore.createEntityForMatch()}
     * automatically have this component attached. This enables:
     * <ul>
     *   <li>Querying entities by match</li>
     *   <li>Match isolation and cleanup</li>
     *   <li>Preventing cross-match entity leakage</li>
     * </ul>
     */
    public static final BaseComponent MATCH_ID = new CoreComponent(
            IdGeneratorV2.newId(), "MATCH_ID");

    public static final BaseComponent ENTITY_ID = new CoreComponent(
            IdGeneratorV2.newId(), "ENTITY_ID");

    /**
     * Component that stores the owner (player) ID for an entity.
     *
     * <p>Entities spawned by or owned by a player have this component attached.
     * This enables:
     * <ul>
     *   <li>Player-scoped snapshot filtering</li>
     *   <li>Ownership-based access control</li>
     *   <li>Player-specific entity queries</li>
     * </ul>
     */
    public static final BaseComponent OWNER_ID = new CoreComponent(
            IdGeneratorV2.newId(), "OWNER_ID");

    /**
     * Internal component class for core components.
     */
    private static class CoreComponent extends BaseComponent {
        CoreComponent(long id, String name) {
            super(id, name);
        }
    }
}
