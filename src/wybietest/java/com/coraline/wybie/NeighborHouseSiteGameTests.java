package com.coraline.wybie;

import com.coraline.bobinsky.*;
import com.coraline.tea.TeaHouse;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class NeighborHouseSiteGameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void bobinskyClearsBestObstructedSiteAndRestoresIt(GameTestHelper h){fallback(h,false,64000);}

    @GameTest(template="empty",timeoutTicks=200)
    public static void teaClearsBestObstructedSiteAndRestoresIt(GameTestHelper h){fallback(h,true,65000);}

    private static void fallback(GameTestHelper h,boolean tea,int x){
        var l=h.getLevel().getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);var origin=new BlockPos(x,220,64000);
        int minX=(x-51)>>4,maxX=(x+51)>>4,minZ=(origin.getZ()-51)>>4,maxZ=(origin.getZ()+51)>>4;
        for(int cx=minX;cx<=maxX;cx++)for(int cz=minZ;cz<=maxZ;cz++){l.setChunkForced(cx,cz,true);l.getChunk(cx,cz);}
        try{
            // Every possible candidate exceeds the old score cutoff, and trunks
            // project above the allowed height band. No clear-site escape exists.
            for(int dx=-51;dx<=51;dx++)for(int dz=-51;dz<=51;dz++){
                l.setBlock(origin.offset(dx,0,dz),Blocks.STONE.defaultBlockState(),2);
                l.setBlock(origin.offset(dx,1,dz),Blocks.OAK_LOG.defaultBlockState(),2);
            }
            var p=new ServerPlayer(l.getServer(),l,new GameProfile(UUID.randomUUID(),"SiteFallbackQA"));
            p.setPos(x+.5,221,origin.getZ()+.5);
            long seed=73021;
            p.getRandom().setSeed(seed);
            for(int i=0;i<36;i++){
                double angle=p.getRandom().nextDouble()*Math.PI*2;int radius=16+p.getRandom().nextInt(25);
                int dx=(int)Math.floor(p.getX()+Math.cos(angle)*radius),dz=(int)Math.floor(p.getZ()+Math.sin(angle)*radius);
                for(int y=222;y<=234;y++)l.setBlock(new BlockPos(dx,y,dz),Blocks.OAK_LOG.defaultBlockState(),2);
            }
            int rx=tea?TeaHouse.RX:4,rz=tea?TeaHouse.RZ:4;
            p.getRandom().setSeed(seed);var center=BobinskyHouse.find(p,rx,rz);
            h.assertTrue(center!=null,"Cluttered terrain prevented the visit house from spawning");
            h.assertTrue(center.getY()==221,"Tree canopy was treated as ground");
            var v=new CompoundTag();v.putLong("House",center.asLong());v.putBoolean("Tea",tea);v.putBoolean("DetailedTeaHouse",tea);
            v.put("Originals",BobinskyHouse.snapshot(l,center,rx,rz));
            if(tea)TeaHouse.build(l,v);else BobinskyHouse.build(l,v);
            for(var pos:BlockPos.betweenClosed(center.offset(-rx,0,-rz),center.offset(rx,9,rz)))
                h.assertFalse(l.getBlockState(pos).is(BlockTags.LOGS)&&l.getBlockState(pos).is(Blocks.OAK_LOG),"Original tree left inside the built house");
            h.assertTrue(l.getBlockState(center.below()).is(tea?Blocks.STONE_BRICKS:Blocks.DARK_OAK_PLANKS)
                ||tea&&l.getBlockState(center.below()).is(Blocks.CRACKED_STONE_BRICKS),"House has no solid floor");
            BobinskyHouse.restore(l,v);
            h.assertTrue(l.getBlockState(center).is(Blocks.OAK_LOG)&&l.getBlockState(center.below()).is(Blocks.STONE),"Cleared terrain was not restored");
            // Cover the entire scan with placement history; fallback must not
            // sacrifice player builds merely to guarantee a spawn.
            var data=BobinskyVisitData.get(p.server);
            for(int dx=-51;dx<=51;dx++)for(int dz=-51;dz<=51;dz++)data.playerPlaced.add(origin.offset(dx,1,dz).asLong());
            try{p.getRandom().setSeed(seed);h.assertTrue(BobinskyHouse.find(p,rx,rz)==null,"Fallback ignored player build protection");}
            finally{for(int dx=-51;dx<=51;dx++)for(int dz=-51;dz<=51;dz++)data.playerPlaced.remove(origin.offset(dx,1,dz).asLong());}
            h.succeed();
        }finally{for(int cx=minX;cx<=maxX;cx++)for(int cz=minZ;cz<=maxZ;cz++)l.setChunkForced(cx,cz,false);}
    }
}
