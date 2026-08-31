package com.ollie.tierborne.playerclass;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffects;

import java.util.List;
import java.util.function.Function;

/** Shared progression shape for playerClasses with damage and movement branches. */
final class StandardSkillTrees {
    private StandardSkillTrees() {}

    static List<Skill> damageAndMovement(
            String rootId,
            String rootName,
            String firstDamageId,
            String secondDamageId,
            String damageName,
            SkillBonusType damageType,
            String firstMovementId,
            String secondMovementId,
            Item rootIcon,
            Item damageIcon,
            Function<String, ResourceLocation> icon
    ) {
        return List.of(
                new Skill(rootId, rootName, rootName.substring(0, 1),
                        "The foundation of the " + rootName + " path.", 0,
                        icon.apply("root"), SkillIcon.item(rootIcon), 0, 0, List.of(), List.of(), true, null, SkillEffect.NONE),
                new Skill(firstDamageId, damageName + " I", "+50%",
                        "Adds 50% " + damageName.toLowerCase() + ".", 1,
                        icon.apply("damage_1"), SkillIcon.item(damageIcon), 1, 0, List.of(rootId), List.of(), false,
                        new SkillUpgrade(damageType, 50), SkillEffect.NONE),
                new Skill(secondDamageId, damageName + " II", "+100%",
                        "Adds another 100% " + damageName.toLowerCase() + ".", 1,
                        icon.apply("damage_2"), SkillIcon.item(damageIcon), 2, 0, List.of(firstDamageId), List.of(), false,
                        new SkillUpgrade(damageType, 100), SkillEffect.NONE),
                new Skill(firstMovementId, "Movement Speed I", "+50%",
                        "Adds 50% intrinsic movement speed.", 1,
                        icon.apply("movement_1"), SkillIcon.effect(MobEffects.MOVEMENT_SPEED), -1, 0, List.of(rootId), List.of(), false,
                        new SkillUpgrade(SkillBonusType.MOVEMENT_SPEED, 50), SkillEffect.NONE),
                new Skill(secondMovementId, "Movement Speed II", "+100%",
                        "Adds another 100% intrinsic movement speed.", 1,
                        icon.apply("movement_2"), SkillIcon.effect(MobEffects.MOVEMENT_SPEED), -2, 0, List.of(firstMovementId), List.of(), false,
                        new SkillUpgrade(SkillBonusType.MOVEMENT_SPEED, 100), SkillEffect.NONE)
        );
    }
}
