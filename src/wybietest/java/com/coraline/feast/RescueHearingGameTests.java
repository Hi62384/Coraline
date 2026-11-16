package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.*;

/** Real owner/phase ledger integration; actual packet-hook acceptance is a separate client check. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueHearingGameTests {
    private static ServerPlayer player(GameTestHelper h){var l=h.getLevel();var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"HearingQA"));var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();RescueChapter.data(s).putBoolean("Started",true);RescueChapter.data(s).putString("Phase","WEB_STEALTH");VisitData.get(p.server).scenes.put(s.owner,s);p.setPos(0,100,0);return p;}
    @GameTest(template="empty") public static void tenAcceptedQuietStepsDoNotBecomeOneFastCrouch(GameTestHelper h){var p=player(h);var s=OtherMotherLoop.scene(p);try{for(int i=0;i<10;i++){double x=p.getX(),z=p.getZ();p.setPos(x+.06476,100,z);RescueWebHearing.acceptedMove(p,x,z);}var sample=RescueWebHearing.consume(p,.6476);h.assertTrue(sample.packets()==10&&Math.abs(sample.distance()-.6476)<1e-8&&Math.abs(sample.loudest()-.06476)<1e-8,"Accepted step ledger lost the real packet boundaries");h.assertFalse(RescueChallengeMath.hears(sample.loudest(),true,false,false,false),"Lag-batched quiet steps falsely heard");h.assertTrue(RescueChallengeMath.hears(sample.loudest(),false,false,false,false)==false&&RescueChallengeMath.hears(sample.loudest(),true,true,false,false),"Sprint threshold changed");}finally{RescueWebHearing.reset(p);VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();}
    @GameTest(template="empty") public static void fastStepAndUnaccountedPhysicalPushCannotHideBehindQuietPackets(GameTestHelper h){var p=player(h);var s=OtherMotherLoop.scene(p);try{for(int i=0;i<10;i++){double x=p.getX();p.setPos(x+(i==4?.3:.04),100,0);RescueWebHearing.acceptedMove(p,x,0);}var sample=RescueWebHearing.consume(p,.66);h.assertTrue(sample.loudest()>=.299999&&RescueChallengeMath.hears(sample.loudest(),true,false,false,false),"Quiet packet padding hid a loud step");double x=p.getX();p.setPos(x+.05,100,0);RescueWebHearing.acceptedMove(p,x,0);h.assertTrue(RescueWebHearing.consume(p,.4).loudest()>.34,"Unaccounted server push silently disappeared");RescueWebHearing.reset(p);RescueChapter.data(s).putString("Phase","GARDEN_SEARCH");x=p.getX();p.setPos(x+.05,100,0);RescueWebHearing.acceptedMove(p,x,0);h.assertTrue(RescueWebHearing.consume(p,.05).packets()==0,"Another chapter contributed a stale web movement sample");}finally{RescueWebHearing.reset(p);VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();}
    private RescueHearingGameTests(){}
}
