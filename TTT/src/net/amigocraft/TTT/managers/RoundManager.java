package net.amigocraft.TTT.managers;

import static net.amigocraft.TTT.TTTPlayer.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.amigocraft.TTT.Body;
import net.amigocraft.TTT.Role;
import net.amigocraft.TTT.Round;
import net.amigocraft.TTT.Stage;
import net.amigocraft.TTT.TTT;
import net.amigocraft.TTT.TTTPlayer;
import net.amigocraft.TTT.utils.NumUtils;
import net.amigocraft.TTT.utils.WorldUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class RoundManager {

	private static TTT plugin = TTT.plugin;

	private static HashMap<String, Integer> tasks = new HashMap<String, Integer>();

	private static List<String> checkPlayers = new ArrayList<String>();

	public void gameTimer(final String worldName){

		for (TTTPlayer t : players)
			if (t.getWorld().equals(worldName))
				if (!KarmaManager.playerKarma.containsKey(t.getName()) &&
						TTT.plugin.getConfig().getBoolean("karma-persistence"))
					KarmaManager.loadKarma(t.getName());

		tasks.put(worldName, plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable(){

			public void run(){

				World w = Bukkit.getWorld(worldName);
				if (w != null)
					w.setTime(6000L);

				// verify that all players are still online
				List<TTTPlayer> offlinePlayers = new ArrayList<TTTPlayer>();
				for (TTTPlayer tp : players){
					if (tp.getWorld().equals(worldName)){
						if (Round.getRound(worldName) != null){
							Player p = plugin.getServer().getPlayer(tp.getName());
							if (p != null){
								if (!plugin.getServer().getWorld("TTT_" + worldName).getPlayers().contains(p)){
									if (checkPlayers.contains(tp.getName())){
										if (plugin.getConfig().getBoolean("verbose-logging"))
											TTT.log.info(tp.getName() +
													" was missing from TTT world for 2 ticks, removing...");
										checkPlayers.remove(tp.getName());
										offlinePlayers.add(tp);
										Bukkit.broadcastMessage("[TTT] " + tp.getName() + " " +
												TTT.local.getMessage("left-map") + " \"" + worldName + "\"");
									}
									else
										checkPlayers.add(tp.getName());
								}
								else if (tp.getRole() == Role.DETECTIVE){ // manage DNA Scanners
									Player tracker = plugin.getServer().getPlayer(tp.getName());
									if (tp.getTracking() != null){
										Player killer = plugin.getServer().getPlayer(tp.getTracking());
										if (killer != null && isPlayer(tp.getTracking()))
											tracker.setCompassTarget(killer.getLocation());
										else {
											tracker.sendMessage(ChatColor.DARK_PURPLE +
													"The player you're tracking has left the round!");
											tp.setTracking(null);
											tracker.setCompassTarget(
													Bukkit.getWorlds().get(1).getSpawnLocation());
										}
									}
									else {
										Random r = new Random();
										tracker.setCompassTarget(new Location(tracker.getWorld(),
												tracker.getLocation().getX() + r.nextInt(10) - 5,
												tracker.getLocation().getY(),
												tracker.getLocation().getZ() + r.nextInt(10) - 5));
									}
								}
							}
						}
					}
				}
				for (TTTPlayer tp : offlinePlayers)
					tp.destroy();
				LobbyManager.updateSigns(worldName);

				// manage scoreboards
				ScoreManager.sbManagers.get(worldName).manage();

				// check if game is over
				boolean iLeft = false;
				boolean tLeft = false;
				for (TTTPlayer tp : players){
					if (!tLeft || !iLeft){
						if (tp.getWorld().equals(worldName) && !tp.isDead()){
							if (!iLeft)
								if (!tp.isTraitor())
									iLeft = true;
							if (!tLeft)
								if (tp.isTraitor())
									tLeft = true;
						}
					}
					else
						break;
				}
				if (!(tLeft && iLeft)){
					resetRound(worldName, iLeft);
				}
				else {
					Round r = Round.getRound(worldName);
					int rTime = r.getTime();
					if (rTime % 60 == 0 && rTime >= 60){
						for (Player p : plugin.getServer().getWorld("TTT_" + worldName)
								.getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(rTime / 60) +
									" " + TTT.local.getMessage("minutes") + " " +
									TTT.local.getMessage("left"));
						}
					}
					else if (rTime % 10 == 0 && rTime > 10 && rTime < 60){
						for (Player p : plugin.getServer().getWorld("TTT_" + worldName)
								.getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(rTime) + " " +
									TTT.local.getMessage("seconds") + " " +
									TTT.local.getMessage("left"));
						}
					}
					else if (rTime < 10 && rTime > 0){
						for (Player p : plugin.getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(rTime) + " " +
									TTT.local.getMessage("seconds") + " " +
									TTT.local.getMessage("left"));
						}
					}
					else if (rTime <= 0){
						resetRound(worldName, true);
						return;
					}
					if (rTime > 0)
						r.tickDown();
				}
				// hide dead players
				for (TTTPlayer p : players){
					if (p.isDead()){
						if (plugin.getServer().getPlayer(p.getName()) != null){
							if (plugin.getServer().getWorld("TTT_" + worldName) != null){
								if (plugin.getServer().getWorld("TTT_" + worldName).getPlayers()
										.contains(
										plugin.getServer().getPlayer(p.getName()))){
									plugin.getServer().getPlayer(p.getName()).setAllowFlight(true);
									for (TTTPlayer other : players){
										if (other.getWorld().equals(worldName) &&
												plugin.getServer().getPlayer(
												other.getName()) != null)
											plugin.getServer().getPlayer(other.getName())
											.hidePlayer(
													plugin.getServer().getPlayer(p.getName()));
									}
								}
							}
						}
					}
				}
			}
		}, 0L, 20L).getTaskId());
	}

	@SuppressWarnings("deprecation")
	public static void resetPlayer(Player p){
		if (isPlayer(p.getName())){
			TTTPlayer tp = getTTTPlayer(p.getName());
			String worldName = tp.getWorld();
			if (tp != null){
				if (tp.isDead()){
					p.setAllowFlight(false);
					for (Player pl : TTT.plugin.getServer().getOnlinePlayers()){
						pl.showPlayer(p);
					}
				}
				KarmaManager.saveKarma(tp);
				tp.setDisplayKarma(tp.getKarma());
				tp.destroy();
				p.getInventory().clear();
				File invF = new File(TTT.plugin.getDataFolder() + File.separator + "inventories"
				+ File.separator +
						p.getName() + ".inv");
				if (invF.exists()){
					try {
						YamlConfiguration invY = new YamlConfiguration();
						invY.load(invF);
						ItemStack[] invI = new ItemStack[p.getInventory().getSize()];
						for (String k : invY.getKeys(false)){
							if (NumUtils.isInt(k))
								invI[Integer.parseInt(k)] = invY.getItemStack(k);
							else if (k.equalsIgnoreCase("h"))
								p.getInventory().setHelmet(invY.getItemStack(k));
							else if (k.equalsIgnoreCase("c"))
								p.getInventory().setChestplate(invY.getItemStack(k));
							else if (k.equalsIgnoreCase("l"))
								p.getInventory().setLeggings(invY.getItemStack(k));
							else if (k.equalsIgnoreCase("b"))
								p.getInventory().setBoots(invY.getItemStack(k));
						}
						p.getInventory().setContents(invI);
						p.updateInventory();
						invF.delete();
					}
					catch (Exception ex){
						ex.printStackTrace();
						p.sendMessage(ChatColor.RED + "[TTT] " + TTT.local.getMessage(
								"inv-load-error"));
					}
				}
			}
			LobbyManager.updateSigns(worldName);
		}
		p.setScoreboard(plugin.getServer().getScoreboardManager().getNewScoreboard());
		WorldUtils.teleportPlayer(p);
	}

	public static void handleJoin(Player p, String worldName){
		File f = new File(TTT.plugin.getDataFolder(), "bans.yml");
		YamlConfiguration y = new YamlConfiguration();
		try {
			y.load(f);
			if (y.isSet(p.getName())){
				int unbanTime = y.getInt(p.getName());
				if (unbanTime > System.currentTimeMillis() / 1000){
					y.set(p.getName(), null);
					y.save(f);
					if (TTT.plugin.getConfig().getBoolean("verbose-logging"))
						TTT.log.info(p.getName() + "'s ban has been lifted");
				}
				else {
					String m = ChatColor.DARK_PURPLE + "[TTT] ";
					if (unbanTime == -1)
						m += TTT.local.getMessage("karma-permaban");
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(unbanTime * 1000);
					String year = Integer.toString(cal.get(Calendar.YEAR) + 1);
					String month = Integer.toString(cal.get(Calendar.MONTH) + 1);
					String day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
					String hour = Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
					String min = Integer.toString(cal.get(Calendar.MINUTE));
					String sec = Integer.toString(cal.get(Calendar.SECOND));
					m += TTT.local.getMessage("karma-ban") + " " +
							hour + ":" + min + ":" + sec + " on " + month + "/" + day + "/" +
							year + ".";
					p.sendMessage(m);
				}
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
			TTT.log.warning("Failed to load bans from disk!");
		}
		boolean valid = false;
		if (Round.getRound(worldName) == null)
			valid = true;
		else if (Round.getRound(worldName).getStage() != Stage.PLAYING)
			valid = true;
		if (valid){
			File folder = null;
			File tttFolder = null;
			for (String s : Bukkit.getWorldContainer().list()){
				if (s.equalsIgnoreCase(worldName)){
					folder = new File(s);
					worldName = s;
				}
				else if (s.equalsIgnoreCase("TTT_" + worldName))
					tttFolder = new File(s);
				if (folder != null && tttFolder != null)
					break;
			}
			if (folder != null && tttFolder != null){
				boolean loaded = false;
				for (World w : Bukkit.getServer().getWorlds()){
					if(w.getName().equals("TTT_" + worldName)){
						loaded = true;
						break;
					}
				}
				Round r = Round.getRound(worldName);
				if (r == null){
					r = new Round(worldName);
				}
				boolean joined = false;
				for (TTTPlayer t : TTTPlayer.players)
					if (t.getName().equals(p.getName()))
						joined = true;
				if (!joined){
					int maxPlayers = plugin.getConfig().getInt("maximum-players");
					if (r.getPlayers().size() < maxPlayers || maxPlayers == -1){
						if (!loaded){
							TTT.plugin.getServer().createWorld
							(new WorldCreator("TTT_" + worldName));
						}
						p.teleport(TTT.plugin.getServer().getWorld("TTT_" + worldName)
								.getSpawnLocation());
						for (Entity e :
							TTT.plugin.getServer().getWorld("TTT_" + worldName).getEntities())
							if (e instanceof LivingEntity)
								e.remove();
						new TTTPlayer(p.getName(), worldName);
						File invF = new File(TTT.plugin.getDataFolder() + File.separator +
								"inventories" + File.separator +
								p.getName() + ".inv");
						Inventory inv = p.getInventory();
						PlayerInventory pInv = (PlayerInventory)inv;
						try {
							if (!invF.exists())
								invF.createNewFile();
							YamlConfiguration invY = new YamlConfiguration();
							invY.load(invF);
							for (int i = 0; i < inv.getContents().length; i++)
								invY.set(Integer.toString(i), inv.getContents()[i]);
							if (pInv.getHelmet() != null)
								invY.set("h", pInv.getHelmet());
							if (pInv.getChestplate() != null)
								invY.set("c", pInv.getChestplate());
							if (pInv.getLeggings() != null)
								invY.set("l", pInv.getLeggings());
							if (pInv.getBoots() != null)
								invY.set("b", pInv.getBoots());
							invY.save(invF);
						}
						catch (Exception ex){
							ex.printStackTrace();
							p.sendMessage(ChatColor.RED + "[TTT] " + TTT.local
									.getMessage("inv-save-error"));
						}
						inv.clear();
						pInv.setArmorContents(new ItemStack[]{null, null, null, null});
						p.sendMessage(ChatColor.GREEN + TTT.local.getMessage("success-join") +
								" " + worldName);
						List<String> alpha = new ArrayList<String>();
						alpha.add("ZerosAce00000");
						alpha.add("momhipie");
						alpha.add("xJHA929x");
						alpha.add("jmm1999");
						alpha.add("jon674");
						alpha.add("HardcoreBukkit");
						alpha.add("shiny3");
						alpha.add("jpf6368");
						List<String> testers = new ArrayList<String>();
						testers.add("Alexandercitt");
						List<String> translators = new ArrayList<String>();
						translators.add("jon674");
						String addition = "";
						if (p.getName().equals("AngryNerd1"))
							addition = ", " + ChatColor.DARK_RED + TTT.local
							.getMessage("creator") + "," +
									ChatColor.DARK_PURPLE;
						if (alpha.contains(p.getName()) && translators.contains(p.getName()))
							addition += ", " + ChatColor.DARK_RED + TTT.local
							.getMessage("alpha-tester") + ", " + TTT.local.getMessage("translator")
							+ "," + ChatColor.DARK_PURPLE;
						else if (testers.contains(p.getName()) && translators.contains(p.getName()))
							addition += ", " + ChatColor.DARK_RED + TTT.local
							.getMessage("tester") + ", " + TTT.local.getMessage("translator")
							+ "," + ChatColor.DARK_PURPLE;
						else if (alpha.contains(p.getName())){
							addition += ", " + ChatColor.DARK_RED + TTT.local
									.getMessage("alpha-tester") + "," +
									ChatColor.DARK_PURPLE;
						}
						else if (testers.contains(p.getName())){
							addition += ", " + ChatColor.DARK_RED + TTT.local
									.getMessage("tester") + "," +
									ChatColor.DARK_PURPLE;
						}
						else if (translators.contains(p.getName())){
							addition += ", " + ChatColor.DARK_RED + TTT.local
									.getMessage("translator") + "," +
									ChatColor.DARK_PURPLE;
						}
						Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[TTT] " + p.getName() + addition + " " +
								TTT.local.getMessage("joined-map") + " \"" + worldName + "\"");
						int ingamePlayers = 0;
						for (TTTPlayer t : players)
							if (t.getWorld().equals(worldName))
								ingamePlayers += 1;
						if (ingamePlayers >= TTT.plugin.getConfig().getInt("minimum-players") &&
								r.getStage() != Stage.PREPARING){
							for (Player pl : TTT.plugin.getServer().getWorld("TTT_" + worldName).getPlayers())
								pl.sendMessage(ChatColor.DARK_PURPLE + TTT.local.getMessage("round-starting"));
							r.setTime(TTT.plugin.getConfig().getInt("setup-time"));
							r.setStage(Stage.PREPARING);
							SetupManager.setupTimer(worldName);
						}
						else
							p.sendMessage(ChatColor.DARK_PURPLE + TTT.local.getMessage("waiting"));
					}
					else
						p.sendMessage(ChatColor.DARK_PURPLE + TTT.local.getMessage("round-full"));
				}
				else
					p.sendMessage(ChatColor.DARK_PURPLE + TTT.local.getMessage("already-entered"));
			}
			else
				p.sendMessage(ChatColor.RED + TTT.local.getMessage("map-invalid"));
			folder = null;
			tttFolder = null;
		}
		else
			p.sendMessage(ChatColor.RED + "[TTT] " + TTT.local.getMessage("in-progress"));
		LobbyManager.updateSigns(worldName);
	}

	public static void resetRound(String worldName, boolean inno){
		System.out.println("resetting");
		Round.getRound(worldName).setStage(Stage.RESETTING);
		Round.getRound(worldName).setTime(0);
		if (tasks.get(worldName) != null)
			plugin.getServer().getScheduler().cancelTask(tasks.get(worldName));
		tasks.remove(worldName);
		List<Body> removeBodies = new ArrayList<Body>();
		List<Body> removeFoundBodies = new ArrayList<Body>();
		for (Body b : TTT.bodies){
			if (b.getPlayer().isDead()){
				if (b.getPlayer().getWorld() != null){
					if (b.getPlayer().getWorld().equals(worldName)){
						removeBodies.add(b);
						if (TTT.foundBodies.contains(b))
							removeFoundBodies.add(b);
					}
				}
				else {
					removeBodies.add(b);
					if (TTT.foundBodies.contains(b))
						removeFoundBodies.add(b);
				}
			}
			else {
				removeBodies.add(b);
				if (TTT.foundBodies.contains(b))
					removeFoundBodies.add(b);
			}
		}

		for (Body b : removeBodies)
			TTT.bodies.remove(b);

		for (Body b : removeFoundBodies)
			TTT.foundBodies.remove(b);

		removeBodies.clear();
		removeFoundBodies.clear();

		KarmaManager.allocateKarma(worldName);

		if (inno)
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "[TTT] " +
					TTT.local.getMessage("innocent-win").replace("%", "\"" + worldName + "\"") + "!");
		else
			Bukkit.broadcastMessage(ChatColor.DARK_RED + "[TTT] " +
					TTT.local.getMessage("traitor-win").replace("%", "\"" + worldName + "\"") + "!");

		List<String> reset = new ArrayList<String>();
		for (TTTPlayer t : TTTPlayer.players)
			if (t.getWorld().equals(worldName))
				if (plugin.getServer().getPlayer(t.getName()) != null)
					reset.add(t.getName());
		for (String s : reset){
			System.out.println(s);
			resetPlayer(plugin.getServer().getPlayer(s));
		}

		plugin.getServer().unloadWorld("TTT_" + worldName, false);
		WorldUtils.rollbackWorld(worldName);
		Round.getRound(worldName).setStage(Stage.WAITING);
		LobbyManager.updateSigns(worldName);
	}
}
