package com.coraline.postgame;

import java.util.*;

/** Immutable, bounded modules for implemented structure families.
 * Other families are not advertised until their own layouts/gameplay exist.
 * Coordinates and variation depend on the independent generation, never a
 * mutable placement cursor or the order in which chunks are requested. */
public final class InfiniteStructurePlan {
    public static final int VERSION=1,MAX_PIECES=32;
    public enum Family {
        DOLL_WORKSHOP(1,Set.of(InfiniteTerrain.Biome.STITCHED_MEADOWS,InfiniteTerrain.Biome.PAPERWOOD_FOREST,InfiniteTerrain.Biome.FORGOTTEN_GARDENS),List.of("investigation","production","thread_bindings")),
        CLOCKWORK_FACTORY(2,Set.of(InfiniteTerrain.Biome.CLOCKWORK_HIGHLANDS,InfiniteTerrain.Biome.OTHERWORLD_BADLANDS),List.of("machinery","sabotage","worker_rescue")),
        SOUL_GLASS_MINE(2,Set.of(InfiniteTerrain.Biome.THREADBOUND_MOUNTAINS,InfiniteTerrain.Biome.PORCELAIN_PLAINS,InfiniteTerrain.Biome.WHITE_RIFT_WASTES),List.of("mining","excavation","rescue")),
        RIFT_DRILL_CENTER(3,Set.of(InfiniteTerrain.Biome.WHITE_RIFT_WASTES,InfiniteTerrain.Biome.PALE_WASTELAND,InfiniteTerrain.Biome.OTHERWORLD_BADLANDS),List.of("infiltration","drill_sabotage","breach_containment")),
        // Append families: old site UUID derivation includes the ordinal.
        RIFT_SOLDIER_OUTPOST(2,Set.of(InfiniteTerrain.Biome.CLOCKWORK_HIGHLANDS,InfiniteTerrain.Biome.PALE_WASTELAND,InfiniteTerrain.Biome.OTHERWORLD_BADLANDS),List.of("combat","armory","exploration")),
        MIRROR_TOWER(3,Set.of(InfiniteTerrain.Biome.PORCELAIN_PLAINS,InfiniteTerrain.Biome.STITCHED_COAST,InfiniteTerrain.Biome.THREADBOUND_MOUNTAINS),List.of("navigation","illusion","exploration")),
        SUNKEN_OTHERWORLD_RUINS(3,Set.of(InfiniteTerrain.Biome.STITCHED_COAST,InfiniteTerrain.Biome.LANTERNWOOD,InfiniteTerrain.Biome.PORCELAIN_PLAINS),List.of("diving","exploration","treasure")),
        PORCELAIN_MANSION(3,Set.of(InfiniteTerrain.Biome.PORCELAIN_PLAINS,InfiniteTerrain.Biome.FORGOTTEN_GARDENS,InfiniteTerrain.Biome.STITCHED_MEADOWS),List.of("investigation","illusion","treasure")),
        DIMENSIONAL_SEAM(3,Set.of(InfiniteTerrain.Biome.WHITE_RIFT_WASTES,InfiniteTerrain.Biome.PALE_WASTELAND,InfiniteTerrain.Biome.THREADBOUND_MOUNTAINS),List.of("alignment","breach_containment","research")),
        OTHER_VILLAGE(1,Set.of(InfiniteTerrain.Biome.STITCHED_MEADOWS,InfiniteTerrain.Biome.BUTTONWOOD_FOREST,InfiniteTerrain.Biome.FORGOTTEN_GARDENS),List.of("trading","inhabitants","exploration")),
        ABANDONED_VILLAGE(2,Set.of(InfiniteTerrain.Biome.PAPERWOOD_FOREST,InfiniteTerrain.Biome.ROOTBOUND_WILDS,InfiniteTerrain.Biome.LANTERNWOOD),List.of("mystery","exploration","buried_archive")),
        ROOTBOUND_TEMPLE(3,Set.of(InfiniteTerrain.Biome.ROOTBOUND_WILDS,InfiniteTerrain.Biome.LANTERNWOOD,InfiniteTerrain.Biome.BUTTONWOOD_FOREST),List.of("exploration","rootbound_threat","buried_sanctum")),
        ANCIENT_BOUNDARY_SHRINE(2,Set.of(InfiniteTerrain.Biome.PALE_WASTELAND,InfiniteTerrain.Biome.PORCELAIN_PLAINS,InfiniteTerrain.Biome.FORGOTTEN_GARDENS),List.of("sanctuary","defense","boundary_research")),
        RESISTANCE_SANCTUARY(2,Set.of(InfiniteTerrain.Biome.FORGOTTEN_GARDENS,InfiniteTerrain.Biome.LANTERNWOOD,InfiniteTerrain.Biome.ROOTBOUND_WILDS),List.of("independent_inhabitants","defense","allies","trading")),
        FORGOTTEN_OBSERVATORY(2,Set.of(InfiniteTerrain.Biome.CLOCKWORK_HIGHLANDS,InfiniteTerrain.Biome.THREADBOUND_MOUNTAINS,InfiniteTerrain.Biome.PORCELAIN_PLAINS),List.of("observation","boundary_research","exploration")),
        GRAND_THEATRE(2,Set.of(InfiniteTerrain.Biome.PORCELAIN_PLAINS,InfiniteTerrain.Biome.STITCHED_MEADOWS,InfiniteTerrain.Biome.CANDY_WONDERLAND),List.of("performance","repeating_script","troupe")),
        ENDLESS_CARNIVAL(2,Set.of(InfiniteTerrain.Biome.CANDY_WONDERLAND,InfiniteTerrain.Biome.STITCHED_MEADOWS,InfiniteTerrain.Biome.PAPERWOOD_FOREST),List.of("moving_targets","repeating_routine","troupe","prizes")),
        ABANDONED_ORPHANAGE(3,Set.of(InfiniteTerrain.Biome.PAPERWOOD_FOREST,InfiniteTerrain.Biome.ROOTBOUND_WILDS,InfiniteTerrain.Biome.PALE_WASTELAND),List.of("investigation","rescue","trapped_inhabitants")),
        ENTITY_HEART_CHAMBER(4,Set.of(InfiniteTerrain.Biome.WHITE_RIFT_WASTES,InfiniteTerrain.Biome.THREADBOUND_MOUNTAINS,InfiniteTerrain.Biome.PALE_WASTELAND),List.of("major_containment","heart_defense","treasure")),
        FALSE_PINK_PALACE(4,Set.of(InfiniteTerrain.Biome.FORGOTTEN_GARDENS,InfiniteTerrain.Biome.PALE_WASTELAND,InfiniteTerrain.Biome.PORCELAIN_PLAINS),List.of("false_memories","buttonroot_cage","story_investigation"));
        public final int difficulty;public final Set<InfiniteTerrain.Biome> biomes;public final List<String> hooks;
        Family(int difficulty,Set<InfiniteTerrain.Biome> biomes,List<String> hooks){this.difficulty=difficulty;this.biomes=Set.copyOf(biomes);this.hooks=List.copyOf(hooks);}
        public String id(){return name().toLowerCase(Locale.ROOT);}
    }
    public enum Role { HALL, WORKROOM, STORAGE, CONTROL, CORRIDOR, CHIMNEY, STAIRS, ORE_VAULT,DRILL_CHAMBER,TOWER,APPROACH,MIRROR_SHAFT,RUIN_CHAMBER,RUIN_WING,MANSION_HALL,SEAM_CHAMBER,VILLAGE_SQUARE,RESIDENCE,RUINED_RESIDENCE,ROOT_SANCTUM,DEFENSE_HALL,DEFENSE_VAULT,OBSERVATORY_HALL,RESEARCH_VAULT,THEATRE_HALL,PERFORMER_DAIS,PERFORMANCE_VAULT,CARNIVAL_GALLERY,CARNIVAL_SUPPLIES,CARNIVAL_VAULT,ORPHAN_OFFICE,ORPHAN_RECORD,ORPHAN_DAIS,ORPHAN_VAULT,FALSE_PALACE_HALL }
    /** Inclusive local module bounds. Floor is y, with eight blocks of footing
     * below it; stairs have their own sloping floor inside the stated bounds. */
    public record Module(int x,int y,int z,int width,int height,int depth,Role role){
        public Module{
            if(width<3||width>23||depth<3||depth>23||height<4||height>32||Math.abs(x)>48||Math.abs(z)>48||y< -24||y>16)throw new IllegalArgumentException("Unbounded structure module");
            Objects.requireNonNull(role);
        }
        public int maxX(){return x+width-1;}public int maxZ(){return z+depth-1;}
    }
    public record Plan(UUID site,UUID generation,long seed,Family family,InfiniteTerrain.Biome biome,int x,int y,int z,int variation,List<Module> modules){
        public Plan{
            Objects.requireNonNull(site);Objects.requireNonNull(generation);Objects.requireNonNull(family);Objects.requireNonNull(biome);
            modules=List.copyOf(modules);if(modules.isEmpty()||modules.size()>MAX_PIECES||variation<0||variation>3)throw new IllegalArgumentException("Invalid immutable structure plan");
        }
    }
    private static long mix(long v){v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;return v^(v>>>31);}
    public static Optional<Plan> at(UUID generation,long seed,Family family,int x,int z){
        Objects.requireNonNull(generation);Objects.requireNonNull(family);
        if(Math.abs((long)x)>29_999_920||Math.abs((long)z)>29_999_920)return Optional.empty();
        var terrain=new InfiniteTerrain(seed);var center=terrain.column(x,z);
        boolean marine=family==Family.SUNKEN_OTHERWORLD_RUINS;
        if(!family.biomes.contains(center.biome())||(marine?center.surfaceY()<InfiniteTerrain.SEA_LEVEL-40||center.surfaceY()>InfiniteTerrain.SEA_LEVEL-12:center.surfaceY()<InfiniteTerrain.SEA_LEVEL+4))return Optional.empty();
        var modules=layout(family,(int)(mix(seed^(long)x*0x632be59bd9b4e019L^(long)z*0x94d049bb133111ebL)&3));
        int low=center.surfaceY(),high=low;
        // Inspect a bounded footprint and the front approach. No live chunk is
        // loaded, no terrain is flattened before this eligibility decision.
        int minX=-5,maxX=5,minZ=-8,maxZ=5;
        for(var room:modules){minX=Math.min(minX,room.x);maxX=Math.max(maxX,room.maxX());minZ=Math.min(minZ,room.z);maxZ=Math.max(maxZ,room.maxZ());}
        for(int dx=minX;dx<=maxX;dx++)for(int dz=minZ;dz<=maxZ;dz++){
            int y=terrain.column(x+dx,z+dz).surfaceY();low=Math.min(low,y);high=Math.max(high,y);
            if((marine?low<InfiniteTerrain.SEA_LEVEL-40||high>InfiniteTerrain.SEA_LEVEL-12:low<InfiniteTerrain.SEA_LEVEL+2)||high-low>(family==Family.RIFT_DRILL_CENTER?10:6))return Optional.empty();
        }
        int variation=(int)(mix(seed^(long)x*0x632be59bd9b4e019L^(long)z*0x94d049bb133111ebL)&3);
        long a=mix(seed^generation.getMostSignificantBits()^(long)x*0x9e3779b97f4a7c15L^family.ordinal());
        long b=mix(seed^generation.getLeastSignificantBits()^(long)z*0x632be59bd9b4e019L^family.ordinal());
        return Optional.of(new Plan(new UUID(a,b),generation,seed,family,center.biome(),x,high+1,z,variation,modules));
    }
    private static Module room(int x,int y,int z,int w,int h,int d,Role role){return new Module(x,y,z,w,h,d,role);}
    static List<Module> layout(Family family,int variation){
        var rooms=new ArrayList<Module>();
        switch(family){
            case DOLL_WORKSHOP->{
                rooms.add(room(-5,0,-5,11,7,11,Role.HALL));
                rooms.add(room(-15,0,-4,7,6,9,variation%2==0?Role.WORKROOM:Role.STORAGE));
                rooms.add(room(9,0,-4,7,6,9,variation%2==0?Role.STORAGE:Role.WORKROOM));
                rooms.add(room(-3,0,9,7,6,7,Role.CONTROL));
                rooms.add(room(-8,0,-1,3,4,3,Role.CORRIDOR));rooms.add(room(6,0,-1,3,4,3,Role.CORRIDOR));
                rooms.add(room(-1,0,6,3,4,3,Role.CORRIDOR));
            }
            case CLOCKWORK_FACTORY->{
                rooms.add(room(-7,0,-9,15,10,19,Role.HALL));
                rooms.add(room(-19,0,-5,9,7,11,Role.WORKROOM));rooms.add(room(11,0,-5,9,7,11,Role.STORAGE));
                rooms.add(room(-4,0,13,9,7,9,Role.CONTROL));
                rooms.add(room(-10,0,-1,3,4,3,Role.CORRIDOR));rooms.add(room(8,0,-1,3,4,3,Role.CORRIDOR));rooms.add(room(-1,0,10,3,4,3,Role.CORRIDOR));
                // Smokestacks are separate bounded pieces, not a renamed room.
                rooms.add(room(-6,10,-7,3,8+variation,3,Role.CHIMNEY));rooms.add(room(4,10,5,3,8+variation,3,Role.CHIMNEY));
            }
            case SOUL_GLASS_MINE->{
                rooms.add(room(-3,0,-5,7,6,9,Role.HALL));
                rooms.add(room(-1,-18,4,3,24,21,Role.STAIRS));
                rooms.add(room(-4,-18,25,9,6,9,Role.WORKROOM));
                rooms.add(room(-6,-18,37,13,8,11,Role.ORE_VAULT));
                rooms.add(room(-1,-18,34,3,4,3,Role.CORRIDOR));
            }
            case RIFT_DRILL_CENTER->{
                rooms.add(room(-10,0,-11,21,10,23,Role.HALL));
                rooms.add(room(-6,0,15,13,10,13,Role.DRILL_CHAMBER));
                rooms.add(room(-22,0,-4,9,7,9,Role.CONTROL));rooms.add(room(14,0,-4,9,7,9,Role.CONTROL));
                rooms.add(room(-4,0,-23,9,7,9,Role.STORAGE));
                rooms.add(room(-13,0,-1,3,4,3,Role.CORRIDOR));rooms.add(room(11,0,-1,3,4,3,Role.CORRIDOR));
                rooms.add(room(-1,0,12,3,4,3,Role.CORRIDOR));rooms.add(room(-1,0,-14,3,4,3,Role.CORRIDOR));
                for(int x:new int[]{-24,20})for(int z:new int[]{-24,18})rooms.add(room(x,0,z,5,14+variation,5,Role.TOWER));
                rooms.add(room(-2,0,-39,5,6,16,Role.APPROACH));
            }
            case RIFT_SOLDIER_OUTPOST->{
                rooms.add(room(-4,0,-5,9,7,11,Role.HALL));
                rooms.add(room(-16,0,-5,9,7,11,Role.WORKROOM));
                rooms.add(room(8,0,-5,9,7,11,Role.STORAGE));
                rooms.add(room(-4,0,9,9,7,9,Role.CONTROL));
                rooms.add(room(-7,0,-1,3,4,3,Role.CORRIDOR));
                rooms.add(room(5,0,-1,3,4,3,Role.CORRIDOR));
                rooms.add(room(-1,0,6,3,4,3,Role.CORRIDOR));
                for(int x:new int[]{-22,18})for(int z:new int[]{-14,17})rooms.add(room(x,0,z,5,14+variation,5,Role.TOWER));
                rooms.add(room(-2,0,-22,5,6,17,Role.APPROACH));
            }
            case MIRROR_TOWER->{
                rooms.add(room(-4,0,-13,9,6,7,Role.HALL));
                rooms.add(room(-1,0,-6,3,4,3,Role.CORRIDOR));
                rooms.add(room(-5,0,-3,11,28,11,Role.MIRROR_SHAFT));
                rooms.add(room(-2,0,-23,5,6,10,Role.APPROACH));
            }
            case SUNKEN_OTHERWORLD_RUINS->{
                rooms.add(room(-6,0,-7,13,9,15,Role.RUIN_CHAMBER));
                rooms.add(room(-18,0,-4,9,7,9,Role.RUIN_WING));
                rooms.add(room(10,0,-4,9,7,9,Role.RUIN_WING));
                rooms.add(room(-9,0,-1,3,4,3,Role.CORRIDOR));
                rooms.add(room(7,0,-1,3,4,3,Role.CORRIDOR));
            }
            case PORCELAIN_MANSION->{
                rooms.add(room(-4,0,-20,9,6,7,Role.HALL));
                rooms.add(room(-1,0,-13,3,4,3,Role.CORRIDOR));
                rooms.add(room(-11,0,-10,23,18,23,Role.MANSION_HALL));
                rooms.add(room(-2,0,-30,5,6,10,Role.APPROACH));
            }
            case DIMENSIONAL_SEAM->{
                rooms.add(room(-6,0,-6,13,9,13,Role.SEAM_CHAMBER));
                rooms.add(room(-3,0,-16,7,6,7,Role.HALL));
                rooms.add(room(-1,0,-9,3,4,3,Role.CORRIDOR));
                rooms.add(room(-3,0,10,7,6,7,Role.STORAGE));
                rooms.add(room(-1,0,7,3,4,3,Role.CORRIDOR));
            }
            case OTHER_VILLAGE->{
                rooms.add(room(-4,0,-4,9,5,9,Role.VILLAGE_SQUARE));
                for(int x:new int[]{-16,8})for(int z:new int[]{-14,-1,12})rooms.add(room(x,0,z,9,7,9,Role.RESIDENCE));
                rooms.add(room(-7,0,-14,3,4,23,Role.VILLAGE_SQUARE));
                rooms.add(room(5,0,-14,3,4,23,Role.VILLAGE_SQUARE));
                rooms.add(room(-7,0,9,3,4,12,Role.VILLAGE_SQUARE));
                rooms.add(room(5,0,9,3,4,12,Role.VILLAGE_SQUARE));
                rooms.add(room(-7,0,-24,5,6,10,Role.APPROACH));
            }
            case ABANDONED_VILLAGE->{
                rooms.add(room(-4,0,-4,9,5,9,Role.VILLAGE_SQUARE));
                rooms.add(room(-16,0,-12,9,6,9,Role.RUINED_RESIDENCE));
                rooms.add(room(8,0,-4,9,6,9,Role.RUINED_RESIDENCE));
                rooms.add(room(-16,0,8,9,6,9,Role.RUINED_RESIDENCE));
                rooms.add(room(-7,0,-12,3,4,23,Role.VILLAGE_SQUARE));
                rooms.add(room(-7,0,11,3,4,6,Role.VILLAGE_SQUARE));
                rooms.add(room(5,0,-4,3,4,9,Role.VILLAGE_SQUARE));
                rooms.add(room(-1,-18,5,3,24,21,Role.STAIRS));
                rooms.add(room(-4,-18,26,9,7,9,Role.CONTROL));
                rooms.add(room(-7,0,-22,5,6,10,Role.APPROACH));
            }
            case ROOTBOUND_TEMPLE->{
                rooms.add(room(-4,0,-13,9,7,9,Role.HALL));
                rooms.add(room(-1,-18,-4,3,24,21,Role.STAIRS));
                rooms.add(room(-6,-18,17,13,9,15,Role.ROOT_SANCTUM));
                rooms.add(room(-18,-18,20,9,7,9,Role.STORAGE));
                rooms.add(room(-9,-18,23,3,4,3,Role.CORRIDOR));
                rooms.add(room(-2,0,-23,5,6,10,Role.APPROACH));
            }
            case ANCIENT_BOUNDARY_SHRINE->{
                rooms.add(room(-6,0,-6,13,8,13,Role.DEFENSE_HALL));
                rooms.add(room(-4,0,10,9,7,9,Role.DEFENSE_VAULT));
                rooms.add(room(-1,0,7,3,4,3,Role.CORRIDOR));
                rooms.add(room(-2,0,-16,5,6,10,Role.APPROACH));
            }
            case RESISTANCE_SANCTUARY->{
                rooms.add(room(-6,0,-6,13,8,13,Role.DEFENSE_HALL));
                rooms.add(room(-18,0,-4,9,7,9,Role.RESIDENCE));
                rooms.add(room(10,0,-4,9,7,9,Role.RESIDENCE));
                rooms.add(room(-18,0,12,9,7,9,Role.RESIDENCE));
                rooms.add(room(-4,0,10,9,7,9,Role.DEFENSE_VAULT));
                rooms.add(room(-9,0,-1,3,4,3,Role.CORRIDOR));rooms.add(room(7,0,-1,3,4,3,Role.CORRIDOR));rooms.add(room(-1,0,7,3,4,3,Role.CORRIDOR));
                rooms.add(room(-9,0,5,3,4,16,Role.VILLAGE_SQUARE));rooms.add(room(-7,0,13,3,4,3,Role.VILLAGE_SQUARE));
                rooms.add(room(-2,0,-16,5,6,10,Role.APPROACH));
            }
            case FORGOTTEN_OBSERVATORY->{
                rooms.add(room(-6,0,-6,13,16,13,Role.OBSERVATORY_HALL));
                rooms.add(room(-3,0,-16,7,6,7,Role.HALL));
                rooms.add(room(-1,0,-9,3,4,3,Role.CORRIDOR));
                rooms.add(room(-18,0,-4,9,7,9,Role.RESEARCH_VAULT));
                rooms.add(room(-9,0,-1,3,4,3,Role.CORRIDOR));
                rooms.add(room(-2,0,-24,5,6,8,Role.APPROACH));
            }
            case GRAND_THEATRE->{
                rooms.add(room(-10,0,-12,21,13,23,Role.THEATRE_HALL));
                rooms.add(room(-4,0,-22,9,7,7,Role.HALL));
                rooms.add(room(-1,0,-15,3,4,3,Role.CORRIDOR));
                rooms.add(room(-8,1,7,3,4,3,Role.PERFORMER_DAIS));rooms.add(room(-1,1,7,3,4,3,Role.PERFORMER_DAIS));rooms.add(room(6,1,7,3,4,3,Role.PERFORMER_DAIS));
                rooms.add(room(14,0,0,9,7,9,Role.PERFORMANCE_VAULT));rooms.add(room(11,0,3,3,4,3,Role.CORRIDOR));
                rooms.add(room(-2,0,-30,5,6,8,Role.APPROACH));
            }
            case FALSE_PINK_PALACE->{
                rooms.add(room(-11,0,-11,23,23,23,Role.FALSE_PALACE_HALL));
                rooms.add(room(-2,0,-21,5,6,10,Role.APPROACH));
            }
            case ENTITY_HEART_CHAMBER->{
                rooms.add(room(-11,0,-11,23,17,23,Role.DEFENSE_HALL));
                rooms.add(room(-4,0,-22,9,7,8,Role.HALL));rooms.add(room(-1,0,-14,3,4,3,Role.CORRIDOR));
                rooms.add(room(-4,0,15,9,8,9,Role.DEFENSE_VAULT));rooms.add(room(-1,0,12,3,4,3,Role.CORRIDOR));
                rooms.add(room(-2,0,-30,5,6,8,Role.APPROACH));
            }
            case ABANDONED_ORPHANAGE->{
                rooms.add(room(-4,0,-4,9,7,9,Role.ORPHAN_OFFICE));
                rooms.add(room(-12,0,4,7,7,9,Role.ORPHAN_RECORD));rooms.add(room(6,0,4,7,7,9,Role.ORPHAN_RECORD));rooms.add(room(-3,0,14,7,7,9,Role.ORPHAN_RECORD));
                rooms.add(room(-5,0,7,11,4,3,Role.CORRIDOR));rooms.add(room(-1,0,5,3,4,9,Role.CORRIDOR));
                rooms.add(room(-12,0,16,5,6,7,Role.ORPHAN_DAIS));rooms.add(room(8,0,16,5,6,7,Role.ORPHAN_DAIS));
                rooms.add(room(-7,0,18,4,4,3,Role.CORRIDOR));rooms.add(room(4,0,18,4,4,3,Role.CORRIDOR));
                rooms.add(room(-3,0,26,7,7,7,Role.ORPHAN_VAULT));rooms.add(room(-1,0,23,3,4,3,Role.CORRIDOR));
                rooms.add(room(-2,0,-14,5,6,10,Role.APPROACH));
            }
            case ENDLESS_CARNIVAL->{
                rooms.add(room(-9,0,-6,19,9,19,Role.CARNIVAL_GALLERY));
                rooms.add(room(-21,0,-3,9,6,9,Role.CARNIVAL_SUPPLIES));rooms.add(room(-12,0,1,3,4,3,Role.CORRIDOR));
                rooms.add(room(-4,0,16,9,7,9,Role.CARNIVAL_VAULT));rooms.add(room(-1,0,13,3,4,3,Role.CORRIDOR));
                rooms.add(room(-8,0,-5,3,4,3,Role.PERFORMER_DAIS));rooms.add(room(6,0,-5,3,4,3,Role.PERFORMER_DAIS));
                rooms.add(room(-2,0,-16,5,6,10,Role.APPROACH));
            }
        }
        return List.copyOf(rooms);
    }
    private InfiniteStructurePlan(){}
}
