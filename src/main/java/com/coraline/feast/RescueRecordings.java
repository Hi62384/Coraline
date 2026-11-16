package com.coraline.feast;

import java.util.*;

/** Explicit original-recording map. FinalBossFight L/R have no assigned cues. */
public final class RescueRecordings {
    public static List<String> names(){
        var out=new ArrayList<>(List.of("rescue_mantis_intro","rescue_mantis_take","rescue_mantis_gone","rescue_theatre_grab","rescue_clothes","rescue_theatre_music","other_father_boss_music"));
        for(char c:"abcde".toCharArray())out.add("rescue_bobinsky_"+c);
        for(char c:"abcdefghijkmnopqs".toCharArray())out.add("finale_"+c);
        for(char c:"ab".toCharArray())out.add("reunion_"+c);
        for(char c:"abc".toCharArray())out.add("farewell_"+c);
        return List.copyOf(out);
    }
    private RescueRecordings(){}
}
