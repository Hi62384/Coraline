from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def read(name):return (base/(name+'.java')).read_text()
def put(name,s):
 with (base/(name+'.java')).open('w',newline='\r\n') as f:f.write(s)
s=read('RatmasterRegion')
start=s.index('            case 2 -> {')
end=s.index('            case 3',start)
s=s[:start]+'''            case 2 -> {
                for (var lane : lanes(center, angle, tier)) region.strip(lane, forward, 17, 17, 1.15);
            }
'''+s[end:]
i=s.index('    static List<Vec3> carriers')
s=s[:i]+'''    static List<Vec3> lanes(Vec3 center, double angle, int tier) {
        if (tier == 0) return List.of(center);
        var across = new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(4);
        return List.of(center.subtract(across), center, center.add(across));
    }

    static RatmasterRegion ballImpact(int tier, Vec3 center, double angle, int beat) {
        var region = new RatmasterRegion(center.y, 1.6);
        var forward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        for (var lane : lanes(center, angle, tier)) {
            region.strip(ballPosition(lane, forward, beat), forward, 1.15, 1.15, 1.15);
        }
        return region;
    }

    static Vec3 ballPosition(Vec3 lane, Vec3 forward, int beat) {
        double progress = Math.max(0, Math.min(1, (beat - 38) / 40.0));
        return lane.add(forward.scale(-16 + 32 * progress));
    }

'''+s[i:]
put('RatmasterRegion',s)
s=read('RescueBosses')
s=s.replace('''            if(impact)for(var member:participants)if(region.hits(member.getBoundingBox())&&member.invulnerableTime<=0)
                RescueCombat.bossHarm(member,tier==2?10:8);''','''            boolean rolling=pattern==2&&beat>=38&&beat<=78;
            var contact=rolling?RatmasterRegion.ballImpact(tier,center,n.getDouble("AttackAngle"),beat):region;
            if(rolling||pattern!=2&&impact)for(var member:participants)
                if(contact.hits(member.getBoundingBox())&&member.invulnerableTime<=0)RescueCombat.bossHarm(member,tier==2?10:8);''')
s=s.replace('''for(int i=0;i<4;i++)RescueProps.hide(p,s,"RatDecoy"+i);RescueEncounters.startChase''','''for(int i=0;i<4;i++)RescueProps.hide(p,s,"RatDecoy"+i);for(int i=0;i<3;i++)RescueProps.hide(p,s,"RatmasterBall"+i);RescueEncounters.startChase''')
s=s.replace('''        var candidates=RatmasterRegion.carriers(center,n.getDouble("AttackAngle"));''','''        double attackAngle=n.getDouble("AttackAngle");
        var forward=new Vec3(Math.cos(attackAngle),0,Math.sin(attackAngle));
        var lanes=RatmasterRegion.lanes(center,attackAngle,tier);
        for(int i=0;i<3;i++){
            String key="RatmasterBall"+i;
            if(attack!=2||beat>78||i>=lanes.size()){RescueProps.hide(p,s,key);continue;}
            var truck=RatmasterRegion.ballPosition(lanes.get(i),forward,beat);
            var ball=RescueEncounters.prop(p,s,key,RescueProps.RAT_BALL,0,truck);
            if(ball!=null){ball.setPos(truck);ball.frame(0,beat,1);}
        }
        var candidates=RatmasterRegion.carriers(center,attackAngle);''')
s=s.replace('var truck=RatmasterRegion.ballPosition','var ballAt=RatmasterRegion.ballPosition').replace('RescueProps.RAT_BALL,0,truck','RescueProps.RAT_BALL,0,ballAt').replace('ball.setPos(truck)','ball.setPos(ballAt)')
put('RescueBosses',s)
