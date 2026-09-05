package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.Tierborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;

public final class BarbarianPlayerClass extends PlayerClass {
    public static final String ID = "barbarian";
    public static final String ROOT = "barbarian_root";
    public static final String BERSERKER = "barbarian_subclass_berserker";
    public static final String LESS_BLEED = "berserker_less_bleed";
    public static final String LONGER_BERSERK = "berserker_longer_duration";
    public static final String BLOOD_FEAST = "berserker_lifesteal";
    public static final String GREATER_FRENZY = "berserker_greater_frenzy";
    public static final String EXECUTIONER = "barbarian_subclass_executioner";
    public static final String EXECUTE_DAMAGE = "executioner_execute_damage";
    public static final String EXECUTE_CHARGE = "executioner_execute_charge";
    public static final String EXECUTE_MOBILITY = "executioner_execute_mobility";
    public static final String LOW_HEALTH_DAMAGE = "executioner_low_health_damage";

    private static final List<String> SUBCLASSES = List.of(BERSERKER, EXECUTIONER);
    private static final List<Skill> SKILLS = List.of(
            skill(ROOT, "Barbarian", "Axe mastery with increased damage and faster recovery.",
                    0, Items.IRON_AXE, 0, 0, List.of(), List.of(), true, SkillEffect.NONE),
            subclass(BERSERKER, "Berserker",
                    "Toggle Berserk mode, gaining speed, axe damage and lifesteal while accumulating Bleed.",
                    Items.REDSTONE, -3, -3),
            node(LESS_BLEED, "Controlled Bleeding", "Reduces Bleed gained from incoming damage.",
                    Items.LEATHER_CHESTPLATE, -4, -4, BERSERKER),
            node(LONGER_BERSERK, "Enduring Rage", "Increases Berserk's maximum duration.",
                    Items.CLOCK, -5, -3, BERSERKER),
            node(BLOOD_FEAST, "Blood Feast", "Increases lifesteal while Berserk is active.",
                    Items.GOLDEN_APPLE, -4, -2, BERSERKER),
            node(GREATER_FRENZY, "Greater Frenzy", "Increases Berserk axe damage and movement speed.",
                    Items.NETHERITE_AXE, -5, -1, BLOOD_FEAST),
            subclass(EXECUTIONER, "Executioner",
                    "Charge Execute with an axe and punish targets more severely as their health falls.",
                    Items.IRON_AXE, 3, 3),
            node(EXECUTE_DAMAGE, "Final Sentence", "Raises Execute's maximum damage multiplier.",
                    Items.NETHERITE_AXE, 4, 4, EXECUTIONER),
            node(EXECUTE_CHARGE, "Swift Execution", "Reduces Execute's charge time.",
                    Items.CLOCK, 5, 3, EXECUTIONER),
            node(EXECUTE_MOBILITY, "Relentless Pursuit", "Reduces Execute's movement penalty.",
                    Items.LEATHER_BOOTS, 4, 2, EXECUTIONER),
            node(LOW_HEALTH_DAMAGE, "Merciless", "Raises the passive damage bonus against weaker targets.",
                    Items.REDSTONE, 5, 1, EXECUTE_MOBILITY)
    );

    public BarbarianPlayerClass() {
        super(ID, "Barbarian", "A brutal axe fighter who chooses blood-fuelled fury or precise execution.",
                texture("barbarian"), Items.IRON_AXE, List.of("Berserker", "Executioner"));
    }

    @Override
    public List<Skill> skills() {
        return SKILLS;
    }

    public Skill selectedSubclass(Set<String> skills) {
        return SKILLS.stream().filter(skill -> SUBCLASSES.contains(skill.id()) && skills.contains(skill.id()))
                .findFirst().orElse(null);
    }

    private static Skill subclass(String id, String name, String description, Item icon, int x, int y) {
        return skill(id, name, "Alternate Attack: " + description, 1, icon, x, y, List.of(ROOT),
                SUBCLASSES.stream().filter(other -> !other.equals(id)).toList(), false,
                SkillEffect.CLASS_SUBCLASS);
    }

    private static Skill node(String id, String name, String description, Item icon,
                              int x, int y, String prerequisite) {
        return skill(id, name, description, 1, icon, x, y, List.of(prerequisite), List.of(), false,
                SkillEffect.NONE);
    }

    private static Skill skill(String id, String name, String description, int cost, Item icon,
                               int x, int y, List<String> prerequisites, List<String> exclusions,
                               boolean automatic, SkillEffect effect) {
        return new Skill(id, name, name.substring(0, 1), description, cost, texture(id),
                SkillIcon.item(icon), x, y, prerequisites, exclusions, automatic, null, effect);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tierborne.MOD_ID, "textures/gui/icons/barbarian_" + name + ".png");
    }
}
