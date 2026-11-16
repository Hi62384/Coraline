import com.coraline.palace.PalaceKeyTransaction;
import com.coraline.palace.PalaceKeyCheckpoint;
import java.util.*;
import net.minecraft.nbt.*;

/** Actual serialized NBT/custody journal tests; NOT inventory/disk/gameplay QA. */
public final class PalaceKeySmoke {
    private static int checks;
    private static final UUID OWNER=UUID.randomUUID(),DOOR=UUID.randomUUID(),BUNDLE=UUID.randomUUID(),OTHER=UUID.randomUUID();
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static CompoundTag original(){
        var n=new CompoundTag();n.putString("id","coraline:little_door_story_key");n.putByte("Count",(byte)1);
        var tag=new CompoundTag();tag.putUUID("StoryKeyOwner",OWNER);tag.putUUID("StoryKeyDoor",DOOR);tag.putUUID("PalaceKeyIdentity",UUID.randomUUID());
        tag.putBoolean("LegacyReplacement",true);tag.putString("CustomForeignData","preserve exactly");
        var display=new CompoundTag();display.putString("Name","{\"text\":\"My original key\"}");tag.put("display",display);n.put("tag",tag);
        var caps=new CompoundTag();caps.putString("another_mod:original_data","do not strip");n.put("ForgeCaps",caps);return n;
    }
    private static PalaceKeyTransaction reload(PalaceKeyTransaction tx){
        var saved=tx.save();var result=PalaceKeyTransaction.load(OWNER,DOOR,saved);
        check(result.blocker().isEmpty(),"Valid custody save blocked");check(result.save().equals(saved),"Custody checkpoint changed on reload");return result;
    }
    public static int run(){
        check(PalaceKeyTransaction.prepare(OWNER,DOOR,BUNDLE,null).isEmpty(),"Null original accepted");
        check(PalaceKeyTransaction.prepare(OWNER,DOOR,null,original()).isEmpty(),"Missing physical bundle accepted");
        for(int cycle=0;cycle<1000;cycle++){
            var stack=original();var snapshot=stack.copy();var tx=PalaceKeyTransaction.prepare(OWNER,DOOR,BUNDLE,stack).orElseThrow();
            var receipt=tx.receipt();check(tx.stage()==PalaceKeyTransaction.Stage.PREPARED&&!tx.spendable(OWNER,receipt),"Prepared key became spendable");
            stack.getCompound("tag").putString("CustomForeignData","mutated after capture");
            check(tx.originalSnapshot().equals(snapshot),"Journal aliased original item NBT");
            var copy=tx.originalSnapshot();copy.remove("ForgeCaps");check(tx.originalSnapshot().equals(snapshot),"Returned snapshot mutates stored original");
            tx=reload(tx);
            check(!tx.dispose(OWNER,receipt,BUNDLE),"Key disposed before actual inventory removal");
            check(!tx.removedAndSaved(OTHER,receipt)&&!tx.removedAndSaved(OWNER,OTHER),"Foreign player/stale receipt captured original");
            check(tx.removedAndSaved(OWNER,receipt)&&tx.spendable(OWNER,receipt),"Removed original did not enter escrow");
            check(tx.removedAndSaved(OWNER,receipt),"Idempotent removal acknowledgement rejected");tx=reload(tx);
            check(!tx.spendable(OTHER,receipt)&&!tx.spendable(OWNER,OTHER),"Foreign owner/receipt can spend key");
            check(!tx.dispose(OWNER,receipt,OTHER)&&!tx.dispose(OTHER,receipt,BUNDLE)&&!tx.dispose(OWNER,OTHER,BUNDLE),"Wrong owner/bundle/receipt disposed key");
            check(tx.dispose(OWNER,receipt,BUNDLE)&&!tx.spendable(OWNER,receipt),"Disposed key still spendable");tx=reload(tx);
            check(!tx.removedAndSaved(OWNER,receipt),"Disposed key resurrected by replayed removal");
            check(tx.dispose(OWNER,receipt,BUNDLE),"Idempotent disposal rejected");
            check(tx.originalSnapshot().equals(snapshot),"Original serialized key/capabilities lost after disposal");
        }
        var valid=PalaceKeyTransaction.prepare(OWNER,DOOR,BUNDLE,original()).orElseThrow().save();
        for(String field:new String[]{"Owner","Door","Key","Receipt","Bundle","Version","Stage","Original"}){
            var damaged=valid.copy();damaged.remove(field);blocked(damaged);
            damaged=valid.copy();damaged.putString(field,"wrong type or invalid identifier");blocked(damaged);
        }
        var changed=valid.copy();changed.getCompound("Original").getCompound("tag").putUUID("PalaceKeyIdentity",OTHER);blocked(changed);
        changed=valid.copy();changed.getCompound("Original").getCompound("tag").putUUID("StoryKeyOwner",OTHER);blocked(changed);
        changed=valid.copy();changed.getCompound("Original").putByte("Count",(byte)2);blocked(changed);
        changed=valid.copy();changed.putInt("Version",2);blocked(changed);
        changed=valid.copy();changed.putString("Blocker","existing review");blocked(changed);
        check(!PalaceKeyTransaction.load(OTHER,DOOR,valid).blocker().isEmpty(),"Foreign owner loaded escrow");
        check(!PalaceKeyTransaction.load(OWNER,OTHER,valid).blocker().isEmpty(),"Foreign door loaded escrow");
        for(String field:new String[]{"StoryKeyOwner","StoryKeyDoor","PalaceKeyIdentity"}){
            var bad=original();bad.getCompound("tag").remove(field);check(PalaceKeyTransaction.prepare(OWNER,DOOR,BUNDLE,bad).isEmpty(),"Unbound original accepted: "+field);
        }
        checkpoints();
        return checks;
    }
    private static void checkpoints(){
        var expected=new CompoundTag();expected.putInt("Version",1);expected.putUUID("Owner",OWNER);expected.putUUID("Door",DOOR);expected.put("KeyCustody",original());
        var progression=new CompoundTag();progression.put("PinkPalaceJourney",expected.copy());var main=new CompoundTag();main.put("StoryProgression",progression);
        var scene=new CompoundTag();scene.putUUID("Owner",OWNER);scene.putUUID("Door",DOOR);scene.put("MainLoop",main);
        var scenes=new ListTag();scenes.add(scene);var data=new CompoundTag();data.put("Scenes",scenes);var file=new CompoundTag();file.put("data",data);
        check(PalaceKeyCheckpoint.story(file,OWNER,DOOR,expected),"Actual SavedData layout rejected");
        check(!PalaceKeyCheckpoint.story(file,OTHER,DOOR,expected)&&!PalaceKeyCheckpoint.story(file,OWNER,OTHER,expected),"Foreign owner/door checkpoint accepted");
        var altered=expected.copy();altered.getCompound("KeyCustody").putByte("Count",(byte)2);check(!PalaceKeyCheckpoint.story(file,OWNER,DOOR,altered),"Non-exact story checkpoint accepted");
        scenes.add(scene.copy());check(!PalaceKeyCheckpoint.story(file,OWNER,DOOR,expected),"Duplicate owner checkpoint accepted");scenes.remove(1);
        var foreign=scene.copy();foreign.putUUID("Owner",OTHER);scenes.add(foreign);check(PalaceKeyCheckpoint.story(file,OWNER,DOOR,expected),"Unrelated owner's state interfered");
        check(!PalaceKeyCheckpoint.story(new CompoundTag(),OWNER,DOOR,new CompoundTag()),"Absent story file interpreted as a successful empty checkpoint");
        var inventory=new ListTag();var key=original();key.putByte("Slot",(byte)4);inventory.add(key);var player=new CompoundTag();player.put("Inventory",inventory.copy());
        check(PalaceKeyCheckpoint.inventory(player,inventory),"Actual player inventory layout rejected");
        check(!PalaceKeyCheckpoint.inventory(player,new ListTag()),"Inventory removal considered saved while original still on disk");
        player.put("Inventory",new ListTag());check(PalaceKeyCheckpoint.inventory(player,new ListTag()),"Saved empty inventory rejected");
        check(!PalaceKeyCheckpoint.inventory(new CompoundTag(),new ListTag()),"Missing inventory file interpreted as saved removal");
        var malformed=new ListTag();malformed.add(StringTag.valueOf("wrong entry type"));player.put("Inventory",malformed);check(!PalaceKeyCheckpoint.inventory(player,malformed),"Malformed player inventory checkpoint accepted");
        try{
            var encoded=new java.io.ByteArrayOutputStream();NbtIo.writeCompressed(file,encoded);
            var decoded=NbtIo.readCompressed(new java.io.ByteArrayInputStream(encoded.toByteArray()));
            check(PalaceKeyCheckpoint.story(decoded,OWNER,DOOR,expected),"Real compressed-NBT world checkpoint lost story receipt");
            player.put("Inventory",inventory.copy());encoded=new java.io.ByteArrayOutputStream();NbtIo.writeCompressed(player,encoded);
            decoded=NbtIo.readCompressed(new java.io.ByteArrayInputStream(encoded.toByteArray()));
            check(PalaceKeyCheckpoint.inventory(decoded,inventory),"Real compressed-NBT player checkpoint changed key slot/data");
        }catch(java.io.IOException failure){throw new AssertionError("Actual compressed-NBT round trip failed",failure);}
    }
    private static void blocked(CompoundTag damaged){
        var before=damaged.copy();var tx=PalaceKeyTransaction.load(OWNER,DOOR,damaged);
        check(!tx.blocker().isEmpty(),"Corrupt custody journal accepted");check(!tx.spendable(OWNER,OTHER)&&!tx.removedAndSaved(OWNER,OTHER)&&!tx.dispose(OWNER,OTHER,BUNDLE),"Blocked journal can mutate/spend");
        var expected=before.copy();expected.putString("Blocker",tx.blocker());check(expected.equals(tx.save()),"Quarantine discarded original evidence");
        var again=PalaceKeyTransaction.load(OWNER,DOOR,tx.save());check(!again.blocker().isEmpty(),"Quarantine vanished on reload");
        var retained=again.save();retained.remove("Blocker");before.remove("Blocker");check(retained.equals(before),"Repeated quarantine reload changed original evidence");
    }
}
