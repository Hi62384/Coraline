from pathlib import Path
b=Path('src/main/java/com/coraline/feast')
p=b/'GhostEscapeRooms.java';s=p.read_text()
a=s.index('            if(!row.contains("Container"))')
z=s.index('        return List.copyOf(valid);',a)
s=s[:a]+'''            if(!row.contains("Container")){
                for(var cabinet:HouseCabinets.CABINETS){
                    if(!room.id().equals(cabinet.room()))continue;
                    var at=GhostVictimWorld.at(s,cabinet.x(),cabinet.y(),cabinet.z());
                    level.getChunkAt(at);
                    if(!(level.getBlockState(at).getBlock() instanceof com.coraline.secrets.HouseStorageBlock))continue;
                    if(GhostVictimNavigation.approach(child,at)==null
                            &&java.util.stream.Stream.of(net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,
                                    net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST)
                                    .noneMatch(direction->GhostVictimNavigation.clear(level,child,at.relative(direction))))continue;
                    row.putBoolean("AuthoredCabinet",true);
                    row.putString("Label",cabinet.name());
                    row.putLong("Container",at.asLong());
                    row.putLong("Door",door.asLong());
                    all.put(room.id(),row);
                    OtherMotherLoop.changed(p);
                    break;
                }
            }
            if(validContainer(p,s,ghost,room.id()))valid.add(room.id());
        }
'''+s[z:]
a=s.index('        if(!(level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity')
s=s[:a]+'''        var row=sites(ghost).getCompound(id);
        if(row.getBoolean("AuthoredCabinet")){
            if(!(level.getBlockState(at).getBlock() instanceof com.coraline.secrets.HouseStorageBlock))return false;
            for(var cabinet:HouseCabinets.CABINETS)
                if(id.equals(cabinet.room())&&at.equals(GhostVictimWorld.at(s,cabinet.x(),cabinet.y(),cabinet.z())))return true;
            return false;
        }
'''+s[a:]
s=s.replace('''/** Physical room adapters over the existing Other House. Every candidate room
 * has an identical locked, empty presentation chest; the eye identity stays in
 * the server quest journal until the authentic Seeing Stone reveals it. */''','''/** Owner-bound investigations use authored Rosewood storage. Legacy prepared
 * chests remain valid for their original attempts. */''')
p.write_text(s)
p=b/'GhostEscapeCovert.java';s=p.read_text()
a=s.index('                int turn=',s.index('case SCAN->'));z=s.index('                return true;',a)
s=s[:a]+'''                String assigned=e.getString("AssignedRoom");
                if(!GhostEscapeRooms.validContainer(p,s,ghost,assigned))return true;
                var at=GhostVictimNavigation.approach(child,center);
                if(at==null||GhostVictimRoutines.travel(p,s,ghost,child,at,false)!=GhostVictimNavigation.Result.ARRIVED)return true;
                var focus=center.getCenter();
                child.getLookControl().setLookAt(focus.x,focus.y,focus.z,25,25);
                var delta=focus.subtract(child.getEyePosition());
                if(delta.lengthSqr()>16||!GhostVictimNavigation.canSeePoint(child,focus,4,130))return true;
                if(GhostEscapeProgress.scanned(ghost,com.coraline.secrets.HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,child.blockPosition()))){
                    hands(child);ghost.remove("ChildRoutine");
                    GhostVictimDirector.words(ghost,e.getBoolean("EyeRoomKnown")?"The Stone showed the ghost eyes in this cabinet. I need the key.":"The Stone doesn't show any eyes in this cabinet.");
                }
'''+s[z:]
s=s.replace('''if(delta.lengthSqr()>16||!GhostVictimNavigation.canSeePoint(child,focus,4,130))return true;''','''var end=focus.subtract(delta.normalize().scale(.45));
                if(delta.lengthSqr()>16||GhostVictimNavigation.view(child).dot(delta.normalize())<.6
                        ||level.clip(new net.minecraft.world.level.ClipContext(child.getEyePosition(),end,
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,child))
                                .getType()!=net.minecraft.world.phys.HitResult.Type.MISS)return true;''')
p.write_text(s)
