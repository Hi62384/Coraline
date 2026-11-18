from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def edit(name,before,after):
 p=base/(name+'.java')
 s=p.read_text()
 assert before in s,name
 with p.open('w',newline='\r\n') as f:f.write(s.replace(before,after,1))
edit('PalaceKeyCustody','    private static CompoundTag read(Path file,long limit)', '    static CompoundTag read(Path file,long limit)')
edit('RescueEpilogue','if(protectedVision(s)&&!n.getBoolean("VisionRestored")){', 'if(protectedVision(s)&&!n.getBoolean("DreamDialogueComplete")&&!n.getBoolean("VisionRestored")){')
edit('RescueEpilogue','if(n.getBoolean("VisionRestored"))return true;\n        var saved=', '''if(n.getBoolean("VisionRestored")&&p.serverLevel()==p.server.overworld())return true;
        if(n.getBoolean("VisionLandingPending")&&p.serverLevel()==p.server.overworld())return verifyVisionLanding(p,n);
        var saved=''')
edit('RescueEpilogue','n.remove("VisionAt");n.remove("Blocker");n.putBoolean("VisionRestored",true);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);OtherMotherLoop.changed(p);return true;', '''p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        n.putBoolean("VisionLandingPending",true);
        OtherMotherLoop.changed(p);
        return verifyVisionLanding(p,n);''')
edit('RescueEpilogue','    private static Vec3 nearReturn(','''    private static boolean verifyVisionLanding(ServerPlayer p,CompoundTag n){
        long now=p.server.overworld().getGameTime();
        if(n.contains("VisionSaveAfter")&&now<n.getLong("VisionSaveAfter")&&n.getLong("VisionSaveAfter")-now<=20)return false;
        try{
            p.server.getPlayerList().saveAll();
            var world=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR);
            var saved=PalaceKeyCustody.read(world.resolve(p.getUUID()+".dat"),16L*1024*1024);
            var position=saved.getList("Pos",Tag.TAG_DOUBLE);
            if(!saved.getString("Dimension").equals(p.server.overworld().dimension().location().toString())
                    ||position.size()!=3||p.distanceToSqr(position.getDouble(0),position.getDouble(1),position.getDouble(2))>.25)
                throw new java.io.IOException("Return position was not confirmed");
        }catch(java.io.IOException|RuntimeException unavailable){
            n.putLong("VisionSaveAfter",now+20);
            n.putString("Blocker","Waiting for storage to confirm your safe return.");
            OtherMotherLoop.changed(p);
            return false;
        }
        n.remove("VisionAt");
        n.remove("VisionSaveAfter");
        n.remove("VisionLandingPending");
        n.remove("Blocker");
        n.putBoolean("VisionRestored",true);
        OtherMotherLoop.changed(p);
        return true;
    }
    private static Vec3 nearReturn(''')
edit('RescueEpilogue','if(!RescueChapter.owns(p,s)||!n.contains("VisionReturn")||n.getBoolean("VisionRestored"))return;', 'if(!RescueChapter.owns(p,s)||!n.contains("VisionReturn")||n.getBoolean("VisionRestored")||n.getBoolean("DreamDialogueComplete"))return;')
edit('PalaceTransport','var level=p.server.overworld();\n        if(!n.hasUUID("ActorSession"))', '''var level=p.server.overworld();
        var homeChunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(at));
        level.getChunkSource().addRegionTicket(TicketType.PORTAL,homeChunk,2,homeChunk.getWorldPosition());
        if(!n.hasUUID("ActorSession"))''')
edit('PalaceTransport','!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(BlockPos.containing(at)).toLong())','!level.areEntitiesLoaded(homeChunk.toLong())')
edit('PalaceTransport','n.putBoolean("Paused",true);n.putString("Blocker",reason);','rememberActors(p,s,n);\n        n.putBoolean("Paused",true);n.putString("Blocker",reason);')
edit('PalaceTransport','        int slot=prop.slot();\n        if(slot<0','''        var scenery=ParentRescueStory.scenery(scene);
        var context=FirstEscapeScene.context(scene);
        for(var reference:List.of(scenery,context))
            if(reference.hasUUID("RescueWellTongs")&&reference.getUUID("RescueWellTongs").equals(prop.getUUID()))return false;
        int slot=prop.slot();
        if(slot<0''')
