package gov.nasa.arc.dert.scenegraph;

import gov.nasa.arc.dert.util.MathUtil;
import gov.nasa.arc.dert.util.Tessellator;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Provides a node for managing sets of points.
 *
 */
public class PointSet extends Node {

	// List of points
	private ArrayList<ReadOnlyVector3> pointList;

	// For polygon
	private Tessellator tessellator;

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public PointSet(String name) {
		super(name);
		pointList = new ArrayList<ReadOnlyVector3>();
	}

	/**
	 * Add a point to the set.
	 * 
	 * @param p
	 *            the point
	 * @param index
	 *            the index to insert the point, -1 indicates to add at the end
	 * @return the index where the point was added
	 */
	public int addPoint(BillboardMarker p, int index) {
		if (index >= getNumberOfChildren() || (index < 0)) {
			attachChild(p);
			index = getChildIndex(p);
		} else {
			attachChildAt(p, index);
			index = getChildIndex(p);
		}
		updateGeometricState(0);
		return (index);
	}

	/**
	 * Remove a point from the set.
	 * 
	 * @param point
	 */
	public void removePoint(BillboardMarker point) {
		detachChild(point);
	}

	private ArrayList<ReadOnlyVector3> getPointList() {
		pointList.clear();
		for (int i = 0; i < getNumberOfChildren(); ++i) {
			pointList.add(getChild(i).getTranslation());
		}
		return (pointList);
	}

	/**
	 * Find the centroid of this point set.
	 * 
	 * @return the centroid
	 */
	public Vector3 getCentroid() {
		int n = getNumberOfChildren();
		if (n == 0) {
			return (null);
		}
		double x = 0;
		double y = 0;
		double z = 0;
		for (int i = 0; i < n; ++i) {
			ReadOnlyVector3 trans = getChild(i).getWorldTranslation();
			x += trans.getX();
			y += trans.getY();
			z += trans.getZ();
		}
		return (new Vector3(x / n, y / n, z / n));
	}

	/**
	 * Get the exact bounds of this point set.
	 * 
	 * @param lowerBound
	 * @param upperBound
	 */
	public void getBounds(Vector3 lowerBound, Vector3 upperBound) {
		int n = getNumberOfChildren();
		if (n == 0) {
			lowerBound.set(0, 0, 0);
			upperBound.set(0, 0, 0);
			return;
		}
		double xMin = Double.MAX_VALUE;
		double xMax = -Double.MAX_VALUE;
		double yMin = Double.MAX_VALUE;
		double yMax = -Double.MAX_VALUE;
		double zMin = Double.MAX_VALUE;
		double zMax = -Double.MAX_VALUE;
		for (int i = 0; i < n; ++i) {
			ReadOnlyVector3 vertex = getChild(i).getWorldTranslation();
			xMin = Math.min(xMin, vertex.getX());
			xMax = Math.max(xMax, vertex.getX());
			yMin = Math.min(yMin, vertex.getY());
			yMax = Math.max(yMax, vertex.getY());
			zMin = Math.min(zMin, vertex.getZ());
			zMax = Math.max(zMax, vertex.getZ());
		}
		lowerBound.set(xMin, yMin, zMin);
		upperBound.set(xMax, yMax, zMax);
	}

	/**
	 * Get the distance along the point set.
	 * 
	 * @return
	 */
	public double getDistance() {
		int pointCount = getNumberOfChildren();
		if (pointCount == 0) {
			return (0);
		}
		Spatial wp0 = getChild(0);
		double distance = 0;
		for (int i = 0; i < pointCount; ++i) {
			Spatial wp = getChild(i);
			ReadOnlyVector3 p0 = wp0.getTranslation();
			ReadOnlyVector3 p1 = wp.getTranslation();
			double d = p0.distance(p1);
			distance += d;
			wp0 = wp;
		}
		return (distance);
	}

	/**
	 * Estimate the area covered by the region defined by these points.
	 * 
	 * @return the area
	 */
	public double getArea() {
		double area = Math.abs(MathUtil.computePolygonArea2D(getPointList()));
		return (area);
	}

	/**
	 * Create the line among points
	 */
	public Line createLine() {
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(3);
		vertexBuffer.limit(0);
		Line line = new Line("_polyline");
		line.getMeshData().setIndexMode(IndexMode.LineStrip);
		line.getMeshData().setVertexBuffer(vertexBuffer);
		line.getSceneHints().setCastsShadows(false);
		line.getSceneHints().setTextureCombineMode(TextureCombineMode.Off);
		line.setModelBound(new BoundingBox());
		line.updateModelBound();
		line.getSceneHints().setCullHint(CullHint.Always);
		line.getSceneHints().setPickingHint(PickingHint.Pickable, false);
		return (line);
	}

	/**
	 * Create the polygon among points
	 * 
	 * @return
	 */
	public Mesh createPolygon() {
		tessellator = new Tessellator();
		Mesh poly = new Mesh("_polygon");
		poly.setModelBound(new BoundingBox());
		poly.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
		poly.getSceneHints().setCullHint(CullHint.Always);
		poly.getSceneHints().setPickingHint(PickingHint.Pickable, false);
		poly.getSceneHints().setCastsShadows(false);
		return (poly);
	}

	/**
	 * Update the line from the points
	 * 
	 * @param line
	 */
	public void updateLine(Mesh line) {
		int pointCount = getNumberOfChildren();
		if (pointCount < 2) {
			return;
		}
		FloatBuffer vertexBuffer = line.getMeshData().getVertexBuffer();
		if ((pointCount * 3) > vertexBuffer.capacity()) {
			vertexBuffer = BufferUtils.createFloatBuffer(pointCount * 3);
		}
		vertexBuffer.clear();
		ReadOnlyVector3 trans = null;
		for (int i = 0; i < pointCount; ++i) {
			int j = i * 3;
			trans = getChild(i).getTranslation();
			vertexBuffer.put(j, trans.getXf());
			vertexBuffer.put(j + 1, trans.getYf());
			vertexBuffer.put(j + 2, trans.getZf());
		}
		vertexBuffer.limit(pointCount * 3);
		vertexBuffer.rewind();
		line.getMeshData().setVertexBuffer(vertexBuffer);
		line.updateModelBound();
	}

	/**
	 * Update the polygon from the points
	 * 
	 * @param poly
	 */
	public void updatePolygon(Mesh poly) {
		if (getNumberOfChildren() < 3) {
			return;
		}
		FloatBuffer vertexBuffer = tessellator.tessellate(getPointList(), null);
		FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(vertexBuffer.capacity());
		MathUtil.computeNormals(vertexBuffer, normalBuffer, true);
		poly.getMeshData().setVertexBuffer(vertexBuffer);
		poly.getMeshData().setNormalBuffer(normalBuffer);
		poly.markDirty(DirtyType.Bounding);
		poly.updateModelBound();
		poly.updateGeometricState(0);
	}
}