package com.coraline.parents;
import com.coraline.config.CoralineConfig;
import com.coraline.registry.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.*;
import java.util.UUID;

public final class ParentEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SKIN=SynchedEntityData.defineId(ParentEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CLAIMED=SynchedEntityData.defineId(ParentEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> OTHER=SynchedEntityData.defineId(ParentEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROZEN=SynchedEntityData.defineId(ParentEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TRUST=SynchedEntityData.defineId(ParentEntity.class,EntityDataSerializers.FLOAT),SUSPICION=SynchedEntityData.defineId(ParentEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LOOP_GAZE=SynchedEntityData.defineId(ParentEntity.class,EntityDataSerializers.BOOLEAN);
    public float trust(){return entityData.get(TRUST);}public float suspicion(){return entityData.get(SUSPICION);}
    public void relationship(float trust,float suspicion){entityData.set(TRUST,trust);entityData.set(SUSPICION,suspicion);entityData.set(LOOP_GAZE,getPersistentData().hasUUID("LoopOwner"));}
    private net.minecraft.world.phys.Vec3 deathAnchor;
    public boolean deathFrozen(){return entityData.get(FROZEN);}
    public void freezeForDeath(){
        if(!deathFrozen()){deathAnchor=position();entityData.set(FROZEN,true);setHealth(Math.max(1,getHealth()));}
        setNoAi(true);setNoGravity(true);setInvulnerable(true);getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    public void finishScriptedDeath(){setHealth(0);super.die(damageSources().genericKill());}
    @Override public boolean hurt(DamageSource source,float amount){return !deathFrozen()&&super.hurt(source,amount);}
    @Override public boolean isPushable(){return !deathFrozen()&&super.isPushable();}
    @Override public boolean isPickable(){return !deathFrozen()&&super.isPickable();}
    public boolean isOtherParent(){return entityData.get(OTHER);}
    public void makeOtherParent(int skin){entityData.set(OTHER,true);entityData.set(CLAIMED,true);entityData.set(SKIN,skin);setInvulnerable(true);}
    public UUID familyId;
    private int voiceCountdown;
    private int nextInteractionVoice;
    public ParentEntity(EntityType<? extends PathfinderMob> type,Level level){
        super(type,level);setPersistenceRequired();
        lookControl=new net.minecraft.world.entity.ai.control.LookControl(this){
            @Override public void tick(){if(!scriptedGaze()||!getNavigation().isDone())super.tick();}
        };
    }
    @Override protected net.minecraft.world.entity.ai.control.BodyRotationControl createBodyControl(){
        return new net.minecraft.world.entity.ai.control.BodyRotationControl(this){
            @Override public void clientTick(){if(!scriptedGaze())super.clientTick();else if(!isPassenger())setYBodyRot(getYRot());}
        };
    }
    public boolean isMom(){return getType()==ModEntities.MOM.get();}
    public int skin(){return entityData.get(SKIN);}
    public static final int SKIN_COUNT=64;
    private static int lastMomSkin=-1,lastDadSkin=-1;
    public void randomizeSkin(){
        int last=isMom()?lastMomSkin:lastDadSkin;
        int selected=random.nextInt(last<0?SKIN_COUNT:SKIN_COUNT-1);
        if(selected>=last&&last>=0)selected++;
        entityData.set(SKIN,selected);
        if(isMom())lastMomSkin=selected;else lastDadSkin=selected;
    }
    public boolean isClaimed(){return entityData.get(CLAIMED);}
    void markClaimedForSetup(){entityData.set(CLAIMED,true);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SKIN,0);entityData.define(CLAIMED,false);entityData.define(OTHER,false);entityData.define(FROZEN,false);entityData.define(TRUST,0F);entityData.define(SUSPICION,0F);entityData.define(LOOP_GAZE,false);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,0.28).add(Attributes.FOLLOW_RANGE,48);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.4));
        goalSelector.addGoal(2,new OpenDoorGoal(this,true){
            @Override public boolean canUse(){return scriptedGaze()&&super.canUse();}
        });
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8){
            @Override public boolean canUse(){return !scriptedGaze()&&super.canUse();}
            @Override public boolean canContinueToUse(){return !scriptedGaze()&&super.canContinueToUse();}
        });
        goalSelector.addGoal(7,new RandomLookAroundGoal(this){
            @Override public boolean canUse(){return !scriptedGaze()&&super.canUse();}
            @Override public boolean canContinueToUse(){return !scriptedGaze()&&super.canContinueToUse();}
        });
    }
    public boolean scriptedGaze(){return isOtherParent()&&(getPersistentData().hasUUID("VisitOwner")||getPersistentData().hasUUID("LoopOwner")||entityData.get(LOOP_GAZE));}
    /** Apply after AI/body control, which otherwise pulls stationary heads back to the body. */
    public void lookAtStoryPlayer(Player player){
        double dx=player.getX()-getX(),dz=player.getZ()-getZ();
        double dy=player.getEyeY()-getEyeY();
        float yaw=(float)(net.minecraft.util.Mth.atan2(dz,dx)*180/Math.PI)-90;
        float pitch=(float)-(net.minecraft.util.Mth.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);
        setYHeadRot(net.minecraft.util.Mth.approachDegrees(getYHeadRot(),yaw,30));
        setXRot(net.minecraft.util.Mth.approachDegrees(getXRot(),pitch,20));
        if(!isPassenger()){
            setYBodyRot(net.minecraft.util.Mth.approachDegrees(yBodyRot,getNavigation().isDone()?yaw:getYRot(),15));
            if(getNavigation().isDone())setYRot(yBodyRot);
        }
        getLookControl().setLookAt(player,40,40);
    }
    public FamilyData.Family family(){return level() instanceof ServerLevel sl && familyId!=null?FamilyData.get(sl.getServer()).families.get(familyId):null;}
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive())return;
        if(deathFrozen()){
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);if(deathAnchor!=null)setPos(deathAnchor);
            var dying=family();if(dying==null||dying.deathPhase==0)finishScriptedDeath();return;
        }
        if(isOtherParent()){
            // Expedition hosts belong to the activity, not to the retired house scene.
            // Handle them before retiredFather/control can discard or reposition them.
            if(getPersistentData().hasUUID("CoralineNewActivityOwner")&&com.coraline.feast.ActivityWorld.isActivity(level())){
                getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                entityData.set(LOOP_GAZE,true);
                var owner=getServer().getPlayerList().getPlayer(getPersistentData().getUUID("CoralineNewActivityOwner"));
                if(owner!=null&&owner.level()==level())lookAtStoryPlayer(owner);
                return;
            }
            if((getPersistentData().getBoolean("CoralineVillageFather")||getPersistentData().getBoolean("CoralineArcheryFather")||getPersistentData().getBoolean("CoralineCakeFather")||getPersistentData().getBoolean("CoralineCarnivalFather")||getPersistentData().getBoolean("CoralineParkourFather")||getPersistentData().getBoolean("CoralineFishingFather")||getPersistentData().getBoolean("CoralineGardenFather")||getPersistentData().getBoolean("CoralineFeastFather")||getPersistentData().getBoolean("CoralineIceFather")||getPersistentData().hasUUID("CoralineMagicOwner")||getPersistentData().hasUUID("CoralineFlyingGardenOwner")||getPersistentData().hasUUID("CoralineCircusOwner")||getPersistentData().hasUUID("CoralineOceanOwner"))&&com.coraline.feast.ActivityWorld.isActivity(level())){
                getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                // Activity fathers have no AI, so turn them explicitly toward their own player.
                // Sync scripted gaze as well, or the client body controller turns them away again.
                entityData.set(LOOP_GAZE,true);
                String ownerKey=getPersistentData().hasUUID("CoralineOceanOwner")?"CoralineOceanOwner":getPersistentData().getBoolean("CoralineArcheryFather")?"CoralineArcheryOwner":getPersistentData().getBoolean("CoralineCakeFather")?"CoralineCakeOwner":getPersistentData().getBoolean("CoralineCarnivalFather")?"CoralineCarnivalOwner":getPersistentData().getBoolean("CoralineParkourFather")?"CoralineParkourOwner":getPersistentData().getBoolean("CoralineFishingFather")?"CoralineFishingOwner":getPersistentData().getBoolean("CoralineGardenFather")?"CoralineGardenOwner":getPersistentData().getBoolean("CoralineFeastFather")?"CoralineFeastOwner":getPersistentData().getBoolean("CoralineIceFather")?"CoralineIceOwner":getPersistentData().hasUUID("CoralineMagicOwner")?"CoralineMagicOwner":getPersistentData().hasUUID("CoralineFlyingGardenOwner")?"CoralineFlyingGardenOwner":getPersistentData().hasUUID("CoralineCircusOwner")?"CoralineCircusOwner":"CoralineVillageOwner";
                if(getPersistentData().hasUUID(ownerKey)){
                    var player=getServer().getPlayerList().getPlayer(getPersistentData().getUUID(ownerKey));
                    if(player!=null&&player.level()==level())lookAtStoryPlayer(player);
                }
                return;
            }
            if(com.coraline.feast.OtherMotherLoop.retiredFather(this)){discard();return;}
            if(com.coraline.feast.OtherMotherLoop.control(this))return;
            if(com.coraline.feast.FirstVisit.control(this))return;
            if(getPersistentData().hasUUID("RealmDoor")){
                var realm=com.coraline.otherworld.RealmData.get(getServer()).realms.get(getPersistentData().getUUID("RealmDoor"));
                if(realm==null||getPersistentData().getInt("RealmGeneration")!=realm.parentGeneration){discard();return;}
            }
            entityData.set(CLAIMED,true);
            if(tickCount%20==0&&getPersistentData().hasUUID("OriginalFamily")){
                var original=FamilyData.get(getServer()).families.get(getPersistentData().getUUID("OriginalFamily"));
                if(original!=null){int saved=isMom()?original.momSkin:original.dadSkin;if(saved>=0)entityData.set(SKIN,saved);}
            }
            if(tickCount%100==0){var home=getPersistentData().contains("OtherHome")?net.minecraft.core.BlockPos.of(getPersistentData().getLong("OtherHome")):blockPosition();getNavigation().moveTo(home.getX()+random.nextInt(13)-6,home.getY(),home.getZ()+random.nextInt(13)-6,.7);}
            tickVoice();return;
        }
        FamilyData.Family f=family();if(f==null)return;
        entityData.set(CLAIMED,f.owner!=null);
        if(f.dead){freezeForDeath();if(f.deathPhase==0)finishScriptedDeath();return;}
        if(tickCount%CoralineConfig.HEAL_INTERVAL.get()==0)heal(CoralineConfig.HEAL_AMOUNT.get().floatValue());
        if(tickCount%100==0){
            if(isMom())f.momSkin=skin();else f.dadSkin=skin();
            if(isMom()){f.momPos=blockPosition();f.momDim=level().dimension().location().toString();}
            else{f.dadPos=blockPosition();f.dadDim=level().dimension().location().toString();}
            FamilyData.get(getServer()).setDirty();
        }
        boolean working=ParentTasks.working(this,f);
        boolean panicking=getLastHurtByMob()!=null&&tickCount-getLastHurtByMobTimestamp()<100;
        // Give work navigation exclusive control so home wandering cannot cancel a delivery.
        if(!panicking){
            if(tickCount%20==0 && (!working||distanceToSqr(f.bed.getCenter())>Math.pow(CoralineConfig.HOME_RADIUS.get()+4,2)))wanderOrFollow(f);
            if(working)ParentTasks.tick(this,f);
        }
        tickVoice();
    }
    void tickVoice(){
        if(voiceCountdown<=0)voiceCountdown=nextVoiceDelay();
        if(--voiceCountdown==0){
            var family=ParentVoices.family(this);
            if(family!=null&&getServer().overworld().getGameTime()<family.dialogueUntil){
                voiceCountdown=20+random.nextInt(81);return;
            }
            speak();
            voiceCountdown=nextVoiceDelay();
        }
    }
    private void speak(){
        if(tickCount<nextInteractionVoice)return;
        var sound=ParentVoices.dialogue(this);playSound(sound,CoralineConfig.VOICE_VOLUME.get().floatValue(),1);
        var family=ParentVoices.family(this);
        if(family!=null)family.dialogueUntil=Math.max(family.dialogueUntil,getServer().overworld().getGameTime()+ParentVoices.length(sound)+20);
        nextInteractionVoice=tickCount+Math.max(ParentVoices.length(sound),CoralineConfig.PARENT_INTERACTION_VOICE_COOLDOWN.get());voiceCountdown=nextVoiceDelay();
    }
    private int nextVoiceDelay(){int variance=Math.min(CoralineConfig.DIALOGUE_VARIATION.get(),CoralineConfig.DIALOGUE_INTERVAL.get()-20);return CoralineConfig.DIALOGUE_INTERVAL.get()-variance+random.nextInt(variance*2+1);}
    private void wanderOrFollow(FamilyData.Family f){
        if(f.bed!=null){
            ServerLevel home=FamilyData.level(getServer(),f.homeDim);
            if(home==null)return;
            if(level()!=home){ParentEvents.transferNear(this,home,f.bed);return;}
            if(distanceToSqr(f.bed.getX()+.5,f.bed.getY(),f.bed.getZ()+.5)>Math.pow(CoralineConfig.HOME_RADIUS.get(),2))
                getNavigation().moveTo(f.bed.getX()+.5,f.bed.getY(),f.bed.getZ()+.5,1);
            else if(getNavigation().isDone()||tickCount%80==0){int r=CoralineConfig.HOME_RADIUS.get();getNavigation().moveTo(f.bed.getX()+random.nextInt(r*2+1)-r,f.bed.getY(),f.bed.getZ()+random.nextInt(r*2+1)-r,1.05);}
        }else if(f.owner!=null){
            ServerPlayer owner=getServer().getPlayerList().getPlayer(f.owner);if(owner==null||!owner.isAlive())return;
            if(com.coraline.otherworld.OtherWorld.isOther(owner.level()))return;
            if(level()!=owner.level()||distanceToSqr(owner)>Math.pow(CoralineConfig.TELEPORT_DISTANCE.get(),2)){ParentEvents.transferNear(this,owner.serverLevel(),owner.blockPosition());return;}
            if(distanceToSqr(owner)>9)getNavigation().moveTo(owner,CoralineConfig.FOLLOW_SPEED.get());else getNavigation().stop();
        }else if(random.nextInt(6)==0){
            var partner=((ServerLevel)level()).getEntity(isMom()?f.dad:f.mom);
            if(partner!=null&&distanceToSqr(partner)>36)getNavigation().moveTo(partner,1);
            else getNavigation().moveTo(getX()+random.nextInt(9)-4,getY(),getZ()+random.nextInt(9)-4,.6);
        }
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(deathFrozen())return InteractionResult.FAIL;
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(isOtherParent())return InteractionResult.CONSUME;
        FamilyData.Family f=family();if(f==null||f.dead)return InteractionResult.PASS;
        speak();
        if(f.owner==null){
            boolean alreadyHasParents=FamilyData.get(getServer()).families.values().stream().anyMatch(other->!other.dead&&player.getUUID().equals(other.owner));
            if(alreadyHasParents){
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("You already have parents."),true);
                return InteractionResult.CONSUME;
            }
            f.owner=player.getUUID();FamilyData.get(getServer()).setDirty();
            entityData.set(CLAIMED,true);
            var bed=ParentsBedItem.forFamily(f.id);if(!player.getInventory().add(bed))player.drop(bed,false);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Your parents will follow you. Place their bed to give them a home."),false);
        }else if(!f.owner.equals(player.getUUID()))player.displayClientMessage(net.minecraft.network.chat.Component.literal("These parents already belong to another family."),true);
        else if(player.isShiftKeyDown()&&player.getItemInHand(hand).isEmpty()&&f.bed==null){
            boolean hasBed=false;
            for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
                var stack=player.getInventory().getItem(slot);var tag=stack.getTagElement("BlockEntityTag");
                if(stack.is(ModItems.PARENTS_BED.get())&&tag!=null&&tag.hasUUID("Family")&&f.id.equals(tag.getUUID("Family"))){hasBed=true;break;}
            }
            if(!hasBed){var replacement=ParentsBedItem.forFamily(f.id);if(!player.getInventory().add(replacement))player.drop(replacement,false);}
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(hasBed?"You already have our bed.":"Here is your parents' replacement bed."),true);
        }
        else if(player.getItemInHand(hand).is(ModItems.PARENTS_BED.get()) && !player.getItemInHand(hand).getOrCreateTagElement("BlockEntityTag").hasUUID("Family")){
            player.getItemInHand(hand).getOrCreateTagElement("BlockEntityTag").putUUID("Family",f.id);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("This bed is now assigned to your parents."),true);
        }else if(!f.isAvailable(getServer())){
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("parents are too busy for you right now!"),true);
        }else if(player instanceof ServerPlayer serverPlayer){
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,new net.minecraft.world.SimpleMenuProvider(
                (id,inventory,who)->new ParentsMenu(id,inventory,this),net.minecraft.network.chat.Component.literal(isMom()?"Mom's tasks":"Dad's tasks")),
                buffer->{buffer.writeVarInt(getId());buffer.writeBoolean(isMom());});
        }
        return InteractionResult.CONSUME;
    }
    @Override public void die(DamageSource source){
        if(level().isClientSide){super.die(source);return;}
        FamilyData.Family f=family();if(f==null||isOtherParent()){super.die(source);return;}
        if(!f.dead)ParentDeath.begin(this,f);
        else if(f.deathPhase==0)finishScriptedDeath();
    }
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.PLAYER_HURT;}
    @Override protected SoundEvent getDeathSound(){return null;}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(familyId!=null)n.putUUID("Family",familyId);n.putInt("Skin",skin());n.putInt("VoiceCountdown",voiceCountdown);n.putBoolean("OtherParent",isOtherParent());n.putBoolean("DeathFrozen",deathFrozen());}
    @Override public void remove(RemovalReason reason){
        if(level() instanceof ServerLevel sl){
            var f=family();if(f!=null){
                if(isMom()){f.momPos=blockPosition();f.momDim=sl.dimension().location().toString();}
                else{f.dadPos=blockPosition();f.dadDim=sl.dimension().location().toString();}
                FamilyData.get(sl.getServer()).setDirty();
            }
        }super.remove(reason);
    }
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);if(n.hasUUID("Family"))familyId=n.getUUID("Family");entityData.set(SKIN,Math.floorMod(n.getInt("Skin"),SKIN_COUNT));voiceCountdown=n.getInt("VoiceCountdown");entityData.set(OTHER,n.getBoolean("OtherParent"));
        if(n.getBoolean("DeathFrozen")){entityData.set(FROZEN,true);deathAnchor=position();}
        if(!level().isClientSide&&family()!=null){var f=family();if(isMom())f.momSkin=skin();else f.dadSkin=skin();FamilyData.get(getServer()).setDirty();}
    }
}
