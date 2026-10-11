package com.coraline.feast;

import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.*;

public final class EndCityActivity {
    // 0 intro, 1 trapped, 2 escaped/bridge, 3 vault, 4 claimed, 5 aborted.
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).offset(600,0,600);}
    static CompoundTag state(VisitData.Scene s){return s.loop.endCity;}
    static boolean owns(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY&&s.loop.activity==OtherMotherActivities.Activity.OTHER_END_CITY&&ActivityWorld.isActivity(p.level());}
    static boolean exitReady(VisitData.Scene s){return state(s).getInt("Phase")>=4;}
    static BlockPos exitPos(BlockPos c,VisitData.Scene s){return state(s).getInt("Phase")==4?c.offset(0,40,-263):c.south(6);}
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){clean(l,s,center(p,s));s.loop.endCity=new CompoundTag();state(s).putLong("VoiceUntil",OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(s.loop.activity)));state(s).putLong("Seed",p.getRandom().nextLong());prepare(p,s,l,center(p,s));OtherMotherLoop.changed(p);}
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){LoopProps.hold(l,c);EndCityCourse.build(l,c);ActivityScenery.father(p,s,l,c.east(2));}
    private static void issue(ServerPlayer p,VisitData.Scene s){
        var n=state(s);if(!n.getBoolean("Borrowed")){int slot=p.getInventory().selected;n.putInt("Slot",slot);n.put("Stored",p.getInventory().getItem(slot).save(new CompoundTag()));n.putBoolean("Borrowed",true);p.getInventory().setItem(slot,ItemStack.EMPTY);}
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(canEat(p,p.getInventory().getItem(i)))return;
        var fruit=new ItemStack(ModItems.ACTIVITY_CHORUS_FRUIT.get());fruit.getOrCreateTag().putUUID("Owner",s.owner);fruit.getOrCreateTag().putInt("Generation",s.loop.generation);
        int slot=n.getInt("Slot");if(p.getInventory().getItem(slot).isEmpty())p.getInventory().setItem(slot,fruit);else ActivityTokens.give(p,fruit);p.getInventory().selected=slot;p.getInventory().setChanged();
    }
    static void restore(ServerPlayer p,VisitData.Scene s){
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ModItems.ACTIVITY_CHORUS_FRUIT.get()))p.getInventory().setItem(i,ItemStack.EMPTY);
        var n=state(s);if(n.getBoolean("Borrowed")){var stored=ItemStack.of(n.getCompound("Stored"));n.putBoolean("Borrowed",false);n.remove("Stored");if(!stored.isEmpty())ActivityTokens.give(p,stored);OtherMotherLoop.changed(p);}
    }
    private static void beginRoom(ServerPlayer p,VisitData.Scene s,BlockPos c,int room){
        var n=state(s);n.putInt("Room",room);n.putInt("Phase",1);n.putInt("Stage",0);n.putInt("Failures",0);n.putInt("Rolls",0);n.putInt("Rune",0);n.putLong("RoomStarted",OtherMotherLoop.now(p));
        var b=EndCityCourse.room(c,room);for(var d:EndCityCourse.candidates(room))if(!EndCityCourse.safe(p.serverLevel(),b,d)){abort(p,s);return;}
        ActivityScenery.teleport(p,Vec3.atBottomCenterOf(b));issue(p,s);OtherMotherLoop.changed(p);
    }
    public static boolean canEat(ServerPlayer p,ItemStack stack){var s=OtherMotherLoop.active(p);return s!=null&&owns(p,s)&&state(s).getInt("Phase")==1&&stack.is(ModItems.ACTIVITY_CHORUS_FRUIT.get())&&stack.hasTag()&&stack.getTag().hasUUID("Owner")&&s.owner.equals(stack.getTag().getUUID("Owner"))&&stack.getTag().getInt("Generation")==s.loop.generation;}
    public static void eat(ServerPlayer p,ItemStack stack){
        if(!canEat(p,stack))return;var s=OtherMotherLoop.active(p);var n=state(s);long now=OtherMotherLoop.now(p);if(now<n.getLong("NextEat"))return;n.putLong("NextEat",now+20);p.getCooldowns().addCooldown(stack.getItem(),20);
        int room=n.getInt("Room"),stage=n.getInt("Stage"),rolls=n.getInt("Rolls");var c=center(p,s);var b=EndCityCourse.room(c,room);
        var route=EndCityChallenge.route(n.getLong("Seed"),room,stage);
        if(n.getInt("Rune")<route.size()||p.position().distanceToSqr(Vec3.atBottomCenterOf(b))>2.25){
            p.displayClientMessage(Component.literal("Activate the glowing runes, then eat fruit on the central gold pad."),true);return;
        }
        // Completing a physical circuit earns a reliable teleport, never a random lucky roll.
        var d=EndCityCourse.candidates(room).stream().filter(v->v.stage()==stage+1).findFirst().orElseThrow();
        if(!EndCityCourse.safe(p.serverLevel(),b,d)){abort(p,s);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));return;}
        var from=p.position();var to=d.vector().add(Vec3.atLowerCornerOf(b));n.putInt("Rolls",rolls+1);n.putInt("Rune",0);n.putInt("Stage",d.stage());
        for(int i=0;i<=10;i++){var at=from.lerp(to,i/10.0);p.serverLevel().sendParticles(ParticleTypes.PORTAL,at.x,at.y+1,at.z,4,.2,.3,.2,.05);}
        ActivityScenery.teleport(p,to);p.playNotifySound(SoundEvents.CHORUS_FRUIT_TELEPORT,SoundSource.PLAYERS,.8F,1);
        if(d.stage()==EndCityCourse.stages(room)){
            n.putInt("Phase",2);ActivityScenery.title(p,"ESCAPED","Follow the lit bridge to the next tower.");p.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.7F,1.4F);
            if(room==4){n.putInt("Phase",3);restore(p,s);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.offset(0,40,-266)));ActivityScenery.title(p,"END CITY ESCAPED","Claim your treasure aboard the dream ship.");}
        }OtherMotherLoop.changed(p);
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(!owns(p,s))return;var n=state(s);int phase=n.getInt("Phase"),room=n.getInt("Room");p.fallDistance=0;long now=OtherMotherLoop.now(p);
        if(p.tickCount%20==0)ActivityScenery.clearWeather(p);
        if(phase==0){if(p.getY()<c.getY()-2)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));if(now>=n.getLong("VoiceUntil")){n.putLong("Deadline",now+EndCityChallenge.DURATION);n.putInt("Hearts",EndCityChallenge.HEARTS);beginRoom(p,s,c,0);ActivityScenery.title(p,"OTHER END CITY","Walk onto each white light. Then eat fruit on the gold pad.");}return;}
        if(p.tickCount%5==0)ExpeditionNetwork.end(p,n.getInt("Hearts"),room,phase,EndCityCourse.room(c,room),now-n.getLong("RoomStarted"));
        if(phase>=4){if(p.getY()<c.getY()-2)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(exitPos(c,s).north(3)));return;}
        if(phase<3&&now>=n.getLong("Deadline")){fail(p,s,"Time ran out.");return;}
        var b=EndCityCourse.room(c,room);
        if(phase==1){
            int stage=n.getInt("Stage");var local=p.position().subtract(Vec3.atLowerCornerOf(b));
            // Only the validated consumption transaction can advance an escape; fly/pearl bypasses recover.
            if(!EndCityCourse.bounds(c).contains(p.position())||local.y< -1||local.y>11+room||Math.abs(local.x)>EndCityCourse.radius(room)||Math.abs(local.z)>EndCityCourse.radius(room)){
                hurt(p,s,now);if(n.getInt("Phase")>=4)return;ActivityScenery.teleport(p,Vec3.atBottomCenterOf(b));
            }
            if(p.tickCount%40==0)issue(p,s);
            var route=EndCityChallenge.route(n.getLong("Seed"),room,stage);int rune=n.getInt("Rune");
            var target=rune<route.size()?b.offset(route.get(rune)):b;
            if(rune<route.size()&&p.position().distanceToSqr(Vec3.atBottomCenterOf(target))<1.2){
                n.putInt("Rune",++rune);OtherMotherLoop.changed(p);
                p.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.6F,1+rune*.15F);
            }
            if(p.tickCount%5==0)p.serverLevel().sendParticles(ParticleTypes.END_ROD,target.getX()+.5,target.getY()+.35,target.getZ()+.5,6,.2,.3,.2,.01);
            long elapsed=now-n.getLong("RoomStarted");
            boolean firing=EndCityChallenge.firing(elapsed,room);
            if(elapsed%EndCityChallenge.cycle(room)==0)p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.get(),SoundSource.PLAYERS,.45F,.6F);
            if(firing&&EndCityChallenge.geometry(b,elapsed,room).intersects(p.getBoundingBox())){hurt(p,s,now);if(n.getInt("Phase")>=4)return;}
        }else if(phase==2){
            if(p.getY()<b.getY()-2){hurt(p,s,now);if(n.getInt("Phase")>=4)return;ActivityScenery.teleport(p,Vec3.atBottomCenterOf(b.offset(EndCityCourse.radius(room)+3,0,0)));}
            if(p.getZ()<b.getZ()-34&&p.getX()>c.getX()+19&&p.getX()<c.getX()+25)beginRoom(p,s,c,room+1);
        }else if(phase==3){
            if(p.getY()<c.getY()+38)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.offset(0,40,-266)));
            var at=c.offset(0,40,-273);if(p.tickCount%20==0&&LoopProps.nearby(p.serverLevel(),s,at).stream().noneMatch(e->e.slot==720)){
                var prize=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.PRIZE_PLATE,Vec3.atBottomCenterOf(at),720);prize.food(0,new ItemStack(Items.ENDER_EYE));prize.setCustomName(Component.literal("Claim End City treasure"));prize.setCustomNameVisible(true);
            }
        }
        if(p.tickCount%20==0)p.displayClientMessage(Component.literal(phase==3?"Claim your End City treasure":"Tower "+(room+1)+"/5 • "+String.format("%d:%02d",Math.max(0,(n.getLong("Deadline")-now)/1200),Math.max(0,(n.getLong("Deadline")-now)/20)%60)+(phase==2?" • Follow the bridge":n.getInt("Rune")>=EndCityChallenge.route(n.getLong("Seed"),room,n.getInt("Stage")).size()?" • Stand on GOLD and hold right-click to eat fruit":" • Walk onto WHITE light ("+(n.getInt("Rune")+1)+"/"+EndCityChallenge.route(n.getLong("Seed"),room,n.getInt("Stage")).size()+")")),true);
    }
    private static void hurt(ServerPlayer p,VisitData.Scene s,long now){
        var n=state(s);if(now<n.getLong("InvulnerableUntil"))return;
        n.putLong("InvulnerableUntil",now+40);n.putInt("Hearts",Math.max(0,n.getInt("Hearts")-1));
        p.playNotifySound(SoundEvents.SHULKER_HURT,SoundSource.PLAYERS,.65F,.8F);
        OtherMotherLoop.changed(p);
        if(n.getInt("Hearts")==0)fail(p,s,"All three hearts lost.");
    }
    private static void fail(ServerPlayer p,VisitData.Scene s,String reason){
        abort(p,s);ActivityScenery.title(p,"END CITY LOST",reason+" No treasure this run.");
    }
    static boolean takePrize(VisitData.Scene s,UUID owner){if(!s.owner.equals(owner)||state(s).getInt("Phase")!=3)return false;state(s).putInt("Phase",4);return true;}
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){if(prop.slot!=720)return false;if(!owns(p,s)||!takePrize(s,p.getUUID()))return true;OtherMotherLoop.changed(p);prop.discard();DesertTempleActivity.jackpot(p,s);ActivityWorld.ensureExit(p,s);return true;}
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){ActivityScenery.clean(l,s,EndCityCourse.bounds(c).inflate(3));}
    static void removeBuild(ServerLevel l,BlockPos c){ActivityScenery.erase(l,EndCityCourse.bounds(c),c.below(2));}
    public static void abort(ServerPlayer p,VisitData.Scene s){if(s.loop.activity!=OtherMotherActivities.Activity.OTHER_END_CITY||!s.owner.equals(p.getUUID()))return;restore(p,s);state(s).putInt("Phase",5);ExpeditionNetwork.end(p,0,0,5,center(p,s),0);OtherMotherLoop.changed(p);if(ActivityWorld.isActivity(p.level())){ActivityScenery.teleport(p,Vec3.atBottomCenterOf(center(p,s)));ActivityWorld.ensureExit(p,s);}}
    private EndCityActivity(){}
}
