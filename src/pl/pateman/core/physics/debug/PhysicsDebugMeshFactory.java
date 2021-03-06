package pl.pateman.core.physics.debug;

import com.bulletphysics.collision.shapes.*;
import org.joml.Vector3f;
import pl.pateman.core.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
final class PhysicsDebugMeshFactory {
    private static final javax.vecmath.Vector3f aabbMin = new javax.vecmath.Vector3f(-1e30f, -1e30f, -1e30f);
    private static final javax.vecmath.Vector3f aabbMax = new javax.vecmath.Vector3f(1e30f, 1e30f, 1e30f);

    private PhysicsDebugMeshFactory() {

    }

    static void getMeshVertices(final CollisionShape collisionShape, final List<Vector3f> vertices,
                                final List<Integer> indices) {

        if (vertices == null || indices == null) {
            throw new IllegalArgumentException("Vertices and indices lists are required");
        }

        //  Check the type of the collision shape and generate the mesh.
        List<Vector3f> result = null;
        if (collisionShape instanceof ConcaveShape) {
            final ArrayListTriangleCallback callback = new ArrayListTriangleCallback();
            ((ConcaveShape) collisionShape).processAllTriangles(callback, aabbMin, aabbMax);

            result = callback.getTriangles();
        } else if (collisionShape instanceof ConvexShape) {
            //  Build the shape hull.
            final ShapeHull shapeHull = new ShapeHull((ConvexShape) collisionShape);
            shapeHull.buildHull(collisionShape.getMargin());

            //  If the hull is valid, process it and fetch triangles.
            final int numOfTriangles = shapeHull.numTriangles();
            if (numOfTriangles > 0) {
                result = new ArrayList<>(numOfTriangles);

                javax.vecmath.Vector3f tmp;
                int index = 0;
                for (int i = 0; i < numOfTriangles; i++) {
                    //  First vertex.
                    tmp = shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(index++));
                    result.add(Utils.convert(new Vector3f(), tmp));

                    //  Second vertex.
                    tmp = shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(index++));
                    result.add(Utils.convert(new Vector3f(), tmp));

                    //  Third vertex.
                    tmp = shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(index++));
                    result.add(Utils.convert(new Vector3f(), tmp));
                }
            }
        }

        if (result != null) {
            vertices.addAll(result);
            for (int i = 0; i < result.size(); i++) {
                indices.add(i);
            }
        }
    }

    private static class ArrayListTriangleCallback extends TriangleCallback {
        private final List<Vector3f> triangles;

        ArrayListTriangleCallback() {
            this.triangles = new ArrayList<>();
        }

        @Override
        public void processTriangle(javax.vecmath.Vector3f[] triangle, int partId, int triangleIndex) {
            this.triangles.add(Utils.convert(new Vector3f(), triangle[0]));
            this.triangles.add(Utils.convert(new Vector3f(), triangle[1]));
            this.triangles.add(Utils.convert(new Vector3f(), triangle[2]));
        }

        List<Vector3f> getTriangles() {
            return triangles;
        }
    }
}
