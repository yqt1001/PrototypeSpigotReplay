package yqt.mc.spigotreplay.listeners;

import java.util.Arrays;
import java.util.LinkedList;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;

import yqt.mc.spigotreplay.Replay;
import yqt.mc.spigotreplay.ReplayPlugin;
import yqt.mc.spigotreplay.ReplayPlugin.ReplayStatus;
import yqt.mc.spigotreplay.entity.PlayerWrapper;

public class BukkitListener implements Listener {

	private ReplayPlugin main;
	private ProtocolManager PM;
	
	public BukkitListener(ReplayPlugin main) {
		this.main = main;
		this.PM = this.main.getProtocol();
	}
	
	/* Force player list item & spawn player packets when a player respawns during replay recording 
	 * I honestly have no clue what packets are called during respawning .-. */
	
	@EventHandler
	public void onRespawn(final PlayerRespawnEvent e) {
		final Replay r = this.main.getRunnables().getActiveReplay();
		
		//replay must be recording
		if(r == null || r.getStatus() != ReplayStatus.RECORDING)
			return;
		
		//fail safe, protocol listeners must be instantiated
		RecordingProtocolHandler RPH = this.main.getRunnables().getProtocolHandler();
		if(RPH == null)
			return;
		
		//send remove player from tab list packet
		//get updated player info first
		final PlayerWrapper player = (PlayerWrapper) r.getClonedEntities().get(e.getPlayer().getEntityId());
		
		if(player == null)
			return;
		
		PacketContainer packet = this.PM.createPacket(PacketType.Play.Server.PLAYER_INFO);
		packet.getPlayerInfoAction().write(0, PlayerInfoAction.REMOVE_PLAYER);
		packet.getPlayerInfoDataLists().write(0, new LinkedList<PlayerInfoData>(Arrays.asList(player.getProfile())));
		r.getCurrentArrayPtr().add(packet);
		
		//wait a tick and then send dummy player spawn packet and update spawntime in player wrapper
		new BukkitRunnable() {
			@Override
			public void run() {
				PacketContainer p1 = PM.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
				r.getCurrentArrayPtr().add(p1);
				player.addSpawnTime(r.getLength());
				
				//send a teleport packet so that the player doesn't spawn in the location according to the playerwrapper
				PacketContainer packet = PM.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
				packet.getIntegers().write(0, player.getNewEID());
				packet.getIntegers().write(1, (int) Math.floor(e.getPlayer().getLocation().getX() * 32.0D));
				packet.getIntegers().write(2, (int) Math.floor(e.getPlayer().getLocation().getY() * 32.0D));
				packet.getIntegers().write(3, (int) Math.floor(e.getPlayer().getLocation().getZ() * 32.0D));
				packet.getBytes().write(0 , (byte) (e.getPlayer().getLocation().getYaw() * 256.0F / 360.0F));
				packet.getBytes().write(0, (byte) (e.getPlayer().getLocation().getPitch() * 256.0F / 360.0F));
				r.getCurrentArrayPtr().add(packet);
			}
		}.runTaskLater(this.main, 2L);
	}
	
	/*
	 * There is no way to distinguish between random teleport packets
	 * and ones that are caused by the server, so use this for that
	 */
	
	@EventHandler
	public void onTeleport(PlayerTeleportEvent e) {
		Replay r = this.main.getRunnables().getActiveReplay();
		
		//replay must be recording
		if(r == null || r.getStatus() != ReplayStatus.RECORDING)
			return;
				
		//fail safe, protocol listeners must be instantiated
		RecordingProtocolHandler RPH = this.main.getRunnables().getProtocolHandler();
		if(RPH == null)
			return;
		
		PlayerWrapper player = (PlayerWrapper) r.getClonedEntities().get(e.getPlayer().getEntityId());
		
		if(player == null)
			return;
		
		//create entity teleport packet for this event
		PacketContainer packet = PM.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
		packet.getIntegers().write(0, player.getNewEID());
		packet.getIntegers().write(1, (int) Math.floor(e.getTo().getX() * 32.0D));
		packet.getIntegers().write(2, (int) Math.floor(e.getTo().getY() * 32.0D));
		packet.getIntegers().write(3, (int) Math.floor(e.getTo().getZ() * 32.0D));
		packet.getBytes().write(0 , (byte) (e.getPlayer().getLocation().getYaw() * 256.0F / 360.0F));
		packet.getBytes().write(0, (byte) (e.getPlayer().getLocation().getPitch() * 256.0F / 360.0F));
		r.getCurrentArrayPtr().add(packet);
	}
}
