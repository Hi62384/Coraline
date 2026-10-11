package com.coraline.secrets;
import net.minecraftforge.common.ForgeConfigSpec;
public final class SecretsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue JOURNAL_TRUST,JOURNAL_SUSPICION,CATCH_SEVERITY;
    static {var b=new ForgeConfigSpec.Builder();b.push("secrets");JOURNAL_TRUST=b.defineInRange("journalTrustSeverityPenalty",.30,0.,1.);JOURNAL_SUSPICION=b.defineInRange("journalSuspicionSeverityPenalty",.35,0.,1.);CATCH_SEVERITY=b.defineInRange("carriedEvidenceSuspicionPenalty",.22,0.,1.);b.pop();SPEC=b.build();}
    private SecretsConfig(){}
}
