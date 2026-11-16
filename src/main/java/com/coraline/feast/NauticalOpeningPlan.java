package com.coraline.feast;

import java.util.*;

/** Recording-driven opening machinery math. Not yet connected to the stage;
 * the established diving A-F controller is deliberately untouched.
 */
public final class NauticalOpeningPlan {
    public static final int SEATED_DELAY=60;
    public static final String ENTRANCE="theatre_entrance";
    public record Cue(char letter,String sound,int duration){}
    public record Frame(double curtain,double boatScene,double fishHeadLift,double spinkLift,double clamLift,
                        double forcibleYaw,double spinkSway,double forcibleSway,boolean finalForciblePose,
                        boolean bothOnstage,double collapse){}
    public record Push(double spinkX,double forcibleX,float spinkYaw,float forcibleYaw){}
    /** The incoming performer stays behind the outgoing performer in the
     * direction of travel; hands meet before the shared push starts. */
    public static Push push(char cue,double fraction){
        if(cue<'G'||cue>'L'||!Double.isFinite(fraction))throw new IllegalArgumentException("Invalid alternating push");
        double u=Math.max(0,Math.min(1,fraction)),sign=(cue-'G')%2==0?-1:1;
        double enter=smooth(u/.22),push=smooth((u-.22)/.48),settle=smooth((u-.70)/.30);
        double incoming=sign*(13*(1-enter)+(1.1-13*push)*enter*(1-settle)),outgoing=-sign*13*push;
        float incomingYaw=(float)(sign*90*(1-settle)),outgoingYaw=(float)(-sign*90*push);
        return sign<0?new Push(incoming,outgoing,incomingYaw,outgoingYaw):new Push(outgoing,incoming,outgoingYaw,incomingYaw);
    }
    private final List<Cue> cues;
    public NauticalOpeningPlan(Map<String,Integer> measured){
        var built=new ArrayList<Cue>();for(char cue='A';cue<='N';cue++){
            String sound=sound(cue);Integer length=measured.get(sound);
            if(length==null||length<=0)throw new IllegalArgumentException("Missing original opening recording: "+sound);
            if((cue=='E'||cue=='F')&&length<=40)throw new IllegalArgumentException("Opening "+cue+" does not leave time before the required end-minus-two-second hold");
            built.add(new Cue(cue,sound,length));
        }cues=List.copyOf(built);
    }
    public static String sound(char cue){if(cue<'A'||cue>'N')throw new IllegalArgumentException("Unknown opening cue");return "theatre_opening_"+Character.toLowerCase(cue);}
    public List<Cue> cues(){return cues;}
    public Frame frame(char letter,double recordingTick){
        if(!Double.isFinite(recordingTick))throw new IllegalArgumentException("Invalid heard-recording cursor");
        var cue=cues.get(letterIndex(letter));double t=Math.max(0,Math.min(cue.duration,recordingTick)),u=t/cue.duration;
        double curtain=letter=='A'?smooth(u):letter=='N'?1-smooth(u):1;
        double boat=letter<='D'?1:0,head=letter<'C'?0:8;
        double spink=letter<='D'?0:-9,clam=letter<'E'?-9:0,yaw=letter<='E'?180:0;
        double swayS=0,swayF=0,collapse=0;boolean hold=false,both=false;
        if(letter=='C')head=8*smooth(u);
        if(letter=='D')swayS=Math.sin(u*Math.PI*4)*(.08+.16*u)*Math.sin(u*Math.PI);
        if(letter=='E'){double change=smooth(t/(cue.duration-40.0));boat=1-change;spink=-9*change;clam=-9*(1-change);}
        if(letter=='F'){
            double active=Math.min(t,cue.duration-40),turn=smooth(active/Math.min(30,cue.duration-40));yaw=180*(1-turn);
            hold=t>=cue.duration-40;swayF=hold?0:Math.sin(active*.07)*.13*Math.sin(Math.PI*active/(cue.duration-40));
        }
        if(letter>='G'&&letter<='L'){
            boolean spinkTakes=(letter-'G')%2==0;double change=smooth(u);
            boat=spinkTakes?change:1-change;spink=-9*(1-boat);clam=-9*boat;
            swayS=spinkTakes?-.15*Math.sin(u*Math.PI):.22*Math.sin(u*Math.PI);
            swayF=spinkTakes?.22*Math.sin(u*Math.PI):-.15*Math.sin(u*Math.PI);
        }
        if(letter=='M'){
            both=true;spink=-9*(1-smooth(u/.20));clam=0;
            boat=.5-.5*Math.cos(Math.PI*2*(3*u+3*u*u));collapse=smooth((u-.25)/.75);
            swayS=Math.sin(t*.31)*.25*collapse;swayF=-Math.sin(t*.27)*.25*collapse;
        }
        if(letter=='N'){both=true;spink=clam=0;boat=0;collapse=1;}
        return new Frame(curtain,boat,head,spink,clam,yaw,swayS,swayF,hold,both,collapse);
    }
    private static int letterIndex(char letter){sound(letter);return letter-'A';}
    private static double smooth(double value){double t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}
}
