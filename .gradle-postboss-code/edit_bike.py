from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def save(name,s):
 with (base/(name+'.java')).open('w',newline='\r\n') as f:f.write(s)
def read(name):return (base/(name+'.java')).read_text()
s=read('StoryBikeRoute')
i=s.index('    private StoryBikeRoute')
s=s[:i]+'''    /** Bounded search toward an actual staging position. Each edge uses the
     * same mounted clearance and step rules as the physical ride. */
    static List<Vec3> toward(WybieBicycleEntity bike,Vec3 goal){
        var start=new Node(bike.blockPosition(),bike.position(),null,0);
        var open=new PriorityQueue<Node>(Comparator.comparingDouble(
                node->node.distance+1.4*node.feet.distanceTo(goal)));
        var seen=new HashSet<BlockPos>();
        open.add(start);
        int expanded=0;
        while(!open.isEmpty()&&expanded<96){
            var node=open.remove();
            if(!seen.add(node.cell))continue;
            expanded++;
            if(node.feet.distanceToSqr(goal)<.36){
                var route=new ArrayList<Vec3>();
                for(var leg=node;leg.previous!=null;leg=leg.previous)route.add(leg.feet);
                Collections.reverse(route);
                return route;
            }
            if(node.distance>=64)continue;
            for(var direction:Direction.Plane.HORIZONTAL){
                var cell=node.cell.relative(direction);
                var feet=step(bike,node.feet,cell.getX()+.5,cell.getZ()+.5,direction.toYRot());
                if(feet==null)continue;
                var feetCell=BlockPos.containing(feet);
                if(!seen.contains(feetCell))open.add(new Node(feetCell,feet,node,node.distance+1));
            }
        }
        return null;
    }
'''+s[i:]
save('StoryBikeRoute',s)
s=read('WybieBicycleEntity')
s=s.replace('if(FirstEscapeStory.storyBike(this)){speed=0;', 'if(FirstEscapeStory.storyBike(this)||getPersistentData().hasUUID("PalaceWellOwner")){speed=0;')
s=s.replace('    void storyAdvance(Vec3 target){','    boolean storyAdvance(Vec3 target){')
s=s.replace('setDeltaMovement(Vec3.ZERO);return;}\n        var before=position();','setDeltaMovement(Vec3.ZERO);return false;}\n        var before=position();')
s=s.replace('entityData.set(STEER,0F);\n    }','entityData.set(STEER,0F);\n        return speed>.001;\n    }',1)
s=s.replace('        if(wellStory)PalaceWellEncounter.driveBike(this);\n','')
s=s.replace('        n.putLong(key+"Position",blockPosition().asLong());','        n.putLong(key+"Position",blockPosition().asLong());\n        if(wellStory){PalaceWellEncounter.driveBike(this);return;}')
i=s.index('    @Override public boolean isControlledByLocalInstance')
s=s[:i]+'''    final java.util.Deque<Vec3> wellRoute=new java.util.ArrayDeque<>();
    long wellReplanAt;
    int wellStallTicks;
'''+s[i:]
save('WybieBicycleEntity',s)
s=read('PalaceWellEncounter')
start=s.index('        Vec3 spawn=null;')
end=s.index('    private static boolean cue(',start)
s=s[:start]+'''        if(away.lengthSqr()<.1)away=new Vec3(0,0,1);
        var b=com.coraline.registry.ModEntities.WYBIE_BICYCLE.get().create(p.serverLevel());
        if(b==null)return null;
        long now=p.serverLevel().getGameTime();
        if(now<n.getLong("BikeSpawnRetryAt"))return null;
        int attempt=n.getInt("BikeSpawnSearch");
        n.putInt("BikeSpawnSearch",attempt==Integer.MAX_VALUE?0:attempt+1);
        n.putLong("BikeSpawnRetryAt",now+5);
        var direction=away.yRot((float)Math.toRadians(45*Math.floorMod(attempt,8)));
        var column=c.add(direction.scale(18+4*Math.floorMod(attempt/8,3)));
        var ground=PalaceWellMotion.ground(p.serverLevel(),w,column,6);
        if(ground==null)return null;
        float heading=StoryBikeRoute.yaw(c.subtract(ground));
        var spawn=StoryBikeRoute.floor(b,ground.x,ground.z,ground.y,heading);
        if(spawn==null)return null;
        for(var viewer:p.serverLevel().players()){
            if(viewer.distanceToSqr(spawn)<128*128
                    &&viewer.getLookAngle().dot(spawn.subtract(viewer.getEyePosition()).normalize())>.15
                    &&p.serverLevel().clip(new ClipContext(viewer.getEyePosition(),spawn.add(0,1.25,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,viewer)).getType()==HitResult.Type.MISS)return null;
        }
        b.setYRot(heading);
        b.setPos(spawn);
        var target=entryTarget(p,w,c,attempt/24);
        if(target==null)return null;
        var route=StoryBikeRoute.toward(b,target);
        if(route==null||route.isEmpty())return null;
        b.configure(s.owner,s.door,progress.session(),s.loop.event.getInt("Serial"),w.getUUID(),true);
        b.getPersistentData().putUUID("PalaceWellOwner",s.owner);
        if(!p.serverLevel().addFreshEntity(b))return null;
        n.putUUID("Bike",b.getUUID());
        n.putLong("BikePosition",b.blockPosition().asLong());
        n.putLong("Entry",BlockPos.containing(target).asLong());
        b.wellRoute.addAll(route);
        OtherMotherLoop.changed(p);
        w=bring(p,s,w,spawn);
        if(w!=null)w.startRiding(b,true);
        return b;
    }
    private static Vec3 entryTarget(ServerPlayer p,CharacterEntity w,Vec3 center,int attempt){
        var offset=new Vec3(4.5,0,-4.5).yRot((float)Math.toRadians(45*Math.floorMod(attempt,8)));
        var at=PalaceWellMotion.ground(p.serverLevel(),w,center.add(offset),8);
        return at==null?null:Vec3.atBottomCenterOf(BlockPos.containing(at));
    }
    public static void driveBike(WybieBicycleEntity b){
        var owner=b.getPersistentData().getUUID("PalaceWellOwner");var s=VisitData.get(b.getServer()).scenes.get(owner);var p=b.getServer().getPlayerList().getPlayer(owner);var n=s==null?null:control(s);
        if(p==null||n==null||n.getBoolean("Paused")||!n.hasUUID("Bike")||!b.getUUID().equals(n.getUUID("Bike"))||!eligible(p,s)){b.stop();return;}
        var progress=state(s);var w=wybie(p,s,progress);if(w==null||w.getVehicle()!=b){b.stop();return;}
        boolean entry=progress.phase()==INTRO_A&&ShowPlayback.started(s)&&ShowPlayback.position(s)>=40;
        boolean departing=progress.phase()==DEPARTING;
        if(!entry&&!departing){b.stop();return;}
        String key=departing?"Departure":"Entry";
        if(!n.contains(key)){b.stop();return;}
        var goal=Vec3.atBottomCenterOf(BlockPos.of(n.getLong(key)));
        if(b.distanceToSqr(goal)<.36){
            b.stop();
            if(entry)n.putBoolean("BikeArrived",true);
            return;
        }
        if(b.wellRoute.isEmpty()){
            long now=p.serverLevel().getGameTime();
            if(now<b.wellReplanAt)return;
            b.wellReplanAt=now+10;
            int attempt=n.getInt("BikeRouteSearch");
            n.putInt("BikeRouteSearch",attempt==Integer.MAX_VALUE?0:attempt+1);
            var route=StoryBikeRoute.toward(b,goal);
            if(route==null){
                Vec3 alternative;
                var c=center(p);
                if(entry)alternative=entryTarget(p,w,c,attempt);
                else{
                    var away=new Vec3(0,0,44).yRot((float)Math.toRadians(45*Math.floorMod(attempt,8)));
                    var ground=PalaceWellMotion.ground(p.serverLevel(),w,c.add(away),18);
                    alternative=ground==null?null:Vec3.atBottomCenterOf(BlockPos.containing(ground));
                }
                if(alternative!=null&&!alternative.equals(goal)){
                    route=StoryBikeRoute.toward(b,alternative);
                    if(route!=null)n.putLong(key,BlockPos.containing(alternative).asLong());
                }
            }
            if(route==null||route.isEmpty()){
                b.stop();
                n.putString("MotionBlocker","No clear bicycle route. Clear the path around Wybie to continue.");
                return;
            }
            n.remove("MotionBlocker");
            b.wellRoute.addAll(route);
        }
        var next=b.wellRoute.peek();
        if(b.storyAdvance(next))b.wellStallTicks=0;
        else if(++b.wellStallTicks>=3){b.wellRoute.clear();b.wellStallTicks=0;return;}
        if(b.position().distanceToSqr(next)<.02)b.wellRoute.remove();
        n.putLong("BikePosition",b.blockPosition().asLong());
    }
'''+s[end:]
s=s.replace('boolean at=b.distanceToSqr(target)<4;','boolean at=n.getBoolean("BikeArrived")&&b.distanceToSqr(target)<.36;')
# A saved bike can exist before its rider transfer was saved. Retry the same actor from its original location.
s=s.replace('if(!at&&w.getVehicle()!=b){var near=', '''if(!at&&w.getVehicle()!=b&&w.distanceToSqr(b)>32*32){
                    if(PalaceWellMotion.seen(p,b))return;
                    w=bring(p,s,w,b.position());
                    if(w==null)return;
                }
                if(!at&&w.getVehicle()!=b){var near=''')
save('PalaceWellEncounter',s)
