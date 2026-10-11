package com.coraline.parents;

import net.minecraft.server.MinecraftServer;
import java.util.UUID;

public final class ParentAccess {
    public static FamilyData.Family family(MinecraftServer server,UUID owner){
        return FamilyData.get(server).families.values().stream()
                .filter(f->!f.dead&&owner.equals(f.owner)).findFirst().orElse(null);
    }
}
