package yqt.mc.spigotreplay;

import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import yqt.mc.spigotreplay.listeners.BukkitListener;

public class ReplayPlugin extends JavaPlugin {
	
	private LinkedList<Replay> replays = new LinkedList<Replay>();
	
	private ProtocolManager PM;
	private RunnableHandler rH;
	
	@Override
	public void onEnable() {
		this.PM = ProtocolLibrary.getProtocolManager();
		this.rH = new RunnableHandler(this);
		
		Bukkit.getPluginManager().registerEvents(new BukkitListener(this), this);
	}
	
	@Override
	public void onDisable() {
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		//if the sender is a player, cast them to a player object
		Player p = null;
		if(sender instanceof Player)
			p = (Player) sender;

		//start recording new replay
		if(cmd.getName().equalsIgnoreCase("rec"))
		{
			Replay r = this.rH.getActiveReplay();
			
			if(r == null)
				this.createReplay(p == null ? Bukkit.getWorld("world") : p.getWorld());
			else if(r.getStatus() == ReplayStatus.RECORDING)
				this.rH.stopRecordingRunnable();
			else
				sender.sendMessage("§cInvalid states!");
				
			return true;
		}
		
		//replay specified or latest replay
		if(cmd.getName().equalsIgnoreCase("startreplay"))
		{
			
			//make sure there is at least one replay valid
			if(this.replays.size() < 1) 
			{
				sender.sendMessage("§cThere is no replays saved.");
				return true;
			}
			
			//make sure that there is no active replay 
			if(this.rH.getActiveReplay() != null)
			{
				sender.sendMessage("§cThere is a replay currently active!");
				return true;
			}
			
			
			//if specifying an index
			if(args.length == 1 && args[0] != null)
			{
				int i = 0;
				try {
					i = Integer.parseInt(args[0]);
					this.rH.startReplayRunnable(this.getReplay(i));
				} catch(Exception e) {
					//really just about any invalid input will throw something
					//I'm just lazy and substituting validation with a try-catch
					
					sender.sendMessage("§cError: " + e.getMessage());
				}
			}
			else
				this.rH.startReplayRunnable(this.getLatestReplay());
			
			
			return true;
		}
		
		//pause or resume replay
		if(cmd.getName().equalsIgnoreCase("p"))
		{
			
			//make sure there is an active replay
			Replay r = this.rH.getActiveReplay();
			if(r != null && r.getStatus() == ReplayStatus.REPLAY)
				this.rH.togglePaused();
			else
				sender.sendMessage("§cThere is no replay currently playing back.");
				
				
			return true;
		}
		
		//broadcast the sent packets from previous specified ticks
		if(cmd.getName().equalsIgnoreCase("lastticks"))
		{
			//make sure there is an active runnable
			Replay r = this.rH.getActiveReplay();
			if(r == null || r.getStatus() != ReplayStatus.REPLAY)
			{
				sender.sendMessage("§cInfalid states!");
				return true;
			}
			
			//if no specified amount, default to 5
			if(args.length < 1)
				this.rH.messageLastSentPackets(sender, 5);
			else
			{
				int i = Integer.parseInt(args[0]);
				if(i != 0)
					this.rH.messageLastSentPackets(sender, i);
				else
					this.rH.messageLastSentPackets(sender, 5);
			}
			
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("getreplays"))
		{
			if(this.replays.size() < 1)
				sender.sendMessage("§eNo replays!");
			else
			{
				Iterator<Replay> it = this.replays.iterator();
				for(int i = 0; it.hasNext(); i++)
					sender.sendMessage("Replay " + i + ": " + it.next().getLength() + " ticks long");
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Creates a replay instance
	 */
	public void createReplay(World w) {
		replays.add(new Replay(this, w, this.replays.size()));
	}
	
	/**
	 * Returns the newest replay created
	 * @return
	 */
	public Replay getLatestReplay() {
		return this.replays.getLast();
	}
	
	/**
	 * Returns the replay at the specified index
	 * @param i
	 * @return
	 */
	public Replay getReplay(int i) {
		return this.replays.get(i);
	}
	
	/**
	 * Returns the RunnableHandler singleton to manage replay runnables
	 * @return
	 */
	public RunnableHandler getRunnables() {
		return this.rH;
	}
	
	/**
	 * Returns the protocol manager instance
	 * @return
	 */
	public ProtocolManager getProtocol() {
		return this.PM;
	}
	
	public enum ReplayStatus {
		RECORDING, REPLAY, IDLE;
	}
}
