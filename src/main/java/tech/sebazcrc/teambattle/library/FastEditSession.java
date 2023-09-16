package tech.sebazcrc.teambattle.library;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class FastEditSession {

    private final org.bukkit.World bukkitWorld;
    private final World world;
    private final HashMap<BlockPosition, IBlockData> modified = new HashMap<>();

    public FastEditSession(org.bukkit.World bukkitWorld, World world) {
        this.bukkitWorld = bukkitWorld;
        this.world = world;
    }

    public void setBlock(int x, int y, int z, Material material) {
        modified.put(new BlockPosition(x, y, z), CraftMagicNumbers.getBlock(material).getBlockData());
    }

    public Material getBlock(int x, int y, int z) {
        IBlockData data = modified.get(new BlockPosition(x, y, z));
        if (data != null)
            return CraftMagicNumbers.getMaterial(data).getItemType();
        return new Location(bukkitWorld, x, y, z).getBlock().getType();
    }

    public void update() {

        //modify blocks
        HashSet<Chunk> chunks = new HashSet<>();
        for (Map.Entry<BlockPosition, IBlockData> entry : modified.entrySet()) {
            Chunk chunk = world.getChunkProvider().getChunkAt(entry.getKey().getX() >> 4, entry.getKey().getZ() >> 4, true);
            chunks.add(chunk);
            chunk.setType(entry.getKey(), entry.getValue(), false);
        }

        //update lights
        LightEngine engine = world.getChunkProvider().getLightEngine();
        for (BlockPosition pos : modified.keySet()) {
            engine.a(pos);
        }

        //unload & load chunk data
        for (Chunk chunk : chunks) {
            PacketPlayOutUnloadChunk unload = new PacketPlayOutUnloadChunk(chunk.getPos().x, chunk.getPos().z);

            PacketPlayOutMapChunk load = new PacketPlayOutMapChunk(chunk, 65535);
            PacketPlayOutLightUpdate light = new PacketPlayOutLightUpdate(chunk.getPos(), engine, true);

            for (Player p : Bukkit.getOnlinePlayers()) {
                EntityPlayer ep = ((CraftPlayer) p).getHandle();
                int dist = Bukkit.getViewDistance() + 1;
                int chunkX = ep.chunkX;
                int chunkZ = ep.chunkZ;
                if (chunk.getPos().x < chunkX - dist ||
                        chunk.getPos().x > chunkX + dist ||
                        chunk.getPos().z < chunkZ - dist ||
                        chunk.getPos().z > chunkZ + dist) continue;
                ep.playerConnection.sendPacket(unload);
                ep.playerConnection.sendPacket(load);
                ep.playerConnection.sendPacket(light);
            }
        }
    }
}