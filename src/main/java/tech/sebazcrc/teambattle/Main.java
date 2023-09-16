package tech.sebazcrc.teambattle;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import tech.sebazcrc.teambattle.board.ScoreHelper;
import tech.sebazcrc.teambattle.game.Game;
import tech.sebazcrc.teambattle.game.enums.GamePhase;
import tech.sebazcrc.teambattle.game.GamePlayer;
import tech.sebazcrc.teambattle.game.enums.GameState;
import tech.sebazcrc.teambattle.game.gui.TeamSelectionGUI;
import tech.sebazcrc.teambattle.library.ItemBuilder;
import tech.sebazcrc.teambattle.library.XSound;
import tech.sebazcrc.teambattle.util.CleanGenerator;
import tech.sebazcrc.teambattle.util.Regions;
import tech.sebazcrc.teambattle.util.Utils;

import java.util.UUID;

public final class Main extends JavaPlugin implements CommandExecutor {
    private static Main instance;
    public boolean FINAL_BATTLE = false;

    private Game game;

    @Override
    public void onEnable() {
        instance = this;
        prepareWorld();

        this.game = new Game();
        TeamSelectionGUI.getInstance();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!ScoreHelper.hasScore(p)) ScoreHelper.createScore(p);

            GamePlayer gp = new GamePlayer(p.getUniqueId(), p.getName());
            game.addPlayer(gp);
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(Regions.getInstance().MAIN_SPAWNPOINT);
            p.getInventory().setHelmet(new ItemStack(Material.WHITE_WOOL));

            p.getInventory().clear();
            p.getInventory().setItem(0, new ItemBuilder(Material.NETHER_STAR).setDisplayName("&6Selector de Equipo").addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addItemFlag(ItemFlag.HIDE_ENCHANTS).build());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);

            ScoreHelper helper = ScoreHelper.getByPlayer(p);
            Scoreboard board = helper.getScoreboard();

            Team ownWait = Utils.getOrRegisterTeam(board, "10-waiting");
            ownWait.setColor(ChatColor.GRAY);
            ownWait.addEntry(p.getName());

            for (Player on : Bukkit.getOnlinePlayers()) {
                if (!on.equals(p)) {
                    ownWait.addEntry(on.getName());

                    Team otherWait = Utils.getOrRegisterTeam(on.getScoreboard(), "10-waiting");
                    otherWait.addEntry(p.getName());
                }
            }

            p.setScoreboard(board);
        }

        getCommand("tb").setExecutor(this);
        getCommand("tc").setExecutor(this);
        TBListeners.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("tb")) {
            Player p = (Player) sender;
            if (!p.hasPermission("teambattle.use")) return false;

            if (args[0].equalsIgnoreCase("start")) {
                game.disableJoins();
                new BukkitRunnable() {
                    private int reaming = 5;

                    @Override
                    public void run() {
                        if (reaming > 0) {
                            for (Player on : Bukkit.getOnlinePlayers()) {
                                on.sendTitle(Utils.format("&6&l" + reaming), Utils.format("&7¡Prepárate para jugar!"), 1, 20, 1);
                                on.sendMessage(Utils.format("&eLa partida comienza en: &b" + reaming + "&e segundos."));
                                on.playSound(on.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 10.0F);
                            }

                            reaming--;
                            return;
                        }

                        game.startGame();
                        this.cancel();
                    }
                }.runTaskTimer(instance, 0L, 20L);
            } else if (args[0].equalsIgnoreCase("debugtime")) {
                getGame().setTime(60*59);
            } else if (args[0].equalsIgnoreCase("debugtimetwo")) {
                getGame().setTime(60*119);
            } else if (args[0].equalsIgnoreCase("cut")) {
                String s = "testeo";
                p.sendMessage(s);
                p.sendMessage(s.substring(0, s.length()-1));
            } else if (args[0].equalsIgnoreCase("finalBattle")) {
                for (Player on : Bukkit.getOnlinePlayers()) {
                    on.teleport(p.getLocation());
                }
                FINAL_BATTLE = true;
            } else if (args[0].equalsIgnoreCase("testDecimal")) {
                double d = 5.5;
                double t = 3;

                p.sendMessage("Decimal d: " + Utils.isDecimal(d));
                p.sendMessage("Decimal t: " + Utils.isDecimal(t));
            } else if (args[0].equalsIgnoreCase("testSlots")) {
                int size = Integer.parseInt(args[1]);
                int avaibleExtraSlots = 0;
                double r = size / 4.0D;
                double d = Utils.decimalOperation(r);

                if (d > 0) {
                    while (d > 0) {
                        avaibleExtraSlots++;
                        d = d - 0.25;
                    }
                }

                p.sendMessage("Slots: " + avaibleExtraSlots);
            } else if (args[0].equalsIgnoreCase("testSl2")) {
                int allTeams = 4;
                int size = Integer.parseInt(args[1]);
                int avaibleExtraSlots = 0;

                if ((size % allTeams) != 0) { // no es múltiplo
                    int maxReduction = Math.max(0, size-allTeams);

                    int i = 0;
                    int result = size;
                    while (i < maxReduction) {
                        result--;
                        if ((result % allTeams) == 0) { // múltiplo
                            avaibleExtraSlots = (i+1);
                            break;
                        }
                        i++;
                    }
                }

                p.sendMessage("Slots: " + avaibleExtraSlots);
            } else if (args[0].equalsIgnoreCase("bots")) {
                int a = Integer.parseInt(args[1]);
                for (int i = 0; i < a; i++) {
                    GamePlayer gp = new GamePlayer(p.getUniqueId(), p.getName());
                    game.addPlayer(gp);
                }
            } else if (args[0].equalsIgnoreCase("bots2")) {
                int a = Integer.parseInt(args[1]);
                for (int i = 0; i < a; i++) {
                    GamePlayer gp = new GamePlayer(UUID.randomUUID(), ("Pepe_"+i));
                    game.addPlayer(gp);
                }
            } else if (args[0].equalsIgnoreCase("players")) {
                int i = 0;
                for (GamePlayer gp : game.getPlayers()) {
                    p.sendMessage(gp.getName() + " - Team: " + (gp.getTeam() == null ? "null" : gp.getTeam().getTeamName()));
                    i++;
                }
                p.sendMessage("");
                p.sendMessage("Jugadores: " + i);
            }
        } else if (label.equalsIgnoreCase("tc")) {
            Player p = (Player) sender;
            GamePlayer gp = game.getPlayer(p.getUniqueId());

            if (gp.getTeam() == null) {
                p.sendMessage(Utils.format("&cNo estás en ningún equipo."));
                return false;
            }
            if (args.length == 0) {
                p.sendMessage(Utils.format("&cEscribe tu mensaje."));
                return false;
            }

            String msg = "";
            for (int i = 0; i < args.length; i++) {
                msg = msg + " " + args[i];
            }

            for (GamePlayer team : gp.getTeam().getPlayers()) {
                team.sendMessage("&8[" + gp.getTeam().getTeamColor() + "Equipo&r&8] &e" + p.getName() + "&7:&f" + msg, false);
            }
        }


        return false;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void prepareWorld() {
        WorldCreator creator = new WorldCreator("teambattle").generateStructures(false).generator(new CleanGenerator()).environment(World.Environment.NORMAL);
        World w = (Bukkit.getWorld("teambattle") == null ? creator.createWorld() : Bukkit.getWorld("teambattle"));

        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.KEEP_INVENTORY, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.SPAWN_RADIUS, 0);
        w.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 100);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

        w.setDifficulty(Difficulty.HARD);

        w.setWeatherDuration(0);
        w.setThunderDuration(0);
        w.setStorm(false);
        w.setThundering(false);

        w.setTime(2500);
        w.setTicksPerMonsterSpawns(100);
    }

    public static Main getInstance() {
        return instance;
    }

    public Game getGame() {
        return game;
    }
}
