package tech.sebazcrc.teambattle.game;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import tech.sebazcrc.teambattle.Main;
import tech.sebazcrc.teambattle.game.enums.FightStatus;
import tech.sebazcrc.teambattle.game.enums.GameState;
import tech.sebazcrc.teambattle.game.player.KillAssist;
import tech.sebazcrc.teambattle.library.XSound;
import tech.sebazcrc.teambattle.util.Utils;

import java.util.*;

public class GamePlayer {

    private UUID id;
    private String name;

    private boolean shouldSetSpectatorMode;

    private GameTeam team;

    private int fightRemoval;

    private GamePlayer killer;
    private int killerRemoval;

    private int deathCount;
    private int kills;
    private int assists;

    private List<KillAssist> killAssists;
    private List<String> pendingMessages;
    private int afkTime;

    public GamePlayer(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.shouldSetSpectatorMode = false;

        this.team = null;

        this.fightRemoval = 0;

        this.killer = null;
        this.killerRemoval = 20;

        this.deathCount = 0;
        this.kills = 0;
        this.assists = 0;
        this.afkTime = -1;

        this.killAssists = new ArrayList<>();
        this.pendingMessages = new ArrayList<>();
    }

    public void tickPlayer(Game game) {
        Player p = getPlayer();

        if (!game.isState(GameState.WAITING)) {
            // ASSIST TICK
            if (!this.killAssists.isEmpty()) this.killAssists.removeIf(KillAssist::tick);

            // KILLER TICK
            if (this.killerRemoval <= 0) {
                this.killer = null;
            } else if (this.killer != null) {
                this.killerRemoval--;
            }

            if (this.fightRemoval > 0) this.fightRemoval--;

            if (p != null && !Main.getInstance().FINAL_BATTLE) {
                if (isSpectator()) {
                    if (!p.isOp() && p.getGameMode() != GameMode.SPECTATOR) {
                        p.setGameMode(GameMode.SPECTATOR);
                    } else if (p.getGameMode() == GameMode.SPECTATOR && !p.getAllowFlight()) {
                        p.setAllowFlight(true);
                        p.setFlying(true);
                    }
                }
            }

            if (this.afkTime >= 2) {
                Main.getInstance().getGame().searchForEnd();
                this.afkTime = -1;
            }

            if (this.afkTime > -1)
                this.afkTime++;
        } else {
            if (p != null && getTeam() == null && p.getOpenInventory().getTopInventory().getType() != InventoryType.CHEST) {
                p.sendTitle(Utils.format("&c&l¡SIN EQUIPO!"), Utils.format("&7Selecciona uno en el menú"), 0, 30, 0);
            }
        }
    }

    public void onDamagePlayer(Player to, GamePlayer gp) {
        gp.onDamageByPlayer(to, this);
        this.fightRemoval = 15;
    }

    public void onDamageByPlayer(Player p, GamePlayer damager) {
        if (killer != null) { // Si alguien había atacado previamente, agrega su asistencia.
            this.killAssists.add(new KillAssist(this.killer));
        }
        setKiller(damager); // Ahora el killer sería este jugador

        KillAssist assist = getAssistance(damager);
        if (assist != null) { // Si este fue asistente elimina su asistencia.
            this.killAssists.remove(assist);
        }

        this.fightRemoval = 15;
    }

    public FightStatus getStatus() {
        return (this.fightRemoval <= 0) ? FightStatus.IDLING : FightStatus.FIGHTING;
    }

    public int getFightRemoval() {
        return fightRemoval;
    }

    private void onDeath(boolean finalKill, EntityDamageEvent cause, String reason) {
        Player p = getPlayer();
        int lives = getLives();

        if (getKiller() != null) {
            getKiller().increaseKills();
        }

        if (finalKill) {
            this.shouldSetSpectatorMode = true;
            if (p != null) {
                p.setGameMode(GameMode.SPECTATOR);
                this.shouldSetSpectatorMode = false;
            }

            sendMessage("&c&l¡HAS MUERTO! &7Perdiste todas tus vidas...", true);

            Bukkit.broadcastMessage(Utils.format("&7☠ &e¡" + getFormatName() + " &e" + getKillMessage(reason, cause) + "!. &c&l¡MUERTE FINAL!"));
            for (GamePlayer gp : Main.getInstance().getGame().getPlayers()) {
                gp.playSound(XSound.ENTITY_WITHER_DEATH, 10.0F, 1.0F);
            }

            Main.getInstance().getGame().searchForEnd();
        } else {
            Bukkit.broadcastMessage(Utils.format("&7☠ &e¡" + getFormatName() + " &e" + getKillMessage(reason, cause) + "!. " + lives + " &c❤ &erestante" + (lives == 1 ? "." : "s.")));
        }

        if (!this.killAssists.isEmpty()) {
            for (KillAssist assist : this.killAssists) {
                assist.getAssist().sendMessage("&7Has contribuido en el asesinato de " + getFormatName(), true);
                assist.getAssist().increaseAssists();
            }
            this.killAssists.clear();
        }

        if (p.getGameMode() == GameMode.SPECTATOR) {
            p.setAllowFlight(true);
            p.setFlying(true);
        }

        clearKillerData();
        clearAssistersData();
    }

    public void onQuit(Player p) {
        if (!isSpectator() && p.getGameMode() != GameMode.SPECTATOR) {
            if (getStatus() == FightStatus.FIGHTING) {
                //Bukkit.broadcastMessage(Utils.format("&c☠ &e¡" + getFormatName() + " &eha sido eliminato por &7Combat Log&e!"));
                this.deathCount = 3;
                dropInventory(p);
                onDeath(true, p.getLastDamageCause(), "Combat Log");
                return;
            }
            this.afkTime = 0;
        }
    }

    private void dropInventory(Player p) {
        List<ItemStack> drops = new ArrayList<>();

        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null && s.getType() != Material.AIR) {
                if (!Utils.cantBeDropped(s.getType())) {
                    drops.add(s);
                }
            }
        }

        for (ItemStack s : p.getInventory().getArmorContents()) {
            if (s != null && s.getType() != Material.AIR) {
                if (!Utils.cantBeDropped(s.getType())) {
                    drops.add(s);
                }
            }
        }

        for (ItemStack s : drops) {
            if (s != null && s.getType() != Material.AIR) p.getWorld().dropItem(p.getLocation().clone().add(0.5, 0, 0.5), s);
        }
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
    }

    private String getKillMessage(String reason, EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();

        String m = "";
        boolean def = false;

        if (reason == null) {
            switch (cause) {
                case SUFFOCATION:
                    m = "se ha sofocado";
                    break;
                case FALL:
                    if (getKiller() == null) {
                        m = "ha caído desde muy alto";
                    } else {
                        m = "fue empujado desde lo alto por " + getKiller().getFormatName();
                    }
                    break;
                case FIRE:
                case FIRE_TICK:
                case LAVA:
                    m = "se ha convertido en cenizas";
                    break;
                case DROWNING:
                    m = "se ahogó";
                    break;
                case BLOCK_EXPLOSION:
                    m = "explotó";
                    break;
                case VOID:
                    if (getKiller() == null) {
                        m = "cayó al vacío";
                    } else {
                        m = "fue empujado al vacío por " + getKiller().getFormatName();
                    }
                    break;
                case LIGHTNING:
                    m = "fue electrizado";
                    break;
                case POISON:
                    m = "fue envenenado";
                    break;
                case WITHER:
                    m = "sufrió la ira de &0Wither&e";
                    break;
                case FALLING_BLOCK:
                    m = "le cayó un bloque en la cabeza";
                    break;
                case ENTITY_ATTACK:
                case ENTITY_SWEEP_ATTACK:
                case PROJECTILE:
                case ENTITY_EXPLOSION:
                    m = "fue " + (cause == EntityDamageEvent.DamageCause.PROJECTILE ? "disparado" : (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ? "explotado" : "asesinado")) + " por ";
                    if (e instanceof EntityDamageByEntityEvent) {
                        if (((EntityDamageByEntityEvent) e).getDamager() instanceof Player) {
                            m = m + getKiller().getFormatName();
                        } else if (((EntityDamageByEntityEvent) e).getDamager() instanceof Projectile) {
                            if (((Projectile) ((EntityDamageByEntityEvent) e).getDamager()).getShooter() instanceof Player) {
                                m = m + getKiller().getFormatName();
                            } else if (((Projectile) ((EntityDamageByEntityEvent) e).getDamager()).getShooter() instanceof Entity) {
                                m = m + ((Entity) ((Projectile) ((EntityDamageByEntityEvent) e).getDamager()).getShooter()).getName();
                            } else {
                                m = m + "desconocido";
                            }
                        } else {
                            m = m + (((EntityDamageByEntityEvent) e).getDamager().getName());
                            if (getKiller() != null) {
                                int ri = new SplittableRandom().nextInt(3);
                                String rmsg = "";

                                if (((EntityDamageByEntityEvent) e).getDamager() instanceof EnderCrystal) {
                                    rmsg = "colocado por";
                                } else if (((EntityDamageByEntityEvent) e).getDamager() instanceof TNTPrimed) {
                                    rmsg = "explotada por";
                                } else {
                                    if (ri == 0) {
                                        rmsg = "huyendo de";
                                    } else if (ri == 1) {
                                        rmsg = "escapando de";
                                    } else {
                                        rmsg = "mienstras intentaba escapar de";
                                    }
                                }

                                m = m + " " + rmsg + " " + getKiller().getFormatName();
                            }
                        }

                    } else {
                        m = m + (getKiller() == null ? "desconocido" : getKiller().getFormatName());
                    }
                    break;
                default:
                    m = "ha muerto";
                    def = true;
                    break;
            }
        } else {
            m = "fue eliminado por &7" + reason;
        }

        if (reason != null && (cause != EntityDamageEvent.DamageCause.VOID && cause != EntityDamageEvent.DamageCause.FALL && cause != EntityDamageEvent.DamageCause.WITHER && cause != EntityDamageEvent.DamageCause.PROJECTILE && cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK && cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
            if (getKiller() != null) {
                m = m + " con ayuda de " + getKiller().getFormatName();
            }
        }

        return m + "&r&e";
    }

    public KillAssist getAssistance(GamePlayer to) {
        for (KillAssist assist : this.killAssists) {
            if (assist.getAssist().getUUID().toString().equalsIgnoreCase(to.getUUID().toString())) {
                return assist;
            }
        }
        return null;
    }

    public GamePlayer getKiller() {
        return killer;
    }

    public void setKiller(GamePlayer killer) {
        this.killer = killer;
        this.killerRemoval = 15;
    }

    public void clearKillerData() {
        this.killer = null;
        this.killerRemoval = 20;
    }

    public void clearAssistersData() {
        this.killAssists.clear();
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void increaseDeathCount(EntityDamageEvent cause) {
        this.deathCount++;
        onDeath(this.deathCount >= 3, cause, null);
    }

    public int getKills() {
        return kills;
    }

    public void increaseKills() {
        Player p = getPlayer();
        if (p != null) p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.AMBIENT, 3.0F, 2.0F);
        if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.AMBIENT, Float.MAX_VALUE, 1.0F);
        this.kills++;
    }

    public int getAssists() {
        return assists;
    }

    public void increaseAssists() {
        this.assists++;
    }

    private int getLives() {
        return 3-deathCount;
    }

    public boolean shouldSetSpectatorMode() {
        return shouldSetSpectatorMode;
    }

    public void setShouldSetSpectatorMode(boolean shouldSetSpectatorMode) {
        this.shouldSetSpectatorMode = shouldSetSpectatorMode;
    }

    public GameTeam getTeam() {
        return team;
    }

    public void setTeam(GameTeam team) {
        this.team = team;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.id);
    }

    public boolean isSpectator() {
        return deathCount >= 3 || getPlayer() == null;
    }

    public void addMessage(String key) {
        this.pendingMessages.add(Utils.format(key));
    }

    public void sendPendingMessage() {
        if (pendingMessages.isEmpty()) return;
        Player p = getPlayer();
        if (p != null) {
            for (String msg : pendingMessages) {
                p.sendMessage(msg);
            }
            p.sendMessage(Utils.format("&eTienes &b" + pendingMessages.size() + " &emensajes pendientes."));
            pendingMessages.clear();
        }
    }

    public void sendMessage(String s, boolean prefix) {
        Player p = getPlayer();

        if (p == null) {
            addMessage(s);
        } else {
            p.sendMessage(Utils.format(s));
        }
    }

    public void sendTitle(String t, String s) {
        sendTitle(t, s, 12, 20 * 8, 12);
    }

    public void sendTitle(String t, String s, int fadein, int stay, int fadeout) {
        Player p = getPlayer();
        if (p != null) {
            p.sendTitle(Utils.format(t), Utils.format(s), fadein, stay, fadeout);
        }
    }

    public void playSound(XSound sound) {
        playSound(sound, 10.0F, 1.0F);
    }

    public void playSound(XSound sound, Float volume, Float pitch) {
        Player p = getPlayer();
        if (p != null) p.playSound(p.getLocation(), sound.parseSound(), SoundCategory.AMBIENT, volume, pitch);
    }

    public UUID getUUID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFormatName() {
        return (team == null ? Utils.format("&7") + name : team.getTeamColor() + name + Utils.format("&r"));
    }

    public List<String> getPendingMessages() {
        return pendingMessages;
    }
}