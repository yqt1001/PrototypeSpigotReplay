package yqt.mc.spigotreplay;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import yqt.mc.spigotreplay.ReplayPlugin.ReplayStatus;
import yqt.mc.spigotreplay.entity.EntityWrapper;
import yqt.mc.spigotreplay.entity.MobWrapper;
import yqt.mc.spigotreplay.entity.ObjectWrapper;
import yqt.mc.spigotreplay.entity.OrbWrapper;
import yqt.mc.spigotreplay.entity.PaintingWrapper;
import yqt.mc.spigotreplay.entity.PlayerWrapper;

public class Replay {

	private ReplayPlugin main;
	private ReplayStatus status = ReplayStatus.RECORDING;
	private int UID;
	
	private World world;
	private int length;
	
	private HashMap<Integer, EntityWrapper> clonedEntities = new HashMap<Integer, EntityWrapper>();
	private HashMap<Integer, LinkedList<PacketContainer>> savedPackets = new HashMap<Integer, LinkedList<PacketContainer>>();
	private LinkedList<PacketContainer> ptr;
	
	public Replay(ReplayPlugin main, World world, int UID) {
		this.main = main;
		this.world = world;
		this.UID = UID;
		this.length = 0;
		
		//prepare to start recording
		this.deepCloneEntities();
		
		//start recording runnable
		this.main.getRunnables().startRecordingRunnable(this);
	}
	
	public void incrementLength() {
		this.length++;
		
		// "I'm sure creating and saving a new array 20 times per second for minutes on end won't cause any horrible memory leaks" That's why this is a prototype!
		this.ptr = new LinkedList<PacketContainer>();
		this.savedPackets.put(this.length, this.ptr);
	}
	
	public int getLength() {
		return this.length;
	}
	
	public ReplayStatus getStatus() {
		return this.status;
	}
	
	public void setStatus(ReplayStatus rs) {
		this.status = rs;
	}
	
	public int getUID() {
		return this.UID;
	}
	
	public World getWorld() {
		return this.world;
	}
	
	public HashMap<Integer, EntityWrapper> getClonedEntities() {
		return this.clonedEntities;
	}
	
	public HashMap<Integer, LinkedList<PacketContainer>> getSavedPackets() {
		return this.savedPackets;
	}
	
	public LinkedList<PacketContainer> getCurrentArrayPtr() {
		return this.ptr;
	}
	
	/* Not the best way to go about doing this, but all I really need is the type information, coordinates, metadata and some other things.. */
	private void deepCloneEntities() {
		List<Entity> entities = this.world.getEntities();
		
		int entityCount[] = {0, 0, 0, 0, 0};
		
		for(Entity e : entities)
		{
			//entity is an experience orb
			if(e.getType() == EntityType.EXPERIENCE_ORB)
			{
				ExperienceOrb orb = (ExperienceOrb) e;
				this.clonedEntities.put(orb.getEntityId(), new OrbWrapper(orb.getEntityId(), orb.getLocation(), orb.getExperience()));
				entityCount[0]++;
			}
			//entity is a painting
			else if(e.getType() == EntityType.PAINTING)
			{
				Painting p = (Painting) e;
				this.clonedEntities.put(p.getEntityId(), new PaintingWrapper(p.getEntityId(), p.getLocation(), p.getArt().toString(), p.getFacing()));
				entityCount[1]++;
			}
			//entity is a player
			else if(e.getType() == EntityType.PLAYER)
			{
				Player p = (Player) e;
				this.clonedEntities.put(p.getEntityId(), new PlayerWrapper(p.getEntityId(), p.getLocation(), p.getName(), p.getUniqueId(), 
						p.getItemInHand(), p.getInventory().getArmorContents(), WrappedDataWatcher.getEntityWatcher(p).deepClone(), 
						p.getGameMode(), WrappedGameProfile.fromPlayer(p)));
				
				entityCount[2]++;
			}
			//entity is a misc object
			else if(ObjectWrapper.VALID_OBJECTS.keySet().contains(e.getType())) 
			{
				this.clonedEntities.put(e.getEntityId(), new ObjectWrapper(e.getEntityId(), e.getLocation(), e.getType(), 0)); //Currently no NMS support for object data, just setting to 0 for now
				entityCount[3]++;
			}
			//entity is a mob
			else 
			{
				this.clonedEntities.put(e.getEntityId(), new MobWrapper(e.getEntityId(), e.getLocation(), e.getType(), e.getVelocity(), 
						WrappedDataWatcher.getEntityWatcher(e).deepClone()));
				
				entityCount[4]++;
			}
		}
		
		Bukkit.broadcastMessage("Cloned " + this.clonedEntities.size() + " entities!");
		Bukkit.broadcastMessage("Entity counts: Exp orb, " + entityCount[0] + " Painting, " + entityCount[1] + " Player, " + entityCount[2] + " Object, " + entityCount[3] + " Mob, " + entityCount[4]);
	}
}
