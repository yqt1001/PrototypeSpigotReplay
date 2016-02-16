package yqt.mc.spigotreplay;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;

import yqt.mc.spigotreplay.ReplayPlugin.ReplayStatus;
import yqt.mc.spigotreplay.entity.EntityWrapper;
import yqt.mc.spigotreplay.entity.MobWrapper;
import yqt.mc.spigotreplay.entity.ObjectWrapper;
import yqt.mc.spigotreplay.entity.OrbWrapper;
import yqt.mc.spigotreplay.entity.PaintingWrapper;
import yqt.mc.spigotreplay.entity.PlayerWrapper;
import yqt.mc.spigotreplay.listeners.RecordingProtocolHandler;

public class RunnableHandler {

	private ReplayPlugin main;
	private Replay active = null;
	private boolean paused = false;
	private int replayTime = 0;
	
	private BukkitTask record;
	private BukkitTask replay;
	
	private ProtocolManager PM;
	private RecordingProtocolHandler rPH;
	
	private LinkedList<PacketType> packetExceptions = new LinkedList<PacketType>(Arrays.asList(
						PacketType.Play.Server.NAMED_ENTITY_SPAWN,
						PacketType.Play.Server.SPAWN_ENTITY_LIVING,
						PacketType.Play.Server.SPAWN_ENTITY,
						PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
						PacketType.Play.Server.SPAWN_ENTITY_PAINTING));
	
	//Instantiate the RunnableHandler singleton
	public RunnableHandler(ReplayPlugin main) {
		this.main = main;
		this.PM = this.main.getProtocol();
	}
	
	
	public Replay getActiveReplay() {
		return this.active;
	}
	
	public void startRecordingRunnable(final Replay r) {
		this.active = r;
		
		//start runnable
		this.record = new BukkitRunnable() {
			@Override
			public void run() {
				active.incrementLength();
			}
		}.runTaskTimer(main, 0L, 1L);
		
		//start listeners
		this.rPH = new RecordingProtocolHandler(this.main, this.active);
		
		Bukkit.broadcastMessage("Started recording replay " + this.active.getUID());
	}
	
	public void stopRecordingRunnable() {
		this.rPH.shutDownListeners();
		this.record.cancel();
		
		Bukkit.broadcastMessage("Ended recording of length " + this.active.getLength());
		
		/* TEMP */
		int savedCount = 0;
		for(LinkedList<PacketContainer> packetList : this.active.getSavedPackets().values())
			savedCount += packetList.size();
		
		Bukkit.broadcastMessage("Recorded " + this.rPH.getCount() + " packets, saved " + savedCount);
		this.rPH = null;
		
		this.active.setStatus(ReplayStatus.IDLE);
		this.active = null;
	}
	
	
	
	public void startReplayRunnable(final Replay r) {
		this.active = r;
		this.active.setStatus(ReplayStatus.REPLAY);
		
		Bukkit.broadcastMessage("Loading replay...");
		
		this.loadReplay();
		
		Bukkit.broadcastMessage("Loaded replay.");
		
		
		
		this.replay = new BukkitRunnable() {
			@Override
			public void run() {
				
				//if paused, don't do anything
				if(paused)
					return;
				
				//send saved packets
				LinkedList<PacketContainer> saved;
				if((saved = active.getSavedPackets().get(replayTime)) != null && saved.size() > 0)
				{
					for(PacketContainer packet : saved)
					{
						//check to see if dummy packet
						if(packetExceptions.contains(packet.getType()))
						{
							//likely a mob spawn exception
							loadEntities(replayTime);
							
							//may be more exceptions in the future, idk
						}
						//if no dummy packets, send them as planned
						else
						{
							for(Player player : Bukkit.getOnlinePlayers())
							{
								try {
									PM.sendServerPacket(player, packet);
								} catch (InvocationTargetException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
				
				if(replayTime % 20 == 0)
					Bukkit.broadcastMessage("" + (replayTime / 20));
				
				//check if replay is up
				if(replayTime == active.getLength())
					stopReplayRunnable();
				else
					replayTime++;
			}
		}.runTaskTimer(main, 0L, 1L);
	}
	
	public void stopReplayRunnable() {
		this.replay.cancel();
		this.stopReplay();
		this.replayTime = 0;
		
		Bukkit.broadcastMessage("Replay ended");
		
		this.active.setStatus(ReplayStatus.IDLE);
		this.active = null;
	}
	
	public void togglePaused() {
		this.paused ^= true;
		
		Bukkit.broadcastMessage((this.paused) ? "P" : "Unp" + "aused");
	}
	
	public RecordingProtocolHandler getProtocolHandler() {
		return this.rPH;
	}
	
	
	/* Method used to load the entities and blocks of a replay */
	private void loadReplay() {
		//make all players spectators
		for(Player p : Bukkit.getOnlinePlayers())
			p.setGameMode(GameMode.SPECTATOR);
		
		//spawn in cloned entities
		this.loadEntities(-1);
	}
	
	/* Method used to spawn in the entities at a specific tick */
	@SuppressWarnings("deprecation")
	private void loadEntities(int time) {
		//spawn in cloned entities
		LinkedList<PacketContainer> packets = new LinkedList<PacketContainer>();
		for(EntityWrapper e : this.active.getClonedEntities().values())
		{
					
			//spawn in players
			if(e.getSpawnTime().contains(time)) {
			if(e instanceof PlayerWrapper)
			{
				PlayerWrapper p = (PlayerWrapper) e;
				Location l = p.getSpawnLocation();
				
				/* PLAYER LIST ITEM PACKET */
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.PLAYER_INFO);
				p1.getPlayerInfoAction().write(0, PlayerInfoAction.ADD_PLAYER);
				p1.getPlayerInfoDataLists().write(0, new LinkedList<PlayerInfoData>(Arrays.asList(p.getProfile())));
				packets.add(p1);
						
				/* PLAYER SPAWN IN PACKET */
				PacketContainer p2 = PM.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
				p2.getIntegers().write(0, p.getNewEID());
				p2.getSpecificModifier(UUID.class).write(0, p.getNewUUID());
				p2.getIntegers().write(1, (int) Math.floor(l.getX() * 32.0D));
				p2.getIntegers().write(2, (int) Math.floor(l.getY() * 32.0D));
				p2.getIntegers().write(3, (int) Math.floor(l.getZ() * 32.0D));
				p2.getBytes().write(0, (byte) (l.getYaw() * 256.0F / 360.0F));
				p2.getBytes().write(1, (byte) (l.getPitch() * 256.0F / 360.0F));
				p2.getIntegers().write(4, (p.getItemInHandAtSpawn().getType() == Material.AIR ? 0 : p.getItemInHandAtSpawn().getTypeId())); //part of me thinks the block ID for air is 0...
				p2.getDataWatcherModifier().write(0, p.getMetadataAtSpawn());
				packets.add(p2);
				
				/* ENTITY EQUIPMENT PACKETS FOR ARMOUR AT SPAWN */
				for(int i = 0; i < 4; i++)
				{
					ItemStack armour = p.getArmourContentsAtSpawn()[i];
					
					//make sure armour is not blank
					if(armour.getType() != Material.AIR)
					{
						PacketContainer equip = PM.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
						equip.getIntegers().write(0, p.getNewEID());
						equip.getIntegers().write(1, i + 1);
						equip.getItemModifier().write(0, armour);
						packets.add(equip);
					}
				}
			}
			
			//spawn in experience orb
			else if(e instanceof OrbWrapper)
			{
				OrbWrapper orb = (OrbWrapper) e;
				Location l = orb.getSpawnLocation();
				
				/* ORB SPAWN PACKET */
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB);
				p1.getIntegers().write(0, orb.getNewEID());
				p1.getIntegers().write(1, (int) Math.floor(l.getX() * 32.0D));
				p1.getIntegers().write(2, (int) Math.floor(l.getY() * 32.0D));
				p1.getIntegers().write(3, (int) Math.floor(l.getZ() * 32.0D));
				p1.getIntegers().write(4, orb.getCount());
				packets.add(p1);
			}
			
			//spawn in painting
			else if(e instanceof PaintingWrapper)
			{
				PaintingWrapper p = (PaintingWrapper) e;
				Location l = p.getSpawnLocation();
				
				/* PAINTING SPAWN PACKET */
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY_PAINTING);
				p1.getStrings().write(0, p.getTitle());
				p1.getBlockPositionModifier().write(0, new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
				//eh protocollib isn't very convenient with painting directions so that's fine I'll just not support paintings for now
			}
			
			//spawn in object
			else if(e instanceof ObjectWrapper)
			{
				ObjectWrapper obj = (ObjectWrapper) e;
				Location l = obj.getSpawnLocation();
				
				/* OBJECT SPAWN PACKET */
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
				p1.getIntegers().write(0, obj.getNewEID());
				p1.getIntegers().write(9, ObjectWrapper.VALID_OBJECTS.get(obj.getType()));
				p1.getIntegers().write(1, (int) Math.floor(l.getX() * 32.0D));
				p1.getIntegers().write(2, (int) Math.floor(l.getY() * 32.0D));
				p1.getIntegers().write(3, (int) Math.floor(l.getZ() * 32.0D));
				p1.getIntegers().write(7, (int) (l.getYaw() * 256.0F / 360.0F));
				p1.getIntegers().write(8, (int) (l.getPitch() * 256.0F / 360.0F));
				p1.getIntegers().write(10, obj.getObjectData());
				packets.add(p1);
			}
			
			//spawn in mob
			else if(e instanceof MobWrapper)
			{
				MobWrapper m = (MobWrapper) e;
				Vector v = m.getSpawnVelocity();
				Location l = m.getSpawnLocation();
				
				/* MOB SPAWN PACKET */
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
				p1.getIntegers().write(0, m.getNewEID());
				p1.getIntegers().write(1, (int) m.getType().getTypeId());
				p1.getIntegers().write(2, (int) Math.floor(l.getX() * 32.0D));
				p1.getIntegers().write(3, (int) Math.floor(l.getY() * 32.0D));
				p1.getIntegers().write(4, (int) Math.floor(l.getZ() * 32.0D));
				p1.getBytes().write(0, (byte) (l.getYaw() * 256.0F / 360.0F));
				p1.getBytes().write(1, (byte) (l.getPitch() * 256.0F / 360.0F));
				p1.getBytes().write(2, (byte) (l.getPitch() * 256.0F / 360.0F));
				p1.getIntegers().write(5, (int) (v.getX() * 8000.0));
				p1.getIntegers().write(6, (int) (v.getY() * 8000.0));
				p1.getIntegers().write(7, (int) (v.getZ() * 8000.0));
				p1.getDataWatcherModifier().write(0, m.getMetadataAtSpawn());
				packets.add(p1);
			}
			
			//send packets to players if valid
			if(packets.size() > 0)
				for(Player player : Bukkit.getOnlinePlayers())
					for(PacketContainer packet : packets)
					{
						try {
							PM.sendServerPacket(player, packet);
						} catch (InvocationTargetException e1) {
							e1.printStackTrace();
						}
					}	
			}
		}
	}
	
	/* Method to handle returning everything back to normal after replay has run its course */
	private void stopReplay() {
		LinkedList<PacketContainer> packets = new LinkedList<PacketContainer>();
		
		//delete all client side entities
		int deleteEIDs[] = new int[this.active.getClonedEntities().values().size()];
		Iterator<EntityWrapper> it = this.active.getClonedEntities().values().iterator();
		for(int i = 0; it.hasNext(); i++)
		{
			EntityWrapper e = it.next();
			
			//if entity is a player, make sure to send remove from tab list packet as well
			if(e instanceof PlayerWrapper)
			{
				PlayerWrapper p = (PlayerWrapper) e;
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.PLAYER_INFO);
				p1.getPlayerInfoAction().write(0, PlayerInfoAction.REMOVE_PLAYER);
				p1.getPlayerInfoDataLists().write(0, new LinkedList<PlayerInfoData>(Arrays.asList(p.getProfile())));
				packets.add(p1);
			}
			
			deleteEIDs[i] = e.getNewEID();
		}
		
		PacketContainer p2 = PM.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		p2.getIntegerArrays().write(0, deleteEIDs); //this is one of those situations where Java 8 would be amazing, if my server was on it :'(
		packets.add(p2);
		
		//set everyone back to creative mode and send entity destroy packet
		for(Player p : Bukkit.getOnlinePlayers())
		{
			p.setGameMode(GameMode.CREATIVE);
			
			for(PacketContainer packet : packets)
			{
				try {
					PM.sendServerPacket(p, packet);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Method to message the command sender the packets sent by the replay in the specified time
	 * @param p
	 * @param amt
	 */
	public void messageLastSentPackets(CommandSender s, int amt) {
		//if more requested ticks than ticks replayed, don't overrun
		if(amt > this.replayTime)
			amt = this.replayTime - 1;
		
		s.sendMessage("Showing the packets sent in the last " + amt + " ticks.");
		
		//send messages
		for(int i = this.replayTime - amt; i < this.replayTime; i++) 
			//start with earliest tick and increase, showing the newest ticks first from players perspective
			for(PacketContainer p : this.active.getSavedPackets().get(i))
				s.sendMessage("Tick " + i + ": " + p.getType().name());
	}
}
