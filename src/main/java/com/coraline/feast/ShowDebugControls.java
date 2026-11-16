package com.coraline.feast;

import java.util.List;

/** Operator-only debugger sections. Offset jumps are advertised as blocked until native seek verification. */
public final class ShowDebugControls {
    public record Control(String id,String label,boolean requiresVerifiedSeek) {}
    private static Control c(String id,String label){return new Control(id,label,false);}
    private static Control seek(String id,String label){return new Control(id,label,true);}
    public static List<Control> theatre(){var controls=new java.util.ArrayList<>(List.of(
        c("force","Force Theatre event"),c("state","State, eligibility and completion"),c("reset","Reset completion"),
        c("on_time","Force on-time arrival"),c("late","Force late arrival"),c("missed","Start missed-attendance search"),
        c("entrance","Go to real basement entrance"),c("enter","Enter Theatre"),c("seat","Go to reserved player seat"),
        c("wybie_present","Request eligible Wybie"),c("wybie_absent","Return and release Wybie"),c("audience","Fill button-dog audience"),
        c("act_one","Start Act One"),seek("act_one_end","Jump to Act One ending"),c("act_two","Start Act Two"),c("a","Play A.wav"),c("b","Play B.wav"),
        c("spink_transform","Preview Spink transformation"),c("forcible_transform","Preview Forcible transformation"),
        c("freeze","Preview D + 2.5 second freeze"),c("trapeze","Start E trapeze routine"),c("grab","Preview final-second player grab"),
        c("swing","Start F swinging sequence"),seek("throw","Jump to F + 24 second throw"),c("rescue","Preview pool rescue"),
        c("applause","Preview final applause"),c("rose","Test the unique rose throw"),c("open_exit","Open Theatre exit for testing"),
        c("return","Return to basement"),c("parents_audio","Play MeetParentsAfterEnd.wav"),c("escort","Test parent escort"),
        c("complete","Complete when required checkpoints and reward are valid"),c("recover","Recover the current safe checkpoint")));
        for(char cue='A';cue<='N';cue++)controls.add(c("opening_"+Character.toLowerCase(cue),"Audition opening "+cue+" (no progression)"));
        return List.copyOf(controls);
    }
    public static List<Control> circus(){return List.of(
        c("force","Force Bobinsky Mouse Circus"),c("state","State, eligibility and completion"),c("reset","Reset completion"),c("invitation","Play original invitation"),
        c("wybie_present","Request eligible Wybie"),c("wybie_absent","Return and release Wybie"),c("attic","Go to actual attic entrance"),c("attic_props","Generate miniature circus"),
        c("cotton_candy","Trigger cotton candy cannon"),c("popcorn","Test popcorn Ferris wheel"),c("enter","Enter giant circus"),c("audience","Go to audience position"),
        c("introduction","Start CircusIntroduction"),seek("intro_reveal","Jump to final three introduction seconds"),c("formation","Preview mouse symbol formation"),
        c("music","Start original main music"),seek("act_1","Jump to parade"),seek("act_2","Jump to acrobatics"),seek("act_3","Jump to balance act"),seek("act_4","Jump to aerial act"),seek("act_5","Jump to finale"),
        c("final_formation","Preview final formation"),seek("music_end","Jump to main music end"),c("bobinsky","Place original Bobinsky at center"),c("approach","Test Bobinsky approach"),
        c("ending","Play CircusEnd"),c("open_exit","Open circus exit for testing"),c("return","Return to attic"),c("departure","Test Wybie offscreen departure"),
        c("complete","Complete when required checkpoints are valid"),c("recover","Test reconnect recovery"));}
    public static boolean needsSeek(boolean theatre,String command){return (theatre?theatre():circus()).stream().anyMatch(c->c.id.equals(command)&&c.requiresVerifiedSeek);}
    public static final String SEEK_BLOCKER=ShowSeekAvailability.blocker();
    /** -1 means the selected jump does not apply to the currently bound genuine clip. */
    public static int seekTick(boolean theatre,String command,String sound,int duration){
        if(duration<=0)return -1;
        int target=-1;
        if(theatre){
            if(command.equals("act_one_end")&&sound.equals("theatre_opening_n"))target=Math.max(0,duration-20);
            if(command.equals("throw")&&sound.equals("theatre_f"))target=480;
        }else{
            if(command.equals("intro_reveal")&&sound.equals("mouse_circus_intro"))target=duration-60;
            if(sound.equals("mouse_circus_music")){
                if(command.equals("music_end"))target=Math.max(0,duration-20);
                else if(command.matches("act_[1-5]"))target=(int)((long)duration*(command.charAt(4)-'1')/5);
            }
        }
        return target>=0&&target<duration?target:-1;
    }

    private ShowDebugControls(){}
}
