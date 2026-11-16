package com.coraline.feast;

import net.minecraft.nbt.*;
import java.util.*;

/** Player Party state and gift accounting. Gift data belongs in the owner's player-data file. */
public final class PlayerPartyLedger {
    public static final int SOCIAL_TICKS=24000,MAX_SHOW_DRAIN=7200;
    public enum Phase { PREPARING,INVITATION,SOCIAL,DRAINING_SHOWS,ANNOUNCING,GATHERING,CAKE_PRESENTATION,GIFT_CEREMONY,CAKE_INTERACTION,FINISHING,COMPLETED,CANCELLED }
    public record Gift(UUID guest,String kind,String name,CompoundTag payload,int count) {
        public Gift {if(guest==null||kind==null||kind.isBlank()||name==null||name.isBlank()||payload==null||payload.isEmpty()||count<1||count>64)throw new IllegalArgumentException("Invalid player-party gift");payload=payload.copy();}
        @Override public CompoundTag payload(){return payload.copy();}
    }
    public static Phase phase(CompoundTag party){try{return Phase.valueOf(party.getString("PlayerPhase"));}catch(IllegalArgumentException missing){return Phase.PREPARING;}}
    public static void phase(CompoundTag party,Phase phase){party.putString("PlayerPhase",phase.name());party.putInt("PhaseTicks",0);}
    public static boolean terminal(CompoundTag party){return phase(party)==Phase.COMPLETED||phase(party)==Phase.CANCELLED;}
    public static void initialize(CompoundTag party,UUID instance){
        if(party.hasUUID("PlayerPartyInstance")){if(!party.getUUID("PlayerPartyInstance").equals(instance))throw new IllegalStateException("Player party instance changed");return;}
        party.putUUID("PlayerPartyInstance",instance);party.putString("Variant","PLAYER_PARTY");party.putInt("SocialElapsed",0);party.putLong("Clock",0);phase(party,Phase.PREPARING);
        party.putBoolean("FinaleDue",false);party.putBoolean("FeastDue",false);party.putBoolean("Pending",true);
    }
    /** Called once per active owner tick; ordinary HousePartyLedger.advance must not run too. */
    public static void advance(CompoundTag party,int delta,boolean paused){
        if(delta<0||delta>20)throw new IllegalArgumentException("Unbounded party tick delta");
        if(paused||terminal(party)||!party.getBoolean("Pending"))return;
        party.putLong("Clock",party.getLong("Clock")+delta);party.putInt("PhaseTicks",Math.min(1_000_000,party.getInt("PhaseTicks")+delta));
        if(phase(party)==Phase.SOCIAL){int elapsed=Math.min(SOCIAL_TICKS,party.getInt("SocialElapsed")+delta);party.putInt("SocialElapsed",elapsed);
            if(elapsed==SOCIAL_TICKS){party.putBoolean("FinaleDue",true);party.putBoolean("FeastDue",true);phase(party,Phase.DRAINING_SHOWS);}}
        if(phase(party)==Phase.DRAINING_SHOWS&&party.getInt("PhaseTicks")>=MAX_SHOW_DRAIN)party.putBoolean("AttractionOverdue",true);
    }
    public static int absenceDelay(double suspicion,double unit){
        double s=Double.isFinite(suspicion)?Math.max(0,Math.min(1,suspicion)):0,u=Double.isFinite(unit)?Math.max(0,Math.min(1,unit)):0;
        return (int)Math.round(1200*(2+8*Math.pow(u,.45+5.5*s)));
    }
    public static boolean absence(CompoundTag party,boolean permitted,boolean searching,double suspicion,double random,int delta,long lastPosition){
        if(delta<0||delta>20)throw new IllegalArgumentException("Unbounded absence delta");
        if(permitted){party.putLong("LastPermittedPosition",lastPosition);if(!searching&&!party.getBoolean("AbsenceReported"))party.remove("AbsenceRemaining");return false;}
        if(searching||party.getBoolean("AbsenceReported"))return false;
        if(!party.contains("AbsenceRemaining"))party.putInt("AbsenceRemaining",absenceDelay(suspicion,random));
        int remaining=Math.max(0,party.getInt("AbsenceRemaining")-delta);party.putInt("AbsenceRemaining",remaining);
        if(remaining==0){party.putBoolean("AbsenceReported",true);return true;}return false;
    }
    public static CompoundTag giftBook(CompoundTag books,UUID owner,UUID instance,List<Gift> gifts){
        String key=instance.toString();if(books.contains(key,Tag.TAG_COMPOUND)){
            var prior=books.getCompound(key);if(!prior.hasUUID("Owner")||!owner.equals(prior.getUUID("Owner")))throw new IllegalStateException("Gift owner changed");return prior;
        }
        if(gifts==null||gifts.isEmpty())throw new IllegalArgumentException("No invited guests");
        var book=new CompoundTag();book.putUUID("Owner",owner);book.putUUID("Instance",instance);var roster=new ListTag();var ids=new HashSet<UUID>();
        for(var gift:gifts){if(gift==null||!ids.add(gift.guest()))throw new IllegalArgumentException("Duplicate gift guest");var g=new CompoundTag();g.putUUID("Guest",gift.guest());g.putString("Kind",gift.kind());g.putString("Name",gift.name());g.put("Payload",gift.payload());g.putInt("Count",gift.count());g.putInt("Remaining",gift.count());roster.add(g);}
        book.put("Gifts",roster);book.putInt("CakeSlices",12);books.put(key,book);return book;
    }
    public static CompoundTag gift(CompoundTag book,UUID id){for(var tag:book.getList("Gifts",Tag.TAG_COMPOUND)){var g=(CompoundTag)tag;if(g.hasUUID("Guest")&&id.equals(g.getUUID("Guest")))return g;}return null;}
    public static boolean attended(CompoundTag book,UUID id){var g=gift(book,id);if(g==null||g.getBoolean("Attended")||book.getBoolean("Cancelled")||book.getBoolean("Unlocked"))return false;g.putBoolean("Attended",true);return true;}
    /** Freeze actual attendance at the finale. An unloaded NPC cannot make its earned gift disappear. */
    public static void unlock(CompoundTag book){if(!book.getBoolean("Cancelled"))book.putBoolean("Unlocked",true);}
    public static List<UUID> awaitingPresentation(CompoundTag book){
        if(!book.getBoolean("Unlocked")||book.getBoolean("Cancelled"))return List.of();var result=new ArrayList<UUID>();
        for(var tag:book.getList("Gifts",Tag.TAG_COMPOUND)){var g=(CompoundTag)tag;if(g.getBoolean("Attended")&&!g.getBoolean("Presented"))result.add(g.getUUID("Guest"));}return List.copyOf(result);
    }
    public static boolean presented(CompoundTag book,UUID id){var g=gift(book,id);if(g==null||book.getBoolean("Cancelled")||!book.getBoolean("Unlocked")||!g.getBoolean("Attended")||g.getBoolean("Presented"))return false;g.putBoolean("Presented",true);return true;}
    public static int remaining(CompoundTag book,UUID requester,UUID id){
        var g=gift(book,id);return !book.hasUUID("Owner")||!requester.equals(book.getUUID("Owner"))||g==null||!g.getBoolean("Presented")||g.getBoolean("Claimed")?0:g.getInt("Remaining");
    }
    /** Set only after actual inventory insertion; the player inventory and this book are saved together. */
    public static void delivered(CompoundTag book,UUID requester,UUID id,int remaining){
        int before=remaining(book,requester,id);if(before<=0||remaining<0||remaining>before)throw new IllegalArgumentException("Invalid gift delivery");
        var g=gift(book,id);g.putInt("Remaining",remaining);if(remaining==0)g.putBoolean("Claimed",true);
    }
    public static boolean cakePresented(CompoundTag book){return book.getBoolean("CakePresented");}
    public static void revealCake(CompoundTag book){if(book.getBoolean("Unlocked")&&!book.getBoolean("Cancelled"))book.putBoolean("CakePresented",true);}
    public static boolean wish(CompoundTag book){if(!cakePresented(book)||book.getBoolean("Cancelled")||book.getBoolean("Wish"))return false;book.putBoolean("Wish",true);return true;}
    public static boolean blowCandles(CompoundTag book){if(!book.getBoolean("Wish")||book.getBoolean("Cancelled")||book.getBoolean("CandlesOut"))return false;book.putBoolean("CandlesOut",true);return true;}
    public static boolean takeSlice(CompoundTag book){int remaining=book.getInt("CakeSlices");if(!book.getBoolean("CandlesOut")||book.getBoolean("Cancelled")||remaining<=0)return false;book.putInt("CakeSlices",remaining-1);return true;}
    public static boolean reserveTakeHomeCake(CompoundTag book){if(!book.getBoolean("CandlesOut")||book.getBoolean("Cancelled")||book.getBoolean("CakeReserved"))return false;book.putBoolean("CakeReserved",true);book.putBoolean("CakePending",true);return true;}
    public static void completeCakeDelivery(CompoundTag book){if(!book.getBoolean("CakePending"))throw new IllegalStateException("Cake was not pending");book.putBoolean("CakePending",false);book.putBoolean("CakeClaimed",true);}
    public static void cancel(CompoundTag book){book.putBoolean("Cancelled",true);/* already presented gifts and take-home cake remain deliverable */}
    private PlayerPartyLedger(){}
}
