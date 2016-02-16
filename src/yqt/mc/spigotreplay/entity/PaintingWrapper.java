package yqt.mc.spigotreplay.entity;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class PaintingWrapper extends EntityWrapper {

	private String title;
	private BlockFace direction;
	
	public PaintingWrapper(int oldEID, Location l, String title, BlockFace dir) {
		this(oldEID, l, -1, title, dir);
	}
	
	public PaintingWrapper(int oldEID, Location l, int spawnedTick, String title, BlockFace dir) {
		super(oldEID, l, spawnedTick);
		
		this.title = title;
		this.direction = dir;
	}

	public String getTitle() {
		return this.title;
	}
	
	public BlockFace getDirection() {
		return this.direction;
	}
	
}
