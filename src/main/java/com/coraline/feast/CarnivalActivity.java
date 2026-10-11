package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;

/** A lit, bounded carnival with physical games, self-serve sweets and paid scenic rides. */
@Mod.EventBusSubscriber(modid="coraline")
public final class CarnivalActivity {
    private static final String OWNER="CoralineCarnivalOwner",GEN="CoralineCarnivalGeneration",NPC="CoralineCarnivalNPC",ATTENDANT="CoralineCarnivalAttendant";
    public static final int TOKEN_COUNT=15,CROWD_COUNT=56;
    public enum Game { RING,DARTS,DUCKS,STRIKER,MIRROR,WHEEL,INSTANT }
    public enum Booth {
        RING_TOSS("RING TOSS",-21,27,2,Game.RING,25,4,Blocks.ORANGE_WOOL,Blocks.YELLOW_WOOL),
        BALLOON_DARTS("BALLOON DARTS",21,27,3,Game.DARTS,25,3,Blocks.RED_WOOL,Blocks.ORANGE_WOOL),
        DUCK_POND("DUCK POND",-21,12,2,Game.DUCKS,25,6,Blocks.CYAN_WOOL,Blocks.YELLOW_WOOL),
        HIGH_STRIKER("HIGH STRIKER",21,12,4,Game.STRIKER,25,7,Blocks.RED_WOOL,Blocks.WHITE_WOOL),
        MIRROR_MAZE("MIRROR MAZE",-21,-3,5,Game.MIRROR,25,4,Blocks.PURPLE_WOOL,Blocks.MAGENTA_WOOL),
        LUCKY_WHEEL("LUCKY WHEEL",21,-3,3,Game.WHEEL,20,3,Blocks.BLUE_WOOL,Blocks.YELLOW_WOOL),
        POPCORN("POPCORN",-42,24,0,Game.INSTANT,0,0,Blocks.YELLOW_WOOL,Blocks.WHITE_WOOL),
        CANDY_APPLES("LOLLIPOPS",42,24,0,Game.INSTANT,0,0,Blocks.RED_WOOL,Blocks.PINK_WOOL),
        FERRIS_WHEEL("FERRIS WHEEL",0,-19,1,Game.INSTANT,0,0,Blocks.ORANGE_WOOL,Blocks.YELLOW_WOOL),
        CAROUSEL("CAROUSEL",36,-15,1,Game.INSTANT,0,0,Blocks.PINK_WOOL,Blocks.YELLOW_WOOL),
        COTTON_CANDY("COTTON CANDY",-42,9,0,Game.INSTANT,0,0,Blocks.PINK_WOOL,Blocks.WHITE_WOOL),
        PIE_CART("HOT DOGS",42,9,0,Game.INSTANT,0,0,Blocks.ORANGE_WOOL,Blocks.WHITE_WOOL),
        LEMONADE("LEMONADE",-42,-7,0,Game.INSTANT,0,0,Blocks.YELLOW_WOOL,Blocks.CYAN_WOOL),
        CHOCOLATE("COCOA BEETLES",42,-7,0,Game.INSTANT,0,0,Blocks.BROWN_WOOL,Blocks.PINK_WOOL);
        public final String title;public final int x,z,difficulty,seconds,steps;public final Game game;public final Block stripeA,stripeB;
        Booth(String title,int x,int z,int difficulty,Game game,int seconds,int steps,Block a,Block b){
            this.title=title;this.x=x;this.z=z;this.difficulty=difficulty;this.game=game;this.seconds=seconds;this.steps=steps;this.stripeA=a;this.stripeB=b;
        }
        public BlockPos console(BlockPos c){return c.offset(x,0,z+3);}
    }
    private CarnivalActivity(){}
    private static void title(ServerPlayer p,String heading,String hint){
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5,32,8));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(heading).withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(hint).withStyle(ChatFormatting.WHITE)));
    }
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(320);}
    public static boolean exitReady(VisitData.Scene s){return true;}
    private static int cost(Booth b){return b==Booth.FERRIS_WHEEL?3:b==Booth.CAROUSEL||b==Booth.MIRROR_MAZE||b==Booth.LUCKY_WHEEL?2:1;}
    static void build(ServerLevel level,BlockPos c){
        if(level.getBlockState(c.below(3)).is(Blocks.LIME_GLAZED_TERRACOTTA))return;
        if(level.getBlockState(c.below()).is(Blocks.ORANGE_GLAZED_TERRACOTTA))removeBuild(level,c);
        for(int x=-56;x<56;x++)for(int z=-56;z<56;z++){
            var ground=c.offset(x,-1,z);boolean rim=x==-56||x==55||z==-56||z==55;
            boolean avenue=Math.abs(x)<=4&&z>=-41&&z<=50;
            boolean cross=Math.abs(z-5)<=4&&x>=-49&&x<=49;
            boolean plaza=Math.abs(x)<=13&&z>=-10&&z<=29;
            Block surface=rim?Blocks.POLISHED_BLACKSTONE:avenue||cross?
                (Math.abs(x)<=1&&Math.floorMod(z,11)<=1?Blocks.GOLD_BLOCK:
                 Math.abs(x)==4||Math.abs(z-5)==4?Blocks.ORANGE_TERRACOTTA:
                 Math.floorMod(x+z,6)==0?Blocks.PINK_TERRACOTTA:Blocks.SMOOTH_SANDSTONE):
                plaza?Blocks.TERRACOTTA:Blocks.GRASS_BLOCK;
            ActivityBlocks.set(level,ground.below(),Blocks.DIRT.defaultBlockState(),2);
            ActivityBlocks.set(level,ground,surface.defaultBlockState(),2);
            if(rim){ActivityBlocks.set(level,c.offset(x,0,z),Blocks.DARK_OAK_FENCE.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(x,1,z),Blocks.DARK_OAK_FENCE.defaultBlockState(),2);}
        }
        for(int x=-52;x<=52;x+=8)for(int z:new int[]{-54,53})post(level,c.offset(x,0,z),4);
        for(int z=-46;z<=46;z+=8)for(int x:new int[]{-54,53})post(level,c.offset(x,0,z),4);
        for(var booth:Booth.values())if(booth.game!=Game.INSTANT)booth(level,c,booth);
        for(var food:new Booth[]{Booth.POPCORN,Booth.CANDY_APPLES,Booth.COTTON_CANDY,Booth.PIE_CART,Booth.LEMONADE,Booth.CHOCOLATE})
            foodCart(level,c.offset(food.x,0,food.z),food);
        ferrisWheel(level,c);carousel(level,c.offset(36,0,-32));mirrorGarden(level,c.offset(-36,0,-32),level.getSeed()^c.asLong());
        // Flower-lined processional boulevard with illuminated gateways and banners.
        for(int side:new int[]{-1,1})for(int z=-15;z<=46;z++){
            if(z%9<7)ActivityBlocks.set(level,c.offset(side*7,0,z),ActivityWorld.persistentLeaves(Blocks.FLOWERING_AZALEA_LEAVES),2);
            if(z%4==0)ActivityBlocks.set(level,c.offset(side*10,0,z),(z%8==0?Blocks.ORANGE_TULIP:Blocks.PINK_TULIP).defaultBlockState(),2);
            if(z%9==0){ActivityBlocks.set(level,c.offset(side*11,0,z),Blocks.POLISHED_BLACKSTONE.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(side*11,1,z),Blocks.DARK_OAK_FENCE.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(side*11,2,z),Blocks.SEA_LANTERN.defaultBlockState(),2);}
        }
        for(int z:new int[]{-9,18,42})gateway(level,c,z);
        for(int z:new int[]{-11,4,19,34}){
            for(int side:new int[]{-1,1})for(int y=0;y<7;y++)ActivityBlocks.set(level,c.offset(side*13,y,z),Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),2);
            for(int x=-12;x<=12;x++)ActivityBlocks.set(level,c.offset(x,7,z),(x%6==0?Blocks.SEA_LANTERN:x%2==0?Blocks.ORANGE_STAINED_GLASS:Blocks.YELLOW_STAINED_GLASS).defaultBlockState(),2);
        }
        // Warm festoon strands and hanging paper lanterns above the central walk.
        for(int z=-28;z<=45;z+=6)for(int x=-5;x<=5;x++){
            ActivityBlocks.set(level,c.offset(x,6,z),(x%4==0?Blocks.SEA_LANTERN:x%2==0?Blocks.ORANGE_STAINED_GLASS:Blocks.PINK_STAINED_GLASS).defaultBlockState(),2);
            if(Math.abs(x)==4){ActivityBlocks.set(level,c.offset(x,5,z),Blocks.CHAIN.defaultBlockState(),2);
                ActivityBlocks.set(level,c.offset(x,4,z),Blocks.LANTERN.defaultBlockState(),2);}
        }
        for(int side:new int[]{-1,1})for(int z:new int[]{-38,-27,-16,0,39}){
            ActivityBlocks.set(level,c.offset(side*48,0,z),Blocks.HAY_BLOCK.defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(side*48,1,z),Blocks.POTTED_ORANGE_TULIP.defaultBlockState(),2);
            ActivityBlocks.set(level,c.offset(side*46,0,z+2),Blocks.CHERRY_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING,side<0?Direction.EAST:Direction.WEST),2);
        }
        // Every outer lawn becomes a series of accessible flower rooms, lit topiary,
        // little seating circles and bright paving. Keep the attraction footprints open.
        for(int x=-48;x<=48;x+=9)for(int z=-46;z<=45;z+=9){
            boolean occupied=Math.abs(x)<14||Math.abs(z-5)<8||
                (Math.abs(x+21)<8||Math.abs(x-21)<8)&&(Math.abs(z-27)<8||Math.abs(z-12)<8||Math.abs(z+3)<8)||
                Math.abs(x)<22&&Math.abs(z+40)<21||Math.abs(x+36)<12&&Math.abs(z+32)<12||
                Math.abs(x-36)<13&&Math.abs(z+32)<13||Math.abs(x)>=37&&Math.abs(x)<=47&&
                (Math.abs(z-24)<6||Math.abs(z-9)<6||Math.abs(z+7)<6);
            if(occupied)continue;
            var at=c.offset(x,0,z);if(!level.isEmptyBlock(at)||!level.getBlockState(at.below()).is(Blocks.GRASS_BLOCK))continue;
            int style=Math.floorMod(x/9+z/9,4);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                var spot=at.offset(dx,0,dz);if(!level.isEmptyBlock(spot)||!level.getBlockState(spot.below()).is(Blocks.GRASS_BLOCK))continue;
                if(Math.abs(dx)==2||Math.abs(dz)==2)ActivityBlocks.set(level,spot,ActivityWorld.persistentLeaves(Blocks.FLOWERING_AZALEA_LEAVES),2);
                else if(Math.abs(dx)+Math.abs(dz)>0)ActivityBlocks.set(level,spot,(Math.floorMod(dx*5+dz*7+style,3)==0?Blocks.ALLIUM:Math.floorMod(dx+dz,2)==0?Blocks.PINK_TULIP:Blocks.ORANGE_TULIP).defaultBlockState(),2);
            }
            if(style==0){ActivityBlocks.set(level,at,Blocks.SEA_LANTERN.defaultBlockState(),2);
                ActivityBlocks.set(level,at.above(),Blocks.PINK_STAINED_GLASS.defaultBlockState(),2);}
            else if(style==1){for(int y=0;y<3;y++)ActivityBlocks.set(level,at.above(y),Blocks.CHERRY_LOG.defaultBlockState(),2);
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)+Math.abs(dz)<=3)ActivityBlocks.set(level,at.offset(dx,3,dz),ActivityWorld.persistentLeaves(Blocks.CHERRY_LEAVES),2);}
            else{ActivityBlocks.set(level,at,Blocks.CHERRY_FENCE.defaultBlockState(),2);ActivityBlocks.set(level,at.above(),Blocks.LANTERN.defaultBlockState(),2);}
        }
        ActivityBlocks.set(level,c.below(),Blocks.ORANGE_GLAZED_TERRACOTTA.defaultBlockState(),2);
        ActivityBlocks.set(level,c.below(2),Blocks.PINK_GLAZED_TERRACOTTA.defaultBlockState(),2);
        ActivityBlocks.set(level,c.below(3),Blocks.LIME_GLAZED_TERRACOTTA.defaultBlockState(),2);
    }
    private static void post(ServerLevel level,BlockPos at,int height){
        for(int y=0;y<height;y++)ActivityBlocks.set(level,at.above(y),Blocks.DARK_OAK_FENCE.defaultBlockState(),2);
        ActivityBlocks.set(level,at.above(height),Blocks.SEA_LANTERN.defaultBlockState(),2);
    }
    private static void gateway(ServerLevel level,BlockPos c,int z){
        for(int side:new int[]{-1,1})for(int y=0;y<=6;y++)ActivityBlocks.set(level,c.offset(side*6,y,z),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),2);
        for(int x=-6;x<=6;x++)ActivityBlocks.set(level,c.offset(x,6,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        for(int x=-5;x<=5;x++)ActivityBlocks.set(level,c.offset(x,7,z),(x%3==0?Blocks.SEA_LANTERN:Blocks.ORANGE_WOOL).defaultBlockState(),2);
        ActivityBlocks.set(level,c.offset(0,5,z),Blocks.GLOWSTONE.defaultBlockState(),2);
    }
    private static void foodCart(ServerLevel level,BlockPos at,Booth food){
        Block canopy=food.stripeA,trim=food.stripeB;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            ActivityBlocks.set(level,at.offset(x,-1,z),(Math.floorMod(x+z,4)==0?Blocks.GOLD_BLOCK:Blocks.CHERRY_PLANKS).defaultBlockState(),2);
            ActivityBlocks.set(level,at.offset(x,4,z),(Math.floorMod(x,2)==0?canopy:trim).defaultBlockState(),2);
        }
        for(int x:new int[]{-2,2})for(int z:new int[]{-2,2})for(int y=0;y<4;y++)ActivityBlocks.set(level,at.offset(x,y,z),Blocks.BIRCH_FENCE.defaultBlockState(),2);
        for(int x:new int[]{-2,2})for(int z:new int[]{-1,1}){
            ActivityBlocks.set(level,at.offset(x,0,z),Blocks.BLACK_CONCRETE.defaultBlockState(),2);
            ActivityBlocks.set(level,at.offset(x,1,z),Blocks.GOLD_BLOCK.defaultBlockState(),2);
        }
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)ActivityBlocks.set(level,at.offset(x,1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        for(int x=-1;x<=1;x++)ActivityBlocks.set(level,at.offset(x,2,0),Blocks.WHITE_STAINED_GLASS.defaultBlockState(),2);
        for(int x=-2;x<=2;x++)ActivityBlocks.set(level,at.offset(x,0,2),x==0?Blocks.DISPENSER.defaultBlockState()
            .setValue(net.minecraft.world.level.block.DispenserBlock.FACING,Direction.SOUTH):Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        for(int x:new int[]{-1,1}){
            ActivityBlocks.set(level,at.offset(x,1,2),Blocks.AIR.defaultBlockState(),2);
            ActivityBlocks.set(level,at.offset(x,0,-1),(food==Booth.LEMONADE?Blocks.CYAN_STAINED_GLASS:food==Booth.CHOCOLATE?Blocks.BROWN_GLAZED_TERRACOTTA:canopy).defaultBlockState(),2);
        }
        ActivityBlocks.set(level,at.offset(0,0,-1),(switch(food){case POPCORN->Blocks.SMOKER;case CANDY_APPLES->Blocks.RED_GLAZED_TERRACOTTA;
            case COTTON_CANDY->Blocks.PINK_STAINED_GLASS;case PIE_CART->Blocks.CAKE;case LEMONADE->Blocks.HONEY_BLOCK;default->Blocks.BROWN_GLAZED_TERRACOTTA;}).defaultBlockState(),2);
        for(int x=-1;x<=1;x++)ActivityBlocks.set(level,at.offset(x,2,-2),(x==0?Blocks.SEA_LANTERN:canopy).defaultBlockState(),2);
        for(int x:new int[]{-2,2})ActivityBlocks.set(level,at.offset(x,3,0),Blocks.LANTERN.defaultBlockState(),2);
        ActivityBlocks.set(level,at.offset(0,5,0),Blocks.SEA_LANTERN.defaultBlockState(),2);
        ActivityBlocks.set(level,at.offset(0,2,2),Blocks.CHERRY_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.SOUTH),2);
        if(level.getBlockEntity(at.offset(0,2,2)) instanceof SignBlockEntity sign){
            var ink=food==Booth.LEMONADE?ChatFormatting.AQUA:food==Booth.CHOCOLATE?ChatFormatting.GOLD:ChatFormatting.LIGHT_PURPLE;
            sign.setText(ActivityBlocks.readable(sign,sign.getFrontText().setMessage(0,Component.literal(food.title).withStyle(ink,ChatFormatting.BOLD))
                .setMessage(1,Component.literal("✦ SELF-SERVE ✦").withStyle(ChatFormatting.WHITE))
                .setMessage(2,Component.literal("1 token • click below").withStyle(ChatFormatting.YELLOW)).setHasGlowingText(true)),true);
        }
    }
    private static void sign(ServerLevel level,BlockPos at,Booth booth){
        ActivityBlocks.set(level,at,Blocks.CHERRY_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.SOUTH),2);
        if(level.getBlockEntity(at) instanceof SignBlockEntity sign){
            var color=switch(booth.game){case RING->ChatFormatting.GOLD;case DARTS->ChatFormatting.RED;case DUCKS->ChatFormatting.AQUA;case STRIKER->ChatFormatting.DARK_RED;case MIRROR->ChatFormatting.LIGHT_PURPLE;default->ChatFormatting.BLUE;};
            var title=switch(booth.game){case RING->"GOLDEN RINGS";case DARTS->"POP! DARTS";case DUCKS->"DUCK SPLASH";case STRIKER->"BELL BASH";case MIRROR->"MIRROR QUEST";default->"FORTUNE WHEEL";};
            var text=sign.getFrontText().setMessage(0,Component.literal("✦ " +title+" ✦").withStyle(color,ChatFormatting.BOLD))
                .setMessage(1,Component.literal(cost(booth)+" token"+(cost(booth)==1?"":"s")+" to play").withStyle(ChatFormatting.YELLOW))
                .setMessage(2,Component.literal("★ ★ ★").withStyle(color)).setHasGlowingText(true);
            sign.setText(ActivityBlocks.readable(sign,text),true);
        }
    }
    private static void booth(ServerLevel level,BlockPos c,Booth booth){
        var b=c.offset(booth.x,0,booth.z);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)ActivityBlocks.set(level,b.offset(x,-1,z),Blocks.STRIPPED_CHERRY_WOOD.defaultBlockState(),2);
        for(int x:new int[]{-4,4})for(int z:new int[]{-3,3})for(int y=0;y<=4;y++)ActivityBlocks.set(level,b.offset(x,y,z),Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(),2);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            ActivityBlocks.set(level,b.offset(x,5,z),(Math.floorMod(x,2)==0?booth.stripeA:booth.stripeB).defaultBlockState(),2);
            if(Math.abs(x)<=2)ActivityBlocks.set(level,b.offset(x,6,z),(Math.floorMod(x,2)==0?booth.stripeB:booth.stripeA).defaultBlockState(),2);
        }
        for(int x=-1;x<=1;x++)for(int z=-2;z<=2;z++)ActivityBlocks.set(level,b.offset(x,7,z),(Math.floorMod(z,2)==0?booth.stripeA:booth.stripeB).defaultBlockState(),2);
        for(int z=-1;z<=1;z++)ActivityBlocks.set(level,b.offset(0,8,z),booth.stripeB.defaultBlockState(),2);
        ActivityBlocks.set(level,b.offset(0,9,0),Blocks.SEA_LANTERN.defaultBlockState(),2);
        for(int x:new int[]{-4,4}){
            ActivityBlocks.set(level,b.offset(x,3,4),(x<0?Blocks.RED_WALL_BANNER:Blocks.YELLOW_WALL_BANNER).defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING,Direction.SOUTH),2);
            ActivityBlocks.set(level,b.offset(x,4,4),Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),2);
        }
        for(int x=-4;x<=4;x++){ActivityBlocks.set(level,b.offset(x,0,-3),Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
            ActivityBlocks.set(level,b.offset(x,0,3),x==0?Blocks.LODESTONE.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);}
        // Solid, decorated target enclosure. The front stays readable, while
        // nobody can simply walk behind the targets for point-blank shots.
        for(int x=-4;x<=4;x++)for(int y=0;y<=3;y++)
            ActivityBlocks.set(level,b.offset(x,y,-4),y==3?Blocks.GOLD_BLOCK.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        for(int z=-3;z<=2;z++)for(int y=0;y<=3;y++)for(int side:new int[]{-1,1})
            ActivityBlocks.set(level,b.offset(side*4,y,z),y==2&&Math.floorMod(z,3)==0?Blocks.SEA_LANTERN.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        for(int x=-4;x<=4;x++)for(int y=0;y<=2;y++)if(y==0||Math.abs(x)>=3)
            ActivityBlocks.set(level,b.offset(x,y,3),y==1?Blocks.PINK_STAINED_GLASS.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        for(int x:new int[]{-3,3}){
            ActivityBlocks.set(level,b.offset(x,3,2),Blocks.SEA_LANTERN.defaultBlockState(),2);
            ActivityBlocks.set(level,b.offset(x,1,3),Blocks.POTTED_PINK_TULIP.defaultBlockState(),2);
        }
        ActivityBlocks.set(level,b.offset(0,4,3),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        sign(level,b.offset(0,4,4),booth);
        for(int side:new int[]{-1,1}){
            ActivityBlocks.set(level,b.offset(side*3,0,-1),Blocks.BARREL.defaultBlockState(),2);
            ActivityBlocks.set(level,b.offset(side*3,1,-1),(booth.difficulty>=4?Blocks.GOLD_BLOCK:Blocks.AMETHYST_BLOCK).defaultBlockState(),2);
            ActivityBlocks.set(level,b.offset(side*3,2,-1),Blocks.GLASS.defaultBlockState(),2);
        }
        if(booth==Booth.RING_TOSS||booth==Booth.DUCK_POND)for(int x=-3;x<=3;x++){
            int rise=8-Math.abs(x)/2;
            ActivityBlocks.set(level,b.offset(x,rise,4),(booth==Booth.RING_TOSS?Blocks.GOLD_BLOCK:Blocks.CYAN_STAINED_GLASS).defaultBlockState(),2);
            if(Math.abs(x)==3)ActivityBlocks.set(level,b.offset(x,rise+1,4),Blocks.SEA_LANTERN.defaultBlockState(),2);
        }
        if(booth==Booth.BALLOON_DARTS)for(int x:new int[]{-4,4})for(int y=6;y<=10;y++)
            ActivityBlocks.set(level,b.offset(x,y,0),(y>=8?(x<0?Blocks.RED_WOOL:Blocks.BLUE_WOOL):Blocks.CHAIN).defaultBlockState(),2);
        if(booth==Booth.MIRROR_MAZE)for(int x:new int[]{-4,4})for(int y=6;y<=10;y++)
            ActivityBlocks.set(level,b.offset(x,y,1),(y%2==0?Blocks.PURPLE_STAINED_GLASS:Blocks.PINK_STAINED_GLASS).defaultBlockState(),2);
        // Each counter has a distinct, legible prop silhouette even before interaction.
        switch(booth){
            case RING_TOSS->{for(int x:new int[]{-2,0,2}){
                int depth=x==0?-3:x<0?-2:-1;
                ActivityBlocks.set(level,b.offset(x,0,depth),(x==0?Blocks.GOLD_BLOCK:Blocks.RED_TERRACOTTA).defaultBlockState(),2);}}
            case BALLOON_DARTS->{for(int x=-3;x<=3;x++)for(int y=0;y<=3;y++)
                ActivityBlocks.set(level,b.offset(x,y,-3),(Math.floorMod(x+y,2)==0?Blocks.PURPLE_TERRACOTTA:Blocks.RED_TERRACOTTA).defaultBlockState(),2);
                for(int x=-3;x<=3;x++)ActivityBlocks.set(level,b.offset(x,4,-3),x%2==0?Blocks.SEA_LANTERN.defaultBlockState():Blocks.GOLD_BLOCK.defaultBlockState(),2);}
            case DUCK_POND->{for(int x=-3;x<=3;x++)for(int z=-2;z<=3;z++){
                    var pos=b.offset(x,0,z);ActivityBlocks.set(level,pos.below(),Blocks.PRISMARINE_BRICKS.defaultBlockState(),2);
                    ActivityBlocks.set(level,pos,Math.abs(x)==3||z==-2||z==3?Blocks.PRISMARINE_BRICKS.defaultBlockState():Blocks.WATER.defaultBlockState(),2);
                }}
            case HIGH_STRIKER->{for(int y=0;y<=8;y++){
                    ActivityBlocks.set(level,b.offset(5,y,2),y==8?Blocks.GOLD_BLOCK.defaultBlockState():Blocks.RED_CONCRETE.defaultBlockState(),2);
                    ActivityBlocks.set(level,b.offset(6,y,2),y==8?Blocks.SEA_LANTERN.defaultBlockState():y%2==0?Blocks.WHITE_CONCRETE.defaultBlockState():Blocks.GOLD_BLOCK.defaultBlockState(),2);
                }
                ActivityBlocks.set(level,b.offset(5,0,3),Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState(),2);
                ActivityBlocks.set(level,b.offset(5,9,2),Blocks.BELL.defaultBlockState(),2);}
            case MIRROR_MAZE->{for(int x=-3;x<=3;x+=2)for(int y=0;y<=3;y++)ActivityBlocks.set(level,b.offset(x,y,-2),(x%4==1?Blocks.PURPLE_STAINED_GLASS:Blocks.PINK_STAINED_GLASS).defaultBlockState(),2);}
            case LUCKY_WHEEL->{for(int x=-3;x<=3;x++)for(int y=0;y<=4;y++)
                ActivityBlocks.set(level,b.offset(x,y,-3),Blocks.PURPLE_CONCRETE.defaultBlockState(),2);}
            case POPCORN->{for(int x:new int[]{-2,2})ActivityBlocks.set(level,b.offset(x,0,-1),Blocks.HAY_BLOCK.defaultBlockState(),2);
                ActivityBlocks.set(level,b.offset(0,0,-1),Blocks.SMOKER.defaultBlockState(),2);}
            case CANDY_APPLES->{for(int x=-2;x<=2;x++)ActivityBlocks.set(level,b.offset(x,0,-1),(x%2==0?Blocks.RED_GLAZED_TERRACOTTA:Blocks.PINK_GLAZED_TERRACOTTA).defaultBlockState(),2);
                for(int x:new int[]{-2,0,2})ActivityBlocks.set(level,b.offset(x,1,-1),Blocks.CAKE.defaultBlockState(),2);}
            default->{ActivityBlocks.set(level,b.offset(0,0,-1),Blocks.GOLD_BLOCK.defaultBlockState(),2);}
        }
    }
    private static void ferrisWheel(ServerLevel level,BlockPos c){
        var hub=c.offset(0,17,-40);
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)if(x*x+y*y<=4)ActivityBlocks.set(level,hub.offset(x,y,1),Blocks.SEA_LANTERN.defaultBlockState(),2);
        for(int side:new int[]{-1,1})for(int y=0;y<17;y++){
            int x=side*(8-(int)Math.round(y*7.0/17));
            ActivityBlocks.set(level,c.offset(x,y,-36),Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),2);
        }
        for(int x=-8;x<=8;x++)ActivityBlocks.set(level,c.offset(x,0,-37),Blocks.GOLD_BLOCK.defaultBlockState(),2);
    }
    private static void carousel(ServerLevel level,BlockPos at){
        for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)if(x*x+z*z<=81){
            ActivityBlocks.set(level,at.offset(x,-1,z),(Math.floorMod(x+z,2)==0?Blocks.CHERRY_PLANKS:Blocks.GOLD_BLOCK).defaultBlockState(),2);
            int rise=10-(int)Math.round(Math.sqrt(x*x+z*z)*.55);
            if(rise>=5)ActivityBlocks.set(level,at.offset(x,rise,z),(Math.floorMod(x+z,3)==0?Blocks.YELLOW_WOOL:Blocks.RED_WOOL).defaultBlockState(),2);
        }
        for(int y=0;y<=10;y++)ActivityBlocks.set(level,at.above(y),Blocks.GOLD_BLOCK.defaultBlockState(),2);
        ActivityBlocks.set(level,at.above(11),Blocks.SEA_LANTERN.defaultBlockState(),2);
        for(int a=0;a<360;a+=45){double angle=Math.toRadians(a);int x=(int)Math.round(Math.cos(angle)*6),z=(int)Math.round(Math.sin(angle)*6);
            ActivityBlocks.set(level,at.offset(x,8,z),Blocks.SEA_LANTERN.defaultBlockState(),2);
        }
    }
    private static void mirrorGarden(ServerLevel level,BlockPos at,long layoutSeed){
        final int cells=12,n=25;boolean[][] open=new boolean[n][n],seen=new boolean[cells][cells];
        int[] xs=new int[cells*cells],zs=new int[cells*cells];int top=0;xs[0]=5;zs[0]=11;seen[11][5]=true;open[23][11]=true;
        var rng=RandomSource.create(layoutSeed^at.asLong()^0xCAFE5EEDL);
        while(top>=0){int x=xs[top],z=zs[top],count=0;int[] dirs=new int[4];
            if(x>0&&!seen[z][x-1])dirs[count++]=0;if(x<cells-1&&!seen[z][x+1])dirs[count++]=1;
            if(z>0&&!seen[z-1][x])dirs[count++]=2;if(z<cells-1&&!seen[z+1][x])dirs[count++]=3;
            if(count==0){top--;continue;}int d=dirs[rng.nextInt(count)],nx=x+(d==0?-1:d==1?1:0),nz=z+(d==2?-1:d==3?1:0);
            open[z*2+1+nz-z][x*2+1+nx-x]=true;open[nz*2+1][nx*2+1]=true;seen[nz][nx]=true;xs[++top]=nx;zs[top]=nz;
        }
        // A few deliberate cycles make this a maze with alternate routes,
        // not merely one winding corridor with dead-end branches.
        int loops=0;
        for(int tries=0;tries<180&&loops<8;tries++){
            int x=1+rng.nextInt(n-2),z=1+rng.nextInt(n-2);
            if(open[z][x]||((x&1)==(z&1)))continue;
            if((x&1)==0&&open[z][x-1]&&open[z][x+1]||(z&1)==0&&open[z-1][x]&&open[z+1][x]){
                open[z][x]=true;loops++;
            }
        }
        open[24][11]=true;open[0][11]=true;
        for(int gx=0;gx<n;gx++)for(int gz=0;gz<n;gz++){
            var pos=at.offset(gx-12,0,gz-12);ActivityBlocks.set(level,pos.below(),(Math.floorMod(gx+gz,11)==0?Blocks.SEA_LANTERN:Blocks.AMETHYST_BLOCK).defaultBlockState(),2);
            if(!open[gz][gx])for(int y=0;y<4;y++)ActivityBlocks.set(level,pos.above(y),(Math.floorMod(gx*7+gz*3,3)==0?Blocks.LIGHT_BLUE_STAINED_GLASS:Blocks.PURPLE_STAINED_GLASS).defaultBlockState(),2);
            else for(int y=0;y<4;y++)ActivityBlocks.set(level,pos.above(y),Blocks.AIR.defaultBlockState(),2);
            ActivityBlocks.set(level,pos.above(4),(Math.floorMod(gx*13+gz*7,17)==0?Blocks.SEA_LANTERN:Blocks.PURPLE_CONCRETE).defaultBlockState(),2);
        }
        ActivityBlocks.set(level,at.offset(-1,-1,-13),Blocks.GOLD_BLOCK.defaultBlockState(),2);
        ActivityBlocks.set(level,at.offset(-1,0,-13),Blocks.SEA_LANTERN.defaultBlockState(),2);
        ActivityBlocks.set(level,at.offset(-1,4,-12),Blocks.AIR.defaultBlockState(),2);
        ActivityBlocks.set(level,at.offset(-1,4,12),Blocks.AIR.defaultBlockState(),2);
        for(int side:new int[]{-1,1})for(int y=0;y<5;y++)ActivityBlocks.set(level,at.offset(-1+side*2,y,-13),Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),2);
    }
    static void removeBuild(ServerLevel level,BlockPos c){
        if(!level.getBlockState(c.below()).is(Blocks.ORANGE_GLAZED_TERRACOTTA)&&!level.getBlockState(c.below(2)).is(Blocks.PINK_GLAZED_TERRACOTTA))return;
        for(int x=-56;x<56;x++)for(int z=-56;z<56;z++)for(int y=-3;y<=10;y++){
            var at=c.offset(x,y,z);if(!level.isEmptyBlock(at))ActivityBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
        }
        for(int x=-20;x<=20;x++)for(int z=-45;z<=-35;z++)for(int y=9;y<=38;y++){
            var at=c.offset(x,y,z);if(!level.isEmptyBlock(at))ActivityBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
        }
        for(int x=26;x<=46;x++)for(int z=-42;z<=-22;z++)for(int y=9;y<=13;y++){
            var at=c.offset(x,y,z);if(!level.isEmptyBlock(at))ActivityBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
        }
    }
    static void clean(ServerLevel level,VisitData.Scene s,BlockPos c){
        var remove=new ArrayList<Entity>();
        for(var e:level.getEntitiesOfClass(Entity.class,new AABB(c).inflate(58,40,58),e->e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))remove.add(e);
        remove.forEach(Entity::discard);
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        var c=center(p,s);build(level,c);clean(level,s,c);removeTokens(p);
        var l=s.loop;l.carnivalTokensRemaining=TOKEN_COUNT;l.carnivalIssued=false;l.carnivalBooth=-1;
        l.carnivalRide=-1;l.carnivalRideUntil=0;
        l.carnivalProgress=0;l.carnivalFailures=0;l.carnivalSeed=p.getRandom().nextLong();l.carnivalGameUntil=0;l.carnivalRoundAt=0;l.carnivalLastActionAt=0;
        l.carnivalGiftAt=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.CARNIVAL));
        ensureFather(p,s,level,c);ensureCrowd(p,s,level,c);ensureAttendants(p,s,level,c);ensureRideSeats(p,s,level,c);ensureGameProps(level,s,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        LoopProps.hold(level,c);LoopProps.hold(level,c.north(40));build(level,c);
        level.setWeatherParameters(6000,0,false,false);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,0));
        ensureFather(p,s,level,c);ensureCrowd(p,s,level,c);ensureAttendants(p,s,level,c);ensureRideSeats(p,s,level,c);ensureGameProps(level,s,c);
        ActivityWorld.ensureExit(p,s);
    }
    private static void ensureGameProps(ServerLevel level,VisitData.Scene s,BlockPos c){
        var l=s.loop;
        var ring=c.offset(Booth.RING_TOSS.x,0,Booth.RING_TOSS.z);
        for(int slot=0;slot<3;slot++){
            final int id=slot;
            if(!level.getEntitiesOfClass(ScenePropEntity.class,new AABB(ring).inflate(5,4,5),
                e->e.kind()==ScenePropEntity.CARNIVAL_PEG&&e.slot==id&&s.owner.equals(e.owner)).isEmpty())continue;
            int x=slot*2-2,depth=slot==1?-3:slot==0?-2:-1;
            LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_PEG,
                new Vec3(ring.getX()+x+.5,ring.getY()+1,ring.getZ()+depth+.5),slot);
        }
        var b=c.offset(Booth.BALLOON_DARTS.x,0,Booth.BALLOON_DARTS.z);
        for(var old:level.getEntitiesOfClass(ScenePropEntity.class,new AABB(b).inflate(5,6,5),e->e.kind()==ScenePropEntity.CARNIVAL_BALLOON&&s.owner.equals(e.owner)&&e.slot>=3))old.discard();
        if(l.carnivalBooth!=Booth.BALLOON_DARTS.ordinal())for(int slot=0;slot<3;slot++){
            final int id=slot;
            if(!level.getEntitiesOfClass(ScenePropEntity.class,new AABB(b).inflate(5,6,5),
                e->e.kind()==ScenePropEntity.CARNIVAL_BALLOON&&e.slot==id&&s.owner.equals(e.owner)).isEmpty())continue;
            int x=slot-1,row=1;
            LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_BALLOON,
                new Vec3(b.getX()+.5+x*.85,b.getY()+.45+row*1.06,b.getZ()-1.62),slot);
        }
        b=c.offset(Booth.DUCK_POND.x,0,Booth.DUCK_POND.z);
        if(l.carnivalBooth!=Booth.DUCK_POND.ordinal())for(int slot=0;slot<8;slot++){
            final int id=slot;
            if(!level.getEntitiesOfClass(ScenePropEntity.class,new AABB(b).inflate(5,4,5),
                e->e.kind()==ScenePropEntity.CARNIVAL_DUCK&&e.slot==id&&s.owner.equals(e.owner)).isEmpty())continue;
            LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_DUCK,
                duckPosition(b,slot,0),slot);
        }
        b=c.offset(Booth.LUCKY_WHEEL.x,0,Booth.LUCKY_WHEEL.z);
        if(level.getEntitiesOfClass(ScenePropEntity.class,new AABB(b).inflate(5,6,5),
            e->e.kind()==ScenePropEntity.CARNIVAL_WHEEL&&s.owner.equals(e.owner)).isEmpty())
            LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_WHEEL,
                new Vec3(b.getX()+.5,b.getY(),b.getZ()+1.2),0);
        b=c.offset(Booth.HIGH_STRIKER.x,0,Booth.HIGH_STRIKER.z);
        if(level.getEntitiesOfClass(ScenePropEntity.class,new AABB(b).inflate(8,10,8),
            e->e.kind()==ScenePropEntity.CARNIVAL_STRIKER_METER&&s.owner.equals(e.owner)).isEmpty())
            LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_STRIKER_METER,
                new Vec3(b.getX()+6.5,b.getY(),b.getZ()+3.4),0);
        for(var food:new Booth[]{Booth.POPCORN,Booth.CANDY_APPLES,Booth.COTTON_CANDY,Booth.PIE_CART,Booth.LEMONADE,Booth.CHOCOLATE}){
            final int id=food.ordinal();var cart=c.offset(food.x,0,food.z);
            var existingFood=level.getEntitiesOfClass(ScenePropEntity.class,new AABB(cart).inflate(4,5,4),
                e->e.kind()==ScenePropEntity.CARNIVAL_FOOD&&e.slot==id&&s.owner.equals(e.owner));
            if(!existingFood.isEmpty()){existingFood.get(0).food(0,cartFood(food));continue;}
            var display=LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_FOOD,
                new Vec3(cart.getX()+1.3,cart.getY()+1.05,cart.getZ()+2.2),id);
            display.food(0,cartFood(food));
        }
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        if(!level.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(46)).inflate(8),e->e.getPersistentData().getBoolean("CoralineCarnivalFather")&&e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())return;
        var realm=RealmData.get(p.server).realms.get(s.door);var family=ParentAccess.family(p.server,s.owner);
        var father=ModEntities.DAD.get().create(level);if(father==null)return;
        father.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));father.setNoAi(true);
        father.getPersistentData().putBoolean("CoralineCarnivalFather",true);father.getPersistentData().putUUID(OWNER,s.owner);
        father.getPersistentData().putUUID("RealmDoor",s.door);father.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        father.setPos(c.getX()+3.5,c.getY(),c.getZ()+46.5);father.setYRot(90);father.setYBodyRot(90);level.addFreshEntity(father);
    }
    public static boolean isCarnivalNpc(Entity e){return e.getPersistentData().getBoolean(NPC)||e.getPersistentData().getBoolean(ATTENDANT);}
    private static void ensureAttendants(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        String[] names={"Golden Rings","Balloon Pop","Duck Splash","Bell Bash","Mirror Keeper","Fortune Spinner","Popcorn Cart","Candy Apple Cart","Ferris Wheel","Carousel"};
        // Retire previously saved food vendors. Snacks are enchanted self-serve
        // carts; human attendants remain only at games and rides.
        for(var old:level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(55,12,55),
            e->e.getPersistentData().getBoolean(ATTENDANT)&&e.getPersistentData().getInt("AttendantSlot")>=6&&
                e.getPersistentData().getInt("AttendantSlot")<=7&&s.owner.equals(e.getPersistentData().getUUID(OWNER))))old.discard();
        for(int slot=0;slot<names.length;slot++){
            if(slot==6||slot==7)continue;
            final int id=slot;
            if(!level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(55,12,55),e->e.getPersistentData().getBoolean(ATTENDANT)&&e.getPersistentData().getInt("AttendantSlot")==id&&e.getPersistentData().getInt(GEN)==s.loop.generation&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())continue;
            var n=ModEntities.FISHING_CONTEST_NPC.get().create(level);if(n==null)continue;
            n.configure(slot*7+3,slot%2==0,true,slot);n.setNoAi(true);n.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            BlockPos at=slot<6?c.offset(Booth.values()[slot].x,0,Booth.values()[slot].z+1):slot==6?c.offset(-42,0,23):slot==7?c.offset(42,0,23):slot==8?c.offset(0,0,-37):c.offset(36,0,-23);
            n.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);n.setYRot(0);n.setYBodyRot(0);n.yHeadRot=0;
            n.setCustomName(Component.literal(names[slot]+" • "+cost(Booth.values()[slot])+" token"+(cost(Booth.values()[slot])==1?"":"s")));
            n.setCustomNameVisible(true);
            n.getPersistentData().putBoolean("CoralineDecoration",true);n.getPersistentData().putBoolean(ATTENDANT,true);n.getPersistentData().putUUID(OWNER,s.owner);
            n.getPersistentData().putInt(GEN,s.loop.generation);n.getPersistentData().putInt("AttendantSlot",slot);
            level.addFreshEntity(n);
        }
    }
    private static void ensureRideSeats(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        for(var old:level.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(55,40,55),
            e->e.kind()==ScenePropEntity.CHAIR&&(e.slot==70||e.slot==71)&&s.owner.equals(e.owner)))old.discard();
        for(int slot=70;slot<=78;slot++){
            final int id=slot,kind=slot==70?ScenePropEntity.CARNIVAL_GONDOLA:ScenePropEntity.CARNIVAL_MOUNT;
            if(!level.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(55,40,55),e->e.kind()==kind&&e.slot==id&&s.owner.equals(e.owner)).isEmpty())continue;
            var at=slot==70?c.offset(0,1,-37):c.offset(42,0,-32);
            var seat=LoopProps.spawn(level,s,kind,Vec3.atBottomCenterOf(at),slot);
            seat.getPersistentData().putUUID(OWNER,s.owner);seat.getPersistentData().putInt("CarnivalRideSeat",slot==70?0:1);
        }
        for(int slot=80;slot<=86;slot++){
            final int id=slot;
            if(!level.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(55,40,55),
                e->e.kind()==ScenePropEntity.CARNIVAL_GONDOLA&&e.slot==id&&s.owner.equals(e.owner)).isEmpty())continue;
            var seat=LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_GONDOLA,
                new Vec3(c.getX()+.5,c.getY()+17,c.getZ()-37.5),slot);
            seat.getPersistentData().putUUID(OWNER,s.owner);
        }
        if(level.getEntitiesOfClass(ScenePropEntity.class,new AABB(c.offset(0,17,-40)).inflate(18),
            e->e.kind()==ScenePropEntity.CARNIVAL_FERRIS_WHEEL&&s.owner.equals(e.owner)).isEmpty())
            LoopProps.spawn(level,s,ScenePropEntity.CARNIVAL_FERRIS_WHEEL,
                new Vec3(c.getX()+.5,c.getY()+17,c.getZ()-39.5),79);
    }
    /** Reserve booth interiors, counters and the player's approach/throwing lanes. */
    static boolean crowdAllowed(double x,double z){
        if(Math.abs(x)>50||Math.abs(z)>50)return false;
        for(var booth:Booth.values())if(booth.ordinal()<6&&
            x>booth.x-7&&x<booth.x+8&&z>booth.z-5&&z<booth.z+10)return false;
        return true;
    }
    static boolean crowdSegmentAllowed(Vec3 from,Vec3 to,BlockPos c){
        int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)*4));
        for(int i=0;i<=steps;i++){
            var at=from.lerp(to,i/(double)steps);
            if(!crowdAllowed(at.x-c.getX(),at.z-c.getZ()))return false;
        }
        return true;
    }
    private static boolean crowdWalkable(ServerLevel level,Villager v,Vec3 at,BlockPos c){
        var below=BlockPos.containing(at).below();
        return crowdAllowed(at.x-c.getX(),at.z-c.getZ())&&
            level.getBlockState(below).isFaceSturdy(level,below,Direction.UP)&&
            !level.getBlockCollisions(v,v.getBoundingBox().move(at.subtract(v.position()))).iterator().hasNext();
    }
    private static void moveCrowd(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var level=p.serverLevel();
        for(var v:level.getEntitiesOfClass(Villager.class,new AABB(c).inflate(55,12,55),e->
            e.getPersistentData().getBoolean(NPC)&&e.getPersistentData().getInt(GEN)==s.loop.generation&&
                s.owner.equals(e.getPersistentData().getUUID(OWNER)))){
            v.setNoAi(true);v.getNavigation().stop();v.setDeltaMovement(Vec3.ZERO);
            // Repair already-saved guests in a booth as well as preventing new intrusions.
            if(!crowdAllowed(v.getX()-c.getX(),v.getZ()-c.getZ())){
                boolean moved=false;
                for(int radius=1;radius<=22&&!moved;radius++)for(int side=0;side<32&&!moved;side++){
                    double a=side*Math.PI/16;
                    var at=new Vec3(v.getX()+Math.cos(a)*radius,c.getY(),v.getZ()+Math.sin(a)*radius);
                    if(crowdWalkable(level,v,at,c)){v.setPos(at);moved=true;}
                }
                v.getPersistentData().remove("CarnivalWalkUntil");continue;
            }
            var n=v.getPersistentData();long now=level.getGameTime();
            if(now>=n.getLong("CarnivalWalkUntil")){
                n.putLong("CarnivalWalkUntil",now+40+v.getRandom().nextInt(80));
                double angle=v.getRandom().nextDouble()*Math.PI*2;
                n.putDouble("CarnivalWalkX",Math.cos(angle)*.09);n.putDouble("CarnivalWalkZ",Math.sin(angle)*.09);
            }
            var next=v.position().add(n.getDouble("CarnivalWalkX"),0,n.getDouble("CarnivalWalkZ"));
            if(!crowdSegmentAllowed(v.position(),next,c)||!crowdWalkable(level,v,next,c)){
                n.putLong("CarnivalWalkUntil",0);continue;
            }
            v.setPos(next);
            float yaw=(float)Math.toDegrees(Math.atan2(-n.getDouble("CarnivalWalkX"),n.getDouble("CarnivalWalkZ")));
            v.setYRot(yaw);v.setYBodyRot(yaw);v.setYHeadRot(yaw);
        }
    }
    private static void ensureCrowd(ServerPlayer p,VisitData.Scene s,ServerLevel level,BlockPos c){
        int found=level.getEntitiesOfClass(Villager.class,new AABB(c).inflate(54,9,54),e->isCarnivalNpc(e)&&e.getPersistentData().getInt(GEN)==s.loop.generation&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).size();
        if(found>=CROWD_COUNT)return;
        var random=RandomSource.create(s.loop.carnivalSeed^0xCA2A1FA1L);
        for(int slot=0;slot<CROWD_COUNT;slot++){
            final int crowdSlot=slot;
            if(!level.getEntitiesOfClass(Villager.class,new AABB(c).inflate(54,9,54),e->isCarnivalNpc(e)&&e.getPersistentData().getInt("CarnivalSlot")==crowdSlot&&e.getPersistentData().getInt(GEN)==s.loop.generation&&s.owner.equals(e.getPersistentData().getUUID(OWNER))).isEmpty())continue;
            BlockPos at=null;
            for(int attempt=0;attempt<24;attempt++){
                int x=random.nextInt(101)-50,z=random.nextInt(101)-50;
                var candidate=c.offset(x,0,z);
                if(crowdAllowed(x+.5,z+.5)&&level.isEmptyBlock(candidate)&&level.isEmptyBlock(candidate.above())&&level.getBlockState(candidate.below()).isFaceSturdy(level,candidate.below(),Direction.UP)){at=candidate;break;}
            }
            if(at==null)continue;
            var v=EntityType.VILLAGER.create(level);if(v==null)continue;
            v.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);v.setPersistenceRequired();v.setNoAi(true);
            v.setCustomName(Component.literal(VillageActivity.RESIDENT_NAME));v.setCustomNameVisible(false);
            v.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(.5);
            var n=v.getPersistentData();n.putBoolean("CoralineDecoration",true);n.putBoolean(NPC,true);n.putUUID(OWNER,s.owner);n.putInt(GEN,s.loop.generation);n.putInt("CarnivalSlot",slot);
            level.addFreshEntity(v);
        }
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var l=s.loop;long now=OtherMotherLoop.now(p);
        if(p.tickCount%20==0)prepare(p,s,p.serverLevel(),c);
        if(p.getY()<78||Math.abs(p.getX()-(c.getX()+.5))>53||Math.abs(p.getZ()-(c.getZ()+.5))>53){
            p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+46.5,180,0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        }
        if(!l.carnivalIssued&&now>=l.carnivalGiftAt)grantTokens(p,s);
        if(p.tickCount%2==0){moveAttendants(p,s,c);moveCrowd(p,s,c);}
        moveRideSeats(p,s,c,now);
        if(l.carnivalRide>=0&&(now>=l.carnivalRideUntil||!p.isPassenger())){
            p.stopRiding();int ride=l.carnivalRide;l.carnivalRide=-1;l.carnivalRideUntil=0;OtherMotherLoop.changed(p);
            if(ride==0)for(var wheel:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c.offset(0,17,-40)).inflate(18),
                e->e.kind()==ScenePropEntity.CARNIVAL_FERRIS_WHEEL&&s.owner.equals(e.owner)))wheel.mode(0);
            var at=ride==0?c.offset(0,0,-35):c.offset(36,0,-22);p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,180,0);
            var souvenir=new ItemStack(ride==0?Items.AMETHYST_SHARD:Items.GLOW_BERRIES,ride==0?2:4);
            p.getInventory().add(souvenir);if(!souvenir.isEmpty())p.drop(souvenir,false);
            p.displayClientMessage(Component.literal("The ride ends with a small souvenir. "+l.carnivalTokensRemaining+" tokens left."),true);
            if(exitReady(s))ActivityWorld.ensureExit(p,s);
        }
        if(l.carnivalBooth>=0){
            var booth=Booth.values()[l.carnivalBooth];
            if(booth==Booth.RING_TOSS&&p.tickCount%10==0)
                for(var peg:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c.offset(booth.x,0,booth.z)).inflate(5,4,5),e->e.kind()==ScenePropEntity.CARNIVAL_PEG&&s.owner.equals(e.owner)))
                    p.serverLevel().sendParticles(p,net.minecraft.core.particles.ParticleTypes.END_ROD,true,peg.getX(),peg.getY()+.85,peg.getZ(),3,.09,.04,.09,0);
            if(booth==Booth.DUCK_POND&&p.tickCount%4==0){
                int target=Math.floorMod((int)((now-l.carnivalRoundAt)/30),8);
                for(var duck:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c.offset(booth.x,0,booth.z)).inflate(5,3,5),e->e.kind()==ScenePropEntity.CARNIVAL_DUCK&&s.owner.equals(e.owner)&&e.slot==target))
                    for(int point=0;point<6;point++){
                        double angle=point*Math.PI/3+now*.12;
                        p.serverLevel().sendParticles(p,new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1F,.8F,.05F),.65F),true,
                            duck.getX()+Math.cos(angle)*.23,duck.getY()+.8,duck.getZ()+Math.sin(angle)*.23,1,0,0,0,0);
                    }
            }
            if(booth==Booth.BALLOON_DARTS||booth==Booth.DUCK_POND)animateTargets(p.serverLevel(),c,booth,now);
            if(booth==Booth.MIRROR_MAZE&&p.getZ()<c.getZ()-44&&Math.abs(p.getX()-(c.getX()-37.5))<2){finish(p,s,true);}
            else if(booth==Booth.LUCKY_WHEEL&&l.carnivalProgress==1&&now-l.carnivalRoundAt>=65){
                for(var wheel:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c.offset(booth.x,0,booth.z)).inflate(5,6,5),
                    e->e.kind()==ScenePropEntity.CARNIVAL_WHEEL&&s.owner.equals(e.owner)))wheel.mode(2);
                finish(p,s,true);
            }
            else if(now>=l.carnivalGameUntil)finish(p,s,false);
            else if((booth==Booth.RING_TOSS||booth==Booth.BALLOON_DARTS)&&!hasIssuedAmmo(p,s,booth)&&now-l.carnivalLastActionAt>40)finish(p,s,l.carnivalProgress>=booth.steps);
            else if(p.tickCount%10==0){
                int seconds=(int)Math.max(0,(l.carnivalGameUntil-now+19)/20);
                String status=booth==Booth.HIGH_STRIKER?"Hits "+l.carnivalTarget+"/3 • "+l.carnivalProgress+" points":
                    booth==Booth.DUCK_POND?"Catch the sparkling duck • "+l.carnivalProgress+"/6":
                    booth==Booth.LUCKY_WHEEL?(l.carnivalProgress==0?"Right-click the wheel":"Spinning..."):
                    booth==Booth.MIRROR_MAZE?"Find the exit":booth==Booth.RING_TOSS?"Aim at sparkling bottle tops • "+l.carnivalProgress+"/4":l.carnivalProgress+"/"+booth.steps+" balloons";
                p.displayClientMessage(Component.literal(seconds+"s  •  "+status).withStyle(ChatFormatting.GOLD),true);
            }
        }
        if(l.carnivalIssued&&l.carnivalBooth<0&&p.tickCount%80==0&&l.carnivalTokensRemaining>0)
            p.displayClientMessage(Component.literal("✦ "+l.carnivalTokensRemaining+" tokens").withStyle(ChatFormatting.GOLD),true);
        if(l.carnivalBooth>=0){
            var booth=Booth.values()[l.carnivalBooth];
            if(booth==Booth.RING_TOSS||booth==Booth.BALLOON_DARTS||booth==Booth.DUCK_POND){
                var base=c.offset(booth.x,0,booth.z);
                if(p.getZ()<base.getZ()+3.35||Math.abs(p.getX()-(base.getX()+.5))>4.2){
                    p.teleportTo(p.serverLevel(),base.getX()+.5,base.getY(),base.getZ()+5.5,p.getYRot(),p.getXRot());
                    p.setDeltaMovement(Vec3.ZERO);
                }
            }
        }
        if(p.tickCount%20==0){
            for(var food:new Booth[]{Booth.POPCORN,Booth.CANDY_APPLES,Booth.COTTON_CANDY,Booth.PIE_CART,Booth.LEMONADE,Booth.CHOCOLATE}){
                var at=c.offset(food.x,1,food.z+2);
                p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,at.getX()+.5,at.getY()+.4,at.getZ()+.5,3,.18,.18,.18,.01);
            }
        }
    }
    private static void moveRideSeats(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        for(var seat:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(55,40,55),e->
            (e.kind()==ScenePropEntity.CARNIVAL_GONDOLA&&(e.slot==70||e.slot>=80&&e.slot<=86)||
             e.kind()==ScenePropEntity.CARNIVAL_MOUNT&&e.slot>=71&&e.slot<=78)&&s.owner.equals(e.owner))){
            if(seat.kind()==ScenePropEntity.CARNIVAL_GONDOLA){
                int index=seat.slot==70?0:seat.slot-79;
                double a=(s.loop.carnivalRide==0?(now-(s.loop.carnivalRideUntil-600))*.018:0)-Math.PI/2+index*Math.PI/4;
                seat.setPos(c.getX()+.5+Math.cos(a)*16,c.getY()+17+Math.sin(a)*16,c.getZ()-37.5);seat.setYRot(180);
            }else{
                double a=Math.PI/2+(s.loop.carnivalRide==1?(now-(s.loop.carnivalRideUntil-600))*.045:0)+(seat.slot-71)*Math.PI/4;
                seat.setPos(c.getX()+36.5+Math.cos(a)*5.6,c.getY()+.1+Math.sin(now*.11+seat.slot)*.19,c.getZ()-32.5+Math.sin(a)*5.6);
                seat.setYRot((float)Math.toDegrees(a)+90);
            }
        }
    }
    private static void moveAttendants(ServerPlayer p,VisitData.Scene s,BlockPos c){
        for(var n:p.serverLevel().getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(55,12,55),
            e->e.getPersistentData().getBoolean(ATTENDANT)&&s.owner.equals(e.getPersistentData().getUUID(OWNER)))){
            int slot=n.getPersistentData().getInt("AttendantSlot");if(slot<0||slot>=6)continue;
            var booth=Booth.values()[slot];var b=c.offset(booth.x,0,booth.z);
            boolean watching=s.loop.carnivalBooth==slot;
            var goal=new Vec3(b.getX()+(slot==5?3.5:watching?3.5:.5),b.getY(),b.getZ()+1.5);
            var next=n.position().lerp(goal,.16);
            if(n.position().distanceToSqr(goal)<.005)next=goal;
            n.setPos(next.x,next.y,next.z);
            n.setYRot(0);n.setYBodyRot(0);n.yHeadRot=0;
        }
    }
    private static boolean tokenMatches(ItemStack stack,ServerPlayer p,VisitData.Scene s){
        return stack.is(ModItems.VILLAGE_TOKEN.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER)&&p.getUUID().equals(stack.getTag().getUUID(OWNER))&&stack.getTag().getInt(GEN)==s.loop.generation;
    }
    public static void removeTokens(ServerPlayer p){
        for(var stack:p.getInventory().items)if(stack.is(ModItems.VILLAGE_TOKEN.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER))stack.setCount(0);
        for(var stack:p.getInventory().offhand)if(stack.is(ModItems.VILLAGE_TOKEN.get())&&stack.hasTag()&&stack.getTag().hasUUID(OWNER))stack.setCount(0);
        removeAmmo(p);
        p.getInventory().setChanged();
    }
    private static void removeAmmo(ServerPlayer p){
        for(var stack:p.getInventory().items)if((stack.is(ModItems.CARNIVAL_RING.get())||stack.is(ModItems.CARNIVAL_DART.get())||stack.is(ModItems.CARNIVAL_MALLET.get()))&&stack.hasTag()&&stack.getTag().hasUUID(OWNER))stack.setCount(0);
        for(var stack:p.getInventory().offhand)if((stack.is(ModItems.CARNIVAL_RING.get())||stack.is(ModItems.CARNIVAL_DART.get())||stack.is(ModItems.CARNIVAL_MALLET.get()))&&stack.hasTag()&&stack.getTag().hasUUID(OWNER))stack.setCount(0);
        p.getInventory().setChanged();
    }
    private static boolean hasIssuedAmmo(ServerPlayer p,VisitData.Scene s,Booth b){
        for(var stack:p.getInventory().items)if(ammoMatches(stack,p,s,b))return true;
        for(var stack:p.getInventory().offhand)if(ammoMatches(stack,p,s,b))return true;
        return false;
    }
    private static boolean ammoMatches(ItemStack stack,ServerPlayer p,VisitData.Scene s,Booth b){
        return (stack.is(ModItems.CARNIVAL_RING.get())||stack.is(ModItems.CARNIVAL_DART.get()))&&stack.hasTag()&&stack.getTag().hasUUID(OWNER)&&
            p.getUUID().equals(stack.getTag().getUUID(OWNER))&&stack.getTag().getInt(GEN)==s.loop.generation&&stack.getTag().getInt("Booth")==b.ordinal();
    }
    private static void grantTokens(ServerPlayer p,VisitData.Scene s){
        if(s.loop.carnivalIssued)return;
        removeTokens(p);var stack=new ItemStack(ModItems.VILLAGE_TOKEN.get(),TOKEN_COUNT);
        stack.getOrCreateTag().putUUID(OWNER,p.getUUID());stack.getTag().putInt(GEN,s.loop.generation);
        ActivityTokens.give(p,stack);
        s.loop.carnivalIssued=true;OtherMotherLoop.changed(p);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.VILLAGER_CELEBRATE,SoundSource.NEUTRAL,1F,1F);
        title(p,"15 CARNIVAL TOKENS","Spend them however you like!");
    }
    private static boolean spendTokens(ServerPlayer p,VisitData.Scene s,int cost){
        int held=0;for(var stack:p.getInventory().items)if(tokenMatches(stack,p,s))held+=stack.getCount();
        for(var stack:p.getInventory().offhand)if(tokenMatches(stack,p,s))held+=stack.getCount();
        if(held<cost)return false;
        for(var stack:p.getInventory().items)if(tokenMatches(stack,p,s)){int n=Math.min(cost,stack.getCount());stack.shrink(n);cost-=n;if(cost==0)break;}
        if(cost>0)for(var stack:p.getInventory().offhand)if(tokenMatches(stack,p,s)){int n=Math.min(cost,stack.getCount());stack.shrink(n);cost-=n;if(cost==0)break;}
        p.getInventory().setChanged();return true;
    }
    public static boolean interactBlock(ServerPlayer p,BlockPos at){
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.CARNIVAL||!ActivityWorld.isActivity(p.level()))return false;
        var l=s.loop;var c=center(p,s);
        for(var food:new Booth[]{Booth.POPCORN,Booth.CANDY_APPLES,Booth.COTTON_CANDY,Booth.PIE_CART,Booth.LEMONADE,Booth.CHOCOLATE})
            if(at.equals(c.offset(food.x,0,food.z+2))){startGame(p,s,food);return true;}
        if(l.carnivalBooth<0)return false;
        var b=Booth.values()[l.carnivalBooth];var base=center(p,s).offset(b.x,0,b.z);long now=OtherMotherLoop.now(p);
        if(b==Booth.HIGH_STRIKER&&at.equals(base.offset(5,0,3))){
            var mallet=p.getMainHandItem();
            if(!mallet.is(ModItems.CARNIVAL_MALLET.get())||!mallet.hasTag()||!mallet.getTag().hasUUID(OWNER)||
                !p.getUUID().equals(mallet.getTag().getUUID(OWNER))||mallet.getTag().getInt(GEN)!=l.generation){
                p.displayClientMessage(Component.literal("Hold the carnival mallet to strike."),true);return true;
            }
            if(now-l.carnivalLastActionAt>=8){l.carnivalLastActionAt=now;
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
                int gap=Math.abs(20-Math.floorMod(now-l.carnivalRoundAt,40));int points=gap<=2?3:gap<=5?2:gap<=8?1:0;
                l.carnivalProgress+=points;l.carnivalTarget++;
                if(points==0)l.carnivalFailures++;
                p.serverLevel().playSound(null,at,SoundEvents.ANVIL_LAND,SoundSource.BLOCKS,1F,points>=2?1.5F:.8F);
                if(points==3){
                    p.serverLevel().playSound(null,base.offset(5,9,2),SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,1.4F,1.1F);
                    p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,base.getX()+5.5,base.getY()+9,base.getZ()+2.5,20,.6,.5,.6,.06);
                }
                p.displayClientMessage(Component.literal((points==3?"PERFECT! ":points==2?"STRONG! ":points==1?"GLANCING ":"MISS ")+l.carnivalProgress+"/9").withStyle(ChatFormatting.GOLD),true);
                if(l.carnivalTarget>=3)finish(p,s,l.carnivalProgress>=b.steps);
                else{
                    l.carnivalRoundAt=now;
                    for(var meter:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(base).inflate(8,10,8),
                        e->e.kind()==ScenePropEntity.CARNIVAL_STRIKER_METER&&s.owner.equals(e.owner)))meter.mode(1+l.carnivalTarget);
                    OtherMotherLoop.changed(p);
                }
            }return true;
        }
        return false;
    }
    public static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        var l=s.loop;
        if(prop.kind()==ScenePropEntity.CARNIVAL_WHEEL){
            if(l.carnivalBooth!=Booth.LUCKY_WHEEL.ordinal()){startGame(p,s,Booth.LUCKY_WHEEL);return true;}
            if(l.carnivalProgress==0){
                l.carnivalProgress=1;l.carnivalRoundAt=OtherMotherLoop.now(p);l.carnivalGameUntil=l.carnivalRoundAt+100;
                prop.cakeStyle(l.carnivalTarget);prop.mode(1);OtherMotherLoop.changed(p);
                p.serverLevel().playSound(null,prop.blockPosition(),SoundEvents.UI_BUTTON_CLICK.value(),SoundSource.BLOCKS,1F,1F);
            }
            return true;
        }
        if(prop.kind()!=ScenePropEntity.CARNIVAL_DUCK)return prop.kind()==ScenePropEntity.CARNIVAL_BALLOON;
        if(l.carnivalBooth!=Booth.DUCK_POND.ordinal())return true;
        if(p.distanceToSqr(prop)>24)return true;
        // Follow the flashing duck; indiscriminate clicking loses time/points.
        int target=Math.floorMod((int)((OtherMotherLoop.now(p)-l.carnivalRoundAt)/30),8);
        if(prop.slot!=target){l.carnivalFailures++;l.carnivalProgress=Math.max(0,l.carnivalProgress-1);
            p.displayClientMessage(Component.literal("Wrong duck — catch the sparkling duck!"),true);OtherMotherLoop.changed(p);return true;}
        if(OtherMotherLoop.now(p)-l.carnivalLastActionAt<25)return true;
        l.carnivalLastActionAt=OtherMotherLoop.now(p);l.carnivalProgress++;l.carnivalTarget++;
        l.carnivalRoundAt-=30;
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.CHICKEN_EGG,SoundSource.PLAYERS,.8F,1.2F);
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,prop.getX(),prop.getY(),prop.getZ(),8,.2,.1,.2,.03);
        if(l.carnivalProgress>=6)finish(p,s,true);
        else OtherMotherLoop.changed(p);
        return true;
    }
    public static void interactNpc(ServerPlayer p,Entity npc){
        if(!npc.getPersistentData().getBoolean(ATTENDANT))return;
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.CARNIVAL||!ActivityWorld.isActivity(p.level()))return;
        if(!npc.getPersistentData().hasUUID(OWNER)||!s.owner.equals(npc.getPersistentData().getUUID(OWNER))||p.distanceToSqr(npc)>36)return;
        int slot=npc.getPersistentData().getInt("AttendantSlot");
        if(slot>=8){boardRide(p,s,slot-8);return;}
        startGame(p,s,Booth.values()[slot]);
    }
    private static void boardRide(ServerPlayer p,VisitData.Scene s,int ride){
        var l=s.loop;if(!l.carnivalIssued){p.displayClientMessage(Component.literal("Wait for Father to finish speaking."),true);return;}
        if(l.carnivalRide>=0||l.carnivalBooth>=0){p.displayClientMessage(Component.literal("Finish your current attraction first."),true);return;}
        int price=cost(ride==0?Booth.FERRIS_WHEEL:Booth.CAROUSEL);
        if(l.carnivalTokensRemaining<price){p.displayClientMessage(Component.literal("This ride costs "+price+" tokens. You have "+l.carnivalTokensRemaining+"."),true);return;}
        var seats=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(center(p,s)).inflate(55,40,55),e->e.kind()==(ride==0?ScenePropEntity.CARNIVAL_GONDOLA:ScenePropEntity.CARNIVAL_MOUNT)&&e.slot==70+ride&&s.owner.equals(e.owner));
        if(seats.isEmpty())return;var seat=seats.get(0);if(!seat.getPassengers().isEmpty()){p.displayClientMessage(Component.literal("This seat is occupied."),true);return;}
        if(p.startRiding(seat,true)){if(!spendTokens(p,s,price)){p.stopRiding();p.displayClientMessage(Component.literal("Your issued tokens are missing."),true);return;}
            l.carnivalTokensRemaining-=price;l.carnivalRide=ride;l.carnivalRideUntil=OtherMotherLoop.now(p)+600;OtherMotherLoop.changed(p);
            if(ride==0)for(var wheel:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(center(p,s).offset(0,17,-40)).inflate(18),
                e->e.kind()==ScenePropEntity.CARNIVAL_FERRIS_WHEEL&&s.owner.equals(e.owner)))wheel.mode(1);
            p.sendSystemMessage(Component.literal(ride==0?"The Ferris wheel lifts you over the lights. Sneak to leave early.":"The carousel begins to turn. Sneak to leave early."));}
    }
    private static void startGame(ServerPlayer p,VisitData.Scene s,Booth selected){
        var l=s.loop;long now=OtherMotherLoop.now(p);
        if(!l.carnivalIssued){p.displayClientMessage(Component.literal("Wait for Father to finish speaking."),true);return;}
        if(l.carnivalRide>=0||l.carnivalBooth>=0){p.displayClientMessage(Component.literal("Finish your current attraction first."),true);return;}
        if(now-l.carnivalLastActionAt<8)return;
        int price=cost(selected);
        if(l.carnivalTokensRemaining<price){p.displayClientMessage(Component.literal(selected.title+" costs "+price+" tokens. You have "+l.carnivalTokensRemaining+"."),true);return;}
        if(!spendTokens(p,s,price)){p.displayClientMessage(Component.literal("Your issued tokens are missing."),true);return;}
        l.carnivalTokensRemaining-=price;l.carnivalBooth=selected.ordinal();l.carnivalProgress=0;l.carnivalFailures=0;
        l.carnivalRoundAt=now;l.carnivalGameUntil=now+(selected==Booth.MIRROR_MAZE?120:selected==Booth.LUCKY_WHEEL?20:selected.game==Game.INSTANT?1:45)*20L;
        l.carnivalTarget=selected==Booth.HIGH_STRIKER||selected==Booth.DUCK_POND?0:p.getRandom().nextInt(8);l.carnivalLastActionAt=now-10;OtherMotherLoop.changed(p);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.UI_BUTTON_CLICK.value(),SoundSource.PLAYERS,.8F,1.1F);
        if(selected.game==Game.INSTANT){
            var spout=center(p,s).offset(selected.x,1,selected.z+2);
            p.serverLevel().playSound(null,spout,SoundEvents.DISPENSER_DISPENSE,SoundSource.BLOCKS,.85F,1.15F);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                spout.getX()+.5,spout.getY()+.3,spout.getZ()+.5,12,.25,.2,.25,.03);
            finish(p,s,true);return;
        }
        if(selected==Booth.MIRROR_MAZE){mirrorGarden(p.serverLevel(),center(p,s).offset(-36,0,-32),p.getRandom().nextLong());
            var at=center(p,s).offset(-37,0,-19);
            p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,180,0);
            title(p,"MIRROR MAZE","Find the illuminated exit");return;}
        if(selected==Booth.LUCKY_WHEEL){title(p,"FORTUNE WHEEL","Right-click to spin");return;}
        if(selected==Booth.HIGH_STRIKER){
            var hammer=new ItemStack(ModItems.CARNIVAL_MALLET.get());hammer.getOrCreateTag().putUUID(OWNER,p.getUUID());hammer.getTag().putInt(GEN,l.generation);
            p.getInventory().add(hammer);if(!hammer.isEmpty())p.drop(hammer,false);
            var base=center(p,s).offset(selected.x,0,selected.z);
            for(var meter:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(base).inflate(8,10,8),
                e->e.kind()==ScenePropEntity.CARNIVAL_STRIKER_METER&&s.owner.equals(e.owner)))meter.mode(1);
            title(p,"BELL BASH","Strike the plate when the light reaches gold");return;}
        if(selected==Booth.DUCK_POND){
            var at=center(p,s).offset(selected.x,0,selected.z);
            p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+4.35,180,25);
            title(p,"DUCK SPLASH","Right-click the gold-sparkling duck • move along the counter");return;}
        setupTargets(p.serverLevel(),center(p,s),selected);
        removeAmmo(p);
        var ammo=new ItemStack(selected==Booth.BALLOON_DARTS?ModItems.CARNIVAL_DART.get():ModItems.CARNIVAL_RING.get(),5);
        ammo.getOrCreateTag().putUUID(OWNER,p.getUUID());ammo.getTag().putInt(GEN,l.generation);ammo.getTag().putInt("Booth",selected.ordinal());
        p.getInventory().add(ammo);if(!ammo.isEmpty())p.drop(ammo,false);
        title(p,selected==Booth.BALLOON_DARTS?"BALLOON DARTS":"RING TOSS",selected==Booth.BALLOON_DARTS?"Pop all 3 fast balloons with 5 darts":"Aim at the gold bottle tops — score 4 with 5 rings");
    }
    private static void setupTargets(ServerLevel level,BlockPos c,Booth b){
        // Targets are persistent modeled entities, never re-painted world blocks.
    }
    static Vec3 duckPosition(BlockPos at,int slot,long now){
        // Two separated rows at the FRONT of the pool, within ordinary interaction reach.
        return new Vec3(at.getX()+.5+(slot%4-1.5)*1.05+(slot/4)*.50+.05*Math.sin(now*.08+slot),
            at.getY()+.85,at.getZ()+1.6+(slot/4)*.90+.06*Math.cos(now*.07+slot));
    }
    static boolean ringHitsNeck(AABB bottle,Vec3 from,Vec3 velocity){
        // Snowball's vanilla EntityHitResult contains the entity origin, NOT contact position.
        var collision=bottle.inflate(.3);
        var contact=collision.contains(from)?java.util.Optional.of(from):collision.clip(from,from.add(velocity));
        return contact.isPresent()&&contact.get().y>=bottle.minY+.35;
    }
    private static void animateTargets(ServerLevel level,BlockPos c,Booth b,long now){
        if(b==Booth.BALLOON_DARTS){
            var at=c.offset(b.x,0,b.z);
            for(var balloon:level.getEntitiesOfClass(ScenePropEntity.class,new AABB(at).inflate(5,6,5),e->e.kind()==ScenePropEntity.CARNIVAL_BALLOON)){
                int slot=balloon.slot;
                double sway=2.05*Math.sin(now*.15+slot*2.094);
                balloon.setPos(at.getX()+.5+sway,at.getY()+1.5+.9*Math.sin(now*.19+slot*2.1),at.getZ()-1.62+slot*.18);
            }
        }else if(b==Booth.DUCK_POND){
            var at=c.offset(b.x,0,b.z);
            for(var duck:level.getEntitiesOfClass(ScenePropEntity.class,new AABB(at).inflate(5,3,5),
                e->e.kind()==ScenePropEntity.CARNIVAL_DUCK)){
                int slot=duck.slot;
                duck.setPos(duckPosition(at,slot,now));
                duck.setYRot((float)(Math.sin(now*.04+slot)*18));
            }
        }
    }
    public static void throwProjectile(ServerPlayer p,ItemStack stack,boolean dart){
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.CARNIVAL||!ActivityWorld.isActivity(p.level()))return;
        var l=s.loop;if(l.carnivalBooth<0)return;var b=Booth.values()[l.carnivalBooth];
        if(!ammoMatches(stack,p,s,b)||dart!=(b==Booth.BALLOON_DARTS))return;
        var base=center(p,s).offset(b.x,0,b.z);
        if(p.getZ()<base.getZ()+3.35||p.getZ()>base.getZ()+8||Math.abs(p.getX()-(base.getX()+.5))>3.6){
            p.displayClientMessage(Component.literal("Throw from behind this booth's line."),true);return;
        }
        var snowball=new Snowball(p.serverLevel(),p);snowball.setItem(new ItemStack(dart?ModItems.CARNIVAL_DART.get():ModItems.CARNIVAL_RING.get()));
        snowball.getPersistentData().putBoolean("CoralineDecoration",true);snowball.getPersistentData().putBoolean("CarnivalThrown",true);
        snowball.getPersistentData().putUUID(OWNER,p.getUUID());snowball.getPersistentData().putInt(GEN,l.generation);snowball.getPersistentData().putInt("Booth",b.ordinal());
        snowball.shootFromRotation(p,p.getXRot(),p.getYRot(),0,dart?1.7F:1.25F,dart?.25F:.5F);p.serverLevel().addFreshEntity(snowball);
        stack.shrink(1); // Issued game ammunition is finite even in Creative.
        l.carnivalLastActionAt=OtherMotherLoop.now(p);OtherMotherLoop.changed(p);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.7F,dart?1.4F:.9F);
    }
    @SubscribeEvent public static void impact(ProjectileImpactEvent event){
        if(!(event.getProjectile() instanceof Snowball ball)||!ball.getPersistentData().getBoolean("CarnivalThrown")||!(ball.getOwner() instanceof ServerPlayer p))return;
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.CARNIVAL||s.loop.carnivalBooth<0)return;
        var tag=ball.getPersistentData();var b=Booth.values()[s.loop.carnivalBooth];
        if(!tag.hasUUID(OWNER)||!p.getUUID().equals(tag.getUUID(OWNER))||tag.getInt(GEN)!=s.loop.generation||tag.getInt("Booth")!=b.ordinal())return;
        if(event.getRayTraceResult() instanceof EntityHitResult entityHit&&
            (entityHit.getEntity() instanceof ServerPlayer||isCarnivalNpc(entityHit.getEntity()))){
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);return;}
        if(event.getRayTraceResult() instanceof EntityHitResult entityHit&&b==Booth.RING_TOSS){
            if(!(entityHit.getEntity() instanceof ScenePropEntity peg)||peg.kind()!=ScenePropEntity.CARNIVAL_PEG||
                !s.owner.equals(peg.owner))return;
            boolean landed=ringHitsNeck(peg.getBoundingBox(),ball.position(),ball.getDeltaMovement());
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);ball.discard();
            if(landed){
                s.loop.carnivalProgress+=peg.slot==1?2:1;
                p.serverLevel().playSound(null,peg.blockPosition(),SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.BLOCKS,1F,1.35F);
                p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,peg.getX(),peg.getY()+.8,peg.getZ(),10,.25,.25,.25,.02);
                if(s.loop.carnivalProgress>=b.steps)finish(p,s,true);else OtherMotherLoop.changed(p);
            }else{s.loop.carnivalFailures++;p.displayClientMessage(Component.literal("Aim higher: hit the gold bottle neck."),true);OtherMotherLoop.changed(p);}
            return;
        }
        if(event.getRayTraceResult() instanceof EntityHitResult entityHit&&b==Booth.BALLOON_DARTS){
            if(!(entityHit.getEntity() instanceof ScenePropEntity balloon)||balloon.kind()!=ScenePropEntity.CARNIVAL_BALLOON||
                !s.owner.equals(balloon.owner))return;
            int points=1;
            s.loop.carnivalProgress+=points;
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,balloon.getX(),balloon.getY()+.7,balloon.getZ(),14,.25,.3,.25,.08);
            p.serverLevel().playSound(null,balloon.blockPosition(),SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.BLOCKS,.45F,1.7F);
            balloon.discard();
            if(s.loop.carnivalProgress>=b.steps)finish(p,s,true);else OtherMotherLoop.changed(p);
            return;
        }
        if(!(event.getRayTraceResult() instanceof BlockHitResult hit))return;
        var pos=hit.getBlockPos();
        s.loop.carnivalFailures++;
        p.serverLevel().playSound(null,pos,SoundEvents.NOTE_BLOCK_BASS.value(),SoundSource.PLAYERS,.5F,.8F);
        OtherMotherLoop.changed(p);
    }
    static int cycle(Booth b){return switch(b.game){case RING->40;case STRIKER->55;case WHEEL->60;default->1;};}
    static int windowStart(Booth b){return switch(b.game){case RING->17;case STRIKER->27;case WHEEL->43;default->0;};}
    static int windowEnd(Booth b){return switch(b.game){case RING->23;case STRIKER->32;case WHEEL->48;default->0;};}
    /** Legacy screen packets cannot score a game; all outcomes now come from in-world hits. */
    public static void action(ServerPlayer p,int choice){}
    private static void finish(ServerPlayer p,VisitData.Scene s,boolean success){
        var l=s.loop;if(l.carnivalBooth<0)return;var booth=Booth.values()[l.carnivalBooth];
        int tier;
        if(booth.game==Game.INSTANT)tier=0;
        else if(booth==Booth.MIRROR_MAZE)tier=success?5:0;
        else if(booth==Booth.LUCKY_WHEEL)tier=success?1+Math.floorMod(l.carnivalTarget,5):0;
        else{
            double progress=Math.min(1.0,l.carnivalProgress/(double)Math.max(1,booth.steps));
            double accuracy=l.carnivalProgress/(double)Math.max(1,l.carnivalProgress+l.carnivalFailures);
            tier=(int)Math.round(5*progress*(.35+.65*accuracy));
            if(success&&tier==0)tier=1;
            if(!success)tier=Math.min(tier,1);
        }
        l.carnivalBooth=-1;l.carnivalGameUntil=0;removeAmmo(p);OtherMotherLoop.changed(p);
        CarnivalNetwork.send(p,s,false);
        ensureGameProps(p.serverLevel(),s,center(p,s));
        var prize=booth==Booth.LUCKY_WHEEL&&success?wheelPrize(l.carnivalTarget):reward(p,tier,booth);
        var name=prize.getHoverName().getString();
        p.getInventory().add(prize);if(!prize.isEmpty())p.drop(prize,false);
        p.serverLevel().playSound(null,p.blockPosition(),success?SoundEvents.UI_TOAST_CHALLENGE_COMPLETE:SoundEvents.VILLAGER_NO,SoundSource.PLAYERS,.9F,1F);
        title(p,booth.game==Game.INSTANT?"SERVED!":success?"PRIZE WINNER!":"GAME OVER",name+" • "+l.carnivalTokensRemaining+" tokens left");
        ActivityWorld.ensureExit(p,s);
    }
    private static ItemStack wheelPrize(int segment){return switch(Math.floorMod(segment,8)){
        case 0->new ItemStack(Items.BREAD,4);case 1->new ItemStack(Items.EMERALD,4);
        case 2->new ItemStack(Items.DIAMOND,2);case 3->new ItemStack(Items.GOLDEN_APPLE,1);
        case 4->new ItemStack(Items.AMETHYST_SHARD,8);case 5->new ItemStack(Items.ENCHANTED_BOOK,1);
        case 6->new ItemStack(Items.GOLD_INGOT,5);default->new ItemStack(Items.NETHERITE_SCRAP,1);
    };}
    private static ItemStack cartFood(Booth booth){
        String id=switch(booth){case POPCORN->"popcorn";case CANDY_APPLES->"lollipop";case COTTON_CANDY->"cotton_candy";
            case PIE_CART->"hot_dog_in_bun";case LEMONADE->"lemonade";default->"cocoa_beetles";};
        return new ItemStack(com.coraline.food.TreatFoods.ITEMS.get(id).get());
    }
    private static ItemStack reward(ServerPlayer p,int tier,Booth booth){
        var r=p.getRandom();if(booth.game==Game.INSTANT)return cartFood(booth);
        return switch(tier){
            case 0->new ItemStack(r.nextBoolean()?Items.COOKIE:Items.PAPER,2+r.nextInt(3));
            case 1->new ItemStack(r.nextBoolean()?Items.IRON_INGOT:Items.EMERALD,2+r.nextInt(3));
            case 2->new ItemStack(r.nextBoolean()?Items.GOLD_INGOT:Items.EMERALD,3+r.nextInt(3));
            case 3->new ItemStack(r.nextBoolean()?Items.DIAMOND:Items.EMERALD,3+r.nextInt(3));
            case 4->new ItemStack(r.nextBoolean()?Items.DIAMOND:Items.NETHERITE_SCRAP,4+r.nextInt(3));
            default->r.nextInt(4)==0?new ItemStack(Items.ENCHANTED_GOLDEN_APPLE,1):new ItemStack(Items.DIAMOND,7+r.nextInt(3));
        };
    }
}
