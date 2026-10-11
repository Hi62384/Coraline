import com.coraline.feast.MansionPuzzle;
public class MansionPuzzleCheck {
    public static void main(String[] args){
        long start=System.nanoTime();
        for(int seed=0;seed<10000;seed++){
            var p=MansionPuzzle.generate(seed);
            if(p.solve()!=(1<<p.jackpot()))throw new AssertionError("Nonunique: "+seed);
            for(var clue:p.clues()){
                int count=Integer.bitCount(clue.mask());
                if(count<9||count>10)throw new AssertionError("Unbalanced clue: "+seed);
                if(clue.speaker()==1&&(clue.text().contains("NOT")||clue.text().contains("(1)")||clue.text().contains("BOTH")))throw new AssertionError("Compound/negative lie");
                int decoded=0;for(var room:p.rooms())if(describes(clue,room,p))decoded|=1<<room.id();
                if(decoded!=clue.mask())throw new AssertionError("Written clue contradicts solver: "+clue.text());
            }
            for(int omit=0;omit<6;omit++){
                int mask=4095;for(int i=0;i<6;i++)if(i!=omit)mask&=p.clues().get(i).mask();
                if(Integer.bitCount(mask)<2)throw new AssertionError("Redundant witness: "+seed);
            }
        }
        System.out.println("10,000 mansion puzzles: unique solution, balanced clues, all six essential; "+(System.nanoTime()-start)/1e9+"s");
    }
    static boolean describes(MansionPuzzle.Clue clue,MansionPuzzle.Room r,MansionPuzzle.Puzzle p){
        String text=clue.text();
        if(clue.speaker()==1)return !atom(text.substring("The jackpot room ".length(),text.length()-1),r,p);
        if(clue.speaker()==2){String[] parts=text.substring(text.indexOf("(1) It ")+7,text.length()-1).split("\\. \\(2\\) It ");return atom(parts[0],r,p)^atom(parts[1],r,p);}
        if(text.startsWith("Rule out every room that "))return !atom(text.substring(25,text.indexOf(". None")),r,p);
        String[] parts=text.substring("Rule out any room that ".length(),text.length()-1).split(" but does NOT meet this condition: it ");
        return !atom(parts[0],r,p)||atom(parts[1],r,p);
    }
    static boolean atom(String text,MansionPuzzle.Room r,MansionPuzzle.Puzzle p){
        if(text.startsWith("has ")){
            if(text.endsWith(" carpet"))return text.equals("has "+MansionPuzzle.COLORS[r.carpet()]+" carpet");
            return text.equals("has "+new String[]{"gold","emerald","amethyst"}[r.emblem()]+" door trim");
        }
        var anchor=p.rooms().stream().filter(a->text.endsWith("the "+a.name())||text.endsWith("the "+a.name()+" is")).findFirst().orElseThrow();
        if(text.startsWith("is on the same floor"))return r.floor()==anchor.floor();
        if(text.startsWith("is in the same wing"))return r.wing()==anchor.wing();
        if(text.startsWith("is next door"))return r.floor()==anchor.floor()&&r.wing()==anchor.wing()&&Math.abs(r.row()-anchor.row())==1;
        if(text.startsWith("is closer"))return r.row()>anchor.row();
        if(text.startsWith("is directly"))return r.row()==anchor.row()&&r.wing()==anchor.wing()&&r.floor()!=anchor.floor();
        throw new AssertionError("Unknown atom: "+text);
    }
}
