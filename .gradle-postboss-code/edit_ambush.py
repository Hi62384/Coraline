from pathlib import Path
b=Path('src/main/java/com/coraline/feast')
def edit(n,o,r):
 p=b/(n+'.java');s=p.read_text();assert o in s,n;p.write_text(s.replace(o,r,1))
edit('GhostVictimManifestations','''        if(motherSaw||motherHeard||reports){''','''        var escape=n.getCompound(GhostEscapeProgress.KEY);
        if(kind==GhostVictimProgress.Disturbance.LIGHT&&mother!=null&&mother.level()==level
                &&GhostEscapeProgress.owns(n,escape)&&GhostEscapeProgress.phase(escape)==GhostEscapeProgress.Phase.WAIT_LURE
                &&mother.distanceToSqr(target)<18*18){
            escape.putLong("Distraction",position.asLong());
            escape.putLong("DistractionUntil",GhostVictimProgress.clock(n)+240);
            escape.remove("StealSignaled");
            n.putLong("MotherInvestigation",position.asLong());
            n.putLong("MotherInvestigateUntil",escape.getLong("DistractionUntil"));
        }
        if(motherSaw||motherHeard||reports){''')
edit('GhostEscapeAmbush','''        boolean away=near&&!GhostVictimNavigation.canSee(mother,child,8,150);
        if(!near||!away){''','''        boolean distracted=near&&distractionReady(ghost,e,mother);
        boolean away=distracted&&!GhostVictimNavigation.canSee(mother,child,8,150);
        if(!away||!e.getBoolean("StealSignaled")){''')
edit('GhostEscapeAmbush','    private GhostEscapeAmbush()','''    static boolean distractionReady(CompoundTag ghost,CompoundTag escape,com.coraline.parents.ParentEntity mother){
        if(mother==null||!escape.contains("Distraction")||GhostVictimProgress.clock(ghost)>=escape.getLong("DistractionUntil"))return false;
        var point=BlockPos.of(escape.getLong("Distraction"));
        if(!ghost.contains("MotherInvestigation")||ghost.getLong("MotherInvestigation")!=point.asLong())return false;
        var direction=point.getCenter().subtract(mother.getEyePosition());
        return mother.distanceToSqr(point.getCenter())<=36&&GhostVictimNavigation.view(mother).dot(direction.normalize())>.65;
    }
    private GhostEscapeAmbush()''')
edit('GhostVictimDirector','''            if(!GhostEscapeProgress.signal(n,!value.equals("escape_stop"),true,false))return;''','''            if(value.equals("escape_go")&&GhostEscapeProgress.owns(n,e)&&GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.WAIT_LURE){
                if(!GhostEscapeAmbush.distractionReady(n,e,AcceptedButtonEyesWorld.mother(p,s))){
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Flicker a nearby light and wait until Other Mother investigates it."),true);
                    return;
                }
                e.putBoolean("StealSignaled",true);
                OtherMotherLoop.changed(p);
                return;
            }
            if(value.equals("escape_stop"))e.remove("StealSignaled");
            if(!GhostEscapeProgress.signal(n,!value.equals("escape_stop"),true,false))return;''')
edit('GhostVictimSnapshot','''boolean stopOnly=phase!=GhostEscapeProgress.Phase.WAIT_GO;''','''boolean stopOnly=phase!=GhostEscapeProgress.Phase.WAIT_GO&&phase!=GhostEscapeProgress.Phase.WAIT_LURE;''')
edit('GhostVictimDirector','''var mirror=n.getCompound("Mirrors").getCompound("bedroom");var at=BlockPos.of(mirror.getLong("Position"));child.getLookControl().setLookAt(at.getX()+.5,at.getY()+1.35,at.getZ()+.5,30,30);''','''GhostVictimMirrors.faceMirror(n,"bedroom",child);''')
