package yqt.mc.spigotreplay.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import yqt.mc.spigotreplay.Replay;
import yqt.mc.spigotreplay.ReplayPlugin;

public class RecordingProtocolHandler {

	private ReplayPlugin main;
	private Replay active;
	private ProtocolManager PM;
	private int count;
	private int selectedPlayer;
	
	private EntityPackets ep;
	
	public RecordingProtocolHandler(ReplayPlugin main, Replay active) {
		this.main = main;
		this.active = active;
		this.PM = this.main.getProtocol();
		this.count = 0;
		this.selectedPlayer = ((Player) Bukkit.getOnlinePlayers().toArray()[0]).getEntityId();
		
		this.setUpListeners();
	}
	
	/* Method to set up the protocol listeners using ProtocolLib */
	private void setUpListeners() {
		ep = new EntityPackets(this.main, this, this.active, this.PM);
	}
	
	/**
	 *  Method used to shut down the protocol listeners 
	 */
	public void shutDownListeners() {
		this.PM.removePacketListeners(this.main);
	}
	
	/**
	 *  This method just checks for duplicates and then stores the packet, simply cloning it 
	 */
	public void standardPacketHandler(PacketContainer p, PacketEvent e) {
		this.incrementCount();
		
		if(e.getPlayer().getEntityId() == this.selectedPlayer)
			this.active.getCurrentArrayPtr().add(p.deepClone());
	}
	
	/**
	 * Returns the randomly selected player who's packets we'll be using to ensure no duplicates
	 * @return
	 */
	public int getSelectedPlayer() {
		return this.selectedPlayer;
	}
	
	/**
	 * Selects a new randomly selected player to intercept packets and force updates all listeners
	 */
	public void getNewSelectedPlayer() {
		int newEID = 0;
		if(Bukkit.getOnlinePlayers().size() > 0)
		{
			for(Player p : Bukkit.getOnlinePlayers())
				if(p.getEntityId() != this.selectedPlayer)
				{
					newEID = p.getEntityId();
					break;
				}
		}
		
		this.setNewSelectedPlayer(newEID);
	}
	
	/**
	 * Forces a new selected player to this EID and then updates the listeners
	 * @param EID
	 */
	public void setNewSelectedPlayer(int EID) {
		this.selectedPlayer = EID;
		
		//force update the listeners
		this.ep.updateSelectedPlayer(this.selectedPlayer);
	}
	
	public int getCount() {
		return this.count;
	}
	
	public void incrementCount() {
		this.count++;
	}
}
