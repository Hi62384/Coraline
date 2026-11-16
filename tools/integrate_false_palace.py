"""Share the physical dial mechanism without borrowing Seam containment credit."""
from integrate_endless_carnival import replace
def main():
    b='src/main/java/com/coraline/postgame/'
    replace(b+'InfiniteStructurePlan.java','List.of("major_containment","heart_defense","treasure"));','List.of("major_containment","heart_defense","treasure")),\n        FALSE_PINK_PALACE(4,Set.of(InfiniteTerrain.Biome.FORGOTTEN_GARDENS,InfiniteTerrain.Biome.PALE_WASTELAND,InfiniteTerrain.Biome.PORCELAIN_PLAINS),List.of("false_memories","buttonroot_cage","story_investigation"));')
    replace(b+'InfiniteStructurePlan.java','ORPHAN_DAIS,ORPHAN_VAULT }','ORPHAN_DAIS,ORPHAN_VAULT,FALSE_PALACE_HALL }')
    replace(b+'InfiniteStructurePlan.java','            case ENTITY_HEART_CHAMBER->{','            case FALSE_PINK_PALACE->{\n                rooms.add(room(-11,0,-11,23,23,23,Role.FALSE_PALACE_HALL));\n                rooms.add(room(-2,0,-21,5,6,10,Role.APPROACH));\n            }\n            case ENTITY_HEART_CHAMBER->{')
    p=b+'SeamLayout.java'
    replace(p,'int x,int z){}','int x,int z,InfiniteStructurePlan.Family family){}')
    replace(p,'    static List<BlockPos> parts(InfiniteStructurePlan.Plan plan){if(plan.family()!=InfiniteStructurePlan.Family.DIMENSIONAL_SEAM)return List.of();','    static boolean family(InfiniteStructurePlan.Family family){return family==InfiniteStructurePlan.Family.DIMENSIONAL_SEAM||family==InfiniteStructurePlan.Family.FALSE_PINK_PALACE;}\n    static List<BlockPos> parts(InfiniteStructurePlan.Plan plan){if(plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE)return List.of(new BlockPos(plan.x()-4,plan.y()+9,plan.z()+6),new BlockPos(plan.x()-6,plan.y()+9,plan.z()-6),new BlockPos(plan.x()+6,plan.y()+9,plan.z()-6),new BlockPos(plan.x(),plan.y()+1,plan.z()+6));if(plan.family()!=InfiniteStructurePlan.Family.DIMENSIONAL_SEAM)return List.of();')
    replace(p,'n.putInt("Version",1);n.putUUID("Generation"','n.putInt("Version",1);n.putString("Family",plan.family().name());n.putUUID("Generation"')
    replace(p,'var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));','InfiniteStructurePlan.Family family;try{if(n.contains("Family")&&!n.contains("Family",Tag.TAG_STRING))return Optional.empty();family=n.contains("Family")?InfiniteStructurePlan.Family.valueOf(n.getString("Family")):InfiniteStructurePlan.Family.DIMENSIONAL_SEAM;}catch(IllegalArgumentException bad){return Optional.empty();}if(!family(family))return Optional.empty();\n        var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"),family);')
    replace(p,'k.seed,InfiniteStructurePlan.Family.DIMENSIONAL_SEAM,k.x,k.z','k.seed,k.family,k.x,k.z')
    p=b+'SeamAnchorEntity.java'
    replace(p,'plan.family()!=InfiniteStructurePlan.Family.DIMENSIONAL_SEAM','!SeamLayout.family(plan.family())')
    replace(p,'return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed();','return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed()&&InfiniteWorldData.get(server.getServer()).matchesGeneration(epoch.id());')
    replace(p,'        var breach=BreachWorldData.get(player.server).view();','        if(plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE){data().putBoolean("Closed",true);data().putUUID("Completion",UUID.randomUUID());data().putString("Outcome",BreachState.Resolution.ZERO.name());setChanged();sync();FalsePalaceLayout.release(level,plan);say(player,"false_palace_opened");return;}\n        var breach=BreachWorldData.get(player.server).view();')
    replace(p,'if(core.closed()){say(player,"seam_closed");return;}','if(core.closed()){say(player,plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE?"false_palace_opened":"seam_closed");return;}')
    replace(p,'if(level.getGameTime()%20==0&&entity.slot==0)entity.sync();','if(level.getGameTime()%20==0&&entity.slot==0){entity.sync();if(entity.authentic()&&entity.closed()&&entity.plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE)FalsePalaceLayout.release(level,entity.plan);}')
    p=b+'InfiniteSitePiece.java'
    replace(p,'        HeartChamberLayout.place(level,clip,plan,index);','        HeartChamberLayout.place(level,clip,plan,index);\n        FalsePalaceLayout.place(level,clip,plan,index);')
    replace(p,'        if(plan.family()==InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER)wall=','        if(plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE)wall=Blocks.PINK_TERRACOTTA.defaultBlockState();\n        if(plan.family()==InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER)wall=')
    replace(p,'if(role==InfiniteStructurePlan.Role.ORPHAN_VAULT||','if(role==InfiniteStructurePlan.Role.FALSE_PALACE_HALL||role==InfiniteStructurePlan.Role.ORPHAN_VAULT||')
    p=b+'InfiniteSiteGuards.java'
    replace(p,'            case ENTITY_HEART_CHAMBER->','            case FALSE_PINK_PALACE->{if(room.role()==InfiniteStructurePlan.Role.FALSE_PALACE_HALL){kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);kinds.add(PostgameHostiles.Kind.FALSE_VILLAGER);}}\n            case ENTITY_HEART_CHAMBER->')
    replace(p,'            result.add(new Assignment(id,kinds.get(ordinal),at));','            if(room.role()==InfiniteStructurePlan.Role.FALSE_PALACE_HALL)at=new BlockPos(plan.x()+(ordinal==0?-7:7),plan.y()+(ordinal==2?9:1),plan.z()+(ordinal==1?6:-6));\n            result.add(new Assignment(id,kinds.get(ordinal),at));')
    p=b+'SiteCacheReceipt.java'
    replace(p,'        if(family==InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE','        if(family==InfiniteStructurePlan.Family.FALSE_PINK_PALACE&&role==InfiniteStructurePlan.Role.FALSE_PALACE_HALL)return 4;\n        if(family==InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE')
    replace(p,'boolean mansion=room.role()==InfiniteStructurePlan.Role.MANSION_HALL;','boolean mansion=room.role()==InfiniteStructurePlan.Role.MANSION_HALL||room.role()==InfiniteStructurePlan.Role.FALSE_PALACE_HALL;')
    p=b+'SiteCacheBlockEntity.java'
    replace(p,'&&OrphanageLayout.cacheReady(level,receipt);','&&OrphanageLayout.cacheReady(level,receipt)&&FalsePalaceLayout.cacheReady(level,receipt);')
    replace(p,'String blockedFeedback(){return receipt.getString("Family")','String blockedFeedback(){return receipt.getString("Family").equals(InfiniteStructurePlan.Family.FALSE_PINK_PALACE.name())?"message.coraline.false_palace_sealed":receipt.getString("Family")')
if __name__=='__main__':main()
