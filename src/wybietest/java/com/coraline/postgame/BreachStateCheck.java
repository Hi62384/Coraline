package com.coraline.postgame;

import java.util.*;
import java.nio.file.*;
import java.util.function.Consumer;
import net.minecraft.nbt.*;

/** Real production arithmetic/receipt kernel and compressed NBT only. No
 * Minecraft bootstrap, Moonstone spending, device, drill or boss is simulated. */
public final class BreachStateCheck {
    private static int checks,files;
    private static Path directory;
    private static final UUID EPOCH=UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static BreachState fresh(){var s=BreachState.fresh();require(s.activate(EPOCH,0,BreachState.Rules.defaults()),"Valid fresh activation failed");return s;}
    private static boolean sameTag(Tag a,Tag b){
        if(a==null||b==null)return a==b;if(a.getId()!=b.getId())return false;
        // Vanilla DoubleTag/FloatTag equality does not equate NaN after a disk
        // read. Preserve the deliberately invalid value/type in this fixture,
        // rather than mistaking that Java comparison quirk for a state reset.
        if(a instanceof DoubleTag x&&b instanceof DoubleTag y)return Double.doubleToLongBits(x.getAsDouble())==Double.doubleToLongBits(y.getAsDouble());
        if(a instanceof FloatTag x&&b instanceof FloatTag y)return Float.floatToIntBits(x.getAsFloat())==Float.floatToIntBits(y.getAsFloat());
        if(a instanceof CompoundTag x&&b instanceof CompoundTag y){if(!x.getAllKeys().equals(y.getAllKeys()))return false;for(String key:x.getAllKeys())if(!sameTag(x.get(key),y.get(key)))return false;return true;}
        if(a instanceof ListTag x&&b instanceof ListTag y){if(x.size()!=y.size()||x.getElementType()!=y.getElementType())return false;for(int i=0;i<x.size();i++)if(!sameTag(x.get(i),y.get(i)))return false;return true;}
        return a.equals(b);
    }
    private static BreachState disk(BreachState s)throws Exception{
        var path=directory.resolve("breach-"+files+++".dat");var n=s.save();NbtIo.writeCompressed(n,path.toFile());var copy=BreachState.load(NbtIo.readCompressed(path.toFile()));
        require(copy.blocker().equals(s.blocker())&&sameTag(copy.save(),n),"Compressed NBT changed state/provenance");return copy;
    }
    private static void reject(CompoundTag original,Consumer<CompoundTag> corrupt)throws Exception{
        var n=original.copy();corrupt.accept(n);var s=BreachState.load(n);require(!s.blocker().isEmpty()&&!s.active(),"Malformed/future journal remained active: "+n);
        require(!s.activate(EPOCH,0,BreachState.Rules.defaults())&&!s.advance(Long.MAX_VALUE)&&!s.addFuel(1,0),"Malformed state was reset/refueled/advanced");
        require(s.complete(BreachState.Kind.RIFT_DRILL,UUID.randomUUID(),UUID.randomUUID(),100000,0)==BreachState.Resolution.REFUSED,"Malformed state resolved a drill");
        require(n.equals(s.save()),"Rejected data was overwritten");disk(s);
    }
    public static void main(String[] args)throws Exception{
        directory=Files.createTempDirectory(Path.of("build-character-cast"),"breach-state-");
        var dormant=BreachState.fresh();require(!dormant.active()&&!dormant.advance(24000)&&!dormant.addFuel(1,0),"Read-only dormant state activated itself");disk(dormant);
        var unpowered=fresh();unpowered.advance(24000);require(unpowered.value()==120000&&unpowered.fuel()==0&&!unpowered.containmentEnabled(),"Unpowered daily rate/fuel gate failed");disk(unpowered);
        var powered=fresh();require(powered.addFuel(1,0),"Kernel fuel input rejected");powered.advance(24000);require(powered.value()==30000&&powered.fuel()==24000&&powered.containmentEnabled(),"One Moonstone is not two days at the slow rate");
        powered.advance(48000);require(powered.value()==60000&&!powered.powered(),"Fuel exhaustion timing was wrong");powered.advance(72000);require(powered.value()==180000,"Unpowered rate did not start at the actual exhaustion boundary");disk(powered);
        var edge=fresh();edge.addFuel(1,0);edge.advance(47999);require(edge.fuel()==1&&edge.containmentEnabled(),"Last powered game tick disabled containment early");
        require(edge.complete(BreachState.Kind.RIFT_DRILL,UUID.randomUUID(),UUID.randomUUID(),1,47999)==BreachState.Resolution.REDUCED,"Remaining fuel tick did not permit containment");
        require(edge.complete(BreachState.Kind.RIFT_DRILL,UUID.randomUUID(),UUID.randomUUID(),1,48000)==BreachState.Resolution.UNPOWERED&&!edge.containmentEnabled(),"Completion at the exhaustion boundary bypassed fuel");disk(edge);
        var whole=fresh();var split=fresh();whole.configure(new BreachState.Rules(12345,56789,48000),0);split.configure(new BreachState.Rules(12345,56789,48000),0);whole.addFuel(1,0);split.addFuel(1,0);whole.advance(100003);
        for(int now=1;now<=100003;now+=37){require(split.advance(now),"Valid monotonically increasing game tick rejected");require(split.value()>=0&&split.value()<=BreachState.MAX,"Split growth escaped range");}split.advance(100003);
        require(whole.save().equals(split.save()),"Clock partition/chunk order changed fuel, fractional growth or state");disk(split);
        var tiny=fresh();tiny.configure(new BreachState.Rules(1,2,24000),0);for(int i=1;i<=24000;i++)tiny.advance(i);require(tiny.value()==2&&tiny.save().getInt("Remainder")==0,"Fractional per-tick rate was lost to rounding");disk(tiny);
        var change=fresh();change.addFuel(1,0);change.configure(new BreachState.Rules(50000,200000,72000),12000);require(change.value()==15000&&change.fuel()==36000,"Config change retroactively changed elapsed rate or existing fuel");change.advance(24000);require(change.value()==40000,"New config rate was not applied after its boundary");change.addFuel(5,24000);require(change.fuel()==384000,"Refueling did not use new per-stone duration/accumulate existing fuel");disk(change);
        var objectives=fresh();objectives.advance(48000);var id=UUID.randomUUID();var receipt=UUID.randomUUID();
        require(objectives.complete(BreachState.Kind.RIFT_DRILL,id,receipt,100000,48000)==BreachState.Resolution.UNPOWERED&&objectives.value()==240000,"Unpowered drill reduced Breach");objectives.addFuel(1,48000);
        require(objectives.complete(BreachState.Kind.RIFT_DRILL,id,receipt,100000,48000)==BreachState.Resolution.ALREADY_RESOLVED&&objectives.value()==240000,"Refueling enabled a deferred drill payout");
        require(objectives.complete(BreachState.Kind.RIFT_DRILL,id,UUID.randomUUID(),100000,48000)==BreachState.Resolution.CONFLICT,"New receipt repeated an already resolved drill");
        require(objectives.complete(BreachState.Kind.RIFT_DRILL,UUID.randomUUID(),receipt,100000,48000)==BreachState.Resolution.CONFLICT,"Copied receipt authorized a second objective");
        var activeReceipt=UUID.randomUUID();var activeId=UUID.randomUUID();require(objectives.complete(BreachState.Kind.RIFT_DRILL,activeId,activeReceipt,100000,48000)==BreachState.Resolution.REDUCED&&objectives.value()==140000,"Powered valid objective did not reduce once");
        objectives=disk(objectives);require(objectives.complete(BreachState.Kind.RIFT_DRILL,activeId,activeReceipt,100000,48000)==BreachState.Resolution.ALREADY_RESOLVED&&objectives.value()==140000,"Restart repeated a paid objective");
        require(objectives.complete(BreachState.Kind.DIMENSIONAL_SEAM,UUID.randomUUID(),UUID.randomUUID(),200000,48000)==BreachState.Resolution.REDUCED&&objectives.value()==0&&objectives.save().getInt("Remainder")==0,"Objective reduction failed to clamp at zero");disk(objectives);
        for(var kind:BreachState.Kind.values()){
            var s=fresh();s.advance(24000);require(s.complete(kind,UUID.randomUUID(),UUID.randomUUID(),100000,24000)==BreachState.Resolution.UNPOWERED,"Containment category bypassed fuel: "+kind);
            s.addFuel(1,24000);require(s.complete(kind,UUID.randomUUID(),UUID.randomUUID(),100000,24000)==BreachState.Resolution.REDUCED&&s.value()==20000,"Powered category did not use the shared gate: "+kind);disk(s);
        }
        var boss=fresh();boss.advance(24000L*400);require(boss.avatarLocked()&&boss.value()==2000000&&!boss.powered(),"2.0 did not lock without fuel");
        long clock=boss.lastTick();boss.addFuel(1,clock);var lockedReceipt=UUID.randomUUID();require(boss.complete(BreachState.Kind.RIFT_DRILL,UUID.randomUUID(),lockedReceipt,100000,clock)==BreachState.Resolution.BOSS_LOCKED&&boss.value()==2000000,"Fuel bypassed the 2.0 Avatar lock");
        boss.advance(clock+48000);clock=boss.lastTick();require(!boss.powered(),"Boss fixture did not exhaust fuel");var actor=UUID.randomUUID();var victory=UUID.randomUUID();require(!boss.avatarDefeated(actor,victory,clock),"Missing/unbound Avatar was assumed defeated");
        require(boss.bindAvatar(actor,clock)&&boss.bindAvatar(actor,clock)&&!boss.bindAvatar(UUID.randomUUID(),clock),"Avatar binding was replaced/reallocated");boss=disk(boss);
        require(!boss.avatarDefeated(actor,lockedReceipt,clock)&&boss.avatarLocked(),"Ordinary objective receipt authorized Avatar victory");
        require(!boss.avatarDefeated(UUID.randomUUID(),victory,clock)&&boss.avatarLocked(),"Foreign boss death unlocked Breach");require(boss.avatarDefeated(actor,victory,clock)&&boss.value()==1500000&&!boss.avatarLocked()&&!boss.powered(),"Actual matching Avatar receipt did not lower to exactly 1.5 without fuel");
        require(!boss.avatarDefeated(actor,victory,clock)&&boss.value()==1500000,"Repeated boss death reduced again");
        require(boss.complete(BreachState.Kind.CORALINE_QUEST,UUID.randomUUID(),victory,100000,clock)==BreachState.Resolution.CONFLICT&&boss.value()==1500000,"Avatar receipt authorized an ordinary quest payout");
        boss=disk(boss);boss.advance(clock+24000L*100);clock=boss.lastTick();require(boss.avatarLocked()&&!boss.bindAvatar(actor,clock),"Previous defeated Avatar was rebound to a new encounter");require(boss.bindAvatar(UUID.randomUUID(),clock),"New encounter could not bind a distinct boss");disk(boss);
        var extreme=fresh();extreme.advance(Long.MAX_VALUE);require(extreme.value()==2000000&&extreme.avatarLocked()&&extreme.fuel()==0,"Huge tick delta overflowed growth or fuel");var before=extreme.save();require(!extreme.advance(0)&&extreme.save().equals(before),"Clock rollback reset progress");disk(extreme);
        var invalidFuel=fresh();var original=invalidFuel.save();require(!invalidFuel.addFuel(0,0)&&!invalidFuel.addFuel(-1,0)&&!invalidFuel.addFuel(65,0)&&invalidFuel.save().equals(original),"Invalid stack-sized fuel mutation accepted");
        var overflow=invalidFuel.save();overflow.putLong("Fuel",Long.MAX_VALUE);var saturated=BreachState.load(overflow);require(!saturated.addFuel(1,0)&&saturated.save().equals(overflow),"Fuel overflow silently truncated/wrapped");disk(saturated);
        var valid=objectives.save();valid.putString("UnknownExtension","preserve");var preserved=BreachState.load(valid);preserved.advance(preserved.lastTick()+1);require(preserved.save().getString("UnknownExtension").equals("preserve"),"Unknown extension erased");disk(preserved);
        var snapshot=preserved.save();snapshot.putInt("Value",0);require(!snapshot.equals(preserved.save()),"Caller snapshot mutated live state");
        reject(valid,n->n.putInt("Version",999));reject(valid,n->n.putDouble("Version",1));reject(valid,n->n.putByte("Activated",(byte)2));reject(valid,n->n.putString("Generation","foreign"));
        reject(valid,n->n.putDouble("Value",Double.NaN));reject(valid,n->n.putInt("Value",-1));reject(valid,n->n.putInt("Value",2000001));reject(valid,n->n.putInt("Remainder",24000));reject(valid,n->n.putLong("Fuel",-1));reject(valid,n->n.putInt("Fuel",100));
        reject(valid,n->n.putLong("LastTick",-1));reject(valid,n->n.putBoolean("AvatarLocked",true));reject(valid,n->n.putUUID("AvatarEntity",UUID.randomUUID()));reject(valid,n->n.putString("Rules","future"));reject(valid,n->n.getCompound("Rules").putInt("Unpowered",1));
        reject(valid,n->n.putString("Objectives","future"));reject(valid,n->n.getCompound("Objectives").putString(UUID.randomUUID().toString(),"bad row"));reject(valid,n->n.getCompound("Objectives").put("bad key",new CompoundTag()));
        reject(valid,n->{var rows=n.getCompound("Objectives");var first=rows.getAllKeys().iterator().next();rows.put(UUID.randomUUID().toString(),rows.getCompound(first).copy());});
        for(var row:valid.getCompound("Objectives").getAllKeys()){
            reject(valid,n->n.getCompound("Objectives").getCompound(row).putString("Receipt","future"));reject(valid,n->n.getCompound("Objectives").getCompound(row).putInt("Applied",-1));reject(valid,n->n.getCompound("Objectives").getCompound(row).putString("Outcome","FUTURE"));
        }
        var inactive=BreachState.fresh().save();reject(inactive,n->n.putUUID("Generation",EPOCH));
        System.out.println("BREACH_FILES="+files+" DIRECTORY="+directory);System.out.println("BREACH_STATE_NBT_OK checks="+checks);
    }
    private BreachStateCheck(){}
}
