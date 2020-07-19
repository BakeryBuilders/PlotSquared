package com.plotsquared.core.queue;

import com.plotsquared.core.util.ChunkUtil;
import com.plotsquared.core.util.MainUtil;
import com.plotsquared.core.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.HashMap;

public class LocalChunk {
    private final BasicQueueCoordinator parent;
    private final int x;
    private final int z;

    private final BaseBlock[][] baseblocks;
    private final BiomeType[][] biomes;
    private final HashMap<BlockVector3, CompoundTag> tiles = new HashMap<>();

    public LocalChunk(BasicQueueCoordinator parent, int x, int z) {
        this.parent = parent;
        this.x = x;
        this.z = z;
        baseblocks = new BaseBlock[16][];
        biomes = new BiomeType[16][];
    }

    public BasicQueueCoordinator getParent() {
        return this.parent;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public BaseBlock[][] getBaseblocks() {
        return this.baseblocks;
    }

    public BiomeType[][] getBiomes() {
        return this.biomes;
    }

    public HashMap<BlockVector3, CompoundTag> getTiles() {
        return this.tiles;
    }

    public void setBiome(final int x, final int y, final int z, final BiomeType biomeType) {
        final int i = y >> 4;
        final int j = ChunkUtil.getJ(x, y, z);
        BiomeType[] array = this.biomes[i];
        if (array == null) {
            array = this.biomes[i] = new BiomeType[4096];
        }
        array[j] = biomeType;
    }

    @Override public int hashCode() {
        return MathMan.pair((short) x, (short) z);
    }

    public void setBlock(final int x, final int y, final int z, final BaseBlock baseBlock) {
        final int i = y >> 4;
        final int j = ChunkUtil.getJ(x, y, z);
        BaseBlock[] array = baseblocks[i];
        if (array == null) {
            array = (baseblocks[i] = new BaseBlock[4096]);
        }
        array[j] = baseBlock;
    }

    public void setTile(final int x, final int y, final int z, final CompoundTag tag) {
        tiles.put(BlockVector3.at(x, y, z), tag);
    }
}
