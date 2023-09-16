package tech.sebazcrc.teambattle;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import tech.sebazcrc.teambattle.board.ScoreHelper;
import tech.sebazcrc.teambattle.game.*;
import tech.sebazcrc.teambattle.game.enums.GamePhase;
import tech.sebazcrc.teambattle.game.enums.GameState;
import tech.sebazcrc.teambattle.game.gui.TeamSelectionGUI;
import tech.sebazcrc.teambattle.library.ItemBuilder;
import tech.sebazcrc.teambattle.util.Regions;
import tech.sebazcrc.teambattle.util.Utils;

import java.util.UUID;

public class TBListeners implements Listener {
    private static TBListeners lis;
    private Main instance;

    public TBListeners() {
        this.instance = Main.getInstance();
        this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Game game = instance.getGame();

        if (!ScoreHelper.hasScore(p)) ScoreHelper.createScore(p);

        if (game.getPlayer(p.getUniqueId()) == null) {
            GamePlayer gp = new GamePlayer(p.getUniqueId(), p.getName());
            game.addPlayer(gp);
            p.teleport(Regions.getInstance().MAIN_SPAWNPOINT);

            p.getInventory().clear();

            p.getInventory().setItem(0, new ItemBuilder(Material.NETHER_STAR).setDisplayName("&6Selector de Equipo").addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addItemFlag(ItemFlag.HIDE_ENCHANTS).build());
            p.getInventory().setHelmet(new ItemStack(Material.WHITE_WOOL));

            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
        }

        ScoreHelper helper = ScoreHelper.getByPlayer(p);
        Scoreboard board = helper.getScoreboard();

        if (game.isState(GameState.WAITING)) {
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
        }

        p.setScoreboard(board);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        TeamSelectionGUI.getInstance().removePlayer(e.getPlayer());

        if (instance.getGame().isState(GameState.PLAYING)) {
            GamePlayer gp = Main.getInstance().getGame().getPlayer(e.getPlayer().getUniqueId());
            if (gp != null) gp.onQuit(e.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (instance.getGame().isState(GameState.WAITING)) {
            ItemStack s = e.getItem();
            if (s != null && s.getType() == Material.NETHER_STAR) {
                TeamSelectionGUI.getInstance().open(p);
            }

            e.setCancelled(true);
            return;
        }

        if (instance.getGame().getPhase() == GamePhase.PVE && (e.getItem() != null && e.getItem().getType().name().toLowerCase().contains("boat"))) {
            p.sendMessage(Utils.format("&cPor prevención de bugs, no se permite usar barcos en la fase de PvE."));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            if (e.getView().getTitle().equalsIgnoreCase(Utils.format("&8Elegir equipo"))) {
                TeamSelectionGUI.getInstance().handleClick(p, e.getCurrentItem(), e);
            }
            if (instance.getGame().isState(GameState.WAITING)) {
                e.setCancelled(true);
            } else {
                ItemStack item;
                if (e.getAction().name().contains("HOTBAR")) {
                    item = e.getView().getBottomInventory().getItem(e.getHotbarButton());
                } else {
                    item = e.getCurrentItem();
                }

                InventoryAction action = e.getAction();
                InventoryType type = e.getView().getTopInventory().getType();

                if (isMagicWool(item)) {
                    boolean container = type == InventoryType.CHEST || type == InventoryType.ANVIL || type == InventoryType.BARREL || type == InventoryType.CARTOGRAPHY || type == InventoryType.DISPENSER || type == InventoryType.DROPPER || type == InventoryType.ENDER_CHEST || type == InventoryType.HOPPER || type == InventoryType.FURNACE || type == InventoryType.BLAST_FURNACE || type == InventoryType.SMOKER || type == InventoryType.STONECUTTER || type == InventoryType.SHULKER_BOX;

                    if (action == InventoryAction.PICKUP_SOME || action == InventoryAction.PICKUP_HALF || action == InventoryAction.PICKUP_ONE || action == InventoryAction.PLACE_ONE) {
                        p.sendMessage(Utils.format("&cNo puedes separar la cantidad de este objeto."));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
                        e.setCancelled(true);

                        if (action == InventoryAction.PLACE_ONE) p.closeInventory();
                    } else if (container) {
                        p.sendMessage(Utils.format("&cNo puedes mover este objeto a otro inventario o hacer un shift-click (mueve el objeto normalmente con el cursor)."));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
                        e.setCancelled(true);
                    }

                    if (e.isCancelled()) {
                        Bukkit.getScheduler().runTaskLater(instance, p::updateInventory, 1L);
                    }
                } else if (isMagicWool(e.getCursor())) {
                    if (action == InventoryAction.PLACE_ONE) {
                        p.sendMessage(Utils.format("&cNo puedes separar la cantidad de este objeto."));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);

                        e.setCancelled(true);

                        p.closeInventory();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            ItemStack s = e.getOldCursor();
            if (isMagicWool(s)) {
                e.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), p::updateInventory, 1L);
            }
        }
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent e) {
        if (isMagicWool(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        Game game = instance.getGame();

        GamePlayer gp = game.getPlayer(p.getUniqueId());

        if (gp == null) return;

        if (gp.getTeam() != null) {
            p.sendActionBar(Utils.format("&eUtiliza el comando &7/tc <mensaje> &epara hablar con tu Team."));
            Bukkit.broadcastMessage(Utils.format(gp.getTeam().formatChatMessage(p.getName(), e.getMessage())));
        } else {
            Bukkit.broadcastMessage(Utils.format("&8[" + "&7&lNinguno" + "&r&8] &e" + p.getName() + " &7> &f" + e.getMessage()));
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevel(FoodLevelChangeEvent e) {
        if (instance.getGame().isState(GameState.WAITING)) {
            e.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onBoat(VehicleCreateEvent e) {
        if (e.getVehicle() instanceof Boat && instance.getGame().getPhase() == GamePhase.PVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        TeamSelectionGUI.getInstance().removePlayer(p);

        Game game = instance.getGame();

        GamePlayer gp = game.getPlayer(p.getUniqueId());

        if (gp == null) return;

        if (e.getTo().getWorld().getName().equalsIgnoreCase("world")) {
            if (gp.getTeam() == null) {
                e.setTo(Regions.getInstance().MAIN_SPAWNPOINT);
            } else {
                e.setTo(gp.getTeam().getCageSpawn());
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (isMagicWool(e.getItemDrop().getItemStack()) || instance.getGame().isState(GameState.WAITING)) {
            p.sendMessage(Utils.format("&cNo puedes lanzar este objeto."));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent e) {
        UUID id = e.getUniqueId();

        if ((instance.getGame().getPlayer(id) == null && instance.getGame().isState(GameState.PLAYING)) || instance.getGame().isState(GameState.ENDED)) {
            e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            e.setKickMessage(Utils.format("&c¡Esta partida ya comenzó / acabó!"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        Game game = instance.getGame();
        GamePlayer gp = game.getPlayer(p.getUniqueId());

        if (gp == null) return;
        if (game.isState(GameState.WAITING)) {
            e.setCancelled(true);
            return;
        }

        for (GameTeam t : game.getTeams()) {
            if (t.getCageCuboid().containsLocation(b.getLocation())) {
                p.sendMessage(Utils.format("&cNo puedes romper bloques aquí."));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent e) {
        e.getItems().removeIf(item -> Tag.WOOL.isTagged(item.getItemStack().getType()));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        Game game = instance.getGame();
        GamePlayer gp = game.getPlayer(p.getUniqueId());

        if (game.isState(GameState.WAITING)) {
            e.setCancelled(true);
            return;
        }

        boolean cancel = e.isCancelled();

        if (gp == null) return;

        for (GameTeam t : game.getTeams()) {
            if (t.getCageCuboid().containsLocation(b.getLocation())) {
                p.sendMessage(Utils.format("&cNo puedes colocar bloques aquí."));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 10.0F, 0.5F);
                cancel = true;
                break;
            }
        }

        e.setCancelled(cancel);

        if (!cancel) {
            ItemStack s = p.getInventory().getItem(e.getHand());
            if (isMagicWool(s)) {
                s.setAmount(64);
            }
        }
    }

    @EventHandler
    public void onSmelt(FurnaceBurnEvent e) {
        if (isMagicWool(e.getFuel())) {
            e.setBurning(false);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (p.getGameMode() == GameMode.SPECTATOR) e.setCancelled(true);
            if (instance.getGame().isState(GameState.WAITING)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageBE(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        if (instance.getGame().isState(GameState.WAITING)) return;

        Player p = (Player) e.getEntity();

        Game game = instance.getGame();
        GamePlayer gp = game.getPlayer(p.getUniqueId());

        if (gp == null) return;

        Player d = null;
        GamePlayer dp = null;
        boolean projectile = e.getDamager() instanceof Projectile;

        if (e.getDamager() instanceof Player) {
            d = (Player) e.getDamager();
            dp = game.getPlayer(d.getUniqueId());

        } else if (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player) {
            d = (Player) ((Projectile) e.getDamager()).getShooter();
            dp = game.getPlayer(d.getUniqueId());
        }
        //Bukkit.broadcastMessage("Player: " + (p == null ? "null" : p.getName()));
        //Bukkit.broadcastMessage("Damager: " + (d == null ? "null" : d.getName()));
        //Bukkit.broadcastMessage("Cause: " + e.getCause().name().toLowerCase());

        if (d != null && dp != null) {
            if (dp.getTeam().getTeamName().equalsIgnoreCase(gp.getTeam().getTeamName())){
                e.setCancelled(true);
            } else{
                e.setCancelled(false);
                d.sendActionBar(Utils.format("&7" + (projectile ? "\uD83C\uDFF9" : "⚔") + " " + gp.getTeam().getTeamColor() + gp.getName() + " &7" + (Math.round(Math.floor(p.getHealth() - e.getFinalDamage()))/2) + " &c❤"));
                dp.onDamagePlayer(p, gp);
            }
        }
    }

    @EventHandler
    public void onProjectileCollide(ProjectileCollideEvent e) {
        if (e.getEntity().getShooter() instanceof Player && e.getCollidedWith() instanceof Player) {
            Player p = (Player) e.getCollidedWith();
            Player d = (Player) e.getEntity().getShooter();

            //Bukkit.broadcastMessage("Player: " + p.getName());
            //Bukkit.broadcastMessage("Damager: " + p.getName());

            GamePlayer gp = instance.getGame().getPlayer(p.getUniqueId());
            GamePlayer gd = instance.getGame().getPlayer(d.getUniqueId());

            if (gp != null && gd != null) {
                //Bukkit.broadcastMessage("Team 1: " + gp.getTeam().getTeamName());
                //Bukkit.broadcastMessage("Team 2: " + gp.getTeam().getTeamName());
                if (gd.getTeam().getTeamName().equalsIgnoreCase(gp.getTeam().getTeamName())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        GamePlayer gp = instance.getGame().getPlayer(p.getUniqueId());

        gp.increaseDeathCount(p.getLastDamageCause());

        boolean keep = gp.getDeathCount() < 3;
        e.setKeepLevel(keep);
        e.setKeepInventory(keep);
        if (keep) e.getDrops().clear();
        if (!keep) {
            try {
                for (ItemStack s : e.getDrops()) {
                    if (s == null) return;
                    if (Utils.cantBeDropped(s.getType())) {
                        e.getDrops().remove(s);
                    }
                }
            } catch (Exception x) {}
        }
        if (keep) {
            p.sendTitle(Utils.format("&c&l¡HAS MUERTO!"), Utils.format("&e" + (3-(gp.getDeathCount())) + " vidas restantes"));
        } else {
            p.sendTitle(Utils.format("&c&l¡HAS MUERTO!"), Utils.format("&e¡No te quedan vidas!"));
        }
        e.setDeathMessage(null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        GamePlayer gp = instance.getGame().getPlayer(p.getUniqueId());

        if (instance.getGame().isState(GameState.WAITING)) {
            e.setRespawnLocation(gp.getTeam() == null ? Regions.getInstance().MAIN_SPAWNPOINT : gp.getTeam().getCageSpawn());
        } else {
            e.setRespawnLocation(gp.getTeam().getSpawnPoint());
        }

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 60.0F, 0.2F);
                p.playSound(p.getLocation(), Sound.ENTITY_SPIDER_DEATH, SoundCategory.AMBIENT, 40.0F, 1.0F);
            }
        }, 3L);
    }

    public static TBListeners getInstance() {
        if (lis == null) lis = new TBListeners();
        return lis;
    }

    public boolean isMagicWool(ItemStack s) {
        if (s == null) return false;
        return Tag.WOOL.isTagged(s.getType()) && s.hasItemMeta() && s.getItemMeta().hasCustomModelData() && s.getItemMeta().getCustomModelData() == 1;
    }
}
