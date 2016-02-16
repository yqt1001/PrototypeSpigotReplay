package yqt.mc.spigotreplay.entity;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;

public class PlayerWrapper extends EntityWrapper {

	private String name;
	private UUID oldUUID;
	private UUID newUUID;
	private Location currentLoc;
	private ItemStack itemInHandAtSpawn;
	private ItemStack armourContentsAtSpawn[];
	private WrappedDataWatcher metadata;
	private PlayerInfoData profile;
	
	public PlayerWrapper(int oldEID, Location l, String name, UUID oldUUID, ItemStack itemInHand, ItemStack armour[], WrappedDataWatcher dw, GameMode gm, WrappedGameProfile wgp) {
		this(oldEID, l, -1, name, oldUUID, itemInHand, armour, dw, gm, wgp);
	}
	
	public PlayerWrapper(int oldEID, Location l, int spawnedTick, String name, UUID oldUUID, ItemStack itemInHand, ItemStack armour[], WrappedDataWatcher dw, GameMode gm, WrappedGameProfile wgp) {
		super(oldEID, l, spawnedTick);
		
		this.name = name;
		this.oldUUID = oldUUID;
		this.newUUID = UUID.randomUUID();
		this.currentLoc = this.getSpawnLocation();
		this.itemInHandAtSpawn = new ItemStack(itemInHand.getType(), itemInHand.getAmount(), itemInHand.getDurability());
		this.armourContentsAtSpawn = new ItemStack[] {armour[0], armour[1], armour[2], armour[3]};
		this.metadata = dw;

		WrappedGameProfile newWGP = new WrappedGameProfile(this.newUUID, "§a" + this.name);
		newWGP.getProperties().removeAll("textures");
		newWGP.getProperties().putAll("textures", wgp.getProperties().get("textures"));
		
		this.profile = new PlayerInfoData(newWGP, 100, NativeGameMode.fromBukkit(gm), WrappedChatComponent.fromText("§a" + this.name));
	}
	
	public String getName() {
		return this.name;
	}
	
	public UUID getOldUUID() {
		return this.oldUUID;
	}
	
	public UUID getNewUUID() {
		return this.newUUID;
	}
	
	public Location getCurrentLoc() {
		return this.currentLoc;
	}
	
	public void setCurrentLoc(Location l) {
		this.currentLoc = l;
	}
	
	public ItemStack getItemInHandAtSpawn() {
		return this.itemInHandAtSpawn;
	}
	
	public ItemStack[] getArmourContentsAtSpawn() {
		return this.armourContentsAtSpawn;
	}
	
	public WrappedDataWatcher getMetadataAtSpawn() {
		return this.metadata;
	}
	
	public PlayerInfoData getProfile() {
		return this.profile;
	}

}
