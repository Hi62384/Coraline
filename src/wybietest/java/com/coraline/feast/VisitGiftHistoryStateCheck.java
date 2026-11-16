package com.coraline.feast;

import java.util.*;
import java.io.*;
import net.minecraft.nbt.*;

/** Real NBT preferences, with no item/Forge registry or reward delivery claims. */
public final class VisitGiftHistoryStateCheck {
    private static int checks;
    private static void require(boolean valid,String message){checks++;if(!valid)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        var story=new CompoundTag();story.putBoolean("UnrelatedStoryFlag",true);
        require(VisitGiftHistory.read(story).isEmpty(),"Legacy empty history failed");
        require(!story.contains("VisitGiftHistory"),"Read mutated old story state");
        require(!VisitGiftHistory.remember(story,0,List.of("coraline:raincoat")),"Invalid generation recorded");
        require(VisitGiftHistory.remember(story,1,List.of("coraline:raincoat","coraline:raincoat","invalid item")),"Real generation not recorded");
        require(VisitGiftHistory.read(story).get(0).ids().equals(List.of("coraline:raincoat")),"Invalid/duplicate IDs survived");
        var before=story.copy();
        require(!VisitGiftHistory.remember(story,1,List.of("coraline:perfect_sword")),"Same preparation rerolled history");
        require(story.equals(before),"Duplicate generation changed story data");
        var recent=VisitGiftHistory.read(story);
        int fresh=VisitGiftHistory.preference(List.of("coraline:raincoat"),id->false,List.of());
        int owned=VisitGiftHistory.preference(List.of("coraline:raincoat"),id->true,List.of());
        int repeated=VisitGiftHistory.preference(List.of("coraline:raincoat"),id->false,recent);
        require(fresh>owned&&fresh>repeated,"No unowned/recent-reward preference");
        require(owned>0&&repeated>0,"Preferences made an eligible item unobtainable");
        require(VisitGiftHistory.preference(List.of(),id->true,recent)==1,"Empty preference was invalid");
        for(int generation=2;generation<=30;generation++) {
            var ids=new ArrayList<String>();for(int item=0;item<32;item++)ids.add("coraline:test_"+generation+"_"+item);
            require(VisitGiftHistory.remember(story,generation,ids),"History stopped accepting legitimate generations");
            var loaded=VisitGiftHistory.read(story);
            require(loaded.size()<=8&&loaded.stream().allMatch(bundle->bundle.ids().size()<=16),"History exceeded its bounds");
        }
        var bytes=new ByteArrayOutputStream();NbtIo.writeCompressed(story,bytes);
        var reloaded=NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
        require(reloaded.equals(story)&&VisitGiftHistory.read(reloaded).equals(VisitGiftHistory.read(story)),"Compressed NBT roundtrip lost preferences");
        require(reloaded.getBoolean("UnrelatedStoryFlag"),"History overwrote unrelated storyline");
        require(VisitGiftHistory.read(reloaded).get(0).generation()==23,"Old preferences were not bounded to the latest eight gifts");
        var foreign=new CompoundTag();foreign.putString("VisitGiftHistory","wrong tag type");var original=foreign.copy();
        require(VisitGiftHistory.read(foreign).isEmpty()&&foreign.equals(original),"Malformed preferences mutated story on read");
        System.out.println("VISIT_GIFT_HISTORY_NBT_OK checks="+checks);
    }
}
