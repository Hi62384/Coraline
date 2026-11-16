package com.coraline.wybie;

import com.coraline.entity.CharacterEntity;
import com.coraline.feast.VisitAudio;
import com.coraline.feast.VisitData;
import com.coraline.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Server-owned A–L performance; saved progress pauses when its owner is away. */
public final class WybieVisitorEntity extends CharacterEntity {
    public static final int WAIT=0,A=1,SEARCH=2,B=3,C=4,D=5,E=6,F=7,G=8,H=9,I=10,THROW=11,J=12,K=13,L=14;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(WybieVisitorEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIME=SynchedEntityData.defineId(WybieVisitorEntity.class,EntityDataSerializers.INT);
    public UUID owner,door;
    private boolean cameraGiven,flashDone,voiceDone,audioStarted,paused;
    private Vec3 escape=Vec3.ZERO;
    public WybieVisitorEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setInvulnerable(true);setPersistenceRequired();}
    @Override public String characterId(){return "wybie";}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,WAIT);entityData.define(TIME,0);}
    public int phase(){return entityData.get(PHASE);}
    public int elapsed(){return entityData.get(TIME);}
    public void bind(ServerPlayer p,UUID door){owner=p.getUUID();this.door=door;watch(p);}
    public void startMusic(ServerPlayer p){VisitAudio.play(p,"wybie_visit_music",true);}
    public static String line(int phase){return switch(phase){case A->"a";case B->"b";case C->"c";case D->"d";case E->"e";case F->"f";case G->"g";case H->"h";case I->"i";case J->"j";case K->"k";case L->"l";default->"";};}
    public String sound(){return "wybie_visit_"+line(phase());}
    public void voiceFinished(String name){if(name.equals(sound()))voiceDone=true;}
    public boolean hasSlug(){return !com.coraline.feast.CanonicalRealWybie.tagged(this)&&phase()>=C&&phase()<THROW&&(phase()!=C||elapsed()>=20);}
    public boolean biking(){return !com.coraline.feast.CanonicalRealWybie.tagged(this)&&phase()==L&&elapsed()>=20;}
    public boolean talking(){return !com.coraline.feast.CanonicalRealWybie.tagged(this)&&!line(phase()).isEmpty();}
    private void begin(int phase,ServerPlayer p){
        entityData.set(PHASE,phase);entityData.set(TIME,0);flashDone=false;voiceDone=false;audioStarted=true;
        if(!line(phase).isEmpty())VisitAudio.play(p,sound(),false);
        if(phase==L){Vec3 away=position().subtract(p.position()).multiply(1,0,1).normalize();if(away.lengthSqr()<.01)away=new Vec3(1,0,0);escape=position().add(away.scale(24));}
    }
    @Override public void tick(){
        super.tick();if(com.coraline.feast.CanonicalRealWybie.tagged(this))return;VisitGrounding.lift(this);if(level().isClientSide||owner==null)return;
        if(getPersistentData().hasUUID("ShowActorOwner"))return;
        var data=VisitData.get(getServer());var scene=data.scenes.get(owner);
        if(scene==null||!door.equals(scene.door)||!getUUID().equals(scene.wybieActor)){stopAudio();discard();return;}
        var p=getServer().getPlayerList().getPlayer(owner);
        if(p==null||!p.isAlive()||p.level()!=level()||distanceToSqr(p)>4096){
            getNavigation().stop();if(!paused){stopAudio();paused=true;}return;
        }
        if(paused||(!audioStarted&&phase()!=WAIT)){
            startMusic(p);if(!line(phase()).isEmpty())VisitAudio.play(p,sound(),false);
            entityData.set(TIME,0);voiceDone=false;paused=false;audioStarted=true;
        }
        if(getY()<level().getMinBuildHeight()+4){setPos(p.position().add(2,0,0));setDeltaMovement(Vec3.ZERO);}
        if(!biking())watch(p);
        int time=elapsed();
        if(phase()==WAIT){getNavigation().stop();if(lookedAt(p))begin(A,p);return;}
        if(phase()==A){if(distanceToSqr(p)>5&&time%10==0)getNavigation().moveTo(p,1.15);else if(distanceToSqr(p)<=5)getNavigation().stop();}
        else if(biking()){if(time%10==0)getNavigation().moveTo(escape.x,escape.y,escape.z,2.1);}
        else getNavigation().stop();
        if(phase()==E&&time>=20&&!cameraGiven){
            var camera=new net.minecraft.world.item.ItemStack(ModItems.CAMERA.get());
            if(!p.getInventory().add(camera))p.drop(camera,false);cameraGiven=true;
            WybieVisitNetwork.camera(p);
        }
        if((phase()==F||phase()==G||phase()==H)&&time>=25&&!flashDone){WybieVisitNetwork.flash(p);flashDone=true;}
        if(phase()==THROW&&time==12){var at=getEyePosition().add(getLookAngle().scale(.7));((net.minecraft.server.level.ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,at.x,at.y,at.z,8,.6,.25,.6,.08);}
        int required=switch(phase()){case C,E,L->20;case F,G,H->25;default->0;};
        boolean done=phase()==SEARCH||phase()==THROW?time>=30:(voiceDone||time>=VisitAudio.length(sound())+60)&&time>=required;
        if(done&&phase()==A&&distanceToSqr(p)>9){entityData.set(TIME,time+1);return;}
        if(done){if(phase()==L){com.coraline.feast.WybieOptionalVisits.introductionCompleted(p);VisitAudio.stopMusic(p,"wybie_visit_music");scene.wybieActor=null;data.setDirty();discard();return;}begin(phase()+1,p);}
        else entityData.set(TIME,time+1);
    }
    public boolean lookedAt(ServerPlayer p){Vec3 to=getEyePosition().subtract(p.getEyePosition());return to.lengthSqr()<1024&&p.getLookAngle().dot(to.normalize())>.985&&p.hasLineOfSight(this);}
    private void watch(ServerPlayer p){getLookControl().setLookAt(p,30,30);if(getNavigation().isDone()){float yaw=(float)Math.toDegrees(Math.atan2(p.getZ()-getZ(),p.getX()-getX()))-90;setYRot(yaw);yBodyRot=yaw;}}
    private void stopAudio(){var p=getServer()==null?null:getServer().getPlayerList().getPlayer(owner);if(p!=null){VisitAudio.stopMusic(p,"wybie_visit_music");if(!line(phase()).isEmpty())VisitAudio.stopVoice(p,sound());}}
    @Override public boolean hurt(DamageSource source,float amount){return com.coraline.feast.CanonicalRealWybie.tagged(this)&&super.hurt(source,amount);}
    @Override public void die(DamageSource source){if(com.coraline.feast.CanonicalRealWybie.tagged(this))super.die(source);}
    @Override public void kill(){if(com.coraline.feast.CanonicalRealWybie.tagged(this))super.kill();}
    @Override public boolean isPushable(){return false;}
    @Override public void push(Entity other){}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(owner!=null)n.putUUID("VisitOwner",owner);if(door!=null)n.putUUID("VisitDoor",door);n.putInt("VisitPhase",phase());n.putInt("VisitTime",elapsed());n.putBoolean("CameraGiven",cameraGiven);n.putBoolean("FlashDone",flashDone);n.putDouble("EscapeX",escape.x);n.putDouble("EscapeY",escape.y);n.putDouble("EscapeZ",escape.z);}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);owner=n.hasUUID("VisitOwner")?n.getUUID("VisitOwner"):null;door=n.hasUUID("VisitDoor")?n.getUUID("VisitDoor"):null;entityData.set(PHASE,Math.max(WAIT,Math.min(L,n.getInt("VisitPhase"))));entityData.set(TIME,n.getInt("VisitTime"));cameraGiven=n.getBoolean("CameraGiven");flashDone=n.getBoolean("FlashDone");escape=new Vec3(n.getDouble("EscapeX"),n.getDouble("EscapeY"),n.getDouble("EscapeZ"));audioStarted=false;}
}
