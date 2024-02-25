package xyz.davidpineiro.blame;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Random;

/**
 *
 */
public class BlameWorldGeneratorThing extends BukkitRunnable{

    //TODO: builders can be turtles!!!!!


    public record RoomCoord(int x, int y, int z){
        static RoomCoord fromBlock(int x, int y, int z){
            final RoomCoord roomCoord = new RoomCoord(
                    x/BlameWorldGenerator.roomXLength,
                    y/BlameWorldGenerator.roomYLength,
                    z/BlameWorldGenerator.roomZLength
            );
            return roomCoord;
        }
    }

    public sealed interface RoomArea permits WallArea, EmptyArea {
        RoomCoord getCoord();

        static RoomArea fromBlock(int x, int y, int z){
            final RoomCoord roomCoord = new RoomCoord(
                    x/BlameWorldGenerator.roomXLength,
                    y/BlameWorldGenerator.roomYLength,
                    z/BlameWorldGenerator.roomZLength
                    );
            //walls
            if(Math.floorMod(x, BlameWorldGenerator.roomXLength) < BlameWorldGenerator.wallThickness
            || Math.floorMod(y, BlameWorldGenerator.roomYLength) < BlameWorldGenerator.wallThickness
            || Math.floorMod(z, BlameWorldGenerator.roomZLength) < BlameWorldGenerator.wallThickness){
                return new WallArea(roomCoord);
            }else{
                return new EmptyArea(roomCoord);
            }
        }
    }

    public record WallArea(RoomCoord roomCoord) implements RoomArea {
        @Override
        public RoomCoord getCoord() {
            return this.roomCoord;
        }

        @Override
        public String toString() {
            return "WallArea{" +
                    "roomCoord=" + roomCoord +
                    '}';
        }
    }

    public record EmptyArea(RoomCoord roomCoord) implements RoomArea {
        @Override
        public RoomCoord getCoord() {
            return this.roomCoord;
        }

        @Override
        public String toString() {
            return "EmptyArea{" +
                    "roomCoord=" + roomCoord +
                    '}';
        }
    }

    public static final class BlameWorldGenerator extends ChunkGenerator{
        static final int bedrockFloorThickness = 3;
        static final int bedrockCeilingThickness = 3;
        static final int roomXLength = 1000;
        static final int roomZLength = 500;
        static final int roomYLength = 200;

        static final int roomHoleRadius = 15;
        static final int wallThickness = 30;
        @Override
        public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkGenerator.ChunkData chunkData) {
            final MessageDigest messageDigest;
            try {
                messageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            final int minHeight = chunkData.getMinHeight();
            final int maxHeight = chunkData.getMaxHeight();

            final String seed = String.valueOf(worldInfo.getSeed());

            final String chunkSeedStr = chunkX + seed + chunkZ;
            final byte[] chunkSeedHash = messageDigest.digest(chunkSeedStr.getBytes());
            Random newRandom = new Random(new String(chunkSeedHash).hashCode());
            final boolean hasLightPillar = newRandom.nextFloat() < 0.05;

            for(int y = minHeight; y < maxHeight; y++) {
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        final int x = chunkX*16 + dx;
                        final int z = chunkZ*16 + dz;

                        final RoomCoord roomCoord = RoomCoord.fromBlock(x,y,z);

                        //light pillar
                        if(hasLightPillar && betweenInc(dx, 8,10) && betweenInc(dz,8,10)){
                            if(dx % 2 == 0){
                                chunkData.setBlock(dx,y,dz, Material.GLOWSTONE);
                            }else{
                                chunkData.setBlock(dx,y,dz, Material.BEDROCK);
                            }
                        }

                        final String roomSeedStr = roomCoord.x + seed + roomCoord.y + String.valueOf(roomCoord.z);
                        final byte[] roomSeedHash = messageDigest.digest(roomSeedStr.getBytes());
                        newRandom = new Random(new String(roomSeedHash).hashCode());
                        final boolean emptyRoom = newRandom.nextFloat() < 0.1;

                        if(emptyRoom){
                            chunkData.setBlock(dx,y,dz, Material.AIR);
                        }

                        final boolean XWallFloor = betweenInc(Math.floorMod(x, roomXLength), 0,wallThickness-1);
                        final boolean YWallFloor = betweenInc(Math.floorMod(y, roomYLength), 0,wallThickness-1);
                        final boolean ZWallFloor = betweenInc(Math.floorMod(z, roomZLength), 0,wallThickness-1);
                        //generate room walls/floors
                        if((XWallFloor ^ YWallFloor ^ ZWallFloor)
                        && !(XWallFloor && YWallFloor && ZWallFloor) //erase when all walls intersect
                        ){
                            final boolean XHole = betweenInc(Math.floorMod(x,roomXLength), roomXLength/2 - roomHoleRadius, roomXLength/2 + roomHoleRadius);
                            final boolean YHole = betweenInc(Math.floorMod(y,roomYLength), roomYLength/2 - roomHoleRadius, roomYLength/2 + roomHoleRadius);
                            final boolean ZHole = betweenInc(Math.floorMod(z,roomZLength), roomZLength/2 - roomHoleRadius, roomZLength/2 + roomHoleRadius);
                            if(XHole && YHole){
                            }else if(YHole && ZHole){
                            }else if(XHole && ZHole){
                            }else{//if its not a hole then put some bedrock
                                chunkData.setBlock(dx,y,dz, Material.BEDROCK);
                            }
                        }

                        //generate floor and ceiling
                        if(
                           betweenInc(y, minHeight, minHeight+bedrockFloorThickness-1)
//                        || betweenInc(y, maxHeight-bedrockCeilingThickness, maxHeight) //no roof?!?!?!?!? MUAHAHAHHA
                        && !(//its not in a hole area... on XZ plane
                               betweenInc(Math.floorMod(z,roomZLength), roomZLength/2, roomZLength/2 + roomHoleRadius)
                            && betweenInc(Math.floorMod(x,roomXLength), roomXLength/2, roomXLength/2 + roomHoleRadius)
                            )
                        ){
                            chunkData.setBlock(dx,y,dz, Material.BEDROCK);
                        }

                        //generate random light pillars
                    }
                }
            }

        }

        @Override public boolean shouldGenerateSurface() {
            return true;
        }
        @Override public boolean shouldGenerateCaves() {
            return true;
        }
        @Override public boolean shouldGenerateDecorations() {
            return true;
        }
        @Override public boolean shouldGenerateMobs() {
            return true;
        }
        @Override public boolean shouldGenerateStructures() {
            return true;
        }
        @Override public boolean shouldGenerateNoise() {
            return true;
        }

    }
    private final String donorWorld;
    private final String receivingWorld;
    private final Blame blame;

    public BlameWorldGeneratorThing(Blame blame, String donorWorld, String receivingWorld) {
        this.donorWorld = donorWorld;
        this.receivingWorld = receivingWorld;
        this.blame = blame;
    }

    private record DonorBlock(int x, int y, int z, Material newMaterial, Biome biome, BlockData blockData){};

    private static LinkedList<DonorBlock> donarQueue = new LinkedList<>();
    private static final int BLOCK_PROCESS_PER_TICK = 500;
    @Override
    public void run() {
        final World receiveWorldz = Bukkit.getWorld(receivingWorld);
        for(int i = 0; i<BLOCK_PROCESS_PER_TICK && !donarQueue.isEmpty(); i++){
            final DonorBlock donorBlock;
            if(donarQueue.size() == 1)
                donorBlock = donarQueue.pop();
            else
                donorBlock = donarQueue.removeLast();

//            final Block donorBlockBlock = donorBlock.block;
            final Block receiveingBlock = receiveWorldz.getBlockAt(
                    donorBlock.x, donorBlock.y, donorBlock.z);

            if(donorBlock.blockData != null)
                receiveingBlock.setBlockData(donorBlock.blockData);
            else if(donorBlock.newMaterial != null)
                receiveingBlock.setType(donorBlock.newMaterial);

            if(donorBlock.biome != null)
                receiveWorldz.setBiome(receiveingBlock.getLocation(), donorBlock.biome);
        }
    }

    /**
     * b <= a <= c
     * @param a
     * @param b
     * @param c
     * @return a >= b && a <= c
     */
    public static boolean betweenInc(int a, int b, int c){
        return a >= b && a <= c;
    }

    interface ChunkBlockConsumer {
        void consume(Block block, int x, int y, int z, int dx, int dz);
    }

    private void chunkBlocksIterator(String world, int chunkx, int chunkz, ChunkBlockConsumer blockConsumer){
        final World donorWorldz = Bukkit.getWorld(world);
        for(int dy=donorWorldz.getMinHeight();dy<donorWorldz.getMaxHeight();dy++) {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    final int x = chunkx * 16 + dx;
                    final int y = dy;
                    final int z = chunkz * 16 + dz;
                    final Block donorBlock = donorWorldz.getBlockAt(x,y,z);

                    blockConsumer.consume(donorBlock, x,y,z,dx,dz);
                }
            }
        }
    }

    public void replaceChunkBlocks(int chunkx, int chunkz, Material material){
        ChunkBlockConsumer blockChunkConsumer = (block, x, y, z, dx, dz) -> {
            donarQueue.add(new DonorBlock(x, y, z, material, null, null));
        };
        chunkBlocksIterator(this.receivingWorld, chunkx, chunkz, blockChunkConsumer);
    }

    public void copyChunkFromDonor(int chunkx, int chunkz, boolean doWall, boolean doWallHoles){
        final int boxStartY = -40;
        final int boxStartX = 6;
        final int boxWidth = 4;
        final int boxHeight = 3;
        final int boxInterdistance = 115;

        ChunkBlockConsumer blockChunkConsumer = (block, x, y, z, dx, dz) -> {
            if(doWall && (dz == 0 || dz == 15 || dx == 0 || dx == 15)){//are we on the wall?
                if(
                    doWallHoles
                    && Math.floorMod((y-boxStartY),(boxHeight+boxInterdistance)) <= boxHeight-1
                    && y >= boxStartY
                    && (
                        betweenInc(dx, boxStartX, boxStartX+boxWidth-1)
                        ||betweenInc(dz, boxStartX, boxStartX+boxWidth-1)
                    )
                ){ //air gap function
                    donarQueue.push(new DonorBlock(x, y, z, Material.AIR, block.getBiome(), null));
                }else{
                    donarQueue.push(new DonorBlock(x, y, z, Material.BEDROCK, block.getBiome(), null));
                }
            }else{
                donarQueue.push(new DonorBlock(x, y, z, null, block.getBiome(), block.getBlockData()));
            }
        };
        chunkBlocksIterator(this.donorWorld, chunkx, chunkz, blockChunkConsumer);
    }

    public void queueBoxPillar(int startX, int startZ, int radius, Material material){
        final World donorWorldz = Bukkit.getWorld(donorWorld);
        for(int y=donorWorldz.getMinHeight();y<donorWorldz.getMaxHeight();y++) {
            for (int dx = -radius/2; dx < radius/2; dx++) {
                for (int dz = -radius/2; dz < radius/2; dz++) {
                    final int x = startX + dx;
                    final int z = startZ + dz;

                    donarQueue.add(new DonorBlock(x, y, z, material, null, null));
                }
            }
        }
    }

    private static BukkitRunnable bukkitRunnable;
    public void start(JavaPlugin plugin){
        bukkitRunnable = this;
        bukkitRunnable.runTaskTimer(plugin, 0, 1);
    }

    public void stop(){
        bukkitRunnable.cancel();
    }

}
