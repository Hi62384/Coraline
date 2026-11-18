from pathlib import Path
root=Path('src/main/java/com/coraline')
def read(path):return (root/path).read_text()
def put(path,s):
 with (root/path).open('w',newline='\r\n') as f:f.write(s)
path='feast/BeldamFinale.java'
s=read(path)
s=s.replace('    static boolean prepareFall(','''    private static final class StageFloor {
        private static final java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> BLUEPRINT=create();
        private static java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> create(){
            var footprint=new java.util.HashSet<BlockPos>();
            for(int x=14;x<=30;x++)for(int z=37;z<=51;z++)footprint.add(com.coraline.secrets.HouseCabinets.at(BlockPos.ZERO,x,5,z));
            var result=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(var cell:RosewoodHouse.palaceCells())if(footprint.contains(cell.offset()))result.put(cell.offset(),cell.state());
            return java.util.Map.copyOf(result);
        }
    }
    static boolean prepareFall(''')
s=s.replace('boolean authored=block.is(net.minecraft.tags.BlockTags.PLANKS)||block.is(net.minecraft.tags.BlockTags.WOODEN_SLABS);','boolean authored=block.equals(StageFloor.BLUEPRINT.get(pos.subtract(RescueEncounters.house(s))));')
s=s.replace('''        var feet=StoryFloorRoute.feet(level,block);
        return RescueEpilogue.safeReturn(level,p,Pose.STANDING,feet)?feet:null;''','''        for(var support:new BlockPos[]{block.below(),block}){
            var shape=level.getBlockState(support).getCollisionShape(level,support);
            if(shape.isEmpty())continue;
            var feet=Vec3.atBottomCenterOf(block).add(0,support.getY()-block.getY()+shape.max(net.minecraft.core.Direction.Axis.Y),0);
            if(RescueEpilogue.safeReturn(level,p,Pose.STANDING,feet))return feet;
        }
        return null;''')
a=s.index('            case CAT_THROW->{\n')
b=s.index('            case FINAL_N',a)
s=s[:a]+'''            case CAT_THROW->{
                var cat=catAtMantel(p,s);
                if(cat==null){n.putInt("Ticks",Math.max(0,t-1));n.putString("Blocker","Waiting for the original Cat before the confrontation can resume.");return;}
                int beat=Math.floorMod(t,100);
                boolean advancing=beat>=44&&beat<77&&t>=n.getInt("CatRecoverUntil");
                if(advancing)moveMother(m,p.position(),.044,p,67);
                else motherAt(m,m.position(),p,beat<44?66:67);
                if(beat==40)p.serverLevel().playSound(null,m.blockPosition(),net.minecraft.sounds.SoundEvents.SPIDER_AMBIENT,net.minecraft.sounds.SoundSource.HOSTILE,.8F,.85F);
                if(n.getBoolean("CatFlying"))catFlight(p,s,cat,m);
                else if(n.getBoolean("CatHeld")){
                    cat.setPos(p.getEyePosition().add(p.getLookAngle().scale(.65)).add(0,-.45,0));
                    RescueChapter.hint(p,advancing?"She's coming. Take aim at her face!":"Aim at Other Mother's face and left-click to throw the Cat.");
                }else RescueChapter.hint(p,"Right-click the Cat on the mantel, then aim and throw.");
                if(advancing&&!n.getBoolean("CatFlying")&&RescueChapter.phase(s)==CAT_THROW&&m.distanceToSqr(p)<1.8)
                    AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS);
            }
'''+s[b:]
s=s.replace('var cat=ShowActors.acquire(p,s,"cat",p.serverLevel(),at);if(cat==null)return null;', 'var cat=ShowActors.recoverCat(p,s,p.serverLevel(),at);if(cat==null)return null;')
a=s.index('    private static void catFlight(');b=s.index('    private static Vec3 webLanding',a)
s=s[:a]+'''    private static void catFlight(ServerPlayer p,VisitData.Scene s,Mob cat,ParentEntity mother){
        var n=RescueChapter.data(s);
        int ticks=n.getInt("CatFlightTick")+1;
        n.putInt("CatFlightTick",ticks);
        var from=cat.position();
        var velocity=new Vec3(n.getDouble("CatVX"),n.getDouble("CatVY"),n.getDouble("CatVZ"));
        var next=from.add(velocity);
        var trace=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(from,next,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,cat));
        if(catHitBeforeBlock(mother,from,next,trace)){
            var face=facePoint(mother);
            cat.setPos(face);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,face.x,face.y,face.z,16,.2,.2,.2,.05);
            p.serverLevel().playSound(null,mother.blockPosition(),net.minecraft.sounds.SoundEvents.CAT_HISS,net.minecraft.sounds.SoundSource.HOSTILE,1.1F,.9F);
            n.remove("CatFlying");
            n.remove("CatHeld");
            n.putBoolean("CatHit",true);
            RescueChapter.go(p,s,FINAL_N);
            return;
        }
        if(trace.getType()!=HitResult.Type.MISS||ticks>45){
            n.remove("CatFlying");
            n.remove("CatHeld");
            n.remove("CatMantel");
            n.putInt("CatRecoverUntil",n.getInt("Ticks")+42);
            catAtMantel(p,s);
            p.serverLevel().playSound(null,cat.blockPosition(),net.minecraft.sounds.SoundEvents.CAT_AMBIENT,net.minecraft.sounds.SoundSource.NEUTRAL,.8F,1.1F);
            RescueChapter.hint(p,"The Cat missed. Pick him up and aim at her face again.");
            return;
        }
        cat.setPos(next);
        n.putDouble("CatVY",velocity.y-.045);
        ShowPlaybackNetwork.pose(cat,34,ticks,1);
    }
'''+s[b:]
a=s.index('            case DOOR_STRUGGLE->{\n')
b=s.index('            case HAND_FALL',a)
s=s[:a]+'''            case DOOR_STRUGGLE->{
                long now=p.serverLevel().getGameTime();
                boolean pulling=n.contains("PressUntil")&&now<=n.getLong("PressUntil")&&n.getLong("PressUntil")-now<=6;
                float progress=n.getFloat("Struggle");
                if(!n.getBoolean("HandCutPending")){
                    progress=Math.max(0,Math.min(1,progress+(pulling?(n.getBoolean("AccessibleStruggle")?.017F:.012F):-.002F)));
                    n.putFloat("Struggle",progress);
                    if(RescueWebLayout.struggleFinished(t,progress))n.putBoolean("HandCutPending",true);
                }
                var door=RescueProps.find(p,s,"WebDoor");
                if(door!=null)door.frame(2,t,progress);
                if(pulling&&t%16==0)p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,net.minecraft.sounds.SoundSource.BLOCKS,.45F,.65F+progress*.5F);
                RescueChapter.hint(p,"Hold to pull the door shut — "+Math.round(progress*100)+"%");
                if(n.getBoolean("HandCutPending")){
                    var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,RescueWebSite.at(p,s,1,1).add(0,.15,-.7));
                    if(hand==null){n.putString("Blocker","Waiting to stage the severed Hand.");return;}
                    hand.frame(1,0,1);
                    p.serverLevel().playSound(null,hand.blockPosition(),net.minecraft.sounds.SoundEvents.WOOD_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1.1F,.72F);
                    n.remove("PressUntil");
                    n.remove("HandCutPending");
                    n.putBoolean("HandSevered",true);
                    RescueChapter.go(p,s,HAND_FALL);
                }else if(t>=240)retry(p,s,DOOR_STRUGGLE,"You lost your grip. Try closing the door again.");
            }
'''+s[b:]
s=s.replace('else if(state==DOOR_STRUGGLE&&action.equals("struggle"))n.putBoolean("Press",true);','else if(state==DOOR_STRUGGLE&&action.equals("struggle"))n.putLong("PressUntil",p.serverLevel().getGameTime()+6);')
s=s.replace('n.putFloat("Struggle",0);n.remove("Press");','if(!n.getBoolean("HandCutPending"))n.putFloat("Struggle",0);n.remove("PressUntil");')
put(path,s)
path='client/RescueClient.java'
s=read(path)
s=s.replace('final String phase;private long lastPress=-10;', 'final String phase;private long lastPress=-10;private boolean holding;')
a=s.index('        @Override public boolean mouseClicked(');b=s.index('        @Override public void render(',a)
s=s[:a]+'''        private void pull(){
            var level=Minecraft.getInstance().level;
            if(level==null)return;
            long now=level.getGameTime();
            if(now>=lastPress+4||now<lastPress){send("struggle");lastPress=now;}
        }
        @Override public void tick(){super.tick();if(holding)pull();}
        @Override public boolean mouseClicked(double x,double y,int button){
            if(phase.equals("DOOR_STRUGGLE")&&button==0){holding=true;pull();return true;}
            return super.mouseClicked(x,y,button);
        }
        @Override public boolean mouseReleased(double x,double y,int button){
            if(phase.equals("DOOR_STRUGGLE")&&button==0){holding=false;return true;}
            return super.mouseReleased(x,y,button);
        }
'''+s[b:]
s=s.replace('Rapidly click to pull the door shut!', 'Hold left click to pull the door shut')
put(path,s)
