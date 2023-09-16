package tech.sebazcrc.teambattle.game.gui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.sebazcrc.teambattle.Main;
import tech.sebazcrc.teambattle.game.Game;
import tech.sebazcrc.teambattle.game.GamePlayer;
import tech.sebazcrc.teambattle.game.GameTeam;
import tech.sebazcrc.teambattle.library.ItemBuilder;
import tech.sebazcrc.teambattle.util.Regions;
import tech.sebazcrc.teambattle.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TeamSelectionGUI {
    private Game game;
    private static TeamSelectionGUI instance;

    private Inventory inv;
    private ItemStack pane;

    private List<Player> viewers;
    private List<TeamStatus> teams;
    //private int maxTeamPlayers;

    public TeamSelectionGUI(Game game) {
        //this.maxTeamPlayers = 1;
        this.game = game;
        this.viewers = new ArrayList<>();
        this.teams = new ArrayList<>();

        this.pane = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ").build();

        this.inv = Bukkit.createInventory(null, 36, Utils.format("&8Elegir equipo"));

        int teamIndex = 0;
        int slot = 10;
        for (GameTeam all : game.getTeams()) {
            teamIndex++;
            this.teams.add(new TeamStatus(all, new ItemBuilder(Material.valueOf(all.getColorName() + "_WOOL")).setDisplayName(all.getTeamColor() + "Equipo " + all.getTeamName()).build(), slot));

            if ((teamIndex % 4) == 0) {
                slot += 4;
            } else {
                slot += 2;
            }
        }

        for (int i = 0; i < inv.getSize(); i++) {
            this.inv.setItem(i, pane.clone());
        }
        inv.setItem(31, new ItemBuilder(Material.BARRIER).setDisplayName("&cAbandonar equipo").setLore(Arrays.asList("", Utils.format("&7Abandona el equipo actual,"), Utils.format("&7te quedarás sin equipo temporalmente"), "", Utils.format("&7Puedes seleccionar otro equipo"), Utils.format("&7sin problemas."), "", Utils.format("&7Útil para ceder un slot a alguien más."))).build());
    }

    public void tick() {
        int perfectPlayers = Math.max(4, (game.getPlayers().isEmpty() ? 2 : game.getPlayers().size()/4));

        int avaibleExtraSlots = 0;
        int size = game.getPlayers().size();
        int allTeams = teams.size(); // 4

        if (size > (allTeams*4)) {
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
        }

        for (TeamStatus status : this.teams) {
            int max = perfectPlayers;
            if (avaibleExtraSlots > 0) {
                max+=1;
                avaibleExtraSlots--;
            }
            status.setMaxPlayers(max);

            updateTeam(status);
        }

        //updateWithTeamStatus(game.getTeam("Azul"), getOrSetItem(10, new ItemBuilder(Material.BLUE_WOOL).setDisplayName("&9Equipo Azul").build()));
        //updateWithTeamStatus(game.getTeam("Rojo"), getOrSetItem(12, new ItemBuilder(Material.RED_WOOL).setDisplayName("&cEquipo Rojo").build()));
        //updateWithTeamStatus(game.getTeam("Amarillo"), getOrSetItem(14, new ItemBuilder(Material.YELLOW_WOOL).setDisplayName("&eEquipo Amarillo").build()));
        //updateWithTeamStatus(game.getTeam("Verde"), getOrSetItem(16, new ItemBuilder(Material.GREEN_WOOL).setDisplayName("&2Equipo Verde").build()));
    }

    private ItemStack updateTeam(TeamStatus status) {
        GameTeam team = status.getTeam();
        int max = status.getMaxPlayers();
        ItemStack item = getOrSetItem(status.getSlot(), status.getSelectionItem());

        ItemMeta m = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(Utils.format(""));
        lore.add(Utils.format(team.getTeamColor() +"&lJugadores máximos&7:"));
        lore.add(Utils.format("&7> &b" + max));
        lore.add(Utils.format(""));
        lore.add(Utils.format(team.getTeamColor() + "&lJugadores (online y offline)&7:"));
        lore.add(Utils.format("&7> &b" + team.getPlayers().size()));
        lore.add(Utils.format(""));

        List<GamePlayer> members = new ArrayList<>(team.getPlayers()).stream().filter(ac -> ac.getPlayer() != null).collect(Collectors.toList());
        if (!members.isEmpty()) {
            lore.add(Utils.format(team.getTeamColor() + "&lIntegrantes&7:"));
            lore.add(Utils.format("&7> " + members.get(0).getName()));
            if (members.size() > 1) lore.add(Utils.format("&7> " + members.get(1).getName()));
            if (members.size() > 2) lore.add(Utils.format("&7> Y &b" + (members.size()-2) + " &7miembros más."));
            lore.add(Utils.format(""));
        }
        lore.add(Utils.format(isTeamFull(status) ? "&6Este equipo no permite más jugadores" : "&aRanura(s) disponible(s)."));

        m.setLore(lore);
        item.setItemMeta(m);

        return item;
    }

    private ItemStack getOrSetItem(int i, ItemStack s) {
        if (this.inv.getItem(i) == null || this.inv.getItem(i).getType() != s.getType()) {
            this.inv.setItem(i, s.clone());
            return s;
        } else {
            return inv.getItem(i);
        }
    }

    public void open(Player p) {
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 1.0F, 1.0F);
        if (!this.viewers.contains(p)) this.viewers.add(p);
    }

    public void handleClick(Player p, ItemStack click, InventoryClickEvent e) {
        e.setCancelled(true);

        GamePlayer gp = game.getPlayer(p.getUniqueId());
        if (click != null && gp != null) {
            Material m = click.getType();
            if (Tag.WOOL.isTagged(m)) {
                String cl = m.name().split("_")[0];

                GameTeam actual = gp.getTeam();
                GameTeam team = game.getTeamByMaterial(cl);
                TeamStatus status = getStatusByTeam(team);

                if (team == null || status == null) {
                    p.sendMessage(Utils.format("&cHa ocurrido un error al encontrar el material " + cl));
                    p.closeInventory();
                    return;
                }

                if (actual != null && actual.getTeamName().equalsIgnoreCase(team.getTeamName())) {
                    p.sendMessage(Utils.format("&cYa te encuentras en este equipo."));
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
                    p.closeInventory();
                    return;
                }

                if (isTeamFull(status)) {
                    p.sendMessage(Utils.format("&cEl equipo está completo... No puedes unirte, prueba más tarde cuando se unan más jugadores a la partida."));
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
                    p.closeInventory();
                } else {
                    if (actual != null) {
                        actual.removePlayer(gp);
                        gp.setTeam(null);
                    }

                    p.sendMessage(Utils.format("&aTe has unido al equipo " + team.getFormattedTeamName()));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 10.0F, 100.0F);

                    gp.setTeam(team);
                    team.join(gp);
                    p.teleport(team.getCageSpawn());
                }
            } else if (m == Material.BARRIER) {
                GameTeam actual = gp.getTeam();
                if (actual != null) {
                    p.sendMessage(Utils.format("&aHas abandonado tu equipo actual."));
                    p.sendMessage(Utils.format("&aPodrás elegir un equipo sin problemas."));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 10.0F, 100.0F);

                    p.getInventory().setHelmet(new ItemStack(Material.WHITE_WOOL));

                    gp.setTeam(null);
                    actual.removePlayer(gp);
                    p.teleport(Regions.getInstance().MAIN_SPAWNPOINT);
                } else {
                    p.sendMessage(Utils.format("&cNo te encuentras en ningún equipo."));
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
                    p.closeInventory();
                }
            }
        }
    }

    private TeamStatus getStatusByTeam(GameTeam team) {
        for (TeamStatus status : this.teams) {
            if (status.getTeam().getTeamName().equalsIgnoreCase(team.getTeamName())) {
                return status;
            }
        }
        return null;
    }

    private boolean isTeamFull(TeamStatus team) {
        return team.getTeam().getPlayers().size() >= team.getMaxPlayers();
    }

    public void removePlayer(Player p) {
        if (viewers.contains(p)) { viewers.remove(p); }
    }

    public static TeamSelectionGUI getInstance() {
        if (instance == null) instance = new TeamSelectionGUI(Main.getInstance().getGame());
        return instance;
    }

    public class TeamStatus {
        private GameTeam team;

        private int maxPlayers;
        private int slot;
        private ItemStack selectionItem;

        public TeamStatus(GameTeam team, ItemStack s, int sl) {
            this.selectionItem = s;
            this.team = team;
            this.maxPlayers = 4;
            this.slot = sl;
        }

        public ItemStack getSelectionItem() {
            return selectionItem;
        }

        public int getSlot() {
            return slot;
        }

        public GameTeam getTeam() {
            return team;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }
    }
}
