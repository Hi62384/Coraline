package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;
import java.util.*;
import static com.coraline.feast.PlayerPartyLedger.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PlayerPartyLedgerGameTests {
    private static final UUID OWNER=new UUID(1,2),OTHER=new UUID(3,4),INSTANCE=new UUID(5,6),GUEST=new UUID(7,8);
    private static CompoundTag payload(String id){var n=new CompoundTag();n.putString("id",id);n.putInt("Count",8);return n;}
    private static Gift sampleGift(UUID id){return new Gift(id,"resident","Guest",payload("minecraft:cookie"),8);}
    private static CompoundTag book(){return giftBook(new CompoundTag(),OWNER,INSTANCE,List.of(sampleGift(GUEST)));}
    private static CompoundTag active(){var n=new CompoundTag();initialize(n,INSTANCE);phase(n,Phase.SOCIAL);return n;}
    private static void ready(CompoundTag n){attended(n,GUEST);unlock(n);presented(n,GUEST);}
    private static void rejects(GameTestHelper h,Runnable action,String message){boolean rejected=false;try{action.run();}catch(IllegalArgumentException|IllegalStateException e){rejected=true;}h.assertTrue(rejected,message);}
    @GameTest(template="empty") public static void socialStartsAfterInvitationOnly(GameTestHelper h){
        var n=new CompoundTag();initialize(n,INSTANCE);advance(n,20,false);h.assertTrue(n.getInt("SocialElapsed")==0,"Preparation consumed social time");phase(n,Phase.INVITATION);advance(n,20,false);h.assertTrue(n.getInt("SocialElapsed")==0,"Invitation consumed social time");phase(n,Phase.SOCIAL);advance(n,20,false);h.assertTrue(n.getInt("SocialElapsed")==20,"Activation did not start social clock");h.succeed();
    }
    @GameTest(template="empty") public static void socialBoundaryAndShowDrainArePersistent(GameTestHelper h){
        var n=active();for(int i=0;i<1199;i++)advance(n,20,false);advance(n,19,false);h.assertTrue(phase(n)==Phase.SOCIAL&&!n.getBoolean("FeastDue"),"Finale began before full 24000 ticks");n=n.copy();advance(n,1,false);h.assertTrue(phase(n)==Phase.DRAINING_SHOWS&&n.getBoolean("FinaleDue")&&n.getBoolean("FeastDue"),"Finale did not freeze new matches and shows");for(int i=0;i<360;i++)advance(n,20,false);h.assertTrue(n.getBoolean("AttractionOverdue"),"Broken show drain has no diagnostic bound");h.assertTrue(phase(n)==Phase.DRAINING_SHOWS,"Drain silently cut a show");h.succeed();
    }
    @GameTest(template="empty") public static void pausesAndFinaleDoNotEatSocialTime(GameTestHelper h){
        var n=active();advance(n,20,true);h.assertTrue(n.getLong("Clock")==0&&n.getInt("SocialElapsed")==0,"Pause changed timer");phase(n,Phase.GIFT_CEREMONY);advance(n,20,false);h.assertTrue(n.getLong("Clock")==20&&n.getInt("SocialElapsed")==0,"Finale timer incorrectly extends social");phase(n,Phase.FINISHING);advance(n,20,false);h.assertTrue(n.getLong("Clock")==40,"Departure clock is frozen before guests leave");phase(n,Phase.COMPLETED);advance(n,20,false);h.assertTrue(n.getLong("Clock")==40,"Completed event still ticks");h.succeed();
    }
    @GameTest(template="empty") public static void invalidDeltaAndInstanceAreRejected(GameTestHelper h){
        var n=active();rejects(h,()->advance(n,21,false),"Unbounded delta accepted");rejects(h,()->advance(n,-1,false),"Negative delta accepted");rejects(h,()->initialize(n,OTHER),"Instance could change");initialize(n,INSTANCE);h.assertTrue(phase(n)==Phase.SOCIAL,"Reconnect reinitialized party");h.succeed();
    }
    @GameTest(template="empty") public static void absenceDistributionStaysTwoToTenMinutes(GameTestHelper h){
        for(int i=0;i<=1000;i++){double u=i/1000.;int low=absenceDelay(0,u),high=absenceDelay(1,u);h.assertTrue(low>=2400&&low<=12000&&high>=2400&&high<=12000,"Absence outside2–10 minute range");h.assertTrue(high<=low,"Higher Suspicion lengthened deadline");}h.assertTrue(absenceDelay(Double.NaN,Double.NaN)==2400,"Nonfinite input produced unsafe timer");h.succeed();
    }
    @GameTest(template="empty") public static void absenceRollPersistsAndChaseCannotReset(GameTestHelper h){
        var n=active();absence(n,false,false,.5,.8,5,10);int first=n.getInt("AbsenceRemaining");n=n.copy();absence(n,false,false,0,1,5,10);h.assertTrue(n.getInt("AbsenceRemaining")==first-5,"Deadline rerolled");absence(n,true,false,0,0,5,99);h.assertTrue(!n.contains("AbsenceRemaining")&&n.getLong("LastPermittedPosition")==99,"Normal return did not cancel pending absence");n.putInt("AbsenceRemaining",5);h.assertTrue(absence(n,false,false,0,0,5,0),"Expired absence not reported");absence(n,true,true,0,0,5,5);h.assertTrue(n.getBoolean("AbsenceReported")&&n.contains("AbsenceRemaining"),"Boundary crossing reset chase");h.assertFalse(absence(n,false,false,0,0,5,0),"Same absence reported twice");h.succeed();
    }
    @GameTest(template="empty") public static void giftPayloadIsDefensivelyCopied(GameTestHelper h){
        var source=payload("minecraft:cookie");var gift=new Gift(GUEST,"resident","Guest",source,8);source.putString("id","minecraft:dirt");var copy=gift.payload();copy.putString("id","minecraft:stone");h.assertTrue(gift.payload().getString("id").equals("minecraft:cookie"),"External mutation changed fixed gift");h.succeed();
    }
    @GameTest(template="empty") public static void duplicateAndInvalidRostersNeverCreateBook(GameTestHelper h){
        var books=new CompoundTag();rejects(h,()->giftBook(books,OWNER,INSTANCE,List.of(sampleGift(GUEST),sampleGift(GUEST))),"Duplicate guest accepted");h.assertTrue(books.isEmpty(),"Rejected roster left partial book");rejects(h,()->giftBook(books,OWNER,INSTANCE,List.of()),"Empty roster accepted");rejects(h,()->new Gift(GUEST,"resident","Guest",new CompoundTag(),1),"Empty payload accepted");rejects(h,()->new Gift(GUEST,"resident","Guest",payload("minecraft:cookie"),65),"Oversized gift accepted");h.succeed();
    }
    @GameTest(template="empty") public static void reconnectAndCancelCannotReroll(GameTestHelper h){
        var books=new CompoundTag();var first=giftBook(books,OWNER,INSTANCE,List.of(sampleGift(GUEST)));ready(first);delivered(first,OWNER,GUEST,3);books=books.copy();var again=giftBook(books,OWNER,INSTANCE,List.of(new Gift(GUEST,"resident","Guest",payload("minecraft:diamond"),1)));h.assertTrue(remaining(again,OWNER,GUEST)==3&&gift(again,GUEST).getCompound("Payload").getString("id").equals("minecraft:cookie"),"Reconnect rerolled payload or claim");cancel(again);h.assertTrue(giftBook(books,OWNER,INSTANCE,List.of(sampleGift(GUEST))).getBoolean("Cancelled"),"Same-instance restart revived cancelled book");final var snapshot=books;rejects(h,()->giftBook(snapshot,OTHER,INSTANCE,List.of(sampleGift(GUEST))),"Different owner claimed same book");h.succeed();
    }
    @GameTest(template="empty") public static void everyActualAttendeeKeepsGiftWhenActorMissing(GameTestHelper h){
        var ids=new ArrayList<UUID>();var gifts=new ArrayList<Gift>();for(int i=0;i<24;i++){var id=new UUID(10,i);ids.add(id);gifts.add(sampleGift(id));}var n=giftBook(new CompoundTag(),OWNER,INSTANCE,gifts);for(int i=0;i<23;i++)attended(n,ids.get(i));unlock(n);h.assertTrue(awaitingPresentation(n).size()==23,"Actual attendance was replaced by random subset or invitation count");h.assertFalse(attended(n,ids.get(23)),"Late attendance changed frozen roster");n=n.copy();for(UUID id:ids.subList(0,23)){h.assertTrue(presented(n,id),"Missing actor prevented gift ledger presentation");h.assertFalse(presented(n,id),"Gift presented twice");h.assertTrue(remaining(n,OWNER,id)==8,"Attendee lost gift");}h.assertTrue(awaitingPresentation(n).isEmpty()&&remaining(n,OWNER,ids.get(23))==0,"Nonattendee received gift");h.succeed();
    }
    @GameTest(template="empty") public static void giftDeliveryIsOwnerOnlyAndRequiresPresentation(GameTestHelper h){
        var n=book();h.assertTrue(remaining(n,OWNER,GUEST)==0,"Gift available before ceremony");attended(n,GUEST);h.assertFalse(presented(n,GUEST),"Gift presented before finale unlocked");unlock(n);presented(n,GUEST);h.assertTrue(remaining(n,OTHER,GUEST)==0,"Other player can claim gift");rejects(h,()->delivered(n,OTHER,GUEST,0),"Other player mutated claim");h.assertTrue(remaining(n,OWNER,GUEST)==8,"Owner lost gift after denied claim");h.succeed();
    }
    @GameTest(template="empty") public static void fullPartialAndCompleteDeliveryPreserveExactCount(GameTestHelper h){
        var n=book();ready(n);delivered(n,OWNER,GUEST,8);h.assertTrue(remaining(n,OWNER,GUEST)==8,"Full inventory deleted gift");delivered(n,OWNER,GUEST,5);n=n.copy();h.assertTrue(remaining(n,OWNER,GUEST)==5,"Restart lost partial remainder");final var current=n;rejects(h,()->delivered(current,OWNER,GUEST,6),"Remaining gift increased");rejects(h,()->delivered(current,OWNER,GUEST,-1),"Negative remainder accepted");delivered(n,OWNER,GUEST,0);h.assertTrue(remaining(n,OWNER,GUEST)==0&&gift(n,GUEST).getBoolean("Claimed"),"Completed delivery not final");rejects(h,()->delivered(current,OWNER,GUEST,0),"Completed gift delivered twice");h.succeed();
    }
    @GameTest(template="empty") public static void cancellationKeepsEarnedPendingGiftsOnly(GameTestHelper h){
        var n=book();ready(n);delivered(n,OWNER,GUEST,4);cancel(n);h.assertTrue(remaining(n,OWNER,GUEST)==4,"Cancellation deleted earned gift");delivered(n,OWNER,GUEST,0);h.assertTrue(remaining(n,OWNER,GUEST)==0,"Cancellation allowed duplicate reward");var before=book();attended(before,GUEST);cancel(before);unlock(before);h.assertTrue(awaitingPresentation(before).isEmpty()&&!presented(before,GUEST),"Cancelled unearned ceremony awarded gifts");h.succeed();
    }
    @GameTest(template="empty") public static void cakeActionsAreOrderedBoundedAndOneTime(GameTestHelper h){
        var n=book();h.assertFalse(wish(n)||blowCandles(n)||takeSlice(n)||reserveTakeHomeCake(n),"Cake usable before reveal");unlock(n);revealCake(n);h.assertTrue(wish(n)&&!wish(n),"Wish replayed");h.assertTrue(blowCandles(n)&&!blowCandles(n),"Candles replayed");for(int i=0;i<12;i++)h.assertTrue(takeSlice(n),"Cake slice missing");h.assertFalse(takeSlice(n),"Infinite cake food");h.assertTrue(reserveTakeHomeCake(n)&&!reserveTakeHomeCake(n),"Infinite take-home cake");n=n.copy();h.assertTrue(n.getBoolean("CakePending"),"Full inventory/reconnect lost take-home cake");completeCakeDelivery(n);h.assertTrue(n.getBoolean("CakeClaimed")&&!n.getBoolean("CakePending"),"Cake claim not finalized");final var current=n;rejects(h,()->completeCakeDelivery(current),"Cake delivered twice");h.succeed();
    }
    @GameTest(template="empty") public static void cancelledCakeCannotCreateNewRewards(GameTestHelper h){
        var n=book();unlock(n);revealCake(n);wish(n);blowCandles(n);reserveTakeHomeCake(n);cancel(n);h.assertFalse(takeSlice(n)||reserveTakeHomeCake(n),"Cancelled party created new cake rewards");h.assertTrue(n.getBoolean("CakePending"),"Cancellation removed an earned cake");completeCakeDelivery(n);h.succeed();
    }
    private PlayerPartyLedgerGameTests(){}
}
