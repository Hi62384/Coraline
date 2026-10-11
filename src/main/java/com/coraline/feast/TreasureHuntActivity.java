package com.coraline.feast;
import static com.coraline.feast.ExpeditionActivities.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.*;
import com.coraline.registry.ModItems;

public final class TreasureHuntActivity {
    private static final Map<UUID,ServerBossEvent> BARS=new HashMap<>();
    static boolean active(ServerPlayer p,VisitData.Scene s){return owns(p,s)&&s.loop.activity==OtherMotherActivities.Activity.TREASURE_HUNT&&state(s).getInt("Phase")>0&&state(s).getInt("Phase")<4;}
    static void build(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){var route=TreasureRoute.generate(state(s).getLong("Seed"));state(s).putIntArray("Route",route);TreasureScenery.build(l,c,state(s).getLong("Seed"));}
    static void begin(ServerPlayer p,VisitData.Scene s){var n=state(s);borrow(p,s,ModItems.TREASURE_NOTE.get());n.putInt("Step",0);n.putLong("End",OtherMotherLoop.now(p)+6000);n.putBoolean("HuntAbilities",true);n.putBoolean("HuntMayfly",p.getAbilities().mayfly);n.putBoolean("HuntFlying",p.getAbilities().flying);ActivityWorld.ensureExit(p,s);ActivityScenery.title(p,"TREASURE HUNT","Read the seal gardens: petals and standing lamps.");openNote(p);}
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){var n=state(s);if(!bounds(c,s.loop.activity).inflate(-1).contains(p.position()))ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c));if(!active(p,s))return;
        p.stopFallFlying();if(p.getAbilities().mayfly||p.getAbilities().flying){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();}
        long left=n.getLong("End")-OtherMotherLoop.now(p);if(left<=0){finish(p,s,false);return;}if(left==1200||left==600||left==200){ActivityScenery.title(p,"",left/20+" seconds remaining");p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(),SoundSource.PLAYERS,.5F,1.2F);}
        if(p.tickCount%10==0){var bar=BARS.computeIfAbsent(s.owner,k->new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS));bar.addPlayer(p);bar.setProgress(left/6000F);bar.setName(Component.literal(String.format("%d:%02d  •  Clue %d / 6",left/1200,left/20%60,Math.min(6,n.getInt("Step")+1))));}
    }
    public static void openNote(ServerPlayer p){var s=OtherMotherLoop.active(p);if(!active(p,s))return;var n=state(s);String text=n.getInt("Phase")==2?"The cellar is open. Descend the stone stairs and claim the treasure before time runs out.":TreasureRoute.clue(n.getLong("Seed"),n.getIntArray("Route"),n.getInt("Step"));ExpeditionNetwork.note(p,"Treasure clue "+Math.min(6,n.getInt("Step")+1)+" / 6",text);}
    public static void interactBlock(ServerPlayer p,BlockPos at){var s=OtherMotherLoop.active(p);if(!active(p,s)||p.distanceToSqr(Vec3.atCenterOf(at))>36||OtherMotherLoop.now(p)>=state(s).getLong("End"))return;var n=state(s);var c=center(p,s);var route=n.getIntArray("Route");int step=n.getInt("Step");
        for(int id=0;id<TreasureRoute.LANDMARKS.size();id++){var m=TreasureRoute.LANDMARKS.get(id);if(at.equals(m.note(c))||at.equals(m.note(c).south())){if(n.getInt("Phase")!=1||route[step]!=id||step>=5){p.displayClientMessage(Component.literal("No matching seal here. Read your current Treasure Note."),true);return;}advance(p,s,id);return;}
            if(at.equals(m.at(c).offset(-7,0,7))){if(OtherMotherLoop.now(p)<n.getLong("DecoyUntil"))return;n.putLong("DecoyUntil",OtherMotherLoop.now(p)+30);p.displayClientMessage(Component.literal("Only a puff of dust. Not this chest."),true);p.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(),SoundSource.PLAYERS,.45F,.7F);p.serverLevel().sendParticles(ParticleTypes.POOF,at.getX()+.5,at.getY()+.8,at.getZ()+.5,8,.3,.2,.3,.03);return;}}
        var door=TreasureScenery.vaultDoor(c,route[5]);if(at.distSqr(door)<=2&&step==5&&n.getInt("Phase")==1){n.putInt("Phase",2);box(p.serverLevel(),door,0,0,0,0,1,0,Blocks.AIR);var pos=TreasureScenery.prize(c,route[5]);var prop=LoopProps.spawn(p.serverLevel(),s,ScenePropEntity.PRIZE_PLATE,Vec3.atBottomCenterOf(pos),780);prop.food(0,new ItemStack(Items.DIAMOND));prop.setCustomName(Component.literal("Claim the hidden treasure"));prop.setCustomNameVisible(true);p.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,.9F,.7F);p.serverLevel().sendParticles(ParticleTypes.END_ROD,door.getX()+.5,door.getY()+1,door.getZ()+.5,20,.4,.7,.4,.02);OtherMotherLoop.changed(p);openNote(p);return;}
        if(p.getMainHandItem().is(ModItems.TREASURE_NOTE.get()))openNote(p);
    }
    static boolean advance(ServerPlayer p,VisitData.Scene s,int id){var n=state(s);if(!active(p,s)||n.getInt("Phase")!=1||n.getInt("Step")>=5||n.getIntArray("Route")[n.getInt("Step")]!=id||OtherMotherLoop.now(p)>=n.getLong("End"))return false;n.putInt("Step",n.getInt("Step")+1);OtherMotherLoop.changed(p);p.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.PLAYERS,.6F,1.3F);openNote(p);return true;}
    static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){if(prop.slot!=780)return false;if(!active(p,s)||state(s).getInt("Phase")!=2||state(s).getInt("Step")!=5||OtherMotherLoop.now(p)>=state(s).getLong("End"))return true;prop.discard();finish(p,s,true);return true;}
    static void finish(ServerPlayer p,VisitData.Scene s,boolean won){var n=state(s);if(n.getInt("Phase")>=4)return;n.putInt("Phase",won?4:5);p.playNotifySound(won?SoundEvents.PLAYER_LEVELUP:SoundEvents.NOTE_BLOCK_BASS.value(),SoundSource.PLAYERS,.7F,won?1.2F:.7F);OtherMotherLoop.changed(p);clear(p,s);restore(p,s);ActivityScenery.title(p,won?"TREASURE FOUND!":"TIME'S UP",won?"The hidden vault is yours.":"The treasure remains hidden.");if(won)DesertTempleActivity.jackpot(p,s);else ActivityScenery.teleport(p,Vec3.atBottomCenterOf(center(p,s)));ActivityWorld.ensureExit(p,s);}
    static void clear(ServerPlayer p,VisitData.Scene s){var b=BARS.remove(p.getUUID());if(b!=null)b.removeAllPlayers();ExpeditionNetwork.note(p,"","");var n=state(s);if(n.getBoolean("HuntAbilities")){n.putBoolean("HuntAbilities",false);p.getAbilities().mayfly=n.getBoolean("HuntMayfly");p.getAbilities().flying=n.getBoolean("HuntFlying");p.onUpdateAbilities();}}
    private TreasureHuntActivity(){}
}
