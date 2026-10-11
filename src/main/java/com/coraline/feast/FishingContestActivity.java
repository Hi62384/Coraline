package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.Arrays;

/** A moonlit lake: timed catches, four real rivals, a judging ceremony and ranked prizes. */
@Mod.EventBusSubscriber(modid="coraline")
public final class FishingContestActivity {
    private static final String OWNER="CoralineFishingOwner",GEN="CoralineFishingGeneration",SLOT="CoralineFishingSlot";
    private static final String[] NAMES={"Mara","Rowan","Juniper","Silas"};
    private static final int[][] SPOTS={{-17,0},{17,0},{-7,-17},{7,-17}};
    public static final int DURATION=3*60*20;
    private FishingContestActivity(){}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).west(440);}
    static boolean exitReady(VisitData.Scene s){return s.loop.fishingStage==5;}
    private static void set(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.Block block){ActivityBlocks.set(level,pos,ActivityWorld.persistentLeaves(block),2);}
    static void build(ServerLevel level,BlockPos c){
        if(level.getBlockState(c.below(8)).is(Blocks.BLUE_GLAZED_TERRACOTTA))return;
        if(level.getBlockState(c.below(5)).is(Blocks.GREEN_GLAZED_TERRACOTTA))removeBuild(level,c);
        var random=RandomSource.create(0xF15A9L);
        for(int x=-44;x<=44;x++)for(int z=-44;z<=44;z++){
            double shoreline=(double)x*x/196+(double)z*z/144;
            boolean lake=shoreline<1+.06*Math.sin(x*.8+z*.35);
            var top=c.offset(x,-1,z);
            int depth=lake?Math.max(1,Math.min(4,(int)Math.ceil(4*(1-Math.sqrt(Math.min(1,shoreline)))))):0;
            // Continuous earth under the entire arena seals both the lake and
            // its sloping banks; no side of the bowl can expose the void.
            for(int y=-8;y<=-2;y++)set(level,c.offset(x,y,z),lake&&y==-1-depth?
                (Math.floorMod(x+z,5)==0?Blocks.CLAY:Blocks.GRAVEL):Blocks.DIRT);
            if(lake){
                for(int d=0;d<depth;d++)set(level,top.below(d),Blocks.WATER);
            }else{
                boolean shore=shoreline<1.34,walk=Math.abs(shoreline-2.55)<.13;
                set(level,top,shore?(Math.floorMod(x-z,4)==0?Blocks.MOSS_BLOCK:Blocks.SAND):walk?Blocks.COARSE_DIRT:Blocks.GRASS_BLOCK);
            }
            if(Math.abs(x)==44||Math.abs(z)==44){set(level,top,Blocks.STONE_BRICKS);set(level,top.above(),Blocks.SPRUCE_FENCE);}
        }
        // A broad dock for the player, and four narrower contestant piers.
        for(int z=11;z<=19;z++)for(int x=-2;x<=2;x++)set(level,c.offset(x,-1,z),Blocks.SPRUCE_PLANKS);
        for(int side:new int[]{-1,1}){
            for(int x=11;x<=19;x++)for(int z=-1;z<=1;z++)set(level,c.offset(side*x,-1,z),Blocks.SPRUCE_PLANKS);
            for(int z=-18;z<=-10;z++)for(int x=6;x<=8;x++)set(level,c.offset(side*x,-1,z),Blocks.SPRUCE_PLANKS);
            for(int z:new int[]{13,18}){set(level,c.offset(side*3,0,z),Blocks.SPRUCE_FENCE);set(level,c.offset(side*3,1,z),Blocks.LANTERN);}
            for(int x:new int[]{13,18}){set(level,c.offset(side*x,0,2),Blocks.SPRUCE_FENCE);set(level,c.offset(side*x,1,2),Blocks.LANTERN);}
        }
        for(int x:new int[]{-8,-6,6,8}){set(level,c.offset(x,0,-17),Blocks.SPRUCE_FENCE);set(level,c.offset(x,1,-17),Blocks.LANTERN);}
        // Willow-like groves, winding path, wildflowers and luminous lake plants.
        for(int[] tree:new int[][]{{-32,-29},{-30,23},{31,-27},{32,21},{-23,34},{23,35}}){
            var base=c.offset(tree[0],0,tree[1]);
            for(int y=0;y<5;y++)set(level,base.above(y),Blocks.STRIPPED_OAK_LOG);
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=3;dy<=6;dy++){
                double r=dx*dx+dz*dz+(dy-4.7)*(dy-4.7)*2;
                if(r<17&&Math.floorMod(dx*3+dz*5+dy,13)!=0)set(level,base.offset(dx,dy,dz),dy>=5&&Math.floorMod(dx+dz,3)==0?Blocks.FLOWERING_AZALEA_LEAVES:Blocks.AZALEA_LEAVES);
            }
            for(int dx:new int[]{-3,3})for(int dz:new int[]{-2,2})set(level,base.offset(dx,0,dz),Blocks.LANTERN);
        }
        for(int i=0;i<220;i++){
            int x=random.nextInt(81)-40,z=random.nextInt(81)-40;
            var at=c.offset(x,0,z);var below=level.getBlockState(at.below());
            if(below.is(Blocks.GRASS_BLOCK)&&level.isEmptyBlock(at))set(level,at,i%9==0?Blocks.FLOWERING_AZALEA:i%4==0?Blocks.ALLIUM:i%3==0?Blocks.BLUE_ORCHID:Blocks.PINK_TULIP);
        }
        for(int i=0;i<24;i++){
            int x=random.nextInt(25)-12,z=random.nextInt(21)-10;
            var at=c.offset(x,0,z);
            if(level.getBlockState(at.below()).is(Blocks.WATER)&&level.isEmptyBlock(at))set(level,at,Blocks.LILY_PAD);
        }
        // Judges face the lake from a raised timber pavilion south of the shore.
        for(int x=-15;x<=15;x++)for(int z=27;z<=39;z++)set(level,c.offset(x,0,z),Blocks.SPRUCE_PLANKS);
        for(int x=-15;x<=15;x++)set(level,c.offset(x,-1,26),Blocks.SPRUCE_STAIRS);
        for(int x:new int[]{-14,14})for(int z:new int[]{28,38}){
            for(int y=1;y<=5;y++)set(level,c.offset(x,y,z),Blocks.STRIPPED_SPRUCE_LOG);
            set(level,c.offset(x,6,z),Blocks.SEA_LANTERN);
        }
        for(int x=-15;x<=15;x++)for(int z=28;z<=38;z++)
            set(level,c.offset(x,6,z),Math.floorMod(x+z,7)==0?Blocks.SEA_LANTERN:Math.floorMod(x,3)==0?Blocks.LIGHT_BLUE_STAINED_GLASS:Blocks.SPRUCE_PLANKS);
        for(int x=-7;x<=7;x++)set(level,c.offset(x,1,32),Blocks.DARK_OAK_PLANKS);
        for(int x:new int[]{-6,0,6}){set(level,c.offset(x,1,30),Blocks.SEA_LANTERN);set(level,c.offset(x,2,30),Blocks.BLUE_CARPET);}
        for(int side:new int[]{-1,1})for(int z=25;z<=39;z+=3){
            set(level,c.offset(side*17,0,z),Blocks.FLOWERING_AZALEA);
            set(level,c.offset(side*20,0,z),Blocks.LANTERN);
        }
        for(int x=-5;x<=5;x++)for(int z=22;z<=26;z++)set(level,c.offset(x,-1,z),x==0&&z%2==0?Blocks.SEA_LANTERN:Blocks.COARSE_DIRT);
        set(level,c.below(8),Blocks.BLUE_GLAZED_TERRACOTTA);
    }
    static void removeBuild(ServerLevel level,BlockPos c){
        if(!level.getBlockState(c.below(5)).is(Blocks.GREEN_GLAZED_TERRACOTTA)&&
            !level.getBlockState(c.below(8)).is(Blocks.BLUE_GLAZED_TERRACOTTA))return;
        for(int x=-46;x<=46;x++)for(int z=-46;z<=46;z++)for(int y=-8;y<=7;y++){
            var at=c.offset(x,y,z);if(!level.isEmptyBlock(at))set(level,at,Blocks.AIR);
        }
    }
    static void clean(ServerLevel level,VisitData.Scene s,BlockPos c){
        var remove=new ArrayList<Entity>();
        for(var e:level.getEntitiesOfClass(Entity.class,new AABB(c).inflate(49,12,49),e->e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))remove.add(e);
        remove.forEach(Entity::discard);
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        var c=center(p,s);build(level,c);clean(level,s,c);removeIssued(p);
        var l=s.loop;long now=OtherMotherLoop.now(p);
        l.fishingStage=0;l.fishingScore=0;l.fishingCatches=0;l.fishingRank=0;l.fishingAnnounceIndex=0;
        Arrays.fill(l.fishingRivalScores,0);l.fishingVoiceUntil=now+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.FISHING_CONTEST));
        l.fishingEndAt=0;l.fishingWalkUntil=0;l.fishingJudgeUntil=0;l.fishingAnnounceAt=0;l.fishingLastNpcAt=0;l.fishingSeed=p.getRandom().nextLong();
        ensureFather(p,s,level,c);ensureNpcs(p,s,level,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        LoopProps.hold(level,c);LoopProps.hold(level,c.south(31));build(level,c);ensureFather(p,s,level,c);ensureNpcs(p,s,level,c);
        level.setWeatherParameters(120000,0,false,false);level.setRainLevel(0);level.setThunderLevel(0);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0));
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        if(!level.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(20)).inflate(8),e->e.getPersistentData().getBoolean("CoralineFishingFather")&&e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var family=ParentAccess.family(p.server,s.owner);
        var father=ModEntities.DAD.get().create(level);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);
        father.getPersistentData().putBoolean("CoralineFishingFather",true);father.getPersistentData().putUUID(OWNER,s.owner);
        father.getPersistentData().putUUID("RealmDoor",s.door);father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+4.5,c.getY(),c.getZ()+20.5);father.setYRot(90);father.setYBodyRot(90);level.addFreshEntity(father);
    }
    private static void ensureNpcs(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        for(int slot=0;slot<7;slot++){
            final int index=slot;
            var existing=level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(47,9,47),npc->owned(npc,s)&&npc.getPersistentData().getInt(SLOT)==index);
            if(!existing.isEmpty()){if(slot>=4)existing.forEach(npc->face(npc,180));continue;}
            var npc=ModEntities.FISHING_CONTEST_NPC.get().create(level);if(npc==null)continue;
            boolean judge=slot>=4,female=Math.floorMod((int)(s.loop.fishingSeed>>>slot),2)==0;
            int skin=Math.floorMod((int)(s.loop.fishingSeed>>(slot*7)),64);
            npc.configure(skin,female,judge,slot);npc.setNoAi(true);
            var at=judge?c.offset((slot-5)*5,1,33):c.offset(SPOTS[slot][0],0,SPOTS[slot][1]);
            npc.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);face(npc,judge?180:SPOTS[slot][0]<-10?-90:SPOTS[slot][0]>10?90:0);
            npc.setFishing(!judge&&s.loop.fishingStage==1);
            npc.setCustomName(Component.literal(judge?"Button-Eyed Judge":NAMES[slot]));npc.setCustomNameVisible(true);
            npc.getPersistentData().putUUID(OWNER,s.owner);npc.getPersistentData().putInt(GEN,s.loop.generation);npc.getPersistentData().putInt(SLOT,slot);
            npc.getPersistentData().putBoolean("CoralineDecoration",true);level.addFreshEntity(npc);
        }
    }
    private static boolean owned(FishingContestNpcEntity npc,VisitData.Scene s){return npc.getPersistentData().hasUUID(OWNER)&&s.owner.equals(npc.getPersistentData().getUUID(OWNER))&&npc.getPersistentData().getInt(GEN)==s.loop.generation;}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var l=s.loop;long now=OtherMotherLoop.now(p);var level=p.serverLevel();
        if(p.tickCount%20==0)prepare(p,s,level,c);
        if(p.getY()<c.getY()-8||Math.abs(p.getX()-(c.getX()+.5))>43||Math.abs(p.getZ()-(c.getZ()+.5))>43){
            p.teleportTo(level,c.getX()+.5,c.getY(),c.getZ()+20.5,180,0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        }
        switch(l.fishingStage){
            case 0->{if(now>=l.fishingVoiceUntil){l.fishingStage=1;l.fishingEndAt=now+DURATION;l.fishingLastNpcAt=now;
                grantRod(p,s);setContestantsFishing(level,s,c,true);OtherMotherLoop.changed(p);p.sendSystemMessage(Component.literal("Father's contest begins! Fish this lake for three minutes. Each catch adds its hidden value to your score."));}}
            case 1->{
                if(now>=l.fishingEndAt){l.fishingStage=2;l.fishingWalkUntil=now+400;removeIssued(p);setContestantsFishing(level,s,c,false);OtherMotherLoop.changed(p);
                    p.sendSystemMessage(Component.literal("Time! Go to the illuminated judging pavilion on the south bank. The other four contestants are bringing their catches."));
                }else{
                    if(now-l.fishingLastNpcAt>=260){npcCatches(p,s,c,now);l.fishingLastNpcAt=now;OtherMotherLoop.changed(p);}
                    if(p.tickCount%20==0){int seconds=(int)Math.max(0,(l.fishingEndAt-now+19)/20);
                        p.displayClientMessage(Component.literal("Fishing contest: "+seconds+"s | "+l.fishingCatches+" catches | value "+l.fishingScore),true);}
                }
            }
            case 2->{
                contestantsToPanel(level,s,c,now>=l.fishingWalkUntil-260);
                if(p.distanceToSqr(Vec3.atBottomCenterOf(c.offset(0,1,29)))<100||now>=l.fishingWalkUntil){
                    if(now>=l.fishingWalkUntil){p.teleportTo(level,c.getX()+.5,c.getY()+1,c.getZ()+29.5,180,0);p.setDeltaMovement(Vec3.ZERO);}
                    contestantsToPanel(level,s,c,true);l.fishingStage=3;l.fishingJudgeUntil=now+200;OtherMotherLoop.changed(p);
                    p.sendSystemMessage(Component.literal("All five hauls are before the button-eyed judges. They will compare their values for ten seconds."));
                }else if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Walk south to the glowing judging pavilion."),true);
            }
            case 3->{if(now>=l.fishingJudgeUntil){l.fishingRank=rank(l);l.fishingStage=4;l.fishingAnnounceAt=now+20;l.fishingAnnounceIndex=0;OtherMotherLoop.changed(p);
                    p.sendSystemMessage(Component.literal("The judges have decided. Rankings, from lowest value to highest..."));}
                else if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Judging catches... "+Math.max(1,(l.fishingJudgeUntil-now+19)/20)+"s"),true);}
            case 4->{if(now>=l.fishingAnnounceAt){announce(p,s);l.fishingAnnounceIndex++;l.fishingAnnounceAt=now+25;OtherMotherLoop.changed(p);
                if(l.fishingAnnounceIndex>=5){prize(p,s);l.fishingStage=5;OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
                    p.sendSystemMessage(Component.literal("Your ranked prize is yours. The return portal has opened behind the judges."));}}}
            default->{}
        }
    }
    private static void npcCatches(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var random=RandomSource.create(s.loop.fishingSeed^now*0x9E3779B97F4A7C15L);
        for(var npc:p.serverLevel().getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(40,8,40),e->owned(e,s)&&!e.judge())){
            int slot=npc.getPersistentData().getInt(SLOT),value=catchPoints(roll(random).value);
            s.loop.fishingRivalScores[slot]+=value;npc.swing(InteractionHand.MAIN_HAND);
            p.serverLevel().sendParticles(ParticleTypes.SPLASH,c.getX()+SPOTS[slot][0]*.75,c.getY(),c.getZ()+SPOTS[slot][1]*.75,8,.5,.1,.5,.02);
        }
    }
    private static void contestantsToPanel(ServerLevel level,VisitData.Scene s,BlockPos c,boolean force){
        for(var npc:level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(44,8,44),e->owned(e,s)&&!e.judge())){
            int slot=npc.getPersistentData().getInt(SLOT);var at=c.offset(-9+slot*6,1,28);
            if(force){npc.getNavigation().stop();npc.setNoAi(true);npc.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);face(npc,0);}
            else{npc.setNoAi(false);npc.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.85);}
        }
    }
    private static void face(FishingContestNpcEntity npc,float yaw){npc.setYRot(yaw);npc.setYBodyRot(yaw);npc.setYHeadRot(yaw);npc.yBodyRotO=yaw;npc.yHeadRotO=yaw;}
    private static void setContestantsFishing(ServerLevel level,VisitData.Scene s,BlockPos c,boolean fishing){
        for(var npc:level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(44,8,44),e->owned(e,s)&&!e.judge()))npc.setFishing(fishing);
    }
    private record Catch(ItemStack stack,int value){}
    private static Catch roll(RandomSource random){
        int n=random.nextInt(100);
        if(n<57)return switch(random.nextInt(7)){
            case 0->new Catch(new ItemStack(Items.STICK,2),2);
            case 1->new Catch(new ItemStack(Items.KELP,3),3);
            case 2->new Catch(new ItemStack(Items.ROTTEN_FLESH),1);
            case 3->new Catch(new ItemStack(Items.BOWL),1);
            case 4->new Catch(new ItemStack(Items.STRING,2),2);
            case 5->new Catch(new ItemStack(Items.LEATHER_BOOTS),3);
            default->new Catch(new ItemStack(Items.BONE,2),2);
        };
        if(n<94)return switch(random.nextInt(7)){
            case 0->new Catch(new ItemStack(Items.COD,2),8);
            case 1->new Catch(new ItemStack(Items.SALMON,2),10);
            case 2->new Catch(new ItemStack(Items.IRON_INGOT,2),14);
            case 3->new Catch(new ItemStack(Items.GOLD_INGOT),12);
            case 4->new Catch(new ItemStack(Items.EMERALD),18);
            case 5->new Catch(new ItemStack(Items.NAUTILUS_SHELL),22);
            default->new Catch(new ItemStack(Items.NAME_TAG),20);
        };
        return switch(random.nextInt(4)){
            case 0->new Catch(new ItemStack(Items.DIAMOND,2),60);
            case 1->new Catch(new ItemStack(Items.HEART_OF_THE_SEA),90);
            case 2->new Catch(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),110);
            default->new Catch(new ItemStack(Items.NETHERITE_SCRAP),75);
        };
    }
    @SubscribeEvent public static void caught(ItemFishedEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.FISHING_CONTEST||!ActivityWorld.isActivity(p.level()))return;
        var l=s.loop;long now=OtherMotherLoop.now(p);if(l.fishingStage!=1||now>=l.fishingEndAt)return;
        if(!issued(p.getMainHandItem(),p,s)&&!issued(p.getOffhandItem(),p,s))return;
        var hook=event.getHookEntity();var c=center(p,s);
        double dx=hook.getX()-c.getX()-.5,dz=hook.getZ()-c.getZ()-.5;
        if(dx*dx/196+dz*dz/144>1.1||!(hook.isInWater()||hook.level().getBlockState(hook.blockPosition()).is(Blocks.WATER)||hook.level().getBlockState(hook.blockPosition().below()).is(Blocks.WATER)))return;
        // Forge's event holds a copy of the vanilla loot list; editing it does
        // not replace the list FishingHook later spawns into the world.
        var reward=roll(p.getRandom());event.setCanceled(true);
        var awarded=reward.stack.copy();
        if(!p.getInventory().add(awarded))p.drop(awarded,false);
        int points=catchPoints(reward.value);l.fishingScore+=points;l.fishingCatches++;OtherMotherLoop.changed(p);
        p.sendSystemMessage(Component.literal("Caught "+reward.stack.getHoverName().getString()+" — "+points+" points (total "+l.fishingScore+")."));
    }
    static boolean issued(ItemStack stack,ServerPlayer p,VisitData.Scene s){return stack.is(ModItems.FISHING_CONTEST_ROD.get())&&stack.hasTag()&&
        stack.getTag().hasUUID(OWNER)&&p.getUUID().equals(stack.getTag().getUUID(OWNER))&&stack.getTag().getInt(GEN)==s.loop.generation;}
    public static boolean canFish(ServerPlayer p,ItemStack stack){var s=OtherMotherLoop.active(p);return s!=null&&s.loop.activity==OtherMotherActivities.Activity.FISHING_CONTEST&&
        ActivityWorld.isActivity(p.level())&&s.loop.fishingStage==1&&OtherMotherLoop.now(p)<s.loop.fishingEndAt&&issued(stack,p,s);}
    private static void grantRod(ServerPlayer p,VisitData.Scene s){
        var stack=new ItemStack(ModItems.FISHING_CONTEST_ROD.get());stack.getOrCreateTag().putUUID(OWNER,p.getUUID());stack.getTag().putInt(GEN,s.loop.generation);
        stack.enchant(Enchantments.FISHING_SPEED,4);
        stack.setHoverName(Component.literal("Father's Contest Rod"));
        if(!p.getInventory().add(stack))p.drop(stack,false);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.FISHING_BOBBER_THROW,SoundSource.PLAYERS,.8F,1.2F);
    }
    static void removeIssued(ServerPlayer p){
        for(var stack:p.getInventory().items)if(stack.is(ModItems.FISHING_CONTEST_ROD.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER)&&p.getUUID().equals(stack.getTag().getUUID(OWNER)))stack.setCount(0);
        for(var stack:p.getInventory().offhand)if(stack.is(ModItems.FISHING_CONTEST_ROD.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER)&&p.getUUID().equals(stack.getTag().getUUID(OWNER)))stack.setCount(0);
        p.getInventory().setChanged();
    }
    // Reeling in consistently matters more than a single lucky jackpot. Both
    // player and rivals use the identical scoring rule; loot itself is unchanged.
    static int catchPoints(int lootValue){return 10+Math.min(20,Math.max(0,lootValue));}
    static int rank(OtherMotherLoopState l){int better=0;for(int value:l.fishingRivalScores)if(value>l.fishingScore)better++;return better+1;}
    private static void announce(ServerPlayer p,VisitData.Scene s){
        var l=s.loop;int[] values={l.fishingScore,l.fishingRivalScores[0],l.fishingRivalScores[1],l.fishingRivalScores[2],l.fishingRivalScores[3]},order={0,1,2,3,4};
        for(int i=0;i<5;i++)for(int j=i+1;j<5;j++)if(values[order[i]]>values[order[j]]||values[order[i]]==values[order[j]]&&order[i]==0){int t=order[i];order[i]=order[j];order[j]=t;}
        int contestant=order[l.fishingAnnounceIndex],place=5-l.fishingAnnounceIndex;
        p.sendSystemMessage(Component.literal(place+ordinal(place)+" place: "+(contestant==0?p.getName().getString():NAMES[contestant-1])+" — "+values[contestant]+" catch value"));
    }
    private static String ordinal(int n){return n==1?"st":n==2?"nd":n==3?"rd":"th";}
    private static void prize(ServerPlayer p,VisitData.Scene s){
        ItemStack[] loot=switch(s.loop.fishingRank){
            case 1->new ItemStack[]{new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),new ItemStack(Items.DIAMOND,8),new ItemStack(Items.HEART_OF_THE_SEA)};
            case 2->new ItemStack[]{new ItemStack(Items.DIAMOND,5),new ItemStack(Items.NAUTILUS_SHELL,2)};
            case 3->new ItemStack[]{new ItemStack(Items.EMERALD,8),new ItemStack(Items.GOLD_INGOT,4)};
            case 4->new ItemStack[]{new ItemStack(Items.IRON_INGOT,6),new ItemStack(Items.COD,4)};
            default->new ItemStack[]{new ItemStack(Items.BREAD,4),new ItemStack(Items.KELP,8)};
        };
        for(var stack:loot){p.getInventory().add(stack);if(!stack.isEmpty())p.drop(stack,false);}
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,1F,1F);
        p.sendSystemMessage(Component.literal("You placed "+s.loop.fishingRank+ordinal(s.loop.fishingRank)+" in the fishing contest and received your prize!"));
    }
}
