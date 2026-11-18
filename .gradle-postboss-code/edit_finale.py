from pathlib import Path
root=Path('src/main/java/com/coraline')
def read(name):return (root/name).read_text()
def save(name,s):
 with (root/name).open('w',newline='\r\n') as f:f.write(s)
name='feast/ParentRescueStory.java'
s=read(name)
a=s.index('        if(RescueChapter.started(s)&&RescueChapter.phase(s)==RescueChapter.Phase.ROOM_LOCKED')
b=s.index('        if(n.getBoolean("DoorLocked")',a)
s=s[:a]+'''        if(BeldamFinale.lockedDoor(s,at)&&OtherWorld.isOther(p.level())){
            p.displayClientMessage(Component.literal("The door is sealed during the confrontation."),true);
            return true;
        }
'''+s[b:]
save(name,s)
name='feast/BeldamFinale.java'
s=read(name)
i=s.index('    public static boolean preflight(')
s=s[:i]+'''    static boolean roomLocked(VisitData.Scene s){
        if(!RescueChapter.started(s))return false;
        var n=RescueChapter.data(s);
        var phase=RescueChapter.phase(s);
        return n.getBoolean("RoomLocked")&&phase.ordinal()>=ROOM_LOCKED.ordinal()
                &&phase.ordinal()<=FINAL_O.ordinal()&&!n.getBoolean("WebEntryReady");
    }
    static boolean lockedDoor(VisitData.Scene s,BlockPos at){
        if(!roomLocked(s)||!ParentRescueStory.scenery(s).contains("LittleDoor",net.minecraft.nbt.Tag.TAG_LONG))return false;
        var entrance=FirstEscapeScene.entrance(s);
        var little=little(s);
        return at.equals(entrance)||at.equals(entrance.above())||at.equals(entrance.south())
                ||at.equals(entrance.south().above())||at.equals(little)||at.equals(little.above());
    }
    static boolean keepPlayerInRoom(ServerPlayer p,VisitData.Scene s){
        if(!roomLocked(s)||RescueChapter.data(s).getBoolean("FloorOpened"))return true;
        var level=p.server.getLevel(OtherWorld.KEY);
        if(level==null)return false;
        if(p.serverLevel()==level&&FirstEscapeScene.fullyInRoom(s,p.getBoundingBox()))return true;
        var center=FirstEscapeScene.room(s);
        LoopProps.hold(level,center);
        for(int radius=0;radius<=5;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            var at=Vec3.atBottomCenterOf(center.offset(dx,0,dz));
            if(!FirstEscapeScene.fullyInRoom(s,p.getDimensions(Pose.STANDING).makeBoundingBox(at))
                    ||!RescueEpilogue.safeReturn(level,p,Pose.STANDING,at))continue;
            p.stopRiding();
            p.setForcedPose(null);
            p.setPose(Pose.STANDING);
            p.teleportTo(level,at.x,at.y,at.z,p.getYRot(),p.getXRot());
            p.fallDistance=0;
            p.setDeltaMovement(Vec3.ZERO);
            return p.serverLevel()==level&&p.position().distanceToSqr(at)<.25;
        }
        RescueChapter.data(s).putString("Blocker","Clear a standing space in the locked confrontation room to resume.");
        return false;
    }
    static boolean prepareFall(ServerPlayer p,VisitData.Scene s){
        var level=p.server.getLevel(OtherWorld.KEY);
        if(level==null)return false;
        var n=RescueChapter.data(s);
        var journal=FirstEscapeScene.context(s).getCompound("Environment");
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        var cells=java.util.ArrayList.<BlockPos>newArrayList(17*15);
        for(int x=14;x<=30;x++)for(int z=37;z<=51;z++){
            var pos=FirstEscapeScene.at(s,x,5,z);
            cells.add(pos);
            level.getChunkAt(pos);
            var block=level.getBlockState(pos);
            if(block.isAir())continue;
            var existing=journal.getCompound(Long.toString(pos.asLong()));
            boolean authored=block.is(net.minecraft.tags.BlockTags.PLANKS)||block.is(net.minecraft.tags.BlockTags.WOODEN_SLABS);
            boolean journaled=existing.contains("Placed")&&block.equals(net.minecraft.nbt.NbtUtils.readBlockState(lookup,existing.getCompound("Placed")));
            if(level.getBlockEntity(pos)!=null||!authored&&!journaled){
                n.remove("FloorOpened");
                n.putString("Blocker","The stage floor at "+pos.toShortString()+" contains an obstruction; move it before continuing.");
                return false;
            }
        }
        for(var pos:cells)if(!level.getBlockState(pos).isAir())FirstEscapeScene.place(p,s,pos,Blocks.AIR.defaultBlockState());
        for(var pos:cells)if(!level.getBlockState(pos).isAir()){
            n.remove("FloorOpened");
            n.putString("Blocker","The floor has not physically opened at "+pos.toShortString()+".");
            return false;
        }
        var room=FirstEscapeScene.room(s);
        var feet=Vec3.atBottomCenterOf(room.below(2));
        var clearance=p.getDimensions(Pose.STANDING).makeBoundingBox(feet).expandTowards(0,2,0);
        if(level.getBlockCollisions(p,clearance).iterator().hasNext()){
            n.remove("FloorOpened");
            n.putString("Blocker","Clear the falling space underneath the confrontation room.");
            return false;
        }
        n.putBoolean("FloorOpened",true);
        return true;
    }
'''+s[i:]
s=s.replace('java.util.ArrayList.<BlockPos>newArrayList(17*15)','new java.util.ArrayList<BlockPos>(17*15)')
s=s.replace('''        if(state==TUNNEL_CHASE''','''        if(!keepPlayerInRoom(p,s))return false;
        if(state.ordinal()>=FINAL_P.ordinal()&&state.ordinal()<=WEB_STEALTH.ordinal()
                &&!enterWeb(p,s,ParentRescueStory.mother(p,s)))return false;
        if(state==TUNNEL_CHASE''',1)
s=s.replace('''                if(RescueWebSite.prepare(p,s)&&t>=30&&!n.getBoolean("FloorOpened")){for(int x=14;x<=30;x++)for(int z=37;z<=51;z++)FirstEscapeScene.place(p,s,FirstEscapeScene.at(s,x,5,z),Blocks.AIR.defaultBlockState());n.putBoolean("FloorOpened",true);}
                if(n.getBoolean("FloorOpened")&&!n.getBoolean("WebEntered")&&(p.getY()<FirstEscapeScene.room(s).getY()-2||p.serverLevel()==p.server.getLevel(ActivityWorld.KEY))){enterWeb(p,s,m);}
                if(done&&n.getBoolean("WebEntered"))RescueChapter.go(p,s,FINAL_P);''','''                if(t<30||!RescueWebSite.prepare(p,s)||webLanding(p,s)==null||!prepareFall(p,s))return;
                if((p.getY()<FirstEscapeScene.room(s).getY()-2||p.serverLevel()==p.server.getLevel(ActivityWorld.KEY))
                        &&enterWeb(p,s,m)&&done)RescueChapter.go(p,s,FINAL_P);''')
a=s.index('    private static void enterWeb(')
b=s.index('    private static void rememberPosition(',a)
s=s[:a]+'''    private static Vec3 webLanding(ServerPlayer p,VisitData.Scene s){
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null||!RescueWebSite.prepare(p,s))return null;
        var landing=RescueWebSite.at(p,s,1,0);
        var block=BlockPos.containing(landing);
        LoopProps.hold(level,block);
        var feet=StoryFloorRoute.feet(level,block);
        return RescueEpilogue.safeReturn(level,p,Pose.STANDING,feet)?feet:null;
    }
    private static boolean enterWeb(ServerPlayer p,VisitData.Scene s,ParentEntity mother){
        var n=RescueChapter.data(s);
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null)return false;
        if(n.getBoolean("WebEntryReady")&&p.serverLevel()==level&&mother!=null&&mother.level()==level
                &&mother.getUUID().equals(s.mom))return true;
        n.remove("WebEntryReady");
        n.remove("WebEntered");
        if(mother==null||!mother.getUUID().equals(s.mom)||!mother.isAlive()){
            n.putString("Blocker","Waiting for the original Other Mother before entering the web.");
            return false;
        }
        var landing=webLanding(p,s);
        var motherAt=Vec3.atBottomCenterOf(RescueWebSite.center(p,s));
        LoopProps.hold(level,BlockPos.containing(motherAt));
        var motherBody=mother.getDimensions(Pose.STANDING).makeBoundingBox(motherAt);
        if(landing==null||level.getBlockCollisions(mother,motherBody).iterator().hasNext()||level.containsAnyLiquid(motherBody)){
            n.putString("Blocker","The player or Other Mother web landing is obstructed.");
            return false;
        }
        n.putUUID("WebEntryMother",mother.getUUID());
        if(p.serverLevel()!=level){
            p.stopRiding();
            p.setForcedPose(null);
            p.setPose(Pose.STANDING);
            p.teleportTo(level,landing.x,landing.y,landing.z,90,0);
            if(p.serverLevel()!=level||p.position().distanceToSqr(landing)>.25)return false;
        }
        p.fallDistance=0;
        var web=RescueProps.ensure(p,s,"Web",RescueProps.WEB,0,Vec3.atLowerCornerOf(RescueWebSite.center(p,s)));
        if(web==null||web.level()!=level)return false;
        web.frame(0,0,1);
        if(mother.level()!=level||mother.distanceToSqr(motherAt)>1){
            var moved=ShowTravel.guest(mother,p,s,level,motherAt,true,"BeldamFinaleReturn");
            if(!(moved instanceof ParentEntity original)||original.level()!=level||!original.getUUID().equals(n.getUUID("WebEntryMother"))
                    ||original.distanceToSqr(motherAt)>1){
                n.putString("Blocker","Waiting for the original Other Mother to reach the web.");
                return false;
            }
            mother=original;
        }
        n.putLong("BeldamAt",mother.blockPosition().asLong());
        n.putBoolean("WebEntered",true);
        n.putBoolean("WebEntryReady",true);
        n.remove("Blocker");
        OtherMotherLoop.changed(p);
        return true;
    }
'''+s[b:]
save(name,s)
name='feast/RescueChapter.java'
s=read(name)
s=s.replace('''        if(state.ordinal()>=Phase.FINAL_A.ordinal()&&state.ordinal()<=Phase.BANGING.ordinal()''','''        if(state==Phase.ROOM_LOCKED&&!BeldamFinale.keepPlayerInRoom(p,s)){sync(p,s);return;}
        if(state.ordinal()>=Phase.FINAL_A.ordinal()&&state.ordinal()<=Phase.BANGING.ordinal()''',1)
a=s.index('    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void block(')
b=s.index('    @SubscribeEvent',a+8)
s=s[:a]+'''    private static boolean protectedFinalDoor(ServerLevel level,BlockPos at){
        if(!OtherWorld.isOther(level))return false;
        return VisitData.get(level.getServer()).scenes.values().stream().anyMatch(scene->BeldamFinale.lockedDoor(scene,at));
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void block(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&(protectedFinalDoor(p.serverLevel(),event.getPos())
                ||event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&BeldamFinale.block(p,event.getPos()))){
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void preventFinalDoorBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event){
        if(event.getLevel() instanceof ServerLevel level&&protectedFinalDoor(level,event.getPos()))event.setCanceled(true);
    }
'''+s[b:]
save(name,s)
name='client/RescueClient.java'
s=read(name).replace('case "HIDING_CHOICE"->{option("Behind the Little Door","behind_little_door",true,0);option("In the garden","",false,1);option("In the theatre","",false,2);option("In the attic","",false,3);}', 'case "HIDING_CHOICE"->option("Behind the Little Door","behind_little_door",true,0);')
s=s.replace('case "FIGHT_CHOICE"->{option("Fight back","fight",true,0);option("Give up","",false,1);}', 'case "FIGHT_CHOICE"->option("Fight back","fight",true,0);')
save(name,s)
