package com.coraline.feast;

import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.phys.Vec3;

/** Owner-authoritative, physical baking workflow. Only the final score enters judging. */
final class CakeMaking {
    static final String[] BASE={"Vanilla","Chocolate","Strawberry","Red Velvet","Carrot","Lemon","Funfetti"};
    static final String[] INGREDIENT={"Flour","Sugar","Eggs","Butter","Vanilla","Cocoa","Strawberries","Red velvet","Carrot","Lemon","Sprinkles","Milk"};
    private static final Item[] ICON={Items.WHEAT,Items.SUGAR,Items.EGG,Items.GOLD_NUGGET,Items.HONEY_BOTTLE,Items.COCOA_BEANS,
        Items.SWEET_BERRIES,Items.RED_DYE,Items.CARROT,Items.GLOW_BERRIES,Items.FIREWORK_STAR,Items.MILK_BUCKET};
    static final String[] FILLING={"Vanilla cream","Chocolate cream","Strawberry jam","Raspberry jam","Caramel","Custard"};
    static final String[] FROSTING={"Vanilla buttercream","Chocolate ganache","Strawberry cream","Blueberry cream","Caramel glaze","Cream cheese","Dark chocolate"};
    static final String[] TOPPING={"Strawberries","Blueberries","Cherries","Chocolate curls","Sprinkles","Candies","Caramel drizzle","Chocolate drizzle","Whipped cream","Button candies"};
    static final String[] TOPPER={"Moon","Star","Flower","Heart","Candle","Button","Tiny crown"};
    private static final int COMMON=15;
    private static final int[] REQUIRE={COMMON|(1<<4),COMMON|(1<<5),COMMON|(1<<6),COMMON|(1<<7),COMMON|(1<<8),COMMON|(1<<9),COMMON|(1<<10)};
    private CakeMaking(){}
    static String baseName(int base){return BASE[Math.max(0,Math.min(BASE.length-1,base))];}
    static CompoundTag design(OtherMotherLoopState l){
        var n=new CompoundTag();n.putInt("Base",Math.max(0,l.cakeBase));n.putInt("Filling",l.cakeFilling);n.putInt("Frosting",l.cakeFrosting);
        n.putInt("Layers",Math.max(1,l.cakeLayers));n.putInt("Toppings",l.cakeToppings);n.putInt("Piping",l.cakePiping);
        n.putInt("Topper",l.cakeTopper);n.putInt("FrostCoverage",l.cakeFrostCoverage);n.putInt("MixQuality",l.cakeMixQuality);
        n.putInt("BakeQuality",l.cakeBakeQuality);return n;
    }
    private static void put(ServerLevel l,BlockPos at,Block b){if(!l.getBlockState(at).is(b))ActivityBlocks.set(l,at,b.defaultBlockState(),2);}
    static void install(ServerLevel level,VisitData.Scene s,BlockPos c){
        // Replace legacy colored debug cubes, including in previously generated arenas.
        for(int z=-3;z<=3;z++){
            put(level,c.offset(-2,0,z),Blocks.SMOOTH_QUARTZ);
            put(level,c.offset(2,0,z),Blocks.SMOOTH_QUARTZ);
            put(level,c.offset(-2,2,z),Blocks.AIR);
            put(level,c.offset(2,2,z),Blocks.AIR);
        }
        for(int z=-3;z<=3;z++){
            put(level,c.offset(-3,1,z),Blocks.CHERRY_TRAPDOOR);
            put(level,c.offset(3,1,z),Blocks.CHERRY_TRAPDOOR);
        }
        put(level,c,ModBlocks.LOW_MARBLE_TABLE.get());
        put(level,c.south(2),ModBlocks.LOW_MARBLE_TABLE.get());
        put(level,c.north(2),Blocks.SMOKER);
        for(int x:new int[]{-1,1}){put(level,c.offset(x,0,-2),Blocks.SMOOTH_QUARTZ);put(level,c.offset(x,0,2),Blocks.SMOOTH_QUARTZ);}
        for(int z:new int[]{-3,3}){put(level,c.offset(-2,3,z),Blocks.SEA_LANTERN);put(level,c.offset(2,3,z),Blocks.SEA_LANTERN);}
        put(level,c.offset(0,0,3),Blocks.POTTED_PINK_TULIP);
        ensureDisplays(level,s,c);
        if(s.loop.cakeStage==3)sync(level,s,c);
    }
    private static void display(ServerLevel level,VisitData.Scene s,BlockPos c,int id,Vec3 pos,Item icon){
        var matches=level.getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(c).inflate(6,4,7),
            e->e.kind()==ScenePropEntity.CAKE_KITCHEN&&e.slot==id&&s.owner.equals(e.owner));
        if(matches.size()==1&&matches.get(0).position().distanceToSqr(pos)<.01)return;
        // Old builds can retain a display at its former position. Search by
        // owner and slot across the booth so it cannot overlap the new one.
        matches.forEach(ScenePropEntity::discard);
        var e=LoopProps.spawn(level,s,ScenePropEntity.CAKE_KITCHEN,pos,id);e.mode(id);e.food(0,new ItemStack(icon));
    }
    private static void ensureDisplays(ServerLevel l,VisitData.Scene s,BlockPos c){
        // One supported row, no upper jars, no shared slots. The right counter
        // changes from recipe cards to decorating tools after baking.
        boolean decorating=s.loop.cakeStep>=4;
        for(var e:LoopProps.nearby(l,s,c))if(e.kind()==ScenePropEntity.CAKE_KITCHEN&&
            (decorating?e.slot>=100&&e.slot<107:e.slot>=200))e.discard();
        for(int id=0;id<12;id++){
            display(l,s,c,id,new Vec3(c.getX()-1.5,c.getY()+1.01,c.getZ()-2.75+id*.55),ICON[id]);
        }
        Item[] recipe={Items.CAKE,Items.COCOA_BEANS,Items.SWEET_BERRIES,Items.RED_DYE,Items.CARROT,Items.GLOW_BERRIES,Items.FIREWORK_STAR};
        if(!decorating)for(int id=0;id<7;id++)display(l,s,c,100+id,new Vec3(c.getX()+2.5,c.getY()+1.01,c.getZ()-2.5+id),recipe[id]);
        else {
            display(l,s,c,200,new Vec3(c.getX()+2.5,c.getY()+1.01,c.getZ()-2),Items.HONEY_BOTTLE);
            display(l,s,c,300,new Vec3(c.getX()+2.5,c.getY()+1.01,c.getZ()-.5),Items.SUGAR);
            display(l,s,c,400,new Vec3(c.getX()+2.5,c.getY()+1.01,c.getZ()+1),Items.SWEET_BERRIES);
            display(l,s,c,500,new Vec3(c.getX()+2.5,c.getY()+1.01,c.getZ()+2.5),Items.FIREWORK_STAR);
        }
    }
    static String instruction(OtherMotherLoopState l,long now){
        if(l.cakeStep==0){
            if(l.cakeBase<0)return "Choose a recipe card on the right";
            int next=nextRequiredIngredient(l);
            if(next>=0)return baseName(l.cakeBase)+": collect "+INGREDIENT[next]+" from the left pantry";
            return "Ingredients ready — right-click the center MIXING BOWL";
        }
        if(l.cakeStep==1){long age=now-l.cakeMixStartedAt;return "MIX "+meter(age,80,40,60)+" "+(age<40?"Wait...":age<=60?"PERFECT — RIGHT-CLICK BOWL NOW!":"Overmixed — right-click bowl");}
        if(l.cakeStep==2)return "Right-click OVEN to bake • Front stand: optional layers";
        if(l.cakeStep==3){long age=now-l.cakeOvenStartedAt;return "OVEN "+meter(age,190,106,144)+" "+(age<40?"RAW — wait":age<85?"RISING — wait":age<106?"GOLDEN — nearly ready":age<=144?"PERFECT — RIGHT-CLICK OVEN NOW!":age<176?"OVERBAKED — remove now":"BURNT — remove cake");}
        if(l.cakeFilling<0)return "Right-click the sparkling FILLING tray on the right";
        if(l.cakeFrosting<0)return "Right-click the sparkling FROSTING tray on the right";
        if(l.cakeFrostCoverage<4)return "FROST "+(l.cakeFrostCoverage+1)+"/4 "+meter(now%60,60,20,40)+" — "+(frostReady(now)?"RIGHT-CLICK CAKE STAND NOW!":"Wait for the green window");
        return "Right trays: add decorations • Front CAKE STAND: right-click to finish";
    }
    private static boolean frostReady(long now){return now%60>=20&&now%60<=40;}
    private static int nextRequiredIngredient(OtherMotherLoopState l){
        if(l.cakeBase<0||l.cakeBase>=REQUIRE.length)return -1;
        int missing=REQUIRE[l.cakeBase]&~l.cakeIngredients;
        return missing==0?-1:Integer.numberOfTrailingZeros(missing);
    }
    private static boolean activeIngredient(OtherMotherLoopState l,int id){
        if(l.cakeStep!=0||l.cakeBase<0||id<0||id>=INGREDIENT.length||(l.cakeIngredients&(1<<id))!=0)return false;
        int next=nextRequiredIngredient(l);
        return next>=0?id==next:!required(l.cakeBase,id);
    }
    private static boolean activeProp(OtherMotherLoopState l,int id){
        if(id>=0&&id<12)return activeIngredient(l,id);
        if(id>=100&&id<107)return l.cakeStep==0&&l.cakeBase<0;
        if(l.cakeStep!=4)return false;
        if(id==200)return l.cakeFilling<0;
        if(id==300)return l.cakeFilling>=0&&l.cakeFrosting<0;
        return (id==400||id==500)&&l.cakeFilling>=0&&l.cakeFrosting>=0&&l.cakeFrostCoverage>=4;
    }
    private static boolean activeStation(OtherMotherLoopState l,BlockPos c,BlockPos at){
        if(at.equals(c))return l.cakeStep==0&&l.cakeBase>=0&&nextRequiredIngredient(l)<0||l.cakeStep==1;
        if(at.equals(c.north(2)))return l.cakeStep==2||l.cakeStep==3;
        if(at.equals(c.south(2)))return l.cakeStep==2||l.cakeStep==4&&l.cakeFilling>=0&&l.cakeFrosting>=0;
        return false;
    }
    private static String meter(long age,int total,int start,int end){
        StringBuilder b=new StringBuilder("[");int cursor=(int)Math.min(19,Math.max(0,age*20/total));
        for(int i=0;i<20;i++)b.append(i==cursor?"§f▌":i*total/20>=start&&i*total/20<=end?"§a▬":"§7▬");
        return b.append("§e]§l").toString();
    }
    static void guidance(ServerPlayer p,VisitData.Scene s,long now){
        // Timed checks must never be hidden behind click confirmations.
        var tag=p.getPersistentData();
        String message=instruction(s.loop,now);
        if(s.loop.cakeStep!=1&&s.loop.cakeStep!=3&&!(s.loop.cakeStep==4&&s.loop.cakeFrosting>=0&&s.loop.cakeFrostCoverage<4)
            &&tag.getLong("CakeHintUntil")>now)message=tag.getString("CakeHint");
        objective(p,message);
        var eye=p.getEyePosition();var look=p.getLookAngle();
        for(var e:LoopProps.nearby(p.serverLevel(),s,CakeContestActivity.center(p,s)))if(e.kind()==ScenePropEntity.CAKE_KITCHEN){
            boolean aimed=e.getBoundingBox().inflate(.08).clip(eye,eye.add(look.scale(4.5))).isPresent();
            e.setCustomName(Component.literal(label(e.slot,s.loop)).withStyle(ChatFormatting.GOLD));e.setCustomNameVisible(aimed&&activeProp(s.loop,e.slot));
        }
    }
    private static String label(int id,OtherMotherLoopState l){
        if(id<12)return "Right-click: Take "+INGREDIENT[id];
        if(id>=100&&id<107)return "Right-click: "+BASE[id-100]+" recipe";
        return switch(id){case 200->"FILLING — click: "+FILLING[(l.cakeFilling+1)%FILLING.length];
            case 300->"FROSTING — click: "+FROSTING[(l.cakeFrosting+1)%FROSTING.length];
            case 400->TOPPING[l.cakeDecorationSlots]+" — click: add / sneak-click: change";case 500->"TOPPER — click to choose";default->"";};
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var l=s.loop;var level=p.serverLevel();
        long remaining=l.cakeBakeUntil-now;
        if(remaining==1200||remaining==600||remaining==200){
            cue(p,c,SoundEvents.NOTE_BLOCK_XYLOPHONE.value(),remaining==200?1.8F:1.2F);
        }
        if(p.tickCount%20==0)ensureDisplays(level,s,c);
        if(l.cakeStep==1&&now-l.cakeMixStartedAt==40||l.cakeStep==3&&now-l.cakeOvenStartedAt==106||l.cakeStep==4&&l.cakeFrostCoverage<4&&now%60==20)
            cue(p,c,SoundEvents.NOTE_BLOCK_BELL.value(),1.5F);
        for(var prop:LoopProps.nearby(level,s,c))if(prop.kind()==ScenePropEntity.CAKE_MIXER){
            prop.mode(l.cakeStep==1?1:0);
            prop.setCustomName(Component.literal("MIXING BOWL").withStyle(ChatFormatting.GOLD));
            prop.setCustomNameVisible(l.cakeStep<=1);
        }
        if(l.cakeStep==3&&p.tickCount%10==0)level.sendParticles(now-l.cakeOvenStartedAt>176?ParticleTypes.SMOKE:ParticleTypes.END_ROD,
            c.getX()+.5,c.getY()+1.2,c.getZ()-1.5,3,.25,.13,.24,.008);
        if(p.tickCount%10==0){sync(level,s,c);
            var displays=LoopProps.nearby(level,s,c);
            if(l.cakeStep==0){
                if(l.cakeBase<0){for(var prop:displays)if(prop.kind()==ScenePropEntity.CAKE_KITCHEN&&prop.slot>=100&&prop.slot<107)pulse(p,prop.position());}
                else {if(nextRequiredIngredient(l)<0)pulse(p,Vec3.atCenterOf(c).add(0,.4,0));
                    for(var prop:displays)if(prop.kind()==ScenePropEntity.CAKE_KITCHEN&&activeIngredient(l,prop.slot))pulse(p,prop.position());}
            }else if(l.cakeStep==1)pulse(p,Vec3.atCenterOf(c).add(0,.4,0));
            else if(l.cakeStep==2){pulse(p,Vec3.atCenterOf(c.south(2)).add(0,.3,0));pulse(p,Vec3.atCenterOf(c.north(2)).add(0,.3,0));}
            else if(l.cakeStep==3)pulse(p,Vec3.atCenterOf(c.north(2)).add(0,.3,0));
            else if(l.cakeFilling<0||l.cakeFrosting<0){for(var prop:displays)if(prop.kind()==ScenePropEntity.CAKE_KITCHEN&&activeProp(l,prop.slot))pulse(p,prop.position());}
            else if(l.cakeFrostCoverage<4){
                pulse(p,Vec3.atCenterOf(c.south(2)).add(0,.4,0));
                if(frostReady(now))level.sendParticles(p,ParticleTypes.HAPPY_VILLAGER,true,c.getX()+.5,c.getY()+1.5,c.getZ()+2.5,2,.06,.05,.06,0);
            }
            else {pulse(p,Vec3.atCenterOf(c.south(2)).add(0,.4,0));for(var prop:displays)if(prop.kind()==ScenePropEntity.CAKE_KITCHEN&&activeProp(l,prop.slot))pulse(p,prop.position());}
        }
        if(l.cakeStep==1&&p.tickCount%7==0)level.playSound(null,c,SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,.18F,1.3F);
        if(l.cakeStep==3&&p.tickCount%19==0)level.playSound(null,c.north(2),SoundEvents.FURNACE_FIRE_CRACKLE,SoundSource.BLOCKS,.25F,.85F);
    }
    private static void pulse(ServerPlayer p,Vec3 at){p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,
        at.x,at.y+.20,at.z,1,.015,.015,.015,0);}
    private static void sync(ServerLevel level,VisitData.Scene s,BlockPos c){
        var l=s.loop;if(l.cakeStage!=3)return;var props=LoopProps.nearby(level,s,c);
        for(int slot=0;slot<3;slot++){
            final int id=slot;
            var found=props.stream().filter(e->e.kind()==ScenePropEntity.CAKE_WORKPIECE&&e.slot==id).findFirst().orElse(null);
            if(found==null){var at=slot==0?c:slot==1?c.north(2):c.south(2);
                found=LoopProps.spawn(level,s,ScenePropEntity.CAKE_WORKPIECE,
                    new Vec3(at.getX()+.5,at.getY()+(slot==1?1.02:.72),at.getZ()+.5),id);}
            found.cakeData(design(l));
            if(slot>0){found.setCustomName(Component.literal(slot==1?"OVEN":"CAKE STAND").withStyle(ChatFormatting.GOLD));
                found.setCustomNameVisible(slot==1?l.cakeStep==2||l.cakeStep==3:l.cakeStep>=4);}
            found.mode(slot==0?(l.cakeStep==0?0:l.cakeStep==1?1:10):
                slot==1?(l.cakeStep==3?4+Math.min(5,(int)((level.getGameTime()-l.cakeOvenStartedAt)/35)):10):
                l.cakeStep>=4?11:l.cakeStep==2?3:10);
        }
    }
    static void objective(ServerPlayer p,String message){p.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW,ChatFormatting.BOLD),true);}
    private static void feedback(ServerPlayer p,BlockPos at,String message){
        p.getPersistentData().putString("CakeHint",message);
        p.getPersistentData().putLong("CakeHintUntil",OtherMotherLoop.now(p)+35);
        objective(p,message);
        p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,at.getX()+.5,at.getY()+.8,at.getZ()+.5,5,.27,.2,.27,.01);
        p.serverLevel().playSound(null,at,SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.BLOCKS,.5F,1.25F);
    }
    private static void cue(ServerPlayer p,BlockPos at,SoundEvent sound,float pitch){
        p.serverLevel().playSound(null,at,sound,SoundSource.BLOCKS,.75F,pitch);
    }
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(prop.kind()==ScenePropEntity.CONTEST_CAKE&&prop.slot==99&&s.loop.cakeStage==4&&!s.loop.cakeTaken)
            return CakeContestActivity.interactBlock(p,CakeContestActivity.center(p,s).south(2));
        if(prop.kind()!=ScenePropEntity.CAKE_KITCHEN)return false;
        if(s.loop.cakeStage!=3)return true;
        var l=s.loop;long now=OtherMotherLoop.now(p);
        int id=prop.slot;
        if(!activeProp(l,id)){objective(p,instruction(l,now));return true;}
        if(now-l.cakeLastActionAt<4)return true;l.cakeLastActionAt=now;
        if(id<12){
            if(l.cakeBase<0){feedback(p,prop.blockPosition(),"Choose your recipe first.");return true;}
            if(l.cakeStep!=0){feedback(p,prop.blockPosition(),"Ingredients are already in the bowl.");return true;}
            if((l.cakeIngredients&(1<<id))!=0){
                if(!required(l.cakeBase,id)&&id!=11){l.cakeIngredients&=~(1<<id);OtherMotherLoop.changed(p);feedback(p,prop.blockPosition(),"Removed "+INGREDIENT[id]+" from your ingredients");}
                else feedback(p,prop.blockPosition(),INGREDIENT[id]+" already collected");return true;
            }
            l.cakeIngredients|=1<<id;
            cue(p,prop.blockPosition(),SoundEvents.ITEM_FRAME_ADD_ITEM,1.15F);
            feedback(p,prop.blockPosition(),"Added "+INGREDIENT[id]+(required(l.cakeBase,id)?" ✓":id==11?" — optional milk":" — wrong for this recipe; click again to remove"));
        }else if(id<107){
            if(l.cakeStep!=0||l.cakeIngredients!=0){feedback(p,prop.blockPosition(),"Choose a recipe before collecting ingredients.");return true;}
            l.cakeBase=id-100;l.cakeFlavor=l.cakeBase%3;
            cue(p,prop.blockPosition(),SoundEvents.BOOK_PAGE_TURN,1.15F);
            feedback(p,prop.blockPosition(),BASE[l.cakeBase]+" — Flour, Sugar, Eggs, Butter, "+INGREDIENT[4+l.cakeBase]);
        }else if(id==200){
            if(l.cakeStep<4){feedback(p,prop.blockPosition(),"Fill the cake after baking.");return true;}
            l.cakeFilling=(l.cakeFilling+1)%FILLING.length;cue(p,prop.blockPosition(),SoundEvents.HONEY_BLOCK_PLACE,1.1F);feedback(p,prop.blockPosition(),"Filling: "+FILLING[l.cakeFilling]);
        }else if(id==300){
            if(l.cakeStep<4){feedback(p,prop.blockPosition(),"Frost the baked cake.");return true;}
            if(l.cakeFrostCoverage>0){feedback(p,prop.blockPosition(),"Finish frosting this cake first.");return true;}
            l.cakeFrosting=(l.cakeFrosting+1)%FROSTING.length;l.cakeIcing=l.cakeFrosting%3;
            cue(p,prop.blockPosition(),SoundEvents.HONEY_BLOCK_PLACE,.85F);
            feedback(p,prop.blockPosition(),"Frosting: "+FROSTING[l.cakeFrosting]);
        }else if(id==400){
            if(l.cakeStep<4||l.cakeFrostCoverage<4){feedback(p,prop.blockPosition(),"Frost the cake before decorating.");return true;}
            int count=Integer.bitCount(l.cakeToppings);if(count>=3){feedback(p,prop.blockPosition(),"Three decorations is enough.");return true;}
            if(!p.isShiftKeyDown()){
                int type=l.cakeDecorationSlots;
                if((l.cakeToppings&(1<<type))!=0){feedback(p,prop.blockPosition(),"Already on cake — choose another.");return true;}
                l.cakeToppings|=1<<type;cue(p,prop.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,1.4F);feedback(p,prop.blockPosition(),"Added "+TOPPING[type]+" ("+(count+1)+"/3)");
            }else{l.cakeDecorationSlots=(l.cakeDecorationSlots+1)%TOPPING.length;
                feedback(p,prop.blockPosition(),"Topping: "+TOPPING[l.cakeDecorationSlots]+" — right-click to add");}
        }else if(id==500){
            if(l.cakeStep<4||l.cakeFrostCoverage<4){feedback(p,prop.blockPosition(),"Decorate after frosting.");return true;}
            l.cakeTopper=(l.cakeTopper+1)%TOPPER.length;cue(p,prop.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,.9F);feedback(p,prop.blockPosition(),"Topper: "+TOPPER[l.cakeTopper]);
        }
        OtherMotherLoop.changed(p);sync(p.serverLevel(),s,CakeContestActivity.center(p,s));return true;
    }
    private static boolean required(int base,int id){return base>=0&&(REQUIRE[base]&(1<<id))!=0;}
    static boolean interact(ServerPlayer p,VisitData.Scene s,BlockPos c,BlockPos at,long now){
        var l=s.loop;if(p.distanceToSqr(Vec3.atCenterOf(at))>40)return false;
        if(!at.equals(c)&&!at.equals(c.south(2))&&!at.equals(c.north(2)))return false;
        if(!activeStation(l,c,at)){objective(p,instruction(l,now));return true;}
        if(now-l.cakeLastActionAt<4)return true;l.cakeLastActionAt=now;
        if(at.equals(c)){
            if(l.cakeStep==0){
                if(l.cakeBase<0){feedback(p,at,"Choose a recipe card on the right.");return true;}
                if(nextRequiredIngredient(l)>=0){feedback(p,at,"Collect the required ingredients from the pantry first.");return true;}
                l.cakeStep=1;l.cakeMixStartedAt=now;cue(p,at,SoundEvents.BREWING_STAND_BREW,1.1F);feedback(p,at,"Mixing — click the bowl when the meter says PERFECT.");
            }else if(l.cakeStep==1){
                long age=now-l.cakeMixStartedAt;l.cakeMixQuality=age<16||age>90?0:age>=40&&age<=60?4:age>=28&&age<=75?3:2;
                l.cakeStep=2;cue(p,at,SoundEvents.BUCKET_EMPTY,1.4F);feedback(p,at,"Mix: "+(l.cakeMixQuality==4?"perfect":l.cakeMixQuality==3?"good":l.cakeMixQuality==2?"rough":"poor"));
            }else feedback(p,at,instruction(l,now));
        }else if(at.equals(c.south(2))){
            if(l.cakeStep==2){l.cakeLayers=l.cakeLayers%3+1;cue(p,at,SoundEvents.WOOD_PLACE,1.25F);feedback(p,at,l.cakeLayers+" layer"+(l.cakeLayers==1?"":"s")+" — put the pan in the oven");}
            else if(l.cakeStep==4){
                if(l.cakeFrosting<0||l.cakeFilling<0){feedback(p,at,instruction(l,now));return true;}
                if(l.cakeFrostCoverage<4){l.cakeFrostCoverage++;
                    boolean smooth=frostReady(now);if(smooth)l.cakeFrostQuality++;
                    cue(p,at,SoundEvents.HONEY_BLOCK_SLIDE,smooth?1.3F:.8F);feedback(p,at,(smooth?"Smooth":"Uneven")+" frosting — "+l.cakeFrostCoverage+"/4 sides");}
                else CakeContestActivity.finishBake(p,s,c,true);
            }else feedback(p,at,instruction(l,now));
        }else if(at.equals(c.north(2))){
            if(l.cakeStep==2){l.cakeStep=3;l.cakeOvenStartedAt=now;
                p.serverLevel().setBlock(at,Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING,Direction.SOUTH).setValue(SmokerBlock.LIT,true),2);
                cue(p,at,SoundEvents.FURNACE_FIRE_CRACKLE,1F);
                feedback(p,at,"Baking — remove when the meter says PERFECT.");
            }else if(l.cakeStep==3){long age=now-l.cakeOvenStartedAt;
                l.cakeBakeQuality=age<40||age>176?0:age>=106&&age<=144?4:age>=85&&age<=160?3:1;
                l.cakeStep=4;p.serverLevel().setBlock(at,Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING,Direction.SOUTH),2);
                cue(p,at,l.cakeBakeQuality>=3?SoundEvents.NOTE_BLOCK_BELL.get():SoundEvents.FIRE_EXTINGUISH,1.15F);
                feedback(p,at,"Bake: "+(l.cakeBakeQuality==4?"perfect":l.cakeBakeQuality==3?"good":l.cakeBakeQuality==1?"poor":"raw or burnt"));
            }else feedback(p,at,instruction(l,now));
        }
        OtherMotherLoop.changed(p);sync(p.serverLevel(),s,c);return true;
    }
    static void complete(ServerPlayer p,VisitData.Scene s,BlockPos c,boolean early){
        var l=s.loop;int required=l.cakeBase<0?0:REQUIRE[l.cakeBase],correct=Integer.bitCount(l.cakeIngredients&required);
        int missing=Integer.bitCount(required&~l.cakeIngredients),wrong=Integer.bitCount(l.cakeIngredients&~(required|(1<<11)));
        int recipe=l.cakeBase<0?0:Math.max(0,25-missing*5-wrong*4);
        int mix=Math.min(15,l.cakeMixQuality*4),bake=Math.min(20,l.cakeBakeQuality*5);
        int assembly=l.cakeStep>=4?(l.cakeLayers==3?9:l.cakeLayers==2?7:5)+(l.cakeFilling>=0?1:0):0;
        int frosting=l.cakeFrosting>=0?Math.min(15,l.cakeFrostCoverage+l.cakeFrostQuality*2+3):0;
        int decoration=Math.min(15,Integer.bitCount(l.cakeToppings)*3+(l.cakeTopper>=0?6:0));
        l.cakeScore=Math.min(100,recipe+mix+bake+assembly+frosting+decoration);
        if(l.cakeStep<4)l.cakeScore=Math.min(l.cakeScore,30);
        if(correct<4)l.cakeScore=Math.min(l.cakeScore,45);
        if(wrong>=2)l.cakeScore=Math.min(l.cakeScore,45);
        if(wrong>=4)l.cakeScore=Math.min(l.cakeScore,35);
        if(l.cakeBase<0)l.cakeBase=0;if(l.cakeFrosting<0)l.cakeFrosting=0;
        l.cakeFlavor=l.cakeBase%3;l.cakeIcing=l.cakeFrosting%3;
        for(var prop:LoopProps.nearby(p.serverLevel(),s,c))if(prop.kind()==ScenePropEntity.CAKE_WORKPIECE)prop.discard();
        p.serverLevel().setBlock(c.north(2),Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING,Direction.SOUTH),2);
    }
}
