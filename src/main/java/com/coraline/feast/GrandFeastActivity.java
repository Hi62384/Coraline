package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModBlocks;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/** A timed, contested feast in a tiny pastel mid-century town. */
public final class GrandFeastActivity {
    private static final String OWNER="CoralineFeastOwner", VILLAGER="CoralineFeastVillager";
    private static final int PLATES=16, GUESTS=18, DURATION=180*20;
    private static final double RING_WIDTH=5.5,RING_DEPTH=15.5,RING_LENGTH=2*(2*RING_WIDTH+2*RING_DEPTH);
    private GrandFeastActivity(){}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(800);}
    static boolean exitReady(VisitData.Scene s){return s.loop.feastFinished;}
    private static void put(ServerLevel l,BlockPos at,Block b){ActivityBlocks.set(l,at,ActivityWorld.persistentLeaves(b),2);}
    private static BlockPos plate(BlockPos c,int slot){return c.offset(slot<8?-2:2,1,-11+(slot%8)*3);}
    private static Item food(int id){return switch(id){
        case 1->Items.BREAD;case 2->Items.BAKED_POTATO;case 3->Items.APPLE;case 4->Items.COOKIE;
        case 5->FeastContent.FOODS.get("corn_on_the_cob").get();case 6->FeastContent.FOODS.get("sweet_peas").get();
        case 7->Items.COOKED_CHICKEN;case 8->Items.COOKED_BEEF;case 9->FeastContent.FOODS.get("sausages").get();
        case 10->FeastContent.FOODS.get("strawberry_pancakes").get();case 11->Items.PUMPKIN_PIE;
        case 12->Items.GOLDEN_CARROT;case 13->Items.GOLDEN_APPLE;
        case 14->FeastContent.FOODS.get("golden_chicken_breast").get();case 15->Items.ENCHANTED_GOLDEN_APPLE;
        default->Items.AIR;
    };}
    private static ItemStack serving(int id){return id==0?ItemStack.EMPTY:new ItemStack(food(id),id<=6?2:id<=11?1:1);}
    private static int rollFood(ServerPlayer p,long elapsed){
        int tier=elapsed<900?0:elapsed<1800?1:elapsed<2700?2:elapsed<3300?3:4;
        int n=p.getRandom().nextInt(100);
        if(tier==0)return n<86?1+p.getRandom().nextInt(6):7+p.getRandom().nextInt(3);
        if(tier==1)return n<25?1+p.getRandom().nextInt(6):7+p.getRandom().nextInt(5);
        if(tier==2)return n<10?1+p.getRandom().nextInt(6):n<74?7+p.getRandom().nextInt(5):12+p.getRandom().nextInt(3);
        if(tier==3)return n<3?15:n<68?12+p.getRandom().nextInt(3):7+p.getRandom().nextInt(5);
        if(n<14)return 15;
        if(n<80)return 12+p.getRandom().nextInt(3);
        if(n<94)return 7+p.getRandom().nextInt(5);
        return 1+p.getRandom().nextInt(6);
    }
    static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(3)).is(Blocks.CYAN_GLAZED_TERRACOTTA))return;
        if(l.getBlockState(c.below(2)).is(Blocks.ORANGE_GLAZED_TERRACOTTA))removeBuild(l,c);
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++){
            var at=c.offset(x,-1,z);put(l,at.below(),Blocks.DIRT);
            boolean rim=Math.abs(x)==49||Math.abs(z)==49;
            boolean street=(Math.abs(x)>=23&&Math.abs(x)<=28)||(Math.abs(z)>=23&&Math.abs(z)<=28);
            boolean square=Math.abs(x)<=19&&Math.abs(z)<=19;
            Block floor=rim?Blocks.POLISHED_DEEPSLATE:street?Blocks.GRAY_CONCRETE:
                square?(Math.floorMod(x/3+z/3,2)==0?Blocks.SMOOTH_STONE:Blocks.POLISHED_ANDESITE):Blocks.GRASS_BLOCK;
            put(l,at,floor);
            if(rim){put(l,at.above(),Blocks.DARK_OAK_FENCE);put(l,at.above(2),Blocks.DARK_OAK_FENCE);}
            if(!square&&!street&&!rim&&Math.floorMod(x*17+z*31,19)==0&&Math.abs(x)>8&&Math.abs(z)>8)
                put(l,at.above(),Math.floorMod(x-z,3)==0?Blocks.PINK_TULIP:Math.floorMod(x+z,3)==0?Blocks.ALLIUM:Blocks.BLUE_ORCHID);
        }
        for(int x:new int[]{-36,36})for(int z:new int[]{-35,-17,0,17,35})house(l,c.offset(x,0,z),Math.floorMod(x+z,5),x<0?Direction.EAST:Direction.WEST);
        house(l,c.north(37),3,Direction.SOUTH);
        for(int x:new int[]{-36,36})for(int z:new int[]{-17,17})shopfront(l,c.offset(x,0,z),x<0?Direction.EAST:Direction.WEST,Math.floorMod(x+z,4));
        // Trimmed boulevards, lamp globes, hanging lanterns and tiny planters.
        for(int x:new int[]{-21,21})for(int z=-35;z<=35;z+=10)streetLight(l,c.offset(x,0,z));
        for(int z:new int[]{-21,21})for(int x=-35;x<=35;x+=10)streetLight(l,c.offset(x,0,z));
        for(int x:new int[]{-42,42})for(int z:new int[]{-17,17})tree(l,c.offset(x,0,z));
        for(int x:new int[]{-13,13})for(int z:new int[]{-36,36})fountain(l,c.offset(x,0,z));
        for(int x=-42;x<=42;x+=8)for(int z:new int[]{-44,44})if(Math.abs(x)>18){
            put(l,c.offset(x,0,z),Blocks.CHERRY_STAIRS);put(l,c.offset(x+2,0,z),Blocks.POTTED_BLUE_ORCHID);
        }
        for(int x:new int[]{-15,15})for(int z:new int[]{-15,15}){
            for(int dx=-3;dx<=3;dx++)for(int dz=-2;dz<=2;dz++){
                var at=c.offset(x+dx,0,z+dz);
                if(Math.abs(dx)==3||Math.abs(dz)==2)put(l,at,Blocks.FLOWERING_AZALEA_LEAVES);
                else if(Math.floorMod(dx+dz,2)==0)put(l,at,Blocks.POTTED_PINK_TULIP);
            }
            put(l,c.offset(x,0,z+4),ModBlocks.ROSE_VELVET_SOFA.get());
        }
        for(int z:new int[]{-31,31})for(int x:new int[]{-12,-8,8,12}){
            put(l,c.offset(x,0,z),Blocks.BARREL);
            put(l,c.offset(x,1,z),x<0?Blocks.POTTED_AZURE_BLUET:Blocks.POTTED_POPPY);
        }
        // Parked little cars are blocks, not loot dispensers: pastel bodies and glass cabins.
        car(l,c.offset(-26,0,-10),Blocks.RED_CONCRETE);
        car(l,c.offset(26,0,11),Blocks.LIGHT_BLUE_CONCRETE);
        car(l,c.offset(-10,0,26),Blocks.YELLOW_CONCRETE);
        for(int x=-3;x<=3;x++)for(int z=-15;z<=15;z++){
            if(Math.abs(x)<=2&&Math.abs(z)<=14){put(l,c.offset(x,0,z),Math.floorMod(z,4)==0?Blocks.RED_WOOL:Math.floorMod(x+z,5)==0?Blocks.WHITE_WOOL:Blocks.DARK_OAK_PLANKS);}
            if(Math.abs(x)==3&&Math.abs(z)<=14)put(l,c.offset(x,0,z),Blocks.DARK_OAK_TRAPDOOR);
        }
        for(int z=-13;z<=13;z+=3){
            put(l,c.offset(0,1,z),z%2==0?Blocks.WHITE_CANDLE:Blocks.RED_CANDLE);
            for(int x:new int[]{-4,4})put(l,c.offset(x,0,z),ModBlocks.ROSE_VELVET_SOFA.get());
        }
        for(int z=-12;z<=12;z+=6){
            put(l,c.offset(0,0,z),Blocks.GOLD_BLOCK);put(l,c.offset(0,1,z),Blocks.POTTED_PINK_TULIP);
            for(int x:new int[]{-2,2})put(l,c.offset(x,1,z),Blocks.CAKE);
        }
        for(int z=-15;z<=15;z+=5)for(int x:new int[]{-3,3}){
            put(l,c.offset(x,0,z),Blocks.GOLD_BLOCK);put(l,c.offset(x,1,z),Blocks.LANTERN);
        }
        for(int x=-17;x<=17;x++)for(int z:new int[]{-18,18})if(Math.floorMod(x,4)==0)put(l,c.offset(x,6,z),Blocks.SEA_LANTERN);
        for(int z:new int[]{-14,14}){
            put(l,c.offset(0,0,z),Blocks.GOLD_BLOCK);
            put(l,c.offset(0,1,z),ModBlocks.FLOWER_CONSOLE.get());
        }
        for(int x:new int[]{-7,7})for(int z:new int[]{-13,13})put(l,c.offset(x,0,z),ModBlocks.BRASS_FLOOR_LAMP.get());
        // The open sides of the square are a café promenade, not unused paving.
        // Keep the inner five-block guest circuit clear for the food race.
        for(int x:new int[]{-12,12})for(int z:new int[]{-12,-4,4,12}){
            var table=c.offset(x,0,z);
            put(l,table,Blocks.SMOOTH_QUARTZ);
            put(l,table.above(),Math.floorMod(z,3)==0?Blocks.POTTED_PINK_TULIP:Blocks.CAKE);
            for(int dx:new int[]{-2,2})put(l,table.offset(dx,0,0),ModBlocks.ROSE_VELVET_SOFA.get());
            for(int dz:new int[]{-2,2})put(l,table.offset(0,0,dz),Blocks.CHERRY_STAIRS);
            put(l,table.offset(x<0?-3:3,0,2),Blocks.FLOWERING_AZALEA_LEAVES);
        }
        for(int z:new int[]{-18,18}){
            for(int x:new int[]{-17,17})for(int y=0;y<=6;y++)put(l,c.offset(x,y,z),Blocks.STRIPPED_BIRCH_LOG);
            for(int x=-16;x<=16;x++)if(Math.floorMod(x,4)!=0)put(l,c.offset(x,6,z),Blocks.CHAIN);
        }
        for(int x=-6;x<=6;x++){put(l,c.offset(x,-1,20),Blocks.PINK_CONCRETE);put(l,c.offset(x,-1,-20),Blocks.PINK_CONCRETE);}
        put(l,c.below(2),Blocks.ORANGE_GLAZED_TERRACOTTA);
        put(l,c.below(3),Blocks.CYAN_GLAZED_TERRACOTTA);
    }
    private static void shopfront(ServerLevel l,BlockPos c,Direction front,int style){
        var door=c.relative(front,5);Block shade=style%2==0?Blocks.RED_WOOL:Blocks.CYAN_WOOL;
        for(int k=-3;k<=3;k++){
            var edge=door.relative(front,1).relative(front.getClockWise(),k);
            put(l,new BlockPos(edge.getX(),c.getY()+3,edge.getZ()),Math.floorMod(k,2)==0?shade:Blocks.WHITE_WOOL);
            var display=door.relative(front,2).relative(front.getClockWise(),k);
            if(Math.abs(k)>1){put(l,new BlockPos(display.getX(),c.getY(),display.getZ()),Blocks.CHERRY_PLANKS);
                if(k%2==0)put(l,new BlockPos(display.getX(),c.getY()+1,display.getZ()),style%2==0?Blocks.CAKE:Blocks.POTTED_PINK_TULIP);}
        }
        for(int side:new int[]{-4,4}){var lamp=door.relative(front,2).relative(front.getClockWise(),side);
            put(l,lamp,Blocks.BARREL);put(l,lamp.above(),Blocks.LANTERN);}
    }
    private static void fountain(ServerLevel l,BlockPos at){
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(x*x+z*z<=10){
            put(l,at.offset(x,-1,z),Blocks.SMOOTH_QUARTZ);
            boolean rim=(x+1)*(x+1)+z*z>10||(x-1)*(x-1)+z*z>10||
                x*x+(z+1)*(z+1)>10||x*x+(z-1)*(z-1)>10;
            put(l,at.offset(x,0,z),rim?Blocks.SMOOTH_QUARTZ:Blocks.WATER);
        }
        put(l,at,Blocks.SEA_LANTERN);put(l,at.above(),Blocks.SMOOTH_QUARTZ);put(l,at.above(2),Blocks.SEA_LANTERN);
    }
    private static void house(ServerLevel l,BlockPos c,int style,Direction front){
        Block wall=switch(style){case 0->Blocks.PINK_CONCRETE;case 1->Blocks.LIGHT_BLUE_CONCRETE;case 2->Blocks.YELLOW_TERRACOTTA;case 3->Blocks.WHITE_CONCRETE;default->Blocks.LIME_TERRACOTTA;};
        Block trim=style%2==0?Blocks.WHITE_CONCRETE:Blocks.CALCITE;
        for(int x=-5;x<=5;x++)for(int z=-4;z<=4;z++){
            put(l,c.offset(x,-1,z),Blocks.CHERRY_PLANKS);
            if(Math.abs(x)==5||Math.abs(z)==4)for(int y=0;y<=4;y++)put(l,c.offset(x,y,z),y==0||y==4?trim:wall);
        }
        for(int x:new int[]{-5,5})for(int z:new int[]{-4,4})for(int y=0;y<=5;y++)put(l,c.offset(x,y,z),Blocks.STRIPPED_BIRCH_LOG);
        for(int y=1;y<=2;y++){
            for(int z:new int[]{-2,2})for(int x:new int[]{-5,5})put(l,c.offset(x,y,z),Blocks.CYAN_STAINED_GLASS);
            for(int x:new int[]{-3,3})for(int z:new int[]{-4,4})put(l,c.offset(x,y,z),Blocks.CYAN_STAINED_GLASS);
        }
        var door=c.relative(front,front.getAxis()==Direction.Axis.X?5:4);
        var facing=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING,front);
        ActivityBlocks.set(l,door,facing.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),2);
        ActivityBlocks.set(l,door.above(),facing.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
        put(l,door.above(2),Blocks.SEA_LANTERN);
        for(int x=-6;x<=6;x++)for(int z=-5;z<=5;z++){
            put(l,c.offset(x,5,z),Math.floorMod(x+z,2)==0?Blocks.DARK_OAK_PLANKS:Blocks.SPRUCE_PLANKS);
            if(Math.abs(x)<5&&Math.abs(z)<4)put(l,c.offset(x,6,z),Blocks.DARK_OAK_PLANKS);
        }
        for(int k=-3;k<=3;k++){
            var awning=door.relative(front,k==0?1:1).relative(front.getClockWise(),k);
            put(l,new BlockPos(awning.getX(),c.getY()+3,awning.getZ()),Math.floorMod(k,2)==0?Blocks.RED_WOOL:Blocks.WHITE_WOOL);
        }
        for(int side:new int[]{-3,3}){
            var pot=door.relative(front,2).relative(front.getClockWise(),side);
            put(l,pot,side<0?Blocks.POTTED_BLUE_ORCHID:Blocks.POTTED_PINK_TULIP);
        }
        put(l,c.offset(0,0,0),Blocks.SEA_LANTERN);
        put(l,c.offset(-2,0,0),ModBlocks.LOW_MARBLE_TABLE.get());
    }
    private static void streetLight(ServerLevel l,BlockPos c){
        put(l,c,Blocks.POLISHED_BLACKSTONE_BRICKS);
        for(int y=1;y<=4;y++)put(l,c.above(y),Blocks.IRON_BARS);
        put(l,c.above(5),Blocks.SEA_LANTERN);
        for(Direction d:Direction.Plane.HORIZONTAL){put(l,c.above(4).relative(d),Blocks.CHAIN);put(l,c.above(3).relative(d),Blocks.LANTERN);}
    }
    private static void tree(ServerLevel l,BlockPos c){
        for(int y=0;y<=4;y++)put(l,c.above(y),Blocks.CHERRY_LOG);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=3;y<=6;y++)
            if(Math.abs(x)+Math.abs(z)+Math.abs(y-4)<=4)put(l,c.offset(x,y,z),Blocks.CHERRY_LEAVES);
        for(int x:new int[]{-3,3})for(int z:new int[]{-3,3})put(l,c.offset(x,0,z),Blocks.FLOWERING_AZALEA);
    }
    private static void car(ServerLevel l,BlockPos c,Block color){
        for(int x=-1;x<=1;x++)for(int z=-2;z<=2;z++)put(l,c.offset(x,0,z),color);
        for(int z=-1;z<=1;z++)put(l,c.offset(0,1,z),Blocks.TINTED_GLASS);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})put(l,c.offset(x,-1,z),Blocks.BLACK_CONCRETE);
        for(int x:new int[]{-1,1}){put(l,c.offset(x,0,-2),Blocks.SEA_LANTERN);put(l,c.offset(x,0,2),Blocks.REDSTONE_LAMP);}
    }
    static void removeBuild(ServerLevel l,BlockPos c){
        if(!l.getBlockState(c.below(2)).is(Blocks.ORANGE_GLAZED_TERRACOTTA))return;
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++)for(int y=-3;y<=8;y++){
            var at=c.offset(x,y,z);if(!l.isEmptyBlock(at))put(l,at,Blocks.AIR);
        }
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){
        var found=new ArrayList<Entity>();for(var e:l.getEntitiesOfClass(Entity.class,new AABB(c).inflate(52,12,52),e->e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))found.add(e);
        for(var e:l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(52,12,52),e->s.owner.equals(e.owner)&&
            (e.kind()==ScenePropEntity.PLATE||e.kind()==ScenePropEntity.FEAST_CART)))if(!found.contains(e))found.add(e);
        found.forEach(Entity::discard);
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        var c=center(p,s);build(l,c);clean(l,s,c);
        var state=s.loop;state.feastVoiceUntil=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.GRAND_FEAST));
        state.feastEndAt=0;state.feastFinished=false;state.feastNextWaveAt=0;state.feastCartAt=0;state.feastCartSlot=0;state.feastGuestCanEatAt=0;
        state.feastWave=0;state.feastWaveKind=0;state.feastBasketCount=0;
        for(int i=0;i<PLATES;i++){state.feastPlateFood[i]=0;state.feastPlateRefillAt[i]=0;state.feastBasketFood[i]=0;}
        ensureFather(p,s,l,c);ensureGuests(p,s,l,c);ensurePlates(l,s,c);ensureCart(l,s,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        LoopProps.hold(l,c);build(l,c);ensureFather(p,s,l,c);ensureGuests(p,s,l,c);ensurePlates(l,s,c);ensureCart(l,s,c);
        l.setWeatherParameters(120000,0,false,false);l.setRainLevel(0);l.setThunderLevel(0);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0));
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        if(!l.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(36)).inflate(7),e->e.getPersistentData().getBoolean("CoralineFeastFather")&&e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var family=ParentAccess.family(p.server,s.owner);
        var father=ModEntities.DAD.get().create(l);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);
        father.getPersistentData().putBoolean("CoralineFeastFather",true);father.getPersistentData().putUUID(OWNER,s.owner);
        father.getPersistentData().putUUID("RealmDoor",s.door);father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+36.5);father.setYRot(90);father.setYBodyRot(90);l.addFreshEntity(father);
    }
    private static Vec3 ring(BlockPos c,double progress){
        double n=Math.floorMod((long)Math.floor(progress), (long)RING_LENGTH)+(progress-Math.floor(progress));
        double x,z;if(n<2*RING_DEPTH){x=RING_WIDTH;z=RING_DEPTH-n;}
        else if((n-=2*RING_DEPTH)<2*RING_WIDTH){x=RING_WIDTH-n;z=-RING_DEPTH;}
        else if((n-=2*RING_WIDTH)<2*RING_DEPTH){x=-RING_WIDTH;z=-RING_DEPTH+n;}
        else{x=-RING_WIDTH+(n-2*RING_DEPTH);z=RING_DEPTH;}
        return new Vec3(c.getX()+.5+x,c.getY(),c.getZ()+.5+z);
    }
    private static void ensureGuests(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        var existing=l.getEntitiesOfClass(Villager.class,new AABB(c).inflate(25,8,25),v->v.getPersistentData().getBoolean(VILLAGER)&&v.getPersistentData().hasUUID(OWNER)&&s.owner.equals(v.getPersistentData().getUUID(OWNER)));
        boolean[] occupied=new boolean[GUESTS];for(var v:existing){int i=v.getPersistentData().getInt("FeastIndex");if(i>=0&&i<GUESTS)occupied[i]=true;}
        for(int i=0;i<GUESTS;i++)if(!occupied[i]){
            var villager=EntityType.VILLAGER.create(l);if(villager==null)continue;
            Vec3 pos=ring(c,i*RING_LENGTH/GUESTS);villager.setPos(pos.x,pos.y,pos.z);villager.setNoAi(true);villager.noPhysics=true;
            villager.setPersistenceRequired();villager.setCustomName(Component.literal(VillageActivity.RESIDENT_NAME));villager.setCustomNameVisible(false);
            villager.getPersistentData().putBoolean("CoralineDecoration",true);villager.getPersistentData().putBoolean(VILLAGER,true);
            villager.getPersistentData().putUUID(OWNER,s.owner);villager.getPersistentData().putInt("FeastIndex",i);
            villager.getPersistentData().putLong("FeastEatAt",stateStart(s)+60+i*8L);
            l.addFreshEntity(villager);
        }
    }
    private static long stateStart(VisitData.Scene s){return s.loop.feastVoiceUntil-VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.GRAND_FEAST));}
    private static ScenePropEntity display(ServerLevel l,VisitData.Scene s,BlockPos c,int slot){
        var at=plate(c,slot);
        return l.getEntitiesOfClass(ScenePropEntity.class,new AABB(at).inflate(.8),e->e.kind()==ScenePropEntity.PLATE&&e.slot==slot&&s.owner.equals(e.owner)).stream().findFirst().orElse(null);
    }
    private static void ensurePlates(ServerLevel l,VisitData.Scene s,BlockPos c){
        for(int slot=0;slot<PLATES;slot++){
            var prop=display(l,s,c,slot);
            if(prop==null){prop=LoopProps.spawn(l,s,ScenePropEntity.PLATE,Vec3.atBottomCenterOf(plate(c,slot)),slot);prop.getPersistentData().putUUID(OWNER,s.owner);}
            boolean covered=s.loop.feastPlateFood[slot]!=0&&s.loop.feastPlateRefillAt[slot]>l.getServer().overworld().getGameTime();
            ItemStack want=covered?ItemStack.EMPTY:serving(s.loop.feastPlateFood[slot]);
            if(prop.mode()!=(covered?1:0))prop.mode(covered?1:0);
            prop.cakeStyle(s.loop.feastPlateFood[slot]>=15?4:s.loop.feastPlateFood[slot]>=12?3:s.loop.feastPlateFood[slot]>=7?2:1);
            if(!ItemStack.isSameItemSameTags(prop.food(0),want)||prop.food(0).getCount()!=want.getCount())prop.food(0,want);
        }
    }
    private static ScenePropEntity ensureCart(ServerLevel l,VisitData.Scene s,BlockPos c){
        var carts=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(22,8,22),
            e->e.kind()==ScenePropEntity.FEAST_CART&&s.owner.equals(e.owner));
        if(!carts.isEmpty())return carts.get(0);
        var cart=LoopProps.spawn(l,s,ScenePropEntity.FEAST_CART,Vec3.atBottomCenterOf(c),99);
        cart.getPersistentData().putUUID(OWNER,s.owner);return cart;
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var st=s.loop;long now=OtherMotherLoop.now(p);
        if(p.tickCount%20==0)prepare(p,s,p.serverLevel(),c);
        moveGuests(p,s,c,now);
        moveCart(p.serverLevel(),s,c,now);
        if(st.feastFinished)return;
        if(now<st.feastVoiceUntil){if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Listen to Father. The feast begins when he finishes speaking."),true);return;}
        if(st.feastEndAt==0){st.feastEndAt=now+DURATION;st.feastNextWaveAt=now+20;OtherMotherLoop.changed(p);
            p.sendSystemMessage(Component.literal("The Grand Feast begins! Listen for serving bells, race the guests to each wave, and fill your basket."));}
        if(now>=st.feastEndAt){
            st.feastFinished=true;for(int i=0;i<PLATES;i++)st.feastPlateFood[i]=0;
            ensurePlates(p.serverLevel(),s,c);OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
            deliverBasket(p,s);
            p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,1,1);
            p.sendSystemMessage(Component.literal("The feast is over. The return portal has appeared at the town entrance."));return;
        }
        if(now>=st.feastNextWaveAt)wave(p,s,c,now);
        if(p.tickCount%5==0)refresh(p,s,c,now);
        if(st.feastEndAt-now<=200&&p.tickCount%20==0){
            p.serverLevel().playSound(null,c,SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,.6F,1.5F);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,c.getX()+.5,c.getY()+7,c.getZ()+.5,18,15,2,15,.04);
        }
        if(p.tickCount%20==0)p.displayClientMessage(Component.literal("Grand Feast: "+Math.max(0,(st.feastEndAt-now+19)/20)+"s | basket "+st.feastBasketCount+" | wave "+st.feastWave),true);
        if(p.getY()<c.getY()-3||Math.abs(p.getX()-(c.getX()+.5))>47||Math.abs(p.getZ()-(c.getZ()+.5))>47){p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+36.5,180,0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}
    }
    private static void refresh(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        ensurePlates(p.serverLevel(),s,c);
    }
    private static void wave(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var st=s.loop;long elapsed=DURATION-(st.feastEndAt-now);boolean frenzy=elapsed>=3000;
        st.feastWave++;int roll=p.getRandom().nextInt(100);
        st.feastWaveKind=elapsed>1200&&roll<8?1:elapsed>1800&&roll<14?2:roll<27?3:roll<37?4:roll<43?5:elapsed>2600&&roll<52?6:0;
        int count=switch(st.feastWaveKind){case 2->1;case 3,6->13;case 4->3;default->6+p.getRandom().nextInt(4);};
        if(frenzy)count=Math.max(count,10);
        for(int i=0;i<PLATES;i++){st.feastPlateFood[i]=0;st.feastPlateRefillAt[i]=0;}
        int offset=p.getRandom().nextInt(PLATES),stride=p.getRandom().nextBoolean()?5:7;
        for(int n=0;n<count;n++){
            int slot=(offset+n*stride)%PLATES,id=rollFood(p,elapsed);
            if(st.feastWaveKind==1)id=p.getRandom().nextBoolean()?11:4;
            if(st.feastWaveKind==5)id=Math.max(11,id);
            if(st.feastWaveKind==6)id=12+p.getRandom().nextInt(4);
            st.feastPlateFood[slot]=id;st.feastPlateRefillAt[slot]=now+18;
        }
        if((st.feastWaveKind==2||st.feastWaveKind==6||frenzy&&st.feastWave%3==0)&&elapsed>1200){
            int centerSlot=p.getRandom().nextInt(PLATES);
            st.feastPlateFood[centerSlot]=elapsed>3000?15:14;st.feastPlateRefillAt[centerSlot]=now+18;
        }
        int seconds=frenzy?5:st.feastWaveKind==5?5:8+p.getRandom().nextInt(8);
        st.feastNextWaveAt=now+seconds*20L;OtherMotherLoop.changed(p);ensurePlates(p.serverLevel(),s,c);
        st.feastCartAt=now;st.feastCartSlot=(offset%8);st.feastGuestCanEatAt=now+(frenzy?29:42);OtherMotherLoop.changed(p);
        p.serverLevel().playSound(null,c,SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,1.2F,frenzy?1.35F:1F);
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,c.getX()+.5,c.getY()+3,c.getZ()+.5,22,4,1,11,.02);
        String label=switch(st.feastWaveKind){case 1->"Dessert rush!";case 2->"Golden platter!";case 3->"Family-style serving!";
            case 4->"A nearly empty fakeout!";case 5->"Double serving!";case 6->"Jackpot course!";default->"Fresh serving!";};
        p.displayClientMessage(Component.literal(label+" Covers lift in a moment."),true);
    }
    private static void moveCart(ServerLevel l,VisitData.Scene s,BlockPos c,long now){
        var cart=ensureCart(l,s,c);long age=now-s.loop.feastCartAt;
        if(s.loop.feastCartAt==0||age<0||age>22||s.loop.feastFinished){if(cart.mode()!=0)cart.mode(0);return;}
        if(cart.mode()!=1)cart.mode(1);
        int side=s.loop.feastWave%2==0?1:-1;double progress=Math.min(1,age/18.0);
        cart.setPos(c.getX()+.5+side*(15-progress*8),c.getY()+.05,c.getZ()-10.5+s.loop.feastCartSlot*3);
        cart.setYRot(side>0?90:270);
    }
    private static void deliverBasket(ServerPlayer p,VisitData.Scene s){
        int count=s.loop.feastBasketCount;
        for(int id=1;id<s.loop.feastBasketFood.length;id++){
            int n=s.loop.feastBasketFood[id];s.loop.feastBasketFood[id]=0;
            while(n>0){int batch=Math.min(n,64);n-=batch;var stack=new ItemStack(food(id),batch);
                p.getInventory().add(stack);if(!stack.isEmpty())p.drop(stack,false);}
        }
        s.loop.feastBasketCount=0;OtherMotherLoop.changed(p);
        p.sendSystemMessage(Component.literal("Your feast basket is yours: "+count+" food items collected."));
    }
    private static void moveGuests(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var l=p.serverLevel();var st=s.loop;double elapsed=st.feastEndAt==0?0:Math.max(0,now-(st.feastEndAt-DURATION));
        var guests=l.getEntitiesOfClass(Villager.class,new AABB(c).inflate(24,8,24),e->e.getPersistentData().getBoolean(VILLAGER)&&e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER)));
        for(var v:guests){
            int i=v.getPersistentData().getInt("FeastIndex");if(i<0||i>=GUESTS)continue;
            int personality=i%7,target=-1;double best=-Double.MAX_VALUE;
            if(st.feastEndAt>0&&!st.feastFinished)for(int slot=0;slot<PLATES;slot++){
                int id=st.feastPlateFood[slot];if(id==0||now<st.feastPlateRefillAt[slot])continue;
                var dish=plate(c,slot);double distance=v.distanceToSqr(dish.getX()+.5,c.getY(),dish.getZ()+.5);
                if(personality==2&&id!=4&&id!=10&&id!=11&&id!=13&&id!=15)continue;
                if((personality==3&&id<11)||(personality==5&&now%90<25))continue;
                double value=switch(personality){case 0->id*4-distance*.04;case 1->25-distance;case 2->id*2-distance*.12;
                    case 3->id*3-distance*.05;case 4->8-distance*.2;case 5->id*2-distance*.07;default->id==15?100:id*2-distance*.12;};
                if(value>best){best=value;target=slot;}
            }
            double speed=(.09+.015*(i%4))*(st.feastEndAt>0&&st.feastEndAt-now<600?1.35:1);
            if(personality==0||personality==4)speed*=1.25;
            Vec3 goal=target<0?ring(c,i*RING_LENGTH/GUESTS+elapsed*speed):new Vec3(
                c.getX()+.5+(target<8?-4.2:4.2),c.getY(),plate(c,target).getZ()+.5);
            Vec3 next=goal;
            if(target>=0&&Math.signum(v.getX()-(c.getX()+.5))!=Math.signum(goal.x-(c.getX()+.5))&&Math.abs(v.getZ()-(c.getZ()+.5))<14.4)
                next=new Vec3(c.getX()+.5+Math.signum(v.getX()-(c.getX()+.5))*5.2,c.getY(),c.getZ()+.5+(goal.z>c.getZ()?15.7:-15.7));
            Vec3 step=next.subtract(v.position());
            if(step.horizontalDistanceSqr()>400)v.teleportTo(next.x,next.y,next.z);
            else{
                Vec3 movement=step.lengthSqr()>speed*speed?step.normalize().scale(speed):step;
                for(var other:guests)if(other!=v){
                    Vec3 apart=v.position().subtract(other.position());double gap=apart.horizontalDistanceSqr();
                    if(gap>.01&&gap<.9)movement=movement.add(apart.x*.035,0,apart.z*.035);
                }
                if(movement.horizontalDistanceSqr()>speed*speed*1.3)
                    movement=movement.normalize().scale(speed*1.14);
                v.noPhysics=true;v.move(MoverType.SELF,movement);v.setDeltaMovement(Vec3.ZERO);
            }
            if(step.horizontalDistanceSqr()>.0001){float yaw=(float)Math.toDegrees(Math.atan2(-step.x,step.z));v.setYRot(yaw);v.setYBodyRot(yaw);v.setYHeadRot(yaw);}
            if(v.distanceToSqr(p)<.7&&now%15==i%15){var shove=p.position().subtract(v.position()).multiply(.06,0,.06);p.setDeltaMovement(p.getDeltaMovement().add(shove));}
            if(st.feastEndAt==0||st.feastFinished||now<st.feastGuestCanEatAt||now<v.getPersistentData().getLong("FeastEatAt"))continue;
            if(target>=0&&v.distanceToSqr(goal)<2.2){take(p,s,c,target,true);v.getPersistentData().putLong("FeastEatAt",now+35+p.getRandom().nextInt(70));
                l.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,v.getX(),v.getY()+1.8,v.getZ(),4,.2,.2,.2,.01);
            }
        }
    }
    private static void take(ServerPlayer p,VisitData.Scene s,BlockPos c,int slot,boolean villager){
        var st=s.loop;int id=st.feastPlateFood[slot];if(id==0||OtherMotherLoop.now(p)<st.feastPlateRefillAt[slot])return;
        ItemStack served=serving(id);
        st.feastPlateFood[slot]=0;st.feastPlateRefillAt[slot]=0;
        var prop=display(p.serverLevel(),s,c,slot);if(prop!=null)prop.food(0,ItemStack.EMPTY);
        OtherMotherLoop.changed(p);
        if(villager){p.serverLevel().playSound(null,plate(c,slot),SoundEvents.VILLAGER_YES,SoundSource.NEUTRAL,.3F,1.2F);return;}
        String name=served.getHoverName().getString();st.feastBasketFood[id]+=served.getCount();st.feastBasketCount+=served.getCount();OtherMotherLoop.changed(p);
        p.serverLevel().playSound(null,plate(c,slot),SoundEvents.ITEM_PICKUP,SoundSource.PLAYERS,.7F,1.4F);
        if(id>=12)p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,plate(c,slot).getX()+.5,c.getY()+2,plate(c,slot).getZ()+.5,15,.3,.2,.3,.03);
        p.displayClientMessage(Component.literal((id>=15?"JACKPOT! ":id>=12?"Rare dish! ":"")+name+" → basket "+st.feastBasketCount),true);
    }
    public static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(s.loop.activity!=OtherMotherActivities.Activity.GRAND_FEAST||prop.kind()!=ScenePropEntity.PLATE||prop.slot<0||prop.slot>=PLATES)return false;
        if(!s.loop.feastFinished&&s.loop.feastEndAt>0&&s.loop.feastPlateFood[prop.slot]!=0){
            if(OtherMotherLoop.now(p)<s.loop.feastPlateRefillAt[prop.slot])p.displayClientMessage(Component.literal("The silver cover is still lifting…"),true);
            else take(p,s,center(p,s),prop.slot,false);
        }
        else p.displayClientMessage(Component.literal(s.loop.feastEndAt==0?"Wait for Father's introduction.":s.loop.feastFinished?"The feast is over.":"That plate is empty. Watch for a fresh serving."),true);
        return true;
    }
}
