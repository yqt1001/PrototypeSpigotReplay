package yqt.mc.spigotreplay.entity;

import org.bukkit.Location;

public class OrbWrapper extends EntityWrapper {

	private int count;
	
	public OrbWrapper(int oldEID, Location l, int count) {
		this(oldEID, l, -1, count);
	}
	
	public OrbWrapper(int oldEID, Location l, int spawnedTick, int count) {
		super(oldEID, l, spawnedTick);
		
		this.count = count;
	}
	
	public int getCount() {
		return this.count;
	}

}
