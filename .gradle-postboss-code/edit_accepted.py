from pathlib import Path
p=Path('src/main/java/com/coraline/feast/GhostEscapeHunt.java')
s=p.read_text()
a=s.index('            // The crossing journal')
b=s.index('            var far=TunnelPassage',a)
s=s[:a]+'''            var other=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
            if(other==null||child.level()!=other&&child.level()!=outside)return true;
            boolean crossed=child.level()==outside;
            if(crossed){
                var owner=child.getPersistentData();
                if(!owner.hasUUID("ShowTravelOwner")||!s.owner.equals(owner.getUUID("ShowTravelOwner"))
                        ||!owner.contains("GhostEscapeChild"))return true;
            }
            var sourceOpen=openPassage(outside,realm.sourceDoor,s.door);
            var realmOpen=crossed?null:openPassage(other,realm.doorPos,s.door);
            if(sourceOpen==null||!crossed&&realmOpen==null){
                e.putString("Blocker","The original Little Door passage is being restored.");
                return true;
            }
'''+s[b:]
s=s.replace('''            if(!GhostVictimNavigation.clear(outside,child,farFeet)||!GhostVictimNavigation.clear(outside,child,farFeet.relative(sourceOpen.getValue(TinyDoorBlock.FACING),2))){''','''            if(!crossed&&(!GhostVictimNavigation.clear(outside,child,farFeet)||!GhostVictimNavigation.clear(outside,child,farFeet.relative(sourceOpen.getValue(TinyDoorBlock.FACING),2)))){''')
s=s.replace('''            if(child.level()==outside){
                var tag=child.getPersistentData();
                if(!tag.hasUUID("ShowTravelOwner")||!s.owner.equals(tag.getUUID("ShowTravelOwner"))||!tag.contains("GhostEscapeChild"))return true;''','''            if(crossed){''')
s=s.replace('''            if(child.level()!=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY))return true;
            var state=child.level().getBlockState(realm.doorPos);if(!state.is(ModBlocks.TINY_DOOR.get()))return true;var facing=state.getValue(TinyDoorBlock.FACING);''','''            var facing=realmOpen.getValue(TinyDoorBlock.FACING);''')
s=s.replace('''            var crossed=ShowTravel.guest(child,p,s,outside,far,true,"GhostEscapeChild");if(crossed instanceof com.coraline.tycoon.TycoonNpc same){''','''            var moved=ShowTravel.guest(child,p,s,outside,far,true,"GhostEscapeChild");if(moved instanceof com.coraline.tycoon.TycoonNpc same
                    &&same.level()==outside&&child.getUUID().equals(same.getUUID())){''')
s=s.replace('''        var original=outside.getBlockState''','''        LoopProps.hold(outside,realm.sourceDoor);
            var original=outside.getBlockState''')
i=s.index('    private GhostEscapeHunt()')
s=s[:i]+'''    private static net.minecraft.world.level.block.state.BlockState openPassage(net.minecraft.server.level.ServerLevel level,BlockPos at,java.util.UUID expected){
        LoopProps.hold(level,at);
        var state=level.getBlockState(at);
        if(!state.is(ModBlocks.TINY_DOOR.get())
                ||!(level.getBlockEntity(at) instanceof com.coraline.otherworld.DoorBlockEntity door)
                ||!expected.equals(door.doorId)||!TunnelPassage.ensure(level,door))return null;
        var open=state.setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true);
        if(!open.equals(state))level.setBlock(at,open,3);
        return level.getBlockState(at).equals(open)?open:null;
    }
'''+s[i:]
p.write_text(s)
p=Path('src/main/java/com/coraline/feast/GhostVictimRoutines.java')
s=p.read_text().replace('''        var mirror=n.getCompound("Mirrors").getCompound(ids.get(Math.floorMod(index,ids.size())));''','''        return mirrorStop(n,child,ids.get(Math.floorMod(index,ids.size())));
    }
    static BlockPos mirrorStop(CompoundTag n,TycoonNpc child,String id){
        if(!GhostVictimMirrors.available(n,id,child.level().dimension().location().toString()))return null;
        var mirror=n.getCompound("Mirrors").getCompound(id);''')
p.write_text(s)
p=Path('src/main/java/com/coraline/feast/GhostEscapeCovert.java')
s=p.read_text().replace('''                if(GhostVictimRoutines.travel(p,s,ghost,child,GhostVictimWorld.bedroom(s),false)==GhostVictimNavigation.Result.ARRIVED){
                    hands(child);GhostVictimRoutines.lookAtMirror(ghost,child);''','''                var destination=GhostVictimRoutines.mirrorStop(ghost,child,"bedroom");
                if(destination==null){
                    e.putString("Blocker","Clear the floor and sightline in front of the bedroom mirror.");
                    return true;
                }
                if(GhostVictimRoutines.travel(p,s,ghost,child,destination,false)==GhostVictimNavigation.Result.ARRIVED
                        &&GhostVictimMirrors.childPresent(p,ghost,"bedroom",child)){
                    e.remove("Blocker");
                    hands(child);GhostVictimRoutines.lookAtMirror(ghost,child);''')
p.write_text(s)
