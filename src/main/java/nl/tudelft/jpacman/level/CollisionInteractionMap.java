package nl.tudelft.jpacman.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.tudelft.jpacman.board.Unit;

/**
 * A map of possible collisions and their handlers.
 *
 * @author Michael de Jong
 * @author Jeroen Roosen 
 */
public class CollisionInteractionMap implements CollisionMap {

    /**
     * The collection of collision handlers.
     */
    private final Map<Class<? extends Unit>,
        Map<Class<? extends Unit>, CollisionHandler<?, ?>>> handlers;

    /**
     * Creates a new, empty collision map.
     */
    public CollisionInteractionMap() {
        this.handlers = new HashMap<>();
    }

    /**
     * Adds a two-way collision interaction to this collection, i.e. the
     * collision handler will be used for both C1 versus C2 and C2 versus C1.
     *
     * @param <C1>
     *            The collider type.
     * @param <C2>
     *            The collidee (unit that was moved into) type.
     *
     * @param collider
     *            The collider type.
     * @param collidee
     *            The collidee type.
     * @param handler
     *            The handler that handles the collision.
     */
    public <C1 extends Unit, C2 extends Unit> void onCollision(
        Class<C1> collider, Class<C2> collidee, CollisionHandler<C1, C2> handler) {
        onCollision(collider, collidee, true, handler);
    }

    /**
     * Adds a collision interaction to this collection.
     *
     * @param <C1>
     *            The collider type.
     * @param <C2>
     *            The collidee (unit that was moved into) type.
     *
     * @param collider
     *            The collider type.
     * @param collidee
     *            The collidee type.
     * @param symetric
     *            <code>true</code> if this collision is used for both
     *            C1 against C2 and vice versa;
     *            <code>false</code> if only for C1 against C2.
     * @param handler
     *            The handler that handles the collision.
     */
    public <C1 extends Unit, C2 extends Unit> void onCollision(
        Class<C1> collider, Class<C2> collidee, boolean symetric,
        CollisionHandler<C1, C2> handler) {
        addCollisionHandler(collider, collidee, handler);
        if (symetric) {
            addCollisionHandler(collidee, collider, new InverseCollisionHandler<>(handler));
        }
    }

    /**
     * Adds the collision interaction..
     *
     * @param colliderClass
     *            The colliderClass type.
     * @param collideeClass
     *            The collideeClass type.
     * @param handler
     *            The handler that handles the collision.
     */
    private void addCollisionHandler(Class<? extends Unit> colliderClass,
                                     Class<? extends Unit> collideeClass, CollisionHandler<?, ?> handler) {
        if (!handlers.containsKey(colliderClass)) {
            handlers.put(colliderClass, new HashMap<>());
        }


        Map<Class<? extends Unit>, CollisionHandler<?, ?>> map = handlers.get(colliderClass);
        map.put(collideeClass, handler);
    }

    /**
     * Handles the collision between two colliding parties, if a suitable
     * collision handler is listed.
     *
     * @param <C1>
     *            The collider type.
     * @param <C2>
     *            The collidee (unit that was moved into) type.
     *
     * @param collider
     *            The collider.
     * @param collidee
     *            The collidee.
     */
    @Override
    public <C1 extends Unit, C2 extends Unit> void collide(C1 collider,
                                                           C2 collidee) {
        CollisionHandler<C1, C2> collisionHandler = getCollisionHandler(collider, collidee);
        if (collisionHandler == null) return;

        collisionHandler.handleCollision(collider, collidee);
    }

    @SuppressWarnings("unchecked")
    private <C1 extends Unit, C2 extends Unit> CollisionHandler<C1, C2> getCollisionHandler(C1 collider, C2 collidee) {
        Class<? extends Unit> colliderKey = getMostSpecificClass(handlers, collider.getClass());
        if (colliderKey == null) {
            return null;
        }

        Map<Class<? extends Unit>, CollisionHandler<?, ?>> map = handlers.get(colliderKey);
        Class<? extends Unit> collideeKey = getMostSpecificClass(map, collidee.getClass());
        if (collideeKey == null) {
            return null;
        }

        return (CollisionHandler<C1, C2>) map.get(collideeKey);
    }

    /**
     * Figures out the most specific class that is listed in the map. I.e. if A
     * extends B and B is listed while requesting A, then B will be returned.
     *
     * @param map
     *            The map with the key collection to find a matching class in.
     * @param key
     *            The class to search the most suitable key for.
     * @return The most specific class from the key collection.
     */
    private Class<? extends Unit> getMostSpecificClass(
        Map<Class<? extends Unit>, ?> map, Class<? extends Unit> key) {
        List<Class<? extends Unit>> collideeInheritance = getInheritance(key);
        for (Class<? extends Unit> pointer : collideeInheritance) {
            if (map.containsKey(pointer)) {
                return pointer;
            }
        }
        return null;
    }

    /**
     * Returns a list of all classes and interfaces the class inherits.
     *
     * @param clazz
     *            The class to create a list of super classes and interfaces
     *            for.
     * @return A list of all classes and interfaces the class inherits.
     */
    @SuppressWarnings("unchecked")
    private List<Class<? extends Unit>> getInheritance(
        Class<? extends Unit> clazz) {
        List<Class<? extends Unit>> inheritanceChain = new ArrayList<>();
        inheritanceChain.add(clazz);

        int currentIndex = 0;
        while (currentIndex < inheritanceChain.size()) {
            Class<?> current = inheritanceChain.get(currentIndex);
            addSuperclassToInheritanceChain(current, inheritanceChain);
            addInterfacesToInheritanceChain(current, inheritanceChain);
            currentIndex++;
        }

        return inheritanceChain;
    }

    private static void addInterfacesToInheritanceChain(Class<?> current, List<Class<? extends Unit>> found) {
        for (Class<?> classInterface : current.getInterfaces()) {
            if (Unit.class.isAssignableFrom(classInterface)) {
                found.add((Class<? extends Unit>) classInterface);
            }
        }
    }

    private static void addSuperclassToInheritanceChain(Class<?> current, List<Class<? extends Unit>> found) {
        Class<?> superClass = current.getSuperclass();
        if (superClass != null && Unit.class.isAssignableFrom(superClass)) {
            found.add((Class<? extends Unit>) superClass);
        }
    }

    /**
     * Handles the collision between two colliding parties.
     *
     * @author Michael de Jong
     *
     * @param <C1>
     *            The collider type.
     * @param <C2>
     *            The collidee type.
     */
    public interface CollisionHandler<C1 extends Unit, C2 extends Unit> {

        /**
         * Handles the collision between two colliding parties.
         *
         * @param collider
         *            The collider.
         * @param collidee
         *            The collidee.
         */
        void handleCollision(C1 collider, C2 collidee);
    }

    /**
     * An symmetrical copy of a collision hander.
     *
     * @author Michael de Jong
     *
     * @param <C1>
     *            The collider type.
     * @param <C2>
     *            The collidee type.
     */
    private static class InverseCollisionHandler<C1 extends Unit, C2 extends Unit>
        implements CollisionHandler<C1, C2> {

        /**
         * The handler of this collision.
         */
        private final CollisionHandler<C2, C1> handler;

        /**
         * Creates a new collision handler.
         *
         * @param handler
         *            The symmetric handler for this collision.
         */
        InverseCollisionHandler(CollisionHandler<C2, C1> handler) {
            this.handler = handler;
        }

        /**
         * Handles this collision by flipping the collider and collidee, making
         * it compatible with the initial collision.
         */
        @Override
        public void handleCollision(C1 collider, C2 collidee) {
            handler.handleCollision(collidee, collider);
        }
    }

}
