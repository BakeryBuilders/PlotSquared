////////////////////////////////////////////////////////////////////////////////////////////////////
// PlotSquared - A plot manager and world generator for the Bukkit API                             /
// Copyright (c) 2014 IntellectualSites/IntellectualCrafters                                       /
//                                                                                                 /
// This program is free software; you can redistribute it and/or modify                            /
// it under the terms of the GNU General Public License as published by                            /
// the Free Software Foundation; either version 3 of the License, or                               /
// (at your option) any later version.                                                             /
//                                                                                                 /
// This program is distributed in the hope that it will be useful,                                 /
// but WITHOUT ANY WARRANTY; without even the implied warranty of                                  /
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                   /
// GNU General Public License for more details.                                                    /
//                                                                                                 /
// You should have received a copy of the GNU General Public License                               /
// along with this program; if not, write to the Free Software Foundation,                         /
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA                               /
//                                                                                                 /
// You can contact us via: support@intellectualsites.com                                           /
////////////////////////////////////////////////////////////////////////////////////////////////////
package com.intellectualcrafters.plot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import com.intellectualcrafters.plot.PlotSquared;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.object.BlockLoc;
import com.intellectualcrafters.plot.object.ChunkLoc;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotBlock;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotManager;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.PlotSettings;
import com.intellectualcrafters.plot.object.PlotWorld;
import com.intellectualcrafters.plot.object.PseudoRandom;
import com.intellectualcrafters.plot.util.bukkit.BukkitUtil;
import com.intellectualcrafters.plot.util.bukkit.SendChunk;

/**
 * plot functions
 *
 * @author Citymonstret
 */
public class MainUtil {
    public final static HashMap<Plot, Integer> runners = new HashMap<>();
    public static boolean canSendChunk = false;
    public static ArrayList<String> runners_p = new ArrayList<>();
    static long state = 1;
    public static HashMap<String, PlotId> lastPlot = new HashMap<>();
    public static HashMap<String, Integer> worldBorder = new HashMap<>();

    public static ArrayList<PlotId> getMaxPlotSelectionIds(final String world, PlotId pos1, PlotId pos2) {
        
        final Plot plot1 = PlotSquared.getPlots(world).get(pos1);
        final Plot plot2 = PlotSquared.getPlots(world).get(pos2);
        
        if (plot1 != null) {
            pos1 = getBottomPlot(plot1).id;
        }
        
        if (plot2 != null) {
            pos2 = getTopPlot(plot2).id;
        }
        
        final ArrayList<PlotId> myplots = new ArrayList<>();
        for (int x = pos1.x; x <= pos2.x; x++) {
            for (int y = pos1.y; y <= pos2.y; y++) {
                myplots.add(new PlotId(x, y));
            }
        }
        return myplots;
    }

    /**
     * Get the number of plots for a player
     *
     * @param plr
     *
     * @return
     */
    public static int getPlayerPlotCount(final String world, final PlotPlayer plr) {
        final UUID uuid = plr.getUUID();
        int count = 0;
        for (final Plot plot : PlotSquared.getPlots(world).values()) {
            if (plot.hasOwner() && plot.owner.equals(uuid) && plot.countsTowardsMax) {
                count++;
            }
        }
        return count;
    }

    public static boolean teleportPlayer(final PlotPlayer player, final Location from, final Plot plot) {
        final Plot bot = MainUtil.getBottomPlot(plot);

        // TODO
        //      boolean result = PlotSquared.IMP.callPlayerTeleportToPlotEvent(player, from, plot);
        
        final boolean result = false;
        // TOOD ^ remove that

        if (!result) {
            final Location location = MainUtil.getPlotHome(bot.world, bot);
            if ((Settings.TELEPORT_DELAY == 0) || Permissions.hasPermission(player, "plots.teleport.delay.bypass")) {
                sendMessage(player, C.TELEPORTED_TO_PLOT);
                player.teleport(location);
                return true;
            }
            sendMessage(player, C.TELEPORT_IN_SECONDS, Settings.TELEPORT_DELAY + "");
            final String name = player.getName();
            TaskManager.TELEPORT_QUEUE.add(name);
            TaskManager.runTaskLater(new Runnable() {
                @Override
                public void run() {
                    if (!TaskManager.TELEPORT_QUEUE.contains(name)) {
                        sendMessage(player, C.TELEPORT_FAILED);
                        return;
                    }
                    TaskManager.TELEPORT_QUEUE.remove(name);
                    if (!player.isOnline()) {
                        return;
                    }
                    sendMessage(player, C.TELEPORTED_TO_PLOT);
                    player.teleport(location);
                }
            }, Settings.TELEPORT_DELAY * 20);
            return true;
        }
        return !result;
    }

    public static int getBorder(final String worldname) {
        if (worldBorder.containsKey(worldname)) {
            PlotSquared.getPlotWorld(worldname);
            return worldBorder.get(worldname) + 16;
        }
        return Integer.MAX_VALUE;
    }

    public static void setupBorder(final String world) {
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        if (!plotworld.WORLD_BORDER) {
            return;
        }
        if (!worldBorder.containsKey(world)) {
            worldBorder.put(world, 0);
        }
        for (final Plot plot : PlotSquared.getPlots(world).values()) {
            updateWorldBorder(plot);
        }
    }

    public static void update(final Location loc) {
        final String world = loc.getWorld();
        final ArrayList<ChunkLoc> chunks = new ArrayList<>();
        final int distance = BukkitUtil.getViewDistance();
        for (int cx = -distance; cx < distance; cx++) {
            for (int cz = -distance; cz < distance; cz++) {
                final ChunkLoc chunk = new ChunkLoc(cx, cz);
                chunks.add(chunk);
            }
        }
        BlockUpdateUtil.setBlockManager.update(world, chunks);
    }

    public static void createWorld(final String world, final String generator) {
    }

    public static PlotId parseId(final String arg) {
        try {
            final String[] split = arg.split(";");
            return new PlotId(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * direction 0 = north, 1 = south, etc:
     *
     * @param id
     * @param direction
     *
     * @return
     */
    public static PlotId getPlotIdRelative(final PlotId id, final int direction) {
        switch (direction) {
            case 0:
                return new PlotId(id.x, id.y - 1);
            case 1:
                return new PlotId(id.x + 1, id.y);
            case 2:
                return new PlotId(id.x, id.y + 1);
            case 3:
                return new PlotId(id.x - 1, id.y);
        }
        return id;
    }

    public static ArrayList<PlotId> getPlotSelectionIds(final PlotId pos1, final PlotId pos2) {
        final ArrayList<PlotId> myplots = new ArrayList<>();
        for (int x = pos1.x; x <= pos2.x; x++) {
            for (int y = pos1.y; y <= pos2.y; y++) {
                myplots.add(new PlotId(x, y));
            }
        }
        return myplots;
    }

    /**
     * Completely merges a set of plots<br> <b>(There are no checks to make sure you supply the correct
     * arguments)</b><br> - Misuse of this method can result in unusable plots<br> - the set of plots must belong to one
     * owner and be rectangular<br> - the plot array must be sorted in ascending order<br> - Road will be removed where
     * required<br> - changes will be saved to DB<br>
     *
     * @param world
     * @param plotIds
     *
     * @return boolean (success)
     */
    public static boolean mergePlots(final String world, final ArrayList<PlotId> plotIds, final boolean removeRoads) {
        if (plotIds.size() < 2) {
            return false;
        }
        final PlotId pos1 = plotIds.get(0);
        final PlotId pos2 = plotIds.get(plotIds.size() - 1);
        final PlotManager manager = PlotSquared.getPlotManager(world);
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);

        boolean result = EventUtil.manager.callMerge(world, getPlot(world, pos1), plotIds);
        if (!result) {
            return false;
        }

        manager.startPlotMerge(plotworld, plotIds);
        for (int x = pos1.x; x <= pos2.x; x++) {
            for (int y = pos1.y; y <= pos2.y; y++) {
                final boolean lx = x < pos2.x;
                final boolean ly = y < pos2.y;
                final PlotId id = new PlotId(x, y);
                final Plot plot = PlotSquared.getPlots(world).get(id);
                Plot plot2 = null;
                if (removeRoads) {
                    removeSign(plot);
                }
                if (lx) {
                    if (ly) {
                        if (!plot.settings.getMerged(1) || !plot.settings.getMerged(2)) {
                            if (removeRoads) {
                                manager.removeRoadSouthEast(plotworld, plot);
                            }
                        }
                    }
                    if (!plot.settings.getMerged(1)) {
                        plot2 = PlotSquared.getPlots(world).get(new PlotId(x + 1, y));
                        mergePlot(world, plot, plot2, removeRoads);
                        plot.settings.setMerged(1, true);
                        plot2.settings.setMerged(3, true);
                    }
                }
                if (ly) {
                    if (!plot.settings.getMerged(2)) {
                        plot2 = PlotSquared.getPlots(world).get(new PlotId(x, y + 1));
                        mergePlot(world, plot, plot2, removeRoads);
                        plot.settings.setMerged(2, true);
                        plot2.settings.setMerged(0, true);
                    }
                }
            }
        }
        for (int x = pos1.x; x <= pos2.x; x++) {
            for (int y = pos1.y; y <= pos2.y; y++) {
                final PlotId id = new PlotId(x, y);
                final Plot plot = PlotSquared.getPlots(world).get(id);
                DBFunc.setMerged(world, plot, plot.settings.getMerged());
            }
        }
        manager.finishPlotMerge(plotworld, plotIds);
        return true;
    }

    /**
     * Merges 2 plots Removes the road inbetween <br> - Assumes the first plot parameter is lower <br> - Assumes neither
     * are a Mega-plot <br> - Assumes plots are directly next to each other <br> - Saves to DB
     *
     * @param world
     * @param lesserPlot
     * @param greaterPlot
     */
    public static void mergePlot(final String world, final Plot lesserPlot, final Plot greaterPlot, final boolean removeRoads) {
        final PlotManager manager = PlotSquared.getPlotManager(world);
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        if (lesserPlot.id.x.equals(greaterPlot.id.x)) {
            if (!lesserPlot.settings.getMerged(2)) {
                lesserPlot.settings.setMerged(2, true);
                greaterPlot.settings.setMerged(0, true);
                if (removeRoads) {
                    manager.removeRoadSouth(plotworld, lesserPlot);
                }
            }
        } else {
            if (!lesserPlot.settings.getMerged(1)) {
                lesserPlot.settings.setMerged(1, true);
                greaterPlot.settings.setMerged(3, true);
                if (removeRoads) {
                    manager.removeRoadEast(plotworld, lesserPlot);
                }
            }
        }
    }

    public static void removeSign(final Plot p) {
        final String world = p.world;
        final PlotManager manager = PlotSquared.getPlotManager(world);
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        final Location loc = manager.getSignLoc(plotworld, p);
        BlockManager.setBlocks(world, new int[] { loc.getX() }, new int[] { loc.getY() }, new int[] { loc.getZ() }, new int[] { 0 }, new byte[] { 0 });
    }

    public static void setSign(String name, final Plot p) {
        if (name == null) {
            name = "unknown";
        }
        final PlotManager manager = PlotSquared.getPlotManager(p.world);
        final PlotWorld plotworld = PlotSquared.getPlotWorld(p.world);
        if (plotworld.ALLOW_SIGNS) {
        final Location loc = manager.getSignLoc(plotworld, p);
            final String id = p.id.x + ";" + p.id.y;
            final String[] lines = new String[] { C.OWNER_SIGN_LINE_1.translated().replaceAll("%id%", id), C.OWNER_SIGN_LINE_2.translated().replaceAll("%id%", id).replaceAll("%plr%", name), C.OWNER_SIGN_LINE_3.translated().replaceAll("%id%", id).replaceAll("%plr%", name), C.OWNER_SIGN_LINE_4.translated().replaceAll("%id%", id).replaceAll("%plr%", name) };
            BlockManager.setSign(p.world, loc.getX(), loc.getY(), loc.getZ(), lines);
        }
    }

    public static String getStringSized(final int max, final String string) {
        if (string.length() > max) {
            return string.substring(0, max);
        }
        return string;
    }

    public static void autoMerge(final String world, final Plot plot, final UUID uuid) {
        if (plot == null) {
            return;
        }
        if (plot.owner == null) {
            return;
        }
        if (!plot.owner.equals(uuid)) {
            return;
        }
        ArrayList<PlotId> plots;
        boolean merge = true;
        int count = 0;
        while (merge) {
            if (count > 16) {
                break;
            }
            count++;
            final PlotId bot = getBottomPlot(plot).id;
            final PlotId top = getTopPlot(plot).id;
            plots = getPlotSelectionIds(new PlotId(bot.x, bot.y - 1), new PlotId(top.x, top.y));
            if (ownsPlots(world, plots, uuid, 0)) {
                final boolean result = mergePlots(world, plots, true);
                if (result) {
                    merge = true;
                    continue;
                }
            }
            plots = getPlotSelectionIds(new PlotId(bot.x, bot.y), new PlotId(top.x + 1, top.y));
            if (ownsPlots(world, plots, uuid, 1)) {
                final boolean result = mergePlots(world, plots, true);
                if (result) {
                    merge = true;
                    continue;
                }
            }
            plots = getPlotSelectionIds(new PlotId(bot.x, bot.y), new PlotId(top.x, top.y + 1));
            if (ownsPlots(world, plots, uuid, 2)) {
                final boolean result = mergePlots(world, plots, true);
                if (result) {
                    merge = true;
                    continue;
                }
            }
            plots = getPlotSelectionIds(new PlotId(bot.x - 1, bot.y), new PlotId(top.x, top.y));
            if (ownsPlots(world, plots, uuid, 3)) {
                final boolean result = mergePlots(world, plots, true);
                if (result) {
                    merge = true;
                    continue;
                }
            }
            merge = false;
        }
        update(getPlotHome(world, plot));
    }

    private static boolean ownsPlots(final String world, final ArrayList<PlotId> plots, final UUID uuid, final int dir) {
        final PlotId id_min = plots.get(0);
        final PlotId id_max = plots.get(plots.size() - 1);
        for (final PlotId myid : plots) {
            final Plot myplot = PlotSquared.getPlots(world).get(myid);
            if ((myplot == null) || !myplot.hasOwner() || !(myplot.getOwner().equals(uuid))) {
                return false;
            }
            final PlotId top = getTopPlot(myplot).id;
            if (((top.x > id_max.x) && (dir != 1)) || ((top.y > id_max.y) && (dir != 2))) {
                return false;
            }
            final PlotId bot = getBottomPlot(myplot).id;
            if (((bot.x < id_min.x) && (dir != 3)) || ((bot.y < id_min.y) && (dir != 0))) {
                return false;
            }
        }
        return true;
    }

    public static void updateWorldBorder(final Plot plot) {
        if (!worldBorder.containsKey(plot.world)) {
            return;
        }
        final String world = plot.world;
        final PlotManager manager = PlotSquared.getPlotManager(world);
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        final Location bot = manager.getPlotBottomLocAbs(plotworld, plot.id);
        final Location top = manager.getPlotTopLocAbs(plotworld, plot.id);
        final int border = worldBorder.get(plot.world);
        final int botmax = Math.max(Math.abs(bot.getX()), Math.abs(bot.getZ()));
        final int topmax = Math.max(Math.abs(top.getX()), Math.abs(top.getZ()));
        final int max = Math.max(botmax, topmax);
        if (max > border) {
            worldBorder.put(plot.world, max);
        }
    }

    /**
     * Create a plot and notify the world border and plot merger
     */
    public static boolean createPlot(final UUID uuid, final Plot plot) {
        if (MainUtil.worldBorder.containsKey(plot.world)) {
            updateWorldBorder(plot);
        }
        final Plot p = createPlotAbs(uuid, plot);
        final PlotWorld plotworld = PlotSquared.getPlotWorld(plot.world);
        if (plotworld.AUTO_MERGE) {
            autoMerge(plot.world, p, uuid);
        }
        return true;
    }

    /**
     * Create a plot without notifying the merge function or world border manager
     */
    public static Plot createPlotAbs(final UUID uuid, final Plot plot) {
        final String w = plot.world;
        final Plot p = new Plot(plot.id, uuid, new ArrayList<UUID>(), new ArrayList<UUID>(), w);
        PlotSquared.updatePlot(p);
        DBFunc.createPlotAndSettings(p);
        return p;
    }

    public static String createId(final int x, final int z) {
        return x + ";" + z;
    }

    public static int square(final int x) {
        return x * x;
    }

    public static short[] getBlock(final String block) {
        if (block.contains(":")) {
            final String[] split = block.split(":");
            return new short[] { Short.parseShort(split[0]), Short.parseShort(split[1]) };
        }
        return new short[] { Short.parseShort(block), 0 };
    }

    /**
     * Clear a plot and associated sections: [sign, entities, border]
     *
     * @param requester
     * @param plot
     */
    public static boolean clearAsPlayer(final Plot plot, final boolean isDelete, final Runnable whenDone) {
        if (runners.containsKey(plot)) {
            return false;
        }
        ChunkManager.manager.clearAllEntities(plot);
        clear(plot.world, plot, isDelete, whenDone);
        removeSign(plot);
        return true;
    }

    public static void clear(final String world, final Plot plot, final boolean isDelete, final Runnable whenDone) {
        final PlotManager manager = PlotSquared.getPlotManager(world);
        final Location pos1 = MainUtil.getPlotBottomLoc(world, plot.id).add(1, 0, 1);
        final int prime = 31;
        int h = 1;
        h = (prime * h) + pos1.getX();
        h = (prime * h) + pos1.getZ();
        state = h;
        System.currentTimeMillis();
        final Location location = MainUtil.getPlotHomeDefault(plot);
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        runners.put(plot, 1);
        if (plotworld.TERRAIN != 0) {
            final Location pos2 = MainUtil.getPlotTopLoc(world, plot.id);
            ChunkManager.manager.regenerateRegion(pos1, pos2, new Runnable() {
                @Override
                public void run() {
                    runners.remove(plot);
                    TaskManager.runTask(whenDone);
                }
            });
            return;
        }
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                MainUtil.setBiome(world, plot, "FOREST");
                runners.remove(plot);
                TaskManager.runTask(whenDone);
                update(location);
            }
        };
        manager.clearPlot(plotworld, plot, isDelete, run);
    }

    public static void setCuboid(final String world, final Location pos1, final Location pos2, final PlotBlock[] blocks) {
        if (blocks.length == 1) {
            setSimpleCuboid(world, pos1, pos2, blocks[0]);
            return;
        }
        final int length = (pos2.getX() - pos1.getX()) * (pos2.getY() - pos1.getY()) * (pos2.getZ() - pos1.getZ());
        final int[] xl = new int[length];
        final int[] yl = new int[length];
        final int[] zl = new int[length];
        final int[] ids = new int[length];
        final byte[] data = new byte[length];
        int index = 0;
        for (int y = pos1.getY(); y < pos2.getY(); y++) {
            for (int x = pos1.getX(); x < pos2.getX(); x++) {
                for (int z = pos1.getZ(); z < pos2.getZ(); z++) {
                    final int i = PseudoRandom.random(blocks.length);
                    xl[index] = x;
                    yl[index] = y;
                    zl[index] = z;
                    final PlotBlock block = blocks[i];
                    ids[index] = block.id;
                    data[index] = block.data;
                    index++;
                }
            }
        }
        BlockManager.setBlocks(world, xl, yl, zl, ids, data);
    }

    public static void setSimpleCuboid(final String world, final Location pos1, final Location pos2, final PlotBlock newblock) {
        final int length = (pos2.getX() - pos1.getX()) * (pos2.getY() - pos1.getY()) * (pos2.getZ() - pos1.getZ());
        final int[] xl = new int[length];
        final int[] yl = new int[length];
        final int[] zl = new int[length];
        final int[] ids = new int[length];
        final byte[] data = new byte[length];
        int index = 0;
        for (int y = pos1.getY(); y < pos2.getY(); y++) {
            for (int x = pos1.getX(); x < pos2.getX(); x++) {
                for (int z = pos1.getZ(); z < pos2.getZ(); z++) {
                    xl[index] = x;
                    yl[index] = y;
                    zl[index] = z;
                    ids[index] = newblock.id;
                    data[index] = newblock.data;
                    index++;
                }
            }
        }
        BlockManager.setBlocks(world, xl, yl, zl, ids, data);
    }

    public static void setBiome(final String world, final Plot plot, final String biome) {
        final int bottomX = getPlotBottomLoc(world, plot.id).getX() + 1;
        final int topX = getPlotTopLoc(world, plot.id).getX();
        final int bottomZ = getPlotBottomLoc(world, plot.id).getZ() + 1;
        final int topZ = getPlotTopLoc(world, plot.id).getZ();
        BukkitUtil.setBiome(world, bottomX, bottomZ, topX, topZ, biome);
    }

    public static int getHeighestBlock(final String world, final int x, final int z) {
        final int result = BukkitUtil.getHeighestBlock(world, x, z);
        if (result == 0) {
            return 64;
        }
        return result;
        //        for (int i = 1; i < world.getMaxHeight(); i++) {
        //            id = world.getBlockAt(x, i, z).getTypeId();
        //            if (id == 0) {
        //                if (safe) {
        //                    return i;
        //                }
        //                safe = true;
        //            }
        //        }
        //        return 64;
    }

    /**
     * Get plot home
     *
     * @param w      World in which the plot is located
     * @param plotid Plot ID
     *
     * @return Home Location
     */
    public static Location getPlotHome(final String w, final PlotId plotid) {
        final Plot plot = getPlot(w, plotid);
        final BlockLoc home = plot.settings.getPosition();
        final Location bot = getPlotBottomLoc(w, plotid);
        final PlotManager manager = PlotSquared.getPlotManager(w);
        if ((home == null) || ((home.x == 0) && (home.z == 0))) {
            final Location top = getPlotTopLoc(w, plotid);
            final int x = ((top.getX() - bot.getX()) / 2) + bot.getX();
            final int z = ((top.getZ() - bot.getZ()) / 2) + bot.getZ();
            final int y = Math.max(getHeighestBlock(w, x, z), manager.getSignLoc(PlotSquared.getPlotWorld(w), plot).getY());
            return new Location(w, x, y, z);
        } else {
            final int y = Math.max(getHeighestBlock(w, home.x, home.z), home.y);
            return bot.add(home.x, y, home.z);
        }
    }

    /**
     * Retrieve the location of the default plot home position
     *
     * @param plot Plot
     *
     * @return the location
     */
    public static Location getPlotHomeDefault(final Plot plot) {
        final Location l = getPlotBottomLoc(plot.world, plot.getId()).subtract(0, 0, 0);
        l.setY(getHeighestBlock(plot.world, l.getX(), l.getZ()));
        return l;
    }

    /**
     * Get the plot home
     *
     * @param w    World
     * @param plot Plot Object
     *
     * @return Plot Home Location
     *
     * @see #getPlotHome(org.bukkit.World, com.intellectualcrafters.plot.object.PlotId)
     */
    public static Location getPlotHome(final String w, final Plot plot) {
        return getPlotHome(w, plot.id);
    }

    /**
     * Refresh the plot chunks
     *
     * @param world World in which the plot is located
     * @param plot  Plot Object
     */
    public static void refreshPlotChunks(final String world, final Plot plot) {
        final int bottomX = getPlotBottomLoc(world, plot.id).getX();
        final int topX = getPlotTopLoc(world, plot.id).getX();
        final int bottomZ = getPlotBottomLoc(world, plot.id).getZ();
        final int topZ = getPlotTopLoc(world, plot.id).getZ();
        final int minChunkX = (int) Math.floor((double) bottomX / 16);
        final int maxChunkX = (int) Math.floor((double) topX / 16);
        final int minChunkZ = (int) Math.floor((double) bottomZ / 16);
        final int maxChunkZ = (int) Math.floor((double) topZ / 16);
        final ArrayList<ChunkLoc> chunks = new ArrayList<>();
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (canSendChunk) {
                    final ChunkLoc chunk = new ChunkLoc(x, z);
                    chunks.add(chunk);
                } else {
                    BukkitUtil.refreshChunk(world, x, z);
                }
            }
        }
        try {
            SendChunk.sendChunk(world, chunks);
        } catch (final Throwable e) {
            canSendChunk = false;
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    BukkitUtil.refreshChunk(world, x, z);
                }
            }
        }
    }

    /**
     * Gets the top plot location of a plot (all plots are treated as small plots) - To get the top loc of a mega plot
     * use getPlotTopLoc(...)
     *
     * @param world
     * @param id
     *
     * @return
     */
    public static Location getPlotTopLocAbs(final String world, final PlotId id) {
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        final PlotManager manager = PlotSquared.getPlotManager(world);
        return manager.getPlotTopLocAbs(plotworld, id);
    }

    /**
     * Gets the bottom plot location of a plot (all plots are treated as small plots) - To get the top loc of a mega
     * plot use getPlotBottomLoc(...)
     *
     * @param world
     * @param id
     *
     * @return
     */
    public static Location getPlotBottomLocAbs(final String world, final PlotId id) {
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        final PlotManager manager = PlotSquared.getPlotManager(world);
        return manager.getPlotBottomLocAbs(plotworld, id);
    }

    /**
     * Obtains the width of a plot (x width)
     *
     * @param world
     * @param id
     *
     * @return
     */
    public static int getPlotWidth(final String world, final PlotId id) {
        return getPlotTopLoc(world, id).getX() - getPlotBottomLoc(world, id).getX();
    }

    /**
     * Gets the top loc of a plot (if mega, returns top loc of that mega plot) - If you would like each plot treated as
     * a small plot use getPlotTopLocAbs(...)
     *
     * @param world
     * @param id
     *
     * @return
     */
    public static Location getPlotTopLoc(final String world, PlotId id) {
        final Plot plot = PlotSquared.getPlots(world).get(id);
        if (plot != null) {
            id = getTopPlot(plot).id;
        }
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        final PlotManager manager = PlotSquared.getPlotManager(world);
        return manager.getPlotTopLocAbs(plotworld, id);
    }

    /**
     * Gets the bottom loc of a plot (if mega, returns bottom loc of that mega plot) - If you would like each plot
     * treated as a small plot use getPlotBottomLocAbs(...)
     *
     * @param world
     * @param id
     *
     * @return
     */
    public static Location getPlotBottomLoc(final String world, PlotId id) {
        final Plot plot = PlotSquared.getPlots(world).get(id);
        if (plot != null) {
            id = getBottomPlot(plot).id;
        }
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        final PlotManager manager = PlotSquared.getPlotManager(world);
        return manager.getPlotBottomLocAbs(plotworld, id);
    }

    public static boolean isUnowned(final String world, final PlotId pos1, final PlotId pos2) {
        for (int x = pos1.x; x <= pos2.x; x++) {
            for (int y = pos1.y; y <= pos2.y; y++) {
                final PlotId id = new PlotId(x, y);
                if (PlotSquared.getPlots(world).get(id) != null) {
                    if (PlotSquared.getPlots(world).get(id).owner != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean move(final String world, final PlotId current, final PlotId newPlot, final Runnable whenDone) {
        final com.intellectualcrafters.plot.object.Location bot1 = MainUtil.getPlotBottomLoc(world, current);
        final com.intellectualcrafters.plot.object.Location bot2 = MainUtil.getPlotBottomLoc(world, newPlot);
        final Location top = MainUtil.getPlotTopLoc(world, current);
        final Plot currentPlot = MainUtil.getPlot(world, current);
        if (currentPlot.owner == null) {
            return false;
        }
        final Plot pos1 = getBottomPlot(currentPlot);
        final Plot pos2 = getTopPlot(currentPlot);
        final PlotId size = MainUtil.getSize(world, currentPlot);
        if (!MainUtil.isUnowned(world, newPlot, new PlotId((newPlot.x + size.x) - 1, (newPlot.y + size.y) - 1))) {
            return false;
        }
        final int offset_x = newPlot.x - pos1.id.x;
        final int offset_y = newPlot.y - pos1.id.y;
        final ArrayList<PlotId> selection = getPlotSelectionIds(pos1.id, pos2.id);
        for (final PlotId id : selection) {
            DBFunc.movePlot(world, new PlotId(id.x, id.y), new PlotId(id.x + offset_x, id.y + offset_y));
            final Plot plot = PlotSquared.getPlots(world).get(id);
            PlotSquared.getPlots(world).remove(id);
            plot.id.x += offset_x;
            plot.id.y += offset_y;
            PlotSquared.getPlots(world).put(plot.id, plot);
        }
        ChunkManager.manager.copyRegion(bot1, top, bot2, new Runnable() {
            @Override
            public void run() {
                final Location bot = bot1.clone().add(1, 0, 1);
                ChunkManager.manager.regenerateRegion(bot, top, null);
                TaskManager.runTaskLater(whenDone, 1);
            }
        });
        return true;
    }

    /**
     * Send a message to the player
     *
     * @param plr Player to recieve message
     * @param msg Message to send
     *
     * @return true Can be used in things such as commands (return PlayerFunctions.sendMessage(...))
     */
    public static boolean sendMessage(final PlotPlayer plr, final String msg) {
        return sendMessage(plr, msg, true);
    }

    public static String colorise(final char alt, final String message) {
        final char[] b = message.toCharArray();
        for (int i = 0; i < (b.length - 1); i++) {
            if ((b[i] == alt) && ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[(i + 1)]) > -1)) {
                b[i] = '�';
                b[(i + 1)] = Character.toLowerCase(b[(i + 1)]);
            }
        }
        return new String(b);
    }

    public static boolean sendMessage(final PlotPlayer plr, String msg, final boolean prefix) {
        msg = colorise('&', msg);
        final String prefixStr = colorise('&', C.PREFIX.s());
        if ((msg.length() > 0) && !msg.equals("")) {
            if (plr == null) {
                PlotSquared.log(prefixStr + msg);
            } else {
                sendMessageWrapped(plr, prefixStr + msg);
            }
        }
        return true;
    }

    public static String[] wordWrap(final String rawString, final int lineLength) {
        if (rawString == null) {
            return new String[] { "" };
        }
        if ((rawString.length() <= lineLength) && (!rawString.contains("\n"))) {
            return new String[] { rawString };
        }
        final char[] rawChars = (rawString + ' ').toCharArray();
        StringBuilder word = new StringBuilder();
        StringBuilder line = new StringBuilder();
        final ArrayList<String> lines = new ArrayList();
        int lineColorChars = 0;
        for (int i = 0; i < rawChars.length; i++) {
            final char c = rawChars[i];
            if (c == '�') {
                word.append('�' + (rawChars[(i + 1)]));
                lineColorChars += 2;
                i++;
            } else if ((c == ' ') || (c == '\n')) {
                if ((line.length() == 0) && (word.length() > lineLength)) {
                    for (final String partialWord : word.toString().split("(?<=\\G.{" + lineLength + "})")) {
                        lines.add(partialWord);
                    }
                } else if (((line.length() + word.length()) - lineColorChars) == lineLength) {
                    line.append(word);
                    lines.add(line.toString());
                    line = new StringBuilder();
                    lineColorChars = 0;
                } else if (((line.length() + 1 + word.length()) - lineColorChars) > lineLength) {
                    for (final String partialWord : word.toString().split("(?<=\\G.{" + lineLength + "})")) {
                        lines.add(line.toString());
                        line = new StringBuilder(partialWord);
                    }
                    lineColorChars = 0;
                } else {
                    if (line.length() > 0) {
                        line.append(' ');
                    }
                    line.append(word);
                }
                word = new StringBuilder();
                if (c == '\n') {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
            } else {
                word.append(c);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        if ((lines.get(0).length() == 0) || (lines.get(0).charAt(0) != '�')) {
            lines.set(0, "�f" + lines.get(0));
        }
        for (int i = 1; i < lines.size(); i++) {
            final String pLine = lines.get(i - 1);
            final String subLine = lines.get(i);

            final char color = pLine.charAt(pLine.lastIndexOf('�') + 1);
            if ((subLine.length() == 0) || (subLine.charAt(0) != '�')) {
                lines.set(i, '�' + (color) + subLine);
            }
        }
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * \\previous\\
     *
     * @param plr
     * @param msg Was used to wrap the chat client length (Packets out--)
     */
    public static void sendMessageWrapped(final PlotPlayer plr, final String msg) {
        //        if (msg.length() > 65) {
        //            final String[] ss = wordWrap(msg, 65);
        //            final StringBuilder b = new StringBuilder();
        //            for (final String p : ss) {
        //                b.append(p).append(p.equals(ss[ss.length - 1]) ? "" : "\n ");
        //            }
        //            msg = b.toString();
        //        }
        //        if (msg.endsWith("\n")) {
        //            msg = msg.substring(0, msg.length() - 2);
        //        }
        plr.sendMessage(msg);
    }

    /**
     * Send a message to the player
     *
     * @param plr Player to recieve message
     * @param c   Caption to send
     *
     * @return
     */
    public static boolean sendMessage(final PlotPlayer plr, final C c, final String... args) {
        if (c.s().length() > 1) {
            String msg = c.s();
            if ((args != null) && (args.length > 0)) {
                for (final String str : args) {
                    if (msg.contains("%s")) {
                        msg = msg.replaceFirst("%s", str);
                    }
                }
            }
            if (plr == null) {
                PlotSquared.log(colorise('&', msg));
            } else {
                sendMessage(plr, msg, c.usePrefix());
            }
        }
        return true;
    }

    public static Plot getBottomPlot(final Plot plot) {
        if (plot.settings.getMerged(0)) {
            final Plot p = PlotSquared.getPlots(plot.world).get(new PlotId(plot.id.x, plot.id.y - 1));
            if (p == null) {
                return plot;
            }
            return getBottomPlot(p);
        }
        if (plot.settings.getMerged(3)) {
            final Plot p = PlotSquared.getPlots(plot.world).get(new PlotId(plot.id.x - 1, plot.id.y));
            if (p == null) {
                return plot;
            }
            return getBottomPlot(p);
        }
        return plot;
    }

    public static Plot getTopPlot(final Plot plot) {
        if (plot.settings.getMerged(2)) {
            return getTopPlot(PlotSquared.getPlots(plot.world).get(new PlotId(plot.id.x, plot.id.y + 1)));
        }
        if (plot.settings.getMerged(1)) {
            return getTopPlot(PlotSquared.getPlots(plot.world).get(new PlotId(plot.id.x + 1, plot.id.y)));
        }
        return plot;
    }

    public static PlotId getSize(final String world, final Plot plot) {
        final PlotSettings settings = plot.settings;
        if (!settings.isMerged()) {
            return new PlotId(1, 1);
        }
        final Plot top = getTopPlot(plot);
        final Plot bot = getBottomPlot(plot);
        return new PlotId((top.id.x - bot.id.x) + 1, (top.id.y - bot.id.y) + 1);
    }

    /**
     * Fetches the plot from the main class
     */
    public static Plot getPlot(final String world, final PlotId id) {
        if (id == null) {
            return null;
        }
        if (PlotSquared.getPlots(world).containsKey(id)) {
            return PlotSquared.getPlots(world).get(id);
        }
        return new Plot(id, null, new ArrayList<UUID>(), new ArrayList<UUID>(), world);
    }

    /**
     * Returns the plot at a location (mega plots are not considered, all plots are treated as small plots)
     * @param loc
     * @return
     */
    public static PlotId getPlotAbs(final Location loc) {
        final String world = loc.getWorld();
        final PlotManager manager = PlotSquared.getPlotManager(world);
        if (manager == null) {
            return null;
        }
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        return manager.getPlotIdAbs(plotworld, loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Returns the plot id at a location (mega plots are considered)
     * @param loc
     * @return
     */
    public static PlotId getPlotId(final Location loc) {
        final String world = loc.getWorld();
        final PlotManager manager = PlotSquared.getPlotManager(world);
        if (manager == null) {
            return null;
        }
        final PlotWorld plotworld = PlotSquared.getPlotWorld(world);
        final PlotId id = manager.getPlotId(plotworld, loc.getX(), loc.getY(), loc.getZ());
        if ((id != null) && (plotworld.TYPE == 2)) {
            if (ClusterManager.getCluster(world, id) == null) {
                return null;
            }
        }
        return id;
    }

    /**
     * Get the maximum number of plots a player is allowed
     *
     * @param p
     * @return
     */
    public static int getAllowedPlots(final PlotPlayer p, final int current) {
        return Permissions.hasPermissionRange(p, "plots.plot", Settings.MAX_PLOTS, current);
    }

    public static Plot getPlot(final Location loc) {
        final PlotId id = getPlotId(loc);
        if (id == null) {
            return null;
        }
        return getPlot(loc.getWorld(), id);
    }
}
