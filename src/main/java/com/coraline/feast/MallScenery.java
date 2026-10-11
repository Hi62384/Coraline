package com.coraline.feast;
import static com.coraline.feast.ExpeditionActivities.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import com.coraline.registry.ModEntities;

/** Two-storey arcade: each retail interior uses a different authored merchandise arrangement. */
final class MallScenery {
    static int side(int store){return store<16?(store%8<4?-1:1):(store%4<2?-1:1);}
    static int floor(int store){return store<16?store/8:store>=20?1:0;}
    static int row(int store){return store<16?store%4:4+store%2;}
    static BlockPos shop(BlockPos c,int store){return c.offset(side(store)*30,floor(store)*8,-14-row(store)*22);}
    static BlockPos display(BlockPos c,int store,int slot){return shop(c,store).offset((slot<4?-1:1)*side(store)*3,1,-6+(slot%4)*4);}
    static final Block[] PALETTES={Blocks.PINK_TERRACOTTA,Blocks.LIGHT_BLUE_TERRACOTTA,Blocks.MOSS_BLOCK,Blocks.YELLOW_TERRACOTTA,Blocks.WHITE_TERRACOTTA,Blocks.DARK_OAK_PLANKS,Blocks.CYAN_TERRACOTTA,Blocks.ORANGE_TERRACOTTA,Blocks.POLISHED_BLACKSTONE_BRICKS,Blocks.QUARTZ_BRICKS,Blocks.PURPLE_TERRACOTTA,Blocks.CUT_SANDSTONE,Blocks.MAGENTA_TERRACOTTA,Blocks.BLUE_TERRACOTTA,Blocks.CHERRY_PLANKS,Blocks.RED_TERRACOTTA};
    static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        box(l,c,-45,-2,-136,45,-1,12,Blocks.SMOOTH_QUARTZ);for(int x=-44;x<=44;x++)for(int z=-135;z<=11;z++)put(l,c,x,-1,z,Math.floorMod(x+z,8)==0?Blocks.CHISELED_QUARTZ_BLOCK:Math.abs(x)<14?Blocks.CALCITE:Blocks.SMOOTH_QUARTZ);
        box(l,c,-45,0,-136,-44,18,12,Blocks.WHITE_TERRACOTTA);box(l,c,44,0,-136,45,18,12,Blocks.WHITE_TERRACOTTA);box(l,c,-45,0,-136,45,18,-135,Blocks.WHITE_TERRACOTTA);box(l,c,-45,0,11,45,18,12,Blocks.WHITE_TERRACOTTA);
        box(l,c,-44,18,-135,44,18,11,Blocks.QUARTZ_BRICKS);box(l,c,-12,18,-130,12,18,5,Blocks.LIGHT_BLUE_STAINED_GLASS);
        for(int x:new int[]{-14,14})for(int z=-128;z<=4;z+=22){box(l,c,x,0,z,x,17,z,Blocks.QUARTZ_PILLAR);put(l,c,x,6,z,Blocks.GOLD_BLOCK);put(l,c,x,14,z,Blocks.GOLD_BLOCK);}
        box(l,c,-43,7,-134,-13,7,10,Blocks.SMOOTH_QUARTZ);box(l,c,13,7,-134,43,7,10,Blocks.SMOOTH_QUARTZ);box(l,c,-13,7,-134,13,7,-83,Blocks.SMOOTH_QUARTZ);box(l,c,-13,7,3,13,7,10,Blocks.SMOOTH_QUARTZ);
        for(int x:new int[]{-13,13})box(l,c,x,8,-82,x,8,2,Blocks.WHITE_STAINED_GLASS_PANE);
        for(int i=0;i<8;i++){box(l,c,-4,i-1,-74-i,4,i-1,-74-i,Blocks.QUARTZ_BRICKS);for(int x=-4;x<=4;x++)ActivityBlocks.set(l,c.offset(x,i,-74-i),Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH),2);}
        box(l,c,-4,7,-83,4,7,-82,Blocks.QUARTZ_BRICKS);for(int z=-81;z<=-74;z++){put(l,c,-5,-z-73,z,Blocks.GOLD_BLOCK);put(l,c,5,-z-73,z,Blocks.GOLD_BLOCK);}
        ActivityScenery.fountain(l,c,0,-35);ActivityScenery.tree(l,c,0,-60);
        for(int z:new int[]{-16,-48,-69}){for(int x:new int[]{-8,8}){box(l,c,x-1,0,z-1,x+1,0,z+1,Blocks.QUARTZ_BRICKS);box(l,c,x-1,1,z-1,x+1,1,z+1,Blocks.FLOWERING_AZALEA_LEAVES);}for(int x=-2;x<=2;x++)put(l,c,x,0,z,Blocks.CHERRY_SLAB);}
        for(int z:new int[]{-15,-40,-66}){box(l,c,0,14,z,0,17,z,Blocks.CHAIN);for(int x=-3;x<=3;x++)for(int dz=-2;dz<=2;dz++)if(Math.abs(x)==3||Math.abs(dz)==2)put(l,c,x,13,z+dz,(x+dz)%2==0?Blocks.SEA_LANTERN:Blocks.GOLD_BLOCK);}
        // A readable entrance directory, clock and conspicuous exit plaza.
        box(l,c,-8,0,2,-8,3,7,Blocks.CHERRY_PLANKS);sign(l,c.offset(-7,2,3),Direction.EAST,"GROUND FLOOR","Candy / Clothes","Garden / Toys","Builders / Textiles");sign(l,c.offset(-7,2,6),Direction.EAST,"UPPER FLOOR","Armory / Jewels","Luxury / Treasure","FOOD COURT: rear");
        box(l,c,-3,11,-134,3,15,-134,Blocks.GOLD_BLOCK);box(l,c,-2,12,-133,2,14,-133,Blocks.WHITE_CONCRETE);put(l,c,0,13,-132,Blocks.BLACK_CONCRETE);put(l,c,0,14,-132,Blocks.BLACK_CONCRETE);put(l,c,1,13,-132,Blocks.BLACK_CONCRETE);
        sign(l,c.offset(0,2,10),Direction.NORTH,"CHECKOUT / EXIT","Purchases follow you","Unspent tokens expire");
        for(int store=0;store<MallStock.STORES.length;store++)interior(l,s,c,store);
        // Food court: tables and stools around the upper entrance gallery.
        for(int x:new int[]{-8,0,8})for(int z:new int[]{4,8}){put(l,c,x,8,z,Blocks.CHERRY_FENCE);put(l,c,x,9,z,Blocks.CHERRY_PRESSURE_PLATE);put(l,c,x-2,8,z,Blocks.CHERRY_SLAB);put(l,c,x+2,8,z,Blocks.CHERRY_SLAB);}
        // Small kiosks occupy islands, not the walking lanes.
        for(int x:new int[]{-7,7}){box(l,c,x-1,0,-5,x+1,0,-3,Blocks.CHERRY_PLANKS);box(l,c,x-1,3,-5,x+1,3,-3,Blocks.PINK_WOOL);put(l,c,x,1,-4,Blocks.POTTED_FERN);put(l,c,x-1,1,-4,Blocks.LANTERN);}
        // Spacious shared food court connecting six specialist food stores.
        for(int x:new int[]{-8,0,8})for(int z:new int[]{-92,-106,-120}){
            put(l,c,x,8,z,Blocks.CHERRY_FENCE);put(l,c,x,9,z,Blocks.CHERRY_PRESSURE_PLATE);
            put(l,c,x-2,8,z,Blocks.CHERRY_SLAB);put(l,c,x+2,8,z,Blocks.CHERRY_SLAB);
        }
        for(int z:new int[]{-90,-112,-132}){
            box(l,c,-10,15,z,10,15,z,Blocks.CHERRY_FENCE);
            for(int x=-8;x<=8;x+=4)ActivityBlocks.set(l,c.offset(x,14,z),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),2);
        }
        sign(l,c.offset(0,11,-85),Direction.SOUTH,"FOOD COURT","Bakery / Street Food","Pizza / Breakfast","Drinks / Desserts");
        for(int side:new int[]{-1,1})for(int floor=0;floor<2;floor++){
            sign(l,c.offset(side*14,2+floor*8,-87),side<0?Direction.EAST:Direction.WEST,
                floor==0?"SPECIALTY WING":"FOOD COURT",
                floor==0?"Building / Redstone":"Pizza / Breakfast",
                floor==0?"Textiles / Outdoors":"Drinks / Desserts","Browse eight products");
        }
        var downstairs=new java.util.ArrayList<>(walkTargets(c,0));var upstairs=new java.util.ArrayList<>(walkTargets(c,1));
        for(var stops:java.util.List.of(downstairs,upstairs)){
            stops.removeIf(at->!l.getBlockState(at.below()).isSolidRender(l,at.below())||!l.isEmptyBlock(at)||!l.isEmptyBlock(at.above()));
            java.util.Collections.shuffle(stops,new java.util.Random(state(s).getLong("Seed")+stops.size()));
        }
        for(int i=0;i<48;i++){
            int floor=i%2;var stops=floor==0?downstairs:upstairs;
            var at=stops.get((i/2)%stops.size());
            var npc=employee(l,s,at,i+18,-1);
            npc.getPersistentData().putBoolean("MallShopper",true);
            npc.getPersistentData().putInt("MallFloor",floor);
            npc.getPersistentData().putLong("MallNextWalk",l.getGameTime()+i*5);
            npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(i%5==0?Items.COOKIE:i%5==1?Items.EMERALD:Items.BUNDLE));
        }
    }
    private static void interior(ServerLevel l,VisitData.Scene s,BlockPos c,int store){var b=shop(c,store);int side=side(store);Block color=PALETTES[store%PALETTES.length];
        box(l,b,-12,-1,-9,12,-1,9,color);box(l,b,-12,0,-10,12,6,-10,color);box(l,b,-12,0,10,12,6,10,color);box(l,b,side*12,0,-9,side*12,6,9,color);
        int front=-side*12;box(l,b,front,0,-9,front,4,9,Blocks.WHITE_STAINED_GLASS);box(l,b,front,0,-2,front,3,2,Blocks.AIR);box(l,b,front,5,-9,front,5,9,color);
        for(int z:new int[]{-7,7}){put(l,b,front,4,z,Blocks.SEA_LANTERN);box(l,b,-10,5,z,10,5,z,Blocks.CHERRY_SLAB);}
        var face=side<0?Direction.EAST:Direction.WEST;sign(l,b.offset(front-side,4,0),face,MallStock.STORES[store],MallStock.CATEGORIES[store], "Right-click to browse");
        for(int slot=0;slot<MallStock.ITEMS_PER_VISIT;slot++){var at=display(c,store,slot);box(l,at,0,-1,0,0,-1,0,store==10||store==9?Blocks.QUARTZ_BRICKS:Blocks.CHERRY_PLANKS);put(l,at,0,0,0,Blocks.SMOOTH_QUARTZ_SLAB);}
        // Purpose-built themes, with an open aisle around every merchandise pedestal.
        switch(store){
            case 0,14,23->{for(int z:new int[]{-7,7}){box(l,b,3,0,z,9,0,z,Blocks.CHERRY_PLANKS);for(int x=4;x<=8;x+=2)put(l,b,x,1,z,Blocks.CAKE);}for(int z=-8;z<=8;z++)put(l,b,front,5,z,z%2==0?Blocks.PINK_WOOL:Blocks.WHITE_WOOL);}
            case 1,8,10->{for(int z:new int[]{-7,7}){var mannequin=new net.minecraft.world.entity.decoration.ArmorStand(l,b.getX()+side*7+.5,b.getY(),b.getZ()+z+.5);ActivityScenery.mark(mannequin,s);mannequin.setNoGravity(true);mannequin.setItemSlot(EquipmentSlot.CHEST,new ItemStack(store==1?Items.LEATHER_CHESTPLATE:store==8?Items.IRON_CHESTPLATE:Items.DIAMOND_CHESTPLATE));mannequin.setYRot(side<0?-90:90);l.addFreshEntity(mannequin);}if(store==10)box(l,b,-8,0,-8,8,0,-8,Blocks.PURPLE_CARPET);}
            case 2->{for(int z=-8;z<=8;z+=4){put(l,b,side*9,0,z,Blocks.MOSS_BLOCK);put(l,b,side*9,1,z,Blocks.AZALEA);put(l,b,-side*8,0,z,Blocks.POTTED_CHERRY_SAPLING);}box(l,b,-8,5,-8,8,5,-8,Blocks.FLOWERING_AZALEA_LEAVES);}
            case 3->{for(int z=-7;z<=7;z++){put(l,b,side*8,0,z,Blocks.OAK_PLANKS);put(l,b,side*8,1,z,Blocks.RAIL);}put(l,b,side*6,0,-7,Blocks.NOTE_BLOCK);put(l,b,side*6,1,-7,Blocks.CARVED_PUMPKIN);}
            case 4->{for(int z:new int[]{-7,7}){put(l,b,side*8,0,z,Blocks.CHISELED_BOOKSHELF);put(l,b,side*8,1,z,Blocks.LANTERN);box(l,b,side>0?5:-9,0,z-1,side>0?9:-5,0,z-1,Blocks.CHERRY_SLAB);}}
            case 5->{box(l,b,-10,0,-9,10,3,-9,Blocks.BOOKSHELF);put(l,b,side*8,0,0,Blocks.LECTERN);put(l,b,side*8,0,7,Blocks.ENCHANTING_TABLE);}
            case 6->{put(l,b,side*8,0,-7,Blocks.ANVIL);put(l,b,side*8,0,0,Blocks.SMITHING_TABLE);put(l,b,side*8,0,7,Blocks.GRINDSTONE);}
            case 7->{box(l,b,-10,0,-8,10,0,-8,Blocks.BARREL);for(int x=-8;x<=8;x+=4)put(l,b,x,1,-8,Blocks.HAY_BLOCK);}
            case 9,11->{box(l,b,-10,0,-8,10,0,-8,Blocks.GOLD_BLOCK);for(int x=-8;x<=8;x+=4){put(l,b,x,1,-8,Blocks.AMETHYST_BLOCK);put(l,b,x,2,-8,Blocks.GLASS);}}
            case 12->{for(int z:new int[]{-7,7}){put(l,b,side*8,0,z,Blocks.CRYING_OBSIDIAN);put(l,b,side*8,1,z,Blocks.BREWING_STAND);}put(l,b,side*8,0,0,Blocks.CAULDRON);}
            case 13->{for(int z:new int[]{-7,0,7}){put(l,b,side*8,0,z,Blocks.NOTE_BLOCK);put(l,b,side*8,1,z,Blocks.JUKEBOX);}LoopProps.spawn(l,s,ScenePropEntity.PIANO,Vec3.atBottomCenterOf(b.offset(-side*7,0,6)),900+store).setYRot(side<0?90:-90);}
            case 15,20,21,22->{box(l,b,-10,0,-8,10,0,-8,Blocks.SMOOTH_QUARTZ);for(int x=-8;x<=8;x+=4){put(l,b,x,1,-8,Blocks.CAKE);put(l,b,x,2,-8,Blocks.GLASS);}}
            case 16->{for(int z:new int[]{-7,0,7}){put(l,b,side*9,0,z,Blocks.BRICKS);put(l,b,side*9,1,z,Blocks.CHISELED_QUARTZ_BLOCK);}}
            case 17->{for(int z:new int[]{-7,0,7}){put(l,b,side*9,0,z,Blocks.PISTON);put(l,b,side*9,1,z,Blocks.REDSTONE_LAMP);}}
            case 18->{for(int z=-8;z<=8;z+=4){put(l,b,side*9,0,z,Blocks.CYAN_WOOL);put(l,b,side*9,1,z,Blocks.PINK_WOOL);put(l,b,side*9,2,z,Blocks.YELLOW_WOOL);}}
            case 19->{put(l,b,side*9,0,-7,Blocks.CAMPFIRE);put(l,b,side*9,0,0,Blocks.BARREL);put(l,b,side*9,0,7,Blocks.CRAFTING_TABLE);}
        }
        box(l,b,side>0?6:-10,0,2,side>0?10:-6,0,2,Blocks.SMOOTH_QUARTZ);put(l,b,side*7,1,2,Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);employee(l,s,b.offset(side*8,0,4),store,store);
    }
    static FishingContestNpcEntity employee(ServerLevel l,VisitData.Scene s,BlockPos at,int skin,int store){var npc=ModEntities.FISHING_CONTEST_NPC.get().create(l);npc.configure(skin*3,skin%2==0,false,skin);npc.setFishing(false);npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);npc.setNoAi(store>=0);ActivityScenery.mark(npc,s);npc.getPersistentData().putInt("MallStore",store);npc.getPersistentData().putBoolean("MallNpc",true);npc.setPos(Vec3.atBottomCenterOf(at));npc.setYRot(store>=0&&side(store)<0?-90:90);npc.setYHeadRot(npc.getYRot());npc.setYBodyRot(npc.getYRot());if(store>=0)npc.setCustomName(Component.literal(MallStock.STORES[store]+" attendant"));l.addFreshEntity(npc);return npc;}
    /** Grounded, unobstructed waypoints: independent choices, not one shared loop. */
    static java.util.List<BlockPos> walkTargets(BlockPos c,int floor){
        var points=new java.util.ArrayList<BlockPos>();int y=floor*8;
        for(int side:new int[]{-1,1})for(int row=0;row<6;row++){
            points.add(c.offset(side*16,y,-14-row*22));
            points.add(c.offset(side*21,y,-14-row*22)); // browse inside the storefront
            points.add(c.offset(side*16,y,-5-row*22));
        }
        if(floor==0)for(int x:new int[]{-7,7})for(int z:new int[]{-8,-24,-44,-68,-94,-116})points.add(c.offset(x,0,z));
        for(int x:new int[]{-8,0,8}){
            points.add(c.offset(x,y,floor==0?1:6));
            for(int z:new int[]{-86,-100,-114,-130})points.add(c.offset(x,y,z));
        }
        return java.util.List.copyOf(points);
    }
    private MallScenery(){}
}
