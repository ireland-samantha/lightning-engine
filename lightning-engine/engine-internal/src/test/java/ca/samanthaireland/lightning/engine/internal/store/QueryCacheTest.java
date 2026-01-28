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


package ca.samanthaireland.lightning.engine.internal.store;

import ca.samanthaireland.lightning.engine.internal.core.store.QueryCache;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class QueryCacheTest {

    private QueryCache queryCache = new QueryCache();

    private static final int COMPONENT_1 = 1;
    private static final int COMPONENT_2 = 2;

    @Test
    void getSet_roundTrip_oneComponent() {
        Set<Long> expected = Set.of(123L);
        queryCache.put(expected, COMPONENT_1);
        Set<Long> actual = queryCache.get(COMPONENT_1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getSet_roundTrip_multipleComponent() {
        Set<Long> expected = Set.of(123L);
        queryCache.put(expected, COMPONENT_1, COMPONENT_2);
        Set<Long> actual = queryCache.get(COMPONENT_1, COMPONENT_2);
        assertThat(actual).isEqualTo(expected);
    }
}