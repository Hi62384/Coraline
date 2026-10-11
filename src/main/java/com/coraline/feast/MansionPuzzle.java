package com.coraline.feast;

import java.util.*;

/** Pure, deterministic puzzle generator. Masks are server-only; the journal never receives them. */
public final class MansionPuzzle {
    public static final String[] NAMES={"Library","Vex Room","Map Room","Axe Room","Cat Room","Chicken Room","Dining Room","Bedroom","Storage Room","Jail Room","Evoker Study","Trophy Room"};
    public static final String[] COLORS={"red","blue","green","purple"};
    public record Room(int id,int identity,int carpet,int emblem){public int floor(){return id/6;}public int wing(){return id%6/3;}public int row(){return id%3;}public String name(){return NAMES[identity];}}
    public record Clue(int mask,int speaker,String text){}
    public record Puzzle(List<Room> rooms,List<Clue> clues,int jackpot){public int solve(){int mask=4095;for(var c:clues)mask&=c.mask;return mask;}}
    private record Atom(int mask,String text){}
    static int mask(List<Room> rooms,java.util.function.Predicate<Room> test){int m=0;for(var r:rooms)if(test.test(r))m|=1<<r.id;return m;}
    public static boolean adjacent(Room a,Room b){return a.floor()==b.floor()&&a.wing()==b.wing()&&Math.abs(a.row()-b.row())==1;}
    public static Puzzle generate(long seed){var random=new Random(seed);
        for(int generation=0;generation<100;generation++){
            var names=new ArrayList<Integer>();for(int i=0;i<12;i++)names.add(i);Collections.shuffle(names,random);var rooms=new ArrayList<Room>();for(int i=0;i<12;i++)rooms.add(new Room(i,names.get(i),random.nextInt(4),random.nextInt(3)));
            int target=random.nextInt(12);var atoms=new ArrayList<Atom>();
            for(int color=0;color<4;color++){int k=color;atoms.add(new Atom(mask(rooms,r->r.carpet==k),"has "+COLORS[color]+" carpet"));}
            for(int emblem=0;emblem<3;emblem++){int k=emblem;atoms.add(new Atom(mask(rooms,r->r.emblem==k),"has "+(k==0?"gold":k==1?"emerald":"amethyst")+" door trim"));}
            for(var anchor:rooms){atoms.add(new Atom(mask(rooms,r->r.floor()==anchor.floor()),"is on the same floor as the "+anchor.name()));atoms.add(new Atom(mask(rooms,r->r.wing()==anchor.wing()),"is in the same wing as the "+anchor.name()));atoms.add(new Atom(mask(rooms,r->adjacent(r,anchor)),"is next door to the "+anchor.name()));atoms.add(new Atom(mask(rooms,r->r.row()>anchor.row()),"is closer to the staircase end of its corridor than the "+anchor.name()+" is"));atoms.add(new Atom(mask(rooms,r->r.row()==anchor.row()&&r.floor()!=anchor.floor()&&r.wing()==anchor.wing()),"is directly above or below the "+anchor.name()));}
            var pool=new ArrayList<Clue>();var seen=new HashSet<String>();
            // A liar gets ONE affirmative statement, never a conjunction whose
            // negation players would reasonably read as two separate inversions.
            for(var a:atoms){int allowed=(~a.mask)&4095;
                if((allowed&(1<<target))!=0&&Integer.bitCount(allowed)>=9&&Integer.bitCount(allowed)<=10){
                    if(seen.add("l"+allowed))pool.add(new Clue(allowed,1,"The jackpot room "+a.text+"."));
                    if(seen.add("t"+allowed))pool.add(new Clue(allowed,0,"Rule out every room that "+a.text+". None of those rooms contains the jackpot."));
                }
            }
            for(var a:atoms)for(var b:atoms){int implication=(~a.mask|b.mask)&4095;int xor=a.mask^b.mask;
                if((implication&(1<<target))!=0&&Integer.bitCount(implication)>=9&&Integer.bitCount(implication)<=10){
                    if(seen.add("t"+implication))pool.add(new Clue(implication,0,"Rule out any room that "+a.text+" but does NOT meet this condition: it "+b.text+"."));
                }
                if((xor&(1<<target))!=0&&Integer.bitCount(xor)>=9&&Integer.bitCount(xor)<=10&&seen.add("x"+xor))pool.add(new Clue(xor,2,"Exactly ONE claim describes the jackpot room, not both: (1) It "+a.text+". (2) It "+b.text+"."));
            }
            for(int attempt=0;attempt<1500;attempt++){Collections.shuffle(pool,random);var chosen=new ArrayList<Clue>();int remaining=4095;
                for(var clue:pool){if(chosen.size()==6)break;if((remaining&clue.mask)==remaining)continue;chosen.add(clue);remaining&=clue.mask;}
                if(chosen.size()!=6||remaining!=(1<<target)||chosen.stream().noneMatch(c->c.speaker==0)||chosen.stream().noneMatch(c->c.speaker==1)||chosen.stream().noneMatch(c->c.speaker==2))continue;
                // Every witness must contribute: any five clues still leave
                // more than one candidate, while all six identify the jackpot.
                boolean easy=false;for(int omitted=0;omitted<6;omitted++){int m=4095;for(int i=0;i<6;i++)if(i!=omitted)m&=chosen.get(i).mask;if(Integer.bitCount(m)==1){easy=true;break;}}
                if(!easy)return new Puzzle(List.copyOf(rooms),List.copyOf(chosen),target);
            }
        }throw new IllegalStateException("Could not construct a fair mansion puzzle");
    }
    private MansionPuzzle(){}
}
