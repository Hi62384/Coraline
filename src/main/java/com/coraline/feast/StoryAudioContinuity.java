package com.coraline.feast;

import java.util.*;

/** Only unconditional recording-to-recording edges. Never crosses an input/movement gate. */
public final class StoryAudioContinuity {
    private static final Map<String,String> NEXT=new HashMap<>();
    static {
        run("theatre_opening_","abcdefghijklmn");run("theatre_","abcdef");
        run("black_is_traditional_","ab");run("black_is_traditional_","defghijk");run("black_is_traditional_","no");run("black_is_traditional_","pqrs");
        run("cocoa_beetles_","cd");run("cocoa_beetles_","efg");
        run("mirror_prison_","bcde");
        run("first_escape_","efg");run("missing_parents_","cd");run("missing_parents_","efg");
        run("parent_rescue_","bcd");run("parent_rescue_","efg");run("parent_rescue_","hijk");run("parent_rescue_","lm");
        run("missing_parents_cat_","ef");run("missing_parents_tea_","abcd");run("missing_parents_tea_","efghi");
        run("rescue_bobinsky_","abc");run("finale_","abc");run("reunion_","ab");run("farewell_","ab");
    }
    private static void run(String prefix,String letters){for(int i=0;i+1<letters.length();i++)NEXT.put(prefix+letters.charAt(i),prefix+letters.charAt(i+1));}
    public static String next(String sound){return NEXT.getOrDefault(sound,"");}
    public static List<String> run(String start){var result=new ArrayList<String>();for(String sound=start;!sound.isEmpty()&&!result.contains(sound)&&result.size()<32;sound=next(sound))result.add(sound);return List.copyOf(result);}
    private StoryAudioContinuity(){}
}
