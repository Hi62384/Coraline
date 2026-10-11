package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;

/** One bounded, persistent village per family's activity realm. Trades belong to the visit, not vanilla merchants. */
public final class VillageActivity {
    public static final int TOKEN_COUNT=20,VILLAGER_COUNT=48;
    public static final String RESIDENT_NAME="Other Village Resident";
    private VillageActivity(){}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){
        var r=RealmData.get(p.server).realms.get(s.door);
        return new BlockPos(r.doorPos.getX(),80,r.doorPos.getZ());
    }
    public static boolean exitReady(VisitData.Scene s){return true;}
    static void build(ServerLevel level,BlockPos c){
        if(level.getBlockState(c.below()).is(Blocks.PURPUR_BLOCK))return;
        // Replace the old 13x13 quartz placeholder, including its glass boundary.
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
            var p=c.offset(x,0,z);
            if(level.getBlockState(p).is(Blocks.GLASS))ActivityBlocks.set(level,p,Blocks.AIR.defaultBlockState(),2);
        }
        // A 100x100 two-layer island; there is no unsafe exposed hole beneath the streets.
        for(int x=-50;x<50;x++)for(int z=-50;z<50;z++){
            var ground=c.offset(x,-1,z);
            ActivityBlocks.set(level,ground.below(),Blocks.DIRT.defaultBlockState(),2);
            boolean road=Math.abs(x)<=2||Math.abs(z)<=2||Math.abs(Math.abs(x)-24)<=2||Math.abs(Math.abs(z)-24)<=2;
            boolean plaza=Math.abs(x)<=9&&Math.abs(z)<=9;
            ActivityBlocks.set(level,ground,(plaza?Blocks.POLISHED_DIORITE:road?Blocks.PACKED_MUD:Blocks.GRASS_BLOCK).defaultBlockState(),2);
            if(x==-49||x==49||z==-49||z==49){
                ActivityBlocks.set(level,ground.above(),Blocks.COBBLESTONE_WALL.defaultBlockState(),2);
                ActivityBlocks.set(level,ground.above(2),Blocks.COBBLESTONE_WALL.defaultBlockState(),2);
            }
        }
        for(int x:new int[]{-35,0,35})for(int z:new int[]{-35,0,35}){
            if(x==0&&z==0)continue;
            house(level,c.offset(x,0,z),Math.floorMod(x+z,3));
        }
        for(int x:new int[]{-14,14})for(int z:new int[]{-14,14})stall(level,c.offset(x,0,z),(x+z)>0);
        for(int x:new int[]{-43,43})for(int z:new int[]{-17,17})tree(level,c.offset(x,0,z));
        // A village bell makes vanilla villagers repeatedly gather at the plaza.
        ActivityBlocks.set(level,c.north(4),Blocks.SEA_LANTERN.defaultBlockState(),2);
        for(int x=-44;x<=44;x+=11)for(int z:new int[]{-44,44})lampPost(level,c.offset(x,0,z));
        // The marker is written last, so a partial/interrupted build is retried.
        ActivityBlocks.set(level,c.below(),Blocks.PURPUR_BLOCK.defaultBlockState(),2);
    }
    static void removeBuild(ServerLevel level,BlockPos c){
        if(!level.getBlockState(c.below()).is(Blocks.PURPUR_BLOCK))return;
        for(int x=-50;x<50;x++)for(int z=-50;z<50;z++)for(int y=-2;y<=7;y++){
            var at=c.offset(x,y,z);
            if(!level.isEmptyBlock(at))ActivityBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private static void house(ServerLevel level,BlockPos c,int style){
        var wall=(style==0?Blocks.CALCITE:style==1?Blocks.PINK_TERRACOTTA:Blocks.CHERRY_PLANKS).defaultBlockState();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            ActivityBlocks.set(level,c.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            if(Math.abs(x)==4||Math.abs(z)==4)for(int y=0;y<=3;y++)ActivityBlocks.set(level,c.offset(x,y,z),wall,2);
        }
        for(int x:new int[]{-4,4})for(int z:new int[]{-4,4})for(int y=0;y<=4;y++)
            ActivityBlocks.set(level,c.offset(x,y,z),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),2);
        for(int y=0;y<=1;y++)ActivityBlocks.set(level,c.offset(0,y,4),Blocks.AIR.defaultBlockState(),2);
        for(int x:new int[]{-4,4})for(int z:new int[]{-1,1})
            ActivityBlocks.set(level,c.offset(x,1,z),Blocks.PINK_STAINED_GLASS.defaultBlockState(),2);
        for(int x:new int[]{-2,2})ActivityBlocks.set(level,c.offset(x,1,-4),Blocks.PINK_STAINED_GLASS.defaultBlockState(),2);
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            int rise=Math.max(0,3-(Math.abs(x)+1)/2);
            for(int y=4;y<=4+rise;y++)ActivityBlocks.set(level,c.offset(x,y,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
        }
        ActivityBlocks.set(level,c.offset(-2,0,2),Blocks.BARREL.defaultBlockState(),2);
        ActivityBlocks.set(level,c.offset(2,0,-2),Blocks.BOOKSHELF.defaultBlockState(),2);
        ActivityBlocks.set(level,c.offset(0,0,-2),Blocks.CHERRY_STAIRS.defaultBlockState(),2);
        for(int x:new int[]{-2,2})ActivityBlocks.set(level,c.offset(x,0,5),Blocks.FLOWERING_AZALEA.defaultBlockState(),2);
    }
    private static void stall(ServerLevel level,BlockPos c,boolean pink){
        for(int x:new int[]{-3,3})for(int z:new int[]{-2,2})for(int y=0;y<=2;y++)
            ActivityBlocks.set(level,c.offset(x,y,z),Blocks.SPRUCE_FENCE.defaultBlockState(),2);
        for(int x=-3;x<=3;x++)for(int z=-2;z<=2;z++)
            ActivityBlocks.set(level,c.offset(x,3,z),(pink?Blocks.PINK_WOOL:Blocks.CYAN_WOOL).defaultBlockState(),2);
        for(int x=-2;x<=2;x++)ActivityBlocks.set(level,c.offset(x,0,1),Blocks.BARREL.defaultBlockState(),2);
        ActivityBlocks.set(level,c.offset(0,1,1),Blocks.FLOWER_POT.defaultBlockState(),2);
    }
    private static void tree(ServerLevel level,BlockPos c){
        for(int y=0;y<=4;y++)ActivityBlocks.set(level,c.above(y),Blocks.CHERRY_LOG.defaultBlockState(),2);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=3;y<=5;y++)
            if(Math.abs(x)+Math.abs(z)+Math.abs(y-4)<5)ActivityBlocks.set(level,c.offset(x,y,z),ActivityWorld.persistentLeaves(Blocks.CHERRY_LEAVES),2);
    }
    private static void lampPost(ServerLevel level,BlockPos c){
        for(int y=0;y<3;y++)ActivityBlocks.set(level,c.above(y),Blocks.DARK_OAK_FENCE.defaultBlockState(),2);
        ActivityBlocks.set(level,c.above(3),Blocks.SHROOMLIGHT.defaultBlockState(),2);
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        var c=center(p,s);
        build(level,c);
        clean(level,s,c);
        s.loop.villageTokensRemaining=TOKEN_COUNT;s.loop.villageIssued=false;s.loop.villageSpawned=false;
        s.loop.villageGiftAt=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.OTHER_VILLAGE));
        ensureFather(p,s,level,c);
        spawnVillagers(p,s,level,c);
        OtherMotherLoop.changed(p);
    }
    static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        var fathers=level.getEntitiesOfClass(ParentEntity.class,new AABB(c).inflate(8),e->e.getPersistentData().getBoolean("CoralineVillageFather")&&e.getPersistentData().hasUUID("CoralineVillageOwner")&&e.getPersistentData().getUUID("CoralineVillageOwner").equals(s.owner));
        if(!fathers.isEmpty())return;
        var r=RealmData.get(p.server).realms.get(s.door);var family=ParentAccess.family(p.server,s.owner);
        var father=ModEntities.DAD.get().create(level);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);
        father.getPersistentData().putBoolean("CoralineVillageFather",true);
        father.getPersistentData().putUUID("CoralineVillageOwner",s.owner);
        father.getPersistentData().putUUID("RealmDoor",s.door);
        father.getPersistentData().putInt("RealmGeneration",r.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+.5);father.setYRot(90);father.setYBodyRot(90);
        level.addFreshEntity(father);
    }
    static void spawnVillagers(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        if(s.loop.villageSpawned)return;
        int existing=level.getEntitiesOfClass(Villager.class,new AABB(c).inflate(48,12,48),v->v.getPersistentData().getBoolean("CoralineVillageVillager")&&v.getPersistentData().hasUUID("CoralineVillageOwner")&&s.owner.equals(v.getPersistentData().getUUID("CoralineVillageOwner"))&&v.getPersistentData().getInt("CoralineVillageGeneration")==s.loop.generation).size();
        var random=RandomSource.create(s.owner.getMostSignificantBits()^s.owner.getLeastSignificantBits()^s.loop.generation);
        int made=existing;
        // One resident per shuffled sector gives every district a crowd instead
        // of letting a random sequence (or the old bell) pack the center.
        var sectors=new java.util.ArrayList<int[]>();
        for(int sx=0;sx<8;sx++)for(int sz=0;sz<8;sz++)sectors.add(new int[]{sx,sz});
        java.util.Collections.shuffle(sectors,new java.util.Random(random.nextLong()));
        for(int tries=0;tries<2000&&made<VILLAGER_COUNT;tries++){
            int[] sector=sectors.get(tries%sectors.size());
            int x=-44+sector[0]*11+random.nextInt(11),z=-44+sector[1]*11+random.nextInt(11);
            if(Math.abs(x)<7&&Math.abs(z)<7)continue;
            var at=c.offset(x,0,z);
            if(!level.isEmptyBlock(at)||!level.isEmptyBlock(at.above())||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP))continue;
            var villager=EntityType.VILLAGER.create(level);if(villager==null)continue;
            villager.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);villager.setPersistenceRequired();
            // A hidden, synchronized name lets the client identify residents;
            // Forge persistent data alone is not sent with villager spawn packets.
            villager.setCustomName(Component.literal(RESIDENT_NAME));villager.setCustomNameVisible(false);
            villager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(.5);
            villager.getPersistentData().putBoolean("CoralineDecoration",true);
            villager.getPersistentData().putBoolean("CoralineVillageVillager",true);
            villager.getPersistentData().putUUID("CoralineVillageOwner",s.owner);
            villager.getPersistentData().putInt("CoralineVillageGeneration",s.loop.generation);
            villager.getPersistentData().putInt("CoralineVillageHomeX",at.getX());
            villager.getPersistentData().putInt("CoralineVillageHomeZ",at.getZ());
            if(level.addFreshEntity(villager))made++;
        }
        s.loop.villageSpawned=made==VILLAGER_COUNT;
        OtherMotherLoop.changed(p);
    }
    /** Let the vanilla villager Brain own walking; competing navigation commands caused speed bursts. */
    static void wander(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        int legacyIndex=0;
        for(var villager:level.getEntitiesOfClass(Villager.class,new AABB(c).inflate(48,12,48),v->
            isVillageVillager(v)&&v.getPersistentData().hasUUID("CoralineVillageOwner")&&
            s.owner.equals(v.getPersistentData().getUUID("CoralineVillageOwner"))&&
            v.getPersistentData().getInt("CoralineVillageGeneration")==s.loop.generation)){
            if(villager.getCustomName()==null||!RESIDENT_NAME.equals(villager.getCustomName().getString())){
                villager.setCustomName(Component.literal(RESIDENT_NAME));villager.setCustomNameVisible(false);
            }
            // Restore residents created by 1.15.7, which saved a slower base speed.
            villager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(.5);
            if(level.getBlockState(c.north(4)).is(Blocks.BELL))ActivityBlocks.set(level,c.north(4),Blocks.SEA_LANTERN.defaultBlockState(),2);
            var data=villager.getPersistentData();
            if(!data.contains("CoralineVillageHomeX")){
                int sector=Math.floorMod(legacyIndex++*23,64);
                int hx=c.getX()-40+(sector/8)*11,hz=c.getZ()-40+(sector%8)*11;
                BlockPos home=new BlockPos(hx,c.getY(),hz);
                for(int ox=-4;ox<=4;ox++)for(int oz=-4;oz<=4;oz++){
                    var candidate=home.offset(ox,0,oz);
                    if(level.isEmptyBlock(candidate)&&level.isEmptyBlock(candidate.above())&&
                        level.getBlockState(candidate.below()).isFaceSturdy(level,candidate.below(),net.minecraft.core.Direction.UP)){home=candidate;ox=5;break;}
                }
                data.putInt("CoralineVillageHomeX",home.getX());data.putInt("CoralineVillageHomeZ",home.getZ());
                villager.teleportTo(home.getX()+.5,home.getY(),home.getZ()+.5);
            }
            if(data.contains("CoralineVillageHomeX")&&data.contains("CoralineVillageHomeZ")){
                double dx=villager.getX()-(data.getInt("CoralineVillageHomeX")+.5),dz=villager.getZ()-(data.getInt("CoralineVillageHomeZ")+.5);
                if(dx*dx+dz*dz>22*22){
                    var home=new BlockPos(data.getInt("CoralineVillageHomeX"),c.getY(),data.getInt("CoralineVillageHomeZ"));
                    villager.getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,.65);
                }
            }
        }
    }
    static void clean(ServerLevel level,VisitData.Scene s,BlockPos c){
        var remove=new ArrayList<Entity>();
        for(var e:level.getEntitiesOfClass(Entity.class,new AABB(c).inflate(50,12,50),e->e.getPersistentData().hasUUID("CoralineVillageOwner")&&s.owner.equals(e.getPersistentData().getUUID("CoralineVillageOwner"))))remove.add(e);
        remove.forEach(Entity::discard);
    }
    static void removeTokens(ServerPlayer p){
        for(var stack:p.getInventory().items)if(stack.is(ModItems.VILLAGE_TOKEN.get()))stack.setCount(0);
        for(var stack:p.getInventory().offhand)if(stack.is(ModItems.VILLAGE_TOKEN.get()))stack.setCount(0);
        p.getInventory().setChanged();
    }
    static void grantTokens(ServerPlayer p,VisitData.Scene s){
        if(s.loop.villageIssued)return;
        removeTokens(p);
        var stack=new ItemStack(ModItems.VILLAGE_TOKEN.get(),TOKEN_COUNT);
        ActivityTokens.give(p,stack);
        s.loop.villageIssued=true;OtherMotherLoop.changed(p);
        p.sendSystemMessage(Component.literal("Father gives you 20 button tokens. Each villager will trade only once."));
        levelSound(p,SoundEvents.VILLAGER_CELEBRATE);
    }
    private static void levelSound(ServerPlayer p,net.minecraft.sounds.SoundEvent sound){p.serverLevel().playSound(null,p.blockPosition(),sound,SoundSource.NEUTRAL,1,1);}
    public static boolean isVillageVillager(Entity e){return e instanceof Villager&&e.getPersistentData().getBoolean("CoralineVillageVillager")&&!e.getPersistentData().getBoolean("CoralineCakeNPC")&&!e.getPersistentData().getBoolean("CoralineCarnivalNPC");}
    public static void trade(ServerPlayer p,Villager v,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return;
        var s=OtherMotherLoop.active(p);
        if(s==null||s.loop.phase!=OtherMotherLoopState.Phase.ACTIVITY||!ActivityWorld.isActivity(p.level())||
            !v.getPersistentData().hasUUID("CoralineVillageOwner")||!p.getUUID().equals(v.getPersistentData().getUUID("CoralineVillageOwner"))||
            v.getPersistentData().getInt("CoralineVillageGeneration")!=s.loop.generation||p.distanceToSqr(v)>25)return;
        if(!s.loop.villageIssued){p.displayClientMessage(Component.literal("Wait for Father to finish speaking."),true);return;}
        if(s.loop.villageTokensRemaining<=0){p.displayClientMessage(Component.literal("All tokens are spent. The return portal is open."),true);return;}
        if(v.getPersistentData().getBoolean("CoralineVillageTraded")){p.displayClientMessage(Component.literal("This villager has already traded with you."),true);return;}
        var token=p.getItemInHand(hand);
        if(!token.is(ModItems.VILLAGE_TOKEN.get())||token.isEmpty()){p.displayClientMessage(Component.literal("Hold a button token to trade."),true);return;}
        token.shrink(1);v.getPersistentData().putBoolean("CoralineVillageTraded",true);
        s.loop.villageTokensRemaining=Math.max(0,s.loop.villageTokensRemaining-1);OtherMotherLoop.changed(p);
        var reward=reward(p.getRandom());var name=reward.getHoverName().getString();
        p.getInventory().add(reward);if(!reward.isEmpty())p.drop(reward,false);
        levelSound(p,SoundEvents.VILLAGER_YES);
        p.sendSystemMessage(Component.literal("The villager gives you "+name+". "+s.loop.villageTokensRemaining+" tokens left."));
        if(s.loop.villageTokensRemaining<=0){
            removeTokens(p);ActivityWorld.ensureExit(p,s);
            p.sendSystemMessage(Component.literal("Your last token is gone. The portal back to the Other World has appeared in the plaza."));
        }
    }
    private static ItemStack reward(RandomSource random){
        int roll=random.nextInt(100);
        if(roll<75)return switch(random.nextInt(9)){
            case 0->new ItemStack(Items.ROTTEN_FLESH,random.nextInt(3)+1);
            case 1->new ItemStack(Items.WHEAT_SEEDS,random.nextInt(6)+2);
            case 2->new ItemStack(Items.STICK,random.nextInt(5)+1);
            case 3->new ItemStack(Items.POISONOUS_POTATO,1);
            case 4->new ItemStack(Items.BOWL,random.nextInt(3)+1);
            case 5->new ItemStack(Items.COBWEB,1);
            case 6->new ItemStack(Items.PAPER,random.nextInt(5)+1);
            case 7->new ItemStack(Items.DIRT,random.nextInt(4)+1);
            default->new ItemStack(Items.BONE,random.nextInt(3)+1);
        };
        if(roll<95)return switch(random.nextInt(7)){
            case 0->new ItemStack(Items.IRON_INGOT,random.nextInt(4)+2);
            case 1->new ItemStack(Items.GOLD_INGOT,random.nextInt(3)+2);
            case 2->new ItemStack(Items.EMERALD,random.nextInt(5)+2);
            case 3->new ItemStack(Items.BREAD,random.nextInt(4)+2);
            case 4->new ItemStack(Items.GOLDEN_CARROT,random.nextInt(4)+2);
            case 5->new ItemStack(Items.LAPIS_LAZULI,random.nextInt(7)+3);
            default->new ItemStack(Items.EXPERIENCE_BOTTLE,random.nextInt(4)+2);
        };
        return switch(random.nextInt(5)){
            case 0->new ItemStack(Items.DIAMOND,random.nextInt(3)+2);
            case 1->new ItemStack(Items.ENCHANTED_GOLDEN_APPLE,1);
            case 2->new ItemStack(Items.NETHERITE_SCRAP,2);
            case 3->new ItemStack(Items.TOTEM_OF_UNDYING,1);
            default->new ItemStack(Items.EMERALD_BLOCK,2);
        };
    }
}
