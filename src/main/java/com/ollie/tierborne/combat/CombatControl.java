package com.ollie.tierborne.combat;

import net.minecraft.world.entity.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reusable offensive-action lock. It intentionally does not lock movement. */
public final class CombatControl {
    private static final Map<UUID,Long> OFFENSE_LOCKED_UNTIL=new HashMap<>();
    private CombatControl(){}
    public static void disableOffense(Entity entity,long until){OFFENSE_LOCKED_UNTIL.merge(entity.getUUID(),until,Math::max);}
    public static boolean offenseDisabled(Entity entity){Long until=OFFENSE_LOCKED_UNTIL.get(entity.getUUID());if(until==null)return false;if(until<=entity.level.getGameTime()||!entity.isAlive()){OFFENSE_LOCKED_UNTIL.remove(entity.getUUID());return false;}return true;}
    public static void clear(Entity entity){OFFENSE_LOCKED_UNTIL.remove(entity.getUUID());}
}
