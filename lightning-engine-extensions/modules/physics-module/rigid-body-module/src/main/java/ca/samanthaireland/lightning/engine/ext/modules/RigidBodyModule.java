package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.RigidBodyRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.PhysicsService;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.RigidBodyService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.*;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsRigidBodyRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.RigidBodyModuleFactory.*;

/**
 * RigidBody module implementation.
 *
 * <p>Provides physics simulation with:
 * <ul>
 *   <li>Velocity tracking (vx, vy, vz)</li>
 *   <li>Acceleration (ax, ay, az)</li>
 *   <li>Mass-based force calculations (F = ma)</li>
 *   <li>Angular velocity and rotation</li>
 *   <li>Drag/damping for realistic movement</li>
 * </ul>
 *
 * <p>The physics system runs each tick:
 * <ol>
 *   <li>Apply forces to calculate acceleration (a = F/m)</li>
 *   <li>Apply acceleration to velocity (v += a * dt)</li>
 *   <li>Apply velocity to position (p += v * dt)</li>
 *   <li>Apply drag to velocity (v *= (1 - drag))</li>
 *   <li>Clear accumulated forces</li>
 * </ol>
 *
 * <p>Note: Position operations use GridMapExports (position components are in GridMapModule).
 */
@Slf4j
public class RigidBodyModule implements EngineModule {
    private final ModuleContext context;
    private final RigidBodyService rigidBodyService;
    private final PhysicsService physicsService;
    private final List<Long> deleteQueue = new ArrayList<>();

    // Lazily resolved reference to GridMapModule's exports for position management
    private GridMapExports gridMapExports;

    // Physics timestep (assuming 1 tick = 1/60 second)
    private static final float DT = 1.0f / 60.0f;

    public RigidBodyModule(ModuleContext context) {
        this.context = context;

        RigidBodyRepository rigidBodyRepository = new EcsRigidBodyRepository(context);

        this.rigidBodyService = new RigidBodyService(rigidBodyRepository);
        this.physicsService = new PhysicsService(rigidBodyRepository);
    }

    /**
     * Gets the GridMapExports, resolving it lazily on first access.
     * This is necessary because GridMapModule may not be initialized when RigidBodyModule is created.
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
    public List<EngineSystem> createSystems() {
        return List.of(
                createForceIntegrationSystem(),
                createPhysicsSystem(),
                createCleanupSystem()
        );
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                AttachRigidBodyCommand.create(rigidBodyService),
                ApplyForceCommand.create(physicsService),
                ApplyImpulseCommand.create(physicsService),
                SetVelocityCommand.create(physicsService),
                SetPositionCommand.create(physicsService),
                ApplyTorqueCommand.create(physicsService),
                DeleteRigidBodyCommand.create(rigidBodyService, deleteQueue)
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
        return "RigidBodyModule";
    }

    // ========== Systems ==========

    // Pre-computed component ID arrays for batch operations (avoid allocation during tick)
    // Force integration read: mass, forceX, forceY, forceZ, torque, inertia, angularVel
    private static final long[] FORCE_READ_IDS = {
            MASS.getId(), FORCE_X.getId(), FORCE_Y.getId(), FORCE_Z.getId(),
            TORQUE.getId(), INERTIA.getId(), ANGULAR_VELOCITY.getId()
    };

    // Force integration write: accelX, accelY, accelZ, angularVel
    private static final long[] FORCE_WRITE_IDS = {
            ACCELERATION_X.getId(), ACCELERATION_Y.getId(), ACCELERATION_Z.getId(), ANGULAR_VELOCITY.getId()
    };

    /**
     * Force integration system: converts forces to acceleration (a = F/m).
     *
     * <p>Optimized with batch component reads/writes using pre-computed component ID arrays.
     */
    private EngineSystem createForceIntegrationSystem() {
        // Pre-allocate buffers for batch operations (reused each tick)
        final float[] readBuf = new float[FORCE_READ_IDS.length];
        final float[] writeBuf = new float[FORCE_WRITE_IDS.length];

        return () -> {
            EntityComponentStore store = context.getEntityComponentStore();
            Set<Long> entities = store.getEntitiesWithComponents(List.of(FLAG, MASS, FORCE_X));

            for (long entity : entities) {
                // Batch read all force components using pre-computed IDs
                store.getComponents(entity, FORCE_READ_IDS, readBuf);

                float mass = readBuf[0];
                float forceX = readBuf[1];
                float forceY = readBuf[2];
                float forceZ = readBuf[3];
                float torque = readBuf[4];
                float inertia = readBuf[5];
                float angularVel = readBuf[6];

                // Prevent division by zero
                if (mass <= 0) mass = 1.0f;
                if (inertia <= 0) inertia = 1.0f;

                // a = F / m
                float accelX = forceX / mass;
                float accelY = forceY / mass;
                float accelZ = forceZ / mass;

                // Handle angular: torque / inertia = angular acceleration
                float angularAccel = torque / inertia;
                angularVel += angularAccel * DT;

                // Batch write acceleration and angular velocity
                writeBuf[0] = accelX;
                writeBuf[1] = accelY;
                writeBuf[2] = accelZ;
                writeBuf[3] = angularVel;

                store.attachComponents(entity, FORCE_WRITE_IDS, writeBuf);

                log.trace("Entity {} force=({},{},{}) -> accel=({},{},{})",
                        entity, forceX, forceY, forceZ, accelX, accelY, accelZ);
            }
        };
    }

    // Physics read: velX, velY, velZ, accelX, accelY, accelZ, linearDrag, angularDrag, rotation, angularVel
    private static final long[] PHYSICS_READ_IDS = {
            VELOCITY_X.getId(), VELOCITY_Y.getId(), VELOCITY_Z.getId(),
            ACCELERATION_X.getId(), ACCELERATION_Y.getId(), ACCELERATION_Z.getId(),
            LINEAR_DRAG.getId(), ANGULAR_DRAG.getId(),
            ROTATION.getId(), ANGULAR_VELOCITY.getId()
    };

    // Physics write: velX, velY, velZ, rotation, angularVel, forceX, forceY, forceZ, torque
    private static final long[] PHYSICS_WRITE_IDS = {
            VELOCITY_X.getId(), VELOCITY_Y.getId(), VELOCITY_Z.getId(),
            ROTATION.getId(), ANGULAR_VELOCITY.getId(),
            FORCE_X.getId(), FORCE_Y.getId(), FORCE_Z.getId(),
            TORQUE.getId()
    };

    /**
     * Physics integration system: velocity += acceleration, position += velocity.
     * Uses GridMapExports to update positions through the proper module boundary.
     *
     * <p>Optimized with batch component reads/writes using pre-computed component ID arrays.
     */
    private EngineSystem createPhysicsSystem() {
        // Pre-allocate buffers for batch operations (reused each tick)
        final float[] readBuf = new float[PHYSICS_READ_IDS.length];
        final float[] writeBuf = new float[PHYSICS_WRITE_IDS.length];

        return () -> {
            EntityComponentStore store = context.getEntityComponentStore();
            GridMapExports exports = getGridMapExports();
            Set<Long> entities = store.getEntitiesWithComponents(List.of(FLAG));

            for (long entity : entities) {
                // Batch read all physics components using pre-computed IDs
                store.getComponents(entity, PHYSICS_READ_IDS, readBuf);

                float velX = readBuf[0];
                float velY = readBuf[1];
                float velZ = readBuf[2];
                float accelX = readBuf[3];
                float accelY = readBuf[4];
                float accelZ = readBuf[5];
                float linearDrag = readBuf[6];
                float angularDrag = readBuf[7];
                float rotation = readBuf[8];
                float angularVel = readBuf[9];

                // Get current position from GridMap exports
                Position currentPos = exports.getPosition(entity).orElse(Position.origin());
                float posX = currentPos.x();
                float posY = currentPos.y();
                float posZ = currentPos.z();

                // Integrate velocity: v += a * dt
                velX += accelX * DT;
                velY += accelY * DT;
                velZ += accelZ * DT;

                // Apply linear drag: v *= (1 - drag)
                if (linearDrag > 0 && linearDrag < 1) {
                    float dragFactor = 1.0f - linearDrag;
                    velX *= dragFactor;
                    velY *= dragFactor;
                    velZ *= dragFactor;
                }

                // Integrate position: p += v * dt
                posX += velX * DT;
                posY += velY * DT;
                posZ += velZ * DT;

                // Update position through GridMapModule exports
                exports.setPosition(entity, posX, posY, posZ);

                // Apply angular drag
                if (angularDrag > 0 && angularDrag < 1) {
                    angularVel *= (1.0f - angularDrag);
                }

                // Integrate rotation
                rotation += angularVel * DT;

                // Batch write all output components
                // Order: velX, velY, velZ, rotation, angularVel, forceX, forceY, forceZ, torque
                writeBuf[0] = velX;
                writeBuf[1] = velY;
                writeBuf[2] = velZ;
                writeBuf[3] = rotation;
                writeBuf[4] = angularVel;
                writeBuf[5] = 0; // Clear force X
                writeBuf[6] = 0; // Clear force Y
                writeBuf[7] = 0; // Clear force Z
                writeBuf[8] = 0; // Clear torque

                store.attachComponents(entity, PHYSICS_WRITE_IDS, writeBuf);

                log.trace("Entity {} pos=({},{},{}) vel=({},{},{})",
                        entity, posX, posY, posZ, velX, velY, velZ);
            }
        };
    }

    /**
     * Cleanup system for deleted rigid bodies.
     */
    private EngineSystem createCleanupSystem() {
        return () -> {
            for (Long entityId : deleteQueue) {
                rigidBodyService.delete(entityId);
                log.debug("Cleaned up rigid body components for entity {}", entityId);
            }
            deleteQueue.clear();
        };
    }
}
