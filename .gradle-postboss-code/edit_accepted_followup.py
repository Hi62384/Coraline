from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def edit(name,old,new):
 p=base/(name+'.java');s=p.read_text();assert old in s,name;p.write_text(s.replace(old,new,1))
edit('GhostEscapeHunt','''                    p.teleportTo(outside,safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());p.setDeltaMovement(0,0,0);p.setForcedPose(null);p.setCamera(p);
                    ghost.remove("ChildRoutine");OtherMotherLoop.changed(p);''','''                    e.putLong("HomeLanding",safe.asLong());
                    ghost.remove("ChildRoutine");
                    OtherMotherLoop.changed(p);
                    p.server.overworld().getDataStorage().save();''')
edit('GhostEscapeRestoration','''        var home=com.coraline.parents.FamilyData.level(p.server,door.dimension);if(home==null||p.serverLevel()!=home)return false;''','''        var home=com.coraline.parents.FamilyData.level(p.server,door.dimension);if(home==null)return false;
        var phase=GhostEscapeProgress.phase(e);
        if(p.serverLevel()!=home&&(phase==GhostEscapeProgress.Phase.SAFE||phase==GhostEscapeProgress.Phase.RESTORING)){
            var child=GhostVictimActors.find(p,s,ghost);
            if(child==null||child.level()!=home)return true;
            LoopProps.hold(home,door.pos);
            var state=home.getBlockState(door.pos);
            if(!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())
                    ||!(home.getBlockEntity(door.pos) instanceof com.coraline.otherworld.DoorBlockEntity tile)
                    ||!s.door.equals(tile.doorId))return true;
            var room=door.pos.relative(state.getValue(com.coraline.block.TinyDoorBlock.FACING),2);
            var at=e.contains("HomeLanding")?net.minecraft.core.BlockPos.of(e.getLong("HomeLanding")):room;
            if(at.distSqr(room)>64)at=room;
            var landing=net.minecraft.world.phys.Vec3.atBottomCenterOf(at);
            if(!RescueEpilogue.safeReturn(home,p,net.minecraft.world.entity.Pose.STANDING,landing)){
                var fallback=com.coraline.otherworld.OtherWorld.safePlayer(home,room);
                if(fallback==null||fallback.distSqr(room)>64)return true;
                landing=net.minecraft.world.phys.Vec3.atBottomCenterOf(fallback);
                if(!RescueEpilogue.safeReturn(home,p,net.minecraft.world.entity.Pose.STANDING,landing))return true;
            }
            p.setCamera(p);p.stopRiding();p.setForcedPose(null);
            p.teleportTo(home,landing.x,landing.y,landing.z,p.getYRot(),p.getXRot());
            if(p.serverLevel()!=home||p.position().distanceToSqr(landing)>.25)return true;
            p.setDeltaMovement(0,0,0);
        }
        if(p.serverLevel()!=home)return false;''')
p=base/'GhostVictimWorld.java';s=p.read_text();a=s.index('        if(n.getBoolean("ExtraHouseObjectsPrepared"))');b=s.index('    private static boolean points(',a)
s=s[:a]+'''        for(String id:new String[]{"house_hall_vase","house_hall_book","house_kitchen_cup","house_bedroom_toy",
                "house_bedroom_clock","supper_bottle","kitchen_knife","supper_cup"})
            GhostVictimManifestations.retireDisplay(p,s,n,id);
        return true;
    }
'''+s[b:];
for line in s.splitlines():
 if 'Manifestations.display' in line and ('"supper_bottle"' in line or '"kitchen_knife"' in line):s=s.replace(line+'\n','')
s=s.replace('''new ItemStack(Items.PUMPKIN_PIE))
            ||!GhostVictimManifestations.display(p,s,n,"supper_cup",base.add(.92,1.08,.82),GhostVictimProgress.Disturbance.OBJECT,new ItemStack(Items.GLASS_BOTTLE)))''','''new ItemStack(Items.PUMPKIN_PIE)))''')
s=s.replace('import net.minecraft.world.item.Item;\n','')
p.write_text(s)
edit('GhostVictimManifestations','    private static void item(','''    static void retireDisplay(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id){
        var all=points(n);
        if(!all.contains(id))return;
        var row=all.getCompound(id);
        var level=level(p,row);
        if(level==null||!row.hasUUID("Entity")||!row.contains("Position",Tag.TAG_LONG))return;
        var at=BlockPos.of(row.getLong("Position"));
        LoopProps.hold(level,at);
        var display=actor(level,row,s,n);
        if(display!=null)display.discard();
        else if(level.getEntity(row.getUUID("Entity"))!=null
                ||!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at).toLong()))return;
        all.remove(id);
        OtherMotherLoop.changed(p);
    }
    private static void item(''')
edit('GhostVictimManifestations','if(!GhostVictimSchedule.manifestationAvailable(n,id))return false;','''if(!GhostVictimSchedule.manifestationAvailable(n,id))return false;
        var end=target.subtract(delta.normalize().scale(.18));
        if(level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p))
                .getType()!=net.minecraft.world.phys.HitResult.Type.MISS)return false;''')
edit('GhostVictimSchedule','''        if(point.startsWith("supper_"))''','''        if(java.util.Set.of("house_hall_vase","house_hall_book","house_kitchen_cup","house_bedroom_toy",
                "house_bedroom_clock","supper_bottle","kitchen_knife","supper_cup").contains(point))return false;
        if(point.startsWith("room_light_")&&GhostEscapeProgress.phase(session.getCompound(GhostEscapeProgress.KEY))
                ==GhostEscapeProgress.Phase.WAIT_LURE)return true;
        if(point.startsWith("supper_"))''')
edit('GhostVictimConfig','defineInRange("victimExplorationSeconds",600,30,600)','defineInRange("victimExplorationSeconds",90,30,600)')
edit('GhostVictimDirector','if(GhostVictimProgress.age(n)>=GhostVictimConfig.EXPLORATION_SECONDS.get()*20L)move(p,s,n,GhostVictimProgress.Phase.BEDTIME);','''if(GhostVictimProgress.age(n)>=GhostVictimConfig.EXPLORATION_SECONDS.get()*20L
                        ||GhostVictimProgress.age(n)>=600&&GhostVictimProgress.child(n).getInt("EvidenceCount")>=2)
                    move(p,s,n,GhostVictimProgress.Phase.BEDTIME);''')
edit('GhostEscapeHiding','''                    if(level.clip(new ClipContext(sight,eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,probe)).getType()==HitResult.Type.MISS)continue;''','''                    var obstruction=level.clip(new ClipContext(sight,eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,probe));
                    if(obstruction.getType()==HitResult.Type.MISS)continue;''')
edit('GhostEscapeHiding','row.putInt("Room",room);row.putDouble','''row.putInt("Room",room);row.putString("Label",coverLabel(room,level.getBlockState(obstruction.getBlockPos()).getBlock().getName().getString()));row.putDouble''')
edit('GhostEscapeHiding','    private static double pathLength(','''    static String coverLabel(int room,String cover){
        String area=switch(room){case 0->"Hall";case 1->"Kitchen";case 2->"Living room";case 3->"Dining room";case 4->"Bedroom";default->"Upstairs corridor";};
        return cover.isBlank()?area+" cover":area+" cover behind "+cover.toLowerCase(java.util.Locale.ROOT);
    }
    private static double pathLength(''')
edit('GhostVictimSnapshot','"Move to "+node.getString("Id").replace(\'_\',\' \')','''"Move to "+(node.contains("Label")?node.getString("Label"):GhostEscapeHiding.coverLabel(node.getInt("Room"),""))''')
