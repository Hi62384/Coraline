package com.coraline.feast;

import com.coraline.otherworld.MouseEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/** Five-mouse, eighty-second obstacle race with an owner-scoped two-ingot wager. */
public final class MiceCircusActivity {
    private static final String OWNER="CoralineCircusOwner",SLOT="CoralineCircusSlot";
    private static final String[] NAMES={"Pearl","Acorn","Clementine","Mist","Midnight"};
    private static final String[] ACTS={"Moonbeam Tightrope","Golden Hoop Sprint","Tower Tumble","Star Cannon Leap","Labyrinth Dash"};
    private MiceCircusActivity(){}

    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(1540);}
    static boolean exitReady(VisitData.Scene s){return s.loop.circusPhase>=6;}
    private static void put(ServerLevel l,BlockPos p,Block b){ActivityBlocks.set(l,p,ActivityWorld.persistentLeaves(b),2);}
    private static boolean owned(Entity e,VisitData.Scene s){return e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER));}
    private static void mark(Entity e,VisitData.Scene s,int slot){e.getPersistentData().putUUID(OWNER,s.owner);e.getPersistentData().putInt(SLOT,slot);e.getPersistentData().putBoolean("CoralineDecoration",true);}

    static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(3)).is(Blocks.CYAN_GLAZED_TERRACOTTA))return;
        if(l.getBlockState(c.below(3)).is(Blocks.LIME_GLAZED_TERRACOTTA))removeBuild(l,c);
        for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++){
            double r=Math.hypot(x,z);if(r>31.5)continue;
            for(int y=0;y<=20;y++)if(!l.isEmptyBlock(c.offset(x,y,z)))put(l,c.offset(x,y,z),Blocks.AIR);
            BlockPos floor=c.offset(x,-1,z);
            put(l,floor.below(),Blocks.DEEPSLATE_BRICKS);
            put(l,floor,r>27?Blocks.GOLD_BLOCK:r<12?Blocks.SMOOTH_QUARTZ:
                Math.abs(x)<3&&z>11?Blocks.MAGENTA_TERRACOTTA:Math.floorMod(x/3+z/3,2)==0?Blocks.RED_TERRACOTTA:Blocks.PURPLE_TERRACOTTA);
            if(r>28.5){
                for(int y=0;y<10;y++)if(!(z>27&&Math.abs(x)<4&&y<5))
                    put(l,c.offset(x,y,z),Math.floorMod((int)Math.round(Math.atan2(z,x)*8/Math.PI),2)==0?Blocks.RED_WOOL:Blocks.WHITE_WOOL);
            }
            if(r<30){
                int roof=11+(int)((30-r)/4.1);Block roofBlock=Math.floorMod((int)Math.round(Math.atan2(z,x)*10/Math.PI),2)==0?Blocks.RED_WOOL:Blocks.WHITE_WOOL;
                put(l,c.offset(x,roof,z),Math.floorMod(x*17+z*23,109)==0?Blocks.SEA_LANTERN:roofBlock);
            }
        }
        put(l,c.below(3),Blocks.CYAN_GLAZED_TERRACOTTA);
        // A circular inlaid ring, underlit footlights, and a carpet aisle from the entrance.
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++){
            double r=Math.hypot(x,z);if(r>=11.4&&r<12.5)put(l,c.offset(x,-1,z),Math.floorMod(x+z,4)==0?Blocks.SEA_LANTERN:Blocks.GOLD_BLOCK);
            if(r<11.2&&Math.floorMod(x*7+z*11,37)==0)put(l,c.offset(x,-1,z),Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA);
        }
        for(int z=13;z<=29;z++)for(int x=-2;x<=2;x++)put(l,c.offset(x,Math.abs(x)==2?-1:0,z),Math.abs(x)==2?Blocks.GOLD_BLOCK:Blocks.RED_CARPET);
        // Two stepped grandstands, broken into boxes so the view into the ring remains clear.
        for(int side:new int[]{-1,1})for(int z=-19;z<=18;z++)for(int t=0;t<4;t++){
            int x=side*(15+t*2);if(Math.hypot(x,z)>27)continue;
            for(int dx=0;dx<=1;dx++)for(int y=-1;y<t;y++)put(l,c.offset(x+side*dx,y,z),t%2==0?Blocks.DARK_OAK_PLANKS:Blocks.SPRUCE_PLANKS);
            if(Math.floorMod(z,4)==0)put(l,c.offset(x,t,z),Blocks.RED_CARPET);
        }
        // Each obstacle is built from the SAME coordinates as the race route.
        for(int x=-8;x<=8;x++)put(l,c.offset(x,2,-10),Blocks.QUARTZ_SLAB);
        // Double slabs have an exact top at y=3, matching the mice's feet.
        for(int x=-8;x<=8;x++)ActivityBlocks.set(l,c.offset(x,2,-10),Blocks.QUARTZ_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE),2);
        for(var leg:CircusRaceCourse.LEGS)if(leg.act()==0||leg.act()==2){var at=leg.to();if(at.y>0&&at.y==Math.floor(at.y))
            for(int y=0;y<(int)at.y;y++)put(l,c.offset((int)at.x,y,(int)at.z),y==(int)at.y-1?Blocks.SMOOTH_QUARTZ:Blocks.PURPLE_TERRACOTTA);}
        // Open hoops: clear center at z=-5, y=2.1; no solid posts in the flight path.
        for(int x:new int[]{-8,-4,0,4,8})for(int z=-7;z<=-3;z++)for(int y=0;y<=4;y++)
            if(z==-7||z==-3||y==0||y==4)put(l,c.offset(x,y,z),Blocks.GOLD_BLOCK);
        put(l,c.offset(-10,0,10),Blocks.COPPER_BLOCK);put(l,c.offset(-10,1,10),Blocks.CUT_COPPER);
        put(l,c.offset(-11,0,10),Blocks.CUT_COPPER);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)put(l,c.offset(10+dx,-1,10+dz),Blocks.SLIME_BLOCK);
        // Serpentine maze walls leave two-block-wide visible corridors.
        for(int x=2;x<=10;x++){put(l,c.offset(x,0,0),Blocks.PURPLE_STAINED_GLASS);put(l,c.offset(x,0,8),Blocks.PURPLE_STAINED_GLASS);}
        for(int x=4;x<=10;x++)put(l,c.offset(x,0,2),Blocks.PURPLE_STAINED_GLASS);
        for(int x=2;x<=8;x++)put(l,c.offset(x,0,4),Blocks.PURPLE_STAINED_GLASS);
        for(int x=4;x<=10;x++)put(l,c.offset(x,0,6),Blocks.PURPLE_STAINED_GLASS);
        for(int x=-1;x<=1;x++)put(l,c.offset(x,-1,12),x==0?Blocks.WHITE_CONCRETE:Blocks.BLACK_CONCRETE);
        Block[] routeColors={Blocks.LIGHT_BLUE_TERRACOTTA,Blocks.YELLOW_TERRACOTTA,Blocks.PURPLE_TERRACOTTA,Blocks.RED_TERRACOTTA,Blocks.LIME_TERRACOTTA};
        for(var leg:CircusRaceCourse.LEGS)if(leg.from().y==0&&leg.to().y==0){
            int steps=(int)Math.ceil(leg.from().distanceTo(leg.to())*3);
            for(int step=0;step<=steps;step++){var at=leg.from().lerp(leg.to(),step/(double)Math.max(1,steps));put(l,c.offset((int)Math.round(at.x),-1,(int)Math.round(at.z)),routeColors[leg.act()]);}
        }
        for(int x=-1;x<=1;x++)put(l,c.offset(x,-1,12),x==0?Blocks.WHITE_CONCRETE:Blocks.BLACK_CONCRETE);
        // Colored tent poles, lantern chains, banners, flower boxes and small concessions.
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6;int x=(int)Math.round(Math.cos(a)*27),z=(int)Math.round(Math.sin(a)*27);
            for(int y=0;y<=11;y++)put(l,c.offset(x,y,z),y%4==0?Blocks.GOLD_BLOCK:Blocks.DARK_OAK_LOG);
            put(l,c.offset(x,12,z),Blocks.SEA_LANTERN);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(dx*dx+dz*dz<=1)put(l,c.offset(x+dx,0,z+dz),Blocks.FLOWERING_AZALEA_LEAVES);
        }
        for(int x:new int[]{-22,22})for(int z:new int[]{-21,-7,7,21}){
            put(l,c.offset(x,0,z),Blocks.CHERRY_PLANKS);put(l,c.offset(x,1,z),Blocks.POTTED_PINK_TULIP);
            put(l,c.offset(x+(x<0?1:-1),0,z),Blocks.SEA_LANTERN);
        }
        for(int x=-4;x<=4;x++)for(int y=5;y<=8;y++)put(l,c.offset(x,y,29),Blocks.AIR);
        for(int x=-5;x<=5;x++)put(l,c.offset(x,9,29),Blocks.GOLD_BLOCK);
        for(int x=-4;x<=4;x++)put(l,c.offset(x,10,29),x%2==0?Blocks.SEA_LANTERN:Blocks.PURPLE_WOOL);
        // Close the decorative entrance; leaving is through the activity portal, not the void.
        for(int x=-4;x<=4;x++)for(int y=0;y<3;y++)put(l,c.offset(x,y,30),Blocks.DARK_OAK_FENCE);
    }
    static void removeBuild(ServerLevel l,BlockPos c){
        if(!l.getBlockState(c.below(3)).is(Blocks.LIME_GLAZED_TERRACOTTA)&&!l.getBlockState(c.below(3)).is(Blocks.CYAN_GLAZED_TERRACOTTA))return;
        for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)if(x*x+z*z<=33*33)
            for(int y=-3;y<=20;y++){BlockPos at=c.offset(x,y,z);if(!l.isEmptyBlock(at))put(l,at,Blocks.AIR);}
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){
        var found=new ArrayList<Entity>();
        for(var e:l.getEntitiesOfClass(Entity.class,new AABB(c).inflate(36,22,36),e->owned(e,s)))found.add(e);
        found.forEach(e->{e.ejectPassengers();e.discard();});
    }
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        var c=center(p,s);build(l,c);clean(l,s,c);var st=s.loop;
        st.circusVoiceUntil=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.MICE_CIRCUS));
        st.circusActAt=0;st.circusAct=0;st.circusSeed=p.getRandom().nextLong();st.circusSelected=-1;st.circusBetMouse=-1;st.circusBetState=0;
        st.circusWager=ItemStack.EMPTY;Arrays.fill(st.circusScores,0);
        st.circusPhase=0;st.circusPhaseUntil=0;st.circusBookieMouse=-1;st.circusGifted=false;
        prepare(p,s,l,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        LoopProps.hold(l,c);LoopProps.hold(l,c.south(24));build(l,c);
        ensureFather(p,s,l,c);ensureCrowd(s,l,c);ensureMice(s,l,c);ensureBookie(s,l,c);ensureBetProps(s,l,c);
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        var existing=l.getEntitiesOfClass(ParentEntity.class,new AABB(c).inflate(36),e->owned(e,s));
        if(!existing.isEmpty()){var dad=existing.get(0);dad.setPos(c.getX()+2.5,c.getY(),c.getZ()+20.5);dad.setYRot(90);dad.setYBodyRot(90);dad.setYHeadRot(90);for(int i=1;i<existing.size();i++)existing.get(i).discard();return;}
        var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null)return;
        var family=ParentAccess.family(p.server,s.owner);var dad=ModEntities.DAD.get().create(l);if(dad==null)return;
        dad.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));dad.setNoAi(true);mark(dad,s,-100);
        dad.getPersistentData().putUUID("RealmDoor",s.door);dad.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        dad.setPos(c.getX()+2.5,c.getY(),c.getZ()+20.5);dad.setYRot(90);dad.setYBodyRot(90);dad.setYHeadRot(90);l.addFreshEntity(dad);
    }
    private static void ensureCrowd(VisitData.Scene s,ServerLevel l,BlockPos c){
        for(int i=0;i<28;i++){
            int slot=100+i;if(!l.getEntitiesOfClass(Villager.class,new AABB(c).inflate(30,8,30),e->owned(e,s)&&e.getPersistentData().getInt(SLOT)==slot).isEmpty())continue;
            var v=EntityType.VILLAGER.create(l);if(v==null)continue;
            int side=i<14?-1:1,j=i%14;double x=side*(16+(j%3)*2),z=-18+(j/3)*8;
            v.setPos(c.getX()+x+.5,c.getY()+(j%3),c.getZ()+z+.5);v.setNoAi(true);v.setPersistenceRequired();
            v.setCustomName(Component.literal(VillageActivity.RESIDENT_NAME));v.setCustomNameVisible(false);
            v.setYRot(side<0?270:90);v.setYBodyRot(v.getYRot());v.setYHeadRot(v.getYRot());mark(v,s,slot);l.addFreshEntity(v);
        }
    }
    private static void ensureMice(VisitData.Scene s,ServerLevel l,BlockPos c){
        for(int i=0;i<NAMES.length;i++){
            int slot=i;if(mouse(l,s,c,i)!=null)continue;
            MouseEntity m=ModEntities.MOUSE.get().create(l);if(m==null)continue;
            m.circusStyle(i);m.setNoGravity(true);m.noPhysics=true;m.setPersistenceRequired();
            m.setCustomName(Component.literal(NAMES[i]));m.setCustomNameVisible(true);mark(m,s,slot);
            Vec3 at=CircusRaceCourse.position(0,i).add(Vec3.atBottomCenterOf(c));m.setPos(at);l.addFreshEntity(m);
        }
    }
    private static MouseEntity mouse(ServerLevel l,VisitData.Scene s,BlockPos c,int i){
        return l.getEntitiesOfClass(MouseEntity.class,new AABB(c).inflate(15,10,15),e->owned(e,s)&&e.getPersistentData().getInt(SLOT)==i).stream().findFirst().orElse(null);
    }
    private static void ensureBookie(VisitData.Scene s,ServerLevel l,BlockPos c){
        if(bookie(l,s,c)!=null)return;
        var e=ModEntities.FISHING_CONTEST_NPC.get().create(l);if(e==null)return;e.configure(27,false,true,99);
        e.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(ModItems.PERFECT_INGOT.get(),2));
        e.setNoAi(true);e.setCustomName(Component.literal("Button-Eyed Bookie"));e.setCustomNameVisible(true);
        e.getPersistentData().putBoolean("CoralineCircusBookie",true);mark(e,s,99);
        e.setPos(c.getX()-15.5,c.getY(),c.getZ()+12.5);e.setYRot(270);e.setYBodyRot(270);e.setYHeadRot(270);l.addFreshEntity(e);
    }
    private static FishingContestNpcEntity bookie(ServerLevel l,VisitData.Scene s,BlockPos c){
        return l.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(c).inflate(35),e->owned(e,s)).stream().findFirst().orElse(null);
    }
    private static void ensureBetProps(VisitData.Scene s,ServerLevel l,BlockPos c){
        var props=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(35),e->owned(e,s)&&(e.kind()==ScenePropEntity.CIRCUS_CHOICE||e.kind()==ScenePropEntity.CIRCUS_BET));
        if(s.loop.circusPhase!=3){props.forEach(Entity::discard);return;}
        for(int i=0;i<NAMES.length;i++){
            int slot=200+i;if(props.stream().anyMatch(e->e.slot==slot))continue;
            var prop=LoopProps.spawn(l,s,ScenePropEntity.CIRCUS_CHOICE,Vec3.atBottomCenterOf(c.offset(-4+i*2,0,17)),slot);
            prop.mode(i);prop.setCustomName(Component.literal("Right-click: "+NAMES[i]));prop.setCustomNameVisible(true);mark(prop,s,slot);
        }
    }
    private static void caption(ServerPlayer p,String speaker,String line,int ticks){VisitAudio.caption(p,speaker,line,ticks);}
    public static void interactBookie(ServerPlayer p,FishingContestNpcEntity e){
        var s=OtherMotherLoop.active(p);if(s==null||s.loop.activity!=OtherMotherActivities.Activity.MICE_CIRCUS||!owned(e,s)||!p.getUUID().equals(s.owner))return;
        if(s.loop.circusPhase==3)caption(p,"Button-Eyed Bookie","My two Perfect Ingots against yours. Pick one of the five mice!",100);
        else if(s.loop.circusPhase<3)p.displayClientMessage(Component.literal("Let Father finish his introduction."),true);
        else p.displayClientMessage(Component.literal("Watch the race — the wager is locked in."),true);
    }
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(prop.kind()!=ScenePropEntity.CIRCUS_CHOICE&&prop.kind()!=ScenePropEntity.CIRCUS_BET)return false;
        if(!p.getUUID().equals(s.owner)||!owned(prop,s)||s.loop.circusPhase!=3)return true;
        int selected=prop.mode();if(selected<0||selected>=NAMES.length)return true;
        int count=p.getInventory().items.stream().filter(i->i.is(ModItems.PERFECT_INGOT.get())).mapToInt(ItemStack::getCount).sum();
        if(count<2){p.displayClientMessage(Component.literal("Pick up your two souvenir Perfect Ingots first."),true);return true;}
        int left=2;for(var item:p.getInventory().items)if(item.is(ModItems.PERFECT_INGOT.get())){int take=Math.min(left,item.getCount());item.shrink(take);left-=take;if(left==0)break;}
        p.getInventory().setChanged();var st=s.loop;st.circusWager=new ItemStack(ModItems.PERFECT_INGOT.get(),2);
        st.circusBetMouse=selected;st.circusSelected=selected;st.circusBookieMouse=(selected+1+(int)Math.floorMod(CircusRaceCourse.hash(st.circusSeed),4))%5;
        st.circusBetState=2;st.circusPhase=4;st.circusPhaseUntil=OtherMotherLoop.now(p)+100;
        caption(p,"Button-Eyed Bookie","You picked "+NAMES[selected]+". I'll back "+NAMES[st.circusBookieMouse]+". Winner takes the stake; neither wins, money back.",100);
        ensureBetProps(s,p.serverLevel(),center(p,s));OtherMotherLoop.changed(p);return true;
    }
    /** 4 = own stake plus bookie's prize, 0 = lost stake, 2 = untouched stake returned. */
    static int payout(int playerMouse,int bookieMouse,int winner){return winner==playerMouse?4:winner==bookieMouse?0:2;}
    private static void settle(ServerPlayer p,VisitData.Scene s){
        var st=s.loop;int winner=CircusRaceCourse.winner(st.circusSeed);
        if(st.circusBetState==2&&!st.circusWager.isEmpty()){
            int returned=payout(st.circusBetMouse,st.circusBookieMouse,winner);
            st.circusWager=ItemStack.EMPTY;st.circusBetState=4;
            if(returned>0)ActivityTokens.give(p,new ItemStack(ModItems.PERFECT_INGOT.get(),returned));
            String result=winner==st.circusBetMouse?"Your mouse won! Your two ingots back, plus my two as your prize.":
                winner==st.circusBookieMouse?"My mouse won! I keep your two Perfect Ingots.":
                "Neither of our mice won. Here are your two Perfect Ingots back.";
            caption(p,"Button-Eyed Bookie",NAMES[winner]+" wins! "+result,180);
        }
        st.circusPhase=6;st.circusAct=ACTS.length;OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
        p.playNotifySound(SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.PLAYERS,.8F,1.1F);
        p.serverLevel().sendParticles(ParticleTypes.FIREWORK,cFor(s,p).getX()+.5,cFor(s,p).getY()+3,cFor(s,p).getZ()+12.5,40,2,2,2,.03);
    }
    private static BlockPos cFor(VisitData.Scene s,ServerPlayer p){return center(p,s);}
    private static void moveBookie(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var e=bookie(p.serverLevel(),s,c);if(e==null)return;
        Vec3 target=new Vec3(c.getX()-1.5,c.getY(),c.getZ()+20.5),delta=target.subtract(e.position());
        if(delta.length()>.08){Vec3 step=delta.normalize().scale(Math.min(.16,delta.length()));e.setPos(e.position().add(step));e.setDeltaMovement(Vec3.ZERO);
            float yaw=(float)Math.toDegrees(Math.atan2(-step.x,step.z));e.setYRot(yaw);e.setYBodyRot(yaw);e.setYHeadRot(yaw);
        }else{float yaw=(float)Math.toDegrees(Math.atan2(-(p.getX()-e.getX()),p.getZ()-e.getZ()));e.setYRot(yaw);e.setYBodyRot(yaw);e.setYHeadRot(yaw);}
    }
    private static void recoverVisitors(ServerPlayer p,BlockPos c){
        for(var visitor:p.serverLevel().players()){
            double r=Math.hypot(visitor.getX()-c.getX()-.5,visitor.getZ()-c.getZ()-.5);
            if(r<70&&(visitor.getY()<c.getY()-3||r>31)){
                visitor.stopRiding();visitor.teleportTo(p.serverLevel(),c.getX()+.5,c.getY()+.1,c.getZ()+20.5,visitor.getYRot(),visitor.getXRot());
                visitor.fallDistance=0;visitor.setDeltaMovement(Vec3.ZERO);
            }
        }
    }
    private static void animate(ServerPlayer p,VisitData.Scene s,BlockPos c,int elapsed){
        var st=s.loop;
        for(int i=0;i<NAMES.length;i++){
            var m=mouse(p.serverLevel(),s,c,i);if(m==null)continue;
            double progress=CircusRaceCourse.progress(st.circusSeed,i,elapsed);
            Vec3 local=CircusRaceCourse.position(progress,i),at=local.add(Vec3.atBottomCenterOf(c));
            if(progress>=1)at=Vec3.atBottomCenterOf(c).add((i-2)*.8,0,12);
            Vec3 delta=at.subtract(m.position());if(delta.horizontalDistanceSqr()>.000001){
                float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));m.setYRot(yaw);m.setYBodyRot(yaw);m.setYHeadRot(yaw);}
            m.setPos(at);m.setDeltaMovement(Vec3.ZERO);
            int act=CircusRaceCourse.legAt(progress).act();
            if(CircusRaceCourse.legAt(progress).arc()==6&&!m.getPersistentData().getBoolean("CircusCannonFired")){
                m.getPersistentData().putBoolean("CircusCannonFired",true);
                p.serverLevel().playSound(null,m.blockPosition(),SoundEvents.FIREWORK_ROCKET_LAUNCH,SoundSource.PLAYERS,.45F,1.3F);
                p.serverLevel().sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,12,.25,.25,.25,.025);
            }
            if(act==4&&!m.getPersistentData().getBoolean("CircusLanded")){
                m.getPersistentData().putBoolean("CircusLanded",true);
                p.serverLevel().playSound(null,m.blockPosition(),SoundEvents.SLIME_BLOCK_FALL,SoundSource.PLAYERS,.6F,1.3F);
            }
            if(st.circusScores[i]!=act+1){
                st.circusScores[i]=act+1;
                if(act==3)p.serverLevel().sendParticles(ParticleTypes.CLOUD,at.x,at.y+.2,at.z,8,.25,.15,.25,.03);
                if(i==st.circusBetMouse)p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.PLAYERS,.45F,1F+act*.12F);
            }
            if(act==3&&CircusRaceCourse.legAt(progress).arc()==6&&elapsed%4==0)
                p.serverLevel().sendParticles(ParticleTypes.CRIT,at.x,at.y+.2,at.z,2,.08,.08,.08,0);
        }
        if(elapsed%20==0){
            int leader=0;for(int i=1;i<NAMES.length;i++)if(CircusRaceCourse.progress(st.circusSeed,i,elapsed)>CircusRaceCourse.progress(st.circusSeed,leader,elapsed))leader=i;
            int winner=CircusRaceCourse.winner(st.circusSeed);if(elapsed>=CircusRaceCourse.finishTick(st.circusSeed,winner))leader=winner;
            int act=CircusRaceCourse.legAt(CircusRaceCourse.progress(st.circusSeed,st.circusBetMouse,elapsed)).act();st.circusAct=act;
            p.displayClientMessage(Component.literal(((1600-elapsed+19)/20)+"s  |  "+ACTS[act]+"  |  Leader: "+NAMES[leader]+"  |  Your mouse: "+NAMES[st.circusBetMouse]),true);
        }
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var st=s.loop;long now=OtherMotherLoop.now(p);recoverVisitors(p,c);
        if(p.tickCount%80==0)prepare(p,s,p.serverLevel(),c);
        if(st.circusPhase>=6){if(p.tickCount%40==0)ActivityWorld.ensureExit(p,s);return;}
        if(now<st.circusVoiceUntil)return;
        if(st.circusPhase==0){
            if(!st.circusGifted){st.circusGifted=true;ActivityTokens.give(p,new ItemStack(ModItems.PERFECT_INGOT.get(),2));}
            caption(p,"Other Father","A show souvenir for you: two Perfect Ingots!",90);
            st.circusPhase=1;st.circusPhaseUntil=now+90;OtherMotherLoop.changed(p);return;
        }
        if(st.circusPhase==1){if(now<st.circusPhaseUntil)return;st.circusPhase=2;st.circusPhaseUntil=now+120;OtherMotherLoop.changed(p);}
        if(st.circusPhase==2){moveBookie(p,s,c);if(now<st.circusPhaseUntil)return;
            st.circusPhase=3;caption(p,"Button-Eyed Bookie","I got two Perfect Ingots too. Wager yours against mine? Choose one of these five mice.",140);
            ensureBetProps(s,p.serverLevel(),c);OtherMotherLoop.changed(p);return;
        }
        if(st.circusPhase==3){
            if(p.tickCount%10==0){p.displayClientMessage(Component.literal("Right-click a mouse display to place your two-ingot bet.").withStyle(net.minecraft.ChatFormatting.YELLOW,net.minecraft.ChatFormatting.BOLD),true);
                for(int i=0;i<5;i++)p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,c.getX()-3.5+i*2,c.getY()+1.4,c.getZ()+17.5,1,.2,.1,.2,0);}
            return;
        }
        if(st.circusPhase==4){
            long left=st.circusPhaseUntil-now;
            if(left>0){if(left<=60&&left%20==0){p.displayClientMessage(Component.literal("Race starts in "+left/20+"..."),true);p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(),SoundSource.PLAYERS,.7F,1.1F);}return;}
            st.circusPhase=5;st.circusActAt=now+CircusRaceCourse.RACE_TICKS;
            VisitAudio.play(p,"circus_race",true);p.playNotifySound(SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,.65F,1.4F);OtherMotherLoop.changed(p);
        }
        int elapsed=(int)Math.max(0,Math.min(CircusRaceCourse.RACE_TICKS,CircusRaceCourse.RACE_TICKS-(st.circusActAt-now)));
        animate(p,s,c,elapsed);
        var spectator=bookie(p.serverLevel(),s,c);
        var backed=mouse(p.serverLevel(),s,c,st.circusBookieMouse);
        if(spectator!=null&&backed!=null){
            var delta=backed.position().add(0,.25,0).subtract(spectator.getEyePosition());
            float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
            float turn=net.minecraft.util.Mth.approachDegrees(spectator.getYHeadRot(),yaw,6);
            spectator.setYRot(turn);spectator.setYBodyRot(turn);spectator.setYHeadRot(turn);
            spectator.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
        }
        if(elapsed>=CircusRaceCourse.RACE_TICKS)settle(p,s);
    }
    static void refund(ServerPlayer p,VisitData.Scene s){
        if(s.loop.circusBetState==2&&!s.loop.circusWager.isEmpty()){
            var refund=s.loop.circusWager.copy();s.loop.circusWager=ItemStack.EMPTY;s.loop.circusBetState=3;
            ActivityTokens.give(p,refund);OtherMotherLoop.changed(p);
        }
    }
}
