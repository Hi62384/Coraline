import com.coraline.palace.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import static com.coraline.palace.PalaceSitePlan.Kind.*;

/** Real planner/NBT rules only. Does not launch Minecraft or generate terrain. */
public final class PalaceSiteSmoke {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static final PalaceSiteData.Survey SAFE=new PalaceSiteData.Survey(true,true,true,4,true);
    private static PalaceSitePlan.Point next(PalaceSiteData data,PalaceSitePlan.Kind kind){for(int i=0;i<4096;i++){var proposed=data.proposal(kind);if(proposed.isPresent())return proposed.get();}throw new AssertionError("No bounded proposal");}
    public static void main(String[] args)throws Exception{
        var origin=new PalaceSitePlan.Point(550_000,-820_000);
        for(long seed=-70;seed<70;seed++)for(int index=0;index<160;index++){
            var palace=PalaceSitePlan.candidate(seed,origin,PALACE,index,null).orElseThrow();
            check(palace.equals(PalaceSitePlan.candidate(seed,origin,PALACE,index,null).orElseThrow()),"Restart changed deterministic candidate");
            check(PalaceSitePlan.validPalaceDistance(origin,palace),"Distance was not measured from displaced world spawn");
            var well=PalaceSitePlan.candidate(seed,origin,WELL,index,palace).orElseThrow();
            check(well.distance(palace)>=220&&well.distance(palace)<=450,"Well is not in the required scenic band");
            check(PalaceSitePlan.regionWeight(seed,palace,palace.x(),palace.z())==1,"Connected region lost its center");
            check(PalaceSitePlan.regionWeight(seed,palace,palace.x()+451,palace.z())==0,"Regional biome leaked outside its unique bounds");
        }
        for(int edge:new int[]{-PalaceSitePlan.WORLD_LIMIT,PalaceSitePlan.WORLD_LIMIT}){
            var spawn=new PalaceSitePlan.Point(edge,edge);int allowed=0;
            for(int i=0;i<100;i++){var candidate=PalaceSitePlan.candidate(8,spawn,PALACE,i,null);if(candidate.isPresent()){allowed++;check(PalaceSitePlan.validPalaceDistance(spawn,candidate.get()),"Border handling clamped/changed radius");}}
            check(allowed>0&&allowed<100,"World-border proposals were not safely filtered");
        }
        var data=new PalaceSiteData();data.initialize(517,new BlockPos(origin.x(),70,origin.z()));
        check(data.proposal(COMPANY).isEmpty()&&data.proposal(WELL).isEmpty(),"Dependent site allocated before Palace identity was fixed");
        var palace=next(data,PALACE);var persisted=PalaceSiteData.load(data.save(new CompoundTag()));
        check(next(persisted,PALACE).equals(palace),"Reload rerolled pending Palace");
        var changedPending=data.save(new CompoundTag());changedPending.getList("Sites",Tag.TAG_COMPOUND).getCompound(0).putInt("CandidateX",palace.x()+1);
        var damagedPending=PalaceSiteData.load(changedPending);check(!damagedPending.blocker().isBlank()&&!damagedPending.reject(PALACE,new PalaceSitePlan.Point(palace.x()+1,palace.z())),"Corrupt pending proposal was accepted/rejected into a reroll");
        changedPending=data.save(new CompoundTag());changedPending.getList("Sites",Tag.TAG_COMPOUND).getCompound(0).putInt("Candidate",0);
        check(!PalaceSiteData.load(changedPending).blocker().isBlank(),"Pending proposal has no deterministic cursor");
        changedPending=data.save(new CompoundTag());changedPending.putString("Initialized","false");check(!PalaceSiteData.load(changedPending).blocker().isBlank(),"Malformed initialization flag discarded world identity");
        persisted.initialize(999,new BlockPos(0,70,0));check(next(persisted,PALACE).equals(palace),"Changed world spawn relocated saved origin");
        var place=new BlockPos(palace.x(),100,palace.z());
        check(!persisted.reserve(PALACE,place,new PalaceSiteData.Survey(false,true,true,0,true)),"Explored terrain was accepted");
        check(!persisted.reserve(PALACE,place,new PalaceSiteData.Survey(true,true,false,0,true)),"Water accepted");
        check(!persisted.reserve(PALACE,place,new PalaceSiteData.Survey(true,true,true,Double.NaN,true)),"Invalid slope accepted");
        check(!persisted.reserve(PALACE,place,new PalaceSiteData.Survey(true,true,true,11,true)),"Steep terrain accepted");
        check(persisted.reserve(PALACE,place,SAFE),"Safe surveyed reservation failed");
        check(!persisted.generated(PALACE),"Reservation falsely became a built structure");
        check(persisted.proposal(PALACE).isEmpty()&&!persisted.reserve(PALACE,place.east(),SAFE),"Second Palace was allocated");
        check(persisted.reserve(PALACE,place,SAFE),"Idempotent retry rejected original Palace");
        var well=next(persisted,WELL);var wellAt=new BlockPos(well.x(),120,well.z());
        check(!persisted.reserve(WELL,wellAt,new PalaceSiteData.Survey(true,true,true,0,false)),"Well without actual Palace visibility accepted");
        check(persisted.reserve(WELL,wellAt,SAFE),"Surveyed visible well rejected");
        var company=next(persisted,COMPANY);var companyAt=new BlockPos(company.x(),80,company.z());
        check(persisted.reserve(COMPANY,companyAt,SAFE),"Company reservation failed");
        check(company.distance(palace)>=600,"Company overlaps Palace biome");
        for(int reload=0;reload<40;reload++){
            persisted=PalaceSiteData.load(persisted.save(new CompoundTag()));
            check(persisted.position(PALACE).orElseThrow().equals(place)&&persisted.position(WELL).orElseThrow().equals(wellAt)&&persisted.position(COMPANY).orElseThrow().equals(companyAt),"Reload moved/duplicated a world landmark");
            check(persisted.proposal(PALACE).isEmpty()&&persisted.proposal(WELL).isEmpty()&&persisted.proposal(COMPANY).isEmpty(),"Another owner/chunk can allocate duplicate landmarks");
        }
        var legacy=new PalaceSiteData();legacy.initialize(2,BlockPos.ZERO);check(legacy.adoptExisting(PALACE,new BlockPos(50,100,0)),"Existing nearby Palace not adopted");
        check(legacy.legacy(PALACE)&&legacy.generated(PALACE)&&!legacy.compatibility(PALACE).isBlank(),"Historical distance incompatibility was hidden");
        check(legacy.proposal(PALACE).isEmpty()&&!legacy.adoptExisting(PALACE,new BlockPos(2000,100,0)),"Legacy conflict made a duplicate Palace");
        var corrupt=persisted.save(new CompoundTag());var list=corrupt.getList("Sites",Tag.TAG_COMPOUND);list.add(list.getCompound(0).copy());corrupt.putString("UnknownFutureMetadata","preserve me");
        var blocked=PalaceSiteData.load(corrupt);check(!blocked.blocker().isBlank()&&blocked.proposal(PALACE).isEmpty(),"Duplicate saved registry silently rerolled");
        var retained=blocked.save(new CompoundTag());check(retained.getList("Sites",Tag.TAG_COMPOUND).size()==4&&retained.getString("UnknownFutureMetadata").equals("preserve me"),"Corrupt original data was discarded");
        var malformed=persisted.save(new CompoundTag());malformed.getList("Sites",Tag.TAG_COMPOUND).getCompound(0).putString("Position","not a coordinate");
        check(!PalaceSiteData.load(malformed).blocker().isBlank(),"Malformed saved anchor would be rerolled");
        malformed=persisted.save(new CompoundTag());malformed.getList("Sites",Tag.TAG_COMPOUND).getCompound(0).putLong("Position",place.east().asLong());
        check(!PalaceSiteData.load(malformed).blocker().isBlank(),"Reserved Palace moved away from its original proposal");
        malformed=persisted.save(new CompoundTag());malformed.getList("Sites",Tag.TAG_COMPOUND).getCompound(0).putString("Legacy","false");
        check(!PalaceSiteData.load(malformed).blocker().isBlank(),"Malformed legacy flag normalized silently");
        malformed=persisted.save(new CompoundTag());var reordered=malformed.getList("Sites",Tag.TAG_COMPOUND);var lastSite=reordered.remove(2);reordered.add(0,lastSite);
        check(PalaceSiteData.load(malformed).blocker().isBlank(),"Landmark validation depends on NBT list ordering");
        var future=persisted.save(new CompoundTag());future.putInt("Version",99);var futureData=PalaceSiteData.load(future);
        check(futureData.save(new CompoundTag()).getInt("Version")==99&&!futureData.blocker().isBlank(),"Unknown schema was rewritten");
        var rejected=new PalaceSiteData();rejected.initialize(8,BlockPos.ZERO);var first=next(rejected,PALACE);check(rejected.reject(PALACE,first),"Failed survey could not be rejected");check(!next(rejected,PALACE).equals(first),"Explicit rejection repeated old proposal");
        var center=new PalaceSitePlan.Point(0,0);
        for(int angle=0;angle<180;angle++){double a=angle*Math.PI/90,last=1;for(int radius=0;radius<=451;radius++){
            double weight=PalaceSitePlan.regionWeight(7,center,Math.cos(a)*radius,Math.sin(a)*radius);
            check(weight>=0&&weight<=last+1e-12,"Region has disconnected bands or unbounded edge weight");last=weight;
        }}
        check(PalaceJourneyTiming.truckBeat(0)==PalaceJourneyTiming.TruckBeat.WAIT&&PalaceJourneyTiming.truckBeat(59)==PalaceJourneyTiming.TruckBeat.WAIT,"Truck starts driving before three seconds");
        check(PalaceJourneyTiming.truckBeat(60)==PalaceJourneyTiming.TruckBeat.DRIVING&&PalaceJourneyTiming.truckBeat(159)==PalaceJourneyTiming.TruckBeat.DRIVING,"Truck driving interval is not exactly five seconds");
        check(PalaceJourneyTiming.truckBeat(160)==PalaceJourneyTiming.TruckBeat.TRANSFER_REQUIRED,"Physical destination transfer not required at eight seconds");
        for(int t=0;t<=200;t++){double rock=PalaceJourneyTiming.truckRock(t,.5);check(Double.isFinite(rock)&&Math.abs(rock)<=.37,"Unbounded truck camera rocking");if(t<60||t>=160)check(rock==0,"Truck still shakes while parked");}
        for(int duration=41;duration<=1500;duration+=7){double last=0;check(PalaceJourneyTiming.downhillProgress(40,duration)==0&&PalaceJourneyTiming.downhillProgress(duration,duration)==1,"A+2/A-end bicycle anchors changed");for(int t=0;t<=duration;t++){double progress=PalaceJourneyTiming.downhillProgress(t,duration);check(progress>=last&&progress<=1,"Bicycle choreography jumped backward");last=progress;}}
        check(PalaceJourneyTiming.HAND_DRAG==100,"Hand drag is not five seconds");
        for(int length=0;length<=32;length++){int window=PalaceJourneyTiming.rescueWindow(length);check(window>=100&&window<=160,"Unfair rescue timer");}
        for(char cue='A';cue<='I';cue++){check(PalaceJourneyTiming.canPlayWybieRecording(cue,false,false),"Surviving branch lost original dialogue");check(!PalaceJourneyTiming.canPlayWybieRecording(cue,true,false),"Previously dead Wybie speaks");check(PalaceJourneyTiming.canPlayWybieRecording(cue,false,true)==(cue<='D'),"New well death failed to suppress later E-I");}
        com.google.gson.JsonObject originals;try(var reader=java.nio.file.Files.newBufferedReader(java.nio.file.Path.of(args[0]))){originals=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();}
        var durations=new java.util.HashMap<String,Integer>();for(var entry:originals.entrySet())durations.put(entry.getKey(),entry.getValue().getAsJsonObject().get("ticks").getAsInt());
        check(com.coraline.feast.FinalChapterRecordings.SOURCES.keySet().equals(durations.keySet()),"Runtime original catalogue omits/disagrees with supplied originals");
        for(var source:com.coraline.feast.FinalChapterRecordings.SOURCES.entrySet())check(source.getValue().equals(originals.getAsJsonObject(source.getKey()).get("source").getAsString()),"Runtime event changed original recording identity");
        var opening=new com.coraline.feast.NauticalOpeningPlan(durations);check(opening.cues().size()==14,"Opening A-N recordings omitted");
        for(var cue:opening.cues()){
            check(cue.sound().startsWith("theatre_opening_")&&!cue.sound().equals("theatre_music")&&!cue.sound().equals("theatre_"+Character.toLowerCase(cue.letter())),"Opening confused with old music/diving namespace");
            for(int t=0;t<=cue.duration();t++){
                var f=opening.frame(cue.letter(),t);check(f.curtain()>=0&&f.curtain()<=1&&f.boatScene()>=0&&f.boatScene()<=1&&f.collapse()>=0&&f.collapse()<=1,"Unbounded scenic machinery");
                if(cue.letter()=='M')check(f.bothOnstage()&&f.clamLift()==0,"M removed Forcible while Spink returned");
            }
        }
        int a=durations.get("theatre_opening_a"),b=durations.get("theatre_opening_b"),c=durations.get("theatre_opening_c"),e=durations.get("theatre_opening_e"),f=durations.get("theatre_opening_f"),n=durations.get("theatre_opening_n");
        check(opening.frame('A',0).curtain()==0&&opening.frame('A',a).curtain()==1,"A curtain endpoints wrong");
        check(opening.frame('B',b).fishHeadLift()==0&&opening.frame('C',0).fishHeadLift()==0&&opening.frame('C',c).fishHeadLift()==8,"Fish costume head lifts prematurely or actor head is used");
        check(opening.frame('E',e-40).boatScene()==0&&opening.frame('E',e-40).clamLift()==0&&opening.frame('E',e).forcibleYaw()==180,"E-minus-two transition/away-facing anchor wrong");
        check(opening.frame('F',0).forcibleYaw()==180&&opening.frame('F',f-40).finalForciblePose()&&opening.frame('F',f).forcibleSway()==0,"Forcible turn/final two-second pose wrong");
        check(opening.frame('N',0).curtain()==1&&opening.frame('N',n).curtain()==0,"N failed to close curtain before second act");
        checks+=PalaceWellSmoke.run();
        checks+=PalaceKeySmoke.run();
        checks+=PalaceGenerationSmoke.run(java.nio.file.Path.of(args[0]).getParent());
        checks+=PalaceTransportSmoke.run();
        checks+=PalaceWybieSmoke.run();
        checks+=PalaceCombatSmoke.run();
        System.out.println("PALACE_SITE_RULES_PASSED "+checks+" checks; deterministic proposals, durable encounter rules and real NBT reload only, NOT worldgen/terrain/LOS/gameplay acceptance");
    }
}
