package tech.sebazcrc.teambattle.board;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import tech.sebazcrc.teambattle.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ScoreHelper {

    private static HashMap<UUID, ScoreHelper> players = new HashMap<>();
    
    public static boolean hasScore(Player player) {
        return players.containsKey(player.getUniqueId());
    }
    
    public static ScoreHelper createScore(Player player) {
        return new ScoreHelper(player);
    }
    
    public static ScoreHelper getByPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public static ScoreHelper getByPlayer(UUID id) {
        return players.get(id);
    }

    public static ScoreHelper removeScore(Player player) {
        return players.remove(player.getUniqueId());
    }

    public static ScoreHelper registerScoreboard() {
        return new ScoreHelper();
    }
    
    private final Scoreboard scoreboard;
    private final Objective sidebar;

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    private ScoreHelper(Player player) {
        this();
        initializePlayer(player);
    }

    private ScoreHelper() {
        scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        sidebar = scoreboard.registerNewObjective("sidebar", "dummy", "Scoreboard");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        for(int i=1; i<=15; i++) {
            Team team = scoreboard.registerNewTeam("SLOT_" + i);
            team.addEntry(genEntry(i));
        }
    }

    private void initializePlayer(Player player) {
        player.setScoreboard(scoreboard);
        players.put(player.getUniqueId(), this);
    }

    public ScoreHelper setTitle(String title) {
        title = ChatColor.translateAlternateColorCodes('&', title);
        sidebar.setDisplayName(title.length()>32 ? title.substring(0, 32) : title);

        return this;
    }

    public String getTitle() {
        return sidebar.getDisplayName();
    }

    public void setSlot(int slot, String text) {
        Team team = scoreboard.getTeam("SLOT_" + slot);
        assert team != null;

        String entry = genEntry(slot);
        if(!scoreboard.getEntries().contains(entry)) {
            sidebar.getScore(entry).setScore(slot);
        }

        //text = ChatColor.translateAlternateColorCodes('&', text);
        // §
        String pre = getFirstSplit(text);
        boolean colored = pre.endsWith("&");

        if (colored) pre = pre.substring(0, pre.length()-1);

        String suf = getFirstSplit(ChatColor.getLastColors(Utils.format(pre)) + (colored ? "&" : "") + getSecondSplit(text));

        team.setPrefix(Utils.format(pre));
        team.setSuffix(Utils.format(suf));
    }

    public void removeSlot(int slot) {
        String entry = genEntry(slot);
        if(scoreboard.getEntries().contains(entry)) {
            scoreboard.resetScores(entry);
        }
    }

    public void setSlotsFromList(List<String> list) {
        while(list.size()>15) {
            list.remove(list.size()-1);
        }
        
        int slot = list.size();

        if(slot<15) {
            for(int i=(slot +1); i<=15; i++) {
                removeSlot(i);
            }
        }

        for(String line : list) {
            setSlot(slot, line);
            slot--;
        }
    }

    private String genEntry(int slot) {
        return ChatColor.values()[slot].toString();
    }

    private String getFirstSplit(String s) {
        return s.length()>16 ? s.substring(0, 16) : s;
    }

    private String getSecondSplit(String s) {
        if(s.length()>32) {
            s = s.substring(0, 32);
        }
        return s.length()>16 ? s.substring(16) : "";
    }
}