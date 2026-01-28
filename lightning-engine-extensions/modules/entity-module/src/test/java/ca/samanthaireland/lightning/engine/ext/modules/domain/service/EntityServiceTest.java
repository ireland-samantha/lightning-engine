package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Entity;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EntityService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntityService")
class EntityServiceTest {

    @Mock
    private EntityRepository entityRepository;

    @Mock
    private MatchService matchService;

    @Mock
    private ModuleResolver moduleResolver;

    @Mock
    private ModuleContext moduleContext;

    @Mock
    private EntityComponentStore entityComponentStore;

    private EntityService entityService;

    @BeforeEach
    void setUp() {
        lenient().when(moduleContext.getEntityComponentStore()).thenReturn(entityComponentStore);
        entityService = new EntityService(
                entityRepository,
                matchService,
                moduleResolver,
                moduleContext
        );
    }

    @Nested
    @DisplayName("spawn")
    class Spawn {

        @Test
        @DisplayName("should create entity")
        void shouldCreateEntity() {
            long matchId = 1L;
            long entityType = 100L;
            long playerId = 42L;
            Entity savedEntity = new Entity(999L, entityType, playerId);

            when(entityRepository.save(eq(matchId), any(Entity.class))).thenReturn(savedEntity);
            when(matchService.getMatch(matchId)).thenReturn(Optional.empty());

            Entity result = entityService.spawn(matchId, entityType, playerId);

            assertThat(result).isEqualTo(savedEntity);
            verify(entityRepository).save(eq(matchId), any(Entity.class));
        }

        @Test
        @DisplayName("should attach module flags when match has enabled modules")
        void shouldAttachModuleFlagsWhenMatchHasEnabledModules() {
            long matchId = 1L;
            long entityType = 100L;
            long playerId = 42L;
            Entity savedEntity = new Entity(999L, entityType, playerId);
            Match match = new Match(matchId, List.of("TestModule"), List.of());

            EngineModule mockModule = mock(EngineModule.class);
            BaseComponent flagComponent = mock(BaseComponent.class);

            when(entityRepository.save(eq(matchId), any(Entity.class))).thenReturn(savedEntity);
            when(matchService.getMatch(matchId)).thenReturn(Optional.of(match));
            when(moduleResolver.resolveModule("TestModule")).thenReturn(mockModule);
            when(mockModule.createFlagComponent()).thenReturn(flagComponent);

            entityService.spawn(matchId, entityType, playerId);

            verify(entityComponentStore).attachComponent(savedEntity.id(), flagComponent, 1.0f);
        }

        @Test
        @DisplayName("should not fail when match service is null")
        void shouldNotFailWhenMatchServiceIsNull() {
            EntityService serviceWithNullMatchService = new EntityService(
                    entityRepository,
                    null,
                    moduleResolver,
                    moduleContext
            );

            long matchId = 1L;
            Entity savedEntity = new Entity(999L, 100L, 42L);
            when(entityRepository.save(eq(matchId), any(Entity.class))).thenReturn(savedEntity);

            Entity result = serviceWithNullMatchService.spawn(matchId, 100L, 42L);

            assertThat(result).isEqualTo(savedEntity);
        }

        @Test
        @DisplayName("should not fail when module resolver is null")
        void shouldNotFailWhenModuleResolverIsNull() {
            EntityService serviceWithNullResolver = new EntityService(
                    entityRepository,
                    matchService,
                    null,
                    moduleContext
            );

            long matchId = 1L;
            Match match = new Match(matchId, List.of("TestModule"), List.of());
            Entity savedEntity = new Entity(999L, 100L, 42L);

            when(entityRepository.save(eq(matchId), any(Entity.class))).thenReturn(savedEntity);
            when(matchService.getMatch(matchId)).thenReturn(Optional.of(match));

            Entity result = serviceWithNullResolver.spawn(matchId, 100L, 42L);

            assertThat(result).isEqualTo(savedEntity);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return entity when found")
        void shouldReturnEntityWhenFound() {
            long entityId = 999L;
            Entity entity = new Entity(entityId, 100L, 42L);
            when(entityRepository.findById(entityId)).thenReturn(Optional.of(entity));

            Optional<Entity> result = entityService.findById(entityId);

            assertThat(result).contains(entity);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            long entityId = 999L;
            when(entityRepository.findById(entityId)).thenReturn(Optional.empty());

            Optional<Entity> result = entityService.findById(entityId);

            assertThat(result).isEmpty();
        }
    }
}
