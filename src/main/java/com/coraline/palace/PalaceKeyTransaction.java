package com.coraline.palace;

import java.util.*;
import net.minecraft.nbt.*;

/** Original serialized item custody journal; NEVER constructs or grants an item.
 * PREPARED is not usable escrow. The inventory adapter must durably remove the
 * tagged original before HELD, and must retain DISPOSED as a permanent tombstone.
 */
public final class PalaceKeyTransaction {
    public enum Stage { PREPARED,HELD,DISPOSED }
    private final UUID owner,door;
    private UUID key,receipt,bundle;
    private CompoundTag original,incompatible;
    private Stage stage;
    private String blocker="";
    private PalaceKeyTransaction(UUID owner,UUID door){this.owner=Objects.requireNonNull(owner);this.door=Objects.requireNonNull(door);}
    public static Optional<PalaceKeyTransaction> prepare(UUID owner,UUID door,UUID bundle,CompoundTag actualStack){
        var state=new PalaceKeyTransaction(owner,door);
        try{if(bundle==null||actualStack==null)throw new IllegalArgumentException("Missing physical bundle/original");state.original=actualStack.copy();state.validateOriginal();
            state.key=state.original.getCompound("tag").getUUID("PalaceKeyIdentity");state.bundle=bundle;state.receipt=UUID.randomUUID();state.stage=Stage.PREPARED;
            return Optional.of(state);
        }catch(IllegalArgumentException malformed){return Optional.empty();}
    }
    private void validateOriginal(){
        if(!original.getString("id").equals("coraline:little_door_story_key")||!original.contains("Count",Tag.TAG_BYTE)||original.getByte("Count")!=1||!original.contains("tag",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Not one original story key");
        var tag=original.getCompound("tag");
        if(!tag.hasUUID("StoryKeyOwner")||!owner.equals(tag.getUUID("StoryKeyOwner"))||!tag.hasUUID("StoryKeyDoor")||!door.equals(tag.getUUID("StoryKeyDoor"))||!tag.hasUUID("PalaceKeyIdentity"))throw new IllegalArgumentException("Original key binding is incomplete/foreign");
    }
    public UUID receipt(){return receipt;}
    public UUID key(){return key;}
    public UUID bundle(){return bundle;}
    public Stage stage(){return stage;}
    public String blocker(){return blocker;}
    public CompoundTag originalSnapshot(){return original==null?new CompoundTag():original.copy();}
    private boolean owns(UUID actor,UUID transaction){return blocker.isEmpty()&&owner.equals(actor)&&receipt.equals(transaction);}
    public boolean removedAndSaved(UUID actor,UUID transaction){
        if(!owns(actor,transaction)||stage==Stage.DISPOSED)return false;
        stage=Stage.HELD;return true;
    }
    public boolean dispose(UUID actor,UUID transaction,UUID physicalBundle){
        if(!owns(actor,transaction)||!bundle.equals(physicalBundle)||stage==Stage.PREPARED)return false;
        stage=Stage.DISPOSED;return true;
    }
    public boolean spendable(UUID actor,UUID transaction){return owns(actor,transaction)&&stage==Stage.HELD;}
    public CompoundTag save(){
        if(incompatible!=null){var preserved=incompatible.copy();preserved.putString("Blocker",blocker);return preserved;}
        var root=new CompoundTag();root.putInt("Version",1);root.putUUID("Owner",owner);root.putUUID("Door",door);root.putUUID("Key",key);root.putUUID("Receipt",receipt);root.putUUID("Bundle",bundle);
        root.putString("Stage",stage.name());root.put("Original",original.copy());return root;
    }
    public static PalaceKeyTransaction load(UUID owner,UUID door,CompoundTag root){
        var state=new PalaceKeyTransaction(owner,door);
        try{
            if(!root.contains("Version",Tag.TAG_INT)||root.getInt("Version")!=1||root.contains("Blocker"))throw new IllegalArgumentException("Unknown/blocked custody schema");
            for(String name:new String[]{"Owner","Door","Key","Receipt","Bundle"})if(!root.hasUUID(name))throw new IllegalArgumentException("Missing "+name);
            if(!owner.equals(root.getUUID("Owner"))||!door.equals(root.getUUID("Door")))throw new IllegalArgumentException("Custody owner/door mismatch");
            if(!root.contains("Original",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Missing actual serialized original");
            state.original=root.getCompound("Original").copy();state.validateOriginal();
            state.key=root.getUUID("Key");state.receipt=root.getUUID("Receipt");state.bundle=root.getUUID("Bundle");state.stage=Stage.valueOf(root.getString("Stage"));
            if(!state.key.equals(state.original.getCompound("tag").getUUID("PalaceKeyIdentity")))throw new IllegalArgumentException("Serialized original identity changed");
        }catch(IllegalArgumentException invalid){state.blocker="Original-key custody requires review: "+invalid.getMessage();state.incompatible=root.copy();}
        return state;
    }
}
