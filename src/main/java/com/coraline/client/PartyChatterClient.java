package com.coraline.client;

import com.coraline.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** One positional murmur for the current real conversation speaker, never a room loop. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class PartyChatterClient {
    private static final Map<Integer,Murmur> active=new HashMap<>();
    public static void accept(int id,boolean start){
        var mc=Minecraft.getInstance();if(!start){var old=active.remove(id);if(old!=null)old.finish();return;}
        if(mc.level==null||active.containsKey(id))return;var actor=mc.level.getEntity(id);if(actor==null)return;
        var sound=new Murmur(actor);active.put(id,sound);mc.getSoundManager().play(sound);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END)active.entrySet().removeIf(entry->entry.getValue().isStopped());}
    private static final class Murmur extends AbstractTickableSoundInstance {
        final Entity actor;final net.minecraft.world.level.Level level;
        Murmur(Entity actor){super(ModSounds.PARTY_CHATTER.get(),SoundSource.NEUTRAL,RandomSource.create());this.actor=actor;level=actor.level();looping=true;delay=0;relative=false;attenuation=Attenuation.LINEAR;volume=0;pitch=.94F+random.nextFloat()*.12F;x=actor.getX();y=actor.getEyeY();z=actor.getZ();}
        @Override public boolean canStartSilent(){return true;}
        void finish(){stop();}
        @Override public void tick(){var mc=Minecraft.getInstance();if(mc.level!=level||mc.player==null||!mc.player.isAlive()||!actor.isAlive()||actor.isRemoved()){stop();return;}x=actor.getX();y=actor.getEyeY();z=actor.getZ();
            // OpenAL already provides distance attenuation. Squaring it a second
            // time made quiet source recordings effectively inaudible at a table.
            boolean speech=VisitAudioClient.voicePlaying()||VisitEventClient.dialogueOpen();
            boolean occluded=level.clip(new net.minecraft.world.level.ClipContext(mc.player.getEyePosition(),actor.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS;
            volume=(speech?.55F:2.2F)*(occluded?.25F:1F);}
    }
    private PartyChatterClient(){}
}
