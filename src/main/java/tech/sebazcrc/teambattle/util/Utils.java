package tech.sebazcrc.teambattle.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import tech.sebazcrc.teambattle.game.GamePlayer;
import tech.sebazcrc.teambattle.game.GameTeam;
import tech.sebazcrc.teambattle.library.ItemBuilder;

import java.util.Arrays;

public class Utils {

    public static String format(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static Team getOrRegisterTeam(Scoreboard board, String s) {
        return board.getTeam(s) == null ? board.registerNewTeam(s) : board.getTeam(s);
    }

    public static void setThunder(World w, int seconds) {
        net.minecraft.server.v1_16_R3.WorldServer nmsw = ((CraftWorld)w).getHandle();

        int secondsInTicks = seconds*20;
        int weatherDuration = 0;
        int thunderDuration = 0;

        if (w.hasStorm() && w.getWeatherDuration() > 0) {
            weatherDuration = (Math.max(nmsw.worldDataServer.getWeatherDuration(), 0));
            thunderDuration = (Math.max(nmsw.worldDataServer.getThunderDuration(), 0));
        }

        nmsw.worldDataServer.setClearWeatherTime(0);
        nmsw.worldDataServer.setWeatherDuration(weatherDuration + secondsInTicks);
        nmsw.worldDataServer.setThunderDuration(thunderDuration + secondsInTicks);
        nmsw.worldDataServer.setStorm(true);
        nmsw.worldDataServer.setThundering(true);
    }

    public static String formatInterval(final int totalTime) {

        int hrs = totalTime / 3600;
        int minAndSec = totalTime % 3600;
        int min = minAndSec / 60;
        int sec = minAndSec % 60;

        String s = String.format((hrs > 0 ? "%02d:" : ""), hrs);

        return String.format(s + "%02d:%02d", min, sec);
    }

    public static boolean isDecimal(double d) {
        int i = (int) d;

        return ((d-i) > 0);
    }

    public static double decimalOperation(double d) {
        int i = (int) d;
        return (d-i);
    }

    public static boolean cantBeDropped(Material s) {
        return Tag.WOOL.isTagged(s) || s == Material.BOW || s == Material.ARROW || s == Material.LEATHER_HELMET || s == Material.LEATHER_CHESTPLATE || s == Material.LEATHER_LEGGINGS || s == Material.LEATHER_BOOTS;
    }

    public static boolean hasSlot(Inventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = inventory.getItem(i);

            if (s == null || s.getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack getWool(GamePlayer gp) {
        GameTeam team = gp.getTeam();
        if (team != null) {
            return new ItemBuilder(Material.valueOf(team.getColorName() + "_WOOL"), 64).setDisplayName(Utils.format(team.getTeamColor() + "&lBloque de Construcción")).setLore(Arrays.asList(" ", Utils.format("&7Construye lana de tu color"), Utils.format("infinitamente."), "", Utils.format("&cTrolear a tu equipo será &lBANEABLE"))).addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addItemFlag(ItemFlag.HIDE_ENCHANTS).setCustomModelData(1).build();
        } else {
            return new ItemBuilder(Material.WHITE_WOOL, 64).setDisplayName(Utils.format("&f&lBloque de Construcción")).setLore(Arrays.asList(" ", Utils.format("&7Construye lana de tu color"), Utils.format("infinitamente."), "", Utils.format("&cTrolear a tu equipo será &lBANEABLE"))).addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addItemFlag(ItemFlag.HIDE_ENCHANTS).setCustomModelData(1).build();
        }
    }
}
