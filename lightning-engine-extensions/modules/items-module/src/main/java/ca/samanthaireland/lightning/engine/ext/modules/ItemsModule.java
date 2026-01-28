package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemTypeRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ItemService;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ItemTypeService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.CreateItemTypeCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.DropItemCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.GetItemTypesCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.PickupItemCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.SpawnItemCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.UseItemCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsItemRepository;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.InMemoryItemTypeRepository;

import java.util.List;

import static ca.samanthaireland.lightning.engine.ext.modules.ItemsModuleFactory.ALL_COMPONENTS;
import static ca.samanthaireland.lightning.engine.ext.modules.ItemsModuleFactory.FLAG;

/**
 * Items module implementation.
 *
 * <p>Provides item type definitions and inventory management:
 * <ul>
 *   <li>createItemType - Define a new item type with properties</li>
 *   <li>spawnItem - Create an item entity in the world</li>
 *   <li>pickupItem - Add item to entity's inventory</li>
 *   <li>dropItem - Remove item from inventory and place in world</li>
 *   <li>useItem - Trigger item use effect</li>
 * </ul>
 */
public class ItemsModule implements EngineModule {

    private final ItemTypeService itemTypeService;
    private final ItemService itemService;

    public ItemsModule(ModuleContext context) {
        ItemTypeRepository itemTypeRepository = new InMemoryItemTypeRepository();
        ItemRepository itemRepository = new EcsItemRepository(
                context.getEntityComponentStore()
        );

        this.itemTypeService = new ItemTypeService(itemTypeRepository);
        this.itemService = new ItemService(itemRepository, itemTypeRepository);
    }

    @Override
    public List<EngineSystem> createSystems() {
        return List.of(); // Items are event-driven, no tick systems needed
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                CreateItemTypeCommand.create(itemTypeService),
                SpawnItemCommand.create(itemService),
                PickupItemCommand.create(itemService),
                DropItemCommand.create(itemService),
                UseItemCommand.create(itemService),
                GetItemTypesCommand.create(itemTypeService)
        );
    }

    @Override
    public List<BaseComponent> createComponents() {
        return ALL_COMPONENTS;
    }

    @Override
    public BaseComponent createFlagComponent() {
        return FLAG;
    }

    @Override
    public String getName() {
        return "ItemsModule";
    }
}
