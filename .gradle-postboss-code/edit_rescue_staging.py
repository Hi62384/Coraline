from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def put(name,s):
 with (base/(name+'.java')).open('w',newline='\r\n') as f:f.write(s)
def read(name):return (base/(name+'.java')).read_text()
s=read('ShowActors')
i=s.index('    public static Mob acquire(')
s=s[:i]+'''    static Mob recoverCat(ServerPlayer p,VisitData.Scene s,ServerLevel level,Vec3 at){
        if(!root(s).getCompound("cat").hasUUID("Entity")&&!canonicalCat(s).hasUUID("Entity"))return null;
        var actor=acquire(p,s,"cat",level,at);
        return actor instanceof CharacterEntity cat&&cat.characterId().equals("cat")&&cat.isAlive()?cat:null;
    }
'''+s[i:]
put('ShowActors',s)
s=read('RescueEncounters')
s=s.replace('case BOB_A,BOB_B,BOB_C,FURNACE,BOB_D,BOB_TENT->{bobinsky(p,s,state,t);', 'case BOB_A,BOB_B,BOB_C,FURNACE,BOB_D,BOB_TENT->{if(!bobinsky(p,s,state,t))return;')
s=s.replace('''                    if(MouseCircusSite.inside(p,s)&&bob.level()==activity){prop(p,s,"Cheese",RescueProps.CHEESE,0,Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)));''','''                    var actorAt=Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)).add(0,2,0);
                    if(bob.level()==activity)ShowActors.position(bob,actorAt,0,7,0,1);
                    if(MouseCircusSite.inside(p,s)&&bob.level()==activity&&bob.distanceToSqr(actorAt)<.25){prop(p,s,"Cheese",RescueProps.CHEESE,0,Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)));''')
a=s.index('            case BOB_E->')
b=s.index('            case RAT_CHASE',a)
s=s[:a]+'''            case BOB_E->{
                var bob=ShowActors.actor(p,s,"evil_other_bobinsky");
                if(bob==null||bob.level()!=p.level()||!MouseCircusSite.inside(p,s))return;
                bob.setInvisible(false);
                ShowActors.position(bob,Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)).add(0,2,0),0,7,t,1);
                if(done)RescueChapter.go(p,s,SWARM_TRANSFORM);
            }
            case SWARM_TRANSFORM->{
                var bob=ShowActors.actor(p,s,"evil_other_bobinsky");
                var anchor=center(p,s);
                var swarm=bob==null||bob.level()!=p.level()||!MouseCircusSite.inside(p,s)?null:prop(p,s,"Swarm",RescueProps.SWARM,0,anchor);
                if(swarm==null||swarm.level()!=p.level()){
                    n.putInt("Ticks",0);
                    n.putString("Blocker","Waiting for the original Bobinsky and the Ratmaster swarm.");
                    return;
                }
                swarm.setPos(anchor);
                float reveal=Math.min(1,t/60F);
                RescueProps.frame(swarm,5,t,reveal);
                ShowActors.position(bob,anchor.add(0,2,0),0,80,t,1-reveal);
                if(t>=55)bob.setInvisible(true);
                if(t>=60)RescueBosses.begin(p,s,SWARM_FIGHT);
            }
'''+s[b:]
s=s.replace('if(t%20==0&&p.getY()<p.level().getMinBuildHeight()+5)retry(p,s,state);','if(state!=COLLAPSE&&t%20==0&&p.getY()<p.level().getMinBuildHeight()+5)retry(p,s,state);')
s=s.replace('    private static void bobinsky(', '    private static boolean bobinsky(')
a=s.index('    private static boolean bobinsky(');b=s.index('    static void startChase(',a)
part=s[a:b].replace('if(tent==null)return;', 'if(tent==null)return false;').replace('if(bob==null)return;', 'if(bob==null||furnace==null)return false;')
part=part.replace('if(t<160){bob.setInvisible(true);return;}', 'if(t<160){bob.setInvisible(true);return false;}')
part=part.replace('roofAnchor(p,s,base)', 'roofAnchor(p,s,bob,base)')
part=part.replace('swing is obstructed or missing.");return;', 'swing is obstructed or missing.");RescueChapter.data(s).putInt("BobSwingTicks",0);return false;')
part=part.replace('if(rig!=null){rig.setPos(anchor);rig.frame(0,t-160,1);}', 'if(rig==null)return false;rig.setPos(anchor);rig.frame(0,t-160,1);')
c=part.index('    private static Vec3 roofAnchor(')
part=part[:c].rstrip()
assert part.endswith('}')
part=part[:-1]+'''    if(state!=BOB_D)return true;
        saved.remove("Blocker");
        int swingTicks=Math.min(24,saved.getInt("BobSwingTicks")+1);
        saved.putInt("BobSwingTicks",swingTicks);
        return swingTicks>=24;
    }
    private static Vec3 roofAnchor(ServerPlayer p,VisitData.Scene s,Mob bob,Vec3 base){
        var n=RescueChapter.data(s);
        var level=p.serverLevel();
        var column=BlockPos.containing(base);
        if(n.contains("BobRoofAnchor")){
            var saved=BlockPos.of(n.getLong("BobRoofAnchor"));
            if(roofClear(level,bob,saved))return Vec3.atBottomCenterOf(saved);
        }
        for(int radius=0;radius<=3;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int y=5;y<=15;y++){
                var cell=column.offset(dx,y,dz);
                if(!roofClear(level,bob,cell))continue;
                n.putLong("BobRoofAnchor",cell.asLong());
                n.putInt("BobSwingTicks",0);
                return Vec3.atBottomCenterOf(cell);
            }
        }
        return null;
    }
    private static boolean roofClear(ServerLevel level,Mob bob,BlockPos pos){
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isFaceSturdy(level,pos,Direction.DOWN))return false;
        var anchor=Vec3.atBottomCenterOf(pos);
        for(double angle:new double[]{-1,-.67,-.33,0,.33,.67,1}){
            var at=anchor.add(Math.sin(angle)*3,-Math.cos(angle)*3-2.5,0);
            if(!PalaceWellMotion.clear(level,bob,at))return false;
        }
        return true;
    }
'''
s=s[:a]+part+s[b:]
s=s.replace('n.putInt("CollapseDuration",1500);','n.putInt("CollapseDuration",1500);n.putInt("CollapseElapsed",0);')
a=s.index('    private static void collapse(');b=s.index('    private static void carryCat',a)
part=s[a:b]
start=part.index('        var n=')
last=part.index('        if(n.getBoolean("CatRescued"))carryCat')
part=part[:start]+'''        var n=RescueChapter.data(s);
        var level=p.server.getLevel(OtherWorld.KEY);
        if(level==null||p.serverLevel()!=level)return;
        var home=house(s);
        var porch=FirstEscapeScene.porch(s);
        var cat=ShowActors.recoverCat(p,s,level,Vec3.atBottomCenterOf(porch.south(12)));
        if(cat==null){
            n.putString("Blocker","Waiting for the original Cat. The collapse clock is paused.");
            return;
        }
        if(!n.getBoolean("CatRescued")){
            if(!n.getBoolean("CatCollapsePlaced")){
                var safe=OtherWorld.safePlayer(level,porch.south(12));
                if(safe==null||!RescueEpilogue.safeReturn(level,p,Pose.STANDING,Vec3.atBottomCenterOf(safe))){
                    n.putString("Blocker","Clear the Cat's escape checkpoint outside the porch.");
                    return;
                }
                cat.setPos(Vec3.atBottomCenterOf(safe));
                n.putBoolean("CatCollapsePlaced",true);
            }
            cat.setInvisible(false);
            cat.setNoAi(true);
            cat.setNoGravity(false);
            cat.noPhysics=false;
            cat.getLookControl().setLookAt(p,20,20);
            n.putInt("Cat",cat.getId());
            LoopProps.hold(level,cat.blockPosition());
        }
        n.remove("Blocker");
        int elapsed=n.contains("CollapseElapsed")?n.getInt("CollapseElapsed")+1:Math.max(0,t-1);
        n.putInt("CollapseElapsed",elapsed);
        double radius=RescueChallengeMath.collapse(elapsed,n.getInt("CollapseDuration"),n.getDouble("CollapseInitialRadius"),48);
        n.putDouble("CollapseRadius",radius);
'''+part[last:]
part=part.replace('||t>=n.getInt("CollapseDuration")','||elapsed>=n.getInt("CollapseDuration")')
s=s[:a]+part+s[b:]
s=s.replace('n.putBoolean("CatRescued",false);n.remove("CatCollapsePlaced");','n.putBoolean("CatRescued",false);n.putInt("CollapseElapsed",0);n.remove("CatCollapsePlaced");')
s=s.replace('if(phase==COLLAPSE&&target==ShowActors.actor(p,s,"cat"))', 'if(phase==COLLAPSE&&n.getBoolean("CatCollapsePlaced")&&target==ShowActors.actor(p,s,"cat"))')
put('RescueEncounters',s)
