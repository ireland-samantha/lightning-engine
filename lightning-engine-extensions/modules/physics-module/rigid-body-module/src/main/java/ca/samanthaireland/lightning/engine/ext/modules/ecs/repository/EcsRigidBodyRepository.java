package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.GridMapExports;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.RigidBody;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.RigidBodyRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.RigidBodyModuleFactory.*;

/**
 * ECS-backed implementation of RigidBodyRepository.
 *
 * <p>Stores rigid body data as entity components in the EntityComponentStore.
 * Uses GridMapExports for position operations to respect module boundaries.
 */
public class EcsRigidBodyRepository implements RigidBodyRepository {

    private final ModuleContext context;
    private GridMapExports gridMapExports;

    public EcsRigidBodyRepository(ModuleContext context) {
        this.context = context;
    }

    /**
     * Gets the EntityComponentStore dynamically from the context.
     *
     * <p>This is important because the context's store is updated after module
     * initialization when the JWT is issued. Capturing the store in the constructor
     * would result in using an empty/stale store.
     */
    private EntityComponentStore getStore() {
        return context.getEntityComponentStore();
    }

    /**
     * Gets the GridMapExports, resolving it lazily on first access.
     */
    private GridMapExports getGridMapExports() {
        if (gridMapExports == null) {
            gridMapExports = context.getModuleExports(GridMapExports.class);
            if (gridMapExports == null) {
                throw new IllegalStateException(
                        "GridMapExports not available. Ensure GridMapModule is loaded before RigidBodyModule.");
            }
        }
        return gridMapExports;
    }

    @Override
    public void save(RigidBody rigidBody) {
        long entityId = rigidBody.entityId();

        // Position - use EntityModule exports
        getGridMapExports().setPosition(entityId,
                rigidBody.position().x(), rigidBody.position().y(), rigidBody.position().z());

        EntityComponentStore store = getStore();

        // Velocity
        store.attachComponents(entityId, VELOCITY_COMPONENTS,
                new float[]{rigidBody.velocity().x(), rigidBody.velocity().y(), rigidBody.velocity().z()});

        // Acceleration
        store.attachComponents(entityId, ACCELERATION_COMPONENTS,
                new float[]{rigidBody.acceleration().x(), rigidBody.acceleration().y(), rigidBody.acceleration().z()});

        // Force
        store.attachComponents(entityId, FORCE_COMPONENTS,
                new float[]{rigidBody.force().x(), rigidBody.force().y(), rigidBody.force().z()});

        // Physical properties
        store.attachComponent(entityId, MASS, rigidBody.mass());
        store.attachComponent(entityId, LINEAR_DRAG, rigidBody.linearDrag());
        store.attachComponent(entityId, ANGULAR_DRAG, rigidBody.angularDrag());
        store.attachComponent(entityId, INERTIA, rigidBody.inertia());

        // Angular state
        store.attachComponent(entityId, ANGULAR_VELOCITY, rigidBody.angularVelocity());
        store.attachComponent(entityId, ROTATION, rigidBody.rotation());
        store.attachComponent(entityId, TORQUE, rigidBody.torque());

        // Flag
        store.attachComponent(entityId, FLAG, 1.0f);
    }

    @Override
    public Optional<RigidBody> findById(long entityId) {
        EntityComponentStore store = getStore();
        Set<Long> entities = store.getEntitiesWithComponents(List.of(FLAG));
        if (!entities.contains(entityId)) {
            return Optional.empty();
        }

        return Optional.of(buildRigidBody(entityId));
    }

    @Override
    public Set<Long> findAllIds() {
        return getStore().getEntitiesWithComponents(List.of(FLAG));
    }

    @Override
    public boolean exists(long entityId) {
        Set<Long> entities = getStore().getEntitiesWithComponents(List.of(FLAG));
        return entities.contains(entityId);
    }

    @Override
    public void delete(long entityId) {
        EntityComponentStore store = getStore();
        for (var component : CORE_COMPONENTS) {
            store.removeComponent(entityId, component);
        }
        store.removeComponent(entityId, FLAG);
    }

    @Override
    public void updatePosition(long entityId, Vector3 position) {
        getGridMapExports().setPosition(entityId, position.x(), position.y(), position.z());
    }

    @Override
    public void updateVelocity(long entityId, Vector3 velocity) {
        getStore().attachComponents(entityId, VELOCITY_COMPONENTS,
                new float[]{velocity.x(), velocity.y(), velocity.z()});
    }

    @Override
    public void updateForce(long entityId, Vector3 force) {
        getStore().attachComponents(entityId, FORCE_COMPONENTS,
                new float[]{force.x(), force.y(), force.z()});
    }

    @Override
    public void updateAcceleration(long entityId, Vector3 acceleration) {
        getStore().attachComponents(entityId, ACCELERATION_COMPONENTS,
                new float[]{acceleration.x(), acceleration.y(), acceleration.z()});
    }

    @Override
    public void updateAngular(long entityId, float angularVelocity, float rotation, float torque) {
        EntityComponentStore store = getStore();
        store.attachComponent(entityId, ANGULAR_VELOCITY, angularVelocity);
        store.attachComponent(entityId, ROTATION, rotation);
        store.attachComponent(entityId, TORQUE, torque);
    }

    private RigidBody buildRigidBody(long entityId) {
        // Get position from EntityModule exports
        Position pos = getGridMapExports().getPosition(entityId).orElse(Position.origin());
        Vector3 position = new Vector3(pos.x(), pos.y(), pos.z());

        EntityComponentStore store = getStore();

        Vector3 velocity = new Vector3(
                store.getComponent(entityId, VELOCITY_X),
                store.getComponent(entityId, VELOCITY_Y),
                store.getComponent(entityId, VELOCITY_Z)
        );

        Vector3 acceleration = new Vector3(
                store.getComponent(entityId, ACCELERATION_X),
                store.getComponent(entityId, ACCELERATION_Y),
                store.getComponent(entityId, ACCELERATION_Z)
        );

        Vector3 force = new Vector3(
                store.getComponent(entityId, FORCE_X),
                store.getComponent(entityId, FORCE_Y),
                store.getComponent(entityId, FORCE_Z)
        );

        float mass = store.getComponent(entityId, MASS);
        float linearDrag = store.getComponent(entityId, LINEAR_DRAG);
        float angularDrag = store.getComponent(entityId, ANGULAR_DRAG);
        float inertia = store.getComponent(entityId, INERTIA);

        float angularVelocity = store.getComponent(entityId, ANGULAR_VELOCITY);
        float rotation = store.getComponent(entityId, ROTATION);
        float torque = store.getComponent(entityId, TORQUE);

        // Ensure mass and inertia are valid for domain object creation
        float validMass = mass <= 0 ? 1.0f : mass;
        float validInertia = inertia <= 0 ? 1.0f : inertia;

        return new RigidBody(
                entityId,
                position,
                velocity,
                acceleration,
                force,
                validMass,
                linearDrag,
                angularDrag,
                angularVelocity,
                rotation,
                torque,
                validInertia
        );
    }
}
