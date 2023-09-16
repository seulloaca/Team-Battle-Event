package tech.sebazcrc.teambattle.util;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class CleanGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random cRandom, int chunkX, int chunkZ, BiomeGrid biomes) {
        return createChunkData(world);
    }
}
