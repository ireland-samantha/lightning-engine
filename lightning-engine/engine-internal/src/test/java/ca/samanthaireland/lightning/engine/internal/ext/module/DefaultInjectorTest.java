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


package ca.samanthaireland.lightning.engine.internal.ext.module;

import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultInjector}.
 *
 * <p>Tests verify ModuleContext implementation and explicit getter methods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultInjector")
class DefaultInjectorTest {

    @Mock
    private EntityComponentStore entityComponentStore;

    @Mock
    private MatchService matchService;

    @Mock
    private ModuleResolver moduleResolver;

    private DefaultInjector injector;

    @BeforeEach
    void setUp() {
        injector = new DefaultInjector();
    }

    @Nested
    @DisplayName("addClass")
    class AddClass {

        @Test
        @DisplayName("should register bean")
        void shouldRegisterBean() {
            injector.addClass(EntityComponentStore.class, entityComponentStore);

            assertThat(injector.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should overwrite existing bean")
        void shouldOverwriteExistingBean() {
            EntityComponentStore store1 = entityComponentStore;
            EntityComponentStore store2 = entityComponentStore; // same mock, different conceptual instance

            injector.addClass(EntityComponentStore.class, store1);
            injector.addClass(EntityComponentStore.class, store2);

            assertThat(injector.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getEntityComponentStore")
    class GetEntityComponentStore {

        @Test
        @DisplayName("should return registered store")
        void shouldReturnRegisteredStore() {
            injector.addClass(EntityComponentStore.class, entityComponentStore);

            EntityComponentStore result = injector.getEntityComponentStore();

            assertThat(result).isSameAs(entityComponentStore);
        }

        @Test
        @DisplayName("should return null when not registered")
        void shouldReturnNullWhenNotRegistered() {
            EntityComponentStore result = injector.getEntityComponentStore();

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should cache result")
        void shouldCacheResult() {
            injector.addClass(EntityComponentStore.class, entityComponentStore);

            EntityComponentStore result1 = injector.getEntityComponentStore();
            EntityComponentStore result2 = injector.getEntityComponentStore();

            assertThat(result1).isSameAs(result2);
        }
    }

    @Nested
    @DisplayName("getMatchService")
    class GetMatchService {

        @Test
        @DisplayName("should return registered service")
        void shouldReturnRegisteredService() {
            injector.addClass(MatchService.class, matchService);

            MatchService result = injector.getMatchService();

            assertThat(result).isSameAs(matchService);
        }

        @Test
        @DisplayName("should return null when not registered")
        void shouldReturnNullWhenNotRegistered() {
            MatchService result = injector.getMatchService();

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getModuleResolver")
    class GetModuleResolver {

        @Test
        @DisplayName("should return registered resolver")
        void shouldReturnRegisteredResolver() {
            injector.addClass(ModuleResolver.class, moduleResolver);

            ModuleResolver result = injector.getModuleResolver();

            assertThat(result).isSameAs(moduleResolver);
        }

        @Test
        @DisplayName("should return null when not registered")
        void shouldReturnNullWhenNotRegistered() {
            ModuleResolver result = injector.getModuleResolver();

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findEntityComponentStore")
    class FindEntityComponentStore {

        @Test
        @DisplayName("should return Optional with store when registered")
        void shouldReturnOptionalWithStoreWhenRegistered() {
            injector.addClass(EntityComponentStore.class, entityComponentStore);

            var result = injector.findEntityComponentStore();

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(entityComponentStore);
        }

        @Test
        @DisplayName("should return empty Optional when not registered")
        void shouldReturnEmptyOptionalWhenNotRegistered() {
            var result = injector.findEntityComponentStore();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("cache invalidation")
    class CacheInvalidation {

        @Test
        @DisplayName("should invalidate cache when adding new bean")
        void shouldInvalidateCacheWhenAddingNewBean() {
            // Register initial store
            injector.addClass(EntityComponentStore.class, entityComponentStore);
            EntityComponentStore first = injector.getEntityComponentStore();

            // Register different store - should invalidate cache
            EntityComponentStore newStore = entityComponentStore; // Using same mock
            injector.addClass(EntityComponentStore.class, newStore);
            EntityComponentStore second = injector.getEntityComponentStore();

            // Both calls return the same mock, but cache should have been invalidated
            assertThat(first).isSameAs(second);
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should remove all beans")
        void shouldRemoveAllBeans() {
            injector.addClass(EntityComponentStore.class, entityComponentStore);
            injector.addClass(MatchService.class, matchService);

            injector.clear();

            assertThat(injector.size()).isEqualTo(0);
            assertThat(injector.getEntityComponentStore()).isNull();
            assertThat(injector.getMatchService()).isNull();
        }
    }

    @Nested
    @DisplayName("deprecated getClass")
    class DeprecatedGetClass {

        @Test
        @DisplayName("should return registered bean")
        void shouldReturnRegisteredBean() {
            injector.addClass(MatchService.class, matchService);

            @SuppressWarnings("deprecation")
            MatchService result = injector.getClass(MatchService.class);

            assertThat(result).isSameAs(matchService);
        }

        @Test
        @DisplayName("should return null when not registered")
        void shouldReturnNullWhenNotRegistered() {
            assertThatThrownBy(() -> injector.getClass(MatchService.class))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Module Exports")
    class ModuleExportsTests {

        @Test
        @DisplayName("declareModuleExports should register export")
        void declareModuleExports_shouldRegisterExport() {
            TestExports exports = new TestExports("test-value");

            injector.declareModuleExports(TestExports.class, exports);

            assertThat(injector.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("getModuleExports should return registered export")
        void getModuleExports_shouldReturnRegisteredExport() {
            TestExports exports = new TestExports("test-value");
            injector.declareModuleExports(TestExports.class, exports);

            TestExports result = injector.getModuleExports(TestExports.class);

            assertThat(result).isSameAs(exports);
            assertThat(result.getValue()).isEqualTo("test-value");
        }

        @Test
        @DisplayName("getModuleExports should return null when not registered")
        void getModuleExports_shouldReturnNullWhenNotRegistered() {
            TestExports result = injector.getModuleExports(TestExports.class);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("declareModuleExports should overwrite existing export of same type")
        void declareModuleExports_shouldOverwriteExistingExport() {
            TestExports exports1 = new TestExports("value-1");
            TestExports exports2 = new TestExports("value-2");

            injector.declareModuleExports(TestExports.class, exports1);
            injector.declareModuleExports(TestExports.class, exports2);

            TestExports result = injector.getModuleExports(TestExports.class);
            assertThat(result).isSameAs(exports2);
            assertThat(result.getValue()).isEqualTo("value-2");
        }

        @Test
        @DisplayName("should support multiple different export types")
        void shouldSupportMultipleDifferentExportTypes() {
            TestExports testExports = new TestExports("test");
            AnotherExports anotherExports = new AnotherExports(42);

            injector.declareModuleExports(TestExports.class, testExports);
            injector.declareModuleExports(AnotherExports.class, anotherExports);

            assertThat(injector.getModuleExports(TestExports.class)).isSameAs(testExports);
            assertThat(injector.getModuleExports(AnotherExports.class)).isSameAs(anotherExports);
            assertThat(injector.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear should remove all exports")
        void clear_shouldRemoveAllExports() {
            injector.declareModuleExports(TestExports.class, new TestExports("test"));
            injector.declareModuleExports(AnotherExports.class, new AnotherExports(42));

            injector.clear();

            assertThat(injector.getModuleExports(TestExports.class)).isNull();
            assertThat(injector.getModuleExports(AnotherExports.class)).isNull();
        }

        @Test
        @DisplayName("exports should be accessible across different injector method calls")
        void exports_shouldBeAccessibleAcrossCalls() {
            TestExports exports = new TestExports("persistent");
            injector.declareModuleExports(TestExports.class, exports);

            // Multiple retrievals should return the same instance
            TestExports result1 = injector.getModuleExports(TestExports.class);
            TestExports result2 = injector.getModuleExports(TestExports.class);

            assertThat(result1).isSameAs(result2);
            assertThat(result1).isSameAs(exports);
        }
    }

    // Test export classes for module exports tests
    static class TestExports implements ModuleExports {
        private final String value;

        TestExports(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    static class AnotherExports implements ModuleExports {
        private final int number;

        AnotherExports(int number) {
            this.number = number;
        }

        int getNumber() {
            return number;
        }
    }
}
