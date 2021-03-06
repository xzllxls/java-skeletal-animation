package pl.pateman.jbullethelloworld;

import com.bulletphysics.collision.broadphase.CollisionFilterGroups;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint;
import com.bulletphysics.linearmath.Transform;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.AbstractEntity;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.physics.debug.PhysicsDebugger;
import pl.pateman.core.physics.raycast.PhysicsRaycast;
import pl.pateman.core.physics.raycast.PhysicsRaycastResult;

import javax.vecmath.Vector3f;
import java.util.*;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

/**
 * Created by pateman.
 */
public final class JBulletHelloWorldScene implements Iterable<AbstractEntity>, Clearable {
    public static final Vector3f DEFAULT_GRAVITY = new Vector3f(0.0f, -9.81f, 0.0f);
    private final Map<String, AbstractEntity> entities;
    private final Map<String, Map<String, Object>> parameters;
    private final List<String> physicsBodies;

    private final DynamicsWorld dynamicsWorld;
    private final PhysicsRaycast raycast;
    private final PhysicsDebugger physicsDebugger;

    public JBulletHelloWorldScene() {
        this.entities = new HashMap<>();
        this.parameters = new HashMap<>();
        this.physicsBodies = new ArrayList<>();

        final DbvtBroadphase broadphase = new DbvtBroadphase();
        final DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        final CollisionDispatcher collisionDispatcher = new CollisionDispatcher(collisionConfiguration);
        final SequentialImpulseConstraintSolver constraintSolver = new SequentialImpulseConstraintSolver();

        this.dynamicsWorld = new DiscreteDynamicsWorld(collisionDispatcher, broadphase, constraintSolver,
                collisionConfiguration);
        this.dynamicsWorld.setGravity(DEFAULT_GRAVITY);
        this.raycast = new PhysicsRaycast(this.dynamicsWorld);
        this.physicsDebugger = new PhysicsDebugger(this.dynamicsWorld);
    }

    public <T extends AbstractEntity> T addEntity(final T entityInstance) {
        final String name = entityInstance.getName();

        this.entities.put(name, entityInstance);
        this.parameters.put(name, new HashMap<>());

        return entityInstance;
    }

    public void addEntityToPhysicsWorld(final String name) {
        final AbstractEntity entity = this.entities.get(name);
        if (entity.getRigidBody() == null) {
            throw new IllegalStateException("Entity '" + name + "' does not have a rigid body");
        }
        this.physicsBodies.add(name);

        entity.forceTransformationUpdate(true);
        this.dynamicsWorld.addRigidBody(entity.getRigidBody());
    }

    public <T extends AbstractEntity> T getEntity(final String name) {
        return (T) this.entities.get(name);
    }

    public <T> T getEntityParameter(final String entity, final String parameter) {
        return (T) this.parameters.get(entity).get(parameter);
    }

    public int addConstraint(final TypedConstraint constraint, boolean disableCollisionsBetweenBodies) {
        if (constraint == null) {
            throw new IllegalArgumentException();
        }

        this.dynamicsWorld.addConstraint(constraint, disableCollisionsBetweenBodies);
        return this.dynamicsWorld.getNumConstraints() - 1;
    }

    public <T extends TypedConstraint> T getConstraint(final int idx) {
        return (T) this.dynamicsWorld.getConstraint(idx);
    }

    public void updateScene(float deltaTime) {
        this.dynamicsWorld.stepSimulation(deltaTime, 10);

        final TempVars tempVars = TempVars.get();
        final Transform transform = tempVars.vecmathTransform;
        for (final String physicsBody : this.physicsBodies) {
            final AbstractEntity abstractEntity = this.entities.get(physicsBody);
            abstractEntity.getRigidBody().getMotionState().getWorldTransform(transform);

            //  Convert between different math libraries.
            transform.getRotation(tempVars.vecmathQuat);
            Utils.convert(tempVars.quat1, tempVars.vecmathQuat);
            Utils.convert(tempVars.vect3d1, transform.origin);

            //  Assign transformation computed by jBullet to the entity. We're passing 'false' as the last parameter,
            //  because we don't want to update the rigid body again since we're reading its transformation right now.
            abstractEntity.setTransformation(tempVars.quat1, tempVars.vect3d1, abstractEntity.getScale(), false);
        }
        tempVars.release();

        this.physicsDebugger.updateDebugEntities();
    }

    public org.joml.Vector3f getGravity() {
        final Vector3f gravity = this.dynamicsWorld.getGravity(new Vector3f());
        return new org.joml.Vector3f(gravity.x, gravity.y, gravity.z);
    }

    public void setGravity(org.joml.Vector3f gravity) {
        this.dynamicsWorld.setGravity(new Vector3f(gravity.x, gravity.y, gravity.z));
    }

    public void setEntityParameter(final String entity, final String parameterName, final Object value) {
        this.parameters.get(entity).put(parameterName, value);
    }

    public PhysicsRaycastResult raycast(final org.joml.Vector3f rayOrigin,
                                        final org.joml.Vector3f rayDirection) {
        return this.raycast(rayOrigin, rayDirection, PhysicsRaycast.DEFAULT_RAY_LENGTH,
                CollisionFilterGroups.DEFAULT_FILTER);
    }

    public PhysicsRaycastResult raycast(final org.joml.Vector3f rayOrigin,
                                        final org.joml.Vector3f rayDirection, float rayLength, short collisionGroup) {
        return this.raycast.raycast(rayOrigin, rayDirection, rayLength, collisionGroup);
    }

    public void debugDrawWorld(final CameraEntity cameraEntity) {
        //  Make sure that the debug info is drawn over the geometry.
        glClear(GL_DEPTH_BUFFER_BIT);
        this.physicsDebugger.debugDrawWorld(cameraEntity);
    }

    @Override
    public Iterator<AbstractEntity> iterator() {
        return this.entities.values().iterator();
    }

    @Override
    public void clear() {
        for (AbstractEntity abstractEntity : this) {
            this.dynamicsWorld.removeRigidBody(abstractEntity.getRigidBody());
            abstractEntity.clearAndDestroy();
        }
        this.physicsBodies.clear();
        this.entities.clear();
        this.physicsDebugger.clearAndDestroy();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
    }
}
