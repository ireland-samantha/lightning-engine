package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.ItemType;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemTypeRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of ItemTypeRepository.
 *
 * <p>Item types are stored in memory per match, as they are definitions
 * rather than ECS entities. This allows for fast lookups and isolation
 * between matches.
 */
public class InMemoryItemTypeRepository implements ItemTypeRepository {

    // Item type registry (keyed by matchId -> typeId -> definition)
    private final Map<Long, Map<Long, ItemType>> itemTypesByMatch = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> nextItemTypeIdByMatch = new ConcurrentHashMap<>();

    private Map<Long, ItemType> getItemTypes(long matchId) {
        return itemTypesByMatch.computeIfAbsent(matchId, k -> new ConcurrentHashMap<>());
    }

    private AtomicLong getNextItemTypeId(long matchId) {
        return nextItemTypeIdByMatch.computeIfAbsent(matchId, k -> new AtomicLong(1));
    }

    @Override
    public ItemType save(long matchId, ItemType itemType) {
        long typeId = getNextItemTypeId(matchId).getAndIncrement();
        ItemType savedType = new ItemType(
                typeId,
                itemType.name(),
                itemType.maxStack(),
                itemType.rarity(),
                itemType.value(),
                itemType.weight(),
                itemType.healAmount(),
                itemType.damageBonus(),
                itemType.armorValue()
        );
        getItemTypes(matchId).put(typeId, savedType);
        return savedType;
    }

    @Override
    public Optional<ItemType> findById(long matchId, long itemTypeId) {
        return Optional.ofNullable(getItemTypes(matchId).get(itemTypeId));
    }

    @Override
    public Collection<ItemType> findAllByMatchId(long matchId) {
        return getItemTypes(matchId).values();
    }

    @Override
    public boolean exists(long matchId, long itemTypeId) {
        return getItemTypes(matchId).containsKey(itemTypeId);
    }
}
