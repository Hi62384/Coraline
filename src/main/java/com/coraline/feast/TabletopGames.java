package com.coraline.feast;

import net.minecraft.nbt.*;
import java.util.*;

/** Durable, server-authoritative rules. Rendering never chooses winners or NPC moves. */
public final class TabletopGames {
    public enum Game { BUTTON_CARDS,THREAD_FOUR,WOBBLY_TOWER,MOUSEBOARD_DASH,BUTTON_FLICK,SILENT_CHARADES }
    public static String voice(Game game){return switch(game){case BUTTON_CARDS->"cards";case THREAD_FOUR->"thread";case WOBBLY_TOWER->"tower";case MOUSEBOARD_DASH->"mouseboard";case BUTTON_FLICK->"flick";case SILENT_CHARADES->"charades";};}
    public static Game game(CompoundTag n){return Game.valueOf(n.getString("Game"));}
    static Random random(CompoundTag n){return new Random(n.getLong("Seed")+n.getInt("Moves")*0x9e3779b9L+n.getInt("DrawCursor"));}
    /** Seat zero is the storyline owner; all other seats are actual NPCs supplied by the controller. */
    public record Participant(String id,String name,int team) {
        public Participant {
            if(id==null||id.isBlank()||id.length()>128||name==null||name.isBlank()||name.length()>80
                    ||id.chars().anyMatch(Character::isISOControl)||name.chars().anyMatch(Character::isISOControl)
                    ||team< -1||team>1)throw new IllegalArgumentException("Invalid tabletop participant");
        }
    }
    private static final List<Participant> FAMILY=List.of(
            new Participant("owner","You",0),new Participant("mother","Other Mother",1),
            new Participant("father","Other Father",1),new Participant("wybie","Other Wybie",0));
    public static CompoundTag start(Game game,long seed){return start(game,seed,FAMILY,true);}
    /** Thread Four requires explicit teams 0/1, both present, with the owner on team zero. */
    public static CompoundTag start(Game game,long seed,List<Participant> participants){
        return start(game,seed,participants,false);
    }
    private static void validateRoster(Game game,List<Participant> participants){
        if(game==null||participants==null||participants.size()<2||participants.size()>4)
            throw new IllegalArgumentException("Tabletop games need two to four participants");
        Set<String> ids=new HashSet<>();boolean opposingTeam=false;
        for(var participant:participants){
            if(participant==null||!ids.add(participant.id()))throw new IllegalArgumentException("Duplicate or missing participant");
            if(game==Game.THREAD_FOUR&&participant.team()<0)throw new IllegalArgumentException("Thread Four needs explicit teams");
            if(participant.team()==1)opposingTeam=true;
        }
        if(game==Game.THREAD_FOUR&&(participants.get(0).team()!=0||!opposingTeam))
            throw new IllegalArgumentException("Thread Four needs an opposing team and owner on team zero");
    }
    /** Missing roster fields are supported only for historical four-person Family Game Night saves. */
    public static int participantCount(CompoundTag n){return n.contains("Participants")?n.getList("Participants",10).size():n.contains("ParticipantCount")?0:4;}
    private static boolean familyRoster(CompoundTag n){return !n.contains("Participants")&&!n.contains("ParticipantCount")||n.getBoolean("FamilyRoster");}
    public static String participantName(CompoundTag n,int who){
        if(who<0||who>=participantCount(n))return "A player";
        return n.contains("Participants")?n.getList("Participants",10).getCompound(who).getString("Name"):FAMILY.get(who).name();
    }
    public static int participantTeam(CompoundTag n,int who){
        if(who<0||who>=participantCount(n))return -1;
        return n.contains("Participants")?n.getList("Participants",10).getCompound(who).getInt("Team"):FAMILY.get(who).team();
    }
    public static String teamName(CompoundTag n,int team){
        if(familyRoster(n))return team==0?"You and Wybie":"Your Other Parents";
        var names=new ArrayList<String>();
        for(int i=0;i<participantCount(n);i++)if(participantTeam(n,i)==team)names.add(participantName(n,i));
        return String.join(" + ",names);
    }
    public static boolean validRoster(CompoundTag n){
        if(!n.contains("Participants"))return !n.contains("ParticipantCount");
        var participants=n.getList("Participants",10);
        if(participants.size()!=n.getInt("ParticipantCount"))return false;
        try{
            var roster=new ArrayList<Participant>();
            for(var tag:participants){var p=(CompoundTag)tag;roster.add(new Participant(p.getString("Id"),p.getString("Name"),p.getInt("Team")));}
            validateRoster(game(n),roster);
            if(n.getBoolean("FamilyRoster")&&!roster.equals(FAMILY))return false;
            if(game(n)==Game.THREAD_FOUR&&!familyRoster(n)){
                var last=n.getIntArray("TeamLast");if(last.length!=2)return false;
                for(int team=0;team<2;team++)if(last[team]< -1||last[team]>=roster.size()
                        ||last[team]>=0&&roster.get(last[team]).team()!=team)return false;
            }
            return true;
        }catch(IllegalArgumentException bad){return false;}
    }
    private static CompoundTag start(Game game,long seed,List<Participant> participants,boolean family){
        validateRoster(game,participants);int count=participants.size();var n=new CompoundTag();
        var roster=new ListTag();for(var participant:participants){var p=new CompoundTag();p.putString("Id",participant.id());p.putString("Name",participant.name());p.putInt("Team",participant.team());roster.add(p);}
        n.put("Participants",roster);n.putInt("ParticipantCount",count);n.putBoolean("FamilyRoster",family);n.putString("Game",game.name());n.putLong("Seed",seed);n.putInt("Winner",-1);n.putIntArray("Scores",new int[count]);n.putInt("Turn",0);n.putInt("TurnTicks",0);var rng=new Random(seed);
        switch(game){
            case BUTTON_CARDS->{var deck=new ArrayList<Integer>();for(int repeat=0;repeat<2;repeat++)for(int color=0;color<4;color++)for(int symbol=0;symbol<8;symbol++)deck.add(color*8+symbol);Collections.shuffle(deck,rng);n.putIntArray("Deck",deck.stream().mapToInt(Integer::intValue).toArray());for(int i=0;i<count;i++)n.putIntArray("Hand"+i,deck.subList(i*5,i*5+5).stream().mapToInt(Integer::intValue).toArray());n.putInt("Top",deck.get(count*5));n.putInt("DrawCursor",count*5+1);n.putInt("Color",n.getInt("Top")/8);n.putInt("Direction",1);}
            case THREAD_FOUR->{int[] board=new int[42];Arrays.fill(board,-1);n.putIntArray("Board",board);if(!family)n.putIntArray("TeamLast",new int[]{0,-1});}
            case WOBBLY_TOWER->{int[] rows=new int[12];Arrays.fill(rows,7);n.putIntArray("Tower",rows);n.putDouble("Stability",1);}
            case MOUSEBOARD_DASH->{n.putIntArray("Mouse",new int[count]);n.putInt("Die",1);}
            case BUTTON_FLICK->n.put("Pucks",new ListTag());
            case SILENT_CHARADES->{n.putInt("Round",0);n.putInt("Actor",count-1);n.putInt("Topic",rng.nextInt(6));}
        }return n;
    }
    public static boolean legalCard(int card,int top,int color){return card>=0&&card<32&&(card%8==7||card/8==color||card%8==top%8);}
    static void advance(CompoundTag n,int skip){
        int turn=n.getInt("Turn"),count=participantCount(n);
        if(game(n)==Game.THREAD_FOUR){
            if(familyRoster(n)){int[] order={0,1,3,2};int i=0;while(order[i]!=turn)i++;turn=order[(i+1)%4];}
            else{
                int team=participantTeam(n,turn),nextTeam=1-team;var last=n.getIntArray("TeamLast");
                last[team]=turn;
                for(int step=1;step<=count;step++){int next=Math.floorMod(last[nextTeam]+step,count);if(participantTeam(n,next)==nextTeam){turn=next;break;}}
                n.putIntArray("TeamLast",last);
            }
        }else if(game(n)==Game.BUTTON_CARDS)turn=Math.floorMod(turn+n.getInt("Direction")*(skip+1),count);
        else turn=(turn+1)%count;
        n.putInt("Turn",turn);n.putInt("Moves",n.getInt("Moves")+1);n.putInt("TurnTicks",0);
    }
    /** Cancellation is terminal, idempotent, and must never qualify for wins or completion rewards. */
    public static void cancel(CompoundTag n){if(!n.getBoolean("Done")){n.putBoolean("Cancelled",true);n.putInt("Physics",0);finish(n,-1);}}
    static void finish(CompoundTag n,int winner){n.putBoolean("Done",true);n.putInt("Winner",winner);}
    static int draw(CompoundTag n){var deck=n.getIntArray("Deck");int cursor=n.getInt("DrawCursor");n.putInt("DrawCursor",cursor+1);return cursor<deck.length?deck[cursor]:random(n).nextInt(32);}
    static void addCard(CompoundTag n,int who,int card){var hand=n.getIntArray("Hand"+who);var result=Arrays.copyOf(hand,hand.length+1);result[hand.length]=card;n.putIntArray("Hand"+who,result);}
    static boolean cards(CompoundTag n,String action){int turn=n.getInt("Turn");var hand=n.getIntArray("Hand"+turn);if(action.equals("draw")){if(Arrays.stream(hand).anyMatch(c->legalCard(c,n.getInt("Top"),n.getInt("Color"))))return false;addCard(n,turn,draw(n));advance(n,0);return true;}
        var parts=action.split(":");if(parts.length!=3||!parts[0].equals("card"))return false;int index=Integer.parseInt(parts[1]),chosen=Integer.parseInt(parts[2]);if(index<0||index>=hand.length||chosen<0||chosen>3)return false;int card=hand[index];if(!legalCard(card,n.getInt("Top"),n.getInt("Color")))return false;int[] out=new int[hand.length-1];for(int i=0,j=0;i<hand.length;i++)if(i!=index)out[j++]=hand[i];n.putIntArray("Hand"+turn,out);n.putInt("Top",card);n.putInt("Color",card%8==7?chosen:card/8);int skip=0;if(card%8==4)skip=1;if(card%8==5)n.putInt("Direction",-n.getInt("Direction"));if(card%8==6)addCard(n,Math.floorMod(turn+n.getInt("Direction"),participantCount(n)),draw(n));if(out.length==0)finish(n,turn);advance(n,skip);return true;
    }
    public static int fourWinner(int[] board){for(int y=0;y<6;y++)for(int x=0;x<7;x++){int team=board[y*7+x];if(team<0)continue;for(int[] dir:new int[][]{{1,0},{0,1},{1,1},{1,-1}}){boolean all=true;for(int step=1;step<4;step++){int xx=x+dir[0]*step,yy=y+dir[1]*step;if(xx<0||xx>=7||yy<0||yy>=6||board[yy*7+xx]!=team){all=false;break;}}if(all)return team;}}return -1;}
    static int dropRow(int[] board,int col){for(int y=0;y<6;y++)if(board[y*7+col]<0)return y;return -1;}
    static boolean thread(CompoundTag n,String action){if(!action.startsWith("column:"))return false;int col=Integer.parseInt(action.substring(7));if(col<0||col>=7)return false;var board=n.getIntArray("Board");int row=dropRow(board,col);if(row<0)return false;int turn=n.getInt("Turn"),team=participantTeam(n,turn);board[row*7+col]=team;n.putIntArray("Board",board);n.putInt("LastColumn",col);n.putInt("LastRow",row);if(fourWinner(board)>=0)finish(n,team==0?0:1);else if(Arrays.stream(board).noneMatch(v->v<0))finish(n,-1);advance(n,0);return true;}
    public static double towerCost(int[] rows,int row,int piece,double precision){if(row<0||row>=rows.length-2||piece<0||piece>2||(rows[row]&(1<<piece))==0)return Double.POSITIVE_INFINITY;int remain=Integer.bitCount(rows[row]);return (remain==1?1:remain==2?.16:.035)+(piece==1?.06:.015)+(rows.length-row)*.002+(1-Math.max(0,Math.min(1,precision)))*.22;}
    static boolean tower(CompoundTag n,String action){var parts=action.split(":");if(parts.length!=4||!parts[0].equals("pull"))return false;int row=Integer.parseInt(parts[1]),piece=Integer.parseInt(parts[2]);double precision=Double.parseDouble(parts[3]);if(!Double.isFinite(precision))return false;var rows=n.getIntArray("Tower");double cost=towerCost(rows,row,piece,precision);if(!Double.isFinite(cost))return false;rows[row]&=~(1<<piece);n.putIntArray("Tower",rows);double stability=n.getDouble("Stability")-cost;n.putDouble("Stability",stability);n.putInt("LastRow",row);n.putInt("LastPiece",piece);if(stability<=0){n.putInt("Loser",n.getInt("Turn"));finish(n,(n.getInt("Turn")+1)%participantCount(n));}advance(n,0);return true;}
    static boolean mouse(CompoundTag n,String action){if(!action.equals("roll:safe")&&!action.equals("roll:risky"))return false;var rng=random(n);int die=1+rng.nextInt(4);n.putInt("Die",die);int turn=n.getInt("Turn");var mice=n.getIntArray("Mouse");int from=mice[turn],to=from+die;boolean fork=from<=7&&to>=7||from<=16&&to>=16;if(fork&&action.equals("roll:risky"))to+=rng.nextDouble()<.65?4:-3;if(to%7==3)to+=2;if(to%9==5)to-=2;if(to==20){int other=(turn+1)%participantCount(n);int saved=mice[other];mice[other]=to;to=saved;}mice[turn]=Math.max(0,to);n.putIntArray("Mouse",mice);n.putInt("LastFrom",from);n.putInt("LastTo",mice[turn]);n.putInt("LastActor",turn);if(mice[turn]>=30)finish(n,turn);advance(n,0);return true;}
    static boolean flick(CompoundTag n,String action){var parts=action.split(":");if(parts.length!=3||!parts[0].equals("flick")||n.getInt("Physics")>0)return false;double angle=Double.parseDouble(parts[1]),power=Double.parseDouble(parts[2]);if(!Double.isFinite(angle)||!Double.isFinite(power)||Math.abs(angle)>70||power<0||power>1)return false;var puck=new CompoundTag();puck.putInt("Owner",n.getInt("Turn"));puck.putDouble("X",0);puck.putDouble("Z",-.85);puck.putDouble("VX",Math.sin(Math.toRadians(angle))*(.015+power*.1));puck.putDouble("VZ",Math.cos(Math.toRadians(angle))*(.015+power*.1));n.getList("Pucks",10).add(puck);n.putInt("Physics",100);return true;}
    public static int puckScore(double x,double z){double distance=Math.hypot(x,z-.35);return distance<.08?5:distance<.2?3:distance<.38?2:distance<.6?1:0;}
    static void physics(CompoundTag n){var pucks=n.getList("Pucks",10);boolean moving=false;for(var tag:pucks){var p=(CompoundTag)tag;double x=p.getDouble("X"),z=p.getDouble("Z"),vx=p.getDouble("VX"),vz=p.getDouble("VZ");x+=vx;z+=vz;if(Math.abs(x)>.93){x=Math.copySign(.93,x);vx=-vx*.45;}if(Math.abs(z)>.93){z=Math.copySign(.93,z);vz=-vz*.45;}vx*=.965;vz*=.965;if(vx*vx+vz*vz<.000003){vx=0;vz=0;}else moving=true;p.putDouble("X",x);p.putDouble("Z",z);p.putDouble("VX",vx);p.putDouble("VZ",vz);}
        for(int i=0;i<pucks.size();i++)for(int j=i+1;j<pucks.size();j++){var a=pucks.getCompound(i);var b=pucks.getCompound(j);double dx=b.getDouble("X")-a.getDouble("X"),dz=b.getDouble("Z")-a.getDouble("Z"),distance=Math.hypot(dx,dz);if(distance<.10&&distance>.00001){double nx=dx/distance,nz=dz/distance,dot=(a.getDouble("VX")-b.getDouble("VX"))*nx+(a.getDouble("VZ")-b.getDouble("VZ"))*nz;if(dot>0){a.putDouble("VX",a.getDouble("VX")-dot*nx);a.putDouble("VZ",a.getDouble("VZ")-dot*nz);b.putDouble("VX",b.getDouble("VX")+dot*nx);b.putDouble("VZ",b.getDouble("VZ")+dot*nz);}a.putDouble("X",a.getDouble("X")-nx*(.10-distance)/2);a.putDouble("Z",a.getDouble("Z")-nz*(.10-distance)/2);b.putDouble("X",b.getDouble("X")+nx*(.10-distance)/2);b.putDouble("Z",b.getDouble("Z")+nz*(.10-distance)/2);}}
        n.putInt("Physics",n.getInt("Physics")-1);if(!moving||n.getInt("Physics")<=0){n.putInt("Physics",0);var scores=new int[participantCount(n)];for(var tag:pucks){var puck=(CompoundTag)tag;scores[puck.getInt("Owner")]+=puckScore(puck.getDouble("X"),puck.getDouble("Z"));puck.putDouble("VX",0);puck.putDouble("VZ",0);}n.putIntArray("Scores",scores);advance(n,0);if(n.getInt("Moves")>=participantCount(n)*3)finish(n,bestScore(scores));}}
    public static final String[] TOPICS={"Bird","Fishing","Clock","Sandwich","Cat","Boat"};
    static boolean charades(CompoundTag n,String action){int topic=n.getInt("Topic"),actor=n.getInt("Actor");var scores=n.getIntArray("Scores");if(actor==0){var parts=action.split(":");if(parts.length!=3||!parts[0].equals("clue"))return false;int prop=Integer.parseInt(parts[1]),gesture=Integer.parseInt(parts[2]);if(prop<0||prop>=6||gesture<0||gesture>=6)return false;n.putInt("ClueProp",prop);n.putInt("ClueGesture",gesture);int quality=(prop==topic?1:0)+(gesture==topic?1:0);if(quality>0)scores[0]+=quality;var rng=random(n);for(int who=1;who<participantCount(n);who++)if(quality==2||quality==1&&rng.nextDouble()<(who==1?.85:who==2?.55:.7))scores[who]++;}
        else{if(!action.startsWith("guess:"))return false;int guess=Integer.parseInt(action.substring(6));if(guess<0||guess>=6)return false;if(guess==topic){scores[0]++;scores[actor]++;}}
        n.putIntArray("Scores",scores);int round=n.getInt("Round")+1;n.putInt("Round",round);if(round>=(familyRoster(n)?6:participantCount(n)*2)){finish(n,bestScore(scores));return true;}int[] actors={3,1,0,2,3,0};n.putInt("Actor",familyRoster(n)?actors[round]:(round+participantCount(n)-1)%participantCount(n));n.putInt("Topic",random(n).nextInt(6));n.putInt("TurnTicks",0);n.putInt("Moves",n.getInt("Moves")+1);return true;}
    public static boolean move(CompoundTag n,String action){if(n.getBoolean("Done")||action==null)return false;if(!validRoster(n)){cancel(n);return false;}int actor=game(n)==Game.SILENT_CHARADES?n.getInt("Actor"):n.getInt("Turn");try{boolean accepted=switch(game(n)){case BUTTON_CARDS->cards(n,action);case THREAD_FOUR->thread(n,action);case WOBBLY_TOWER->tower(n,action);case MOUSEBOARD_DASH->mouse(n,action);case BUTTON_FLICK->flick(n,action);case SILENT_CHARADES->charades(n,action);};if(accepted){n.putInt("LastActor",actor);n.putInt("ActionCount",n.getInt("ActionCount")+1);}return accepted;}catch(NumberFormatException|IndexOutOfBoundsException bad){return false;}}
    static int best(int[] scores){int best=0;for(int i=1;i<scores.length;i++)if(scores[i]>scores[best])best=i;return best;}
    static int bestScore(int[] scores){int best=best(scores);for(int i=0;i<scores.length;i++)if(i!=best&&scores[i]==scores[best])return -1;return best;}
    public static String ai(CompoundTag n){int who=n.getInt("Turn");var rng=random(n);return switch(game(n)){
        case BUTTON_CARDS->{var hand=n.getIntArray("Hand"+who);var legal=new ArrayList<Integer>();for(int i=0;i<hand.length;i++)if(legalCard(hand[i],n.getInt("Top"),n.getInt("Color")))legal.add(i);if(legal.isEmpty())yield "draw";int index=legal.get(rng.nextInt(legal.size()));if(who==1)index=legal.stream().min(Comparator.comparingInt(i->hand[i]%8==7?10:hand[i]%8>=4?0:2)).orElse(index);int[] colors=new int[4];for(int c:hand)colors[c/8]++;yield "card:"+index+":"+best(colors);}
        case THREAD_FOUR->{var board=n.getIntArray("Board");int team=participantTeam(n,who);int chosen=-1;for(int test:new int[]{team,1-team}){for(int col=0;col<7;col++){int row=dropRow(board,col);if(row<0)continue;var copy=board.clone();copy[row*7+col]=test;if(fourWinner(copy)==test){chosen=col;break;}}if(chosen>=0)break;}if(chosen<0)for(int col:new int[]{3,2,4,1,5,0,6})if(dropRow(board,col)>=0){chosen=col;break;}if(who==2&&rng.nextDouble()<.18){var legal=new ArrayList<Integer>();for(int c=0;c<7;c++)if(dropRow(board,c)>=0)legal.add(c);if(!legal.isEmpty())chosen=legal.get(rng.nextInt(legal.size()));}yield "column:"+Math.max(0,chosen);}
        case WOBBLY_TOWER->{var rows=n.getIntArray("Tower");int row=0,piece=0;double cost=Double.MAX_VALUE;for(int r=0;r<10;r++)for(int c=0;c<3;c++){double v=towerCost(rows,r,c,.95);if(who==2)v+=rng.nextDouble()*.16;if(v<cost){cost=v;row=r;piece=c;}}yield "pull:"+row+":"+piece+":"+(who==1?.95:who==2?.72:.85);}
        case MOUSEBOARD_DASH->"roll:"+(who==1&&n.getIntArray("Mouse")[who]<Arrays.stream(n.getIntArray("Mouse")).max().orElse(0)||who==3&&rng.nextBoolean()?"risky":"safe");
        case BUTTON_FLICK->{double error=(who==1?2:who==2?13:7)*rng.nextGaussian();yield "flick:"+Math.max(-60,Math.min(60,error))+":"+Math.max(.1,Math.min(.9,.43+rng.nextGaussian()*(who==1?.04:.12)));}
        case SILENT_CHARADES->n.getInt("Actor")==0?"clue:0:0":"guess:"+rng.nextInt(6);
    };}
    public static int npcTurnDelay(CompoundTag n){return game(n)==Game.BUTTON_CARDS?12+Math.floorMod((int)(n.getLong("Seed")+n.getInt("Moves")*17L),13):40;}
    public static void tick(CompoundTag n){if(n.getBoolean("Done"))return;if(!validRoster(n)){cancel(n);return;}int elapsed=n.getInt("Elapsed")+1;n.putInt("Elapsed",elapsed);n.putInt("TurnTicks",n.getInt("TurnTicks")+1);if(n.getInt("Physics")>0)physics(n);else if(game(n)!=Game.SILENT_CHARADES&&n.getInt("Turn")!=0&&n.getInt("TurnTicks")>=npcTurnDelay(n))move(n,ai(n));else if(n.getInt("TurnTicks")>=400){if(game(n)==Game.SILENT_CHARADES)move(n,n.getInt("Actor")==0?"clue:5:0":"guess:5");else move(n,ai(n));}if(elapsed>=4200&&!n.getBoolean("Done")){n.putBoolean("TimedOut",true);finish(n,-1);}}
    /** A safety deadline must not fabricate a winner or qualify as completed play. */
    public static boolean completedNormally(CompoundTag n){return n.getBoolean("Done")&&!n.getBoolean("TimedOut")&&!n.getBoolean("Cancelled")&&validRoster(n);}
    public static boolean participantWon(CompoundTag n,int participant){
        if(!completedNormally(n)||participant<0||participant>=participantCount(n))return false;
        if(game(n)==Game.WOBBLY_TOWER)return n.contains("Loser")&&n.getInt("Loser")>=0&&n.getInt("Loser")<participantCount(n)&&n.getInt("Loser")!=participant;
        if(game(n)==Game.THREAD_FOUR)return n.getInt("Winner")==participantTeam(n,participant);
        return n.getInt("Winner")==participant;
    }
    public static String resultText(CompoundTag n){
        if(n.getBoolean("Cancelled"))return "Game cancelled. No winner.";
        if(n.getBoolean("TimedOut"))return "Time is up. No winner.";
        if(!n.getBoolean("Done"))return "Game in progress";
        if(game(n)==Game.WOBBLY_TOWER&&n.contains("Loser")){int loser=n.getInt("Loser");return participantName(n,loser)+" toppled the tower. The others win!";}
        int winner=n.getInt("Winner");
        if(winner<0||winner>=participantCount(n))return "A draw!";
        if(game(n)==Game.THREAD_FOUR)return teamName(n,winner)+" won!";
        return participantName(n,winner)+" won!";
    }
    /** Never disclose opponents' cards or the unspoken actor's secret word to the client. */
    public static CompoundTag visible(CompoundTag n){var copy=n.copy();copy.remove("Deck");copy.remove("Seed");for(String key:new HashSet<>(copy.getAllKeys()))if(key.startsWith("Hand")&&!key.equals("Hand0"))copy.remove(key);for(int i=1;i<participantCount(n);i++)copy.putInt("HandCount"+i,n.getIntArray("Hand"+i).length);if(game(n)==Game.SILENT_CHARADES&&n.getInt("Actor")!=0)copy.remove("Topic");return copy;}
    /** World props and spectators must never receive even the owner's hand or secret acting word. */
    public static CompoundTag spectatorVisible(CompoundTag n){
        var copy=visible(n);copy.putInt("HandCount0",n.getIntArray("Hand0").length);copy.remove("Hand0");copy.remove("Topic");return copy;
    }
    private TabletopGames(){}
}
