package net.amigocraft.TTT.listeners;

import static net.amigocraft.TTT.TTTPlayer.isPlayer;

import net.amigocraft.TTT.TTT;
import net.amigocraft.TTT.managers.LobbyManager;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

public class BlockListener implements Listener {

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e){
		if (isPlayer(e.getPlayer().getName()))
			e.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e){
		if (isPlayer(e.getPlayer().getName()))
			e.setCancelled(true);
	}

	@EventHandler
	public void onSignChange(SignChangeEvent e){
		if (e.getLine(0).equalsIgnoreCase("[TTT]")){
			if (e.getPlayer().hasPermission("ttt.lobby.create")){
				LobbyManager.manageSign(e.getBlock(), e.getLine(2), e.getLine(1), e.getPlayer());
			}
			else
				e.getPlayer().sendMessage(ChatColor.RED + TTT.local.getMessage("no-permission"));
		}
	}
}
