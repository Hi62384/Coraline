package com.coraline.feast;
import static com.coraline.feast.ExpeditionActivities.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.sounds.*;
import com.coraline.registry.ModItems;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

@Mod.EventBusSubscriber(modid="coraline")
public final class SnowballBattleActivity {
    static final String[] NAMES={"Buttons","Marbles","Frost","Skipper","Mittens","Flurry"};
    private static final Map<UUID,ServerBossEvent> BARS=new HashMap<>();
    static boolean active(ServerPlayer p,VisitData.Scene s){return owns(p,s)&&s.loop.activity==OtherMotherActivities.Activity.SNOWBALL_BATTLE;}
    static BlockPos arena(BlockPos c){return c.north(38);}
    static Vec3 spawn(BlockPos c,int i){double angle=2*Math.PI*i/7;return Vec3.atBottomCenterOf(arena(c)).add(Math.sin(angle)*25,0,Math.cos(angle)*25);}
    static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){SnowBattleScenery.build(l,s,c);}
    static void begin(ServerPlayer p,VisitData.Scene s){var n=state(s);borrow(p,s,ModItems.SNOWBALL_POUCH.get());n.putInt("Health",5);n.putInt("Phase",1);n.putLong("Go",OtherMotherLoop.now(p)+60);n.putBoolean("BattleAbilities",true);n.putBoolean("BattleMayfly",p.getAbilities().mayfly);n.putBoolean("BattleFlying",p.getAbilities().flying);n.putLong("EffectsAt",OtherMotherLoop.now(p));var effects=new ListTag();for(var effect:p.getActiveEffects())effects.add(effect.save(new CompoundTag()));n.put("BattleEffects",effects);p.removeAllEffects();
        var c=center(p,s);ActivityScenery.teleport(p,spawn(c,0));var fighters=new ListTag();int[] colors={0xDB6868,0x66A5BF,0xCCCCDD,0x8EB86A,0xB881BD,0xEDBE62};for(int i=0;i<6;i++){var npc=MallScenery.employee(p.serverLevel(),s,BlockPos.containing(spawn(c,i+1)),i+40,-1);npc.getPersistentData().remove("MallNpc");npc.getPersistentData().putInt("SnowStyle",i);npc.getPersistentData().putLong("NextThrow",OtherMotherLoop.now(p)+120+i*7);npc.getPersistentData().putBoolean("SnowFighter",true);npc.setCustomName(Component.literal(NAMES[i]));npc.setNoAi(false);npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.SNOWBALL));for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.FEET}){var stack=new ItemStack(slot==EquipmentSlot.HEAD?Items.LEATHER_HELMET:slot==EquipmentSlot.CHEST?Items.LEATHER_CHESTPLATE:Items.LEATHER_BOOTS);((DyeableLeatherItem)stack.getItem()).setColor(stack,colors[i]);npc.setItemSlot(slot,stack);npc.setDropChance(slot,0);}var f=new CompoundTag();f.putUUID("Id",npc.getUUID());f.putInt("Health",5);f.putInt("Style",i);fighters.add(f);}n.put("Fighters",fighters);ActivityScenery.title(p,"SNOWBALL BATTLE","Right-click to throw — five hits and you’re out.");}
    static List<LivingEntity> living(ServerPlayer p,VisitData.Scene s){var result=new ArrayList<LivingEntity>();if(state(s).getInt("Health")>0)result.add(p);for(var tag:state(s).getList("Fighters",10)){var f=(CompoundTag)tag;if(f.getInt("Health")>0&&p.serverLevel().getEntity(f.getUUID("Id")) instanceof LivingEntity e)result.add(e);}return result;}
    static CompoundTag fighter(VisitData.Scene s,UUID id){for(var tag:state(s).getList("Fighters",10)){var f=(CompoundTag)tag;if(f.getUUID("Id").equals(id))return f;}return null;}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){var n=state(s);int phase=n.getInt("Phase");if(phase>=3){if(phase==3&&p.tickCount%15==0){var at=prizePosition(c);p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,at.x,at.y+.6,at.z,3,.2,.15,.2,0);}if(!bounds(c,s.loop.activity).contains(p.position())||p.getY()<c.getY()-2)ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));return;}
        p.stopFallFlying();if(p.getAbilities().mayfly||p.getAbilities().flying){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();}p.removeAllEffects();
        long now=OtherMotherLoop.now(p);if(phase==1){if(p.distanceToSqr(spawn(c,0))>.2)ActivityScenery.teleport(p,spawn(c,0));long left=n.getLong("Go")-now;if(left<=0){n.putInt("Phase",2);n.putLong("BattleAt",now);ActivityScenery.title(p,"GO!","Right-click to throw — five hits and you’re out.");}else if(left%20==0){ActivityScenery.title(p,""+left/20,"");p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(),SoundSource.PLAYERS,.5F,1);}return;}
        for(var helper:p.serverLevel().players())if(helper!=p&&bounds(c,s.loop.activity).contains(helper.position())&&helper.getZ()<c.getZ()-6)ActivityScenery.teleport(helper,Vec3.atBottomCenterOf(c.offset(6,0,5)));
        for(var tag:n.getList("Fighters",10)){var f=(CompoundTag)tag;if(f.getInt("Health")>0&&p.serverLevel().getEntity(f.getUUID("Id"))==null){finish(p,s,false);return;}}
        if(needsRecovery(c,p.position()))ActivityScenery.teleport(p,recovery(c,p.position()));
        var alive=living(p,s);if(alive.size()==1&&alive.get(0)==p){finish(p,s,true);return;}if(alive.isEmpty()){finish(p,s,false);return;}
        if(p.tickCount%10==0){var bar=BARS.computeIfAbsent(s.owner,k->new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.PROGRESS));bar.addPlayer(p);bar.setProgress(n.getInt("Health")/5F);bar.setName(Component.literal("❄ ".repeat(n.getInt("Health"))+" • Fighters: "+alive.size()));}
        for(var entity:alive)if(entity instanceof FishingContestNpcEntity npc)ai(p,s,c,npc,alive,now);
        for(var ball:p.serverLevel().getEntitiesOfClass(Snowball.class,bounds(c,s.loop.activity).inflate(12),e->ActivityScenery.owned(e,s))){if(ball.tickCount>100||!bounds(c,s.loop.activity).contains(ball.position()))ball.discard();else if(ball.tickCount%4==0)p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE,ball.getX(),ball.getY(),ball.getZ(),1,0,0,0,0);}
        long age=now-n.getLong("BattleAt");if(age>0&&age%800==0){p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE,arena(c).getX(),c.getY()+5,arena(c).getZ(),100,26,2,26,.03);p.playNotifySound(SoundEvents.POWDER_SNOW_FALL,SoundSource.AMBIENT,.6F,.8F);}
        // Late-match pressure brings opponents to the player; never teleport an active player.
        if(age==3600||age==4800){ActivityScenery.title(p,"SNOWSTORM","The remaining rivals are hunting you!");for(var e:alive)if(e instanceof FishingContestNpcEntity npc){npc.getPersistentData().putUUID("SnowTarget",p.getUUID());npc.getNavigation().moveTo(p,1.15);}}
        if(age>=6000)finish(p,s,false); // A clean draw, never an unearned last-survivor prize.
    }
    static Vec3 prizePosition(BlockPos c){return Vec3.atBottomCenterOf(c.offset(-10,1,0)).add(0,.05,0);}
    // The arena includes the lobby and raised overlooks. Its actual walls, not a
    // smaller arbitrary radius, define recovery. Recover locally, not at spawn.
    static boolean needsRecovery(BlockPos c,Vec3 at){
        return at.y<c.getY()-3||at.y>c.getY()+14||at.x<c.getX()-39||at.x>c.getX()+40
                ||at.z<c.getZ()-77||at.z>c.getZ()+13;
    }
    static Vec3 recovery(BlockPos c,Vec3 at){
        double x=at.x<c.getX()?c.getX()-12.5:c.getX()+12.5;
        double z=Math.max(c.getZ()-65.5,Math.min(c.getZ()-12.5,at.z));
        return new Vec3(x,c.getY()+1,z);
    }
    static Vec3 throwVelocity(Vec3 from,Vec3 target,Vec3 motion,double lead){
        Vec3 result=target.subtract(from).normalize().scale(1.45);
        double best=Double.MAX_VALUE;
        for(int ticks=4;ticks<=38;ticks++){
            double drag=Math.pow(.99,ticks),sum=(1-drag)/.01;
            var future=target.add(Math.max(-.35,Math.min(.35,motion.x))*ticks*lead,0,
                    Math.max(-.35,Math.min(.35,motion.z))*ticks*lead);
            var delta=future.subtract(from);
            var v=new Vec3(delta.x/sum,(delta.y+.03/.01*(ticks-sum))/sum,delta.z/sum);
            double error=Math.abs(v.length()-1.45);
            if(error<best){best=error;result=v;}
        }
        return result;
    }
    static boolean visible(LivingEntity a,LivingEntity b){return a.level().clip(new ClipContext(a.getEyePosition(),b.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,a)).getType()==HitResult.Type.MISS;}
    static void ai(ServerPlayer p,VisitData.Scene s,BlockPos c,FishingContestNpcEntity npc,List<LivingEntity> alive,long now){var f=fighter(s,npc.getUUID());int style=f.getInt("Style");var data=npc.getPersistentData();LivingEntity target=data.hasUUID("SnowTarget")&&p.serverLevel().getEntity(data.getUUID("SnowTarget")) instanceof LivingEntity t&&alive.contains(t)?t:null;
        if(target==null||Math.floorMod(now+style*7,40)==0||!visible(npc,target)&&now%20==0){double best=Double.MAX_VALUE;LivingEntity chosen=null;int focus=0;for(var other:alive)if(other!=npc&&other.getPersistentData().hasUUID("SnowTarget")&&other.getPersistentData().getUUID("SnowTarget").equals(p.getUUID()))focus++;
            for(var candidate:alive)if(candidate!=npc){double score=npc.distanceToSqr(candidate)+(visible(npc,candidate)?0:120)+p.getRandom().nextInt(70);if(candidate==p){boolean passive=now-Math.max(state(s).getLong("BattleAt"),state(s).getLong("LastPlayerThrow"))>160;score-=passive?500:100;if(focus>=2&&alive.size()>3)score+=350;}if(data.hasUUID("RecentAttacker")&&data.getUUID("RecentAttacker").equals(candidate.getUUID()))score-=60;if(score<best){best=score;chosen=candidate;}}target=chosen;if(target!=null)data.putUUID("SnowTarget",target.getUUID());}
        if(target==null)return;npc.getLookControl().setLookAt(target,30,30);double distance=npc.distanceTo(target);
        if(Math.floorMod(now+style*3,20)==0){var away=npc.position().subtract(target.position()).multiply(1,0,1).normalize();double desired=style==0||style==5?8:style==2?22:14;boolean retreat=f.getInt("Health")==1&&(style==1||style==4)&&distance<18;double forward=distance>desired&&!retreat?-5:distance<desired-3||retreat?5:0;double strafe=(style==3?6:3)*(Math.floorMod(now/60+style,2)==0?1:-1);var next=npc.position().add(away.scale(forward)).add(-away.z*strafe,0,away.x*strafe);var ac=arena(c);double nx=Math.max(ac.getX()-31,Math.min(ac.getX()+31,next.x)),nz=Math.max(ac.getZ()-31,Math.min(ac.getZ()+31,next.z));if(!visible(npc,target))npc.getNavigation().moveTo(target,style==3?1.2:1.0);else npc.getNavigation().moveTo(nx,c.getY(),nz,style==3?1.2:.95);}
        if(now>=data.getLong("NextThrow")&&distance<34&&visible(npc,target)){
            var ball=new Snowball(p.serverLevel(),npc);ActivityScenery.mark(ball,s);
            ball.getPersistentData().putBoolean("BattleSnowball",true);ball.getPersistentData().putInt("Generation",s.loop.generation);
            // Solve the vanilla drag/gravity trajectory, then lead horizontal movement.
            var velocity=throwVelocity(npc.getEyePosition(),target.position().add(0,target.getBbHeight()*.65,0),
                    target.getDeltaMovement(),style==2?.9:.65);
            ball.shoot(velocity.x,velocity.y,velocity.z,(float)velocity.length(),style==2?1.5F:style==5?3.5F:2.5F);
            p.serverLevel().addFreshEntity(ball);npc.swing(InteractionHand.MAIN_HAND);
            data.putLong("NextThrow",now+(style==5?28:style==0?34:style==2?48:40)+p.getRandom().nextInt(14));
            p.serverLevel().playSound(null,npc.blockPosition(),SoundEvents.SNOWBALL_THROW,SoundSource.NEUTRAL,.35F,1.1F);
        }
    }
    public static void throwBall(ServerPlayer p){var s=OtherMotherLoop.active(p);if(!active(p,s)||state(s).getInt("Phase")!=2||!validTool(p,s,ModItems.SNOWBALL_POUCH.get()))return;long now=OtherMotherLoop.now(p);if(now<state(s).getLong("NextThrow"))return;state(s).putLong("NextThrow",now+12);state(s).putLong("LastPlayerThrow",now);var ball=new Snowball(p.serverLevel(),p);ActivityScenery.mark(ball,s);ball.getPersistentData().putBoolean("BattleSnowball",true);ball.getPersistentData().putInt("Generation",s.loop.generation);ball.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.5F,.2F);p.serverLevel().addFreshEntity(ball);p.getCooldowns().addCooldown(ModItems.SNOWBALL_POUCH.get(),12);p.playNotifySound(SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.6F,1.1F);}
    @SubscribeEvent public static void impact(ProjectileImpactEvent event){if(!(event.getProjectile() instanceof Snowball ball)||!ball.getPersistentData().getBoolean("BattleSnowball")||ball.level().isClientSide)return;var data=ball.getPersistentData();var p=data.hasUUID(ActivityScenery.OWNER)?ball.getServer().getPlayerList().getPlayer(data.getUUID(ActivityScenery.OWNER)):null;var s=p==null?null:OtherMotherLoop.active(p);if(p==null||!active(p,s)||state(s).getInt("Phase")!=2||data.getInt("Generation")!=s.loop.generation){event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);ball.discard();return;}
        if(event.getRayTraceResult() instanceof EntityHitResult hit){var target=hit.getEntity();if(target!=p&&fighter(s,target.getUUID())==null){event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);return;}if(target instanceof LivingEntity living)hit(p,s,living,ball.getOwner());}
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);p.serverLevel().sendParticles(ParticleTypes.ITEM_SNOWBALL,ball.getX(),ball.getY(),ball.getZ(),8,.18,.18,.18,.03);ball.discard();}
    static void hit(ServerPlayer p,VisitData.Scene s,LivingEntity target,Entity attacker){if(state(s).getInt("Phase")!=2)return;var f=target==p?state(s):fighter(s,target.getUUID());if(f==null||f.getInt("Health")<=0)return;f.putInt("Health",f.getInt("Health")-1);if(attacker!=null){target.getPersistentData().putUUID("RecentAttacker",attacker.getUUID());var push=target.position().subtract(attacker.position()).normalize().scale(.12);target.setDeltaMovement(target.getDeltaMovement().add(push.x,.04,push.z));target.hurtMarked=true;}p.serverLevel().sendParticles(ParticleTypes.ITEM_SNOWBALL,target.getX(),target.getY()+1,target.getZ(),14,.3,.4,.3,.05);p.serverLevel().playSound(null,target.blockPosition(),SoundEvents.SNOW_BREAK,SoundSource.PLAYERS,.7F,1.2F);if(target==p)p.displayClientMessage(Component.literal("Hit! "+f.getInt("Health")+" / 5 snow hearts").withStyle(net.minecraft.ChatFormatting.RED,net.minecraft.ChatFormatting.BOLD),true);if(attacker==p)p.displayClientMessage(Component.literal(target.getName().getString()+"  "+f.getInt("Health")+" / 5"),true);
        if(f.getInt("Health")==0){if(target==p){finish(p,s,false);return;}if(target instanceof FishingContestNpcEntity npc){npc.getNavigation().stop();npc.setNoAi(true);npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);}var at=center(p,s).offset(-12+f.getInt("Style")*4,0,8);target.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);p.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.PLAYERS,.6F,.8F);}OtherMotherLoop.changed(p);}
    static void finish(ServerPlayer p,VisitData.Scene s,boolean win){var n=state(s);if(n.getInt("Phase")>=3)return;n.putInt("Phase",win?3:5);n.putBoolean("Won",win);p.playNotifySound(win?SoundEvents.PLAYER_LEVELUP:SoundEvents.NOTE_BLOCK_BASS.value(),SoundSource.PLAYERS,.65F,win?1.2F:.7F);clear(p,s);restore(p,s);restoreBattle(p,s);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(center(p,s)));ActivityScenery.title(p,win?"WINNER!":"ELIMINATED",win?"Claim your prize at the gold podium.":"The return portal is open.");if(win){var at=prizePosition(center(p,s));var prize=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.PRIZE_PLATE,at,760);prize.food(0,new ItemStack(Items.DIAMOND));prize.setCustomName(Component.literal("Right-click: Winner's prize"));prize.setCustomNameVisible(true);}ActivityWorld.ensureExit(p,s);OtherMotherLoop.changed(p);}
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){if(prop.slot!=760)return false;if(!active(p,s)||state(s).getInt("Phase")!=3)return true;state(s).putInt("Phase",4);OtherMotherLoop.changed(p);prop.discard();DesertTempleActivity.jackpot(p,s);ActivityWorld.ensureExit(p,s);return true;}
    static void clear(ServerPlayer p,VisitData.Scene s){var bar=BARS.remove(p.getUUID());if(bar!=null)bar.removeAllPlayers();for(var e:p.serverLevel().getEntitiesOfClass(Entity.class,bounds(center(p,s),s.loop.activity).inflate(12),e->ActivityScenery.owned(e,s))){if(e instanceof Snowball)e.discard();if(e instanceof FishingContestNpcEntity npc){npc.getNavigation().stop();npc.setNoAi(true);}}}
    static void restoreBattle(ServerPlayer p,VisitData.Scene s){var n=state(s);if(!n.getBoolean("BattleAbilities"))return;n.putBoolean("BattleAbilities",false);p.getAbilities().mayfly=n.getBoolean("BattleMayfly");p.getAbilities().flying=n.getBoolean("BattleFlying");p.onUpdateAbilities();int elapsed=(int)Math.max(0,OtherMotherLoop.now(p)-n.getLong("EffectsAt"));for(var tag:n.getList("BattleEffects",10)){var effect=MobEffectInstance.load((CompoundTag)tag);if(effect!=null&&(effect.isInfiniteDuration()||effect.getDuration()>elapsed))p.addEffect(new MobEffectInstance(effect.getEffect(),effect.isInfiniteDuration()?-1:effect.getDuration()-elapsed,effect.getAmplifier(),effect.isAmbient(),effect.isVisible()));}n.remove("BattleEffects");}
    private SnowballBattleActivity(){}
}
