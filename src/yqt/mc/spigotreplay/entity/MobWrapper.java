package yqt.mc.spigotreplay.entity;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class MobWrapper extends EntityWrapper {

	private EntityType type;
	private Vector velocity;
	private WrappedDataWatcher metadata;
	
	public MobWrapper(int oldEID, Location l, EntityType type, Vector v, WrappedDataWatcher dw) {
		this(oldEID, l, -1, type, v, dw);
	}
	
	public MobWrapper(int oldEID, Location l, int spawnedTick, EntityType type, Vector v, WrappedDataWatcher dw) {
		super(oldEID, l, spawnedTick);
		
		this.type = type;
		this.velocity = v;
		this.metadata = dw;
	}
	
	public EntityType getType() {
		return this.type;
	}
	
	public Vector getSpawnVelocity() {
		return this.velocity;
	}
	
	public WrappedDataWatcher getMetadataAtSpawn() {
		return this.metadata;
	}

}
