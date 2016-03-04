package yqt.mc.spigotreplay.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import yqt.mc.spigotreplay.ReplayPlugin;

public class MiscPackets {
	
	/* Manage listening, saving and modifying everything else */
	
	private ReplayPlugin main;
	private RecordingProtocolHandler handler;
	private ProtocolManager PM;
	
	public MiscPackets(ReplayPlugin main, RecordingProtocolHandler handler, ProtocolManager PM) {
		this.main = main;
		this.handler = handler;
		this.PM = PM;
		
		this.setUpListeners();
	}
	
	private void setUpListeners() {
		
		//Particles
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.WORLD_PARTICLES) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.standardPacketHandler(e.getPacket(), e);
			}
		});
		
		//Effect
		this.PM.addPacketListener(new PacketAdapter(main, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
			@Override
			public void onPacketSending(PacketEvent e) {
				handler.standardPacketHandler(e.getPacket(), e);
			}
		});
	}
}
