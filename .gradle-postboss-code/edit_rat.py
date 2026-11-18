from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def put(name,s):
 with (base/(name+'.java')).open('w',newline='\r\n') as f:f.write(s)
def read(name):return (base/(name+'.java')).read_text()
s=read('RescueEncounters')
s=s.replace('''var n=RescueChapter.data(s);RescueChapter.hint(p,"Catch the rat on the circus ball! Right-click it to grab the eye.");''','''var n=RescueChapter.data(s);
        if(n.getBoolean("RatResetPending")){resetRatSegment(p,s);return;}
        RescueChapter.hint(p,"Catch the rat on the circus ball! Right-click it to grab the eye.");''')
s=s.replace('if(moved!=null){n.putUUID("ChaseRat",moved.getUUID());n.putInt("RatLeg",2);', '''if(moved!=null&&rat.getUUID().equals(moved.getUUID())&&moved.level()==p.level()){
                    n.putInt("RatLeg",2);
                    n.putLong("RatHouseStart",moved.blockPosition().asLong());
                    n.remove("RatHouseMoved");''')
s=s.replace('RescueProps.retire(p,s,"RatBall0");unlockAttic','retireRatBall(p,s,0);unlockAttic')
s=s.replace('''        n.putLong("RatAt",rat.blockPosition().asLong());n.putString("RatDimension",rat.level().dimension().location().toString());LoopProps.hold((ServerLevel)rat.level(),rat.blockPosition());
        if(t>=1800)retry(p,s,RAT_CHASE);''','''        n.putLong("RatAt",rat.blockPosition().asLong());n.putString("RatDimension",rat.level().dimension().location().toString());LoopProps.hold((ServerLevel)rat.level(),rat.blockPosition());
        if(leg>=2&&rat.getVehicle() instanceof CinematicPropEntity ball&&ball.kind()==RescueProps.RAT_BALL){
            if(!n.contains("RatHouseStart"))n.putLong("RatHouseStart",ball.blockPosition().asLong());
            if(ball.blockPosition().distSqr(BlockPos.of(n.getLong("RatHouseStart")))>=9)n.putBoolean("RatHouseMoved",true);
        }''')
s=s.replace('''var n=RescueChapter.data(s);var ball=RescueProps.ensure(p,s,"RatBall"+leg,RescueProps.RAT_BALL,0,rat.position());if(ball==null)return;''','''var n=RescueChapter.data(s);
        if(rat.level()!=p.level())return;
        var ball=RescueProps.ensure(p,s,"RatBall"+leg,RescueProps.RAT_BALL,0,rat.position());
        if(ball==null){
            if(retireRatBall(p,s,leg))clearRatPath(n);
            else n.putString("Blocker","Waiting for the original circus ball or its recorded chunk.");
            return;
        }
        if(leg>=2&&n.getBoolean("RatHouseMoved")==false&&!n.contains("RatHouseStart"))n.putLong("RatHouseStart",ball.blockPosition().asLong());''')
s=s.replace('''        var direction=footing.subtract(ball.position()).multiply(1,0,1);double speed=p.distanceToSqr(rat)>24*24?.075:.145;''','''        if(n.contains("RatBallLast")&&n.getLong("RatBallLast")==ball.blockPosition().asLong()
                &&ball.getDeltaMovement().horizontalDistanceSqr()<.003){
            int stalled=n.getInt("RatStuck")+1;
            n.putInt("RatStuck",stalled);
            if(stalled>=20){clearRatPath(n);return;}
        }else n.putInt("RatStuck",0);
        n.putLong("RatBallLast",ball.blockPosition().asLong());
        var direction=footing.subtract(ball.position()).multiply(1,0,1);double speed=leg>=2&&!n.getBoolean("RatHouseMoved")?.19:p.distanceToSqr(rat)>24*24?.075:.145;''')
s=s.replace('''if(!canClaimRatEye(n.getInt("RatLeg"))){RescueChapter.hint(p,"Follow the rat out of the circus and return to the attic before catching it.");return true;}''','''if(!canClaimRatEye(n.getInt("RatLeg"))||!n.getBoolean("RatHouseMoved")||n.getBoolean("RatResetPending")
                ||!(target.getVehicle() instanceof CinematicPropEntity ball)||ball.kind()!=RescueProps.RAT_BALL||!ball.owned(s)){
            RescueChapter.hint(p,"Follow the rolling rat into the house and cut it off once it is moving.");
            return true;
        }''')
s=s.replace('''for(int i=0;i<3;i++)RescueProps.retire(p,s,"RatBall"+i);RescueProps.retire(p,s,"ChaseObstacles");''','''for(int i=0;i<3;i++)retireRatBall(p,s,i);RescueProps.retire(p,s,"ChaseObstacles");''')
a=s.index('            FirstEscapeStory.hint(p,"The rat escaped')
b=s.index('        }else if(state==COLLAPSE)',a)
s=s[:a]+'''            if(!n.getBoolean("RatResetPending")){
                n.putBoolean("RatResetPending",true);
                OtherMotherLoop.changed(p);
                p.server.overworld().getDataStorage().save();
            }
            resetRatSegment(p,s);
'''+s[b:]
i=s.index('    private RescueEncounters()')
s=s[:i]+'''    private static void clearRatPath(CompoundTag n){
        n.remove("RatPathTarget");
        n.remove("RatPath");
        n.remove("RatPathNode");
        n.remove("RatBallLast");
        n.putInt("RatStuck",0);
    }
    private static void resetRatSegment(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);
        var rat=chaseRat(p,s);
        if(rat==null){n.putString("Blocker","Waiting for the recorded chase rat before resuming.");return;}
        int leg=n.getInt("RatLeg");
        var level=p.server.getLevel(leg>=2?OtherWorld.KEY:ActivityWorld.KEY);
        var tent=MouseCircusSite.tent(p,s);
        if(level==null||leg>=2&&tent==null)return;
        var playerAt=leg>=2?tent.south(1):MouseCircusSite.center(p,s).south(16);
        var ratAt=leg>=2?tent.south(3):MouseCircusSite.center(p,s).south(10);
        LoopProps.hold(level,ratAt);
        var safe=OtherWorld.safePlayer(level,ratAt);
        if(safe==null)return;
        for(int slot=0;slot<3;slot++)if(!retireRatBall(p,s,slot)){
            n.putString("Blocker","Waiting for the recorded circus ball before resuming the chase.");
            return;
        }
        if(!RescueChapter.transfer(p,level,playerAt))return;
        var moved=ShowTravel.guest(rat,p,s,level,Vec3.atBottomCenterOf(safe),true,"RescueRatReturn");
        if(moved==null||!rat.getUUID().equals(moved.getUUID())||moved.level()!=level)return;
        moved.setPos(Vec3.atBottomCenterOf(safe));
        n.putInt("RatLeg",leg>=2?2:0);
        n.putInt("RatNode",0);
        n.putLong("RatAt",moved.blockPosition().asLong());
        n.putString("RatDimension",moved.level().dimension().location().toString());
        n.putLong("RatHouseStart",moved.blockPosition().asLong());
        n.remove("RatHouseMoved");
        n.remove("RatExitWait");
        n.remove("RatResetPending");
        clearRatPath(n);
        RescueChapter.go(p,s,RAT_CHASE);
    }
    private static boolean retireRatBall(ServerPlayer p,VisitData.Scene s,int leg){
        var scenery=ParentRescueStory.scenery(s);
        String key="RescueRatBall"+leg;
        if(!scenery.contains(key))return !FirstEscapeScene.context(s).contains(key);
        if(!scenery.hasUUID(key))return false;
        var id=scenery.getUUID(key);
        var level=com.coraline.parents.FamilyData.level(p.server,scenery.getString(key+"Dimension"));
        if(level==null||!scenery.contains(key+"Position",Tag.TAG_LONG))return false;
        var at=BlockPos.of(scenery.getLong(key+"Position"));
        LoopProps.hold(level,at);
        Entity original=null;
        for(var world:p.server.getAllLevels()){
            var entity=world.getEntity(id);
            if(entity==null)continue;
            if(original!=null||world!=level)return false;
            original=entity;
        }
        if(original==null&&!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at).toLong()))return false;
        if(original!=null){
            if(!(original instanceof CinematicPropEntity ball)||ball.kind()!=RescueProps.RAT_BALL||ball.slot()!=0||!ball.owned(s))return false;
            var n=RescueChapter.data(s);
            if(ball.getPassengers().stream().anyMatch(rider->!n.hasUUID("ChaseRat")||!rider.getUUID().equals(n.getUUID("ChaseRat"))))return false;
            ball.ejectPassengers();
            ball.discard();
        }
        var n=RescueChapter.data(s);
        var retired=n.getCompound("RetiredRatBalls");
        retired.putBoolean(id.toString(),true);
        n.put("RetiredRatBalls",retired);
        for(var field:new String[]{key,key+"Position",key+"Dimension"})scenery.remove(field);
        OtherMotherLoop.changed(p);
        return true;
    }
    static boolean retiredRatBall(CinematicPropEntity prop,VisitData.Scene scene){
        return prop.kind()==RescueProps.RAT_BALL&&prop.boundTo(scene)
                &&RescueChapter.data(scene).getCompound("RetiredRatBalls").getBoolean(prop.getUUID().toString());
    }
'''+s[i:]
put('RescueEncounters',s)
s=read('CinematicPropEntity')
s=s.replace('if(!owned(s)||!FirstEscapeStory.retiredGrab', '''if(RescueEncounters.retiredRatBall(this,s)&&getPassengers().stream().noneMatch(passenger->passenger instanceof ServerPlayer)){
            ejectPassengers();
            discard();
            return true;
        }
        if(!owned(s)||!FirstEscapeStory.retiredGrab''')
put('CinematicPropEntity',s)
