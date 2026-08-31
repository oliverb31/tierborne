package com.ollie.tierborne.playerclass;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public abstract class PlayerClass implements SkillTreeDefinition {
    private final String id;
    private final String displayName;
    private final String description;
    private final ResourceLocation icon;
    private final Item iconItem;
    private final List<String> subclassPreviewNames;

    protected PlayerClass(String id, String displayName, String description, ResourceLocation icon, Item iconItem) {
        this(id, displayName, description, icon, iconItem, List.of());
    }

    protected PlayerClass(String id, String displayName, String description, ResourceLocation icon,
                          Item iconItem, List<String> subclassPreviewNames) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.iconItem = iconItem;
        this.subclassPreviewNames = List.copyOf(subclassPreviewNames);
    }

    @Override public final String id() { return id; }
    @Override public final String displayName() { return displayName; }
    public final String description() { return description; }
    public final ResourceLocation icon() { return icon; }
    public final List<String> subclassPreviewNames() { return subclassPreviewNames; }
    @Override public final ItemStack iconStack() { return new ItemStack(iconItem); }
    @Override public abstract List<Skill> skills();
}
