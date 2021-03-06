package gov.nasa.arc.dert.scenegraph;

import gov.nasa.arc.dert.Dert;
import gov.nasa.arc.dert.view.world.MoveEdit;

import java.util.ArrayList;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;

/**
 * Abstract base class for objects that can be moved along the terrain.
 *
 */
public abstract class Movable extends Node {

	private boolean pinned, inMotion;
	protected boolean strictZ;
	private ArrayList<MotionListener> listeners;

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public Movable(String name) {
		super(name);
		listeners = new ArrayList<MotionListener>();
	}

	/**
	 * Determine mobility
	 * 
	 * @return
	 */
	public boolean isPinned() {
		return (pinned);
	}

	/**
	 * Set mobility
	 * 
	 * @param pinned
	 */
	public void setPinned(boolean pinned) {
		this.pinned = pinned;
		if (pinned) {
			inMotion = false;
		}
		markDirty(DirtyType.RenderState);
	}

	/**
	 * Determine if moving
	 * 
	 * @return
	 */
	public boolean isInMotion() {
		return (inMotion);
	}

	/**
	 * Set moving
	 * 
	 * @param inMotion
	 * @param pickPosition
	 */
	public void setInMotion(boolean inMotion, ReadOnlyVector3 pickPosition) {
		this.inMotion = inMotion;
		enableHighlight(inMotion);
	}
	
	public boolean isStrictZ() {
		return(strictZ);
	}
	
	public void setStrictZ(boolean strictZ) {
		this.strictZ = strictZ;
	}

	protected abstract void enableHighlight(boolean enable);

	/**
	 * Add listener
	 * 
	 * @param mol
	 */
	public void addMotionListener(MotionListener mol) {
		listeners.add(mol);
	}

	/**
	 * Remove listener
	 * 
	 * @param mol
	 */
	public void removeMotionListener(MotionListener mol) {
		listeners.remove(mol);
	}

	/**
	 * Notify listeners
	 */
	public void notifyListeners() {
		if (inMotion) {
			updateListeners();
		}
	}

	/**
	 * Notify listeners
	 */
	public void updateListeners() {
		ReadOnlyVector3 position = getTranslation();
		for (int i = 0; i < listeners.size(); ++i) {
			listeners.get(i).move(this, position);
		}
	}

	/**
	 * Get the radius of this movable.
	 * 
	 * @return
	 */
	public double getRadius() {
		updateWorldTransform(true);
		updateWorldBound(true);
		BoundingVolume wb = getWorldBound();
		if (wb == null) {
			return (1);
		}
		ReadOnlyVector3 s = getWorldScale();
		return (wb.getRadius() / s.getX());
	}

	/**
	 * Set the location
	 * 
	 * @param i
	 * @param p
	 */
	public void setLocation(double x, double y, double z, boolean doEdit, boolean zOnly) {
		if (doEdit)
			Dert.getMainWindow().getUndoHandler().addEdit(new MoveEdit(this, new Vector3(getTranslation()), strictZ));
		setTranslation(x, y, z);
		updateListeners();
	}
}
