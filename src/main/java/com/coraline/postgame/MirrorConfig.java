package com.coraline.postgame;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class MirrorConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue MAX_QUANTITY=B.defineInRange("maximum_quantity",64,1,64);
    public static final ForgeConfigSpec.IntValue COMMON=B.comment("Other Diamond price units per 64 copied items; price is rounded upward.").defineInRange("common_building_units_per_64",1,1,1000000);
    public static final ForgeConfigSpec.IntValue NATURAL=B.defineInRange("natural_resource_units_per_64",4,1,1000000);
    public static final ForgeConfigSpec.IntValue PROCESSED=B.defineInRange("processed_material_units_per_64",8,1,1000000);
    public static final ForgeConfigSpec.IntValue RARE=B.defineInRange("rare_resource_units_per_64",256,1,1000000);
    public static final ForgeConfigSpec.IntValue EQUIPMENT=B.defineInRange("equipment_units_per_64",4096,1,1000000);
    public static final ForgeConfigSpec.IntValue UNKNOWN=B.defineInRange("unknown_modded_units_per_64",4096,64,1000000);
    public static final ForgeConfigSpec.BooleanValue PERFECT=B.comment("Explicitly permit ordinary Perfect material/Essence duplication. Does not permit currency, Hearts or unsafe data.").define("allow_perfect_materials",false);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERRIDES=B.comment("Item ID=units per 64, for example minecraft:diamond=256. Cannot bypass safety exclusions.")
        .defineListAllowEmpty("value_overrides",List.of(),entry->entry instanceof String s&&s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+=[1-9][0-9]{0,5}"));
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DENY=B.defineListAllowEmpty("additional_forbidden_items",List.of(),entry->entry instanceof String s&&s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
    public static final ForgeConfigSpec SPEC=B.build();
    private MirrorConfig(){}
}
