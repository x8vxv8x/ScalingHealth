package com.smd.scalinghealth.utils;

import com.smd.scalinghealth.Tags;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;

public final class SHI18n {
    private SHI18n() {}

    public static String key(String prefix, String path) {
        return prefix + "." + Tags.MOD_ID + "." + path;
    }

    public static String itemKey(String itemName) {
        return key("item", itemName);
    }

    public static String itemKey(Item item, String suffix) {
        return item.getTranslationKey() + "." + suffix;
    }

    public static String itemText(String itemName, String suffix, Object... args) {
        return I18n.format(itemKey(itemName) + "." + suffix, args);
    }

    public static String itemText(Item item, String suffix, Object... args) {
        return I18n.format(itemKey(item, suffix), args);
    }

    public static String miscText(String suffix, Object... args) {
        return I18n.format(key("misc", suffix), args);
    }
}
