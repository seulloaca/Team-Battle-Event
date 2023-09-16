package tech.sebazcrc.teambattle.game;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import tech.sebazcrc.teambattle.Main;
import tech.sebazcrc.teambattle.board.ScoreHelper;
import tech.sebazcrc.teambattle.library.Cuboid;
import tech.sebazcrc.teambattle.library.ItemBuilder;
import tech.sebazcrc.teambattle.library.LeatherArmorBuilder;
import tech.sebazcrc.teambattle.library.XSound;
import tech.sebazcrc.teambattle.util.Utils;

import java.util.*;

public class GameTeam {
    private List<GamePlayer> players;

    private Location spawnPoint;
    private Location cageSpawn;
    private Cuboid cageCuboid;

    private String teamName;
    private ChatColor teamColor;
    private Color color;
    private String colorName;

    private int index;

    //private Team team;

    public GameTeam(Location spawnPoint, Location cageSpawn, Cuboid cageCuboid, String teamName, ChatColor teamColor, Color color, String cname, int index) {
        this.spawnPoint = spawnPoint;
        this.cageSpawn = cageSpawn;
        this.cageCuboid = cageCuboid;
        this.teamName = teamName;
        this.teamColor = teamColor;
        this.color = color;
        this.players = new ArrayList<>();
        this.colorName = cname;

        //this.team = board.registerNewTeam(teamName);
        //team.setColor(teamColor);
        //team.setCanSeeFriendlyInvisibles(true);
    }

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public String getTeamName() {
        return teamName;
    }

    public ChatColor getTeamColor() {
        return teamColor;
    }

    public void join(GamePlayer player) {
        if (!this.players.contains(player)) {
            this.players.add(player);
            for (GamePlayer members : getPlayers()) {
                members.sendMessage(Utils.format(getTeamColor() + player.getName() + "&e se ha unido al equipo &7(&b" + getPlayers().size() + " &7jugadores)&e."), false);
                members.playSound(XSound.UI_BUTTON_CLICK);
            }

            Player p = player.getPlayer();
            if (p != null) {
                //addArmor(p);
                p.getInventory().setHelmet(new ItemStack(Material.valueOf(getColorName() + "_WOOL")));
            }
        }

        //this.team.addEntry(player.getName());
    }

    void addArmor(Player p) {
        p.getInventory().setHelmet(new LeatherArmorBuilder(Material.LEATHER_HELMET, 1).setColor(getColor()).setUnbrekeable(true).build());
        p.getInventory().setChestplate(new LeatherArmorBuilder(Material.LEATHER_CHESTPLATE, 1).setColor(getColor()).setUnbrekeable(true).build());
        p.getInventory().setLeggings(new LeatherArmorBuilder(Material.LEATHER_LEGGINGS, 1).setColor(getColor()).setUnbrekeable(true).build());
        p.getInventory().setBoots(new LeatherArmorBuilder(Material.LEATHER_BOOTS, 1).setColor(getColor()).setUnbrekeable(true).build());
    }

    public void removePlayer(GamePlayer player) {
        if (this.players.contains(player)) {
            this.players.remove(player);

            for (GamePlayer members : getPlayers()) {
                members.sendMessage(Utils.format(getTeamColor() + player.getName() + "&e ha abandonado el equipo &7(&b" + getPlayers().size() + " &7jugadores)&e."), false);
                members.playSound(XSound.UI_BUTTON_CLICK);
            }
        }
    }

    public boolean isEliminated() {
        for (GamePlayer gp : getPlayers()) {
            if (!gp.isSpectator()) return false;
        }
        return true;
    }

    public String getFormattedTeamName() {
        return Utils.format(getTeamColor() + "&l" + StringUtils.capitalize(getTeamName()));
    }

    public String formatChatMessage(String playerName, String message) {
        return Utils.format("&8[" + getFormattedTeamName() + "&r&8] &e" + playerName + " &7> &f" + message);
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public Cuboid getCageCuboid() {
        return cageCuboid;
    }

    public Location getCageSpawn() {
        return cageSpawn;
    }

    public String getColorName() {
        return colorName;
    }

    public Color getColor() {
        return color;
    }

}
