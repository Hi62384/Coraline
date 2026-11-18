from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
p=base/'GhostVictimMirrors.java'
s=p.read_text()
a=s.index('    public static boolean childPresent('); b=s.index('    public static boolean enter(',a)
s=s[:a]+'''    public static boolean childPresent(ServerPlayer p,CompoundTag n,String id,TycoonNpc child){
        var mirror=point(n,id);
        if(child==null||child.level()!=p.level()||!available(n,id,p.level().dimension().location().toString())
                ||p.distanceToSqr(surface(mirror))>16)return false;
        return childAtMirror(n,id,child,true);
    }
    static boolean childAtMirror(CompoundTag n,String id,TycoonNpc child,boolean requireFacing){
        return child!=null&&child.isAlive()&&!child.isInvisible()&&GhostVictimProgress.hasVictim(n)
                &&child.owner!=null&&n.hasUUID("Owner")&&child.owner.equals(n.getUUID("Owner"))
                &&GhostVictimProgress.child(n).getUUID("Id").equals(child.resident)
                &&n.hasUUID("VictimActor")&&child.getUUID().equals(n.getUUID("VictimActor"))
                &&childCanSeeMirror(n,id,child,child.position())
                &&(!requireFacing||GhostVictimNavigation.view(child).dot(surface(point(n,id)).subtract(child.getEyePosition()).normalize())
                        >=Math.cos(Math.toRadians(65)));
    }
    static boolean childCanSeeMirror(CompoundTag n,String id,net.minecraft.world.entity.LivingEntity child,Vec3 feet){
        var mirror=point(n,id);
        if(!mirror.contains("Position"))return false;
        var center=surface(mirror);
        var eye=feet.add(0,child.getEyeHeight(),0);
        var delta=center.subtract(eye);
        if(delta.lengthSqr()>25||delta.lengthSqr()<.01||feet.subtract(center).dot(normal(mirror))<.1)return false;
        var end=center.subtract(delta.normalize().scale(.16));
        return child.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,child)).getType()==HitResult.Type.MISS;
    }
    static void faceMirror(CompoundTag n,String id,TycoonNpc child){
        var target=surface(point(n,id));
        child.getLookControl().setLookAt(target.x,target.y,target.z,30,30);
    }
'''+s[b:]
p.write_text(s)
p=base/'GhostVictimWorld.java'
s=p.read_text().replace('''            var eye=Vec3.atBottomCenterOf(stored).add(0,child.getEyeHeight(),0);
            if(eye.distanceTo(GhostVictimMirrors.surface(mirror))<4.95)return stored;''','''            var path=child.getNavigation().createPath(stored,0);
            if(GhostVictimMirrors.childCanSeeMirror(n,"bedroom",child,Vec3.atBottomCenterOf(stored))
                    &&(child.blockPosition().equals(stored)||path!=null&&path.canReach()))return stored;''')
s=s.replace('''if(distance>=4.95)continue;''','''if(distance>=4.95||!GhostVictimMirrors.childCanSeeMirror(n,"bedroom",child,Vec3.atBottomCenterOf(candidate)))continue;''')
p.write_text(s)
p=base/'GhostEscapeCovert.java'
s=p.read_text().replace('GhostVictimRoutines.mirrorStop(ghost,child,"bedroom")','GhostVictimWorld.bedtimeConversationPosition(s,level,child,ghost)')
s=s.replace('''false)==GhostVictimNavigation.Result.ARRIVED
                        &&GhostVictimMirrors.childPresent(p,ghost,"bedroom",child)){
                    e.remove("Blocker");
                    hands(child);GhostVictimRoutines.lookAtMirror(ghost,child);''','''false)==GhostVictimNavigation.Result.ARRIVED){
                    GhostVictimMirrors.faceMirror(ghost,"bedroom",child);
                    if(!GhostVictimMirrors.childAtMirror(ghost,"bedroom",child,true))return true;
                    e.remove("Blocker");
                    hands(child);''')
p.write_text(s)
p=base/'GhostVictimRoutines.java'
s=p.read_text().replace('''        return mirrorStop(n,child,ids.get(Math.floorMod(index,ids.size())));
    }
    static BlockPos mirrorStop(CompoundTag n,TycoonNpc child,String id){
        if(!GhostVictimMirrors.available(n,id,child.level().dimension().location().toString()))return null;''','''        String id=ids.get(Math.floorMod(index,ids.size()));''')
p.write_text(s)
