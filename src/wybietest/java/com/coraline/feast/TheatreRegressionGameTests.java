package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TheatreRegressionGameTests {
    @GameTest(template="empty") public static void transientAerialFramesNeverCallTheStrictFContract(GameTestHelper h){
        for(int duration:new int[]{0,1,200,559,560,1200})for(int tick:new int[]{0,1,479,480,539,540})for(int side:new int[]{-1,1}){
            var q=TheatreAerialGeometry.carrying(side,tick,duration,false);
            h.assertTrue(Double.isFinite(q.x())&&Double.isFinite(q.y())&&Double.isFinite(q.z()),"Transient prop/performer packets produced invalid geometry");
        }
        boolean refused=false;try{ScriptedShowTimeline.theatrePlayer(0,559);}catch(IllegalArgumentException expected){refused=true;}
        h.assertTrue(refused,"Render fix weakened the authoritative mandatory throw/rescue duration check");h.succeed();
    }
    @GameTest(template="empty") public static void reservedChairsReplaceExactlyTwoAudienceRowPositions(GameTestHelper h){
        int holes=0;for(int row=0;row<TheatreSite.ROWS;row++)for(int col=0;col<TheatreSite.SEATS_PER_ROW;col++)if(TheatreSite.reserved(row,col))holes++;
        h.assertTrue(holes==0,"Aisle layout still removes audience row seats");h.succeed();
    }
    @GameTest(template="empty") public static void continuousTheatreAudioNeverCrossesTheCurtainIntermission(GameTestHelper h){
        var opening=StoryAudioContinuity.run("theatre_opening_a");var aerial=StoryAudioContinuity.run("theatre_a");
        h.assertTrue(opening.size()==14&&opening.get(13).equals("theatre_opening_n"),"A-N chain was truncated or omitted");
        h.assertTrue(aerial.size()==6&&aerial.get(5).equals("theatre_f"),"A-F chain was not continuous");
        h.assertTrue(StoryAudioContinuity.next("theatre_opening_n").isEmpty()&&StoryAudioContinuity.next("theatre_f").isEmpty(),"Automatic playback crossed the curtain/input boundary");h.succeed();
    }
}
