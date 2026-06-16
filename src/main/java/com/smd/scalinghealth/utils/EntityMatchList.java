package com.smd.scalinghealth.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.util.*;
import java.util.regex.Pattern;

public class EntityMatchList {

    private final List<String> list = new ArrayList<>();
    private boolean whitelist = false;

    private final Set<ResourceLocation> exactEntries = new HashSet<>();
    private final List<Pattern> wildcardPatterns = new ArrayList<>();

    /**
     * 添加一条配置项，同时解析到对应的内部结构中。
     */
    public void add(String str) {
        list.add(str);
        parseEntry(str);
    }

    /**
     * 清空所有数据。
     */
    public void clear() {
        list.clear();
        exactEntries.clear();
        wildcardPatterns.clear();
    }

    /**
     * 根据 list 重建预编译匹配结构。通常只在配置加载后调用。
     */
    private void rebuild() {
        exactEntries.clear();
        wildcardPatterns.clear();
        for (String entry : list) {
            parseEntry(entry);
        }
    }

    /**
     * 将单条配置文本解析为精确匹配项或通配符正则。
     */
    private void parseEntry(String entry) {
        if (entry.contains("*")) {
            String regex = "(?i)" + Pattern.quote(entry).replace("\\*", ".*");
            wildcardPatterns.add(Pattern.compile(regex));
        } else {
            ResourceLocation rl = new ResourceLocation(entry);
            exactEntries.add(rl);
        }
    }

    /**
     * 检查实体是否匹配名单（结合黑白名单模式）。
     */
    public boolean matches(Entity entity) {
        boolean contains = this.contains(entity);
        return this.whitelist == contains;
    }

    /**
     * 判断实体是否在列表中（不含黑白名单逻辑）。
     */
    public boolean contains(Entity entity) {
        ResourceLocation resource = EntityList.getKey(entity);
        if (resource == null) return false;

        if (exactEntries.contains(resource)) {
            return true;
        }

        String id = resource.toString();
        for (Pattern pattern : wildcardPatterns) {
            if (pattern.matcher(id).matches()) {
                return true;
            }
        }
        return false;
    }

    public void loadConfig(Configuration config, String name, String category,
                           String[] defaults, boolean defaultWhitelist, String comment) {
        this.clear();
        String[] entries = config.getStringList(name + " List", category, defaults, comment);
        config.getCategory(category).get(name + " List").setLanguageKey(languageKey(category, name + " List"));
        Collections.addAll(list, entries);
        rebuild();

        this.whitelist = config.getBoolean(name + " IsWhitelist", category, defaultWhitelist,
                "If true, the list is a whitelist. Otherwise it is a blacklist.");
        config.getCategory(category).get(name + " IsWhitelist").setLanguageKey(languageKey(category, name + " IsWhitelist"));
    }

    private static String languageKey(String category, String name) {
        return "config.scalinghealth." + category.replace(Configuration.CATEGORY_SPLITTER, ".")
                + "." + name.toLowerCase().replace("(", "").replace(")", "").replace("-", "")
                .replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
}
