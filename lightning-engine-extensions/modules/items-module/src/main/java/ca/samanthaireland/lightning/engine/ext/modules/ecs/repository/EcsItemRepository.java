package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.EntityModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Item;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.ItemsModuleFactory.*;

/**
 * ECS-backed implementation of ItemRepository.
 *
 * <p>Stores item data as entity components in the EntityComponentStore.
 */
public class EcsItemRepository implements ItemRepository {

    private final EntityComponentStore store;

    public EcsItemRepository(EntityComponentStore store) {
        this.store = store;
    }

    @Override
    public Item save(long matchId, Item item) {
        // Create entity with match ID automatically attached
        long entityId = store.createEntityForMatch(matchId);

        // Attach entity type components
        store.attachComponents(entityId, EntityModuleFactory.CORE_COMPONENTS,
                new float[]{item.itemTypeId(), 0, 0});

        // Position
        store.attachComponents(entityId, GridMapModuleFactory.POSITION_COMPONENTS,
                new float[]{item.positionX(), item.positionY(), 0});

        // Item components
        store.attachComponent(entityId, ITEM_TYPE_ID, item.itemTypeId());
        store.attachComponent(entityId, STACK_SIZE, item.stackSize());
        store.attachComponent(entityId, MAX_STACK, item.maxStack());
        store.attachComponent(entityId, OWNER_ENTITY_ID, item.ownerEntityId());
        store.attachComponent(entityId, SLOT_INDEX, item.slotIndex());
        store.attachComponent(entityId, FLAG, 1.0f);

        // Item type properties
        store.attachComponent(entityId, ITEM_NAME_HASH, item.nameHash());
        store.attachComponent(entityId, ITEM_RARITY, item.rarity());
        store.attachComponent(entityId, ITEM_VALUE, item.value());
        store.attachComponent(entityId, ITEM_WEIGHT, item.weight());
        store.attachComponent(entityId, HEAL_AMOUNT, item.healAmount());
        store.attachComponent(entityId, DAMAGE_BONUS, item.damageBonus());
        store.attachComponent(entityId, ARMOR_VALUE, item.armorValue());

        return new Item(
                entityId,
                item.itemTypeId(),
                item.stackSize(),
                item.maxStack(),
                item.ownerEntityId(),
                item.slotIndex(),
                item.positionX(),
                item.positionY(),
                item.nameHash(),
                item.rarity(),
                item.value(),
                item.weight(),
                item.healAmount(),
                item.damageBonus(),
                item.armorValue()
        );
    }

    @Override
    public Optional<Item> findById(long itemEntityId) {
        Set<Long> itemEntities = store.getEntitiesWithComponents(List.of(FLAG));
        if (!itemEntities.contains(itemEntityId)) {
            return Optional.empty();
        }

        float flag = store.getComponent(itemEntityId, FLAG);
        if (flag <= 0) {
            return Optional.empty();
        }

        return Optional.of(new Item(
                itemEntityId,
                (long) store.getComponent(itemEntityId, ITEM_TYPE_ID),
                (int) store.getComponent(itemEntityId, STACK_SIZE),
                (int) store.getComponent(itemEntityId, MAX_STACK),
                (long) store.getComponent(itemEntityId, OWNER_ENTITY_ID),
                (int) store.getComponent(itemEntityId, SLOT_INDEX),
                store.getComponent(itemEntityId, GridMapModuleFactory.POSITION_X),
                store.getComponent(itemEntityId, GridMapModuleFactory.POSITION_Y),
                (int) store.getComponent(itemEntityId, ITEM_NAME_HASH),
                (int) store.getComponent(itemEntityId, ITEM_RARITY),
                store.getComponent(itemEntityId, ITEM_VALUE),
                store.getComponent(itemEntityId, ITEM_WEIGHT),
                store.getComponent(itemEntityId, HEAL_AMOUNT),
                store.getComponent(itemEntityId, DAMAGE_BONUS),
                store.getComponent(itemEntityId, ARMOR_VALUE)
        ));
    }

    @Override
    public void updateOwnership(long itemEntityId, long ownerEntityId, int slotIndex) {
        store.attachComponent(itemEntityId, OWNER_ENTITY_ID, ownerEntityId);
        store.attachComponent(itemEntityId, SLOT_INDEX, slotIndex);
    }

    @Override
    public void updatePosition(long itemEntityId, float positionX, float positionY) {
        store.attachComponent(itemEntityId, GridMapModuleFactory.POSITION_X, positionX);
        store.attachComponent(itemEntityId, GridMapModuleFactory.POSITION_Y, positionY);
    }

    @Override
    public void updateStackSize(long itemEntityId, int stackSize) {
        store.attachComponent(itemEntityId, STACK_SIZE, stackSize);
    }

    @Override
    public void delete(long itemEntityId) {
        for (BaseComponent c : CORE_COMPONENTS) {
            store.removeComponent(itemEntityId, c);
        }
        store.removeComponent(itemEntityId, FLAG);
    }

    @Override
    public boolean exists(long itemEntityId) {
        Set<Long> itemEntities = store.getEntitiesWithComponents(List.of(FLAG));
        if (!itemEntities.contains(itemEntityId)) {
            return false;
        }
        float flag = store.getComponent(itemEntityId, FLAG);
        return flag > 0;
    }
}
