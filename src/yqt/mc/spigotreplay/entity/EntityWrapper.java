package yqt.mc.spigotreplay.entity;

import java.util.LinkedList;

import org.bukkit.Location;

public class EntityWrapper {

	/*
	 * Entity wrapper for cloning and creating a mirror image of entities as the replay recording begins
	 */
	
	private int oldEID;
	private int newEID;
	private Location l;
	private LinkedList<Integer> spawnedTick = new LinkedList<Integer>();
	
	public EntityWrapper(int oldEID, Location l, int spawnedTick) {
		this.oldEID = oldEID;
		this.l = new Location(l.getWorld(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
		this.spawnedTick.add(spawnedTick); //this is a linkedlist because respawns cause duplicate times
		this.newEID = this.oldEID + 2000; //probably a bad idea to just assume that there will never be more than 2000 entities...but for testing this is fine
	}
	
	public int getOldEID() {
		return this.oldEID;
	}
	
	public int getNewEID() {
		return this.newEID;
	}
	
	public Location getSpawnLocation() {
		return this.l;
	}
	
	public LinkedList<Integer> getSpawnTime() {
		return this.spawnedTick;
	}
	
	public void addSpawnTime(int time) {
		this.spawnedTick.add(time);
	}
}
