package com.coraline.feast;

import java.util.HashSet;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class IncidentalDialogueGameTests {
    @GameTest(template="empty") public static void exactSpeakerTextLookupNeverVoicesAnotherCharacter(GameTestHelper h){
        String mother=IncidentalDialogue.sound("Other Mother","What shall we raise our glasses to?");
        String father=IncidentalDialogue.sound("Other Father","What shall we raise our glasses to?");
        h.assertTrue(mother!=null&&father!=null&&!mother.equals(father),"Same words reused across different reference speakers");
        h.assertTrue(IncidentalDialogue.sound("Cat","What shall we raise our glasses to?")==null,"Cat receives a parent voice");
        h.assertTrue(IncidentalDialogue.sound("Other Wybie","What shall we raise our glasses to?")==null,"Silent Other Wybie receives a parent voice");
        h.assertTrue(IncidentalDialogue.sound("Other Mother","This is only a GUI objective.")==null,"Uncatalogued UI text receives unrelated audio");
        h.succeed();
    }
    @GameTest(template="empty") public static void allIncidentalVoicesHaveRealRegisteredPackagedRecordings(GameTestHelper h){
        h.assertTrue(IncidentalDialogue.soundIds().size()==78&&new HashSet<>(IncidentalDialogue.soundIds()).size()==78,"Missing/duplicate incidental voice IDs");
        for(String sound:IncidentalDialogue.soundIds()){
            h.assertTrue(VisitAudio.SOUNDS.containsKey(sound)&&VisitAudio.SOUNDS.get(sound).isPresent(),"Unregistered incidental voice: "+sound);
            int actual=NumberedEventDialogue.recordedTicks(sound);
            h.assertTrue(actual>0&&VisitAudio.length(sound)==actual,"Missing recording or merged duration: "+sound);
        }
        h.succeed();
    }
    private IncidentalDialogueGameTests(){}
}
