package com.coraline.otherworld;

import static com.coraline.otherworld.TunnelPassageTransition.Decision.*;

public final class TunnelPassageTransitionCheck {
    private static int checks;
    private static void check(TunnelPassageTransition.Decision expected,boolean restoring,boolean available,
            boolean original,boolean placement,boolean safe){
        var actual=TunnelPassageTransition.decide(restoring,available,original,placement,safe);
        if(expected!=actual)throw new AssertionError(expected+" != "+actual);
        checks++;
    }

    public static void main(String[] args){
        check(WRITE,false,true,true,false,true);
        check(WAIT,false,true,true,false,false);
        check(WAIT,false,true,false,false,true);
        check(WAIT,false,false,false,true,true);
        check(VERIFIED,false,true,false,true,false);
        check(WRITE,true,true,false,true,true);
        check(WAIT,true,true,false,true,false);
        check(WAIT,true,false,false,false,true);
        check(VERIFIED,true,true,true,false,false);
        check(VERIFIED,true,true,false,false,false);
        check(VERIFIED,true,true,true,true,true);
        System.out.println("TUNNEL_PASSAGE_TRANSITIONS_OK "+checks);
    }
}
