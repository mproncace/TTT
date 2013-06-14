package net.amigocraft.TTT.managers;

import java.io.File;
import java.util.HashMap;

import org.bukkit.configuration.file.YamlConfiguration;

import net.amigocraft.TTT.Role;
import net.amigocraft.TTT.TTT;
import net.amigocraft.TTT.TTTPlayer;

public class KarmaManager {

	public static HashMap<String, Integer> playerKarma = new HashMap<String, Integer>();

	public static void saveKarma(String worldName){
		for (TTTPlayer t : TTTPlayer.players){
			if (t.getWorld().equals(worldName)){
				playerKarma.remove(t.getName());
				playerKarma.put(t.getName(), t.getKarma());
				saveKarma(t);
			}
		}

	}

	public static void saveKarma(TTTPlayer t){
		File karmaFile = new File(TTT.plugin.getDataFolder(), "karma.yml");
		try {
			if (karmaFile.exists()){
				YamlConfiguration karmaYaml = new YamlConfiguration();
				karmaYaml.load(karmaFile);
				karmaYaml.set(t.getName(), t.getKarma());
				karmaYaml.save(karmaFile);
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public static void swapDisplayKarma(String worldName){
		for (TTTPlayer t : TTTPlayer.players){
			if (t.getWorld().equals(worldName)){
				t.setDisplayKarma(t.getKarma());
			}
		}
	}

	public static void loadKarma(String pName){
		File karmaFile = new File(TTT.plugin.getDataFolder(), "karma.yml");
		try {
			if (karmaFile.exists()){
				YamlConfiguration karmaYaml = new YamlConfiguration();
				karmaYaml.load(karmaFile);
				if (karmaYaml.isSet(pName))
					playerKarma.put(pName, karmaYaml.getInt(pName));
				else
					playerKarma.put(pName, 1000);
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public static void allocateKarma(String worldName){
		for (TTTPlayer t : TTTPlayer.players){
			if (t.getWorld().equals(worldName)){
				t.addKarma((int)(1 / ((double)t.getKarma() / 1000) * 15));
			}
		}
	}

	public static void handleDamageKarma(TTTPlayer damager, TTTPlayer victim, int damage){
		if (damager.isTraitor() == victim.isTraitor())
			damager.subtractKarma((int)(
					victim.getKarma() * (damage * TTT.plugin.getConfig().getDouble("player-damages-ally-ratio"))));
		else if (damager.getRole() == Role.INNOCENT && victim.getRole() == Role.TRAITOR)
			damager.addKarma((int)(TTT.plugin.getConfig().getInt("max-karma") *
					damage * TTT.plugin.getConfig().getDouble("i-damages-t-ratio")));
	}

	public static void handleKillKarma(TTTPlayer killer, TTTPlayer victim){
		if (killer.isTraitor() == victim.isTraitor())
			killer.subtractKarma((int)(victim.getKarma() * 15 * TTT.plugin.getConfig().getDouble("player-kills-ally")));
		else if (killer.getRole() == Role.TRAITOR && victim.getRole() == Role.INNOCENT)
			killer.addKarma(TTT.plugin.getConfig().getInt("tbonus"));
	}
}
