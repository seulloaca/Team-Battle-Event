package tech.sebazcrc.teambattle.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import tech.sebazcrc.teambattle.Main;
import tech.sebazcrc.teambattle.board.ScoreHelper;
import tech.sebazcrc.teambattle.board.ScoreStringBuilder;
import tech.sebazcrc.teambattle.game.enums.FightStatus;
import tech.sebazcrc.teambattle.game.enums.GamePhase;
import tech.sebazcrc.teambattle.game.enums.GameState;
import tech.sebazcrc.teambattle.game.gui.TeamSelectionGUI;
import tech.sebazcrc.teambattle.library.*;
import tech.sebazcrc.teambattle.util.Regions;
import tech.sebazcrc.teambattle.util.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private List<GamePlayer> players;
    private List<GameTeam> teams;
    private int time;

    private GameState state;
    private GamePhase phase;

    private int ticks;

    // SCOREBOARD
    //private ScoreHelper scoreboard;
    public Map<Player, Integer> currentSubString;
    private ArrayList<String> lines;

    private boolean joins;

    public Game() {
        this.players = new ArrayList<>();
        this.time = 0;
        this.ticks = 0;
        this.joins = true;
        this.state = GameState.WAITING;
        this.phase = GamePhase.WAITING;
        this.teams = new ArrayList<>();

        this.lines = new ArrayList<>();

        lines.add("&3&lTEAM BATTLE");
        lines.add("&b&lT&3&lEAM BATTLE");
        lines.add("&3&lT&b&lE&3&lAM BATTLE");
        lines.add("&3&lTE&b&lA&3&lM BATTLE");
        lines.add("&3&lTEA&b&lM &3&lBATTLE");
        lines.add("&3&lTEAM &b&lB&3&lATTLE");
        lines.add("&3&lTEAM B&b&lA&3&lTTLE");
        lines.add("&3&lTEAM BA&b&lT&3&lTLE");
        lines.add("&3&lTEAM BAT&b&lT&3&lLE");
        lines.add("&3&lTEAM BATT&b&lL&3&lE");
        lines.add("&3&lTEAM BATTL&b&lE");
        lines.add("&3&lTEAM BATTLE");
        lines.add("&b&lTEAM BATTLE");
        lines.add("&b&lTEAM BATTLE");
        lines.add("&3&lTEAM BATTLE");
        lines.add("&b&lTEAM BATTLE");
        lines.add("&b&lTEAM BATTLE");
        lines.add("&3&lTEAM BATTLE");
        lines.add("&3&lTEAM BATTLE");
        lines.add("&3&lTEAM BATTLE");
        lines.add("&3&lTEAM BATTLE");
        lines.add("&3&lTEAM BATTLE");

        this.currentSubString = new HashMap<>();

        //this.scoreboard = ScoreHelper.registerScoreboard();
        // this.scoreboard.setTitle("&e&lTEAM BATTLE");
        registerTeams();
        scheduleTask();
    }

    private void registerTeams() {
        addTeam("Azul", ChatColor.BLUE, Regions.getInstance().BLUE_TEAM_SPAWNPOINT, Regions.getInstance().BLUE_TEAM_CAGE, Regions.getInstance().BLUE_TEAM_SPAWN, Color.BLUE, "BLUE", 0);
        addTeam("Rojo", ChatColor.RED, Regions.getInstance().RED_TEAM_SPAWNPOINT, Regions.getInstance().RED_TEAM_CAGE, Regions.getInstance().RED_TEAM_SPAWN, Color.RED, "RED", 1);
        addTeam("Amarillo", ChatColor.YELLOW, Regions.getInstance().YELLOW_TEAM_SPAWNPOINT, Regions.getInstance().YELLOW_TEAM_CAGE, Regions.getInstance().YELLOW_TEAM_SPAWN, Color.YELLOW, "YELLOW", 2);
        addTeam("Verde", ChatColor.DARK_GREEN, Regions.getInstance().GREEN_TEAM_SPAWNPOINT, Regions.getInstance().GREEN_TEAM_CAGE, Regions.getInstance().GREEN_TEAM_SPAWN, Color.GREEN, "GREEN", 3);
    }

    private void addTeam(String name, ChatColor color, org.bukkit.Location spawnpoint, org.bukkit.Location cageSpawn, Cuboid spawnArea, Color c2, String tn, int index) {
        this.teams.add(new GameTeam(spawnpoint, cageSpawn, spawnArea, name, color, c2, tn, index));
    }

    public void scheduleTask() {
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            boolean isSecond = (ticks % 20) == 0;

            for (GamePlayer player : getPlayers()) {
                if (isSecond) player.tickPlayer(this);

                if (player.getPlayer() != null) {
                    Player p = player.getPlayer();

                    createScoreboard(p);
                    updateScoreboard(p, player);
                    updateTabList(p);

                    if ((p.getAllowFlight() || p.isFlying()) && !p.isOp() && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
                        p.setFlying(false);
                        p.setAllowFlight(false);
                    }
                    if (player.shouldSetSpectatorMode() && p.getGameMode() != GameMode.SPECTATOR && !p.isOp()) {
                        p.setGameMode(GameMode.SPECTATOR);
                        p.setAllowFlight(true);
                        p.setFlying(true);
                        player.setShouldSetSpectatorMode(false);
                    }
                }
            }

            if (isState(GameState.PLAYING) && isSecond) {
                if (time <= 3600) {
                    int minAndSec = time % 3600;
                    int min = minAndSec / 60;
                    int sec = minAndSec % 60;

                    if (min >= 55) {
                        if (min == 59) {
                            if (sec == 0 || sec == 15 || sec == 30 || sec == 45 || sec >= 50) {
                                broadcast("&eLas barreras se eliminarán en &b" + (60-sec) + " &esegundos.");
                            }
                        } else if (sec == 0) {
                            broadcast("&eLas barreras se eliminarán en &b" + (60 - min) + " &eminutos.");
                        }
                    }
                    if (time == 3600) {
                        broadcast("&a¡Se han eliminado las barreras!", Sound.ITEM_TRIDENT_THUNDER, 100.0F, -1);

                        World w = Bukkit.getWorld("teambattle");

                        Utils.setThunder(w, 60*60*2);
                        FastEditSession session = new FastEditSession(w, ((CraftWorld)w).getHandle());

                        for (Block b : Regions.getInstance().BARRIERS_AREA) {
                            if (b.getType() == Material.BARRIER) {
                                session.setBlock(b.getX(), b.getY(), b.getZ(), Material.AIR);
                            }
                        }
                        session.update();

                        this.phase = GamePhase.PVP;
                    }
                }

                time++;
            } else if (isState(GameState.WAITING)) {
                TeamSelectionGUI.getInstance().tick();
            }
            ticks+=4;
        }, 0L, 4L);
    }

    public void broadcast(String s) {
        broadcast(s, Sound.UI_BUTTON_CLICK, 100.0F, 1.0F);
    }

    public void broadcast(String s, Sound sound, float volume, float pitch) {
        for (Player on : Bukkit.getOnlinePlayers()) {
            on.sendMessage(Utils.format(s));
            on.playSound(on.getLocation(), sound, SoundCategory.AMBIENT, volume, pitch);
        }
    }

    private void updateTabList(Player p) {
        p.setPlayerListHeader(Utils.format(ScoreHelper.getByPlayer(p).getTitle() + "&r\n&7¡Bienvenido a SebazCRC Projects!\n&7Organizo distintos eventos para la comunidad\n&7de vez en cuando.\n"));
        p.setPlayerListFooter(Utils.format("\n&7¡Puedes apoyarme en estos enlaces!\n&e&lDonaciones:&7 donacionessebazcrc.craftingstore.net\n&9&lDiscord:&7 https://discord.gg/w58wzrcJU8"));
    }

    public void startGame() {
        getPlayers().removeIf(ap -> Bukkit.getPlayer(ap.getUUID()) == null || !ap.getPlayer().isOnline());

        enableJoins();
        populateTeams();
        preparePlayerScoreboards();

        for (GamePlayer on : getPlayers()) {
            Player p = on.getPlayer();

            on.sendTitle(Utils.format("&a&l¡Buena suerte!"), Utils.format("&7¡Ha comenzado la partida!"), 8, 20 * 3, 8);
            on.sendMessage(Utils.format("&a¡La partida ha comenzado!"), true);
            on.playSound(XSound.ENTITY_PLAYER_LEVELUP, Float.MAX_VALUE, 0.3F);

            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20.0D);
            p.setFoodLevel(20);
            p.setLevel(0);
            p.setExp(0.0F);
            p.teleport(on.getTeam().getSpawnPoint());
            populatePlayerInventory(p);

            p.setFlying(false);
            p.setAllowFlight(false);
        }

        World w = Bukkit.getWorld("teambattle");
        FastEditSession session = new FastEditSession(w, ((CraftWorld) w).getHandle());
        for (Block b : Regions.getInstance().LOBBY_AREA.getBlocks()) {
            session.setBlock(b.getX(), b.getY(), b.getZ(), Material.AIR);
        }
        session.update();

        setPhase(GamePhase.PVE);
        setState(GameState.PLAYING);
    }

    public void populateTeams() {
        int teamIndex = 0;

        for (GamePlayer gp : getPlayers()) {
            if (gp.getTeam() != null) continue;
            int joining = teamIndex % 4;

            GameTeam team = getTeams().get(joining);
            gp.setTeam(team);
            team.join(gp);

            teamIndex++;
        }
    }

    public void preparePlayerScoreboards() {
        for (GamePlayer gp : getPlayers()) {
            Scoreboard board = ScoreHelper.getByPlayer(gp.getUUID()).getScoreboard();

            int i = 0;
            for (GameTeam team : getTeams()) {
                Team bt = Utils.getOrRegisterTeam(board, String.format(""+(i), "%02d"));
                bt.setColor(team.getTeamColor());
                team.getPlayers().forEach(ap -> bt.addEntry(ap.getName()));
                i++;
            }

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {
                @Override
                public void run() {
                    Player p = gp.getPlayer();
                    if (p != null) {
                        p.setScoreboard(board);
                    }
                }
            }, 2L);
        }
    }

    public void populatePlayerInventory(Player p) {
        GamePlayer gp = getPlayer(p.getUniqueId());
        GameTeam team = gp.getTeam();

        if (team != null) {
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);

            team.addArmor(p);

            p.getInventory().addItem(new ItemBuilder(Material.WOODEN_AXE).build());
            p.getInventory().addItem(new ItemBuilder(Material.SPRUCE_SAPLING, 4).build());
            p.getInventory().addItem(new ItemBuilder(Material.BREAD, 32).build());
            p.getInventory().addItem(new ItemBuilder(Material.GOLDEN_APPLE, 12).build());
            p.getInventory().addItem(new ItemBuilder(Material.COW_SPAWN_EGG, 2).build());
            p.getInventory().addItem(new ItemBuilder(Material.VILLAGER_SPAWN_EGG, 2).build());

            p.getInventory().setItem(8, Utils.getWool(gp));
            p.getInventory().setItem(7, new ItemBuilder(Material.BOW).addEnchant(Enchantment.ARROW_INFINITE, 1).addEnchant(Enchantment.DURABILITY, 3).setDisplayName(Utils.format(team.getTeamColor() + "&lArco")).build());
            p.getInventory().setItem(6, new ItemStack(Material.ARROW));

            //if (!team.getScoreboardTeam().hasEntry(p.getName())) team.getScoreboardTeam().addEntry(p.getName());
        } else {
            throw new Error("No se ha definido el equipo para " + p.getName());
        }
    }

    public void searchForEnd() {
        if (isState(GameState.ENDED)) return;

        List<GameTeam> alive = getAliveTeams();

        if (alive.size() == 1) {
            GameTeam team = getAliveTeams().get(0);

            Bukkit.broadcastMessage(Utils.format("&e¡El equipo " + team.getFormattedTeamName() + " &r&eha ganado la partida!"));

            for (GamePlayer gp : getPlayers()) {
                gp.playSound(XSound.BLOCK_NOTE_BLOCK_PLING, 10.0F, 100.0F);
                if (!gp.getTeam().getTeamName().equalsIgnoreCase(team.getTeamName())) {
                    gp.sendTitle("&c&l¡PERDISTE!", "&7El equipo " + team.getFormattedTeamName() + " &r&7ha ganado la partida.");
                } else {
                    gp.sendTitle(gp.getTeam().getTeamColor() + "&l¡GG!", "&7Tu equipo ganó la partida.");
                }
            }

            this.state = GameState.ENDED;
        } else if (alive.size() == 0) {
            Bukkit.broadcastMessage(Utils.format("&e¡Vaya!, al parecer todos murieron a la vez."));
            this.state = GameState.ENDED;
        }
    }

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public List<GamePlayer> getSpectators() {
        return getPlayers().stream().filter(GamePlayer::isSpectator).collect(Collectors.toList());
    }

    public List<GamePlayer> getAlivePlayers() {
        return getPlayers().stream().filter(gamePlayer -> !gamePlayer.isSpectator()).collect(Collectors.toList());
    }

    public List<GameTeam> getTeams() {
        return teams;
    }

    public List<GameTeam> getAliveTeams() {
        return getTeams().stream().filter(team -> !team.isEliminated()).collect(Collectors.toList());
    }

    public void setTime(int time) {
        this.time = time;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public boolean isPhase(GamePhase p) {
        return this.phase == p;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public boolean isState(GameState s) {
        return state == s;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public GamePlayer getPlayer(UUID id) {
        for (GamePlayer gp : getPlayers()) {
            if (gp.getUUID().toString().equalsIgnoreCase(id.toString())) return gp;
        }
        return null;
    }

    public void addPlayer(GamePlayer gamePlayer) {
        this.players.add(gamePlayer);
    }

    private void createScoreboard(Player p) {
        Scoreboard b = ScoreHelper.getByPlayer(p).getScoreboard();
        if (!p.getScoreboard().equals(b)) p.setScoreboard(b);
    }

    private void updateScoreboard(Player p, GamePlayer gp) {
        ScoreHelper helper = ScoreHelper.getByPlayer(p);
        String s = getScoreboardLines(p, gp);

        String[] split = s.split("\n");
        List<String> lines = new ArrayList<>(Arrays.asList(split));
        helper.setSlotsFromList(lines);

        if (!this.currentSubString.containsKey(p)) {
            this.currentSubString.put(p, 0);
        } else {
            int plus = this.currentSubString.get(p) + 1;
            if (plus > this.lines.size()-1) {
                plus = 0;
            }
            this.currentSubString.replace(p, plus);
        }

        ScoreHelper.getByPlayer(p).setTitle(Utils.format(this.lines.get(this.currentSubString.get(p))));
    }


    private String getScoreboardLines(Player p, GamePlayer gp) {
        ScoreStringBuilder b = new ScoreStringBuilder(true);

        if (!isState(GameState.WAITING)) {
            if (isState(GameState.ENDED) || ((time % 10) != 0)) {
                b.add("&bJugadores: " + "&f" + getAlivePlayers().size());
            } else {
                b.add("&bEspectadores: " + "&f" + getSpectators().stream().filter(ac -> ac.getPlayer() != null).count());
            }

            if (isState(GameState.PLAYING)) {
                b.add("&bTiempo: &f" + Utils.formatInterval(this.time)).space();

                b.add("&7⚝ &bFase: &f" + phase.getName());
                b.add("&7☠ &bKills: &f" + gp.getKills());
                b.add("&7☠ &bMuertes: &f" + gp.getDeathCount() + "&7 / &b3");
                b.add("&7⚔ &bAsistencias: &f" + gp.getAssists());
                if (gp.getStatus() == FightStatus.FIGHTING) b.add("&7⚔ &cEn lucha " + (gp.getFightRemoval() == 15 ? "" : "&7(&b" + gp.getFightRemoval() + "&7)"));
                b.space();

                for (GameTeam teams : getTeams()) {
                    b.add(teams.getFormattedTeamName() + "&7 > " + "&f" + teams.getPlayers().stream().filter(player -> !player.isSpectator()).count());
                }

                b.space();
            } else if (isState(GameState.ENDED)) {
                b.space();
                b.add("&fMuchas gracias");
                b.add("&fpor participar.");
            }
        } else {
            b.add("&c&lESPERANDO...");
            b.add(" ");
            b.add("&bJugadores:").add("&f" + Bukkit.getOnlinePlayers().size());
            b.add(" ");
            b.add("&bModalidad");
            b.add("&fTeam Battle &7v1.1.1");
            b.add(" ");
            b.add("&bEquipo:");
            b.add("&7" + (gp.getTeam() == null ? "Ninguno" : (gp.getTeam().getTeamColor() + "&l" + gp.getTeam().getTeamName())));
            b.add(" ");
        }

        return b.build();
    }

    public GameTeam getTeam(String n) {
        for (GameTeam t : getTeams()) {
            if (t.getTeamName().equalsIgnoreCase(n)) return t;
        }

        return null;
    }

    public GameTeam getTeamByMaterial(String colorName) {
        for (GameTeam t : getTeams()) {
            if (t.getColorName().equalsIgnoreCase(colorName)) return t;
        }

        return null;
    }

    public GameState getState() {
        return this.state;
    }

    public void disableJoins() {
        this.joins = false;
    }

    public void enableJoins() {
        this.joins = true;
    }

    public boolean isJoiningEnabled() {
        return this.joins;
    }
}
