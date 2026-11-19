import com.coraline.palace.PalaceWellResumeRules;
import java.util.UUID;

/**
 * Engine-independent original-entity/session replay checks.
 * Run with javac -d /tmp/postboss .../PalaceWellResumeRules.java tools/PalaceWellResumeRulesCheck.java
 * and java -cp /tmp/postboss PalaceWellResumeRulesCheck
 */
public final class PalaceWellResumeRulesCheck {
    private static void check(boolean valid,String reason){if(!valid)throw new AssertionError(reason);}
    public static void main(String[] args){
        UUID owner=UUID.fromString("a1000000-0000-4000-8000-000000000001");
        UUID door=UUID.fromString("a2000000-0000-4000-8000-000000000002");
        UUID wybie=UUID.fromString("a3000000-0000-4000-8000-000000000003");
        UUID originalSession=UUID.fromString("a4000000-0000-4000-8000-000000000004");
        UUID resumed=UUID.fromString("a5000000-0000-4000-8000-000000000005");
        var original=new PalaceWellResumeRules.Bicycle(owner,door,wybie,originalSession,owner,true);
        check(PalaceWellResumeRules.canRebind(original,owner,door,wybie,resumed),
                "An original well bicycle was rejected after the session rotated.");
        check(!PalaceWellResumeRules.canRebind(original,UUID.randomUUID(),door,wybie,resumed),"Other owner accepted.");
        check(!PalaceWellResumeRules.canRebind(original,owner,UUID.randomUUID(),wybie,resumed),"Other door accepted.");
        check(!PalaceWellResumeRules.canRebind(original,owner,door,UUID.randomUUID(),resumed),"Other rider accepted.");
        check(!PalaceWellResumeRules.canRebind(
                new PalaceWellResumeRules.Bicycle(owner,door,wybie,originalSession,UUID.randomUUID(),true),
                owner,door,wybie,resumed),"Foreign well lease accepted.");
        check(!PalaceWellResumeRules.canRebind(
                new PalaceWellResumeRules.Bicycle(owner,door,wybie,originalSession,owner,false),
                owner,door,wybie,resumed),"Ordinary player bicycle accepted.");
        check(!PalaceWellResumeRules.canRebind(
                new PalaceWellResumeRules.Bicycle(owner,door,wybie,null,owner,true),
                owner,door,wybie,resumed),"Unbound bike accepted.");
        check(!PalaceWellResumeRules.canRebind(original,owner,door,wybie,null),"Missing new session accepted.");
        System.out.println("PalaceWellResumeRulesCheck: 8 scenarios passed.");
    }
}
