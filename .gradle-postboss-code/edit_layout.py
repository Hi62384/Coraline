from pathlib import Path
root=Path('src/main/java/com/coraline')
def read(path): return (root/path).read_text()
def save(path,s):
 with (root/path).open('w',newline='\r\n') as f:f.write(s)
path='palace/PalaceTerrainPlan.java'
s=read(path)
i=s.index('    public static boolean vegetation')
s=s[:i]+'''    /** Converts the persisted terrain cursor to world columns in chunk order. */
    static PalaceSitePlan.Point column(int minX,int minZ,int cursor){
        int side=EXTENT*2+1;
        int zChunk=(cursor/side+(minZ&15))/16;
        int rows=Math.max(0,zChunk*16-(minZ&15));
        int height=Math.min(16-((minZ+rows)&15),side-rows);
        int inRows=cursor-rows*side;
        int xChunk=(inRows/height+(minX&15))/16;
        int columns=Math.max(0,xChunk*16-(minX&15));
        int width=Math.min(16-((minX+columns)&15),side-columns);
        int inChunk=inRows-columns*height;
        return new PalaceSitePlan.Point(minX+columns+inChunk%width,minZ+rows+inChunk/width);
    }
'''+s[i:]
save(path,s)
path='palace/PalaceWorldGeneration.java'
s=read(path)
s=s.replace('var palace=d.position(Kind.PALACE).orElseThrow();int x=palace.getX()-R+i%SIDE,z=palace.getZ()-R+i/SIDE;var c=chunk(l,x,z);if(c==null)return;', '''var palace=d.position(Kind.PALACE).orElseThrow();
        int minX=palace.getX()-R,minZ=palace.getZ()-R;
        var cell=n.getBoolean("CompanyFirst")?PalaceTerrainPlan.column(minX,minZ,i):new PalaceSitePlan.Point(minX+i%SIDE,minZ+i/SIDE);
        int x=cell.x(),z=cell.z();
        var c=chunk(l,x,z);if(c==null)return;''')
s=s.replace('if((i%SIDE)%16==0&&(i/SIDE)%16==0){','if(n.getBoolean("CompanyFirst")?(x==minX||(x&15)==0)&&(z==minZ||(z&15)==0):(i%SIDE)%16==0&&(i/SIDE)%16==0){')
s=s.replace('if(sample.weight()==0){n.putInt("Cursor",i+1);return;}', 'if(sample.weight()==0){n.putInt("Cursor",i+1);d.setDirty();return;}')
save(path,s)
path='feast/CinematicPropEntity.java'
s=read(path).replace('            if(kind()==RescueProps.RAT_BALL)return new AABB(','''            if(kind()==RescueProps.TONGS){
                double yaw=Math.toRadians(getYRot());
                double x=.19*Math.abs(Math.cos(yaw))+.67*Math.abs(Math.sin(yaw));
                double z=.67*Math.abs(Math.cos(yaw))+.19*Math.abs(Math.sin(yaw));
                return new AABB(getX()-x,getY()-.10,getZ()-z,getX()+x,getY()+.08,getZ()+z);
            }
            if(kind()==RescueProps.RAT_BALL)return new AABB(''')
save(path,s)
path='feast/PalaceTransport.java'
s=read(path)
s=s.replace('        if(n.contains("Route")','        if(n.contains("ActorSession")&&!n.hasUUID("ActorSession"))return null;\n        if(n.contains("Route")',1)
s=s.replace('        var level=p.server.overworld();\n        if(!level.areEntitiesLoaded', '''        var level=p.server.overworld();
        if(!n.hasUUID("ActorSession"))n.putUUID("ActorSession",n.getCompound("Progress").getUUID("Session"));
        if(!level.areEntitiesLoaded''')
s=s.replace('equals(n.getCompound("Progress").getUUID("Session"))','equals(n.getUUID("ActorSession"))')
s=s.replace('putUUID("PalaceTransportSession",n.getCompound("Progress").getUUID("Session"))','putUUID("PalaceTransportSession",n.getUUID("ActorSession"))')
s=s.replace('if(kind==MOVER&&migrateMover(prop)', '''if(kind==MOVER&&migrateMover(prop)''')
s=s.replace('n.put("Progress",PalaceTransportProgress.start(s.owner,s.door).save());root.put("Transport",n);', '''var progress=PalaceTransportProgress.start(s.owner,s.door);
        n.put("Progress",progress.save());n.putUUID("ActorSession",progress.session());root.put("Transport",n);''')
s=s.replace('''if(state.phase()==NOTICE&&vehicle==null&&!n.contains("Truck")){
            if(p.distanceToSqr(desk(p).getCenter())>25||!freeBay(p,s,n,false)||!destination(p,n))return 0;
            vehicle=ensure(p,s,n,"Truck",TRUCK,0,parked(p,n,false));
        }
        if(vehicle==null||state.phase()!=NOTICE&&p.distanceToSqr(vehicle)>144
                ||state.phase()==NOTICE&&p.distanceToSqr(desk(p).getCenter())>25)return 0;''','''if(state.phase()==NOTICE){
            if(p.distanceToSqr(desk(p).getCenter())>25||!freeBay(p,s,n,false))return 0;
            if(vehicle==null&&n.hasUUID("Truck"))return 0;
            if(!n.hasUUID("ActorSession"))n.putUUID("ActorSession",state.session());
        }else if(vehicle==null||p.distanceToSqr(vehicle)>144)return 0;''')
s=s.replace('    @SubscribeEvent public static void desk(PlayerInteractEvent.RightClickBlock event){if(!(event.getEntity() instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(!eligible(p,s)||!event.getPos().equals(desk(p)))return;event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);offer(p,s);}', '''    @SubscribeEvent public static void desk(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.serverLevel()!=p.server.overworld())return;
        var sites=PalaceSiteData.get(p.server);
        if(!sites.companyMapReady()||!event.getPos().equals(desk(p)))return;
        var s=OtherMotherLoop.scene(p);
        var root=PalaceJourney.existing(s);
        if(!PalaceJourney.owns(p,s)||root==null||!root.getBoolean("MapIssued"))return;
        if(!sites.ready()){
            p.displayClientMessage(Component.literal(sites.blocker().isEmpty()
                    ?"The Pink Palace arrival route is still being prepared.":sites.blocker()),true);
            return;
        }
        if(!eligible(p,s))return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        offer(p,s);
    }''')
save(path,s)
