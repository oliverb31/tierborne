package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.Tierborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;

public final class GeneralSkillTree implements SkillTreeDefinition {
    public static final GeneralSkillTree INSTANCE = new GeneralSkillTree();
    public static final String ROOT = "general_player_root";
    public static final String LUMBERJACK = "general_subclass_lumberjack";
    public static final String MINER = "general_subclass_miner";
    public static final String BLACKSMITH = "general_subclass_blacksmith";
    public static final String WOODCUTTING_SPEED = "general_woodcutting_speed";
    public static final String WOOD_DROPS = "general_wood_drops";
    public static final String MINING_SPEED = "general_mining_speed";
    public static final String ORE_DROPS = "general_ore_drops";
    public static final String MATERIAL_RECOVERY = "general_material_recovery";
    public static final String ENCHANTED_CRAFTING = "general_enchanted_crafting";

    private static final List<Skill> SKILLS = List.of(
            skill(ROOT, "Player", "P", "The root of general progression.", 0, Items.PLAYER_HEAD, 0, 0,
                    List.of(), List.of(), true, null, SkillEffect.NONE),
            skill(LUMBERJACK, "Lumberjack", "AXE", "Specialise in harvesting wood and trees.", 1, Items.IRON_AXE, -2, 1,
                    List.of(ROOT), List.of(MINER, BLACKSMITH), false, null, SkillEffect.GENERAL_SUBCLASS),
            skill(MINER, "Miner", "PICK", "Specialise in extracting stone and valuable ores.", 1, Items.IRON_PICKAXE, 0, 1,
                    List.of(ROOT), List.of(LUMBERJACK, BLACKSMITH), false, null, SkillEffect.GENERAL_SUBCLASS),
            skill(BLACKSMITH, "Blacksmith", "ANVIL", "Specialise in crafting weapons and armour.", 1, Items.ANVIL, 2, 1,
                    List.of(ROOT), List.of(LUMBERJACK, MINER), false, null, SkillEffect.GENERAL_SUBCLASS),
            skill(WOODCUTTING_SPEED, "Woodcutting Speed", "+50%",
                    "Break eligible axe-mineable wood " + GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT + "% faster.",
                    1, Items.IRON_AXE, -3, 2,
                    List.of(LUMBERJACK), List.of(), false,
                    new SkillUpgrade(SkillBonusType.WOODCUTTING_SPEED,
                            GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT), SkillEffect.NONE),
            skill(WOOD_DROPS, "Increased Wood Drops", "+50%",
                    GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT
                            + "% chance to gain 1 additional eligible wood block. Block entities are excluded.",
                    1, Items.OAK_LOG, -2, 2,
                    List.of(LUMBERJACK), List.of(), false,
                    new SkillUpgrade(SkillBonusType.WOOD_DROP_CHANCE,
                            GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT), SkillEffect.WOOD_DROPS),
            skill(MINING_SPEED, "Mining Speed", "+50%",
                    "Break pickaxe-mineable blocks " + GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT + "% faster.",
                    1, Items.IRON_PICKAXE, -1, 2,
                    List.of(MINER), List.of(), false,
                    new SkillUpgrade(SkillBonusType.MINING_SPEED,
                            GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT), SkillEffect.NONE),
            skill(ORE_DROPS, "Increased Ore Drops", "+50%",
                    GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT
                            + "% chance to gain 1 additional ore drop. Does not trigger with Silk Touch.",
                    1, Items.RAW_IRON, 0, 2,
                    List.of(MINER), List.of(), false,
                    new SkillUpgrade(SkillBonusType.ORE_DROP_CHANCE,
                            GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT), SkillEffect.ORE_DROPS),
            skill(MATERIAL_RECOVERY, "Material Recovery", "MR",
                    GeneralSkillBalance.MATERIAL_REFUND_CHANCE_PERCENT
                            + "% chance to return 1 actual ingredient when crafting eligible equipment.",
                    1, Items.IRON_INGOT, 2, 2,
                    List.of(BLACKSMITH), List.of(ENCHANTED_CRAFTING), false, null, SkillEffect.MATERIAL_RECOVERY),
            skill(ENCHANTED_CRAFTING, "Enchanted Crafting", "EC",
                    GeneralSkillBalance.ENCHANTED_CRAFTING_CHANCE_PERCENT
                            + "% chance for eligible equipment to gain compatible random enchantments at power "
                            + GeneralSkillBalance.ENCHANTMENT_MIN_LEVEL + "-"
                            + GeneralSkillBalance.ENCHANTMENT_MAX_LEVEL + ".",
                    1, Items.ENCHANTED_BOOK, 3, 2,
                    List.of(BLACKSMITH), List.of(MATERIAL_RECOVERY), false, null, SkillEffect.ENCHANTED_CRAFTING)
    );

    private GeneralSkillTree() {}

    @Override public String id() { return "general"; }
    @Override public String displayName() { return "General"; }
    @Override public ItemStack iconStack() { return new ItemStack(Items.COMPASS); }
    @Override public List<Skill> skills() { return SKILLS; }

    public Skill selectedSubclass(Set<String> unlockedSkills) {
        return SKILLS.stream().filter(skill -> skill.effect() == SkillEffect.GENERAL_SUBCLASS)
                .filter(skill -> unlockedSkills.contains(skill.id())).findFirst().orElse(null);
    }

    private static Skill skill(String id, String name, String label, String description, int cost,
                               net.minecraft.world.item.Item iconItem,
                               int column, int row, List<String> prerequisites, List<String> exclusions,
                               boolean automatic, SkillUpgrade upgrade, SkillEffect effect) {
        return new Skill(id, name, label, description, cost,
                new ResourceLocation(Tierborne.MOD_ID, "textures/gui/icons/" + id + ".png"),
                SkillIcon.item(iconItem), column, row, prerequisites, exclusions, automatic, upgrade, effect);
    }
}
