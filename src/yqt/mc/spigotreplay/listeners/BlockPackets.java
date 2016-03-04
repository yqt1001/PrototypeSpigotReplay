package yqt.mc.spigotreplay.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import yqt.mc.spigotreplay.Replay;
import yqt.mc.spigotreplay.ReplayPlugin;

public class BlockPackets {

	/* Object to manage listening, saving and modifying block related packets */
	
	private ReplayPlugin main;
	private RecordingProtocolHandler handler;
	private ProtocolManager PM;
	private Replay active;
	private int p;
	
	public BlockPackets(ReplayPlugin main, RecordingProtocolHandler handler, Replay r, ProtocolManager PM) {
		this.main = main;
		this.handler = handler;
		this.PM = PM;
		this.active = r;
		this.p = this.handler.getSelectedPlayer();
		
		this.setUpListeners();
	}
	
	private void setUpListeners() {
		
		//Block breaking animation
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.BLOCK_BREAK_ANIMATION) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.incrementCount();
				handler.standardPacketHandler(e.getPacket(), e);
			}
		});
		
		//Block actions
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.BLOCK_ACTION) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.incrementCount();
				handler.standardPacketHandler(e.getPacket(), e);
			}
		});
		
		//Single block change
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.BLOCK_CHANGE) {
			@Override
			public void onPacketSending(PacketEvent e) {
				// packet.deepClone() throws errors for this packet, manually clone it
				
				handler.incrementCount();
				handler.incrementCount();
				if(e.getPlayer().getEntityId() != p)
					return;
				
				PacketContainer p = e.getPacket();
				PacketContainer newPacket = PM.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
				newPacket.getBlockData().write(0, p.getBlockData().read(0));
				newPacket.getBlockPositionModifier().write(0, p.getBlockPositionModifier().read(0));
				active.getCurrentArrayPtr().add(newPacket);
			}
		});
		
		//Multi block change
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
			@Override
			public void onPacketSending(PacketEvent e) {
				// packet.deepClone() throws errors for this packet, manually clone it
				
				handler.incrementCount();
				handler.incrementCount();
				if(e.getPlayer().getEntityId() != p)
					return;
				
				PacketContainer p = e.getPacket();
				PacketContainer newPacket = PM.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
				newPacket.getChunkCoordIntPairs().write(0, p.getChunkCoordIntPairs().read(0));
				newPacket.getMultiBlockChangeInfoArrays().write(0, p.getMultiBlockChangeInfoArrays().read(0));
				active.getCurrentArrayPtr().add(newPacket);
			}
		});
		
		//Explosion packet
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.EXPLOSION) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.incrementCount();
				handler.standardPacketHandler(e.getPacket(), e);
			}
		});
		
		//Update sign
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.UPDATE_SIGN) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.incrementCount();
				handler.standardPacketHandler(e.getPacket(), e);
			}
		});
	}
	
	public void updateSelectedPlayer(int p) {
		this.p = p;
	}
}
