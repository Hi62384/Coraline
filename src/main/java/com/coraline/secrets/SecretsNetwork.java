package com.coraline.secrets;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import com.coraline.feast.*;

public final class SecretsNetwork {
    public record Snapshot(CompoundTag data){}
    public record Request(UUID nonce,String action,String value){}
    private record Session(UUID nonce,boolean debug,boolean journal,long expires){}
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<UUID,Long> LAST=new HashMap<>();
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","secrets"),()->"1","1"::equals,"1"::equals);
    public static void register(){
        CHANNEL.messageBuilder(Snapshot.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeNbt(m.data)).decoder(b->new Snapshot(b.readNbt())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.SecretJournalScreen.receive(m.data))).add();
        CHANNEL.messageBuilder(Request.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUUID(m.nonce);b.writeUtf(m.action,48);b.writeUtf(m.value,4096);}).decoder(b->new Request(b.readUUID(),b.readUtf(48),b.readUtf(4096))).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)request(p,m);}).add();
    }
    public static void send(ServerPlayer p,CompoundTag n){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(n));}
    public static void request(UUID nonce,String action,String value){CHANNEL.sendToServer(new Request(nonce,action,value));}
    public static void open(ServerPlayer p,boolean debug,boolean journal){if(debug&&!p.hasPermissions(2))return;var s=SecretTransactions.state(p);if(s==null){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Begin an Other World storyline before using the Secrets Debugger."),true);return;}if(!debug&&!SecretsRuntime.active(p))return;if(journal&&!SecretsRuntime.atJournal(p))return;var session=new Session(UUID.randomUUID(),debug,journal,OtherMotherLoop.now(p)+12000);SESSIONS.put(p.getUUID(),session);refresh(p,session);}
    private static void refresh(ServerPlayer p,Session session){var s=SecretTransactions.state(p);var n=new CompoundTag();n.putUUID("Nonce",session.nonce);n.putBoolean("Debug",session.debug);n.putBoolean("AtJournal",session.journal&&SecretsRuntime.atJournal(p));n.put("Journal",SecretsState.strings(s.journal));n.putIntArray("Attributes",SecretRegistry.totals(s.journal));n.putInt("Hiding",s.hiding);n.putInt("Reward",s.pendingReward);n.put("Abilities",SecretsState.strings(SecretRegistry.abilities(s.journal)));n.putLong("Player",p.blockPosition().asLong());
        var carry=new ArrayList<String>();for(int i=0;i<p.getInventory().getContainerSize();i++){String id=SecretTransactions.evidence(p,p.getInventory().getItem(i));if(!id.isEmpty())carry.add(id);}n.put("Carried",SecretsState.strings(carry));
        if(s.value(3)>=66||session.debug){n.put("Rooms",s.world.getList("Rooms",10).copy());if(s.value(3)>=100||session.debug){var mother=MotherSearch.mother(p);if(mother!=null)n.putLong("Mother",mother.blockPosition().asLong());}}
        if(session.debug){var scene=OtherMotherLoop.scene(p);n.putFloat("Trust",scene.loop.trust);n.putFloat("Suspicion",scene.loop.suspicion);n.put("Exposed",SecretsState.strings(s.exposed));n.put("Suspected",SecretsState.strings(s.suspected));n.putString("Conversation",Reconciliation.stage(p));n.putBoolean("ProposalPending",scene.loop.phase==OtherMotherLoopState.Phase.PROPOSAL||scene.loop.phase==OtherMotherLoopState.Phase.WARNING);n.putString("Search",s.search.name());n.putLong("Target",s.target.asLong());n.putLong("Pending",s.pendingTicks);n.putDouble("Inspection",s.inspectionProgress);n.putDouble("Sight",30*(1-.5*s.effective(2))*(1-s.soulWeakening()));for(int i=0;i<6;i++){n.putDouble("Counter"+i,s.counter(i));n.putDouble("Override"+i,s.overrides[i]);n.putDouble("Effective"+i,s.effective(i));}}
        send(p,n);
    }
    private static void request(ServerPlayer p,Request r){if(r.action.equals("pin")){SecretLocks.pin(p,r.nonce);return;}var session=SESSIONS.get(p.getUUID());if(session==null||!session.nonce.equals(r.nonce)||OtherMotherLoop.now(p)>session.expires||session.debug&&!p.hasPermissions(2))return;var s=SecretTransactions.state(p);if(s==null)return;long now=OtherMotherLoop.now(p);if(now-LAST.getOrDefault(p.getUUID(),-10L)<2)return;LAST.put(p.getUUID(),now);
        boolean journal=session.journal&&SecretsRuntime.atJournal(p);var ids=new LinkedHashSet<String>();for(String id:r.value.split(","))if(SecretRegistry.ALL.containsKey(id))ids.add(id);
        switch(r.action){
            case "refresh"->{}
            case "move_journal"->{if(journal){JournalPlacement.begin(p);return;}}
            case "deposit"->{if(journal)for(String id:ids)SecretTransactions.deposit(p,id);}
            case "delete"->{if(journal)for(String id:ids)SecretTransactions.delete(p,id);}
            case "blackmail"->{if(journal)SecretTransactions.makeBlackmail(p,ids);}
            case "reward"->{if(SecretsRuntime.active(p))try{SecretTransactions.reward(p,Integer.parseInt(r.value));}catch(NumberFormatException ignored){}}
            case "ability"->{if(SecretsRuntime.active(p))SecretsRuntime.ability(p,r.value);}
            default->{if(!session.debug)return;debug(p,r.action,r.value,ids);}
        }
        SecretTransactions.dirty(p);refresh(p,session);
    }
    private static void debug(ServerPlayer p,String action,String value,Set<String> ids){var s=SecretTransactions.state(p);try{switch(action){
        case "scold_preview"->{var parts=value.split(",");if(parts.length==2)Reconciliation.preview(p,Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));}
        case "scold_event"->Reconciliation.debugEvent(p,Integer.parseInt(value));
        case "voice_preview"->{var parts=value.split(",");if(parts.length==3)SecretDialogue.preview(p,Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));}
        case "voice_event"->{if(!SecretsRuntime.active(p))return;int event=Integer.parseInt(value);if(event<0||event>5)return;if(event<3){var m=MotherSearch.mother(p);if(m!=null){m.teleportTo(p.getX()+2,p.getY(),p.getZ());MotherSearch.catchPlayer(p);}}else for(int i=0;i<p.getInventory().getContainerSize();i++)if(SecretTransactions.offer(p,p.getInventory().getItem(i),event==4?1:event==5?0:-1))break;}
        case "give"->{for(String id:ids)SecretTransactions.issue(p,id);}
        case "give_all"->{for(String id:SecretRegistry.ALL.keySet())SecretTransactions.issue(p,id);}
        case "give_loop"->{var loop=SecretRegistry.LOOPS.get(value);if(loop!=null)for(String id:loop.secrets())SecretTransactions.issue(p,id);}
        case "deposit_debug"->s.journal.addAll(ids);
        case "remove_debug"->s.journal.removeAll(ids);
        case "clear"->s.journal.clear();
        case "expose"->s.exposed.addAll(ids);
        case "unaware"->{s.exposed.removeAll(ids);s.suspected.removeAll(ids);}
        case "suspect"->s.suspected.addAll(ids);
        case "expose_all"->s.exposed.addAll(SecretRegistry.ALL.keySet());
        case "forget"->{s.exposed.clear();s.suspected.clear();}
        case "override"->{var values=value.split(",");if(values.length!=8)return;double[] v=new double[8];for(int i=0;i<8;i++){v[i]=Double.parseDouble(values[i]);if(!Double.isFinite(v[i])||v[i]<0||v[i]>1)return;}OtherMotherLoop.relationship(p,(float)v[0],(float)v[1]);for(int i=0;i<6;i++)s.overrides[i]=v[i+2];}
        case "natural"->Arrays.fill(s.overrides,-1);
        case "hide"->s.hiding=Math.max(0,Math.min(5,Integer.parseInt(value)));
        case "inspect"->{if(SecretsRuntime.active(p))SecretsRuntime.inspect(p,Integer.parseInt(value));}
        case "discover"->SecretTransactions.discoverJournal(p);
        case "inspection_time"->{double progress=Double.parseDouble(value);if(Double.isFinite(progress))s.inspectionProgress=Math.max(0,Math.min(1,progress));}
        case "search"->{s.search=SecretsState.Search.SEARCHING;s.target=p.blockPosition();}
        case "stop"->s.search=SecretsState.Search.NORMAL;
        case "miss"->MotherSearch.missed(p);
        case "delay"->s.pendingTicks=Math.max(0,Math.min(12000,Long.parseLong(value)));
        case "chase"->{s.search=SecretsState.Search.DIRECT_CHASE;s.target=p.blockPosition();}
        case "noise"->{if(SecretsRuntime.active(p))MotherSearch.noise(p,p.blockPosition(),90,"debug");}
        case "doll"->{if(SecretsRuntime.active(p))SecretSpies.doll(p,p.blockPosition().north(3));}
        case "rat"->{if(SecretsRuntime.active(p))SecretSpies.rat(p,p.blockPosition().north(3));}
        case "rat_alert"->{if(SecretsRuntime.active(p))for(var rat:p.serverLevel().getEntitiesOfClass(com.coraline.otherworld.MouseEntity.class,p.getBoundingBox().inflate(24),e->e.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("SecretOwner")))){rat.getPersistentData().putBoolean("Alerted",true);rat.setGlowingTag(true);break;}}
        case "clear_spies"->{if(SecretsRuntime.active(p))for(var spy:p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class,p.getBoundingBox().inflate(64),e->e.getPersistentData().hasUUID("SecretOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("SecretOwner"))&&!e.getPersistentData().getString("SpyKind").isEmpty())){SecretSpies.removeRat(p,spy);spy.discard();}}
        case "report"->{if(SecretsRuntime.active(p))SecretSpies.report(p);}
        case "mother"->{var m=MotherSearch.mother(p);if(m!=null)m.teleportTo(p.getX()+2,p.getY(),p.getZ());}
        case "trap"->{if(SecretsRuntime.active(p))SecretsRuntime.trap(p,p.blockPosition(),Math.floorMod(Integer.parseInt(value),3));}
        case "teleport"->{if(!SecretsRuntime.active(p))return;int i=Integer.parseInt(value);var rooms=SecretWorld.rooms(s);if(i<0||i>=rooms.size())return;var at=net.minecraft.core.BlockPos.of(rooms.get(i).getLong("Pos")).north(7);p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);}
        case "generate_tier"->{if(!SecretsRuntime.active(p))return;int target=(Math.max(0,Math.min(3,Integer.parseInt(value)))+1)*3;for(int count=0;count<12&&SecretWorld.rooms(s).size()<target;count++)SecretWorld.buildNext(p);}
        case "unlock_debug"->{if(SecretsRuntime.active(p))SecretLocks.unlock(p,Integer.parseInt(value));}
        case "reveal_debug"->{if(SecretsRuntime.active(p))s.world.putLong("DebugRevealUntil",OtherMotherLoop.now(p)+400);}
        case "regenerate"->{if(!SecretsRuntime.active(p))return;var realm=SecretWorld.realm(p);if(realm==null||realm.porch==null)return;p.teleportTo(p.serverLevel(),realm.porch.getX()+.5,realm.porch.getY(),realm.porch.getZ()+.5,p.getYRot(),0);s.visit=-1;SecretWorld.begin(p);}
        case "blackmail_debug"->SecretTransactions.makeBlackmail(p,ids);
        case "accept","reject"->{if(SecretsRuntime.active(p))for(int i=0;i<p.getInventory().getContainerSize();i++)if(SecretTransactions.offer(p,p.getInventory().getItem(i),action.equals("accept")?1:0))break;}
        default->{}
    }}catch(NumberFormatException ignored){} }
    public static void clear(UUID id){SESSIONS.remove(id);LAST.remove(id);}
    private SecretsNetwork(){}
}
