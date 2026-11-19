package com.coraline.palace;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import static com.coraline.palace.PalaceSitePlan.Kind;

/** One world registry, never per-player/per-chunk allocation. Generation is not
 * enabled by this foundation: a future terrain survey/builder must consume its
 * reservations, and an operator must resolve corrupt saves without rerolling.
 */
public final class PalaceSiteData extends SavedData {
    private final EnumMap<Kind,CompoundTag> sites=new EnumMap<>(Kind.class);
    private boolean initialized;
    private long seed;
    private PalaceSitePlan.Point spawn;
    private String blocker="";
    private CompoundTag incompatibleOriginal;
    private CompoundTag generation=new CompoundTag();
    CompoundTag generation(){return generation;}
    void stopGeneration(String reason){if(blocker.isEmpty()){blocker=reason;setDirty();}}
    boolean reselectUnbuiltSite(){
        if(generation.contains("DeferredPalace")&&!position(Kind.COMPANY).isPresent()){
            var site=sites.get(Kind.COMPANY);
            if(site!=null){site.remove("Pending");site.remove("CandidateX");site.remove("CandidateZ");}
            for(String name:new String[]{"CompanyClaimed","CompanyX","CompanyY","CompanyZ"})generation.remove(name);
            PalaceGenerationRules.enter(generation,PalaceGenerationRules.Stage.SELECT_COMPANY);
            blocker="";
            setDirty();
            return true;
        }
        if(incompatibleOriginal!=null||PalaceGenerationRules.stage(generation).ordinal()>PalaceGenerationRules.Stage.SURVEY_COMPANY.ordinal()
                ||Arrays.stream(Kind.values()).anyMatch(this::generated))return false;
        var palace=sites.get(Kind.PALACE);
        int next=palace==null?0:palace.getInt("Candidate");
        sites.clear();
        if(next>0){var cursor=new CompoundTag();cursor.putInt("Candidate",next);sites.put(Kind.PALACE,cursor);}
        generation=new CompoundTag();
        blocker="";
        setDirty();
        return true;
    }
    void retryLegacyPause(){
        if(incompatibleOriginal!=null)return;
        // Older builds crashed while grading an early Moving Company because
        // COMPANY_PREPARE incorrectly read Palace/Well positions which did not
        // exist yet. Resume the exact reserved Company cursor, never reroll.
        boolean earlyCompanyMissingPalace=blocker.startsWith("Palace generation needs review at COMPANY_PREPARE cursor ")
                &&blocker.endsWith(": No value present")
                &&generation.contains("DeferredPalace",Tag.TAG_COMPOUND)
                &&PalaceGenerationRules.stage(generation)==PalaceGenerationRules.Stage.COMPANY_PREPARE
                &&generation.getBoolean("CompanyClaimed")&&position(Kind.COMPANY).isPresent()
                &&position(Kind.PALACE).isEmpty()&&position(Kind.WELL).isEmpty();
        if(earlyCompanyMissingPalace
                ||blocker.equals("Palace generation stopped safely: IllegalStateException: Landmark chunk request failed safely")
                ||blocker.startsWith("Palace generation needs review at ")
                &&blocker.contains(": Actual block placement was refused at ")){
            blocker="";
            setDirty();
        }else if(blocker.startsWith("Player exploration reached the pending region; job stopped")
                ||blocker.startsWith("Player/world edit inside pending landmark footprint;")){
            reselectUnbuiltSite();
        }
    }
    boolean prioritizeCompany(){
        if(!initialized||!blocker.isEmpty()||companyMapReady()||position(Kind.COMPANY).isPresent()||generation.contains("DeferredPalace"))return false;
        var stage=PalaceGenerationRules.stage(generation);
        if(stage!=PalaceGenerationRules.Stage.SELECT_PALACE&&stage!=PalaceGenerationRules.Stage.SURVEY_REGION&&stage!=PalaceGenerationRules.Stage.SELECT_WELL)return false;
        var early=new CompoundTag();
        early.put("DeferredPalace",generation.copy());
        PalaceGenerationRules.enter(early,PalaceGenerationRules.Stage.SELECT_COMPANY);
        generation=early;
        setDirty();
        return true;
    }
    void deferPalaceEdit(){
        if(generation.contains("DeferredPalace")){
            generation.putBoolean("DeferredPalaceEdited",true);
            setDirty();
        }
    }
    void resumeAfterEarlyCompany(){
        if(!generation.contains("DeferredPalace")||!companyMapReady())return;
        var saved=generation.getCompound("DeferredPalace").copy();
        if(saved.isEmpty())PalaceGenerationRules.enter(saved,PalaceGenerationRules.Stage.SELECT_PALACE);
        for(String name:new String[]{"CompanyX","CompanyZ","CompanyY"})saved.putInt(name,generation.getInt(name));
        saved.putBoolean("CompanyClaimed",true);
        boolean edited=generation.getBoolean("DeferredPalaceEdited");
        generation=saved;
        if(edited)stopGeneration("The deferred Palace footprint was edited during Company construction. The verified Company remains available.");
        setDirty();
    }
    public boolean companyMapReady(){return incompatibleOriginal==null&&generated(Kind.COMPANY)&&position(Kind.COMPANY).isPresent();}
    boolean continueReservedCompany(){
        var stage=PalaceGenerationRules.stage(generation);
        if(stage!=PalaceGenerationRules.Stage.SELECT_WELL&&stage!=PalaceGenerationRules.Stage.SELECT_COMPANY)return false;
        if(stage==PalaceGenerationRules.Stage.SELECT_WELL&&position(Kind.WELL).isEmpty())return false;
        var company=position(Kind.COMPANY).orElse(null);
        if(company==null)return false;
        if(companyMapReady()&&generation.contains("DeferredPalace")){
            resumeAfterEarlyCompany();
            return true;
        }
        generation.putBoolean("CompanyClaimed",true);
        generation.putInt("CompanyX",company.getX());
        generation.putInt("CompanyY",company.getY());
        generation.putInt("CompanyZ",company.getZ());
        generation.putBoolean("CompanyFirst",true);
        PalaceGenerationRules.enter(generation,companyMapReady()?PalaceGenerationRules.Stage.TERRAIN:PalaceGenerationRules.Stage.COMPANY_PREPARE);
        setDirty();
        return true;
    }
    public boolean palaceArrivalReady(){
        return incompatibleOriginal==null&&generated(Kind.PALACE)&&position(Kind.PALACE).isPresent();
    }
    /** Only the real streamed builder calls this after its complete readback pass. */
    boolean generationVerified(Kind kind,BlockPos at,int verified,int expected){
        if(!blocker.isEmpty()||expected<=0||verified!=expected||position(kind).isEmpty()||!position(kind).get().equals(at))return false;
        sites.get(kind).putBoolean("Generated",true);setDirty();return true;
    }
    public static PalaceSiteData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(PalaceSiteData::load,PalaceSiteData::new,"coraline_palace_sites");}
    public void initialize(long worldSeed,BlockPos worldSpawn){if(initialized||!blocker.isEmpty())return;seed=worldSeed;spawn=new PalaceSitePlan.Point(worldSpawn.getX(),worldSpawn.getZ());initialized=true;setDirty();}
    public boolean initialized(){return initialized;}
    public String blocker(){return blocker;}
    public Optional<BlockPos> position(Kind kind){var site=sites.get(kind);return site!=null&&site.contains("Position",Tag.TAG_LONG)?Optional.of(BlockPos.of(site.getLong("Position"))):Optional.empty();}
    public boolean legacy(Kind kind){var site=sites.get(kind);return site!=null&&site.getBoolean("Legacy");}
    public boolean generated(Kind kind){var site=sites.get(kind);return site!=null&&site.getBoolean("Generated");}
    private Optional<PalaceSitePlan.Point> palace(){return position(Kind.PALACE).map(at->new PalaceSitePlan.Point(at.getX(),at.getZ()));}
    /** At most one proposal per call; caller budgets calls per tick. Pending
     * proposals survive reload and are not rerolled on player/chunk events.
     */
    public Optional<PalaceSitePlan.Point> proposal(Kind kind){
        if(!initialized||!blocker.isEmpty()||position(kind).isPresent()||kind==Kind.WELL&&palace().isEmpty()||kind==Kind.COMPANY&&palace().isEmpty()&&!generation.contains("DeferredPalace"))return Optional.empty();
        var site=sites.computeIfAbsent(kind,key->new CompoundTag());
        if(site.getBoolean("Pending"))return Optional.of(new PalaceSitePlan.Point(site.getInt("CandidateX"),site.getInt("CandidateZ")));
        int index=site.getInt("Candidate");if(index>=PalaceSitePlan.MAX_CANDIDATES){blocker="No safe "+kind+" candidate within the bounded search; no duplicate allocated.";setDirty();return Optional.empty();}
        site.putInt("Candidate",index+1);var point=PalaceSitePlan.candidate(seed,spawn,kind,index,palace().orElse(null));
        point.ifPresent(at->{site.putBoolean("Pending",true);site.putInt("CandidateX",at.x());site.putInt("CandidateZ",at.z());});setDirty();return point;
    }
    public boolean reject(Kind kind,PalaceSitePlan.Point pending){
        var site=sites.get(kind);if(!initialized||!blocker.isEmpty()||!matches(site,pending)||position(kind).isPresent())return false;
        site.remove("Pending");site.remove("CandidateX");site.remove("CandidateZ");setDirty();return true;
    }
    /** Evidence must come from the eventual real terrain/LOS survey, not a UI
     * request. A reservation is not a structure-generation completion receipt.
     */
    public record Survey(boolean untouched,boolean supported,boolean dry,double heightVariation,boolean palaceVisible){
        boolean safe(Kind kind){return untouched&&supported&&dry&&Double.isFinite(heightVariation)&&heightVariation>=0&&heightVariation<=10&&(kind!=Kind.WELL||palaceVisible);}
    }
    public boolean reserve(Kind kind,BlockPos at,Survey survey){
        if(!initialized||!blocker.isEmpty()||survey==null||!survey.safe(kind))return false;
        return reservePosition(kind,at);
    }
    /** A placement PLAN inside the builder's already virgin-checked claim, not
     * evidence that the future foundation/LOS currently exists. Generated stays
     * false until real block readback and final actual LOS validation finish.
     */
    boolean reserveGenerationPlan(Kind kind,BlockPos at){
        if(!initialized||!blocker.isEmpty())return false;
        if(!(kind==Kind.COMPANY&&generation.contains("DeferredPalace"))&&(!generation.getBoolean("PalaceClaimed")||!generation.getBoolean("ProfileSurveyed")))return false;
        var stage=PalaceGenerationRules.stage(generation);
        if(kind==Kind.PALACE&&(stage!=PalaceGenerationRules.Stage.SURVEY_REGION||generation.getInt("Cursor")!=2401||at.getY()!=generation.getInt("ProfileBase")))return false;
        if(kind==Kind.WELL&&(stage!=PalaceGenerationRules.Stage.SELECT_WELL||at.getY()!=generation.getInt("ProfileBase")+36))return false;
        if(kind==Kind.COMPANY&&(stage!=PalaceGenerationRules.Stage.SURVEY_COMPANY||generation.getInt("Cursor")!=49||!generation.getBoolean("CompanyClaimed")||at.getY()!=generation.getInt("CompanyY")))return false;
        return reservePosition(kind,at);
    }
    private boolean reservePosition(Kind kind,BlockPos at){
        var current=position(kind);if(current.isPresent())return current.get().equals(at);
        var point=new PalaceSitePlan.Point(at.getX(),at.getZ());var site=sites.get(kind);if(!matches(site,point))return false;
        if(kind==Kind.PALACE&&!PalaceSitePlan.validPalaceDistance(spawn,point))return false;
        if(kind==Kind.WELL&&(palace().isEmpty()||point.distance(palace().get())<220||point.distance(palace().get())>450))return false;
        site.putLong("Position",at.asLong());site.remove("Pending");site.remove("CandidateX");site.remove("CandidateZ");setDirty();return true;
    }
    public boolean ready(){
        if(!blocker.isEmpty())return false;for(var kind:Kind.values())if(!generated(kind)||position(kind).isEmpty())return false;
        return generation.isEmpty()||PalaceGenerationRules.stage(generation)==PalaceGenerationRules.Stage.DONE;
    }
    /** Preserve an already existing canonical landmark, even if its historical
     * distance is incompatible. Never allocate a second building to correct it.
     */
    public boolean adoptExisting(Kind kind,BlockPos at){
        if(!initialized||!blocker.isEmpty())return false;new PalaceSitePlan.Point(at.getX(),at.getZ());
        var current=position(kind);if(current.isPresent())return current.get().equals(at);
        var site=new CompoundTag();site.putLong("Position",at.asLong());site.putBoolean("Legacy",true);site.putBoolean("Generated",true);sites.put(kind,site);
        if(kind==Kind.PALACE&&!PalaceSitePlan.validPalaceDistance(spawn,new PalaceSitePlan.Point(at.getX(),at.getZ())))site.putString("Compatibility","Existing Palace lies outside 1000–10000 m from the original world spawn; preserved without a duplicate.");
        setDirty();return true;
    }
    public String compatibility(Kind kind){var site=sites.get(kind);return site==null?"":site.getString("Compatibility");}
    private static boolean matches(CompoundTag site,PalaceSitePlan.Point point){return point!=null&&site!=null&&site.getBoolean("Pending")&&site.getInt("CandidateX")==point.x()&&site.getInt("CandidateZ")==point.z();}
    public static PalaceSiteData load(CompoundTag root){
        var data=new PalaceSiteData();data.initialized=root.getBoolean("Initialized");data.seed=root.getLong("Seed");data.blocker=root.getString("Blocker");
        try{
            flag(root,"Initialized");
            if(root.contains("Blocker")&&!root.contains("Blocker",Tag.TAG_STRING))throw new IllegalArgumentException("Malformed registry blocker");
            if(root.contains("Version")&&(!root.contains("Version",Tag.TAG_INT)||root.getInt("Version")!=1))throw new IllegalArgumentException("Unknown world-site schema");
            if(root.contains("Generation")){
                if(!root.contains("Generation",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Malformed generation checkpoint");
                data.generation=root.getCompound("Generation").copy();PalaceGenerationRules.validate(data.generation);
            }
            if(data.initialized){if(root.getInt("Version")!=1||!root.contains("Seed",Tag.TAG_LONG)||!root.contains("SpawnX",Tag.TAG_INT)||!root.contains("SpawnZ",Tag.TAG_INT))throw new IllegalArgumentException("Incomplete world-site origin");data.spawn=new PalaceSitePlan.Point(root.getInt("SpawnX"),root.getInt("SpawnZ"));}
            if(root.contains("Sites")&&!root.contains("Sites",Tag.TAG_LIST))throw new IllegalArgumentException("Malformed landmark list");
            if(root.contains("Sites",Tag.TAG_LIST)&&((ListTag)root.get("Sites")).size()>0&&((ListTag)root.get("Sites")).getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Malformed landmark entries");
            for(var entry:root.getList("Sites",Tag.TAG_COMPOUND)){
                var site=((CompoundTag)entry).copy();var kind=Kind.valueOf(site.getString("Kind"));if(data.sites.putIfAbsent(kind,site)!=null)throw new IllegalArgumentException("Duplicate saved "+kind);
                for(String key:new String[]{"Legacy","Generated","Pending"})flag(site,key);
                if(site.contains("Position")&&!site.contains("Position",Tag.TAG_LONG)||site.getBoolean("Generated")&&!site.contains("Position",Tag.TAG_LONG))throw new IllegalArgumentException("Incomplete saved landmark");
                if(site.contains("Candidate")&&!site.contains("Candidate",Tag.TAG_INT))throw new IllegalArgumentException("Malformed candidate cursor");
                if(site.contains("Position",Tag.TAG_LONG)){var at=BlockPos.of(site.getLong("Position"));new PalaceSitePlan.Point(at.getX(),at.getZ());}
                if(site.getInt("Candidate")<0||site.getInt("Candidate")>PalaceSitePlan.MAX_CANDIDATES)throw new IllegalArgumentException("Invalid candidate cursor");
                if(site.getBoolean("Pending")){if(!site.contains("CandidateX",Tag.TAG_INT)||!site.contains("CandidateZ",Tag.TAG_INT))throw new IllegalArgumentException("Incomplete pending proposal");new PalaceSitePlan.Point(site.getInt("CandidateX"),site.getInt("CandidateZ"));}
            }
            if(!data.initialized&&!data.sites.isEmpty())throw new IllegalArgumentException("Saved landmarks without world origin");
            // Validate after all entries are read: list order is not an origin.
            // A damaged pending coordinate/cursor must not be accepted as a new
            // terrain survey at an unrelated (possibly player-built) location.
            for(var entry:data.sites.entrySet()){
                var kind=entry.getKey();var site=entry.getValue();
                boolean positioned=site.contains("Position",Tag.TAG_LONG),pending=site.getBoolean("Pending");
                if(positioned&&pending||site.getBoolean("Legacy")&&(!positioned||pending))throw new IllegalArgumentException("Conflicting landmark state");
                if(!pending&&(site.contains("CandidateX")||site.contains("CandidateZ")))throw new IllegalArgumentException("Orphan proposal coordinates");
                if(site.getBoolean("Legacy"))continue;
                if(!positioned&&!pending)continue;
                int cursor=site.getInt("Candidate");
                if(cursor<1||kind==Kind.WELL&&data.palace().isEmpty()
                        ||kind==Kind.COMPANY&&data.palace().isEmpty()&&!data.generation.contains("DeferredPalace")&&!site.getBoolean("Generated"))throw new IllegalArgumentException("Missing proposal origin/cursor");
                var expected=PalaceSitePlan.candidate(data.seed,data.spawn,kind,cursor-1,data.palace().orElse(null));
                var actual=positioned?data.position(kind).map(at->new PalaceSitePlan.Point(at.getX(),at.getZ())).orElseThrow():new PalaceSitePlan.Point(site.getInt("CandidateX"),site.getInt("CandidateZ"));
                if(expected.isEmpty()||!expected.get().equals(actual))throw new IllegalArgumentException("Saved landmark does not match its deterministic proposal");
            }
            if(!data.generation.isEmpty()){
                var work=data.generation;var stage=PalaceGenerationRules.stage(work);
                for(var kind:new Kind[]{Kind.PALACE,Kind.COMPANY}){
                    String prefix=kind==Kind.PALACE?"Palace":"Company",coordinate=kind==Kind.PALACE?"Candidate":"Company";
                    if(!work.getBoolean(prefix+"Claimed"))continue;
                    var site=data.sites.get(kind);var at=new PalaceSitePlan.Point(work.getInt(coordinate+"X"),work.getInt(coordinate+"Z"));
                    if(site==null||site.getBoolean("Legacy")&&!(kind==Kind.COMPANY&&data.companyMapReady()))throw new IllegalArgumentException("Generation claim has no original proposal");
                    if(data.position(kind).isPresent()){var saved=data.position(kind).get();if(saved.getX()!=at.x()||saved.getZ()!=at.z())throw new IllegalArgumentException("Claim moved away from reserved landmark");}
                    else if(!matches(site,at))throw new IllegalArgumentException("Claim moved away from pending proposal");
                }
                if(!work.contains("DeferredPalace")&&stage.ordinal()>=PalaceGenerationRules.Stage.TERRAIN.ordinal())for(var kind:Kind.values())if(data.position(kind).isEmpty())throw new IllegalArgumentException("Generation stage has no reserved landmark");
                if(stage==PalaceGenerationRules.Stage.DONE)for(var kind:Kind.values())if(!data.generated(kind))throw new IllegalArgumentException("Completed world job has an unverified landmark");
            }
        }catch(IllegalArgumentException bad){data.blocker="Saved Palace registry needs compatibility review: "+bad.getMessage()+". No reroll or generation permitted.";data.incompatibleOriginal=root.copy();}
        return data;
    }
    private static void flag(CompoundTag tag,String key){if(tag.contains(key)&&(!tag.contains(key,Tag.TAG_BYTE)||(tag.getByte(key)!=0&&tag.getByte(key)!=1)))throw new IllegalArgumentException("Malformed "+key+" flag");}
    @Override public CompoundTag save(CompoundTag root){if(incompatibleOriginal!=null){var preserved=incompatibleOriginal.copy();preserved.putString("Blocker",blocker);return preserved;}root.putInt("Version",1);root.putBoolean("Initialized",initialized);root.putLong("Seed",seed);root.putString("Blocker",blocker);if(!generation.isEmpty())root.put("Generation",generation.copy());if(spawn!=null){root.putInt("SpawnX",spawn.x());root.putInt("SpawnZ",spawn.z());}var list=new ListTag();for(var kind:Kind.values())if(sites.containsKey(kind)){var site=sites.get(kind).copy();site.putString("Kind",kind.name());list.add(site);}root.put("Sites",list);return root;}
}
