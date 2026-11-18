from pathlib import Path
root=Path('src/main/java/com/coraline')
def read(path): return (root/path).read_text()
def save(path,s):
 with (root/path).open('w',newline='\r\n') as f:f.write(s)
path='feast/RescueEpilogue.java'
s=read(path)
s=s.replace('if(protectedVision(s)){if(!n.contains("VisionReturn"))saveVision(p,s);holdVision(p,s);if(p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY)&&state!=EYES_CLOSE){recoverVision(p,s);return;}}','''if(protectedVision(s)&&!n.getBoolean("VisionRestored")){
            if(!n.contains("VisionReturn"))saveVision(p,s);
            holdVision(p,s);
            if(p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY)&&state!=EYES_CLOSE){
                enterVision(p,s);
                return;
            }
        }''')
s=s.replace('case EYES_CLOSE->{if(t>=40&&(visionReady(p,s)||t>=200)){if(p.server.getLevel(ActivityWorld.KEY)!=null){enterVision(p,s);RescueChapter.go(p,s,CELESTIAL_OPEN);}else if(restoreVision(p,s))RescueChapter.go(p,s,RELEASED_SPIRITS);}}','''case EYES_CLOSE->{
                if(t>=40&&enterVision(p,s))RescueChapter.go(p,s,CELESTIAL_OPEN);
            }''')
s=s.replace('for(int i=0;i<3;i++){var ghost=ghost(p,s,i);if(ghost!=null){ghost.setInvisible(false);ghost.frame(t>=240?5:4,t,1);}}','''if(!n.getBoolean("VisionRestored"))
                    for(int i=0;i<3;i++){var ghost=ghost(p,s,i);if(ghost!=null){ghost.setInvisible(false);ghost.frame(t>=240?5:4,t,1);}}''')
start=s.index('    private static boolean visionReady')
end=s.index('    private static void holdVision',start)
s=s[:start]+'''    private static boolean enterVision(ServerPlayer p,VisitData.Scene s){
        var c=visionCenter(p,s);
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null)return false;
        LoopProps.hold(level,c);
        p.stopRiding();
        p.setForcedPose(null);
        p.setPose(Pose.STANDING);
        p.teleportTo(level,c.getX()+.5,c.getY(),c.getZ()+.5,0,0);
        if(p.serverLevel()!=level||p.distanceToSqr(Vec3.atBottomCenterOf(c))>.25)return false;
        RescueChapter.data(s).putLong("VisionAt",c.asLong());
        return true;
    }
'''+s[end:]
s=s.replace('n.putBoolean("VisionRestored",false);if(p.serverLevel()', 'if(p.serverLevel()')
start=s.index('    private static CinematicPropEntity ghost')
end=s.index('        if(old==null)return null;var l=',start)
s=s[:start]+'''    private static CinematicPropEntity ghost(ServerPlayer p,VisitData.Scene s,int index){
        var stage=p.server.getLevel(ActivityWorld.KEY);
        if(stage==null)return null;
        String key="FarewellProjection"+index;
        var context=FirstEscapeScene.context(s);
        var at=Vec3.atBottomCenterOf(visionCenter(p,s)).add((index-1)*2,0,4);
        var chunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(at));
        LoopProps.hold(stage,BlockPos.containing(at));
        var old=findGhost(p,s,index);
        var projection=FirstEscapeScene.prop(p,s,key);
        if(projection!=null&&(projection.level()!=stage||projection.kind()!=CinematicPropEntity.GHOST_CHILD
                ||projection.slot()!=index||projection.canonicalGhost(s)))return null;
        if(old==null){
            if(projection!=null){
                projection.setPos(at);
                return projection;
            }
            if(context.hasUUID(key)){
                var savedAt=context.contains(key+"Position")?BlockPos.of(context.getLong(key+"Position")):BlockPos.containing(at);
                if(savedAt.distSqr(BlockPos.containing(at))>16||!stage.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(savedAt).toLong()))return null;
                for(var level:p.server.getAllLevels())if(level.getEntity(context.getUUID(key))!=null)return null;
                context.remove(key);
                context.remove(key+"Position");
                OtherMotherLoop.changed(p);
            }
            if(!stage.areEntitiesLoaded(chunk.toLong()))return null;
            return FirstEscapeScene.ensure(p,s,key,CinematicPropEntity.GHOST_CHILD,index,stage,at);
        }
        if(projection!=null)projection.discard();
'''+s[end:]
s=s.replace('if(old==null)return null;var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return null;var c=visionCenter(p,s);var at=Vec3.atBottomCenterOf(c).add((index-1)*2,0,4);Entity result=old;', 'var l=stage;Entity result=old;')
s=s.replace('var n=RescueChapter.data(s);var saved=n.getCompound("VisionReturn");var l=FamilyData.level(p.server,saved.getString("Dimension"));if(l==null)return false;', '''var n=RescueChapter.data(s);
        if(n.getBoolean("VisionRestored"))return true;
        var saved=n.getCompound("VisionReturn");
        var l=p.server.overworld();
        if(!saved.getString("Dimension").equals(l.dimension().location().toString()))return false;''')
start=s.index('        l.getChunkAt(BlockPos.containing(original));Vec3 target=original;')
end=s.index('        p.teleportTo(l,target',start)
s=s[:start]+'''        Vec3 target=safeReturn(l,p,pose,original)?original:nearReturn(l,p,pose,BlockPos.containing(original),4);
        var door=DoorData.get(p.server).doors.get(s.door);
        if(target==null&&door!=null&&door.placed&&s.owner.equals(door.owner)&&FamilyData.level(p.server,door.dimension)==l){
            target=nearReturn(l,p,pose,door.pos,6);
            if(target==null){
                for(var family:FamilyData.get(p.server).families.values()){
                    if(!s.owner.equals(family.owner)||family.bed==null||FamilyData.level(p.server,family.homeDim)!=l
                            ||family.bed.distSqr(door.pos)>32*32)continue;
                    target=nearReturn(l,p,pose,family.bed,6);
                    if(target!=null)break;
                }
            }
        }
        if(target==null){
            n.putString("Blocker","Clear safe footing and standing space near your original Little Door.");
            return false;
        }
'''+s[end:]
s=s.replace('n.remove("VisionAt");n.putBoolean("VisionRestored",true);','n.remove("VisionAt");n.remove("Blocker");n.putBoolean("VisionRestored",true);')
start=s.index('    private static boolean safeReturn')
end=s.index('    private RescueEpilogue()',start)
s=s[:start]+'''    private static Vec3 nearReturn(ServerLevel level,ServerPlayer player,Pose pose,BlockPos anchor,int radius){
        LoopProps.hold(level,anchor);
        for(int r=0;r<=radius;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            for(int dy:new int[]{0,1,-1,2,-2,3,-3}){
                var at=Vec3.atBottomCenterOf(anchor.offset(dx,dy,dz));
                if(safeReturn(level,player,pose,at))return at;
            }
        }
        return null;
    }
    private static boolean safeReturn(ServerLevel level,ServerPlayer player,Pose pose,Vec3 at){
        var body=player.getDimensions(pose).makeBoundingBox(at);
        var low=BlockPos.containing(body.minX,body.minY-.10,body.minZ);
        var high=BlockPos.containing(body.maxX,body.maxY,body.maxZ);
        if(low.getY()<level.getMinBuildHeight()||high.getY()>=level.getMaxBuildHeight()
                ||!level.getWorldBorder().isWithinBounds(low)||!level.getWorldBorder().isWithinBounds(high)
                ||!level.hasChunksAt(low,high)||!level.noCollision(player,body)||level.containsAnyLiquid(body))return false;
        for(var pos:BlockPos.betweenClosed(low,high)){
            var state=level.getBlockState(pos);
            if(state.is(net.minecraft.tags.BlockTags.FIRE)||state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                    ||state.is(net.minecraft.world.level.block.Blocks.CACTUS)||state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                    ||state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)||state.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)
                    ||state.is(net.minecraft.tags.BlockTags.CAMPFIRES))return false;
        }
        var support=new AABB(body.minX,body.minY-.10,body.minZ,body.maxX,body.minY,body.maxZ);
        return level.getBlockCollisions(player,support).iterator().hasNext();
    }
    public static void recoverVision(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        if(!RescueChapter.owns(p,s)||!n.contains("VisionReturn")||n.getBoolean("VisionRestored"))return;
        holdVision(p,s);
        n.remove("Audio");
        n.remove("AudioTail");
        if(RescueChapter.phase(s)!=EYES_CLOSE&&p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY))enterVision(p,s);
    }
'''+s[end:]
save(path,s)
path='feast/PalaceWellEncounter.java'
s=read(path).replace('if(b.distanceToSqr(goal)<.36){\n            b.stop();','if(b.distanceToSqr(goal)<.36){\n            b.wellRoute.clear();\n            b.stop();')
save(path,s)
