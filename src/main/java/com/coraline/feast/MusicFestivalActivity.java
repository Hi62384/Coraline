package com.coraline.feast;

import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Festival gameplay inside the existing owner/portal/reward lifecycle. */
public final class MusicFestivalActivity {
    private static final Map<UUID,Performance> LIVE=new HashMap<>();
    private static final class Performance {
        final int track;final List<FestivalChart.Note> chart;final int[] resolved;final FestivalChart.Score score=new FestivalChart.Score();
        final List<FishingContestNpcEntity> audience=new ArrayList<>();final List<Tip> tips=new ArrayList<>();
        int grade;long gradeAt,lastPress=-100,nextDonation;
        Performance(int track){this.track=track;chart=FestivalChart.track(track).notes();resolved=new int[chart.size()];}
    }
    private record Tip(ScenePropEntity prop,Vec3 from,Vec3 to,long born){}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).south(600);}
    static BlockPos exitPos(BlockPos c){return c.south(9);}
    static CompoundTag state(VisitData.Scene s){return s.loop.festival;}
    static boolean owns(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY&&s.loop.activity==OtherMotherActivities.Activity.MUSIC_FESTIVAL&&ActivityWorld.isActivity(p.level());}
    static boolean exitReady(VisitData.Scene s){return state(s).getInt("Phase")==1||state(s).getInt("Phase")>=3;}
    static Vec3 station(BlockPos c){return Vec3.atBottomCenterOf(c.offset(0,1,-26));}
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        clean(l,s,center(p,s));s.loop.festival=new CompoundTag();state(s).putLong("VoiceUntil",OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(s.loop.activity)));state(s).putLong("Seed",p.getRandom().nextLong());int previous=p.getPersistentData().contains("FestivalLastTrack")?p.getPersistentData().getInt("FestivalLastTrack"):-1;int track=FestivalChart.choose(state(s).getLong("Seed"),previous);state(s).putInt("Track",track);p.getPersistentData().putInt("FestivalLastTrack",track);prepare(p,s,l,center(p,s));OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        LoopProps.hold(l,c);FestivalGrounds.build(l,c);ActivityScenery.father(p,s,l,c.east(2));
        if(LoopProps.nearby(l,s,c.north(25)).stream().noneMatch(e->e.kind()==ScenePropEntity.FESTIVAL_CONSOLE)){
            LoopProps.spawn(l,s,ScenePropEntity.FESTIVAL_CONSOLE,Vec3.atBottomCenterOf(c.offset(0,1,-24)),700);
            var pool=LoopProps.spawn(l,s,ScenePropEntity.GIFT,Vec3.atBottomCenterOf(c.offset(4,1,-23)),701);pool.mode(1);
            for(int[] at:new int[][]{{0,-62},{-30,-34},{30,-61}}){var piano=LoopProps.spawn(l,s,ScenePropEntity.PIANO,Vec3.atBottomCenterOf(c.offset(at[0],1,at[1])),702);piano.setYRot(180);
                var performer=npc(l,s,80+Math.abs(at[0]));performer.setPos(c.getX()+at[0]+.5,c.getY()+1,c.getZ()+at[1]-.8);performer.setBaking(true);l.addFreshEntity(performer);
            }
            for(int i=0;i<18;i++){
                var npc=npc(l,s,i+60);int zone=i/6;int x=new int[]{0,-30,30}[zone]+(i%3-1)*3,z=new int[]{-57,-29,-56}[zone]+i%2*3;
                npc.setPos(c.getX()+x+.5,c.getY(),c.getZ()+z+.5);l.addFreshEntity(npc);
            }
        }
    }
    private static FishingContestNpcEntity npc(ServerLevel l,VisitData.Scene s,int slot){
        var npc=ModEntities.FISHING_CONTEST_NPC.get().create(l);npc.configure(slot*7,slot%2==0,false,slot);npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);npc.setNoAi(true);npc.setSilent(true);npc.setNoGravity(true);ActivityScenery.mark(npc,s);return npc;
    }
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(prop.kind()!=ScenePropEntity.FESTIVAL_CONSOLE)return false;
        if(!owns(p,s)||state(s).getInt("Phase")!=1)return true;
        state(s).putInt("Phase",2);state(s).putLong("Start",OtherMotherLoop.now(p)+60);state(s).put("Pool",new ListTag());
        var run=new Performance(state(s).getInt("Track"));LIVE.put(s.owner,run);run.nextDonation=200;
        var stage=station(center(p,s));
        p.connection.teleport(stage.x,stage.y,stage.z,0,5);p.setDeltaMovement(Vec3.ZERO);
        ActivityScenery.title(p,"MUSIC FESTIVAL","Play the notes. Build your crowd.");
        sync(p,s,run,-60);OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);return true;
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        if(!owns(p,s))return;var n=state(s);int phase=n.getInt("Phase");long now=OtherMotherLoop.now(p);
        if(p.tickCount%20==0)ActivityScenery.clearWeather(p);
        if(p.getY()<c.getY()-3||!FestivalGrounds.bounds(c).contains(p.position()))ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));
        if(phase==0&&now>=n.getLong("VoiceUntil")){n.putInt("Phase",1);ActivityWorld.ensureExit(p,s);OtherMotherLoop.changed(p);phase=1;}
        if(phase==1){
            if(p.tickCount%20==0){p.displayClientMessage(Component.literal(FestivalChart.track(n.getInt("Track")).title()+" • "+String.format("%d:%02d",FestivalChart.track(n.getInt("Track")).duration()/1200,(FestivalChart.track(n.getInt("Track")).duration()/20)%60)+" • Right-click your stage instrument"),true);p.serverLevel().sendParticles(ParticleTypes.NOTE,c.getX()+.5,c.getY()+3,c.getZ()-24,3,.8,.2,.2,0);}
            return;
        }
        if(phase!=2)return;
        var run=LIVE.get(s.owner);if(run==null){abort(p,s);return;} // saved mid-run: no duplicate reward or stranded lock
        int elapsed=(int)(now-n.getLong("Start"));
        if(now-run.lastPress==3)for(var prop:LoopProps.nearby(p.serverLevel(),s,c.north(24)))if(prop.kind()==ScenePropEntity.FESTIVAL_CONSOLE)prop.mode(0);
        if(p.distanceToSqr(station(c))>.3)ActivityScenery.teleport(p,station(c));p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        if(elapsed>=FestivalChart.track(run.track).duration()){finish(p,s,c,run);return;}
        for(int i=0;i<run.chart.size();i++)if(run.resolved[i]==0&&elapsed>run.chart.get(i).tick()+FestivalChart.WINDOW+2){run.resolved[i]=3;run.score.miss(false);run.grade=3;run.gradeAt=now;}
        crowd(p,s,c,run,elapsed);
        if(elapsed>=0&&elapsed>=run.nextDonation){if(run.score.crowd>0)donate(p,s,c,run,now,elapsed);run.nextDonation=elapsed+Math.max(35,200-run.score.crowd*3);}
        for(var iterator=run.tips.iterator();iterator.hasNext();){var tip=iterator.next();double t=(now-tip.born)/20.0;if(t>=1){tip.prop.discard();iterator.remove();continue;}
            tip.prop.setPos(tip.from.lerp(tip.to,t).add(0,2*Math.sin(Math.PI*t),0));}
        if(p.tickCount%4==0)sync(p,s,run,elapsed);
        if(elapsed<0&&elapsed%20==0)p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(),SoundSource.PLAYERS,.4F,1.2F);
    }
    public static void press(ServerPlayer p,int generation,int lane){
        var s=OtherMotherLoop.active(p);if(s==null||!owns(p,s)||s.loop.generation!=generation||state(s).getInt("Phase")!=2||lane<0||lane>3)return;
        var run=LIVE.get(s.owner);if(run==null)return;long now=OtherMotherLoop.now(p);int elapsed=(int)(now-state(s).getLong("Start"));if(elapsed<0||elapsed>=FestivalChart.track(run.track).duration())return;
        // Multiple different buttons in one tick are wrong inputs, not four free hit attempts.
        if(now==run.lastPress){run.score.miss(true);run.grade=3;run.gradeAt=now;return;}run.lastPress=now;
        int found=-1,best=99;for(int i=0;i<run.chart.size();i++){var note=run.chart.get(i);int error=Math.abs(note.tick()-(elapsed-1));if(run.resolved[i]==0&&note.lane()==lane&&error<=FestivalChart.WINDOW&&error<best){found=i;best=error;}}
        if(found<0){run.score.miss(true);run.grade=3;run.gradeAt=now;int nearest=-1,delta=999;for(int i=0;i<run.chart.size();i++){int d=Math.abs(run.chart.get(i).tick()-elapsed);if(run.resolved[i]==0&&d<delta){delta=d;nearest=i;}}
            if(nearest>=0){playNote(p,run.chart.get(nearest),true);if(delta<=FestivalChart.WINDOW+2)run.resolved[nearest]=3;}return;}
        run.resolved[found]=best<=2?1:2;run.score.hit(best<=2);run.grade=run.resolved[found];run.gradeAt=now;
        var c=center(p,s);playNote(p,run.chart.get(found),false);
        p.serverLevel().sendParticles(ParticleTypes.NOTE,c.getX()-.5+lane*.65,c.getY()+2.3,c.getZ()-24,2,0,.1,0,0);
        for(var prop:LoopProps.nearby(p.serverLevel(),s,c.north(24)))if(prop.kind()==ScenePropEntity.FESTIVAL_CONSOLE)prop.mode(1+lane);
        if(run.score.streak%10==0){p.playNotifySound(SoundEvents.VILLAGER_CELEBRATE,SoundSource.NEUTRAL,.45F,1.1F);p.serverLevel().sendParticles(ParticleTypes.FIREWORK,c.getX(),c.getY()+7,c.getZ()-25,12,5,1,2,.02);}
    }
    private static void playNote(ServerPlayer p,FestivalChart.Note note,boolean wrong){
        // The prompt performs the source chord, not an arbitrary lane pitch.
        for(int midi:note.tones())tone(p,midi+(wrong?1:0),midi==note.midi()?.85F:.36F);
    }
    private static void tone(ServerPlayer p,int midi,float volume){
        int root=midi<54?42:midi>78?90:66;
        var sound=midi<54?com.coraline.registry.ModSounds.FESTIVAL_HARP_LOW:midi>78?com.coraline.registry.ModSounds.FESTIVAL_HARP_HIGH:com.coraline.registry.ModSounds.FESTIVAL_HARP;
        p.playNotifySound(sound.get(),SoundSource.RECORDS,volume,(float)Math.pow(2,(midi-root)/12.0));
    }
    private static void crowd(ServerPlayer p,VisitData.Scene s,BlockPos c,Performance run,int elapsed){
        var l=p.serverLevel();if(p.tickCount%40==0||run.audience.isEmpty()){
            run.audience.removeIf(e->!e.isAlive());
            for(int i=0;i<48;i++){final int slot=i;if(run.audience.stream().anyMatch(e->e.slot()==slot))continue;
                var npc=npc(l,s,i);npc.setPos(Vec3.atBottomCenterOf(c).add((i%2==0?-1:1)*(12+i%4),0,-8-(i/8)*2));l.addFreshEntity(npc);run.audience.add(npc);}
        }
        for(var npc:run.audience){int i=npc.slot();boolean attending=i<run.score.crowd;
            Vec3 target=Vec3.atBottomCenterOf(c).add(attending?(i%8-3.5)*1.2:(i%2==0?-1:1)*(13+i%4),0,attending?-19+(i/8)*1.5:-8-(i/8)*2);
            Vec3 difference=target.subtract(npc.position());double d=difference.horizontalDistance();
            if(d>.13){Vec3 step=new Vec3(difference.x,0,difference.z).normalize().scale(.13);npc.setPos(npc.position().add(step));float yaw=(float)Math.toDegrees(Math.atan2(-step.x,step.z));npc.setYRot(yaw);npc.setYHeadRot(yaw);npc.setYBodyRot(yaw);}
            else {npc.setYRot(attending?180:0);npc.setYBodyRot(attending?180:0);npc.setYHeadRot((attending?180:0)+(float)Math.sin((elapsed+i*5)*.08)*12);if(attending&&run.score.streak>=5)npc.setPos(target.add(0,Math.max(0,Math.sin((elapsed+i*2)*Math.PI/10))*.12,0));}
            npc.setDeltaMovement(Vec3.ZERO);if(attending&&run.score.streak>=10&&p.tickCount%40==i%40)npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
    private static void donate(ServerPlayer p,VisitData.Scene s,BlockPos c,Performance run,long now,int elapsed){
        int tier=run.score.tier();if(run.score.accuracy()<55)tier=Math.min(tier,1);var rng=p.getRandom();ItemStack prize;
        if(tier==4&&run.score.streak>=30&&elapsed>FestivalChart.track(run.track).duration()*3/4&&rng.nextInt(12)==0)prize=new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get());
        else if(tier>=3&&rng.nextInt(4)==0)prize=new ItemStack(Items.DIAMOND);
        else if(tier>=2&&rng.nextBoolean())prize=new ItemStack(Items.GOLD_INGOT);
        else if(tier>=1&&rng.nextBoolean())prize=new ItemStack(Items.EMERALD);
        else prize=FeastContent.serving(rng,rng.nextInt(4));
        var list=state(s).getList("Pool",Tag.TAG_COMPOUND);list.add(prize.save(new CompoundTag()));state(s).put("Pool",list);OtherMotherLoop.changed(p);
        var members=run.audience.stream().filter(e->e.slot()<run.score.crowd).toList();if(!members.isEmpty()){
            var donor=members.get(rng.nextInt(members.size()));var from=donor.position().add(0,1.2,0);var prop=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.FESTIVAL_TIP,from,703);prop.food(0,prize.copy());
            run.tips.add(new Tip(prop,from,Vec3.atBottomCenterOf(c.offset(4,1,-23)).add(0,.4,0),now));donor.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        for(var prop:LoopProps.nearby(p.serverLevel(),s,c.offset(4,1,-23)))if(prop.slot==701){var items=new ArrayList<ItemStack>();for(int i=Math.max(0,list.size()-9);i<list.size();i++)items.add(ItemStack.of(list.getCompound(i)));prop.treasure(items);}
    }
    private static void sync(ServerPlayer p,VisitData.Scene s,Performance run,int elapsed){FestivalNetwork.send(p,new FestivalNetwork.State(s.loop.generation,elapsed,run.track,run.score.crowd,run.score.streak,run.score.accuracy(),OtherMotherLoop.now(p)-run.gradeAt<12?run.grade:0,FestivalChart.pack(run.chart),run.resolved));}
    private static void finish(ServerPlayer p,VisitData.Scene s,BlockPos c,Performance run){
        if(state(s).getInt("Phase")!=2)return;state(s).putInt("Phase",3);var pool=state(s).getList("Pool",Tag.TAG_COMPOUND).copy();state(s).remove("Pool");OtherMotherLoop.changed(p);
        int total=0;for(var item:pool){var stack=ItemStack.of((CompoundTag)item);total+=stack.getCount();ActivityTokens.give(p,stack);}
        for(var tip:run.tips)tip.prop.discard();LIVE.remove(s.owner);FestivalNetwork.clear(p);p.displayClientMessage(Component.empty(),true);
        ActivityScenery.title(p,"BRAVO!","Crowd "+run.score.crowd+" • "+run.score.accuracy()+"% • "+total+" gifts");
        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,.5F,1.1F);ActivityWorld.ensureExit(p,s);
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){LIVE.remove(s.owner);ActivityScenery.clean(l,s,FestivalGrounds.bounds(c).inflate(5));}
    static void removeBuild(ServerLevel l,BlockPos c){ActivityScenery.erase(l,FestivalGrounds.bounds(c),c.below(2));}
    public static void abort(ServerPlayer p,VisitData.Scene s){if(s.loop.activity!=OtherMotherActivities.Activity.MUSIC_FESTIVAL||!s.owner.equals(p.getUUID()))return;LIVE.remove(s.owner);state(s).remove("Pool");state(s).putInt("Phase",3);FestivalNetwork.clear(p);OtherMotherLoop.changed(p);if(ActivityWorld.isActivity(p.level()))ActivityWorld.ensureExit(p,s);}
    private MusicFestivalActivity(){}
}
