package xyz.davidpineiro.blame;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.davidpineiro.blame.gui.BlameGuiHandler;

import javax.annotation.Nullable;

public class Blame extends JavaPlugin implements Listener {

//    Plugin blameson;
    BlameWorldGeneratorThing blameWorldGeneratorThing = new BlameWorldGeneratorThing(
            this,"blameDonor", "blame");
    BlameGuiHandler blameGuiHandler = new BlameGuiHandler(this);

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        blameGuiHandler.register();
//        blameson = this.getServer().getPluginManager().getPlugin("Blameson");
        blameWorldGeneratorThing.start(this);
    }

    @Override
    public void onDisable() {
        blameWorldGeneratorThing.stop();
        getLogger().info("fully UNloaded");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, @Nullable String id) {
        return new BlameWorldGeneratorThing.BlameWorldGenerator();
    }



    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        final Player player = e.getPlayer();
        final Location loc = player.getLocation();
        switch(e.getMessage().toLowerCase()){
            case "copy" -> {
                final Chunk chunk = e.getPlayer().getLocation().getChunk();
                blameWorldGeneratorThing.copyChunkFromDonor(chunk.getX(), chunk.getZ(), false, false);
                e.getPlayer().sendMessage("queued the blocked");
            }
            case "copywalls" -> {
                final Chunk chunk = e.getPlayer().getLocation().getChunk();
                blameWorldGeneratorThing.copyChunkFromDonor(chunk.getX(), chunk.getZ(), true, true);
                e.getPlayer().sendMessage("queued the blocked");
            }
            case "pillar" -> {
                blameWorldGeneratorThing.queueBoxPillar((int)loc.getX(), (int)loc.getZ(), 6, Material.SMOOTH_STONE);
                e.getPlayer().sendMessage("queued the blocked");
            }
            case "delete" -> {
                final Chunk chunk = e.getPlayer().getLocation().getChunk();
                blameWorldGeneratorThing.replaceChunkBlocks(chunk.getX(), chunk.getZ(), Material.AIR);
                e.getPlayer().sendMessage("queued the blocked");
            }
        }
    }

    @EventHandler
    public void click(PlayerInteractEvent e){
        final Location loc = e.getPlayer().getLocation();

        final boolean XWallFloor = BlameWorldGeneratorThing.betweenInc(Math.floorMod((int) loc.getX(), BlameWorldGeneratorThing.BlameWorldGenerator.roomXLength), 0,BlameWorldGeneratorThing.BlameWorldGenerator.wallThickness-1);
        final boolean YWallFloor = BlameWorldGeneratorThing.betweenInc(Math.floorMod((int) loc.getY(), BlameWorldGeneratorThing.BlameWorldGenerator.roomYLength), 0,BlameWorldGeneratorThing.BlameWorldGenerator.wallThickness-1);
        final boolean ZWallFloor = BlameWorldGeneratorThing.betweenInc(Math.floorMod((int) loc.getZ(), BlameWorldGeneratorThing.BlameWorldGenerator.roomZLength), 0,BlameWorldGeneratorThing.BlameWorldGenerator.wallThickness-1);

        final BlameWorldGeneratorThing.RoomArea roomArea = BlameWorldGeneratorThing.RoomArea.fromBlock(
                loc.getBlockX(),loc.getBlockY(),loc.getBlockZ()
        );
        Bukkit.broadcastMessage(String.format("roomArea: %s, xWall: %s, yWall %s, zWall: %s", roomArea, XWallFloor, YWallFloor, ZWallFloor));
    }

}
