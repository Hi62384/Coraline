package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.coraline.feast.PlayerPartyLedger.Phase.*;

/** Player celebration component of VisitEvents, sharing guest, dialogue and tabletop infrastructure. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PlayerParty {
    private static final String BOOKS="CoralinePlayerPartyGifts";
    private static final int CAKE_SLOT=4000,GIFT_SLOT=4001;
    /** Authored world envelopes, not a radius from the house. All connecting routes must be supplied explicitly. */
    public record Area(String id,String dimension,AABB bounds) {
        public Area {if(id==null||id.isBlank()||dimension==null||dimension.isBlank()||bounds==null)throw new IllegalArgumentException("Invalid player-party area");}
    }
    /** Required integration seam: real decorations, authored garden/routes and the shared attic audience show. No fake ready fallback. */
    public interface Presentation {
        boolean prepare(ServerPlayer p,VisitData.Scene s);
        /** True only after custom AND remaining vanilla/mod storage extraction paths are guarded. */
        boolean storageSecured(ServerPlayer p,VisitData.Scene s);
        List<Area> grounds(ServerPlayer p,VisitData.Scene s);
        Vec3 celebration(ServerPlayer p,VisitData.Scene s);
        int capacity(ServerPlayer p,VisitData.Scene s);
        /** Must use the completed Overworld visit, not a flag set merely on first spawn. */
        boolean eligible(ServerPlayer p,VisitData.Scene s,String kind);
        /** Safe, decorated cake pedestal top; validate clearance without overwriting the house. */
        Vec3 cakePosition(ServerPlayer p,VisitData.Scene s);
        boolean circusReady(ServerPlayer p,VisitData.Scene s);
        void tickCircus(ServerPlayer p,VisitData.Scene s,Mob actualBobinsky);
        boolean circusPlaying(VisitData.Scene s);
        void finishCircus(VisitData.Scene s);
        void cakeChanged(ServerPlayer p,VisitData.Scene s,ScenePropEntity cake,int slices,boolean candlesOut);
        void cleanup(ServerPlayer p,VisitData.Scene s);
    }
    private static Presentation presentation=new PlayerPartyPresentation();
    public static void install(Presentation implementation){presentation=Objects.requireNonNull(implementation);}
    public static boolean configured(){return presentation!=null;}
    public static boolean isPlayerParty(VisitData.Scene s){return s!=null&&s.loop.event.getString("Type").equals("PLAYER_PARTY")&&HouseParty.data(s).getString("Variant").equals("PLAYER_PARTY");}
    /** Persisted ownership for rollback/storage only; it does not grant actions or public-floor permission. */
    public static VisitData.Scene owned(ServerPlayer p){var s=OtherMotherLoop.scene(p);return isPlayerParty(s)&&p.getUUID().equals(s.owner)&&HousePartyLedger.owned(HouseParty.data(s),p.getUUID(),s.loop.event.getInt("Serial"))?s:null;}
    public static VisitData.Scene active(ServerPlayer p){
        var s=owned(p);if(s==null||OtherMotherLoop.active(p)!=s||p.isDeadOrDying()||s.loop.event.getBoolean("ShowCleanupDeferred"))return null;var n=HouseParty.data(s);
        return p.level().dimension().location().toString().equals(n.getString("HouseDimension"))||n.getBoolean("TheatrePrepared")&&TheatreSite.inside(p,s)?s:null;
    }
    public static OtherMotherDialogue.Pool pool(String category,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/player_party/"+category+"/"+SecretDialogue.stateId(state),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var result=new ArrayList<OtherMotherDialogue.Pool>();for(String category:List.of("invitation","checkin","guest_report","search","surprise","cake","completion"))for(var state:OtherMotherWelcome.State.values())result.add(pool(category,state));return result;}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!configured()||!p.getUUID().equals(s.owner)||!NumberedEventDialogue.ready("player_party"))return false;
        var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return false;
        if(isPlayerParty(s)&&HouseParty.data(s).hasUUID("PlayerPartyInstance"))return true;
        var n=new CompoundTag();int capacity=Math.max(8,Math.min(24,presentation.capacity(p,s)));
        HousePartyLedger.begin(n,p.getUUID(),s.loop.event.getInt("Serial"),p.getRandom().nextLong(),capacity);n.putInt("Capacity",capacity);
        n.putLong("House",realm.house.asLong());n.putString("HouseDimension",p.level().dimension().location().toString());
        PlayerPartyLedger.initialize(n,UUID.nameUUIDFromBytes((s.owner+":"+s.door+":"+s.loop.event.getInt("Serial")+":player-party").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        s.loop.event.put("Party",n);s.loop.event.putBoolean("PartyPending",true);PartyGuests.roster(p,s);
        // Preserve the user's permanent storyline consequence, independent of ordinary visit resets.
        n.getList("Guests",Tag.TAG_COMPOUND).removeIf(t->{String kind=((CompoundTag)t).getString("Kind");return kind.equals("other_wybie")?!WybieOptionalVisits.allowed(s):kind.startsWith("other_")&&!presentation.eligible(p,s,kind);});
        n.putBoolean("CircusEligible",named(s,"other_bobinsky")!=null);
        n.putBoolean("TheatreEligible",named(s,"other_miss_spink")!=null&&named(s,"other_miss_forcible")!=null);
        var gifts=new ArrayList<PlayerPartyLedger.Gift>();
        for(var g:PartyGuests.guests(s)){var stack=giftPayload(p,s,g,n.getLong("Seed"));gifts.add(new PlayerPartyLedger.Gift(g.getUUID("Id"),g.getString("Kind"),g.getString("Name"),stack.save(new CompoundTag()),stack.getCount()));}
        PlayerPartyLedger.giftBook(books(p),p.getUUID(),n.getUUID("PlayerPartyInstance"),gifts);savePlayer(p);OtherMotherLoop.changed(p);return true;
    }
    /** Must finish before the director starts the invitation. Missing garden/routes or attractions remain blocked. */
    public static boolean ready(ServerPlayer p,VisitData.Scene s){
        if(!configured()||!isPlayerParty(s))return false;var n=HouseParty.data(s);
        var areas=presentation.grounds(p,s);if(areas==null||areas.stream().noneMatch(a->a.id().equals("backyard"))||areas.stream().noneMatch(a->a.id().equals("porch"))
                ||ShowHouseAccess.atticRoute(p,s).isEmpty()||ShowHouseAccess.basementRoute(p,s).isEmpty()){n.putString("PreparationBlocker","Missing verified backyard, porch, attic or cellar route metadata");return false;}
        if(n.getBoolean("TheatreEligible")&&(ShowRecordings.duration("theatre_music")<=0||ShowRecordings.duration("theatre_music")>PlayerPartyLedger.MAX_SHOW_DRAIN)||n.getBoolean("CircusEligible")&&(ShowRecordings.duration("mouse_circus_music")<=0||ShowRecordings.duration("mouse_circus_music")>PlayerPartyLedger.MAX_SHOW_DRAIN)){n.putString("PreparationBlocker","An eligible original show recording is missing or exceeds the safe full-track drain bound");return false;}
        if(!presentation.storageSecured(p,s)){n.putString("PreparationBlocker","Server-side protected-storage extraction coverage is incomplete");return false;}
        if(!presentation.prepare(p,s)||n.getBoolean("CircusEligible")&&!presentation.circusReady(p,s)){n.putString("PreparationBlocker","Party decoration or eligible performance site is not ready");return false;}
        if(n.getBoolean("TheatreEligible")){if(!TheatreSite.prepare(p,s)){n.putString("PreparationBlocker","Shared theatre could not be safely prepared");return false;}n.putBoolean("TheatrePrepared",true);}
        n.remove("PreparationBlocker");n.putBoolean("Prepared",true);PlayerPartyLedger.phase(n,INVITATION);return true;
    }
    /** Called after the complete authored invitation, never during site preparation. */
    public static void begin(ServerPlayer p,VisitData.Scene s){if(!mayRecover(p,s))return;var n=HouseParty.data(s);if(!n.getBoolean("Prepared")||PlayerPartyLedger.phase(n)!=INVITATION)return;n.putBoolean("Suspended",false);n.remove("ReconnectPending");n.putBoolean("Activated",true);ShowHouseAccess.open(p,s,"attic");ShowHouseAccess.open(p,s,"basement");PlayerPartyLedger.phase(n,SOCIAL);OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.PARTY_SOCIAL);OtherMotherLoop.changed(p);}
    private static ItemStack giftPayload(ServerPlayer p,VisitData.Scene s,CompoundTag g,long seed){
        var id=g.getUUID("Id");var rng=RandomSource.create(seed^id.getMostSignificantBits()^id.getLeastSignificantBits());
        OtherMotherDialogue.Loot category=switch(g.getString("Kind")){case "other_bobinsky"->OtherMotherDialogue.Loot.FOOD;case "other_miss_spink"->OtherMotherDialogue.Loot.CLOTHING;case "other_miss_forcible"->OtherMotherDialogue.Loot.MUSIC;case "other_wybie"->OtherMotherDialogue.Loot.TOOLS;default->rng.nextInt(10)<6?OtherMotherDialogue.Loot.FOOD:rng.nextInt(8)==0?OtherMotherDialogue.Loot.TOOLS:OtherMotherDialogue.Loot.TREASURE;};
        var options=VisitGiftPools.generate(category,rng,VisitGiftPools.progress(p,s));var result=options.get(rng.nextInt(options.size())).copy();result.setCount(Math.min(result.getCount(),result.is(Items.DIAMOND)?1:8));return result;
    }
    private static CompoundTag books(ServerPlayer p){var tag=p.getPersistentData();if(!tag.contains(BOOKS,Tag.TAG_COMPOUND))tag.put(BOOKS,new CompoundTag());return tag.getCompound(BOOKS);}
    private static CompoundTag book(ServerPlayer p,VisitData.Scene s){return books(p).getCompound(HouseParty.data(s).getUUID("PlayerPartyInstance").toString());}
    private static void savePlayer(ServerPlayer p){p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();p.server.getPlayerList().saveAll();}
    /** Genuine secret chambers and archive/storage rooms remain forbidden even when the attic/basement are public. */
    public static boolean allowed(ServerPlayer p,Vec3 at){
        var s=active(p);if(s==null||!configured()||!HouseParty.data(s).getBoolean("Activated"))return false;
        if(HouseParty.data(s).getBoolean("TheatreEligible")&&TheatreSite.allowed(p,s,p.level().dimension(),at))return true;
        var n=HouseParty.data(s);if(!p.level().dimension().location().toString().equals(n.getString("HouseDimension")))return false;
        var pos=BlockPos.containing(at);var house=BlockPos.of(n.getLong("House"));var secrets=SecretTransactions.state(p);
        if(secrets==null)return false;
        for(var room:SecretWorld.rooms(secrets)){var c=BlockPos.of(room.getLong("Pos"));if(Math.abs(c.getX()-pos.getX())<=7&&Math.abs(c.getZ()-pos.getZ())<=7&&pos.getY()>=c.getY()-1&&pos.getY()<c.getY()+10)return false;}
        for(var room:HouseCabinets.ROOMS)if((room.id().equals("archive")||room.id().equals("storage"))&&HouseCabinets.contains(house,room,pos))return false;
        for(var room:HouseCabinets.ROOMS)if((room.id().equals("attic")||room.id().equals("basement"))&&HouseCabinets.contains(house,room,pos))return true;
        if(ShowHouseAccess.onRoute(p,s,at,true)||ShowHouseAccess.onRoute(p,s,at,false))return true;
        if(PartyRegions.rooms().stream().anyMatch(r->r.bounds(house).contains(at)))return true;
        return presentation.grounds(p,s).stream().anyMatch(a->a.dimension().equals(n.getString("HouseDimension"))&&a.bounds().contains(at));
    }
    /** Shared social AI calls this instead of the first/second-floor-only House Party destination. */
    public static Optional<BlockPos> destination(ServerPlayer p,Mob guest,int index,long seed){
        var s=active(p);if(s==null||!configured()||guest.level()!=p.level())return Optional.empty();var n=HouseParty.data(s);
        if(TheatreSite.inside(p,s))return Optional.empty(); // The show controller owns audience seating and intermissions.
        if(!p.level().dimension().location().toString().equals(n.getString("HouseDimension")))return Optional.empty();
        var house=BlockPos.of(n.getLong("House"));var areas=new ArrayList<AABB>();for(var room:PartyRegions.rooms())areas.add(room.bounds(house));
        for(var room:HouseCabinets.ROOMS)if(Set.of("attic","basement").contains(room.id())){var a=HouseCabinets.at(house,room.min()[0],room.min()[1],room.min()[2]);var b=HouseCabinets.at(house,room.max()[0],room.max()[1],room.max()[2]);areas.add(new AABB(a,b.offset(1,1,1)));}
        for(var area:presentation.grounds(p,s))if(area.id().equals("backyard")&&area.dimension().equals(n.getString("HouseDimension")))areas.add(area.bounds());
        if(areas.isEmpty())return Optional.empty();var area=areas.get(Math.floorMod(index,areas.size()));var random=new Random(seed);
        for(int attempt=0;attempt<48;attempt++){
            int x=(int)Math.floor(area.minX+random.nextDouble()*(area.maxX-area.minX)),z=(int)Math.floor(area.minZ+random.nextDouble()*(area.maxZ-area.minZ));
            for(int y=(int)Math.ceil(area.minY);y<area.maxY;y++){var at=new BlockPos(x,y,z);var point=Vec3.atBottomCenterOf(at);
                if(!allowed(p,point)||!OutingSites.feet(p.serverLevel(),at)||!p.serverLevel().noCollision(guest,guest.getDimensions(guest.getPose()).makeBoundingBox(point)))continue;
                if(p.serverLevel().getEntities(guest,new AABB(point,point).inflate(.9,1,.9)).stream().anyMatch(e->e instanceof net.minecraft.world.entity.LivingEntity))continue;
                var path=guest.getNavigation().createPath(at,0);if(path!=null&&path.canReach())return Optional.of(at.immutable());
            }
        }return Optional.empty();
    }
    public static boolean permittedDoor(ServerPlayer p,String room){var s=active(p);return s!=null&&HouseParty.data(s).getBoolean("Activated")&&(room.equals("attic")||room.equals("basement"));}
    public static boolean protectedStorage(ServerPlayer p,BlockPos pos){
        var s=owned(p);if(s==null)return false;var n=HouseParty.data(s);if(!p.level().dimension().location().toString().equals(n.getString("HouseDimension")))return false;
        var house=BlockPos.of(n.getLong("House"));return Arrays.stream(HouseCabinets.ROOMS).anyMatch(r->Set.of("attic","basement","archive").contains(r.id())&&HouseCabinets.contains(house,r,pos));
    }
    public static boolean protectedCabinet(ServerPlayer p,int id){return owned(p)!=null&&p.level().dimension().location().toString().equals(HouseParty.data(owned(p)).getString("HouseDimension"))&&id>=0&&id<HouseCabinets.CABINETS.length&&Set.of("attic","basement","archive").contains(HouseCabinets.CABINETS[id].room());}
    public static void storageWarning(ServerPlayer p,BlockPos at){
        var s=active(p);if(s==null)return;var n=HouseParty.data(s);long now=n.getLong("Clock");n.putLong("StorageAttempt",at.asLong());n.putLong("StorageAttemptUntil",now+40);
        if(now>=n.getLong("StorageWarningAfter")){p.displayClientMessage(Component.literal("Other Mother asked you not to take anything from here."),true);n.putLong("StorageWarningAfter",now+20);}
    }
    public static void backgroundTick(ServerPlayer p,VisitData.Scene s){
        if(!isPlayerParty(s)||active(p)!=s)return;var n=HouseParty.data(s);if(!n.getBoolean("Activated"))return;if(!configured()){n.putString("PreparationBlocker","Player Party presentation adapter is unavailable");return;}
        if(s.loop.suspicion>=1||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP){cancel(p,s);return;}
        recoverReconnect(p,s);
        PartyMatches.tick(p,s); // Must refund an interrupted game even when party time is paused.
        boolean paused=s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p)||p.isSleeping()||n.getBoolean("Suspended");
        PlayerPartyLedger.advance(n,1,paused);if(paused){presentation.finishCircus(s);TheatreAudienceShow.requestFinish(s);if(n.getBoolean("TheatreEligible"))TheatreAudienceShow.tick(p,s);if(n.getBoolean("CircusEligible")){var bob=named(s,"other_bobinsky");presentation.tickCircus(p,s,bob==null?null:entity(p,bob));}return;}
        boolean dirty=false;for(var g:PartyGuests.guests(s))if(g.contains("ArrivedAt")&&!Set.of("QUEUED","ARRIVING","GONE").contains(g.getString("State")))dirty|=PlayerPartyLedger.attended(book(p,s),g.getUUID("Id"));if(dirty)savePlayer(p);
        PartySurveillance.tick(p,s);if(s.loop.secrets.searching())return;PartyGuests.tick(p,s);PartyConversations.tick(p,s);
        if(n.getBoolean("CircusEligible")){var g=named(s,"other_bobinsky");if(g!=null)presentation.tickCircus(p,s,entity(p,g));}
        if(n.getBoolean("TheatreEligible"))TheatreAudienceShow.tick(p,s);
        if(PlayerPartyLedger.phase(n)==SOCIAL&&n.getBoolean("TheatreEligible")&&TheatreSite.inside(p,s)&&!TheatreAudienceShow.playing(s)&&n.getLong("Clock")>=n.getLong("NextTheatre")){
            var spink=named(s,"other_miss_spink");var forcible=named(s,"other_miss_forcible");
            if(spink!=null&&forcible!=null&&(approachTheatre(p,s,spink)&approachTheatre(p,s,forcible))){
                // Shared site moves these exact actors and returns their new entity bindings; no duplicate performers.
                Mob a=TheatreSite.enterGuest(entity(p,spink),p,s),b=TheatreSite.enterGuest(entity(p,forcible),p,s);
                if(a!=null)spink.putUUID("Entity",a.getUUID());if(b!=null)forcible.putUUID("Entity",b.getUUID());
                if(a!=null&&b!=null&&TheatreAudienceShow.start(p,s,a,b)){spink.putString("State","PERFORMING");forcible.putString("State","PERFORMING");inviteAudience(p,s);}
                else {if(a!=null){Mob back=TheatreSite.exitGuest(a,p,s);if(back!=null){spink.putUUID("Entity",back.getUUID());spink.putString("State","WANDERING");}}if(b!=null){Mob back=TheatreSite.exitGuest(b,p,s);if(back!=null){forcible.putUUID("Entity",back.getUUID());forcible.putString("State","WANDERING");}}}
                n.putLong("NextTheatre",n.getLong("Clock")+7200);
            }
        }
        tickAudience(p,s);
        if(n.getBoolean("TheatreEligible")&&n.getLong("Clock")>=n.getLong("TransitionAfter")){
            boolean moved=TheatreSite.inside(p,s)&&TheatreSite.atExit(p,s)?TheatreSite.exit(p,s):PlayerPartyLedger.phase(n)==SOCIAL&&TheatreSite.atEntrance(p,s)&&TheatreSite.enter(p,s);
            if(moved)n.putLong("TransitionAfter",n.getLong("Clock")+40);
        }
        if(PlayerPartyLedger.phase(n)==DRAINING_SHOWS){presentation.finishCircus(s);TheatreAudienceShow.requestFinish(s);
            if(n.getBoolean("AttractionOverdue"))n.putString("PreparationBlocker","A performance exceeded its declared full-track drain bound; awaiting safe finish");
            if(!PartyMatches.active(s)&&!presentation.circusPlaying(s)&&!TheatreAudienceShow.playing(s)){if(returnFromTheatre(p,s)){restoreAudience(s);PlayerPartyLedger.phase(n,ANNOUNCING);}}}
        if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    /** Resolve the canonical actor across levels; do not mistake a theatre transfer for disappearance. */
    private static Mob entity(ServerPlayer p,CompoundTag g){
        if(!g.hasUUID("Entity")||!g.hasUUID("Id"))return null;
        for(var level:p.server.getAllLevels())if(level.getEntity(g.getUUID("Entity")) instanceof Mob m){var tag=m.getPersistentData();
            if(tag.hasUUID("PartyOwner")&&p.getUUID().equals(tag.getUUID("PartyOwner"))&&tag.hasUUID("PartyGuest")&&g.getUUID("Id").equals(tag.getUUID("PartyGuest")))return m;}
        return null;
    }
    private static boolean approachTheatre(ServerPlayer p,VisitData.Scene s,CompoundTag g){
        var actor=entity(p,g);var door=TheatreSite.houseDoor(p,s);if(actor==null||door==null)return false;
        if(!Set.of("WANDERING","CONVERSING","APPROACHING_THEATRE").contains(g.getString("State")))return false;
        if(actor.getPersistentData().contains("TheatreGuestReturn"))return true;
        g.putString("State","APPROACHING_THEATRE");if(actor.distanceToSqr(Vec3.atBottomCenterOf(door))<=9)return true;
        if(actor.tickCount%40==0){if(actor.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}var path=actor.getNavigation().createPath(door,1);if(path!=null&&path.canReach())actor.getNavigation().moveTo(path,.85);}return false;
    }
    private static void inviteAudience(ServerPlayer p,VisitData.Scene s){
        var door=TheatreSite.houseDoor(p,s);if(door==null)return;int count=0;
        for(var g:PartyGuests.guests(s)){var actor=entity(p,g);if(count==4)break;if(actor==null||!Set.of("WANDERING","CONVERSING").contains(g.getString("State"))||actor.distanceToSqr(Vec3.atBottomCenterOf(door))>1024)continue;
            g.putBoolean("TheatreAudience",true);g.putInt("TheatreSeat",20+count++);g.putString("State","APPROACHING_THEATRE");
        }
    }
    private static void tickAudience(ServerPlayer p,VisitData.Scene s){
        if(!HouseParty.data(s).getBoolean("TheatreEligible"))return;
        boolean playing=TheatreAudienceShow.playing(s);
        for(var g:PartyGuests.guests(s)){
            if(!g.getBoolean("TheatreAudience"))continue;var actor=entity(p,g);if(actor==null)continue;
            if(!playing){TheatreSite.unseatGuest(actor,p,s);var returned=TheatreSite.exitGuest(actor,p,s);if(returned!=null){g.putUUID("Entity",returned.getUUID());g.putString("State","WANDERING");g.remove("TheatreAudience");g.remove("TheatreSeat");}continue;}
            if(!TheatreSite.inside(p,s))continue;
            if(!actor.getPersistentData().contains("TheatreGuestReturn")){if(!approachTheatre(p,s,g))continue;actor=TheatreSite.enterGuest(actor,p,s);if(actor==null)continue;g.putUUID("Entity",actor.getUUID());g.putString("State","AUDIENCE");}
            if(TheatreSite.seatGuest(actor,p,s,g.getInt("TheatreSeat"))&&actor.tickCount%200==0)PartyGuests.gesture(actor,7,35);
        }
        // Keep the actual performers available for intermission conversations; finale/cleanup owns their return.
        if(!playing)for(var g:PartyGuests.guests(s))if(g.getString("State").equals("PERFORMING")&&g.getString("Kind").startsWith("other_miss_")){var actor=entity(p,g);if(actor!=null){TheatreSite.unseatGuest(actor,p,s);g.putString("State","WANDERING");PartyGuests.gesture(actor,7,50);}}
    }
    private static boolean returnFromTheatre(ServerPlayer p,VisitData.Scene s){
        if(TheatreSite.inside(p,s)&&!TheatreSite.exit(p,s)){HouseParty.data(s).putString("PreparationBlocker","Safe theatre return is unavailable; party retained for recovery");return false;}
        if(HouseParty.data(s).getBoolean("TheatreEligible")&&!TheatreSite.returnAll(p,s)){HouseParty.data(s).putString("PreparationBlocker","A theatre guest return is pending; party ownership retained for recovery");return false;}
        for(var g:PartyGuests.guests(s)){var actor=entity(p,g);if(actor!=null&&actor.getPersistentData().contains("TheatreGuestReturn")){HouseParty.data(s).putString("PreparationBlocker","A theatre guest could not safely return; party retained for recovery");return false;}}
        return true;
    }
    private static void restoreAudience(VisitData.Scene s){for(var g:PartyGuests.guests(s))if(Set.of("PERFORMING","AUDIENCE","APPROACHING_THEATRE").contains(g.getString("State"))){g.putString("State","WANDERING");g.remove("TheatreAudience");g.remove("TheatreSeat");}}
    private static CompoundTag named(VisitData.Scene s,String kind){return PartyGuests.guests(s).stream().filter(g->g.getString("Kind").equals(kind)).findFirst().orElse(null);}
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(active(p)!=s)return false;var n=HouseParty.data(s);if(!configured())return true;var ledger=book(p,s);long now=n.getLong("Clock");
        if(s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p)||n.getBoolean("Suspended")||now<n.getLong("VoiceUntil")||PartySurveillance.warningSpeechActive(s))return true;
        switch(PlayerPartyLedger.phase(n)){
            case ANNOUNCING->{if(!returnFromTheatre(p,s))return true;PartyConversations.close(p,s);PlayerPartyLedger.unlock(ledger);savePlayer(p);assignCrowd(p,s);n.putLong("VoiceUntil",now+NumberedEventDialogue.mother(p,"player_party","surprise"));PlayerPartyLedger.phase(n,GATHERING);}
            case GATHERING->{Vec3 center=presentation.celebration(p,s);if(p.distanceToSqr(center)>36)return true;if(crowdReady(p,s)||n.getInt("PhaseTicks")>=600){if(!ensureCake(p,s))return true;PlayerPartyLedger.revealCake(ledger);savePlayer(p);n.putLong("VoiceUntil",now+NumberedEventDialogue.mother(p,"player_party","cake"));p.serverLevel().sendParticles(ParticleTypes.FIREWORK,center.x,center.y+2,center.z,36,1.5,1,1.5,.02);for(var g:PartyGuests.guests(s)){var actor=entity(p,g);if(actor!=null)PartyGuests.gesture(actor,7,100);}PlayerPartyLedger.phase(n,CAKE_PRESENTATION);}}
            case CAKE_PRESENTATION->{n.putLong("VoiceUntil",now+NumberedEventDialogue.say(p,"player_party","guest_cheer_1"));PlayerPartyLedger.phase(n,GIFT_CEREMONY);}
            case GIFT_CEREMONY->presentGift(p,s);
            case CAKE_INTERACTION->{if(n.getInt("PhaseTicks")>=3600&&PlayerPartyLedger.awaitingPresentation(ledger).isEmpty())PlayerPartyLedger.phase(n,FINISHING);}
            case FINISHING->{if(!n.getBoolean("ClosingSpoken")){n.putBoolean("ClosingSpoken",true);n.putLong("VoiceUntil",now+NumberedEventDialogue.mother(p,"player_party","finish"));return true;}if(!n.getBoolean("Departing")){PartyGuests.depart(p,s);n.putBoolean("Departing",true);}if(PartyGuests.departed(p,s)){if(!returnFromTheatre(p,s))return true;PlayerPartyLedger.phase(n,COMPLETED);if(cleanup(p,s))VisitEvents.next(p,s,false);}}
            case COMPLETED->{if(cleanup(p,s))VisitEvents.next(p,s,false);}
            default->{}
        }return true;
    }
    private static void assignCrowd(ServerPlayer p,VisitData.Scene s){
        var list=PartyGuests.guests(s).stream().filter(g->{var gift=PlayerPartyLedger.gift(book(p,s),g.getUUID("Id"));return gift!=null&&gift.getBoolean("Attended");}).toList();var center=presentation.celebration(p,s);
        for(int i=0;i<list.size();i++){var g=list.get(i);var actor=entity(p,g);if(actor==null)continue;var target=PlayerPartyPresentation.crowd(center,i);
            var path=actor.getNavigation().createPath(BlockPos.containing(target),0);if(!allowed(p,target)||!OutingSites.feet(p.serverLevel(),BlockPos.containing(target))||!p.serverLevel().noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(target))||path==null||!path.canReach()){g.putBoolean("GiftPresentationFallback",true);continue;}
            g.putLong("CelebrationTarget",BlockPos.containing(target).asLong());g.putString("State","CELEBRATING");actor.stopRiding();actor.getNavigation().moveTo(path,.85);
        }
    }
    public static void control(Mob actor,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        if(g.getString("State").equals("APPROACHING_THEATRE")){approachTheatre(p,s,g);return;}
        String key=g.getString("State").equals("PRESENTING_GIFT")?"PresentTarget":"CelebrationTarget";
        if(!g.contains(key)){actor.getNavigation().stop();return;}var at=BlockPos.of(g.getLong(key));
        if(actor.distanceToSqr(Vec3.atBottomCenterOf(at))<2){actor.getNavigation().stop();actor.getLookControl().setLookAt(p,25,20);}else if(actor.tickCount%40==0)actor.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.85);
    }
    private static boolean crowdReady(ServerPlayer p,VisitData.Scene s){return PartyGuests.guests(s).stream().allMatch(g->{var actor=entity(p,g);return actor==null||g.getBoolean("GiftPresentationFallback")||g.contains("CelebrationTarget")&&actor.distanceToSqr(Vec3.atBottomCenterOf(BlockPos.of(g.getLong("CelebrationTarget"))))<4;});}
    private static ScenePropEntity cake(ServerPlayer p,VisitData.Scene s){return VisitEvents.props(p,s).stream().filter(e->e.slot==CAKE_SLOT&&e.getPersistentData().hasUUID("PlayerParty")&&e.getPersistentData().getUUID("PlayerParty").equals(HouseParty.data(s).getUUID("PlayerPartyInstance"))).findFirst().orElse(null);}
    private static boolean ensureCake(ServerPlayer p,VisitData.Scene s){
        var cake=cake(p,s);if(cake==null){var at=presentation.cakePosition(p,s);if(at==null||!allowed(p,at)||!p.serverLevel().noCollision(new AABB(at.x-.45,at.y,at.z-.45,at.x+.45,at.y+.9,at.z+.45))){HouseParty.data(s).putString("PreparationBlocker","Cake pedestal is missing or obstructed");return false;}
            cake=VisitEvents.prop(p,s,ScenePropEntity.CONTEST_CAKE,at,CAKE_SLOT);cake.cakeStyle(2<<4);cake.getPersistentData().putUUID("PlayerParty",HouseParty.data(s).getUUID("PlayerPartyInstance"));}
        var book=book(p,s);presentation.cakeChanged(p,s,cake,book.getInt("CakeSlices"),book.getBoolean("CandlesOut"));return true;
    }
    private static void presentGift(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);var book=book(p,s);var pending=PlayerPartyLedger.awaitingPresentation(book);if(pending.isEmpty()){PlayerPartyLedger.phase(n,CAKE_INTERACTION);return;}
        var id=pending.get(0);var g=HousePartyLedger.guest(n,id);var actor=g==null?null:entity(p,g);long now=n.getLong("Clock");
        if(!n.hasUUID("Presenting")||!n.getUUID("Presenting").equals(id)){n.putUUID("Presenting",id);n.putLong("PresentStarted",now);if(actor!=null){var target=presentation.celebration(p,s).add(1.5,0,0);g.putLong("PresentTarget",BlockPos.containing(target).asLong());g.putString("State","PRESENTING_GIFT");actor.getNavigation().moveTo(target.x,target.y,target.z,.9);showPresent(p,s,actor,id);}return;}
        if(actor!=null&&actor.level()==p.level())for(var prop:VisitEvents.props(p,s))if(prop.slot==GIFT_SLOT&&prop.getPersistentData().hasUUID("GiftGuest")&&id.equals(prop.getPersistentData().getUUID("GiftGuest")))prop.setPos(actor.position().add(actor.getLookAngle().scale(.85)).add(0,.9,0));
        long elapsed=now-n.getLong("PresentStarted");if(elapsed<30||actor!=null&&actor.distanceToSqr(p)>16&&elapsed<140)return;
        if(actor!=null)PartyGuests.gesture(actor,6,40);
        var gift=PlayerPartyLedger.gift(book,id);String kind=gift.getString("Kind");int speech=40;
        String line=switch(kind){case "other_bobinsky"->"bobinsky_gift";case "other_miss_spink"->"spink_gift";case "other_miss_forcible"->"forcible_gift";case "counterpart"->"counterpart_gift_"+(1+p.getRandom().nextInt(2));default->"";};
        if(!line.isEmpty())speech=NumberedEventDialogue.say(p,"player_party",line);else VisitAudio.caption(p,gift.getString("Name"),kind.equals("other_wybie")?"Wybie holds out a carefully wrapped present and nods.":"A present, just for you.",speech);
        // Each stable guest gets its own persisted present, even if its presentation needed recovery.
        PlayerPartyLedger.presented(book,id);savePlayer(p);deliverPending(p);removePresent(p,s);if(g!=null){g.remove("PresentTarget");g.putString("State","CELEBRATING");}n.remove("Presenting");n.putLong("VoiceUntil",now+Math.max(10,speech));
    }
    private static void showPresent(ServerPlayer p,VisitData.Scene s,Mob actor,UUID guest){
        removePresent(p,s);if(actor.level()!=p.level())return;
        var at=actor.position().add(actor.getLookAngle().scale(.85)).add(0,.9,0);
        var prop=VisitEvents.prop(p,s,ScenePropEntity.GIFT,at,GIFT_SLOT);prop.getPersistentData().putUUID("PlayerParty",HouseParty.data(s).getUUID("PlayerPartyInstance"));prop.getPersistentData().putUUID("GiftGuest",guest);
    }
    private static void removePresent(ServerPlayer p,VisitData.Scene s){UUID instance=HouseParty.data(s).getUUID("PlayerPartyInstance");for(var prop:VisitEvents.props(p,s))if(prop.slot==GIFT_SLOT&&prop.getPersistentData().hasUUID("PlayerParty")&&instance.equals(prop.getPersistentData().getUUID("PlayerParty")))prop.discard();}
    public static boolean interact(ServerPlayer p,ScenePropEntity prop){if(PartyDecorations.interact(p,prop))return true;var s=active(p);if(s==null||!p.getUUID().equals(prop.owner)||!prop.getPersistentData().hasUUID("PlayerParty")||!HouseParty.data(s).getUUID("PlayerPartyInstance").equals(prop.getPersistentData().getUUID("PlayerParty")))return false;if(prop==cake(p,s)&&prop.distanceToSqr(p)<=16)showCake(p,s);return true;}
    private static void showCake(ServerPlayer p,VisitData.Scene s){
        var book=book(p,s);if(!PlayerPartyLedger.cakePresented(book))return;var n=HouseParty.data(s);n.putUUID("CakeNonce",UUID.randomUUID());
        List<String> choices=!book.getBoolean("Wish")?List.of("Make a wish"):!book.getBoolean("CandlesOut")?List.of("Blow out candles"):List.of("Eat a slice","Take the celebration cake home","Back");
        var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putUUID("Nonce",n.getUUID("CakeNonce"));packet.putString("Speaker","Your party cake");packet.putString("Text",book.getBoolean("CandlesOut")?book.getInt("CakeSlices")+" slices remain.":"A cake made especially for you.");packet.put("Options",SecretsState.strings(choices));SecretsNetwork.send(p,packet);
    }
    public static boolean choice(ServerPlayer p,UUID nonce,String action){
        var s=active(p);if(s==null)return false;var n=HouseParty.data(s);if(!n.hasUUID("CakeNonce")||!n.getUUID("CakeNonce").equals(nonce))return false;
        var prop=cake(p,s);if(prop==null||prop.distanceToSqr(p)>16||SecretDialogue.busy(p)||Reconciliation.active(p))return true;
        if(action.equals("close")){n.remove("CakeNonce");return true;}
        int choice;try{choice=Integer.parseInt(action);}catch(NumberFormatException bad){return true;}var book=book(p,s);
        if(!book.getBoolean("Wish")){if(choice!=0)return true;PlayerPartyLedger.wish(book);}else if(!book.getBoolean("CandlesOut")){if(choice!=0)return true;PlayerPartyLedger.blowCandles(book);}else if(choice==0){if(p.canEat(false)&&PlayerPartyLedger.takeSlice(book))p.getFoodData().eat(2,.1F);}else if(choice==1)PlayerPartyLedger.reserveTakeHomeCake(book);else if(choice==2){n.remove("CakeNonce");var close=new CompoundTag();close.putBoolean("VisitConversation",true);close.putBoolean("Close",true);SecretsNetwork.send(p,close);return true;}else return true;
        savePlayer(p);deliverPending(p);n.remove("CakeNonce");ensureCake(p,s);showCake(p,s);return true;
    }
    public static void deliverPending(ServerPlayer p){
        var scene=OtherMotherLoop.scene(p);if(scene!=null&&scene.loop.event.getBoolean("ShowCleanupDeferred"))return;
        if(!p.getPersistentData().contains(BOOKS))return;boolean changed=false;
        for(String key:books(p).getAllKeys()){var book=books(p).getCompound(key);if(!book.hasUUID("Owner")||!p.getUUID().equals(book.getUUID("Owner")))continue;
            for(var tag:book.getList("Gifts",Tag.TAG_COMPOUND)){var gift=(CompoundTag)tag;UUID id=gift.getUUID("Guest");int count=PlayerPartyLedger.remaining(book,p.getUUID(),id);if(count<=0)continue;var stack=ItemStack.of(gift.getCompound("Payload")).copyWithCount(count);p.getInventory().add(stack);if(stack.getCount()!=count){PlayerPartyLedger.delivered(book,p.getUUID(),id,stack.getCount());changed=true;}}
            if(book.getBoolean("CakePending")){var stack=new ItemStack(Items.CAKE);p.getInventory().add(stack);if(stack.isEmpty()){PlayerPartyLedger.completeCakeDelivery(book);changed=true;}}
        }if(changed)savePlayer(p);
    }
    public static void parentControl(com.coraline.parents.ParentEntity actor,ServerPlayer p,VisitData.Scene s){
        if(!configured()||actor.level()!=p.level())return;var n=HouseParty.data(s);actor.stopRiding();actor.setMaxUpStep(1.05F);
        if(PlayerPartyLedger.phase(n).ordinal()>=ANNOUNCING.ordinal()){
            var at=presentation.celebration(p,s).add(actor.isMom()?2:-2,0,-2);if(actor.distanceToSqr(at)<3){actor.getNavigation().stop();actor.lookAtStoryPlayer(p);}else if(actor.tickCount%40==0)actor.getNavigation().moveTo(at.x,at.y,at.z,.85);return;
        }
        if(actor.tickCount%100!=0||p.getRandom().nextInt(3)!=0)return;
        destination(p,actor,p.getRandom().nextInt(32),p.getRandom().nextLong()).ifPresent(at->actor.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.75));
    }
    public static boolean interact(ServerPlayer p,com.coraline.parents.ParentEntity actor){
        var s=active(p);if(s==null||!Objects.equals(actor.getUUID(),actor.isMom()?s.mom:s.dad)||actor.distanceToSqr(p)>36||!actor.hasLineOfSight(p)||s.loop.secrets.searching()||SecretDialogue.busy(p))return false;
        if(actor.isMom()&&MotherInterrupts.blackmail(p))return true;var n=HouseParty.data(s);if(!n.getBoolean("Activated")||n.getLong("Clock")<n.getLong("HostVoiceUntil")||PartySurveillance.warningSpeechActive(s))return true;
        PartyConversations.close(p,s);int length;
        if(actor.isMom())length=NumberedEventDialogue.mother(p,"player_party","check");
        else {String category=PlayerPartyLedger.phase(n)==FINISHING?"finish":PlayerPartyLedger.phase(n).ordinal()>=ANNOUNCING.ordinal()?"celebration":n.getLong("Clock")<2400?"greeting":p.getRandom().nextBoolean()?"entertainment":"social";length=NumberedEventDialogue.say(p,"player_party","father_"+category+"_"+(1+p.getRandom().nextInt(2)));}
        n.putLong("HostVoiceUntil",n.getLong("Clock")+length+80);actor.lookAtStoryPlayer(p);return true;
    }
    /** Debug never fabricates attendance, bypasses preparation, or directly awards a gift. */
    public static void debug(ServerPlayer p,String action){
        if(!p.hasPermissions(2))return;var s=active(p);if(s==null)return;var n=HouseParty.data(s);
        switch(action){case "state"->p.sendSystemMessage(Component.literal("Player Party "+PlayerPartyLedger.phase(n)+", social="+n.getInt("SocialElapsed")+", guests="+PartyGuests.guests(s).size()+", gifts waiting="+PlayerPartyLedger.awaitingPresentation(book(p,s)).size()+", blocker="+n.getString("PreparationBlocker")));
            case "finale"->{if(PlayerPartyLedger.phase(n)==SOCIAL){n.putInt("SocialElapsed",PlayerPartyLedger.SOCIAL_TICKS);PlayerPartyLedger.advance(n,0,false);}}
            case "resume"->resume(p,s);case "deliver"->deliverPending(p);default->{} }
        OtherMotherLoop.changed(p);
    }
    public static void suspend(ServerPlayer p,VisitData.Scene s){if(s==null||owned(p)!=s)return;HouseParty.data(s).putBoolean("Suspended",true);PartyConversations.close(p,s);PartyMatches.suspend(p,s);HouseParty.data(s).remove("CakeNonce");}
    public static void resume(ServerPlayer p,VisitData.Scene s){if(owned(p)!=s||OtherMotherLoop.active(p)!=s||s.loop.event.getBoolean("ShowCleanupDeferred"))return;HouseParty.data(s).putBoolean("Suspended",false);HousePartyLedger.reconciled(HouseParty.data(s));deliverPending(p);OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.PARTY_SOCIAL);}
    /** A benign reconnect is not a reconciliation: retain the inner phase, clocks and absence incident. */
    private static boolean mayRecover(ServerPlayer p,VisitData.Scene s){return s!=null&&active(p)==s&&s.loop.suspicion<1&&s.loop.secrets.search!=SecretsState.Search.SCRIPTED_TRAP&&!s.loop.secrets.searching()&&!Reconciliation.active(p)&&!SecretDialogue.busy(p);}
    private static void recoverReconnect(ServerPlayer p,VisitData.Scene s){
        if(!mayRecover(p,s))return;var n=HouseParty.data(s);
        if(!n.getBoolean("ReconnectPending")||!n.getBoolean("Activated")||PlayerPartyLedger.phase(n)==CANCELLED)return;
        n.putBoolean("Suspended",false);n.remove("ReconnectPending");OtherMotherLoop.changed(p);
    }
    public static void reconnect(ServerPlayer p,VisitData.Scene s){
        if(s==null||owned(p)!=s)return;
        suspend(p,s);HouseParty.data(s).putBoolean("ReconnectPending",true);TheatreAudienceShow.suspend(p,s);BobinskyAudienceShow.suspend(p,s);PartyEscrow.recover(p,Set.of());recoverReconnect(p,s);deliverPending(p);
        /* TheatreSite owns exact safe-return recovery before a new show. */
    }
    public static void cancel(ServerPlayer p,VisitData.Scene s){PlayerPartyLedger.cancel(book(p,s));PlayerPartyLedger.phase(HouseParty.data(s),CANCELLED);savePlayer(p);cleanup(p,s);}
    public static boolean cleanup(ServerPlayer p,VisitData.Scene s){
        if(!isPlayerParty(s)||!p.getUUID().equals(s.owner))return true;if(!returnFromTheatre(p,s))return false;PartyConversations.close(p,s);PartyMatches.cleanup(p,s);PartyEscrow.cancelSerial(p,s.loop.event.getInt("Serial"));
        boolean theatreRestored=TheatreAudienceShow.cleanup(p,s),circusRestored=BobinskyAudienceShow.cleanup(p,s);if(!theatreRestored||!circusRestored){HouseParty.data(s).putString("PreparationBlocker","A party performer is awaiting safe original-state restoration");return false;}if(HouseParty.data(s).getBoolean("TheatrePrepared")&&!TheatreSite.release(p,s)){HouseParty.data(s).putString("PreparationBlocker","Theatre release is awaiting a safe actor return");return false;}ShowHouseAccess.restore(p,s);if(configured())presentation.cleanup(p,s);
        UUID instance=HouseParty.data(s).getUUID("PlayerPartyInstance");for(var prop:VisitEvents.props(p,s))if(prop.getPersistentData().hasUUID("PlayerParty")&&prop.getPersistentData().getUUID("PlayerParty").equals(instance)){prop.ejectPassengers();prop.discard();}
        PartyGuests.cleanup(p,s);s.loop.event.remove("PartyPending");s.loop.event.remove("Party");PartyStorageGuard.release();deliverPending(p);OtherMotherLoop.changed(p);return true;
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p&&p.tickCount%100==0&&!p.isDeadOrDying())deliverPending(p);}
    @SubscribeEvent public static void cloned(PlayerEvent.Clone event){if(event.getOriginal().getPersistentData().contains(BOOKS))event.getEntity().getPersistentData().put(BOOKS,event.getOriginal().getPersistentData().getCompound(BOOKS).copy());}
    public static String debugOperator(ServerPlayer p,VisitData.Scene s,String action,String value){
        if(!NumberedDebugActions.operator(p,s)||active(p)!=s||!configured())return "No configured, owned Player Party is active.";
        var n=HouseParty.data(s);var phase=PlayerPartyLedger.phase(n);var ledger=book(p,s);
        if(action.equals("inventory"))return "Inventory free slots: "+p.getInventory().items.stream().filter(ItemStack::isEmpty).count()+". Full-inventory gifts remain in the persisted delivery queue; no inventory is overwritten.";
        if(action.equals("duplicate")){
            int claimed=0,remaining=0;var ids=new HashSet<UUID>();
            for(var tag:ledger.getList("Gifts",Tag.TAG_COMPOUND)){var gift=(CompoundTag)tag;if(!gift.hasUUID("Guest")||!ids.add(gift.getUUID("Guest")))return "Gift ledger audit FAILED: duplicate/missing stable guest identity.";if(gift.getBoolean("Claimed")){claimed++;if(PlayerPartyLedger.remaining(ledger,p.getUUID(),gift.getUUID("Guest"))!=0)return "Gift ledger audit FAILED: claimed item became deliverable.";}else remaining+=PlayerPartyLedger.remaining(ledger,p.getUUID(),gift.getUUID("Guest"));}
            return "Read-only gift audit: "+ids.size()+" unique guests, "+claimed+" claimed, "+remaining+" deliverable items. Claimed entries cannot be reissued.";
        }
        if(s.loop.secrets.searching()||Reconciliation.active(p)||SecretDialogue.busy(p))return "Finish actual pursuit/reconciliation/dialogue before this control.";
        switch(action){
            case "resume"->{resume(p,s);return "Resumed after reconciliation; the original clock and gifts were retained.";}
            case "backyard"->{for(var area:presentation.grounds(p,s))if(area.id().equals("backyard")&&area.dimension().equals(p.level().dimension().location().toString()))return NumberedDebugActions.teleport(p,BlockPos.containing(area.bounds().getCenter()));return "No verified backyard in the current dimension.";}
            case "attic"->{if(TheatreSite.inside(p,s))return "Return from the theatre before going to the attic.";return NumberedDebugActions.teleport(p,MouseCircusSite.atticDoor(p,s));}
            case "theatre"->{if(TheatreSite.inside(p,s))return TheatreSite.exit(p,s)?"Returned through the saved basement transition.":"Safe theatre return is blocked.";return NumberedDebugActions.teleport(p,TheatreSite.houseDoor(p,s));}
            case "regenerate"->{if(!n.getBoolean("TheatreEligible"))return "The real Spink/Forcible progression is not complete.";return TheatreSite.prepare(p,s)?"Shared theatre validated/prepared without replacing occupied structures.":"Shared theatre preparation remains blocked.";}
            case "bobinsky"->{if(!n.getBoolean("CircusEligible")||phase!=SOCIAL)return "Requires real Bobinsky attendance and social phase.";n.putLong("NextCircus",n.getLong("Clock"));var g=named(s,"other_bobinsky");presentation.tickCircus(p,s,g==null?null:entity(p,g));OtherMotherLoop.changed(p);return "Attic show requested with actual Bobinsky; reach the attic and allow his navigation.";}
            case "spink"->{if(!n.getBoolean("TheatreEligible")||phase!=SOCIAL)return "Requires eligible Spink/Forcible and social phase.";n.putLong("NextTheatre",n.getLong("Clock"));OtherMotherLoop.changed(p);return "Theatre show is due; enter the real venue and allow the actual performers to arrive.";}
            case "enter"->{if(!n.getBoolean("TheatreEligible")||phase!=SOCIAL)return "Basement remains normal when theatre prerequisites are absent.";return TheatreSite.enter(p,s)?"Entered the owned theatre through its real doorway.":"Stand at the real basement entrance; safe entry remains gated.";}
            case "basement"->{if(n.getBoolean("TheatreEligible"))return "Theatre is eligible in this actual party. Normal-basement fallback requires a party whose real prerequisites are absent.";return NumberedDebugActions.teleport(p,TheatreSite.houseDoor(p,s));}
            case "storage"->{if(!protectedStorage(p,p.blockPosition()))return "Stand beside actual protected attic/basement storage to test its warning.";storageWarning(p,p.blockPosition());OtherMotherLoop.changed(p);return "Actual protected-area warning published; extraction remains blocked.";}
            case "finale"->{debug(p,"finale");return "Finale requested; full recordings and active games drain before the celebration.";}
            case "assemble"->{if(phase==ANNOUNCING){tick(p,s);return "Normal announcement/gathering tick ran.";}if(phase!=GATHERING)return "Guest assembly requires the normal gathering checkpoint.";assignCrowd(p,s);return "Actual attendees assigned reachable crowd spots.";}
            case "cake"->{if(phase!=GATHERING&&phase!=CAKE_PRESENTATION)return "Cake presentation requires the gathering checkpoint.";tick(p,s);return "Normal cake tick ran; real proximity, voice and pedestal checks still apply.";}
            case "cheer"->{if(phase.ordinal()<GATHERING.ordinal()||phase.ordinal()>CAKE_INTERACTION.ordinal())return "Cheering requires the active celebration.";for(var g:PartyGuests.guests(s)){var actor=entity(p,g);if(actor!=null&&actor.level()==p.level())PartyGuests.gesture(actor,7,100);}return "Actual nearby guests cheered; no attendance or reward flags changed.";}
            case "gift"->{
                if(phase!=GIFT_CEREMONY&&phase!=CAKE_INTERACTION)return "Earned gifts require the real gift ceremony.";
                var roster=PartyGuests.guests(s);if(roster.isEmpty())return "No guest roster.";int index=NumberedDebugActions.integer(value,0,roster.size()-1);UUID id=roster.get(index).getUUID("Id");
                if(!PlayerPartyLedger.presented(ledger,id))return "That guest did not actually attend, was already presented, or rewards are not unlocked.";
                n.putBoolean("OperatorGiftPresentation",true);savePlayer(p);deliverPending(p);OtherMotherLoop.changed(p);return "OPERATOR TEST: one actual attendee's unpresented gift queued once; full inventory remains safe.";
            }
            case "deliver"->{deliverPending(p);return "Retried only already-presented, unclaimed gifts through the persisted delivery queue.";}
            case "complete"->{if(phase!=FINISHING&&phase!=COMPLETED)return "Completion requires the normal finishing checkpoint; no celebration/reward flags were fabricated.";tick(p,s);return "Normal ending tick ran; actual guest departure and safe venue cleanup remain required.";}
            default->{return "Unknown Player Party control.";}
        }
    }
    public static void debugInspect(ServerPlayer p,VisitData.Scene s,List<String> out){
        if(!NumberedDebugActions.operator(p,s)||owned(p)!=s)return;
        var n=HouseParty.data(s);var ledger=book(p,s);
        out.add("Presentation blocker: "+n.getString("PresentationBlocker"));out.add("Attic show: "+BobinskyAudienceShow.blocker(s));out.add("Theatre show: "+TheatreAudienceShow.blocker(s));
        out.add("Actual attendance frozen "+ledger.getBoolean("Unlocked")+" | awaiting presentation "+PlayerPartyLedger.awaitingPresentation(ledger).size());
        out.add("Cake presented "+PlayerPartyLedger.cakePresented(ledger)+" | candles out "+ledger.getBoolean("CandlesOut")+" | slices "+ledger.getInt("CakeSlices"));
        int index=0;for(var tag:ledger.getList("Gifts",Tag.TAG_COMPOUND)){if(index++>=32)break;var gift=(CompoundTag)tag;out.add("Gift "+gift.getString("Name")+": attended="+gift.getBoolean("Attended")+", presented="+gift.getBoolean("Presented")+", claimed="+gift.getBoolean("Claimed")+", remaining="+gift.getInt("Remaining"));}
    }

    private PlayerParty(){}
}
