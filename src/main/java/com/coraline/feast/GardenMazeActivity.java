package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Arrays;

/** A deterministic, walkable hedge maze with a timed prize garden. */
public final class GardenMazeActivity {
    private static final String OWNER="CoralineGardenOwner";
    private static final int CELLS=29, GRID=CELLS*2+1, DURATION=120*20;
    private GardenMazeActivity(){}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(640);}
    static boolean exitReady(VisitData.Scene s){return s.loop.gardenFinished;}
    private static void put(ServerLevel l,BlockPos p,net.minecraft.world.level.block.Block b){ActivityBlocks.set(l,p,ActivityWorld.persistentLeaves(b),2);}
    private static boolean[][] passages(long seed){
        boolean[][] open=new boolean[GRID][GRID],seen=new boolean[CELLS][CELLS];
        int[] xs=new int[CELLS*CELLS],zs=new int[CELLS*CELLS];int top=0;
        xs[0]=CELLS/2;zs[0]=CELLS-1;seen[zs[0]][xs[0]]=true;
        open[zs[0]*2+1][xs[0]*2+1]=true;var random=RandomSource.create(seed);
        while(top>=0){int x=xs[top],z=zs[top],count=0;int[] dirs=new int[4];
            if(x>0&&!seen[z][x-1])dirs[count++]=0;
            if(x+1<CELLS&&!seen[z][x+1])dirs[count++]=1;
            if(z>0&&!seen[z-1][x])dirs[count++]=2;
            if(z+1<CELLS&&!seen[z+1][x])dirs[count++]=3;
            if(count==0){top--;continue;}
            int dir=dirs[random.nextInt(count)],nx=x+(dir==0?-1:dir==1?1:0),nz=z+(dir==2?-1:dir==3?1:0);
            open[z*2+1+(nz-z)][x*2+1+(nx-x)]=true;open[nz*2+1][nx*2+1]=true;
            seen[nz][nx]=true;xs[++top]=nx;zs[top]=nz;
        }
        open[GRID-1][GRID/2]=true;open[0][GRID/2]=true;
        // A giant maze must still be finishable in two minutes. A handful of
        // hidden cross-passages trim extreme DFS detours without making a straight route.
        for(int pass=0;pass<10;pass++){
            int[] path=solution(open);if(path.length<=301)break;
            int[] onPath=new int[GRID*GRID];Arrays.fill(onPath,-1);
            for(int i=0;i<path.length;i++)onPath[path[i]]=i;
            int best=-1,bestGain=0;
            for(int z=1;z<GRID-1;z++)for(int x=1;x<GRID-1;x++){
                if(open[z][x])continue;int pos=z*GRID+x;
                int gain=Math.max(bridgeGain(onPath,pos-1,pos+1),bridgeGain(onPath,pos-GRID,pos+GRID));
                if(gain>bestGain){bestGain=gain;best=pos;}
            }
            if(bestGain<20)break;open[best/GRID][best%GRID]=true;
        }
        // A perfect DFS maze is mostly one long corridor. Open a controlled
        // number of additional cell-to-cell joins across the whole maze so
        // players encounter loops and route choices every few turns.
        var junctions=RandomSource.create(seed^0x7A2D6E41L);
        for(int z=2;z<GRID-2;z++)for(int x=2;x<GRID-2;x++){
            if(open[z][x]||((x+z)&1)!=1||junctions.nextInt(100)>=27)continue;
            boolean horizontal=open[z][x-1]&&open[z][x+1],vertical=open[z-1][x]&&open[z+1][x];
            if(horizontal||vertical)open[z][x]=true;
        }
        for(int pass=0;pass<12&&solution(open).length>260;pass++){
            int[] path=solution(open),onPath=new int[GRID*GRID];int bestAt=-1,bestGain=0;Arrays.fill(onPath,-1);
            for(int i=0;i<path.length;i++)onPath[path[i]]=i;
            for(int z=2;z<GRID-2;z++)for(int x=2;x<GRID-2;x++){
                if(open[z][x])continue;int at=z*GRID+x;
                int gain=Math.max(bridgeGain(onPath,at-1,at+1),bridgeGain(onPath,at-GRID,at+GRID));
                if(gain>bestGain){bestGain=gain;bestAt=at;}
            }
            if(bestAt<0)break;open[bestAt/GRID][bestAt%GRID]=true;
        }
        return open;
    }
    private static int bridgeGain(int[] path,int a,int b){return path[a]<0||path[b]<0?0:Math.abs(path[a]-path[b])-2;}
    private static int[] solution(boolean[][] open){
        int[] previous=new int[GRID*GRID],queue=new int[GRID*GRID];Arrays.fill(previous,-1);
        int start=(GRID-2)*GRID+GRID/2,goal=GRID/2,head=0,tail=0;
        queue[tail++]=start;previous[start]=start;
        while(head<tail&&previous[goal]<0){int at=queue[head++],x=at%GRID,z=at/GRID;
            for(int next:new int[]{at-1,at+1,at-GRID,at+GRID}){
                int nx=next%GRID,nz=next/GRID;
                if(nx<0||nx>=GRID||nz<0||nz>=GRID||Math.abs(nx-x)+Math.abs(nz-z)!=1||!open[nz][nx]||previous[next]>=0)continue;
                previous[next]=at;queue[tail++]=next;
            }
        }
        if(previous[goal]<0)throw new IllegalStateException("Garden maze route disconnected");
        int length=1;for(int at=goal;at!=start;at=previous[at])length++;
        int[] path=new int[length];for(int at=goal,i=length-1;i>=0;i--,at=previous[at])path[i]=at;
        return path;
    }
    static void build(ServerLevel l,BlockPos c,VisitData.Scene s){
        if(l.getBlockState(c.below(2)).is(Blocks.GREEN_GLAZED_TERRACOTTA)){
            if(l.getBlockState(c.below(5)).is(Blocks.CYAN_GLAZED_TERRACOTTA))return;
            removeBuild(l,c);
        }
        boolean[][] open=passages(s.owner.getMostSignificantBits()^s.door.getLeastSignificantBits()^s.loop.generation);
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++){
            var at=c.offset(x,-1,z);put(l,at.below(),Blocks.DIRT);
            boolean rim=Math.abs(x)==49||Math.abs(z)==49;
            boolean garden=z<-31&&Math.abs(x)<30;
            put(l,at,rim?Blocks.MOSSY_COBBLESTONE:garden&&Math.floorMod(x+z,9)==0?Blocks.PINK_CONCRETE:
                garden&&Math.abs(x)<=2?Blocks.CHERRY_PLANKS:Blocks.GRASS_BLOCK);
            if(rim){put(l,at.above(),Blocks.CHERRY_FENCE);put(l,at.above(2),Blocks.CHERRY_FENCE);}
        }
        // Give the maze an inhabited garden setting, not an empty grass margin.
        for(int x=-47;x<=47;x++)for(int z=-47;z<=47;z++){
            boolean side=Math.abs(x)>=34;
            boolean forecourt=z>=33;
            if(!side&&!forecourt)continue;
            var at=c.offset(x,0,z);
            if((Math.abs(x)==34&&z>=-33&&z<=32)||(z==36&&Math.abs(x)<=44)){
                put(l,at.below(),Math.floorMod(x+z,8)==0?Blocks.SEA_LANTERN:Blocks.MOSSY_STONE_BRICKS);
            }else if(Math.floorMod(x*17+z*29,11)==0&&Math.abs(x)>4){
                put(l,at,Math.floorMod(x-z,3)==0?Blocks.PINK_TULIP:Math.floorMod(x+z,3)==0?Blocks.ALLIUM:Blocks.BLUE_ORCHID);
            }else if(Math.floorMod(x*7-z*13,37)==0&&Math.abs(x)>5){
                put(l,at,Blocks.FLOWERING_AZALEA);
            }
        }
        for(int x:new int[]{-42,42})for(int z:new int[]{-35,-17,2,20,40}){
            for(int y=0;y<4;y++)put(l,c.offset(x,y,z),Blocks.CHERRY_LOG);
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(dx*dx+dz*dz<=10){
                put(l,c.offset(x+dx,4,z+dz),Blocks.CHERRY_LEAVES);
                if(Math.floorMod(dx*3+dz*5,7)==0)put(l,c.offset(x+dx,3,z+dz),Blocks.FLOWERING_AZALEA_LEAVES);
            }
        }
        for(int x:new int[]{-39,39})for(int z:new int[]{-26,12}){
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                var at=c.offset(x+dx,-1,z+dz);
                if(Math.abs(dx)==3||Math.abs(dz)==3){put(l,at,Blocks.MOSSY_STONE_BRICKS);put(l,at.above(),Blocks.MOSSY_STONE_BRICKS);}
                else{put(l,at,Blocks.CLAY);put(l,at.above(),Blocks.WATER);}
            }
            for(int dx:new int[]{-4,4})for(int dz:new int[]{-4,4})put(l,c.offset(x+dx,0,z+dz),Blocks.FLOWERING_AZALEA);
        }
        for(int x:new int[]{-35,35})for(int z:new int[]{-29,-10,10,29}){
            put(l,c.offset(x,0,z),Blocks.CHERRY_FENCE);
            put(l,c.offset(x,1,z),Blocks.CHERRY_FENCE);
            put(l,c.offset(x,2,z),Blocks.SEA_LANTERN);
        }
        for(int x:new int[]{-7,7})put(l,c.offset(x,0,34),ModBlocks.BRASS_FLOOR_LAMP.get());
        for(int x:new int[]{-20,20}){
            put(l,c.offset(x,0,40),ModBlocks.ROSE_VELVET_SOFA.get());
            put(l,c.offset(x,0,42),ModBlocks.LOW_MARBLE_TABLE.get());
        }
        for(int x:new int[]{-24,-15,15,24})for(int z:new int[]{39,44}){
            for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=1;dz++){
                var at=c.offset(x+dx,0,z+dz);
                put(l,at.below(),Blocks.MOSS_BLOCK);
                put(l,at,Math.floorMod(dx+dz,3)==0?Blocks.FLOWERING_AZALEA:Math.floorMod(dx,2)==0?Blocks.PINK_TULIP:Blocks.ALLIUM);
            }
        }
        for(int gz=0;gz<GRID;gz++)for(int gx=0;gx<GRID;gx++){
            int x=gx-GRID/2,z=gz-GRID/2;var at=c.offset(x,0,z);
            if(open[gz][gx]){put(l,at.below(),Math.floorMod(gx+gz,7)==0?Blocks.SEA_LANTERN:Blocks.MOSS_BLOCK);continue;}
            for(int y=0;y<=2;y++)put(l,at.above(y),y==2&&Math.floorMod(gx*17+gz*31,13)==0?Blocks.FLOWERING_AZALEA_LEAVES:Blocks.AZALEA_LEAVES);
            if(gx%8==0&&gz%8==0)put(l,at.above(3),Blocks.SEA_LANTERN);
            if(Math.floorMod(gx*11+gz*7,23)==0)put(l,at.above(2),Blocks.SHROOMLIGHT);
        }
        // A pair of floral arches frames the only entrances; the route inside is one connected maze.
        for(int z:new int[]{-30,30}){
            for(int side:new int[]{-1,1})for(int y=0;y<=4;y++)put(l,c.offset(side*2,y,z),Blocks.CHERRY_LOG);
            for(int x=-2;x<=2;x++)put(l,c.offset(x,5,z),x==0?Blocks.SEA_LANTERN:Blocks.FLOWERING_AZALEA_LEAVES);
        }
        for(int x=-29;x<=29;x++)for(int z:new int[]{-31,31})if(Math.abs(x)>1)for(int y=0;y<4;y++)put(l,c.offset(x,y,z),Blocks.FLOWERING_AZALEA_LEAVES);
        for(int z=-30;z<=31;z++)for(int x:new int[]{-31,31})for(int y=0;y<4;y++)put(l,c.offset(x,y,z),Blocks.FLOWERING_AZALEA_LEAVES);
        // The finish garden is intentionally much richer than the entrance so the missed prizes remain visible.
        for(int x=-28;x<=28;x++)for(int z=-48;z<=-33;z++){
            if(Math.abs(x)>4&&Math.floorMod(x*13+z*7,5)==0)put(l,c.offset(x,0,z),
                Math.floorMod(x+z,3)==0?Blocks.PINK_TULIP:Math.floorMod(x-z,2)==0?Blocks.ALLIUM:Blocks.BLUE_ORCHID);
            if(Math.abs(x)>8&&Math.floorMod(x*5+z*11,37)==0)put(l,c.offset(x,0,z),Blocks.FLOWERING_AZALEA);
            if(Math.abs(x)>10&&Math.floorMod(x*7+z*13,41)==0)put(l,c.offset(x,0,z),Blocks.POTTED_AZURE_BLUET);
        }
        for(int x:new int[]{-23,-14,14,23})for(int z:new int[]{-45,-35}){
            for(int y=0;y<4;y++)put(l,c.offset(x,y,z),Blocks.CHERRY_LOG);
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(dx*dx+dz*dz<=10){
                put(l,c.offset(x+dx,4,z+dz),Blocks.CHERRY_LEAVES);
                if(Math.floorMod(dx*3+dz*5,7)==0)put(l,c.offset(x+dx,3,z+dz),Blocks.FLOWERING_AZALEA_LEAVES);
            }
        }
        for(int x=-12;x<=12;x++)for(int z=-47;z<=-37;z++)if((Math.abs(x)==12||z==-47||z==-37)&&!(z==-37&&Math.abs(x)<=2)){
            put(l,c.offset(x,0,z),Blocks.CHERRY_LEAVES);
            if(Math.floorMod(x+z,4)==0)put(l,c.offset(x,1,z),Blocks.SEA_LANTERN);
        }
        for(int x:new int[]{-3,3})for(int y=0;y<=3;y++)put(l,c.offset(x,y,-37),Blocks.CHERRY_LOG);
        for(int x=-3;x<=3;x++)put(l,c.offset(x,4,-37),x==0?Blocks.SEA_LANTERN:Blocks.FLOWERING_AZALEA_LEAVES);
        for(int x:new int[]{-16,16})for(int z:new int[]{-39,-45})put(l,c.offset(x,0,z),ModBlocks.BRASS_FLOOR_LAMP.get());
        for(int x:new int[]{-9,-3,3,9}){
            var table=c.offset(x,0,-42);put(l,table,ModBlocks.LOW_MARBLE_TABLE.get());
            for(int side:new int[]{-1,1})put(l,table.offset(side,0,0),Blocks.POTTED_PINK_TULIP);
        }
        for(int x=-5;x<=5;x++){put(l,c.offset(x,0,-48),Blocks.CHERRY_PLANKS);if(Math.abs(x)==5)for(int y=1;y<5;y++)put(l,c.offset(x,y,-48),Blocks.CHERRY_FENCE);}
        put(l,c.below(2),Blocks.GREEN_GLAZED_TERRACOTTA);
        put(l,c.below(5),Blocks.CYAN_GLAZED_TERRACOTTA);
        put(l,c.below(3),Blocks.YELLOW_GLAZED_TERRACOTTA);
        put(l,c.below(4),Blocks.BLUE_GLAZED_TERRACOTTA);
    }
    private static void upgradePassages(ServerLevel level,BlockPos c,VisitData.Scene s){
        if(level.getBlockState(c.below(4)).is(Blocks.BLUE_GLAZED_TERRACOTTA))return;
        var open=passages(s.owner.getMostSignificantBits()^s.door.getLeastSignificantBits()^s.loop.generation);
        for(int gz=0;gz<GRID;gz++)for(int gx=0;gx<GRID;gx++)if(open[gz][gx]){
            var at=c.offset(gx-GRID/2,0,gz-GRID/2);
            for(int y=0;y<=3;y++)if(!level.isEmptyBlock(at.above(y)))put(level,at.above(y),Blocks.AIR);
        }
        put(level,c.below(4),Blocks.BLUE_GLAZED_TERRACOTTA);
    }
    private static void repairLeaves(ServerLevel level,BlockPos c,VisitData.Scene s){
        if(level.getBlockState(c.below(3)).is(Blocks.YELLOW_GLAZED_TERRACOTTA))return;
        // Older saved mazes contain non-persistent leaves, and some hedge
        // segments have already vanished. Restore only authored hedge cells.
        boolean[][] open=passages(s.owner.getMostSignificantBits()^s.door.getLeastSignificantBits()^s.loop.generation);
        for(int gz=0;gz<GRID;gz++)for(int gx=0;gx<GRID;gx++)if(!open[gz][gx]){
            var at=c.offset(gx-GRID/2,0,gz-GRID/2);
            for(int y=0;y<=2;y++)if(level.isEmptyBlock(at.above(y)))put(level,at.above(y),
                y==2&&Math.floorMod(gx*17+gz*31,13)==0?Blocks.FLOWERING_AZALEA_LEAVES:Blocks.AZALEA_LEAVES);
        }
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++)for(int y=0;y<=6;y++){
            var at=c.offset(x,y,z);var state=level.getBlockState(at);
            if(state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock&&
                !state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT))
                ActivityBlocks.set(level,at,state.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),2);
        }
        put(level,c.below(3),Blocks.YELLOW_GLAZED_TERRACOTTA);
    }
    static void removeBuild(ServerLevel l,BlockPos c){
        if(!l.getBlockState(c.below(2)).is(Blocks.GREEN_GLAZED_TERRACOTTA))return;
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++)for(int y=-5;y<=8;y++){
            var at=c.offset(x,y,z);if(!l.isEmptyBlock(at))put(l,at,Blocks.AIR);
        }
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){
        var found=new ArrayList<Entity>();for(var e:l.getEntitiesOfClass(Entity.class,new AABB(c).inflate(52,12,52),e->e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))found.add(e);
        found.forEach(Entity::discard);
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        var c=center(p,s);build(l,c,s);clean(l,s,c);var state=s.loop;
        state.gardenVoiceUntil=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.GARDEN_MAZE));
        state.gardenEndAt=0;state.gardenFinished=false;state.gardenEligible=false;state.gardenClaimMask=0;
        ensureFather(p,s,l,c);ensurePrizes(l,s,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        LoopProps.hold(l,c);LoopProps.hold(l,c.north(42));build(l,c,s);repairLeaves(l,c,s);upgradePassages(l,c,s);lowerPrizeTables(l,c);ensureFather(p,s,l,c);ensurePrizes(l,s,c);
        l.setWeatherParameters(120000,0,false,false);l.setRainLevel(0);l.setThunderLevel(0);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
            net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
            net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0));
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        if(!l.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(34)).inflate(8),e->e.getPersistentData().getBoolean("CoralineGardenFather")&&e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var family=ParentAccess.family(p.server,s.owner);
        var father=ModEntities.DAD.get().create(l);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);
        father.getPersistentData().putBoolean("CoralineGardenFather",true);father.getPersistentData().putUUID(OWNER,s.owner);
        father.getPersistentData().putUUID("RealmDoor",s.door);father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+34.5);father.setYRot(90);father.setYBodyRot(90);l.addFreshEntity(father);
    }
    private static ItemStack prize(int slot){return switch(slot){case 0->new ItemStack(Items.EMERALD,8);case 1->new ItemStack(Items.GOLD_INGOT,12);case 2->new ItemStack(Items.DIAMOND,5);default->new ItemStack(Items.AMETHYST_SHARD,16);};}
    private static void lowerPrizeTables(ServerLevel level,BlockPos c){
        for(int x:new int[]{-9,-3,3,9}){
            var table=c.offset(x,0,-42);
            if(!level.getBlockState(table).is(ModBlocks.LOW_MARBLE_TABLE.get()))put(level,table,ModBlocks.LOW_MARBLE_TABLE.get());
            if(level.getBlockState(table.above()).is(Blocks.EMERALD_BLOCK)||level.getBlockState(table.above()).is(Blocks.GOLD_BLOCK)||
                level.getBlockState(table.above()).is(Blocks.DIAMOND_BLOCK)||level.getBlockState(table.above()).is(Blocks.AMETHYST_BLOCK))put(level,table.above(),Blocks.AIR);
        }
    }
    private static void ensurePrizes(ServerLevel level,VisitData.Scene s,BlockPos c){
        for(int slot=0;slot<4;slot++){final int id=slot;int x=new int[]{-9,-3,3,9}[slot];var table=c.offset(x,0,-42);
            var existing=level.getEntitiesOfClass(ScenePropEntity.class,new AABB(table).inflate(1,4,1),e->(e.kind()==ScenePropEntity.PLATE||e.kind()==ScenePropEntity.PRIZE_PLATE)&&e.slot==id&&s.owner.equals(e.owner));
            for(var prop:existing)if(prop.kind()!=ScenePropEntity.PRIZE_PLATE||prop.getY()<c.getY()+.7||prop.getY()>c.getY()+1)prop.discard();
            var active=existing.stream().filter(prop->!prop.isRemoved()).findFirst().orElse(null);
            if(active!=null){
                ItemStack expected=(s.loop.gardenClaimMask&(1<<slot))==0?prize(slot):ItemStack.EMPTY;
                if(!ItemStack.isSameItemSameTags(active.food(0),expected)||active.food(0).getCount()!=expected.getCount())active.food(0,expected);
                continue;
            }
            var display=LoopProps.spawn(level,s,ScenePropEntity.PRIZE_PLATE,new Vec3(table.getX()+.5,table.getY()+.76,table.getZ()+.5),slot);
            display.getPersistentData().putUUID(OWNER,s.owner);
            if((s.loop.gardenClaimMask&(1<<slot))==0)display.food(0,prize(slot));
        }
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var st=s.loop;long now=OtherMotherLoop.now(p);
        if(p.tickCount%20==0)prepare(p,s,p.serverLevel(),c);
        if(st.gardenFinished)return;
        if(now<st.gardenVoiceUntil){
            if(Math.abs(p.getX()-(c.getX()+.5))>5||Math.abs(p.getZ()-(c.getZ()+34.5))>4){
                p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+34.5,180,0);p.setDeltaMovement(Vec3.ZERO);
            }
            if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Wait for Father. The garden gate opens when he finishes speaking."),true);
            return;
        }
        if(st.gardenEndAt==0){st.gardenEndAt=now+DURATION;OtherMotherLoop.changed(p);
            p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+28.5,180,0);
            p.sendSystemMessage(Component.literal("The garden maze begins! Reach the prize garden in two minutes."));
        }
        if(now>=st.gardenEndAt){finish(p,s,c,false);return;}
        if(p.getZ()<c.getZ()-29.5&&Math.abs(p.getX()-(c.getX()+.5))<2.5){finish(p,s,c,true);return;}
        if(Math.abs(p.getX()-(c.getX()+.5))>30||p.getZ()>c.getZ()+30.5||p.getZ()<c.getZ()-31.5||p.getY()<c.getY()-2){
            p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+28.5,180,0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        }
        if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Garden maze: "+Math.max(0,(st.gardenEndAt-now+19)/20)+"s remaining"),true);
    }
    private static void finish(ServerPlayer p,VisitData.Scene s,BlockPos c,boolean won){
        var st=s.loop;if(st.gardenFinished)return;
        st.gardenFinished=true;st.gardenEligible=won;OtherMotherLoop.changed(p);
        // Reaching the goal should feel continuous. Only a timeout rescues the
        // player, and that rescue lands well before the prize tables/exit.
        if(!won){p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()-35.5,180,0);
            p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}
        ActivityWorld.ensureExit(p,s);
        p.serverLevel().playSound(null,p.blockPosition(),won?SoundEvents.UI_TOAST_CHALLENGE_COMPLETE:SoundEvents.VILLAGER_NO,SoundSource.PLAYERS,1F,1F);
        p.sendSystemMessage(Component.literal(won?"You reached the hidden garden! Right-click the four illuminated prize tables to claim their treasures.":
            "Time ran out. Father shows you the hidden garden and the prizes you missed, but their tables are sealed. The return portal is here."));
    }
    public static boolean interactBlock(ServerPlayer p,BlockPos at){
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.GARDEN_MAZE||!ActivityWorld.isActivity(p.level()))return false;
        var c=center(p,s);if(at.getZ()!=c.getZ()-42||at.getY()!=c.getY())return false;
        int slot=switch(at.getX()-c.getX()){case -9->0;case -3->1;case 3->2;case 9->3;default->-1;};if(slot<0)return false;
        return claim(p,s,slot,at);
    }
    public static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(s.loop.activity!=OtherMotherActivities.Activity.GARDEN_MAZE||prop.kind()!=ScenePropEntity.PRIZE_PLATE||prop.slot<0||prop.slot>=4)return false;
        return claim(p,s,prop.slot,prop.blockPosition());
    }
    private static boolean claim(ServerPlayer p,VisitData.Scene s,int slot,BlockPos at){
        var st=s.loop;
        if(!st.gardenFinished||!st.gardenEligible){p.displayClientMessage(Component.literal("These were the prizes at the end. You cannot claim them after the timer expired."),true);return true;}
        if((st.gardenClaimMask&(1<<slot))!=0){p.displayClientMessage(Component.literal("You already claimed this prize."),true);return true;}
        if(p.distanceToSqr(Vec3.atCenterOf(at))>36)return true;
        ItemStack prize=prize(slot);
        String prizeName=prize.getHoverName().getString();
        st.gardenClaimMask|=1<<slot;OtherMotherLoop.changed(p);
        var c=center(p,s);for(var display:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c.offset(new int[]{-9,-3,3,9}[slot],0,-42)).inflate(1),e->e.kind()==ScenePropEntity.PRIZE_PLATE&&e.slot==slot&&s.owner.equals(e.owner)))display.food(0,ItemStack.EMPTY);
        p.getInventory().add(prize);if(!prize.isEmpty())p.drop(prize,false);
        p.serverLevel().playSound(null,at,SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,.8F,1.3F);
        p.sendSystemMessage(Component.literal("Claimed "+prizeName+". "+(4-Integer.bitCount(st.gardenClaimMask))+" tables remain."));return true;
    }
}
