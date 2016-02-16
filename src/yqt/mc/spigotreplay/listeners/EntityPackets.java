package yqt.mc.spigotreplay.listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import yqt.mc.spigotreplay.Replay;
import yqt.mc.spigotreplay.ReplayPlugin;
import yqt.mc.spigotreplay.entity.EntityWrapper;
import yqt.mc.spigotreplay.entity.MobWrapper;
import yqt.mc.spigotreplay.entity.ObjectWrapper;
import yqt.mc.spigotreplay.entity.OrbWrapper;
import yqt.mc.spigotreplay.entity.PaintingWrapper;
import yqt.mc.spigotreplay.entity.PlayerWrapper;

public class EntityPackets {

	/* Singleton to manage the listening to entity related packets */
	
	/*
	 * This file is testament to how hard fully understanding the Minecraft protocol is.
	 * I thought it would be a reasonable ~200 line file and take an hour or two to write.
	 * 
	 * Oh boy was I wrong!!! I don't understand why Mojang feels it's so necessary to have
	 * differing standards for pretty much everything. I truly thought it would be a matter
	 * of changing the entity IDs on the packets and calling it a day.. How I wish to go back 
	 * to that day and slap me in my face and say "you need at least 10 more classes to accomplish this!".
	 * 
	 * First it was the mob spawning. Not too bad really, just requires a heavy amount of code to accomplish.
	 * Then was the movement packets. Harder and it's nearly impossible to check for duplicates and even now I'm
	 * not 100% sure I managed it....Then I decided to use serverbound packets to save player movement. BAD IDEA!
	 * 
	 * "Oh look players stopped moving again!" -> a while later...
	 * "Wow that was a stupid mistake, I forgot to multiply the delta of the distance
	 * by 32. (line 503) The difference is too small and when casting to a byte, it would floor it to 0 making 
	 * entities completely stationary. If only I could fix head motion..." -> a while later...
	 * "Turns out that wiki.vg is just making this confusing, you only need to do an additional
	 * input % 360 to make the client packets readable, on top of all the other operations. (line 447) I wish 
	 * I could remember how I came so quickly to that, almost like MOD 360 is the answer to life.
	 * Welp I broke player movement again somehow..."
	 * 
	 * So many small things to think about, I could write a novel with all the corners I cut that would
	 * need to be fixed if rolling this out to production (god forbid). I only have a week off from school though!
	 * 
	 * If you wish to recreate the replay idea, I have no idea if there is a better way. Some things that work amazingly
	 * in theory cause entities to randomly disappear. It's really just eternal trial and error.
	 * 
	 * LUCKILY it's all client side so nothing absolutely terrible - you can never play on your server again - happens!
	 */
	
	private ReplayPlugin main;
	private RecordingProtocolHandler handler;
	private Replay active;
	private ProtocolManager PM;
	private int p;
	
	public EntityPackets(ReplayPlugin main, RecordingProtocolHandler handler, Replay active, ProtocolManager PM) {
		this.main = main;
		this.handler = handler;
		this.active = active;
		this.PM = PM;
		this.p = this.handler.getSelectedPlayer();
		
		this.setUpListeners();
	}
	
	private void setUpListeners() {
		//Entity equipment
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_EQUIPMENT) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Player entering bed
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.BED) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Player animation
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ANIMATION) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Collect item
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.COLLECT) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.incrementCount();
				
				if(e.getPlayer().getEntityId() != p)
					return;
				
				//handle this packet differently, requires two updated EIDs
				PacketContainer p = e.getPacket();
				int newEID = active.getClonedEntities().get(p.getIntegers().read(0)).getNewEID();
				int newOEID = active.getClonedEntities().get(p.getIntegers().read(1)).getNewEID();
				
				PacketContainer packet = PM.createPacket(PacketType.Play.Server.COLLECT);
				packet.getIntegers().write(0, newEID);
				packet.getIntegers().write(1, newOEID);
				active.getCurrentArrayPtr().add(packet);
			}
		});
		
		//Entity velocity change
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_VELOCITY) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Entity destroy
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_DESTROY) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.incrementCount();
				
				//this is the first packet sent out if a player disconnects, if the selected player disconnects...
				PacketContainer packet = e.getPacket();
				final int oldEIDs[] = packet.getIntegerArrays().read(0);
				
				for(int i = 0; i < oldEIDs.length; i++)
					if(oldEIDs[i] == p)
						handler.setNewSelectedPlayer(e.getPlayer().getEntityId());
				
				if(e.getPlayer().getEntityId() != p)
					return;
				
				/* 
				 * This packet has been throwing so many weird false positives that I'm
				 * reluctant to support it, but it's necessary for when players quit.
				 */
				
				new BukkitRunnable() {
					@Override
					public void run() {
						int newEIDs[] = new int[oldEIDs.length];
						for(int i = 0; i < oldEIDs.length; i++)
							for(Entity ent : active.getWorld().getEntities())
								if(ent.getEntityId() == oldEIDs[i])
									if(!ent.isValid())
										newEIDs[i] = active.getClonedEntities().get(oldEIDs[i]).getNewEID();
						
						if(newEIDs.length > 0)
						{
							PacketContainer newPacket = PM.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
							newPacket.getIntegerArrays().write(0, newEIDs);
							active.getCurrentArrayPtr().add(newPacket);
						}
					}
				}.runTaskLater(main, 1L);
			}
		});
		
		//Relative entity move
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.REL_ENTITY_MOVE) {
			@Override
			public void onPacketSending(PacketEvent e) {
				//make sure this is not a player
				if(!(active.getClonedEntities().get(e.getPacket().getIntegers().read(0)) instanceof PlayerWrapper))
					standardEntityPacketHandler(e.getPacket(), e);
				else
					handler.incrementCount();
			}
		});
		
		//Entity look
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_LOOK) {
			@Override
			public void onPacketSending(PacketEvent e) {
				//make sure this is not a player
				if(!(active.getClonedEntities().get(e.getPacket().getIntegers().read(0)) instanceof PlayerWrapper))
					standardEntityPacketHandler(e.getPacket(), e);
				else
					handler.incrementCount();
			}
		});
		
		//Entity look and relative move
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_MOVE_LOOK) {
			@Override
			public void onPacketSending(PacketEvent e) {
				//make sure this is not a player
				if(!(active.getClonedEntities().get(e.getPacket().getIntegers().read(0)) instanceof PlayerWrapper))
					standardEntityPacketHandler(e.getPacket(), e);
				else
					handler.incrementCount();
			}
		});
		
		//Entity teleport
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_TELEPORT) {
			@Override
			public void onPacketSending(PacketEvent e) {
				//make sure this is not a player
				if(!(active.getClonedEntities().get(e.getPacket().getIntegers().read(0)) instanceof PlayerWrapper))
					standardEntityPacketHandler(e.getPacket(), e);
				else
					handler.incrementCount();
			}
		});
		
		//Entity head move
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_HEAD_ROTATION) {
			@Override
			public void onPacketSending(PacketEvent e) {
				if(!(active.getClonedEntities().get(e.getPacket().getIntegers().read(0)) instanceof PlayerWrapper))
					standardEntityPacketHandler(e.getPacket(), e);
				else
					handler.incrementCount();
			}
		});
		
		//Entity status
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_STATUS) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Entity passenger
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ATTACH_ENTITY) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.incrementCount();
				final PacketContainer packet = e.getPacket();
				
				//if selected entity is sending this packet, set this packet sender as the new selected player
				if(packet.getIntegers().read(0) == p)
					handler.setNewSelectedPlayer(e.getPlayer().getEntityId());
				
				if(e.getPlayer().getEntityId() != p)
					return;
				
				int newEID = active.getClonedEntities().get(packet.getIntegers().read(1)).getNewEID();
				int newVEID = 0;
				if(packet.getIntegers().read(2) > -1)
					newVEID = active.getClonedEntities().get(packet.getIntegers().read(2)).getNewEID();
				else
					newVEID = -1;
					
				PacketContainer nPacket = PM.createPacket(PacketType.Play.Server.ATTACH_ENTITY);
				nPacket.getIntegers().write(0, packet.getIntegers().read(0));
				nPacket.getIntegers().write(1, newEID);
				nPacket.getIntegers().write(2, newVEID);
				active.getCurrentArrayPtr().add(nPacket);
				
				if(newVEID != -1)
					return;
				
				//send entity teleport packet to fix displacement on dismount
				new BukkitRunnable() {
					@Override
					public void run() {
						Entity e = null;
						for(Entity ent : active.getWorld().getEntities())
							if(ent.getEntityId() == packet.getIntegers().read(1))
								e = ent;
						
						if(e == null)
							return;
						
						EntityWrapper ew = active.getClonedEntities().get(e.getEntityId());
						
						PacketContainer packet = PM.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
						packet.getIntegers().write(0, ew.getNewEID());
						packet.getIntegers().write(1, (int) Math.floor(e.getLocation().getX() * 32.0D));
						packet.getIntegers().write(2, (int) Math.floor(e.getLocation().getY() * 32.0D));
						packet.getIntegers().write(3, (int) Math.floor(e.getLocation().getZ() * 32.0D));
						packet.getBytes().write(0 , (byte) (e.getLocation().getYaw() * 256.0F / 360.0F));
						packet.getBytes().write(0, (byte) (e.getLocation().getPitch() * 256.0F / 360.0F));
						active.getCurrentArrayPtr().add(packet);
					}
				}.runTaskLater(main, 3L);
			}
		});
		
		//Entity metadata
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_METADATA) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Entity effect
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY_EFFECT) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Remove entity effect
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Entity properties
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.UPDATE_ATTRIBUTES) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Entity NBT
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.UPDATE_ENTITY_NBT) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Entity
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.ENTITY) {
			@Override
			public void onPacketSending(PacketEvent e) {
				standardEntityPacketHandler(e.getPacket(), e);
			}
		});
		
		//Player gamemode, display name updates and remove player
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.PLAYER_INFO) {
			@Override
			public void onPacketSending(PacketEvent e) {
				//make sure if is one of the wanted states
				PacketContainer packet = e.getPacket();
				if(packet.getPlayerInfoAction().read(0) == PlayerInfoAction.ADD_PLAYER)
					return;
				
				//I'm fairly certain that the selected player receives this packet as well in cases of updating display name & ping, this packet is sent after entity destroy for removal from tab list
				if(e.getPlayer().getEntityId() != p)
					return;
				
				handler.incrementCount();
				
				//update all player info data values to fake players
				ArrayList<PlayerInfoData> pidList = (ArrayList<PlayerInfoData>) packet.getPlayerInfoDataLists().read(0); //apparently, this array needs to be an ArrayList
				LinkedList<PlayerInfoData> updatedPid = new LinkedList<PlayerInfoData>();
				
				for(PlayerInfoData pid : pidList)
				{
					//unfortunately nowhere in PlayerInfoData are the entity IDs stored, so manually get them using UUIDs
					UUID oldUUID = pid.getProfile().getUUID();
					
					PlayerWrapper player = null;
					for(EntityWrapper ew : active.getClonedEntities().values())
						if(ew instanceof PlayerWrapper)
							if(((PlayerWrapper) ew).getOldUUID().equals(oldUUID))
								player = (PlayerWrapper) ew;
					
					if(player != null)
						updatedPid.add(player.getProfile());
				}
				
				PacketContainer newPacket = PM.createPacket(PacketType.Play.Server.PLAYER_INFO);
				newPacket.getPlayerInfoAction().write(0, packet.getPlayerInfoAction().read(0));
				newPacket.getPlayerInfoDataLists().write(0, updatedPid);
				active.getCurrentArrayPtr().add(newPacket);
			}
		});
		
		/* Move and animation packets from player */
		
		//Player move
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Client.POSITION) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				handler.incrementCount();
				PlayerWrapper pw = (PlayerWrapper) active.getClonedEntities().get(e.getPlayer().getEntityId());
				
				if(pw != null)
				{
					PacketContainer p = e.getPacket();
					
					Location oldLoc = pw.getCurrentLoc();
					Location newLoc = new Location(oldLoc.getWorld(), p.getDoubles().read(0), p.getDoubles().read(1), p.getDoubles().read(2), oldLoc.getYaw(), oldLoc.getPitch());
					
					if(newLoc.distance(oldLoc) > 4)
					{
						//create entity teleport packet
						PacketContainer packet = PM.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
						packet.getIntegers().write(0, pw.getNewEID());
						packet.getIntegers().write(1, (int) Math.floor(newLoc.getX() * 32.0D));
						packet.getIntegers().write(2, (int) Math.floor(newLoc.getY() * 32.0D));
						packet.getIntegers().write(3, (int) Math.floor(newLoc.getZ() * 32.0D));
						packet.getBytes().write(0 , (byte) (newLoc.getYaw() * 256.0F / 360.0F));
						packet.getBytes().write(0, (byte) (newLoc.getPitch() * 256.0F / 360.0F));
						active.getCurrentArrayPtr().add(packet);
					}
					else
					{
						//create entity relative move packet
						PacketContainer packet = PM.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE);
						packet.getIntegers().write(0, pw.getNewEID());
						packet.getBytes().write(0, (byte) ((newLoc.getX() - oldLoc.getX()) * 32.0D));
						packet.getBytes().write(1, (byte) ((newLoc.getY() - oldLoc.getY()) * 32.0D));
						packet.getBytes().write(2, (byte) ((newLoc.getZ() - oldLoc.getZ()) * 32.0D));
						packet.getBooleans().write(0, p.getBooleans().read(0));
						active.getCurrentArrayPtr().add(packet);
					}
					
					pw.setCurrentLoc(newLoc);
				}
			}
		});
		
		//Player look
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Client.LOOK) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				handler.incrementCount();
				PlayerWrapper pw = (PlayerWrapper) active.getClonedEntities().get(e.getPlayer().getEntityId());
				
				if(pw == null)
					return;
				
				PacketContainer p = e.getPacket();

				//entity look packet doesn't really work, so send the relative movement packet as well but keep rel movement to 0
				PacketContainer packet = PM.createPacket(PacketType.Play.Server.ENTITY_MOVE_LOOK);
				packet.getIntegers().write(0, pw.getNewEID());
				packet.getBytes().write(0, (byte) 0);
				packet.getBytes().write(1, (byte) 0);
				packet.getBytes().write(2, (byte) 0);
				packet.getBytes().write(3, (byte) ((p.getFloat().read(0) % 360) * 256.0F / 360.0F));
				packet.getBytes().write(4, (byte) ((p.getFloat().read(1) % 360) * 256.0F / 360.0F));
				packet.getBooleans().write(0, p.getBooleans().read(0));
				active.getCurrentArrayPtr().add(packet);
				
				//entity head rotation packet
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
				p1.getIntegers().write(0, pw.getNewEID());
				p1.getBytes().write(0, (byte) ((p.getFloat().read(0) % 360) * 256.0F / 360.0F));
				active.getCurrentArrayPtr().add(p1);
			}
		});
		
		//Player rel look & move
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Client.POSITION_LOOK) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				handler.incrementCount();
				PlayerWrapper pw = (PlayerWrapper) active.getClonedEntities().get(e.getPlayer().getEntityId());
				
				if(pw == null)
					return;
				
				PacketContainer p = e.getPacket();
				
				Location oldLoc = pw.getCurrentLoc();
				Location newLoc = new Location(oldLoc.getWorld(), p.getDoubles().read(0), p.getDoubles().read(1), p.getDoubles().read(2), p.getFloat().read(0) % 360, p.getFloat().read(1) % 360);
				
				if(newLoc.distance(oldLoc) > 4)
				{
					//create entity teleport packet
					PacketContainer packet = PM.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
					packet.getIntegers().write(0, pw.getNewEID());
					packet.getIntegers().write(1, (int) Math.floor(newLoc.getX() * 32.0D));
					packet.getIntegers().write(2, (int) Math.floor(newLoc.getY() * 32.0D));
					packet.getIntegers().write(3, (int) Math.floor(newLoc.getZ() * 32.0D));
					packet.getBytes().write(0 , (byte) (newLoc.getYaw() * 256.0F / 360.0F));
					packet.getBytes().write(0, (byte) (newLoc.getPitch() * 256.0F / 360.0F));
					active.getCurrentArrayPtr().add(packet);
				}
				else
				{
					//client has been known to send many near-duplicates of this packet per tick, check to make sure
					if(active.getCurrentArrayPtr().size() > 0)
					{
						Iterator<PacketContainer> it = active.getCurrentArrayPtr().iterator();
						PacketContainer next = null;
						while(it.hasNext() && (next = it.next()) != null)
							if(next.getType() == PacketType.Play.Server.ENTITY_MOVE_LOOK && next.getIntegers().read(0) == pw.getNewEID())
								return;
					}
					
					
					//create entity rel look & move packet
					PacketContainer packet = PM.createPacket(PacketType.Play.Server.ENTITY_MOVE_LOOK);
					packet.getIntegers().write(0, pw.getNewEID());
					packet.getBytes().write(0, (byte) ((newLoc.getX() - oldLoc.getX()) * 32.0D));
					packet.getBytes().write(1, (byte) ((newLoc.getY() - oldLoc.getY()) * 32.0D));
					packet.getBytes().write(2, (byte) ((newLoc.getZ() - oldLoc.getZ()) * 32.0D));
					packet.getBytes().write(3, (byte) (newLoc.getYaw() * 256.0F / 360.0F));
					packet.getBytes().write(4, (byte) (newLoc.getPitch() * 256.0F / 360.0F));
					packet.getBooleans().write(0, p.getBooleans().read(0));
					active.getCurrentArrayPtr().add(packet);
				}
				
				pw.setCurrentLoc(newLoc);
				
				//entity head rotation packet
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
				p1.getIntegers().write(0, pw.getNewEID());
				p1.getBytes().write(0, (byte) (newLoc.getYaw() * 256.0F / 360.0F));
				active.getCurrentArrayPtr().add(p1);
			}
		});
		
		
		
		/* MOB SPAWNING */
		
		//Player spawn
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
			@Override
			public void onPacketSending(PacketEvent e) {
				//no point directly saving this packet as we need more data, so get player object and generically create wrapper
				PacketContainer p = e.getPacket();
				int oldEID = p.getIntegers().read(0);
				handler.incrementCount();
				
				if(active.getClonedEntities().get(oldEID) == null)
				{
					Player player = null;
					for(Player players : Bukkit.getOnlinePlayers())
						if(players.getEntityId() == oldEID)
							player = players;
						
						
					if(player != null && player.getWorld().equals(active.getWorld()))
					{
						//add dummy to runtime packet list to trigger that an entity should be spawned
						active.getCurrentArrayPtr().add(PM.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN));
								
						//add player to cloned entity list
						active.getClonedEntities().put(oldEID, new PlayerWrapper(oldEID, player.getLocation(), 
								active.getLength(), player.getName(), player.getUniqueId(), player.getItemInHand(), 
								player.getInventory().getArmorContents(), WrappedDataWatcher.getEntityWatcher(player), 
								player.getGameMode(), WrappedGameProfile.fromPlayer(player)));
					}
				}
			}
		});
		
		//Mob spawn
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
			@Override
			public void onPacketSending(PacketEvent e) {
				//we could just directly save this packet, but I like the way it's done with players
				PacketContainer p = e.getPacket();
				int oldEID = p.getIntegers().read(0);
				handler.incrementCount();

				if(active.getClonedEntities().get(oldEID) == null)
				{
					Entity entity = null;
					for(Entity entities : active.getWorld().getEntities())
						if(entities.getEntityId() == oldEID)
							entity = entities;
						
						
					if(entity != null)
					{
						//add dummy to runtime packet list to trigger that an entity should be spawned
						active.getCurrentArrayPtr().add(PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY_LIVING));
								
						//add mob to cloned entity list
						active.getClonedEntities().put(oldEID, new MobWrapper(oldEID, entity.getLocation(), active.getLength(), entity.getType(), entity.getVelocity(), WrappedDataWatcher.getEntityWatcher(entity)));
					}
				}
			}
		});
		
		//Object spawn
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.SPAWN_ENTITY) {
			@Override
			public void onPacketSending(PacketEvent e) {
				PacketContainer p = e.getPacket();
				int oldEID = p.getIntegers().read(0);
				int objectData = p.getIntegers().read(10);
				handler.incrementCount();
				
				if(active.getClonedEntities().get(oldEID) == null)
				{
					Entity entity = null;
					for(Entity entities : active.getWorld().getEntities())
						if(entities.getEntityId() == oldEID)
							entity = entities;
						
						
					if(entity != null)
					{
						//add dummy to runtime packet list to trigger that an entity should be spawned
						active.getCurrentArrayPtr().add(PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY));
						
						//add object to cloned entity list
						active.getClonedEntities().put(oldEID, new ObjectWrapper(oldEID, entity.getLocation(), active.getLength(), entity.getType(), objectData));
					}
				}
			}
		});
		
		//Spawn experience orb
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB) {
			@Override
			public void onPacketSending(PacketEvent e) {
				PacketContainer p = e.getPacket();
				int oldEID = p.getIntegers().read(0);
				int count = p.getIntegers().read(4);
				handler.incrementCount();
				
				if(active.getClonedEntities().get(oldEID) == null)
				{
					Entity entity = null;
					for(Entity entities : active.getWorld().getEntities())
						if(entities.getEntityId() == oldEID)
							entity = entities;
						
						
					if(entity != null)
					{
						//add dummy to runtime packet list to trigger that an entity should be spawned
						active.getCurrentArrayPtr().add(PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB));
							
						//add orb to cloned entity list
						active.getClonedEntities().put(oldEID, new OrbWrapper(oldEID, entity.getLocation(), active.getLength(), count));
					}
				}
			}
		});
		
		//Spawn painting
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.SPAWN_ENTITY_PAINTING) {
			@Override
			public void onPacketSending(PacketEvent e) {
				PacketContainer p = e.getPacket();
				int oldEID = p.getIntegers().read(0);
				String art = p.getStrings().read(0);
				handler.incrementCount();
				
				if(active.getClonedEntities().get(oldEID) == null)
				{
					Entity entity = null;
					for(Entity entities : active.getWorld().getEntities())
						if(entities.getEntityId() == oldEID)
							entity = entities;
					
						
					if(entity != null)
					{
						//add dummy to runtime packet list to trigger that an entity should be spawned
						active.getCurrentArrayPtr().add(PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY_PAINTING));
								
						//add painting to cloned entity list
						Painting painting = (Painting) entity;
						active.getClonedEntities().put(oldEID, new PaintingWrapper(oldEID, painting.getLocation(), art, painting.getFacing()));
					}
				}
			}
		});
	}
	
	/* Entity equivalent to standard packet handler, which converts the old EID to new */
	private void standardEntityPacketHandler(PacketContainer p, PacketEvent e) {
		this.handler.incrementCount();
		
		//if selected entity is sending this packet, set this packet sender as the new selected player
		if(p.getIntegers().read(0) == this.p)
			this.handler.setNewSelectedPlayer(e.getPlayer().getEntityId());
		
		//if the packet is going to the selected player, add it to the list
		if(e.getPlayer().getEntityId() == this.p)
		{
			int newEID = this.active.getClonedEntities().get(p.getIntegers().read(0)).getNewEID();
			PacketContainer newPacket = p.deepClone();
			newPacket.getIntegers().write(0, newEID);
			this.active.getCurrentArrayPtr().add(newPacket);
		}
	}
	
	public void updateSelectedPlayer(int p) {
		this.p = p;
	}
}
