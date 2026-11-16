package com.coraline.feast;

import java.util.*;

/** Shared deterministic chart; submissions are still checked against the server's round. */
public final class ConstellationPuzzle {
    public static final int ROUNDS=6,STARS=16;
    public static int count(int round){return 3+Math.min(2,Math.max(0,round)/2);}
    public static int[][] stars(long seed,int round){var random=new Random(seed+round*7919L);var result=new int[STARS][2];for(int i=0;i<STARS;i++){result[i][0]=-90+(i%4)*55+random.nextInt(13);result[i][1]=-52+(i/4)*33+random.nextInt(9);}return result;}
    public static Set<Integer> targets(long seed,int round){var ids=new ArrayList<Integer>();for(int i=0;i<STARS;i++)ids.add(i);Collections.shuffle(ids,new Random(seed^((round+1)*104729L)));return new TreeSet<>(ids.subList(0,count(round)));}
    public static boolean matches(long seed,int round,Collection<Integer> selection){return round>=0&&round<ROUNDS&&selection.size()==count(round)&&new HashSet<>(selection).size()==selection.size()&&targets(seed,round).equals(new HashSet<>(selection));}
    private ConstellationPuzzle(){}
}
