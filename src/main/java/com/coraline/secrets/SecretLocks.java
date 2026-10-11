package com.coraline.secrets;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;

/** A timed pin mechanism, evaluated using server time and an expiring capability. */
public final class SecretLocks {
    private record Attempt(UUID nonce,int room,long start,int pins,int completed,double speed,double window,long last){}
    private static final Map<UUID,Attempt> ACTIVE=new HashMap<>();
    public static void open(ServerPlayer p,int room){var s=SecretTransactions.state(p);var rooms=SecretWorld.rooms(s);if(room<0||room>=rooms.size())return;int tier=rooms.get(room).getInt("Tier");if(SecretRegistry.tier(s.value(0))<tier){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Code Knowledge required: "+new String[]{"0.00","0.33","0.66","1.00"}[tier]),true);return;}
        if(s.unlocked.contains(room))return;double counter=s.counter(0);var a=new Attempt(UUID.randomUUID(),room,p.serverLevel().getGameTime(),2+tier+(int)(counter*2),0,.035+tier*.012+counter*.015,.27-.025*tier-.06*counter,0);ACTIVE.put(p.getUUID(),a);send(p,a);}
    private static void send(ServerPlayer p,Attempt a){var n=new CompoundTag();n.putBoolean("Lock",true);n.putUUID("Nonce",a.nonce);n.putLong("Start",a.start);n.putInt("Pins",a.pins);n.putInt("Completed",a.completed);n.putDouble("Speed",a.speed);n.putDouble("Window",a.window);SecretsNetwork.send(p,n);}
    public static void pin(ServerPlayer p,UUID nonce){var a=ACTIVE.get(p.getUUID());if(a==null||!a.nonce.equals(nonce)||!SecretsRuntime.active(p))return;long now=p.serverLevel().getGameTime();var s=SecretTransactions.state(p);var rooms=SecretWorld.rooms(s);if(a.room>=rooms.size())return;var at=BlockPos.of(rooms.get(a.room).getLong("Pos"));if(now-a.start>1200||p.distanceToSqr(at.offset(0,1,-5).getCenter())>36||SecretRegistry.tier(s.value(0))<rooms.get(a.room).getInt("Tier")){ACTIVE.remove(p.getUUID());return;}if(now-a.last<6)return;
        double cursor=(Math.sin((now-a.start)*a.speed)+1)/2;boolean hit=Math.abs(cursor-.5)<=a.window/2;int done=hit?a.completed+1:Math.max(0,a.completed-1);
        p.playNotifySound(hit?net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get():net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.get(),net.minecraft.sounds.SoundSource.BLOCKS,.45F,hit?1.3F:.7F);
        if(done>=a.pins){unlock(p,a.room);ACTIVE.remove(p.getUUID());SecretTransactions.dirty(p);SecretsNetwork.open(p,false,false);}else {var next=new Attempt(a.nonce,a.room,a.start,a.pins,done,a.speed,a.window,now);ACTIVE.put(p.getUUID(),next);send(p,next);}}
    public static void unlock(ServerPlayer p,int index){var s=SecretTransactions.state(p);var rooms=SecretWorld.rooms(s);if(index<0||index>=rooms.size())return;s.unlocked.add(index);var at=BlockPos.of(rooms.get(index).getLong("Pos"));for(int y=0;y<3;y++)SecretWorld.place(p,at.offset(0,y,-5),Blocks.AIR);MotherSearch.observed(p,"magical_locks",at);SecretTransactions.dirty(p);}
    public static void clear(UUID id){ACTIVE.remove(id);}
    private SecretLocks(){}
}
