package com.coraline.feast;

import java.util.*;
import java.util.function.*;

/** Pure weighted selection; readiness is checked before replacing a live event. */
public final class VisitEventSelection {
    /** Keep the activity cadence, but rotate eligible ordinary events oldest-first.
     * The caller persists the last-selected sequence so reconnects cannot starve a type. */
    public static <T> T chooseFair(Collection<T> order,Predicate<T> permitted,T previous,T activity,ToLongFunction<T> last,IntUnaryOperator random){
        var draw=chooseFortySixty(order,permitted,previous,activity,random);
        if(Objects.equals(draw,activity))return draw;
        var choices=new ArrayList<T>();long oldest=Long.MAX_VALUE;
        for(T value:order){if(Objects.equals(value,activity)||Objects.equals(value,previous)||!permitted.test(value))continue;
            long age=last.applyAsLong(value);if(age<oldest){oldest=age;choices.clear();}if(age==oldest)choices.add(value);
        }
        return choices.isEmpty()?draw:choices.get(random.applyAsInt(choices.size()));
    }
    /** Non-repeating Markov selection: 40% activity / equal 60% others in steady state. */
    public static <T> T chooseFortySixty(Collection<T> order,Predicate<T> permitted,T previous,T activity,IntUnaryOperator random){
        var others=new ArrayList<T>();for(T value:order)if(!Objects.equals(value,activity)&&permitted.test(value))others.add(value);
        boolean portal=permitted.test(activity);
        if(others.isEmpty())return activity;
        if(Objects.equals(previous,activity))return others.get(random.applyAsInt(others.size()));
        boolean previousOther=others.remove(previous);
        if(others.isEmpty())return activity;
        // An activity must be followed by a non-activity. Compensate on the next draw:
        // .4 = (1-.4) * 2/3. Ordinary types remain symmetric and never repeat either.
        if(portal&&(previousOther?random.applyAsInt(3)<2:random.applyAsInt(100)<40))return activity;
        return others.get(random.applyAsInt(others.size()));
    }
    public static <T> T choose(Collection<T> order,ToIntFunction<T> weight,Predicate<T> permitted,
                               T previous,T fallback,IntUnaryOperator random){
        var candidates=new ArrayList<T>();var weights=new ArrayList<Integer>();
        for(T value:order){int w=weight.applyAsInt(value);if(w>0&&permitted.test(value)){candidates.add(value);weights.add(w);}}
        if(candidates.size()>1&&previous!=null){int index=candidates.indexOf(previous);if(index>=0){candidates.remove(index);weights.remove(index);}}
        if(candidates.isEmpty())return fallback;
        long total=0;for(int w:weights)total+=w;
        if(total>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid event weight total");
        int selected=random.applyAsInt((int)total);
        if(selected<0||selected>=total)throw new IllegalArgumentException("Random event draw outside total");
        for(int i=0;i<candidates.size();i++){selected-=weights.get(i);if(selected<0)return candidates.get(i);}
        throw new IllegalStateException("No event for weighted draw");
    }
    private VisitEventSelection(){}
}
