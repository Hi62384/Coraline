package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

    /** Twenty owner-scoped, deterministic sleight-of-eye puzzles in a private theatre. */
public final class MagicShowActivity {
    private static final String OWNER="CoralineMagicOwner",GEN="CoralineMagicGeneration";
    private static final int ROUNDS=20, SPOTS=9;
    private static final String[] THEMES={"Midnight supper","Button-eye farm","Village market","Enchanted forest","Treasure vault",
        "Dream bedroom","Tiny circus","Nether cabaret","The End","Moonlit garden"};
    private static final Item[][] OBJECTS={
        {Items.CAKE,Items.PUMPKIN_PIE,Items.CANDLE,Items.RED_CANDLE,Items.BREAD,Items.GOLDEN_CARROT,Items.FLOWER_POT,Items.COOKIE,Items.APPLE},
        {Items.WHEAT,Items.HAY_BLOCK,Items.CARROT,Items.PUMPKIN,Items.MELON,Items.EGG,Items.LEATHER,Items.BEETROOT,Items.HONEY_BOTTLE},
        {Items.EMERALD,Items.BARREL,Items.LANTERN,Items.BELL,Items.BOOK,Items.PAPER,Items.FLOWER_POT,Items.SPYGLASS,Items.CLOCK},
        {Items.OAK_SAPLING,Items.RED_MUSHROOM,Items.BROWN_MUSHROOM,Items.GLOW_BERRIES,Items.LANTERN,Items.AZALEA,Items.FERN,Items.HONEYCOMB,Items.MOSS_BLOCK},
        {Items.CHEST,Items.GOLD_BLOCK,Items.DIAMOND,Items.EMERALD,Items.AMETHYST_SHARD,Items.IRON_SWORD,Items.GOLDEN_APPLE,Items.REDSTONE,Items.NETHER_STAR},
        {Items.BLUE_BED,Items.CLOCK,Items.PAINTING,Items.BOOKSHELF,Items.LANTERN,Items.RABBIT_FOOT,Items.FLOWER_POT,Items.MUSIC_DISC_13,Items.CAKE},
        {Items.SLIME_BALL,Items.LEAD,Items.RED_WOOL,Items.BLUE_WOOL,Items.YELLOW_WOOL,Items.FIREWORK_ROCKET,Items.RABBIT_FOOT,Items.BELL,Items.CARROT_ON_A_STICK},
        {Items.BLAZE_ROD,Items.CRIMSON_FUNGUS,Items.WARPED_FUNGUS,Items.GOLD_INGOT,Items.MAGMA_CREAM,Items.NETHER_WART,Items.SOUL_LANTERN,Items.GHAST_TEAR,Items.BLACKSTONE},
        {Items.ENDER_PEARL,Items.CHORUS_FRUIT,Items.PURPUR_BLOCK,Items.END_ROD,Items.SHULKER_SHELL,Items.DRAGON_BREATH,Items.OBSIDIAN,Items.AMETHYST_SHARD,Items.END_CRYSTAL},
        {Items.POPPY,Items.DANDELION,Items.BLUE_ORCHID,Items.ALLIUM,Items.PINK_TULIP,Items.AZURE_BLUET,Items.LILY_OF_THE_VALLEY,Items.SUNFLOWER,Items.FLOWERING_AZALEA}
    };
    private static final Block[] BACKDROPS={Blocks.RED_TERRACOTTA,Blocks.LIME_TERRACOTTA,Blocks.ORANGE_TERRACOTTA,Blocks.GREEN_TERRACOTTA,
        Blocks.POLISHED_BLACKSTONE_BRICKS,Blocks.PURPLE_TERRACOTTA,Blocks.MAGENTA_TERRACOTTA,Blocks.CRIMSON_PLANKS,Blocks.PURPUR_BLOCK,Blocks.MOSS_BLOCK};
    private static final int[] COST={2,5,9,13,17,20};
    private MagicShowActivity(){}

    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(1180);}
    static boolean exitReady(VisitData.Scene s){return s.loop.magicStage>=7;}
    // Scene changes replace supported decorations (lanterns, pots, candles). Suppress
    // their drops so each illusion cannot litter the stage with free items.
    private static void put(ServerLevel l,BlockPos p,Block b){ActivityBlocks.set(l,p,ActivityWorld.persistentLeaves(b),2|32);}
    private static void mark(Entity e,VisitData.Scene s){e.getPersistentData().putUUID(OWNER,s.owner);e.getPersistentData().putInt(GEN,s.loop.generation);e.getPersistentData().putBoolean("CoralineDecoration",true);}
    private static boolean owned(Entity e,VisitData.Scene s){return e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))&&e.getPersistentData().getInt(GEN)==s.loop.generation;}

    /** Unlike a random block scatter, ten authored palettes share a fixed, readable stage grammar. */
    static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(4)).is(Blocks.PINK_GLAZED_TERRACOTTA))return;
        if(l.getBlockState(c.below(4)).is(Blocks.MAGENTA_GLAZED_TERRACOTTA))removeBuild(l,c);
        for(int x=-25;x<=25;x++)for(int z=-24;z<=24;z++){
            BlockPos at=c.offset(x,-1,z);put(l,at.below(),Blocks.DEEPSLATE_BRICKS);
            boolean border=Math.abs(x)==25||Math.abs(z)==24;
            boolean stage=Math.abs(x)<=12&&z>=-20&&z<=-4;
            put(l,at,border?Blocks.GOLD_BLOCK:stage?Blocks.DARK_OAK_PLANKS:
                Math.abs(x)<=3?Blocks.PURPLE_WOOL:Math.floorMod(x/3+z/3,2)==0?Blocks.RED_WOOL:Blocks.MAGENTA_WOOL);
            if(stage)put(l,at.above(),Math.floorMod(x+z,7)==0?Blocks.POLISHED_BLACKSTONE:Blocks.DARK_OAK_PLANKS);
            if(border)for(int y=0;y<=13;y++)put(l,c.offset(x,y,z),y%4==0?Blocks.GOLD_BLOCK:Math.floorMod(x+z,9)==0?Blocks.CHISELED_POLISHED_BLACKSTONE:Blocks.RED_TERRACOTTA);
            if(!border)put(l,c.offset(x,14,z),Math.floorMod(x,7)==0||Math.floorMod(z,7)==0?Blocks.DARK_OAK_PLANKS:
                Math.floorMod(x*3+z*5,19)==0?Blocks.SEA_LANTERN:Blocks.PURPLE_CONCRETE);
        }
        // Scalloped proscenium, footlights, gallery rails and a curtained rear arch.
        for(int x=-14;x<=14;x++){
            for(int z:new int[]{-22,-3}){
                put(l,c.offset(x,9,z),Blocks.GOLD_BLOCK);put(l,c.offset(x,10,z),Blocks.PURPLE_CONCRETE);
                if(Math.floorMod(x,3)==0)put(l,c.offset(x,8,z),Blocks.SEA_LANTERN);
            }
            // Inlaid footlights illuminate the clues without a solid barrier in front of them.
            if(Math.abs(x)<=12)put(l,c.offset(x,-1,-3),Math.floorMod(x,3)==0?Blocks.SEA_LANTERN:Blocks.GOLD_BLOCK);
        }
        for(int x:new int[]{-14,-12,12,14})for(int y=0;y<=10;y++)for(int z:new int[]{-22,-3})
            put(l,c.offset(x,y,z),y%3==0?Blocks.GOLD_BLOCK:Blocks.POLISHED_BLACKSTONE_BRICKS);
        for(int x=-20;x<=20;x++)for(int z:new int[]{-16,0,18})if(Math.abs(x)>13){
            put(l,c.offset(x,4,z),Blocks.DARK_OAK_PLANKS);put(l,c.offset(x,5,z),Blocks.GOLD_BLOCK);
            if(Math.floorMod(x,3)==0)put(l,c.offset(x,6,z),Blocks.DARK_OAK_FENCE);
        }
        for(int x:new int[]{-20,20})for(int z=-19;z<=18;z+=7){
            for(int y=0;y<6;y++)put(l,c.offset(x,y,z),Blocks.POLISHED_BLACKSTONE_BRICKS);
            put(l,c.offset(x,6,z),Blocks.GOLD_BLOCK);put(l,c.offset(x,7,z),Blocks.SEA_LANTERN);
            put(l,c.offset(x+(x<0?1:-1),0,z),Blocks.POTTED_BLUE_ORCHID);
        }
        for(int x:new int[]{-15,15})for(int z:new int[]{-14,-5,5,14}){
            put(l,c.offset(x,0,z),Blocks.CHERRY_PLANKS);put(l,c.offset(x,1,z),Blocks.POTTED_PINK_TULIP);
            put(l,c.offset(x+(x<0?-1:1),0,z),Blocks.LANTERN);
        }
        for(int x:new int[]{-12,0,12})for(int z:new int[]{-13,4,16}){
            for(int y=11;y<=13;y++)put(l,c.offset(x,y,z),Blocks.CHAIN);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                put(l,c.offset(x+dx,10,z+dz),Math.abs(dx)+Math.abs(dz)==0?Blocks.SEA_LANTERN:Blocks.GOLD_BLOCK);
            put(l,c.offset(x,9,z),Blocks.AMETHYST_CLUSTER);
        }
        for(int z=3;z<=19;z+=4)for(int x=-12;x<=12;x++)if(Math.abs(x)>2&&Math.floorMod(x+z,5)==0)
            put(l,c.offset(x,0,z),Blocks.RED_CARPET);
        for(int z=-2;z<=21;z++)for(int x=-3;x<=3;x++){
            put(l,c.offset(x,0,z),Math.abs(x)==3?Blocks.YELLOW_CARPET:
                Math.floorMod(z,5)==0?Blocks.MAGENTA_CARPET:Blocks.PURPLE_CARPET);
        }
        for(int z:new int[]{0,8,16})for(int x:new int[]{-4,4}){
            put(l,c.offset(x,0,z),Blocks.DARK_OAK_PLANKS);
            put(l,c.offset(x,1,z),Blocks.POTTED_PINK_TULIP);
        }
        // Arcade-style prize counter: displays sit in front of the low counter,
        // with a bright stocked wall behind the attendant's side.
        for(int z=-1;z<=20;z++){
            put(l,c.offset(18,0,z),Blocks.POLISHED_BLACKSTONE_BRICKS);
            put(l,c.offset(18,1,z),Blocks.CHERRY_SLAB);
            put(l,c.offset(21,0,z),Blocks.PURPLE_CONCRETE);
            if(z%3==0){put(l,c.offset(21,1,z),Blocks.GOLD_BLOCK);put(l,c.offset(21,2,z),Blocks.SEA_LANTERN);}
            else {put(l,c.offset(21,1,z),Blocks.MAGENTA_CONCRETE);put(l,c.offset(21,2,z),Blocks.PURPLE_CONCRETE);}
            put(l,c.offset(21,3,z),Blocks.GOLD_BLOCK);
        }
        for(int x=-10;x<=10;x++)put(l,c.offset(x,0,-2),Blocks.DARK_OAK_FENCE);
        // Solid side wings prevent an oblique view behind a closed curtain.
        for(int side:new int[]{-1,1})for(int x=10;x<=24;x++)for(int y=0;y<=8;y++){
            int sx=side*x;
            for(int z=-5;z<=-3;z++)put(l,c.offset(sx,y,z),y==8?Blocks.GOLD_BLOCK:Blocks.PURPLE_CONCRETE);
        }
        put(l,c.below(4),Blocks.PINK_GLAZED_TERRACOTTA);
    }
    static void removeBuild(ServerLevel l,BlockPos c){
        if(!l.getBlockState(c.below(4)).is(Blocks.MAGENTA_GLAZED_TERRACOTTA)&&!l.getBlockState(c.below(4)).is(Blocks.PINK_GLAZED_TERRACOTTA))return;
        for(int x=-25;x<=25;x++)for(int z=-24;z<=24;z++)for(int y=-4;y<=14;y++){
            var at=c.offset(x,y,z);if(!l.isEmptyBlock(at))put(l,at,Blocks.AIR);
        }
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){
        var found=new ArrayList<Entity>();for(var e:l.getEntitiesOfClass(Entity.class,new AABB(c).inflate(27,17,26),e->owned(e,s)))found.add(e);
        found.forEach(e->{e.ejectPassengers();e.discard();});
    }
    private static void clearStageDrops(ServerLevel l,BlockPos c){
        // These are caused by replacing the stage's own supported flowers and
        // lanterns. Keep the cleanup tightly inside the inaccessible stage.
        AABB stage=new AABB(c.offset(-11,0,-20),c.offset(12,9,-4));
        for(ItemEntity item:l.getEntitiesOfClass(ItemEntity.class,stage,e->true))item.discard();
    }
    private static ItemStack token(VisitData.Scene s){
        ItemStack stack=new ItemStack(ModItems.VILLAGE_TOKEN.get());
        stack.getOrCreateTag().putUUID(OWNER,s.owner);stack.getTag().putInt(GEN,s.loop.generation);return stack;
    }
    private static boolean showToken(ItemStack stack,VisitData.Scene s){
        return stack.is(ModItems.VILLAGE_TOKEN.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER)&&
            s.owner.equals(stack.getTag().getUUID(OWNER))&&stack.getTag().getInt(GEN)==s.loop.generation;
    }
    private static int tokenCount(ServerPlayer p,VisitData.Scene s){
        int count=0;for(ItemStack stack:p.getInventory().items)if(showToken(stack,s))count+=stack.getCount();
        for(ItemStack stack:p.getInventory().offhand)if(showToken(stack,s))count+=stack.getCount();return count;
    }
    private static void spendTokens(ServerPlayer p,VisitData.Scene s,int cost){
        for(ItemStack stack:p.getInventory().items){if(cost==0)break;if(showToken(stack,s)){int take=Math.min(cost,stack.getCount());stack.shrink(take);cost-=take;}}
        for(ItemStack stack:p.getInventory().offhand){if(cost==0)break;if(showToken(stack,s)){int take=Math.min(cost,stack.getCount());stack.shrink(take);cost-=take;}}
        p.getInventory().setChanged();
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        var c=center(p,s);build(l,c);clean(l,s,c);
        var st=s.loop;st.magicStage=0;st.magicRound=0;st.magicTokens=0;st.magicAnswer=-1;st.magicSeed=p.getRandom().nextLong();
        st.magicVoiceUntil=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.MAGIC_SHOW));st.magicPhaseUntil=0;
        ensureFather(p,s,l,c);ensureAudience(p,s,l,c);ensureCurtain(l,s,c);
        p.sendSystemMessage(Component.literal("Walk up the gold aisle to the front rail. When the curtains reopen, right-click the display that changed."));
        OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        LoopProps.hold(l,c);LoopProps.hold(l,c.south(21));build(l,c);ensureFather(p,s,l,c);ensureAudience(p,s,l,c);ensureCurtain(l,s,c);
        if(s.loop.magicStage>=1&&s.loop.magicStage<=6)ensureSpots(l,s,c);
        if(s.loop.magicStage>=7)ensurePrizes(l,s,c);
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        if(!l.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(10)).inflate(7),e->owned(e,s)).isEmpty())return;
        var family=ParentAccess.family(p.server,s.owner);var realm=RealmData.get(p.server).realms.get(s.door);
        var father=ModEntities.DAD.get().create(l);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);mark(father,s);
        father.getPersistentData().putUUID("RealmDoor",s.door);if(realm!=null)father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+10.5);father.setYRot(180);father.setYBodyRot(180);l.addFreshEntity(father);
    }
    private static void ensureAudience(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        var audience=l.getEntitiesOfClass(Villager.class,new AABB(c).inflate(23,5,22),e->owned(e,s));
        if(audience.size()>=24)return;
        // No AI, no wandering: memory-puzzle objects cannot shift while observed.
        int seat=0;
        for(int z:new int[]{4,8,12,16,20})for(int x:new int[]{-12,-9,-6,6,9,12}){
            if((x+z)%4==0)continue;
            if(seat++<audience.size())continue;
            var chair=LoopProps.spawn(l,s,ScenePropEntity.CHAIR,new Vec3(c.getX()+x+.5,c.getY(),c.getZ()+z+.5),-100-seat);
            chair.setYRot(180);mark(chair,s);
            Villager v=EntityType.VILLAGER.create(l);if(v==null)continue;mark(v,s);
            v.setNoAi(true);v.setPersistenceRequired();v.setCustomName(Component.literal(VillageActivity.RESIDENT_NAME));v.setCustomNameVisible(false);
            v.setPos(chair.getX(),chair.getY(),chair.getZ());v.setYRot(180);v.setYBodyRot(180);l.addFreshEntity(v);v.startRiding(chair,true);
        }
    }
    private static ScenePropEntity curtain(ServerLevel l,VisitData.Scene s,BlockPos c){
        var existing=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c.north(3)).inflate(12,10,2),e->owned(e,s)&&e.kind()==ScenePropEntity.MAGIC_CURTAIN);
        return existing.isEmpty()?null:existing.get(0);
    }
    private static void ensureCurtain(ServerLevel l,VisitData.Scene s,BlockPos c){
        var all=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(25,15,25),e->e.kind()==ScenePropEntity.MAGIC_CURTAIN&&s.owner.equals(e.owner));
        ScenePropEntity prop=null;
        for(var candidate:all){if(prop==null&&owned(candidate,s))prop=candidate;else candidate.discard();}
        if(prop==null){prop=LoopProps.spawn(l,s,ScenePropEntity.MAGIC_CURTAIN,new Vec3(c.getX()+.5,c.getY()+1,c.getZ()-3.5),-1);mark(prop,s);}
        int stage=s.loop.magicStage;
        // Reconcile saved/respawned curtains with the authoritative phase.
        // Do not reset animation clocks while a transition is in progress.
        if(stage==2||stage==5||stage==6)prop.mode(2);
        else if(stage==0||stage>=7)prop.mode(0);
    }
    private static int slots(int round){return round<5?5:round<10?7:SPOTS;}
    private static long roundSeed(long seed,int round){
        long v=seed+0x9E3779B97F4A7C15L*(round+1);v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;
        v=(v^(v>>>27))*0x94D049BB133111EBL;return v^(v>>>31);
    }
    private static int theme(long seed,int round){
        int[] order={0,1,2,3,4,5,6,7,8,9};var r=RandomSource.create(seed^(round/10*0x5F11A7E9L));
        for(int i=9;i>0;i--){int j=r.nextInt(i+1),swap=order[i];order[i]=order[j];order[j]=swap;}
        return order[round%10];
    }
    private record Scene(int theme,ItemStack[] before,ItemStack[] after,int answer,int change){}
    private static Scene generate(long seed,int round){
        int n=slots(round),theme=theme(seed,round);var random=RandomSource.create(roundSeed(seed,round));
        ItemStack[] before=new ItemStack[n],after=new ItemStack[n];
        for(int i=0;i<n;i++){
            int choice=random.nextInt(OBJECTS[theme].length);
            // Early tricks are sparse; the final rows contain repeated families and more visual clutter.
            before[i]=new ItemStack(OBJECTS[theme][choice]);after[i]=before[i].copy();
        }
        int answer=random.nextInt(n);
        int change=1; // Every trick swaps a visible object; never add/remove an empty display.
        Item replacement;do{replacement=OBJECTS[theme][random.nextInt(OBJECTS[theme].length)];}while(replacement==before[answer].getItem());
        after[answer]=new ItemStack(replacement);
        Scene scene=new Scene(theme,before,after,answer,change);validate(scene);return scene;
    }
    private static void validate(Scene scene){
        int changed=0;for(int i=0;i<scene.before.length;i++){
            if(scene.before[i].isEmpty()||scene.after[i].isEmpty())throw new IllegalStateException("Empty Magic Show display");
            if(!ItemStack.isSameItemSameTags(scene.before[i],scene.after[i]))changed++;
        }
        if(changed!=1||scene.answer<0||scene.answer>=scene.before.length||ItemStack.isSameItemSameTags(scene.before[scene.answer],scene.after[scene.answer]))
            throw new IllegalStateException("Ambiguous Magic Show trick");
    }
    /** Test hook exercises every theme and verifies exactly one nonempty replacement. */
    public static void validatePerformance(long seed){
        for(int round=0;round<ROUNDS;round++)validate(generate(seed,round));
    }
    private static double spotX(int slot,int count){return (slot-(count-1)/2.0)*2.05;}
    private static void backdrop(ServerLevel l,BlockPos c,int theme,int round){
        Block wall=BACKDROPS[theme],accent=round>=15?Blocks.GOLD_BLOCK:round>=10?Blocks.SEA_LANTERN:Blocks.AMETHYST_BLOCK;
        for(int x=-11;x<=11;x++)for(int z=-19;z<=-7;z++)for(int y=1;y<=6;y++){
            var at=c.offset(x,y,z);if(!l.isEmptyBlock(at))put(l,at,Blocks.AIR);
        }
        for(int x=-11;x<=11;x++)for(int y=1;y<=8;y++){
            Block b=Math.floorMod(x*7+y*11,13)==0?accent:Math.abs(x)%5==0?Blocks.GOLD_BLOCK:wall;
            put(l,c.offset(x,y,-20),b);
        }
        for(int x=-11;x<=11;x++)for(int z=-19;z<=-5;z++)
            put(l,c.offset(x,0,z),Math.floorMod(x+z,5)==0?Blocks.GOLD_BLOCK:wall);
        for(int x:new int[]{-10,10})for(int z:new int[]{-16,-10}){
            put(l,c.offset(x,1,z),Blocks.GOLD_BLOCK);put(l,c.offset(x,2,z),theme==7?Blocks.SOUL_LANTERN:Blocks.LANTERN);
        }
        stageSet(l,c,theme);
        clearStageDrops(l,c);
    }
    /** Static themed scenery never changes during a trick; only the front display clues do. */
    private static void stageSet(ServerLevel l,BlockPos c,int theme){
        for(int x:new int[]{-7,7})for(int z:new int[]{-15,-10}){
            Block base=switch(theme){case 0->Blocks.DARK_OAK_PLANKS;case 1->Blocks.HAY_BLOCK;case 2->Blocks.BARREL;
                case 3->Blocks.OAK_LOG;case 4->Blocks.GOLD_BLOCK;case 5->Blocks.BOOKSHELF;
                case 6->Blocks.MAGENTA_WOOL;case 7->Blocks.POLISHED_BLACKSTONE_BRICKS;
                case 8->Blocks.PURPUR_BLOCK;default->Blocks.MOSS_BLOCK;};
            put(l,c.offset(x,1,z),base);
            Block top=switch(theme){case 0->Blocks.CAKE;case 1->Blocks.CARVED_PUMPKIN;case 2->Blocks.LANTERN;
                case 3->Blocks.FLOWERING_AZALEA_LEAVES;case 4->Blocks.EMERALD_BLOCK;case 5->Blocks.POTTED_BLUE_ORCHID;
                case 6->Blocks.SEA_LANTERN;case 7->Blocks.SOUL_LANTERN;case 8->Blocks.END_ROD;
                default->Blocks.FLOWERING_AZALEA;};
            put(l,c.offset(x,2,z),top);
        }
        switch(theme){
            case 0->{for(int x=-3;x<=3;x++)for(int z=-15;z<=-13;z++)put(l,c.offset(x,1,z),Blocks.DARK_OAK_PLANKS);
                for(int x:new int[]{-2,0,2})put(l,c.offset(x,2,-14),x==0?Blocks.CAKE:Blocks.CANDLE);}
            case 1->{for(int x=-3;x<=3;x++)put(l,c.offset(x,1,-16),Blocks.HAY_BLOCK);
                for(int x:new int[]{-3,0,3})put(l,c.offset(x,2,-16),Blocks.JACK_O_LANTERN);}
            case 2->{for(int x:new int[]{-4,4})for(int z=-17;z<=-11;z++)put(l,c.offset(x,4,z),Blocks.PINK_WOOL);
                for(int x:new int[]{-4,4}){put(l,c.offset(x,1,-14),Blocks.BARREL);put(l,c.offset(x,2,-14),Blocks.BELL);}}
            case 3->{for(int x:new int[]{-3,3}){for(int y=1;y<=4;y++)put(l,c.offset(x,y,-15),Blocks.OAK_LOG);
                for(int dx=-2;dx<=2;dx++)put(l,c.offset(x+dx,5,-15),Blocks.OAK_LEAVES);}
                put(l,c.offset(0,1,-13),Blocks.RED_MUSHROOM_BLOCK);}
            case 4->{for(int x=-4;x<=4;x++)put(l,c.offset(x,1,-15),Blocks.GOLD_BLOCK);
                for(int x:new int[]{-3,0,3})put(l,c.offset(x,2,-15),Blocks.DIAMOND_BLOCK);}
            case 5->{for(int x=-4;x<=4;x++)put(l,c.offset(x,1,-16),Blocks.BOOKSHELF);
                put(l,c.offset(0,1,-12),Blocks.LOOM);put(l,c.offset(0,2,-12),Blocks.POTTED_ALLIUM);}
            case 6->{for(int x:new int[]{-4,0,4})for(int y=1;y<=3;y++)put(l,c.offset(x,y,-16),y%2==0?Blocks.YELLOW_WOOL:Blocks.RED_WOOL);
                for(int x:new int[]{-4,0,4})put(l,c.offset(x,4,-16),Blocks.SEA_LANTERN);}
            case 7->{for(int x=-3;x<=3;x++)put(l,c.offset(x,1,-16),Blocks.POLISHED_BLACKSTONE_BRICKS);
                for(int x:new int[]{-3,0,3})put(l,c.offset(x,2,-16),Blocks.SOUL_LANTERN);}
            case 8->{for(int x:new int[]{-3,0,3}){put(l,c.offset(x,1,-15),Blocks.PURPUR_PILLAR);
                put(l,c.offset(x,2,-15),Blocks.PURPUR_PILLAR);put(l,c.offset(x,3,-15),Blocks.END_ROD);}}
            default->{for(int x=-4;x<=4;x++)put(l,c.offset(x,1,-16),Blocks.FLOWERING_AZALEA_LEAVES);
                for(int x:new int[]{-3,0,3})put(l,c.offset(x,1,-12),Blocks.MOSS_BLOCK);
                put(l,c.offset(0,2,-12),Blocks.PINK_TULIP);}
        }
    }
    private static void clearSpots(ServerLevel l,VisitData.Scene s,BlockPos c){
        for(var e:l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c.north(5)).inflate(13,5,4),e->owned(e,s)&&e.kind()==ScenePropEntity.MAGIC_SPOT))e.discard();
    }
    private static void ensureSpots(ServerLevel l,VisitData.Scene s,BlockPos c){
        int round=s.loop.magicRound;if(round>=ROUNDS)return;Scene scene=generate(s.loop.magicSeed,round);
        boolean changed=s.loop.magicStage>=4;
        for(int i=0;i<scene.before.length;i++){
            int slot=i;double x=spotX(i,scene.before.length);
            var existing=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c.offset((int)Math.round(x),1,-5)).inflate(1.5,2,1.5),e->owned(e,s)&&e.kind()==ScenePropEntity.MAGIC_SPOT&&e.slot==slot);
            ScenePropEntity prop=existing.isEmpty()?LoopProps.spawn(l,s,ScenePropEntity.MAGIC_SPOT,new Vec3(c.getX()+.5+x,c.getY()+1,c.getZ()-4.8),slot):existing.get(0);
            if(existing.isEmpty())mark(prop,s);
            ItemStack expected=changed?scene.after[slot]:scene.before[slot];
            if(!ItemStack.matches(prop.food(0),expected))prop.food(0,expected);
        }
    }
    private static void setCurtain(ServerLevel l,VisitData.Scene s,BlockPos c,int mode){var curtain=curtain(l,s,c);if(curtain!=null)curtain.mode(mode);}
    private static void burst(ServerLevel l,BlockPos at,boolean success){
        l.sendParticles(success?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.POOF,at.getX()+.5,at.getY()+1.5,at.getZ()+.5,28,1.7,1,.7,.1);
    }
    private static void beginRound(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var st=s.loop;if(st.magicRound>=ROUNDS){openCounter(p,s,c);return;}
        Scene scene=generate(st.magicSeed,st.magicRound);st.magicAnswer=scene.answer;
        clearSpots(p.serverLevel(),s,c);backdrop(p.serverLevel(),c,scene.theme,st.magicRound);ensureSpots(p.serverLevel(),s,c);
        st.magicStage=1;st.magicPhaseUntil=now+16;setCurtain(p.serverLevel(),s,c,1);burst(p.serverLevel(),c.north(9),true);
        p.serverLevel().playSound(null,c,SoundEvents.ILLUSIONER_CAST_SPELL,SoundSource.PLAYERS,.9F,1.15F);
        p.sendSystemMessage(Component.literal("Trick "+(st.magicRound+1)+" / 20 — "+THEMES[scene.theme]+". Watch closely!"));OtherMotherLoop.changed(p);
    }
    private static void feedback(ServerPlayer p,VisitData.Scene s,boolean correct){
        var st=s.loop;if(st.magicStage!=5)return;
        if(correct){st.magicTokens++;ActivityTokens.give(p,token(s));}
        st.magicStage=6;st.magicPhaseUntil=OtherMotherLoop.now(p)+18;
        BlockPos c=center(p,s);var props=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c.north(5)).inflate(13,5,4),e->owned(e,s)&&e.kind()==ScenePropEntity.MAGIC_SPOT&&e.slot==st.magicAnswer);
        if(!props.isEmpty())props.get(0).mode(1);
        burst(p.serverLevel(),c.offset((int)Math.round(spotX(st.magicAnswer,slots(st.magicRound))),2,-5),correct);
        p.serverLevel().playSound(null,c,correct?SoundEvents.UI_TOAST_CHALLENGE_COMPLETE:SoundEvents.VILLAGER_NO,SoundSource.PLAYERS,.9F,correct?1.4F:.8F);
        p.sendSystemMessage(Component.literal(correct?"Correct! +1 Magic Token":"Wrong!"));OtherMotherLoop.changed(p);
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var st=s.loop;long now=OtherMotherLoop.now(p);
        if(p.tickCount%20==0)prepare(p,s,p.serverLevel(),c);
        if(p.tickCount%10==0&&st.magicStage>=1&&st.magicStage<=6)clearStageDrops(p.serverLevel(),c);
        // A rail protects the trick set, while movement and camera remain player controlled.
        if(st.magicStage>=1&&st.magicStage<=6&&p.getZ()<c.getZ()-1.5){
            p.teleportTo(p.serverLevel(),p.getX(),c.getY(),c.getZ()+.5,p.getYRot(),p.getXRot());p.setDeltaMovement(Vec3.ZERO);
        }
        if(p.getY()<c.getY()-3||Math.abs(p.getX()-(c.getX()+.5))>23||Math.abs(p.getZ()-(c.getZ()+.5))>23){
            p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+9.5,p.getYRot(),p.getXRot());p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        }
        if(st.magicStage==0){if(now>=st.magicVoiceUntil)beginRound(p,s,c,now);else if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Father's introduction — the show begins when he finishes."),true);return;}
        if(st.magicStage==8){if(p.tickCount%40==0)ActivityWorld.ensureExit(p,s);return;}
        if(st.magicStage==7){if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Button Tokens: "+tokenCount(p,s)+" | Right-click the prize counter; portal is open."),true);return;}
        if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Trick "+(st.magicRound+1)+" / 20 | Magic Tokens: "+st.magicTokens+
            (st.magicStage==5?" | Choose a changed display!":"")),true);
        if(st.magicStage==5&&p.tickCount%10==0)for(var prop:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,
            new AABB(c.north(5)).inflate(13,5,4),e->owned(e,s)&&e.kind()==ScenePropEntity.MAGIC_SPOT))
            p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,prop.getX(),prop.getY()+1.25,prop.getZ(),5,.3,.18,.3,.008);
        if(now<st.magicPhaseUntil)return;
        switch(st.magicStage){
            case 1->{st.magicStage=2;st.magicPhaseUntil=now+(st.magicRound<5?75:st.magicRound<15?65:55);setCurtain(p.serverLevel(),s,c,2);}
            case 2->{st.magicStage=3;st.magicPhaseUntil=now+16;setCurtain(p.serverLevel(),s,c,3);p.serverLevel().playSound(null,c,SoundEvents.ILLUSIONER_CAST_SPELL,SoundSource.PLAYERS,.65F,.75F);}
            case 3->{st.magicStage=4;st.magicPhaseUntil=now+16;ensureSpots(p.serverLevel(),s,c);setCurtain(p.serverLevel(),s,c,1);burst(p.serverLevel(),c.north(9),true);}
            case 4->{st.magicStage=5;st.magicPhaseUntil=now+(st.magicRound<5?135:st.magicRound<15?120:105);setCurtain(p.serverLevel(),s,c,2);}
            case 5->feedback(p,s,false);
            case 6->{st.magicStage=9;st.magicPhaseUntil=now+16;setCurtain(p.serverLevel(),s,c,3);}
            case 9->{st.magicRound++;if(st.magicRound==10)p.sendSystemMessage(Component.literal("Father raises his hands. The theatre brightens; the tricks grow stranger."));beginRound(p,s,c,now);}
            default->{}
        }
        OtherMotherLoop.changed(p);
    }
    private static int prizeZ(int tier){return 1+tier*3;}
    private static ItemStack prize(int tier){return switch(tier){
        case 0->new ItemStack(Items.GOLDEN_CARROT,4);case 1->new ItemStack(Items.EMERALD,8);case 2->new ItemStack(Items.DIAMOND,4);
        case 3->new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get(),6);
        case 4->new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get(),4);
        default->new ItemStack(Items.NETHER_STAR);
    };}
    private static void ensurePrizes(ServerLevel l,VisitData.Scene s,BlockPos c){
        for(int tier=0;tier<6;tier++){
            int id=tier;var pos=c.offset(16,1,prizeZ(tier));
            var existing=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(pos).inflate(2,3,2),e->owned(e,s)&&e.kind()==ScenePropEntity.MAGIC_PRIZE&&e.slot==id);
            if(!existing.isEmpty())continue;
            var display=LoopProps.spawn(l,s,ScenePropEntity.MAGIC_PRIZE,Vec3.atBottomCenterOf(pos),tier);mark(display,s);display.food(0,prize(tier));
            display.setCustomName(Component.literal(COST[tier]+" buttons · "+prize(tier).getHoverName().getString()));display.setCustomNameVisible(true);
        }
        for(var old:l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(24,4,24),e->owned(e,s)&&e.kind()==ScenePropEntity.MAGIC_FINISH))old.discard();
    }
    private static void openCounter(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var st=s.loop;st.magicStage=7;st.magicPhaseUntil=0;clearSpots(p.serverLevel(),s,c);setCurtain(p.serverLevel(),s,c,3);
        ensurePrizes(p.serverLevel(),s,c);burst(p.serverLevel(),c.north(6),true);
        ActivityWorld.ensureExit(p,s);
        p.serverLevel().playSound(null,c,SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.PLAYERS,1,1.2F);
        p.sendSystemMessage(Component.literal("The show is over! Your Button Tokens are in your inventory. Visit the prize counter, or use the open portal."));
        OtherMotherLoop.changed(p);
    }
    public static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        var st=s.loop;if(s.loop.activity!=OtherMotherActivities.Activity.MAGIC_SHOW)return false;
        if(prop.kind()==ScenePropEntity.MAGIC_SPOT){
            if(st.magicStage==5&&prop.slot>=0&&prop.slot<slots(st.magicRound))feedback(p,s,prop.slot==st.magicAnswer);
            else p.displayClientMessage(Component.literal("Wait for the second reveal before choosing."),true);
            return true;
        }
        if(prop.kind()==ScenePropEntity.MAGIC_PRIZE){
            if(st.magicStage!=7)return true;int tier=prop.slot;if(tier<0||tier>=COST.length)return true;
            if(tokenCount(p,s)<COST[tier]){p.displayClientMessage(Component.literal("Need "+COST[tier]+" Button Tokens; you have "+tokenCount(p,s)+"."),true);return true;}
            // Debit the saved ledger before delivery; repeated clicks cannot duplicate this purchase.
            spendTokens(p,s,COST[tier]);st.magicTokens=tokenCount(p,s);OtherMotherLoop.changed(p);ItemStack reward=prize(tier);
            p.getInventory().add(reward);if(!reward.isEmpty())p.drop(reward,false);
            p.serverLevel().playSound(null,prop.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,.9F,1.2F);
            p.sendSystemMessage(Component.literal("Prize claimed! "+st.magicTokens+" Magic Tokens left."));return true;
        }
        if(prop.kind()==ScenePropEntity.MAGIC_FINISH){
            if(st.magicStage!=7)return true;st.magicStage=8;OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
            p.sendSystemMessage(Component.literal("The exit portal is open at the theatre's rear aisle."));return true;
        }
        return false;
    }
}
