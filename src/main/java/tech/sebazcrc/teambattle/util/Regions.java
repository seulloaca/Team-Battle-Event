package tech.sebazcrc.teambattle.util;

import org.bukkit.Bukkit;
import tech.sebazcrc.teambattle.library.Cuboid;
import org.bukkit.Location;

public class Regions {

    private static Regions instance;

    public Location BLUE_TEAM_CAGE;
    public Location RED_TEAM_CAGE;
    public Location YELLOW_TEAM_CAGE;
    public Location GREEN_TEAM_CAGE;

    public Cuboid BLUE_TEAM_SPAWN;
    public Cuboid RED_TEAM_SPAWN;
    public Cuboid YELLOW_TEAM_SPAWN;
    public Cuboid GREEN_TEAM_SPAWN;

    public Location BLUE_TEAM_SPAWNPOINT;
    public Location RED_TEAM_SPAWNPOINT;
    public Location YELLOW_TEAM_SPAWNPOINT;
    public Location GREEN_TEAM_SPAWNPOINT;
    public Location MAIN_SPAWNPOINT;

    public Cuboid BARRIERS_AREA;
    public Cuboid LOBBY_AREA;

    public Regions() {

        BLUE_TEAM_CAGE = build(-151.544, 97, 319.553, -0.1F, 2.9F);
        RED_TEAM_CAGE = build(-151.549, 97, 337.452, 179.8F, 4.7F);
        YELLOW_TEAM_CAGE = build(-160.491, 97, 328.481, -89.7F, 2.6F);
        GREEN_TEAM_CAGE = build(-142.484, 97, 328.468, 90.6F, 1.4F);

        BLUE_TEAM_SPAWN = new Cuboid(build(-158, 66, 222), build(-146, 77, 210));
        RED_TEAM_SPAWN = new Cuboid(build(-158, 64, 446), build(-146, 76, 434));
        YELLOW_TEAM_SPAWN = new Cuboid(build(-258, 63, 334), build(-270, 75, 322));
        GREEN_TEAM_SPAWN = new Cuboid(build(-34, 65, 334), build(-46, 77, 322));

        BLUE_TEAM_SPAWNPOINT = build(-151.542, 69, 216.401, 0, 0);
        RED_TEAM_SPAWNPOINT = build(-151.501, 67, 440.529, -180, 0);
        YELLOW_TEAM_SPAWNPOINT = build(-263.528, 66, 328.500, -90, 0);
        GREEN_TEAM_SPAWNPOINT = build(-39.488, 68, 328.473, 90, 0);

        MAIN_SPAWNPOINT = build(-151.486, 97, 328.446, 179.5F, 4.2F);

        BARRIERS_AREA = new Cuboid(build(-81, 0, 399), build(-224, 134, 257));
        LOBBY_AREA = new Cuboid(build(-125, 91, 356), build(-178, 110, 304));

        //BLUE_TEAM_CAGE = build(-128.525, 132.10794, -105.547, 0.3F, 2.0F);
        //RED_TEAM_CAGE = build(-126.502, 132.10794, -38.376, -179.9F, 1.6F);

        //BLUE_TEAM_SPAWN = new Cuboid(build(-133, 87, -102), build(-123, 94, -110));
        //RED_TEAM_SPAWN = new Cuboid(build(-131, 80, -44), build(-123, 87, -36));

        //BLUE_TEAM_SPAWNPOINT = build(-128.466, 89, -105.550, 88.3F, 4.6F);
        //RED_TEAM_SPAWNPOINT = build(-126.492, 82, -39.482, 90.2F, 4.1F);
    }

    private Location build(double x, double y, double z) {
        return new Location(Bukkit.getWorld("teambattle"), x, y, z);
    }

    private Location build(double x, double y, double z, float yaw, float pitch) {
        return new Location(Bukkit.getWorld("teambattle"), x, y, z, yaw, pitch);
    }

    private Location build(int x, int y, int z) {
        return new Location(Bukkit.getWorld("teambattle"), x, y, z);
    }

    private Location build(int x, int y, int z, float yaw, float pitch) {
        return new Location(Bukkit.getWorld("teambattle"), x, y, z, yaw, pitch);
    }

    public static Regions getInstance() {
        if (instance == null) instance = new Regions();
        return instance;
    }
}
