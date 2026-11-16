package com.coraline.palace;

import java.util.UUID;
import net.minecraft.nbt.*;

/** Read-back predicates over the actual vanilla SavedData/player file layouts.
 * Deliberately no IO, world access, item creation or interpretation of absence
 * as permission to allocate a replacement key.
 */
public final class PalaceKeyCheckpoint {
    public static boolean story(CompoundTag file,UUID owner,UUID door,CompoundTag expected){
        if(file==null||expected==null||owner==null||door==null||!file.contains("data",Tag.TAG_COMPOUND))return false;
        var data=file.getCompound("data");if(!data.contains("Scenes",Tag.TAG_LIST))return false;
        var scenes=(ListTag)data.get("Scenes");if(scenes.getElementType()!=Tag.TAG_COMPOUND)return false;
        int found=0;boolean exact=false;
        for(var entry:scenes){
            var n=(CompoundTag)entry;if(!n.hasUUID("Owner")||!owner.equals(n.getUUID("Owner")))continue;
            found++;
            if(n.hasUUID("Door")&&door.equals(n.getUUID("Door"))&&n.contains("MainLoop",Tag.TAG_COMPOUND)){
                var main=n.getCompound("MainLoop");
                exact=main.contains("StoryProgression",Tag.TAG_COMPOUND)&&expected.equals(main.getCompound("StoryProgression").get("PinkPalaceJourney"));
            }
        }
        return found==1&&exact;
    }
    public static boolean inventory(CompoundTag file,ListTag expected){
        if(file==null||expected==null||!file.contains("Inventory",Tag.TAG_LIST))return false;
        var actual=(ListTag)file.get("Inventory");
        // An empty list legitimately has element type END rather than COMPOUND.
        return (actual.isEmpty()||actual.getElementType()==Tag.TAG_COMPOUND)&&expected.equals(actual);
    }
    private PalaceKeyCheckpoint(){}
}
