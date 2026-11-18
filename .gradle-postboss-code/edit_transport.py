from pathlib import Path
base = Path('src/main/java/com/coraline/feast')
def edit(name, before, after):
 p=base/(name+'.java')
 s=p.read_text()
 assert before in s, name
 with p.open('w',newline='\r\n') as f: f.write(s.replace(before,after,1))
edit('CinematicPropEntity','    public boolean owned(VisitData.Scene s){','    boolean boundTo(VisitData.Scene s){return s!=null&&Objects.equals(owner,s.owner)&&Objects.equals(door,s.door);}\n    @Override public void tick(){super.tick();if(retiredCanonicalGhost()', 'INVALID') if False else None
edit('CinematicPropEntity','    public boolean owned(VisitData.Scene s){','    boolean boundTo(VisitData.Scene s){return s!=null&&Objects.equals(owner,s.owner)&&Objects.equals(door,s.door);}\n    public boolean owned(VisitData.Scene s){')
edit('CinematicPropEntity','super.tick();if(retiredCanonicalGhost()', 'super.tick();if(!level().isClientSide&&(kind()==TAFFY_BOWL||kind()==RescueProps.TONGS))PalaceTransport.migrateMover(this);if(retiredCanonicalGhost()')
edit('PalaceTransport','if(!(e instanceof CinematicPropEntity prop)||!prop.owned(s))return null;', 'if(!(e instanceof CinematicPropEntity prop)||!prop.boundTo(s))return null;')
edit('PalaceTransport','if(kind==MOVER&&(prop.kind()==CinematicPropEntity.TAFFY_BOWL||prop.kind()==81)){prop.configure(s,MOVER,slot);OtherMotherLoop.changed(p);return prop;}','if(kind==MOVER&&migrateMover(prop)&&prop.slot()==slot)return prop;')
p=base/'PalaceTransport.java'
s=p.read_text()
start=s.index('    public static boolean legacyMover(')
end=s.index('    private static CinematicPropEntity ensure',start)
s=s[:start]+'''    static boolean migrateMover(CinematicPropEntity prop){
        if(prop.owner()==null||prop.kind()!=CinematicPropEntity.TAFFY_BOWL&&prop.kind()!=RescueProps.TONGS)return false;
        var scene=VisitData.get(prop.getServer()).scenes.get(prop.owner());
        if(!prop.boundTo(scene))return false;
        var transport=data(scene);
        if(transport==null)return false;
        int slot=prop.slot();
        if(slot<0||slot>=4||!transport.hasUUID("Mover"+slot)||!transport.getUUID("Mover"+slot).equals(prop.getUUID()))return false;
        prop.configure(scene,MOVER,slot);
        prop.setPos(prop.position()); // Refresh the box after changing kind.
        return true;
    }
'''+s[end:]
with p.open('w',newline='\r\n') as f:f.write(s)
edit('CinematicPropEntity','kind()==TAFFY_BOWL&&!PalaceTransport.legacyMover(this)&&!MissingParentsTea.active(scene)','kind()==TAFFY_BOWL&&!MissingParentsTea.active(scene)')
edit('PalaceTransport','if(state.phase()==COMPLETE){reapCompleted(p,other,reservation);if(reservation.getBoolean("Cleaned"))continue;}\n            if(state.phase()==COMPLETE)continue;\n            if(reservation.getBoolean("Cleaned"))continue;', 'if(state.phase()==COMPLETE){\n                boolean usesBay=arrived==reservation.getBoolean("Arrived")\n                        &&bay==(arrived?reservation.getInt("DestinationBay"):reservation.getInt("Bay"));\n                if(usesBay&&!reapCompleted(p,other,reservation))return false;\n                continue;\n            }')
edit('PalaceTransport','if(n.getBoolean("VerifiedCleanup"))return true;\n        if(progress(ownerScene,n).phase()!=COMPLETE)return false;', 'var state=progress(ownerScene,n);\n        if(!state.blocker().isEmpty()||state.phase()!=COMPLETE)return false;\n        return retireActors(requester,ownerScene,n,true);\n    }\n    private static boolean retireActors(ServerPlayer requester,VisitData.Scene ownerScene,CompoundTag n,boolean completed){')
edit('PalaceTransport','        if(owner!=null&&owner.level()==level&&owner.distanceToSqr(anchor)<576)return false;\n        if(level.players().stream().anyMatch(player->player.distanceToSqr(anchor)<256))return false;', '''        boolean verified=n.getBoolean("VerifiedCleanup");
        if(verified&&List.of("Truck","Carrier","Mover0","Mover1","Mover2","Mover3").stream()
                .noneMatch(key->n.hasUUID(key)&&level.getEntity(n.getUUID(key))!=null))return true;
        if(completed&&owner!=null&&owner.level()==level&&owner.distanceToSqr(anchor)<576)return false;
        if(completed&&level.players().stream().anyMatch(player->player.distanceToSqr(anchor)<256))return false;''')
edit('PalaceTransport','prop.isVehicle()||prop.isPassenger()||level.players()', 'prop.isVehicle()||prop.isPassenger()||completed&&level.players()')
edit('PalaceTransport','        var truck=find(p,s,n,"Truck");var carrier=find(p,s,n,"Carrier");if((truck==null', '''        var state=progress(s,n);
        if(!state.blocker().isEmpty()||!PalaceJourney.owns(p,s))return 0;
        if(state.phase()==NOTICE&&PalaceSiteData.get(p.server).ready()&&p.distanceToSqr(desk(p).getCenter())<=25){
            if(!retireActors(p,s,n,false))return 0;
            PalaceJourney.existing(s).remove("Transport");
            OtherMotherLoop.changed(p);
            p.server.overworld().getDataStorage().save();
            return 1;
        }
        var truck=find(p,s,n,"Truck");var carrier=find(p,s,n,"Carrier");if((truck==null''')
edit('PalaceTransport','        if(!state.resume(s.owner))return 0;\n        if(state.phase()!=NOTICE&&state.phase()!=EXIT&&!attached(p,vehicle))return 0;', '''        if(state.phase().ordinal()<=TRANSIT.ordinal()&&!destination(p,n))return 0;
        if(state.phase()==NOTICE){
            var points=route(p,n);
            for(int i=1;i<points.length;i++)if(!clearRoute(p,points[i-1],points[i]))return 0;
        }
        if(state.phase()!=NOTICE&&state.phase()!=EXIT&&(!clearPlayer(p,vehicle.position().add(0,1,0))||!attached(p,vehicle)))return 0;
        if(!state.resume(s.owner))return 0;''')
edit('PalaceTransport','if(n.contains(key))return find(p,s,n,key); // Unloaded/missing UUID is never permission to clone.\n        var e=', '''if(n.contains(key))return find(p,s,n,key); // Unloaded/missing UUID is never permission to clone.
        var level=p.server.overworld();
        if(!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(BlockPos.containing(at)).toLong()))return null;
        var found=level.getEntitiesOfClass(CinematicPropEntity.class,new AABB(at,at).inflate(24,12,24),
                prop->prop.boundTo(s)&&prop.kind()==kind&&prop.slot()==slot);
        if(!found.isEmpty()){
            if(found.size()!=1)return null;
            var original=found.get(0);
            var tag=original.getPersistentData();
            if(!tag.hasUUID("PalaceTransportSession")||!tag.getUUID("PalaceTransportSession").equals(n.getCompound("Progress").getUUID("Session")))return null;
            n.putUUID(key,original.getUUID());
            n.putLong(key+"Position",original.blockPosition().asLong());
            OtherMotherLoop.changed(p);
            return original;
        }
        var e=''')
edit('PalaceTransport','e.configure(s,kind,slot);e.setPos(at);','e.configure(s,kind,slot);e.getPersistentData().putUUID("PalaceTransportSession",n.getCompound("Progress").getUUID("Session"));e.setPos(at);')
edit('PalaceTransport','if(verified&&List.of','if(verified&&completed&&List.of')
edit('PalaceTransport','for(var prop:props)prop.discard();n.putBoolean("Cleaned",true);n.putBoolean("VerifiedCleanup",true);OtherMotherLoop.changed(requester);return true;', '''for(var prop:props)prop.discard();
        if(completed){
            n.putBoolean("Cleaned",true);
            n.putBoolean("VerifiedCleanup",true);
            OtherMotherLoop.changed(requester);
        }
        return true;''')
