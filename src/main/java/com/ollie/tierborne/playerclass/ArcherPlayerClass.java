package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.Tierborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ArcherPlayerClass extends PlayerClass {
    public static final String ID = "archer";
    public static final String ROOT = "archer_root";
    public static final String BOW_DAMAGE_1 = "archer_bow_damage_1";
    public static final String BOW_DAMAGE_2 = "archer_bow_damage_2";
    public static final String MOVE_SPEED_1 = "archer_move_speed_1";
    public static final String MOVE_SPEED_2 = "archer_move_speed_2";

    private static final List<Skill> SKILLS = StandardSkillTrees.damageAndMovement(
            ROOT, "Archer", BOW_DAMAGE_1, BOW_DAMAGE_2, "Bow Damage",
            SkillBonusType.BOW_DAMAGE, MOVE_SPEED_1, MOVE_SPEED_2,
            Items.BOW, Items.BOW, ArcherPlayerClass::texture);

    public ArcherPlayerClass() {
        super(ID, "Archer",
                "A precise ranged fighter using deadly arrows and swift movement.",
                texture("archer"), Items.BOW);
    }

    @Override
    public List<Skill> skills() {
        return SKILLS;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tierborne.MOD_ID, "textures/gui/icons/archer_" + name + ".png");
    }
}
