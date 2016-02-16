package yqt.mc.spigotreplay.entity;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class ObjectWrapper extends EntityWrapper {

	//Static constant of entity types that use this wrapper according to protocollib
	public static final HashMap<EntityType, Integer> VALID_OBJECTS = new HashMap<EntityType, Integer>();
	static {
		VALID_OBJECTS.put(EntityType.BOAT, 1);
		VALID_OBJECTS.put(EntityType.DROPPED_ITEM, 2);
		VALID_OBJECTS.put(EntityType.MINECART, 10);
		VALID_OBJECTS.put(EntityType.PRIMED_TNT, 50);
		VALID_OBJECTS.put(EntityType.ENDER_CRYSTAL, 51);
		VALID_OBJECTS.put(EntityType.ARROW, 60);
		VALID_OBJECTS.put(EntityType.SNOWBALL, 61);
		VALID_OBJECTS.put(EntityType.EGG, 62);
		VALID_OBJECTS.put(EntityType.FIREBALL, 63);
		VALID_OBJECTS.put(EntityType.SMALL_FIREBALL, 64);
		VALID_OBJECTS.put(EntityType.ENDER_PEARL, 65);
		VALID_OBJECTS.put(EntityType.WITHER_SKULL, 66);
		VALID_OBJECTS.put(EntityType.FALLING_BLOCK, 70);
		VALID_OBJECTS.put(EntityType.ITEM_FRAME, 71);
		VALID_OBJECTS.put(EntityType.ENDER_SIGNAL, 72);
		VALID_OBJECTS.put(EntityType.SPLASH_POTION, 73);
		VALID_OBJECTS.put(EntityType.THROWN_EXP_BOTTLE, 75);
		VALID_OBJECTS.put(EntityType.FIREWORK, 76);
		VALID_OBJECTS.put(EntityType.LEASH_HITCH, 77);
		VALID_OBJECTS.put(EntityType.ARMOR_STAND, 78);
		VALID_OBJECTS.put(EntityType.FISHING_HOOK, 90);
	}
	
	
	private EntityType type;
	private int objectData;
	
	public ObjectWrapper(int oldEID, Location l, EntityType type, int objectData) {
		this(oldEID, l, -1, type, objectData);
	}
	
	public ObjectWrapper(int oldEID, Location l, int spawnedTick, EntityType type, int objectData) {
		super(oldEID, l, spawnedTick);
		
		this.type = type;
		this.objectData = objectData;
	}
	
	public EntityType getType() {
		return this.type;
	}

	public int getObjectData() {
		return this.objectData;
	}
}
