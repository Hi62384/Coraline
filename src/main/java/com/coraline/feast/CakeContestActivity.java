package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModBlocks;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** A persistent, server-scored two-minute festival contest with four rival bakers. */
@Mod.EventBusSubscriber(modid="coraline")
public final class CakeContestActivity {
    private static final String OWNER="CoralineCakeOwner",GEN="CoralineCakeGeneration",NPC="CoralineCakeNPC",ROLE="CoralineCakeRole";
    private static final int[] CHEF_X={-16,-8,8,16};
    private static final String[] CHEFS={"Chef Rosalie","Chef Juniper","Chef Mallow","Chef Sterling"};
    public static final int AUDIENCE=36,STEPS=4,BAKE_TICKS=120*20;
    private static final Map<UUID,ServerBossEvent> TIMERS=new ConcurrentHashMap<>();
    private CakeContestActivity(){}
    static void clearTimer(ServerPlayer p){ServerBossEvent bar=TIMERS.remove(p.getUUID());if(bar!=null)bar.removeAllPlayers();}
    private static void timer(ServerPlayer p,long remaining){
        ServerBossEvent bar=TIMERS.computeIfAbsent(p.getUUID(),id->new ServerBossEvent(Component.literal("Cake Contest"),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS));
        bar.addPlayer(p);bar.setName(Component.literal("Cake Contest  •  "+Math.max(0,(remaining+19)/20)+"s"));
        bar.setProgress(Math.max(0,Math.min(1,remaining/(float)BAKE_TICKS)));
    }
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).west(160);}
    public static boolean exitReady(VisitData.Scene s){return s.loop.cakeStage==8;}
    static void build(ServerLevel level,BlockPos c){
        if(level.getBlockState(c.below(2)).is(Blocks.PINK_GLAZED_TERRACOTTA))return;
        if(level.getBlockState(c.below()).is(Blocks.PURPLE_GLAZED_TERRACOTTA))removeBuild(level,c);
        for(int x=-40;x<40;x++)for(int z=-40;z<40;z++){
            var ground=c.offset(x,-1,z);
            ActivityBlocks.set(level,ground.below(),Blocks.DIRT.defaultBlockState(),2);
            boolean rim=x==-40||x==39||z==-40||z==39;
            boolean aisle=Math.abs(x)<=2&&z>=-25&&z<=32;
            boolean dance=Math.abs(x)<=21&&z>=-8&&z<=9;
            ActivityBlocks.set(level,ground,(rim?Blocks.POLISHED_BLACKSTONE:aisle?(x==0&&z%4==0?Blocks.GOLD_BLOCK:Blocks.POLISHED_DIORITE):dance?Blocks.GRASS_BLOCK:Blocks.GRASS_BLOCK).defaultBlockState(),2);
            if(rim)for(int y=0;y<2;y++)ActivityBlocks.set(level,ground.above(y+1),Blocks.CHERRY_FENCE.defaultBlockState(),2);
        }
        // Five matching cake stands; the middle one belongs to the player.
        for(int i=0;i<5;i++){
            int x=(i-2)*8;
            for(int dx=-3;dx<=3;dx++)for(int z=-4;z<=5;z++)
                ActivityBlocks.set(level,c.offset(x+dx,-1,z),(Math.abs(dx)==3||z==-4||z==5?Blocks.MAGENTA_TERRACOTTA:Blocks.WHITE_TERRACOTTA).defaultBlockState(),2);
            for(int dx:new int[]{-3,3})for(int z:new int[]{-3,4})for(int y=0;y<4;y++)
                ActivityBlocks.set(level,c.offset(x+dx,y,z),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),2);
            for(int dx=-3;dx<=3;dx++)for(int z=-3;z<=4;z++)
                ActivityBlocks.set(level,c.offset(x+dx,4,z),(Math.floorMod(dx+z,2)==0?Blocks.PINK_WOOL:Blocks.WHITE_WOOL).defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(x,0,0),ModBlocks.LOW_MARBLE_TABLE.get().defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(x,0,-1),Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(x,0,-2),Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.SmokerBlock.FACING,Direction.SOUTH),2);
            ActivityBlocks.set(level,c.offset(x-2,0,-1),ModBlocks.FLOWER_CONSOLE.get().defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(x+2,0,-1),ModBlocks.CHERRY_SIDEBOARD.get().defaultBlockState(),2);
            for(int z=-2;z<=2;z+=2){
                ActivityBlocks.set(level,c.offset(x-2,0,z),(z==-2?Blocks.HAY_BLOCK:z==0?Blocks.HONEYCOMB_BLOCK:Blocks.WHITE_WOOL).defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(x+2,0,z),(z==-2?ModBlocks.CAKE_COCOA_RACK.get():z==0?ModBlocks.CAKE_VANILLA_RACK.get():ModBlocks.CAKE_BLUEBERRY_RACK.get()).defaultBlockState(),2);
            }
            for(int dx:new int[]{-3,3})ActivityBlocks.set(level,c.offset(x+dx,4,0),Blocks.SEA_LANTERN.defaultBlockState(),2);
        }
        // Raised judging panel, five presentation plinths and a seat facing the judges.
        for(int x=-13;x<=13;x++)for(int z=-32;z<=-19;z++)ActivityBlocks.set(level,c.offset(x,-1,z),Blocks.POLISHED_DIORITE.defaultBlockState(),2);
        for(int x=-12;x<=12;x++)for(int z=-31;z<=-27;z++)
            ActivityBlocks.set(level,c.offset(x,0,z),(Math.floorMod(x,4)==0?Blocks.GOLD_BLOCK:Blocks.CHERRY_PLANKS).defaultBlockState(),2);
        for(int x:new int[]{-8,-4,0,4,8})ActivityBlocks.set(level,c.offset(x,0,-22),Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
        ActivityBlocks.set(level,c.offset(0,0,-14),ModBlocks.ROSE_VELVET_SOFA.get().defaultBlockState(),2);
        for(int x=-12;x<=12;x++){
            ActivityBlocks.set(level,c.offset(x,0,-33),ActivityWorld.persistentLeaves(Blocks.CHERRY_LEAVES),2);
            if(Math.floorMod(x,3)==0)ActivityBlocks.set(level,c.offset(x,1,-33),ActivityWorld.persistentLeaves(Blocks.FLOWERING_AZALEA_LEAVES),2);
        }
        // Audience seats face the judges from the lawn in front of the dais.
        for(int i=0;i<AUDIENCE;i++){
            var seat=audienceSeat(c,i);
            ActivityBlocks.set(level,seat,Blocks.CHERRY_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING,Direction.NORTH),2);
            ActivityBlocks.set(level,seat.below(),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        }
        for(int side:new int[]{-1,1}){
            for(int z=-22;z<=24;z+=5){
                ActivityBlocks.set(level,c.offset(side*24,0,z),Blocks.FLOWERING_AZALEA.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(side*37,0,z),Blocks.PINK_TULIP.defaultBlockState(),2);
            }
            for(int z:new int[]{-33,-17,0,17,33}){
                for(int y=0;y<3;y++)ActivityBlocks.set(level,c.offset(side*38,y,z),Blocks.CHERRY_FENCE.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(side*38,3,z),Blocks.SEA_LANTERN.defaultBlockState(),2);
            }
            for(int x=24;x<=37;x++)for(int z:new int[]{-35,35})if(Math.floorMod(x,2)==0)
                ActivityBlocks.set(level,c.offset(side*x,0,z),ActivityWorld.persistentLeaves(Blocks.CHERRY_LEAVES),2);
        }
        // Low planted parterres make the walk to the contestants feel like a festival garden.
        for(int side:new int[]{-1,1}){
            for(int z=8;z<=31;z++){
                if(z%8<6)ActivityBlocks.set(level,c.offset(side*6,0,z),ActivityWorld.persistentLeaves(Blocks.CHERRY_LEAVES),2);
                if(z%3==0){ActivityBlocks.set(level,c.offset(side*10,0,z),Blocks.PINK_TULIP.defaultBlockState(),2);
                    ActivityBlocks.set(level,c.offset(side*12,0,z),Blocks.ALLIUM.defaultBlockState(),2);}
            }
            for(int z:new int[]{10,18,26}){
                ActivityBlocks.set(level,c.offset(side*8,0,z),Blocks.CHERRY_FENCE.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(side*8,1,z),Blocks.CHERRY_FENCE.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(side*8,2,z),Blocks.SEA_LANTERN.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(side*15,0,z),Blocks.MAGENTA_BANNER.defaultBlockState(),2);
            }
        }
        for(int z:new int[]{10,22}){
            for(int side:new int[]{-1,1})for(int y=0;y<=6;y++)ActivityBlocks.set(level,c.offset(side*5,y,z),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),2);
            for(int x=-5;x<=5;x++)ActivityBlocks.set(level,c.offset(x,6,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(0,5,z),Blocks.CHAIN.defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(0,4,z),Blocks.SEA_LANTERN.defaultBlockState(),2);
        }
        for(int z:new int[]{-27,-6,15,29}){
            for(int side:new int[]{-1,1})for(int y=0;y<=5;y++)ActivityBlocks.set(level,c.offset(side*19,y,z),Blocks.CHERRY_FENCE.defaultBlockState(),2);
            for(int x=-18;x<=18;x++)ActivityBlocks.set(level,c.offset(x,6,z),(Math.floorMod(x,6)==0?Blocks.SEA_LANTERN:Blocks.PINK_STAINED_GLASS).defaultBlockState(),2);
        }
        // Orchard corners, lit paths, sweet carts and flower beds fill the whole
        // festival lawn while leaving every booth and judging approach clear.
        for(int side:new int[]{-1,1})for(int z:new int[]{-28,-2,22}){
            int x=side*31;var tree=c.offset(x,0,z);
            for(int y=0;y<5;y++)ActivityBlocks.set(level,tree.above(y),Blocks.CHERRY_LOG.defaultBlockState(),2);
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=3;dy<=6;dy++)
                if(dx*dx+dz*dz+(dy-4)*(dy-4)<13)ActivityBlocks.set(level,tree.offset(dx,dy,dz),
                    ActivityWorld.persistentLeaves(Math.floorMod(dx+dz+dy,7)==0?Blocks.FLOWERING_AZALEA_LEAVES:Blocks.CHERRY_LEAVES),2);
            for(int dx:new int[]{-4,4})for(int dz:new int[]{-4,4}){
                ActivityBlocks.set(level,tree.offset(dx,0,dz),Blocks.SEA_LANTERN.defaultBlockState(),2);
                ActivityBlocks.set(level,tree.offset(dx,1,dz),Blocks.PINK_STAINED_GLASS.defaultBlockState(),2);
            }
        }
        for(int side:new int[]{-1,1})for(int z=12;z<=34;z+=7){
            int x=side*26;var cart=c.offset(x,0,z);
            for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=1;dz++)ActivityBlocks.set(level,cart.offset(dx,-1,dz),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            for(int dx:new int[]{-2,2})for(int dz:new int[]{-1,1})for(int y=0;y<3;y++)ActivityBlocks.set(level,cart.offset(dx,y,dz),Blocks.CHERRY_FENCE.defaultBlockState(),2);
            for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=1;dz++)ActivityBlocks.set(level,cart.offset(dx,3,dz),
                Math.floorMod(dx+dz,2)==0?Blocks.PINK_WOOL.defaultBlockState():Blocks.WHITE_WOOL.defaultBlockState(),2);
            ActivityBlocks.set(level,cart,Blocks.BARREL.defaultBlockState(),2);
            ActivityBlocks.set(level,cart.offset(side,0,0),Blocks.CAKE.defaultBlockState(),2);
        }
        for(int x=-37;x<=37;x++)for(int z=-36;z<=36;z++){
            var at=c.offset(x,0,z);if(!level.isEmptyBlock(at)||!level.getBlockState(at.below()).is(Blocks.GRASS_BLOCK))continue;
            int n=Math.floorMod(x*37+z*19+x*z,37);
            if(n==0)ActivityBlocks.set(level,at,Blocks.PINK_TULIP.defaultBlockState(),2);
            else if(n==1)ActivityBlocks.set(level,at,Blocks.ALLIUM.defaultBlockState(),2);
            else if(n==2)ActivityBlocks.set(level,at,Blocks.WHITE_TULIP.defaultBlockState(),2);
            else if(n==3&&Math.abs(x)>23)ActivityBlocks.set(level,at,Blocks.FLOWERING_AZALEA.defaultBlockState(),2);
        }
        ActivityBlocks.set(level,c.below(),Blocks.PURPLE_GLAZED_TERRACOTTA.defaultBlockState(),2);
        ActivityBlocks.set(level,c.below(2),Blocks.PINK_GLAZED_TERRACOTTA.defaultBlockState(),2);
    }
    private static BlockPos audienceSeat(BlockPos c,int i){int col=i%8,row=i/8,x=col<4?-20+col*5:5+(col-4)*5;return c.offset(x,0,-9-row*2);}
    static void removeBuild(ServerLevel level,BlockPos c){
        if(!level.getBlockState(c.below()).is(Blocks.PURPLE_GLAZED_TERRACOTTA)&&!level.getBlockState(c.below(2)).is(Blocks.PINK_GLAZED_TERRACOTTA))return;
        for(int x=-40;x<40;x++)for(int z=-40;z<40;z++)for(int y=-2;y<=6;y++){
            var at=c.offset(x,y,z);if(!level.isEmptyBlock(at))ActivityBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
        }
    }
    static void clean(ServerLevel level,VisitData.Scene s,BlockPos c){
        var remove=new ArrayList<Entity>();
        for(var e:level.getEntitiesOfClass(Entity.class,new AABB(c).inflate(42,10,42),e->e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))remove.add(e);
        for(var e:level.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(42,10,42),e->(e.kind()==ScenePropEntity.CHAIR||e.kind()==ScenePropEntity.CONTEST_CAKE||e.kind()==ScenePropEntity.CAKE_MIXER||e.kind()==ScenePropEntity.CAKE_WORKPIECE||e.kind()==ScenePropEntity.CAKE_KITCHEN)&&s.owner.equals(e.owner)))remove.add(e);
        remove.forEach(Entity::discard);
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        var c=center(p,s);build(level,c);clean(level,s,c);removeCake(p);
        var l=s.loop;long now=OtherMotherLoop.now(p);
        l.cakeStage=0;l.cakeVoiceUntil=now+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.CAKE_MAKING_CONTEST));
        l.cakeCountdownUntil=0;l.cakeBakeUntil=0;l.cakeStepAt=0;l.cakeJudgeUntil=0;l.cakeAnnounceAt=0;
        l.cakeStep=0;l.cakeScore=0;l.cakeMistakes=0;l.cakeRank=0;l.cakeAnnounceIndex=0;l.cakeTaken=false;l.cakeSubmitted=false;
        l.cakeIngredients=0;l.cakeMixCount=0;l.cakeFlavor=-1;l.cakeIcing=-1;l.cakeTopper=-1;l.cakeOvenReadyAt=0;
        l.cakeBase=-1;l.cakeFilling=-1;l.cakeFrosting=-1;l.cakeLayers=1;l.cakeToppings=0;l.cakePiping=-1;l.cakeExtra=-1;
        l.cakeFrostCoverage=0;l.cakeFrostQuality=0;l.cakeDecorationSlots=0;
        l.cakeMixQuality=0;l.cakeBakeQuality=0;l.cakeMixStartedAt=0;l.cakeOvenStartedAt=0;
        l.cakeSeed=p.getRandom().nextLong();l.cakeLastActionAt=0;
        ensureFather(p,s,level,c);ensureVillagers(p,s,level,c);ensureSeat(p,s,level,c);ensureStations(level,s,c);CakeMaking.install(level,s,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        level.setWeatherParameters(6000,0,false,false);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
            net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
            net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0));
        LoopProps.hold(level,c);LoopProps.hold(level,c.north(24));build(level,c);upgradeRacks(level,c);
        ensureFather(p,s,level,c);ensureVillagers(p,s,level,c);ensureSeat(p,s,level,c);ensureStations(level,s,c);CakeMaking.install(level,s,c);
    }
    private static void upgradeRacks(ServerLevel level,BlockPos c){
        for(int i=0;i<5;i++)for(int z:new int[]{-2,0,2}){
            var at=c.offset((i-2)*8+2,0,z);
            var old=level.getBlockState(at);
            if(old.is(Blocks.BROWN_GLAZED_TERRACOTTA)||old.is(Blocks.YELLOW_GLAZED_TERRACOTTA)||old.is(Blocks.MAGENTA_GLAZED_TERRACOTTA))
                ActivityBlocks.set(level,at,(z==-2?ModBlocks.CAKE_COCOA_RACK.get():z==0?ModBlocks.CAKE_VANILLA_RACK.get():ModBlocks.CAKE_BLUEBERRY_RACK.get()).defaultBlockState(),2);
        }
    }
    private static void ensureStations(ServerLevel level,VisitData.Scene s,BlockPos c){
        if(LoopProps.nearby(level,s,c).stream().noneMatch(e->e.kind()==ScenePropEntity.CAKE_MIXER))
            LoopProps.spawn(level,s,ScenePropEntity.CAKE_MIXER,new Vec3(c.getX()+.5,c.getY()+.55,c.getZ()+.5),-1);
        // Retire the old giant hologram labels, including those saved in older worlds.
        for(var label:level.getEntitiesOfClass(ArmorStand.class,new AABB(c).inflate(6,5,7),
            e->e.getPersistentData().getBoolean("CoralineCakeLabel")&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))label.discard();
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        if(!level.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(25)).inflate(8),e->e.getPersistentData().getBoolean("CoralineCakeFather")&&e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var family=ParentAccess.family(p.server,s.owner);
        var father=ModEntities.DAD.get().create(level);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);
        father.getPersistentData().putBoolean("CoralineCakeFather",true);father.getPersistentData().putUUID(OWNER,s.owner);
        father.getPersistentData().putUUID("RealmDoor",s.door);father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+25.5);father.setYRot(90);father.setYBodyRot(90);level.addFreshEntity(father);
    }
    private static void ensureVillagers(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        for(var old:level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(39,8,39),v->isContestNpc(v)&&"Audience".equals(v.getPersistentData().getString(ROLE))))old.discard();
        var random=RandomSource.create(s.loop.cakeSeed);
        for(int i=0;i<AUDIENCE;i++){
            int side=i%2==0?-1:1,x=side*(26+random.nextInt(9)),z=-30+random.nextInt(61);
            spawnAudience(level,s,c.offset(x,0,z),i);
        }
        for(int i=0;i<4;i++)spawnNpc(level,s,c.offset(CHEF_X[i],0,22),"Chef",i,false);
        for(int i=0;i<3;i++)spawnNpc(level,s,c.offset((i-1)*4,1,-29),"Judge",i,true);
    }
    private static void spawnAudience(ServerLevel level,VisitData.Scene s,BlockPos at,int slot){
        if(!level.getEntitiesOfClass(Villager.class,new AABB(at).inflate(90,8,90),v->isContestNpc(v)&&v.getPersistentData().getInt("CakeSlot")==slot&&v.getPersistentData().getInt(GEN)==s.loop.generation&&s.owner.equals(v.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var v=net.minecraft.world.entity.EntityType.VILLAGER.create(level);if(v==null)return;
        v.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);v.setPersistenceRequired();v.setInvulnerable(true);
        v.setCustomName(Component.literal(VillageActivity.RESIDENT_NAME));v.setCustomNameVisible(false);
        var n=v.getPersistentData();n.putBoolean("CoralineDecoration",true);n.putBoolean(NPC,true);n.putUUID(OWNER,s.owner);n.putInt(GEN,s.loop.generation);n.putString(ROLE,"Audience");n.putInt("CakeSlot",slot);
        level.addFreshEntity(v);
    }
    private static void spawnNpc(ServerLevel level,VisitData.Scene s,BlockPos at,String role,int slot,boolean still){
        // Match the assigned slot across the whole arena, not just its spawn point:
        // wandering spectators must never be cloned when a different NPC is missing.
        if(!level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(at).inflate(90,8,90),v->isContestNpc(v)&&role.equals(v.getPersistentData().getString(ROLE))&&v.getPersistentData().getInt("CakeSlot")==slot&&v.getPersistentData().getInt(GEN)==s.loop.generation&&s.owner.equals(v.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var v=ModEntities.FISHING_CONTEST_NPC.get().create(level);if(v==null)return;
        int skin=Math.floorMod((int)(s.loop.cakeSeed>>>(slot%8*7))+role.hashCode(),64);
        v.configure(skin,slot%3==0,role.equals("Judge"),slot);v.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        v.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);v.setPersistenceRequired();v.setNoAi(still);
        v.setCustomName(Component.literal(role.equals("Chef")?CHEFS[slot]:role.equals("Judge")?"Button-Eyed Judge":"Festival Guest"));v.setCustomNameVisible(!role.equals("Audience"));
        if(role.equals("Judge")){v.setYRot(0);v.setYBodyRot(0);v.setYHeadRot(0);}
        var n=v.getPersistentData();n.putBoolean("CoralineDecoration",true);n.putBoolean(NPC,true);n.putUUID(OWNER,s.owner);n.putInt(GEN,s.loop.generation);n.putString(ROLE,role);n.putInt("CakeSlot",slot);
        level.addFreshEntity(v);
    }
    private static void ensureSeat(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        if(LoopProps.nearby(level,s,c.north(14)).stream().anyMatch(e->e.kind()==ScenePropEntity.CHAIR&&e.slot==42))return;
        var chair=LoopProps.spawn(level,s,ScenePropEntity.CHAIR,Vec3.atBottomCenterOf(c.north(14)),42);chair.setYRot(180);
    }
    public static boolean isContestNpc(Entity e){return (e instanceof FishingContestNpcEntity||e instanceof Villager)&&e.getPersistentData().getBoolean(NPC);}
    private static void chefsToStands(ServerLevel level,VisitData.Scene s,BlockPos c,boolean freeze){
        for(var v:chefs(level,s,c)){
            int slot=v.getPersistentData().getInt("CakeSlot");var at=c.offset(CHEF_X[slot],0,3);
            if(freeze){v.getNavigation().stop();v.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);v.setNoAi(true);v.setYRot(180);v.setYBodyRot(180);v.setYHeadRot(180);v.setBaking(s.loop.cakeStage<=3);}
            else if(v.distanceToSqr(Vec3.atBottomCenterOf(at))>2)v.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.6);
        }
    }
    private static java.util.List<FishingContestNpcEntity> chefs(ServerLevel level,VisitData.Scene s,BlockPos c){
        return level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(35,8,35),v->isContestNpc(v)&&"Chef".equals(v.getPersistentData().getString(ROLE))&&s.owner.equals(v.getPersistentData().getUUID(OWNER))&&v.getPersistentData().getInt(GEN)==s.loop.generation);
    }
    private static void chefsToPanel(ServerLevel level,VisitData.Scene s,BlockPos c,long now){
        for(var v:chefs(level,s,c)){
            v.setBaking(false);
            int slot=v.getPersistentData().getInt("CakeSlot"),x=CHEF_X[slot];var at=c.offset(x<0?x/2:x/2,0,-19);
            if(v.getPersistentData().getBoolean("CakePresented"))continue;
            v.setNoAi(false);
            if(v.distanceToSqr(Vec3.atBottomCenterOf(at))>3&&now-s.loop.cakeBakeUntil<200){
                double dx=at.getX()+.5-v.getX(),dz=at.getZ()+.5-v.getZ();float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
                v.setYRot(yaw);v.setYBodyRot(yaw);v.setYHeadRot(yaw);
                v.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.75);continue;
            }
            v.getNavigation().stop();v.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);v.setNoAi(true);v.setYRot(180);v.setYBodyRot(180);v.setYHeadRot(180);
            showCake(level,s,c.offset(x/2,1,-22),100+slot,style(slot, s.loop.cakeSeed),rivalDesign(slot,s.loop.cakeSeed));
            v.getPersistentData().putBoolean("CakePresented",true);
        }
    }
    private static int style(int slot,long seed){return Math.floorMod((int)(seed>>>(slot*7)),3)|Math.floorMod(slot+1,3)<<2|Math.floorMod(slot+2,3)<<4;}
    private static net.minecraft.nbt.CompoundTag rivalDesign(int slot,long seed){
        var n=new net.minecraft.nbt.CompoundTag();int base=Math.floorMod((int)(seed>>>(slot*9)),CakeMaking.BASE.length);
        n.putInt("Base",base);n.putInt("Layers",2+slot%2);n.putInt("Filling",(base+slot)%CakeMaking.FILLING.length);
        n.putInt("Frosting",(base+slot*2)%CakeMaking.FROSTING.length);n.putInt("FrostCoverage",4);
        n.putInt("Toppings",(1<<((base+slot)%CakeMaking.TOPPING.length))|(1<<((base+slot+3)%CakeMaking.TOPPING.length)));
        n.putInt("Piping",slot%3);n.putInt("Topper",(base+slot)%CakeMaking.TOPPER.length);return n;
    }
    private static void showCake(ServerLevel level,VisitData.Scene s,BlockPos at,int slot,int style){
        showCake(level,s,at,slot,style,new net.minecraft.nbt.CompoundTag());
    }
    private static void showCake(ServerLevel level,VisitData.Scene s,BlockPos at,int slot,int style,net.minecraft.nbt.CompoundTag design){
        for(var existing:level.getEntitiesOfClass(ScenePropEntity.class,new AABB(at).inflate(1),e->e.kind()==ScenePropEntity.CONTEST_CAKE&&e.slot==slot&&s.owner.equals(e.owner)))existing.discard();
        var cake=LoopProps.spawn(level,s,ScenePropEntity.CONTEST_CAKE,Vec3.atBottomCenterOf(at),slot);cake.cakeStyle(style);cake.cakeData(design);
    }
    private static void audienceWander(ServerLevel level,VisitData.Scene s,BlockPos c,long now){
        for(var v:level.getEntitiesOfClass(Villager.class,new AABB(c).inflate(39,8,39),e->isContestNpc(e)&&"Audience".equals(e.getPersistentData().getString(ROLE)))){
            if(v.isPassenger())continue;
            int slot=v.getPersistentData().getInt("CakeSlot"),side=slot%2==0?-1:1;
            if(Math.abs(v.getX()-c.getX())<23){v.teleportTo(c.getX()+side*27+.5,c.getY(),v.getZ());v.getNavigation().stop();}
            if(Math.floorMod(now+slot*43,100)==0){
                int x=side*(26+Math.floorMod(slot*13+(int)(now/100)*7,10));
                int z=-30+Math.floorMod(slot*19+(int)(now/100)*11,61);
                v.getNavigation().moveTo(c.getX()+x+.5,c.getY(),c.getZ()+z+.5,.75);
            }
        }
    }
    private static void seatAudience(ServerLevel level,VisitData.Scene s,BlockPos c){
        for(var v:level.getEntitiesOfClass(Villager.class,new AABB(c).inflate(39,8,39),e->isContestNpc(e)&&"Audience".equals(e.getPersistentData().getString(ROLE)))){
            if(v.isPassenger())continue;
            int slot=v.getPersistentData().getInt("CakeSlot");var at=audienceSeat(c,slot);
            v.getNavigation().stop();v.setNoAi(true);v.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);v.setYRot(180);v.setYBodyRot(180);v.setYHeadRot(180);
            ActivityBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
            var chair=LoopProps.spawn(level,s,ScenePropEntity.CHAIR,Vec3.atBottomCenterOf(at),200+slot);chair.setYRot(180);v.startRiding(chair,true);
        }
    }
    private static void lockToStand(ServerPlayer p,BlockPos c){
        // Give the player the entire walkable booth, including the pantry,
        // mixer, oven and decorating counter. Only crossing the canopy edge
        // brings them back in.
        double x=p.getX()-c.getX()-.5,z=p.getZ()-c.getZ()-.5;
        if(Math.abs(x)>2.85||z< -3.4||z>4.45||p.getY()<c.getY()-.5||p.getY()>c.getY()+2.7){
            p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+3.5,p.getYRot(),p.getXRot());p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        }
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var l=s.loop;long now=OtherMotherLoop.now(p);var level=p.serverLevel();
        if(p.tickCount%20==0)prepare(p,s,level,c);
        if(p.getY()<78||Math.abs(p.getX()-(c.getX()+.5))>38||Math.abs(p.getZ()-(c.getZ()+.5))>38){
            p.teleportTo(level,c.getX()+.5,c.getY(),c.getZ()+25.5,180,0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        }
        if(l.cakeStage<6&&p.tickCount%20==0)audienceWander(level,s,c,now);
        switch(l.cakeStage){
            case 0->{if(now>=l.cakeVoiceUntil){l.cakeStage=1;OtherMotherLoop.changed(p);p.sendSystemMessage(Component.literal("Father: Your stand is the middle pink-and-white pavilion. Go there to begin!"));}}
            case 1->{
                if(p.tickCount%20==0)chefsToStands(level,s,c,false);
                if(p.distanceToSqr(Vec3.atBottomCenterOf(c.south(3)))<6.25){
                    chefsToStands(level,s,c,true);l.cakeStage=2;l.cakeCountdownUntil=now+60;OtherMotherLoop.changed(p);
                    p.displayClientMessage(Component.literal("Baking starts in 3 seconds"),true);
                }else if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Go to the middle baking stand."),true);
            }
            case 2->{
                lockToStand(p,c);
                if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Baking begins in "+Math.max(1,(l.cakeCountdownUntil-now+19)/20)+"..."),true);
                if(now>=l.cakeCountdownUntil){l.cakeStage=3;l.cakeBakeUntil=now+BAKE_TICKS;l.cakeStepAt=now;OtherMotherLoop.changed(p);
                    VisitAudio.play(p,"cake_baking",true);
                    p.getPersistentData().remove("CakeHintUntil");
                    p.displayClientMessage(Component.literal("Choose a recipe card on the right"),true);CakeContestNetwork.send(p,s,false);}
            }
            case 3->{
                lockToStand(p,c);
                CakeMaking.tick(p,s,c,now);
                if(p.tickCount%5==0){timer(p,l.cakeBakeUntil-now);CakeMaking.guidance(p,s,now);}
                if(now>=l.cakeBakeUntil){finishBake(p,s,c,false);
                }
            }
            case 4->{
                if(p.tickCount%20==0)chefsToPanel(level,s,c,now);
                if(p.tickCount%10==0){
                    CakeMaking.objective(p,l.cakeTaken?"Right-click the sparkling judging table to place your cake":"Right-click your finished cake on the front stand");
                    var target=l.cakeTaken?c.north(22):c.south(2);
                    level.sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,target.getX()+.5,target.getY()+1.3,target.getZ()+.5,8,.35,.15,.35,.01);
                }
            }
            case 5->{
                if(p.tickCount%20==0)chefsToPanel(level,s,c,now);
                if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Right-click the rose velvet chair in front of the judges to sit."),true);
            }
            case 6->{
                if(now>=l.cakeJudgeUntil){l.cakeStage=7;l.cakeAnnounceAt=now+20;l.cakeAnnounceIndex=0;l.cakeRank=rank(s);
                    OtherMotherLoop.changed(p);p.sendSystemMessage(Component.literal("The judges have decided. Results, from worst to best..."));}
                else if(p.tickCount%20==0)p.displayClientMessage(Component.literal("The judges are deciding... "+Math.max(1,(l.cakeJudgeUntil-now+19)/20)+"s"),true);
            }
            case 7->{if(now>=l.cakeAnnounceAt){
                announce(p,s);l.cakeAnnounceIndex++;l.cakeAnnounceAt=now+24;OtherMotherLoop.changed(p);
                if(l.cakeAnnounceIndex>=5){givePrize(p,s);l.cakeStage=8;p.stopRiding();OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
                    p.sendSystemMessage(Component.literal("Your prize is yours. The return portal has opened behind the audience."));}
            }}
            default->{}
        }
    }
    private static String instruction(OtherMotherLoopState l,long now){return switch(l.cakeStep){
        case 0->"LEFT: flour/eggs/milk | RIGHT: flavor rack | CENTER: mix bowl";
        case 1->"Stir at the marble mixer ("+l.cakeMixCount+"/3)";
        case 2->l.cakeOvenReadyAt==0?"Right-click the OVEN behind the bowl":"Bake: "+Math.max(0,(l.cakeOvenReadyAt-now+19)/20)+"s; then click oven again";
        default->"RIGHT: icing rack + topper sideboard | CENTER: finish cake";
    };}
    static int targetFor(OtherMotherLoopState l){return 0;}
    public static void action(ServerPlayer p,int choice){p.displayClientMessage(Component.literal("Use the ingredient baskets and baking machines in your booth."),true);}
    static void finishBake(ServerPlayer p,VisitData.Scene s,BlockPos c,boolean early){
        VisitAudio.stopMusic(p,"cake_baking");
        var l=s.loop;if(l.cakeStage!=3)return;
        clearTimer(p);
        CakeMaking.complete(p,s,c,early);
        l.cakeStage=4;l.cakeStep=STEPS;OtherMotherLoop.changed(p);CakeContestNetwork.send(p,s,false);
        for(var chef:chefs(p.serverLevel(),s,c)){chef.setBaking(false);chef.getNavigation().stop();}
        showCake(p.serverLevel(),s,c.offset(0,1,2),99,cakeStyle(l),CakeMaking.design(l));removeIngredients(p,s);
        p.displayClientMessage(Component.literal("Cake complete! Bring it to the judging table."),true);
    }
    private static int cakeStyle(OtherMotherLoopState l){return l.cakeFlavor|(l.cakeIcing<<2)|(l.cakeTopper<<4);}
    private static void removeIngredients(ServerPlayer p,VisitData.Scene s){for(var stack:p.getInventory().items)if(stack.hasTag()&&stack.getTag().getBoolean("CoralineCakeIngredient")&&stack.getTag().hasUUID(OWNER)&&p.getUUID().equals(stack.getTag().getUUID(OWNER)))stack.setCount(0);p.getInventory().setChanged();}
    private static void takeIngredient(ServerPlayer p,VisitData.Scene s,int ingredient){
        var l=s.loop;if((l.cakeIngredients&(1<<ingredient))!=0){p.displayClientMessage(Component.literal("Already packed this ingredient."),true);return;}
        var stack=new ItemStack(ingredient==0?Items.WHEAT:ingredient==1?Items.EGG:Items.MILK_BUCKET);
        stack.setHoverName(Component.literal(ingredient==0?"Contest Flour":ingredient==1?"Contest Eggs":"Contest Milk"));
        stack.getOrCreateTag().putBoolean("CoralineCakeIngredient",true);stack.getTag().putUUID(OWNER,p.getUUID());stack.getTag().putInt(GEN,l.generation);
        if(!p.getInventory().add(stack)){p.displayClientMessage(Component.literal("Make room for baking ingredients."),true);return;}
        l.cakeIngredients|=1<<ingredient;l.cakeScore+=7;OtherMotherLoop.changed(p);
        p.displayClientMessage(Component.literal("Picked up "+stack.getHoverName().getString()+". Ingredients "+Integer.bitCount(l.cakeIngredients)+"/3"),true);
    }
    public static boolean interactBlock(ServerPlayer p,BlockPos at){
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.CAKE_MAKING_CONTEST||!ActivityWorld.isActivity(p.level()))return false;
        var l=s.loop;var c=center(p,s);long now=OtherMotherLoop.now(p);
        if(l.cakeStage==3)return CakeMaking.interact(p,s,c,at,now);
        /* Legacy station flow removed. Keep stage four's pickup and the
           existing judging handoff below entirely unchanged. */
        /*if(l.cakeStage==3){
            if(p.distanceToSqr(Vec3.atCenterOf(at))>36)return false;
            if(at.getX()==c.getX()-2&&at.getY()==c.getY()&&Math.abs(at.getZ()-c.getZ())<=2&&Math.floorMod(at.getZ()-c.getZ(),2)==0){
                takeIngredient(p,s,(at.getZ()-c.getZ()+2)/2);return true;
            }
            if(at.getX()==c.getX()+2&&at.getY()==c.getY()&&Math.abs(at.getZ()-c.getZ())<=2&&Math.floorMod(at.getZ()-c.getZ(),2)==0){
                int choice=(at.getZ()-c.getZ()+2)/2;
                if(l.cakeStep==0){l.cakeFlavor=choice;p.sendSystemMessage(Component.literal("Batter flavor: "+new String[]{"chocolate","vanilla","berry"}[choice]));}
                else if(l.cakeStep==3){l.cakeIcing=choice;p.sendSystemMessage(Component.literal("Icing: "+new String[]{"cream","blueberry","rose"}[choice]));}
                else p.displayClientMessage(Component.literal("Finish mixing and baking first."),true);
                OtherMotherLoop.changed(p);return true;
            }
            if(at.equals(c.offset(2,0,-1))){
                if(l.cakeStep==3){l.cakeTopper=(l.cakeTopper+1)%3;OtherMotherLoop.changed(p);
                    p.sendSystemMessage(Component.literal("Topper: "+new String[]{"berries","golden sugar","flowers"}[l.cakeTopper]+". Click again to change."));}
                else p.displayClientMessage(Component.literal("Decorate after baking."),true);
                return true;
            }
            if(at.equals(c.north(2))){
                if(l.cakeStep==2){
                    if(l.cakeOvenReadyAt==0){l.cakeOvenReadyAt=now+160;l.cakeScore+=10;OtherMotherLoop.changed(p);
                        p.sendSystemMessage(Component.literal("Batter is in the oven. Listen for the chime in eight seconds, then open it."));}
                    else if(now>=l.cakeOvenReadyAt){l.cakeStep=3;l.cakeScore+=16;l.cakeStepAt=now;OtherMotherLoop.changed(p);
                        p.sendSystemMessage(Component.literal("Cake baked! Pick an icing at the right flavor rack, cycle a topper at the sideboard, then finish at the mixer."));}
                    else p.displayClientMessage(Component.literal("The cake needs "+Math.max(1,(l.cakeOvenReadyAt-now+19)/20)+" more seconds in the oven."),true);
                }else p.displayClientMessage(Component.literal(instruction(l,now)),true);
                return true;
            }
            if(at.equals(c)){
                if(l.cakeStep==0){
                    if(l.cakeIngredients!=7||l.cakeFlavor<0){p.displayClientMessage(Component.literal("Collect flour, eggs and milk, and choose a flavor first."),true);return true;}
                    removeIngredients(p,s);l.cakeStep=1;l.cakeMixCount=0;l.cakeStepAt=now;l.cakeScore+=8;OtherMotherLoop.changed(p);
                    p.sendSystemMessage(Component.literal("Ingredients combined. Stir the mixer three times, leaving a beat between stirs."));
                }else if(l.cakeStep==1){
                    if(now-l.cakeStepAt<8){p.displayClientMessage(Component.literal("Give the batter a moment between stirs."),true);return true;}
                    l.cakeMixCount++;l.cakeScore+=7;l.cakeStepAt=now;OtherMotherLoop.changed(p);
                    p.serverLevel().playSound(null,c,SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.PLAYERS,.5F,1F+l.cakeMixCount*.12F);
                    if(l.cakeMixCount>=3){l.cakeStep=2;p.sendSystemMessage(Component.literal("Batter ready! Put it into the oven behind the mixer."));}
                    else p.displayClientMessage(Component.literal("Mixing "+l.cakeMixCount+"/3"),true);
                }else if(l.cakeStep==3){
                    if(l.cakeIcing<0||l.cakeTopper<0){p.displayClientMessage(Component.literal("Choose an icing and a topper first."),true);return true;}
                    l.cakeScore+=22;finishBake(p,s,c,true);
                }else p.displayClientMessage(Component.literal(instruction(l,now)),true);
                return true;
            }
        }*/
        if(at.equals(c)||at.equals(c.south(2))){
            if(l.cakeStage==4&&!l.cakeTaken){
                var cake=new ItemStack(ModItems.CONTEST_CAKE.get());cake.getOrCreateTag().putUUID(OWNER,p.getUUID());cake.getTag().putInt(GEN,l.generation);cake.getTag().putInt("CakeScore",l.cakeScore);
                cake.getTag().putInt("CakeStyle",cakeStyle(l));cake.getTag().put("CakeDesign",CakeMaking.design(l));
                cake.setHoverName(Component.literal(CakeMaking.baseName(l.cakeBase)+" Festival Cake"));
                if(!p.getInventory().add(cake)){p.displayClientMessage(Component.literal("Make room in your inventory for your contest cake."),true);return true;}
                l.cakeTaken=true;for(var prop:LoopProps.nearby(p.serverLevel(),s,c))if(prop.kind()==ScenePropEntity.CONTEST_CAKE&&prop.slot==99)prop.discard();OtherMotherLoop.changed(p);
                p.displayClientMessage(Component.literal("Place cake on the glowing judging table"),true);
            }
            return true;
        }
        if(at.equals(c.north(22))){
            if(l.cakeStage==4&&l.cakeTaken&&!l.cakeSubmitted){
                var stack=findCake(p,s);if(stack==null){p.displayClientMessage(Component.literal("Bring the cake you baked at your stand."),true);return true;}
                int style=stack.getTag().getInt("CakeStyle");var design=stack.getTag().getCompound("CakeDesign");stack.shrink(1);l.cakeSubmitted=true;l.cakeStage=5;OtherMotherLoop.changed(p);
                showCake(p.serverLevel(),s,c.offset(0,1,-22),99,style,design);
                p.displayClientMessage(Component.literal("Sit in the rose velvet judging chair"),true);
            }return true;
        }
        return false;
    }
    private static ItemStack findCake(ServerPlayer p,VisitData.Scene s){
        for(var stack:p.getInventory().items)if(cakeMatches(stack,p,s))return stack;
        for(var stack:p.getInventory().offhand)if(cakeMatches(stack,p,s))return stack;
        return null;
    }
    private static boolean cakeMatches(ItemStack stack,ServerPlayer p,VisitData.Scene s){return stack.is(ModItems.CONTEST_CAKE.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER)&&p.getUUID().equals(stack.getTag().getUUID(OWNER))&&stack.getTag().getInt(GEN)==s.loop.generation;}
    static void removeCake(ServerPlayer p){
        for(var stack:p.getInventory().items)if(stack.is(ModItems.CONTEST_CAKE.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER))stack.setCount(0);
        for(var stack:p.getInventory().offhand)if(stack.is(ModItems.CONTEST_CAKE.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER))stack.setCount(0);
        p.getInventory().setChanged();
    }
    public static boolean sit(ServerPlayer p,VisitData.Scene s,ScenePropEntity chair){
        if(s.loop.activity!=OtherMotherActivities.Activity.CAKE_MAKING_CONTEST||s.loop.cakeStage!=5||chair.kind()!=ScenePropEntity.CHAIR||chair.slot!=42)return false;
        if(!p.startRiding(chair,true))return false;
        chefsToPanel(p.serverLevel(),s,center(p,s),OtherMotherLoop.now(p)+200);
        seatAudience(p.serverLevel(),s,center(p,s));
        var l=s.loop;l.cakeStage=6;l.cakeJudgeUntil=OtherMotherLoop.now(p)+200;OtherMotherLoop.changed(p);
        p.sendSystemMessage(Component.literal("All five cakes are on display. The judges will deliberate for ten seconds."));return true;
    }
    @SubscribeEvent public static void preventEarlyDismount(EntityMountEvent event){
        if(!event.isDismounting()||!(event.getEntityMounting() instanceof ServerPlayer p)||!(event.getEntityBeingMounted() instanceof ScenePropEntity chair)||chair.slot!=42)return;
        var s=OtherMotherLoop.active(p);if(s!=null&&s.loop.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST&&s.loop.cakeStage>=6&&s.loop.cakeStage<=7)event.setCanceled(true);
    }
    private static int[] scores(VisitData.Scene s){
        int[] values=new int[5];values[0]=s.loop.cakeScore;
        var random=RandomSource.create(s.loop.cakeSeed^0xCACE12345L);
        values[1]=55+random.nextInt(21);values[2]=60+random.nextInt(26);
        values[3]=45+random.nextInt(46);values[4]=65+random.nextInt(24);
        return values;
    }
    static int rank(VisitData.Scene s){int[] scores=scores(s);int better=0;for(int i=1;i<5;i++)if(scores[i]>scores[0])better++;return better+1;}
    private static void announce(ServerPlayer p,VisitData.Scene s){
        int[] values=scores(s),order={0,1,2,3,4};
        for(int i=0;i<5;i++)for(int j=i+1;j<5;j++)if(values[order[i]]>values[order[j]]||values[order[i]]==values[order[j]]&&order[i]==0){int t=order[i];order[i]=order[j];order[j]=t;}
        int contestant=order[s.loop.cakeAnnounceIndex],place=5-s.loop.cakeAnnounceIndex;
        p.sendSystemMessage(Component.literal(place+ordinal(place)+" place: "+(contestant==0?p.getName().getString():CHEFS[contestant-1])+" ("+values[contestant]+" points)"));
    }
    private static String ordinal(int n){return n==1?"st":n==2?"nd":n==3?"rd":"th";}
    private static void givePrize(ServerPlayer p,VisitData.Scene s){
        ItemStack[] loot=switch(s.loop.cakeRank){
            case 1->new ItemStack[]{new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),new ItemStack(Items.DIAMOND,8),new ItemStack(Items.NETHERITE_SCRAP,2)};
            case 2->new ItemStack[]{new ItemStack(Items.DIAMOND,5),new ItemStack(Items.GOLDEN_APPLE,2)};
            case 3->new ItemStack[]{new ItemStack(Items.EMERALD,8),new ItemStack(Items.DIAMOND,2)};
            case 4->new ItemStack[]{new ItemStack(Items.GOLD_INGOT,4),new ItemStack(Items.EMERALD,2)};
            default->new ItemStack[]{new ItemStack(Items.BREAD,4),new ItemStack(Items.SUGAR,8)};
        };
        for(var item:loot){if(!p.getInventory().add(item))p.drop(item,false);}
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,1F,1F);
        p.sendSystemMessage(Component.literal("You placed "+s.loop.cakeRank+ordinal(s.loop.cakeRank)+" and received your cake-contest prize!"));
    }
}
