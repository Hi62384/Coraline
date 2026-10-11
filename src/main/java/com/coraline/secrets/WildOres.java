package com.coraline.secrets;

import com.coraline.otherworld.OtherWorldEstate;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.Block;
import java.util.Random;

/** Small, persisted surface deposits. Only sampled near explorers, after terrain copying. */
public final class WildOres {
    static int kind(double roll,double distance){return distance>=180&&roll<.002?3:roll<.16?2:roll<.40?1:0;}
    static Block block(int kind){return switch(kind){case 1->ModBlocks.OTHER_DIAMOND_ORE.get();case 2->ModBlocks.MOONSTONE_ORE.get();case 3->ModBlocks.SOULGLASS_ORE.get();default->ModBlocks.OTHER_IRON_ORE.get();};}
    public static void tick(ServerPlayer p){
        var r=SecretWorld.realm(p);var s=SecretTransactions.state(p);
        if(r==null||r.house==null||r.sourceDoor==null||s==null)return;
        var cells=s.world.getCompound("OreCells");
        int cx=Math.floorDiv(p.getBlockX()-r.house.getX(),24),cz=Math.floorDiv(p.getBlockZ()-r.house.getZ(),24);
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            int x=cx+dx,z=cz+dz;String key=x+":"+z;if(cells.contains(key))continue;
            var random=new Random(s.seed^((long)x*341873128712L)^((long)z*132897987541L));
            var at=r.house.offset(x*24+4+random.nextInt(16),0,z*24+4+random.nextInt(16));
            if(!p.serverLevel().hasChunkAt(at)||!SecretWorld.terrainCopied(r,at))continue;
            double distance=OtherWorldEstate.outsideDistance(r.house,at);
            double ix=at.getX()-r.sourceDoor.getX()-r.dx,iz=at.getZ()-r.sourceDoor.getZ()-r.dz;
            cells.putBoolean(key,true);s.world.put("OreCells",cells);SecretTransactions.dirty(p);
            if(distance<60||ix*ix+iz*iz>Math.pow(r.radius-12,2)||random.nextDouble()>.65)continue;
            if(SecretWorld.rooms(s).stream().anyMatch(room->{var c=BlockPos.of(room.getLong("Pos"));return Math.abs(c.getX()-at.getX())<12&&Math.abs(c.getZ()-at.getZ())<12;}))continue;
            int ore=kind(random.nextDouble(),distance),count=ore==3?1:2+random.nextInt(3);
            for(int i=0;i<count;i++){
                int ox=at.getX()+i%2,oz=at.getZ()+i/2;
                int y=SecretWorld.ground(p.serverLevel(),ox,oz);if(y==Integer.MIN_VALUE)continue;
                var pos=new BlockPos(ox,y-1,oz);var state=p.serverLevel().getBlockState(pos);
                // No excavation, furniture replacement, floating ore or deposits under water.
                if(!(state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(BlockTags.DIRT))||!p.serverLevel().getFluidState(pos.above()).isEmpty())continue;
                if(s.world.getCompound("Placed").contains(Long.toString(pos.asLong())))continue;
                SecretWorld.place(p,pos,block(ore));
            }
            return; // At most one small deposit per second, no chunk generation or full-world scans.
        }
    }
    public static boolean canMine(ServerPlayer p,BlockPos at){
        if(!SecretsRuntime.active(p)||!(p.getMainHandItem().getItem() instanceof PickaxeItem))return false;
        var state=p.serverLevel().getBlockState(at);boolean ore=false;
        for(int i=0;i<4;i++)if(state.is(block(i)))ore=true;
        if(!ore||!p.getMainHandItem().isCorrectToolForDrops(state))return false;
        var s=SecretTransactions.state(p);String key=Long.toString(at.asLong());
        // Only this owner's generated deposits bypass the realm's building protection.
        return s!=null&&s.world.getCompound("Placed").contains(key);
    }
    private WildOres(){}
}
