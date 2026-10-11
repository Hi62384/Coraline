package com.coraline.parents;

import com.coraline.config.CoralineConfig;
import com.coraline.registry.ModSounds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.sounds.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Saved, server-owned sequence: sounds are positional, never attached to a dying entity. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ParentDeath {
    public static void begin(ParentEntity first,FamilyData.Family f){
        if(f.dead)return;
        var server=first.getServer();
        for(var level:server.getAllLevels())for(var id:new java.util.UUID[]{f.mom,f.dad}){
            if(id!=null&&level.getEntity(id) instanceof ParentEntity p){
                if(p.isMom()){f.momPos=p.blockPosition();f.momDim=level.dimension().location().toString();}
                else{f.dadPos=p.blockPosition();f.dadDim=level.dimension().location().toString();}
                p.freezeForDeath();
            }
        }
        f.dead=true;f.deathPhase=1;f.firstDeathMom=first.isMom();f.deathStartedAt=server.overworld().getGameTime();
        play(server,f,f.firstDeathMom);
        FamilyData.get(server).setDirty();
        com.coraline.feast.FamilyVisitReset.endFamily(server,f);
        com.coraline.otherworld.DoorData.get(server).endFamily(f.owner);
        com.coraline.doll.DollBondData.get(server).endFamily(f.owner);
    }
    private static SoundEvent sound(boolean mom){return mom?ModSounds.MOM_DEATH.get():ModSounds.DAD_DEATH.get();}
    private static void play(MinecraftServer server,FamilyData.Family f,boolean mom){
        var level=FamilyData.level(server,mom?f.momDim:f.dadDim);var pos=mom?f.momPos:f.dadPos;
        if(level!=null)level.playSound(null,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,sound(mom),SoundSource.NEUTRAL,CoralineConfig.VOICE_VOLUME.get().floatValue(),1);
    }
    private static ParentEntity find(MinecraftServer server,FamilyData.Family f,boolean mom){
        var level=FamilyData.level(server,mom?f.momDim:f.dadDim);var pos=mom?f.momPos:f.dadPos;var id=mom?f.mom:f.dad;
        if(level==null||id==null)return null;
        level.getChunkSource().addRegionTicket(TicketType.PORTAL,new ChunkPos(pos),2,pos);
        level.getChunkAt(pos);
        return level.getEntity(id) instanceof ParentEntity p?p:null;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var server=event.getServer();var data=FamilyData.get(server);long now=server.overworld().getGameTime();
        for(var f:data.families.values())if(f.deathPhase!=0){
            var mom=find(server,f,true);var dad=find(server,f,false);
            if(mom!=null&&mom.isAlive())mom.freezeForDeath();if(dad!=null&&dad.isAlive())dad.freezeForDeath();
            boolean currentMom=f.deathPhase==1?f.firstDeathMom:!f.firstDeathMom;
            var current=currentMom?mom:dad;long elapsed=now-f.deathStartedAt;
            int deathDelay=currentMom?CoralineConfig.DEATH_DELAY.get():CoralineConfig.DAD_DEATH_DELAY.get();
            if(elapsed>=deathDelay&&current!=null&&current.isAlive())current.finishScriptedDeath();
            // Any first parent loaded late must still complete its scheduled death.
            var first=f.firstDeathMom?mom:dad;
            if(f.deathPhase==2&&first!=null&&first.isAlive())first.finishScriptedDeath();
            if(elapsed>=Math.max(deathDelay+1,ParentVoices.length(sound(currentMom)))){
                if(f.deathPhase==1){f.deathPhase=2;f.deathStartedAt=now;play(server,f,!f.firstDeathMom);}
                else f.deathPhase=0;
                data.setDirty();
            }
        }
    }
}
