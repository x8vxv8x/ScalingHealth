package com.smd.scalinghealth.config;

import com.smd.scalinghealth.Tags;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.event.DamageScaling;
import com.smd.scalinghealth.lib.EnumAreaDifficultyMode;
import com.smd.scalinghealth.lib.EnumHealthModMode;
import com.smd.scalinghealth.lib.SimpleExpression;
import com.smd.scalinghealth.utils.EntityDifficultyChangeList;
import com.smd.scalinghealth.utils.EntityMatchList;
import com.smd.scalinghealth.utils.PlayerMatchList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public class Config {
    public static final class Debug {
        public static boolean debugMode;
        public static boolean debugOverlay;
        public static boolean logSpawns;
        public static boolean logPlayerDamage;
    }

    public static final class Client {
        public static final class Difficulty {
            public static boolean renderMeter;
            public static boolean renderMeterAlways;
            public static int meterPosX;
            public static int meterPosY;
        }
    }

    public static final class Player {
        public static final class Health {
            public static boolean allowModify;
            public static int startingHealth;
            public static int maxHeartContainers;
        }
    }

    public static final class FakePlayer {
        public static boolean generateHearts = true;
        public static boolean haveDifficulty = false;
    }

    public static final class Mob {
        public static float damageMultiplier;
        public static float maxDamageBoost;
        public static float hostilePotionChance;
        public static float passivePotionChance;
        public static float xpBoost;
        public static EntityMatchList damageBonusBlacklist = new EntityMatchList();

        public static final class Health {
            public static boolean allowBoss;
            public static boolean allowPeaceful;
            public static boolean allowHostile;
            public static float hostileHealthMultiplier;
            public static float peacefulHealthMultiplier;
            public static EnumHealthModMode healthScalingMode = EnumHealthModMode.MULTI_HALF;
            public static List<Integer> dimensionBlacklist = new ArrayList<>();
            public static EntityMatchList mobBlacklist = new EntityMatchList();
            private static final String[] mobBlacklistDefaults = new String[]{};
        }
    }

    public static final class Items {
        public static final class Heart {
            public static int bossMin;
            public static int bossMax;
            public static float chanceHostile;
            public static float chancePassive;
            public static boolean dropShardsInstead;
            public static boolean healingEvent;
            public static int healthRestored;
            public static int healthPerContainer;
            public static int xpCost;
        }

        public static float cursedHeartChange;
        public static float enchantedHeartChange;
        public static boolean healingItemFireEvent;
    }

    public static final class Wealth {
        public static boolean enabled;
        public static boolean warnInvalidDrops;
        public static boolean difficultyBasedOnPlayer;
        public static boolean respectMobLootGameRule;
        public static boolean requireRecentlyHit;
        public static boolean allowFakePlayers;
    }

    public static final class Difficulty {
        public static float groupAreaBonus;
        public static float lostOnDeath;
        public static float maxValue;
        public static float minValue;
        public static float perBossKill;
        public static float perHostileKill;
        public static float perPassiveKill;
        public static int searchRadius;
        public static float startValue;
        public static boolean statsConsumeDifficulty;

        public static final String[] DEFAULT_DIFFICULTY_LUNAR_MULTIPLIERS = {
                "1.5", "1.3", "1.2", "1.0", "0.8", "1.0", "1.2", "1.3"
        };
        public static PlayerMatchList DIFFICULTY_EXEMPT_PLAYERS = new PlayerMatchList();
        public static EntityDifficultyChangeList DIFFICULTY_PER_KILL_BY_MOB = new EntityDifficultyChangeList();
        public static Map<Integer, Float> DIMENSION_INCREASE_MULTIPLIER = new HashMap<>();
        public static Map<Integer, SimpleExpression> DIMENSION_VALUE_FACTOR = new HashMap<>();
        public static EnumAreaDifficultyMode AREA_DIFFICULTY_MODE = EnumAreaDifficultyMode.LOCAL_PLAYERS;
        public static boolean DIFFICULTY_LUNAR_MULTIPLIERS_ENABLED = false;
        public static float[] DIFFICULTY_LUNAR_MULTIPLIERS = new float[8];
    }

    static final String split = Configuration.CATEGORY_SPLITTER;
    public static final String CAT_MAIN = "main";
    public static final String CAT_DEBUG = CAT_MAIN + split + "debug";
    public static final String CAT_CLIENT = CAT_MAIN + split + "client";
    public static final String CAT_PLAYER = CAT_MAIN + split + "player";
    public static final String CAT_FAKE_PLAYER = CAT_MAIN + split + "fake_players";
    public static final String CAT_PLAYER_DAMAGE = CAT_PLAYER + split + "damage";
    public static final String CAT_PLAYER_HEALTH = CAT_PLAYER + split + "health";
    public static final String CAT_MOB = CAT_MAIN + split + "mob";
    public static final String CAT_MOB_HEALTH = CAT_MOB + split + "health";
    public static final String CAT_MOB_POTION = CAT_MOB + split + "potion";
    public static final String CAT_ITEMS = CAT_MAIN + split + "items";
    public static final String CAT_CURSED_HEART = CAT_ITEMS + split + "cursed_heart";
    public static final String CAT_ENCHANTED_HEART = CAT_ITEMS + split + "enchanted_heart";
    public static final String CAT_WEALTH = CAT_MAIN + split + "wealth";
    public static final String CAT_DIFFICULTY = CAT_MAIN + split + "difficulty";
    public static final String CAT_DIFFICULTY_LUNAR_PHASES = CAT_DIFFICULTY + split + "lunar_phases";

    private static final List<String> ROOT_CATEGORIES = Arrays.asList(
            CAT_DEBUG,
            CAT_CLIENT,
            CAT_PLAYER_DAMAGE,
            CAT_PLAYER_HEALTH,
            CAT_FAKE_PLAYER,
            CAT_MOB,
            CAT_MOB_HEALTH,
            CAT_MOB_POTION,
            CAT_ITEMS,
            CAT_CURSED_HEART,
            CAT_ENCHANTED_HEART,
            CAT_WEALTH,
            CAT_DIFFICULTY,
            CAT_DIFFICULTY_LUNAR_PHASES
    );

    public static final Config INSTANCE = new Config();

    private Configuration config;

    public void init(File file) {
        String path = file.getPath().replaceFirst("\\.cfg$", "/main.cfg");
        this.config = new Configuration(new File(path), true);
        load();
    }

    public void load() {
        try {
            ensureConfig();
            applyCategoryLanguageKeys();
            loadDebug();
            loadClient();
            loadPlayer();
            loadFakePlayers();
            loadMobs();
            loadItems();
            loadWealth();
            loadDifficulty();
            DamageScaling.INSTANCE.loadConfig(config);

            if (Items.Heart.bossMax < Items.Heart.bossMin) {
                Items.Heart.bossMax = Items.Heart.bossMin;
            }
            ScalingHealth.LOGGER.info("配置已加载。");
        } catch (Exception ex) {
            ScalingHealth.LOGGER.fatal("无法加载配置文件，模组可能无法正常工作。", ex);
        }
    }

    public void save() {
        ensureConfig();
        if (config.hasChanged()) {
            config.save();
        }
    }

    public Configuration getConfiguration() {
        ensureConfig();
        return config;
    }

    public List<IConfigElement> getConfigElements() {
        ensureConfig();
        List<IConfigElement> elements = new ArrayList<>();
        for (String categoryName : ROOT_CATEGORIES) {
            ConfigCategory category = config.getCategory(categoryName);
            elements.add(new ConfigElement(category));
        }
        return elements;
    }

    public <T extends Enum<T>> T loadEnum(String name, String category, Class<T> enumClass, T defaultValue, String comment) {
        String[] validValues = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toArray(String[]::new);
        String value = config.getString(name, category, defaultValue.name(), comment, validValues);
        setLanguageKey(category, name);
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        ScalingHealth.LOGGER.warn("配置项 \"{}\" 的值 \"{}\" 无效，改用默认值 {}。", name, value, defaultValue.name());
        return defaultValue;
    }

    private void loadDebug() {
        Debug.debugMode = bool(CAT_DEBUG, "Debug Mode", false,
                "总调试开关。开启后其它调试配置才有意义。");
        Debug.debugOverlay = bool(CAT_DEBUG, "Debug Overlay", false,
                "在屏幕上绘制生命修正、难度等调试信息。仅建议测试时开启。");
        Debug.logSpawns = bool(CAT_DEBUG, "Log Spawns", false,
                "调试模式下记录怪物生成和属性修改细节。大量刷怪时会影响性能。");
        Debug.logPlayerDamage = bool(CAT_DEBUG, "Log Player Damage", false,
                "调试模式下记录玩家受到伤害时的伤害缩放细节。");
    }

    private void loadClient() {
        Client.Difficulty.renderMeter = bool(CAT_CLIENT, "Render Difficulty Meter", true,
                "是否显示难度条。关闭后客户端完全不绘制难度条。");
        Client.Difficulty.renderMeterAlways = bool(CAT_CLIENT, "Render Difficulty Meter Always", false,
                "是否一直显示难度条。当前按键会直接切换这个开关。");
        Client.Difficulty.meterPosX = integer(CAT_CLIENT, "Position X", 5, Integer.MIN_VALUE, Integer.MAX_VALUE,
                "难度条 X 坐标。负数表示从屏幕右侧反向定位。");
        Client.Difficulty.meterPosY = integer(CAT_CLIENT, "Position Y", -30, Integer.MIN_VALUE, Integer.MAX_VALUE,
                "难度条 Y 坐标。负数表示从屏幕底部反向定位。");
    }

    private void loadPlayer() {
        Player.Health.allowModify = bool(CAT_PLAYER_HEALTH, "Allow Modified Health", true,
                "是否允许本模组修改玩家最大生命值。关闭后心之容器不会提升生命上限。");
        Player.Health.startingHealth = integer(CAT_PLAYER_HEALTH, "Starting Health", 20, 2, Integer.MAX_VALUE,
                "玩家初始生命值，单位为半颗心。原版为 20。");
        Player.Health.maxHeartContainers = integer(CAT_PLAYER_HEALTH, "Max Heart Containers", 0, 0, Integer.MAX_VALUE,
                "玩家最多可累计使用的心之容器数量。0 表示无限制。");
    }

    private void loadFakePlayers() {
        FakePlayer.generateHearts = bool(CAT_FAKE_PLAYER, "Can Generate Hearts", true,
                "假玩家击杀怪物时是否能产生心之容器掉落。关闭可降低刷怪机产出。");
        FakePlayer.haveDifficulty = bool(CAT_FAKE_PLAYER, "Have Difficulty", false,
                "假玩家是否拥有难度数据。多数情况下建议关闭。");
    }

    private void loadMobs() {
        Mob.damageMultiplier = floating(CAT_MOB, "Damage Modifier", 0.1f, 0f, Float.MAX_VALUE,
                "怪物每点难度获得的额外攻击力倍率。0 表示禁用额外攻击力。");
        Mob.maxDamageBoost = floating(CAT_MOB, "Max Damage Bonus", 10f, 0f, 1000f,
                "怪物最多获得多少额外攻击力。0 表示无限制。");
        Mob.hostilePotionChance = floating(CAT_MOB, "Potion Chance (Hostiles)", 0.375f, 0f, 1f,
                "敌对怪物获得额外药水效果的概率。仍需要怪物剩余难度足够。");
        Mob.passivePotionChance = floating(CAT_MOB, "Potion Chance (Passives)", 0.025f, 0f, 1f,
                "被动生物获得额外药水效果的概率。仍需要生物剩余难度足够。");
        Mob.xpBoost = floating(CAT_MOB, "XP Boost", 0.01f, 0f, 1f,
                "怪物每点难度提供的额外经验百分比。0.01 表示 100 难度额外 100%。");

        Mob.damageBonusBlacklist.clear();
        for (String str : stringList(CAT_MOB, "Damage Bonus Blacklist", new String[0],
                "不会获得额外攻击力的实体 ID 列表，支持 * 通配符。")) {
            Mob.damageBonusBlacklist.add(str);
        }

        Mob.Health.allowBoss = bool(CAT_MOB_HEALTH, "Allow Boss Extra Health", true,
                "Boss 是否能按难度获得额外生命值。");
        Mob.Health.allowPeaceful = bool(CAT_MOB_HEALTH, "Allow Peaceful Extra Health", true,
                "和平/被动生物是否能按难度获得额外生命值。");
        Mob.Health.allowHostile = bool(CAT_MOB_HEALTH, "Allow Hostile Extra Health", true,
                "敌对怪物是否能按难度获得额外生命值。");
        Mob.Health.hostileHealthMultiplier = floating(CAT_MOB_HEALTH, "Base Health Modifier", 0.5f, 0f, Float.MAX_VALUE,
                "敌对怪物每点难度至少获得的额外生命值倍率。");
        Mob.Health.peacefulHealthMultiplier = floating(CAT_MOB_HEALTH, "Base Health Modifier Peaceful", 0.25f, 0f, Float.MAX_VALUE,
                "被动生物每点难度至少获得的额外生命值倍率。");
        Mob.Health.healthScalingMode = loadEnum("Scaling Mode", CAT_MOB_HEALTH, EnumHealthModMode.class,
                EnumHealthModMode.MULTI_HALF,
                "额外生命值应用方式：ADD 为直接加值；MULTI 为按基础生命乘法；MULTI_HALF/MULTI_QUARTER 会降低高血量生物的收益。");

        Mob.Health.dimensionBlacklist.clear();
        for (String str : stringList(CAT_MOB_HEALTH, "Dimension Blacklist", new String[0],
                "不会获得额外生命值的维度 ID 列表。只能填写整数。")) {
            Integer value = ConfigLineParser.parseInt("Dimension Blacklist", str, str);
            if (value != null) {
                Mob.Health.dimensionBlacklist.add(value);
            }
        }

        Mob.Health.mobBlacklist.loadConfig(config, "Blacklist", CAT_MOB_HEALTH,
                Mob.Health.mobBlacklistDefaults, false,
                "生命值缩放名单。列表项为实体 ID，支持 * 通配符。");
    }

    private void loadItems() {
        Items.Heart.bossMin = integer(CAT_ITEMS, "Hearts Dropped by Boss Min", 3, 0, 64,
                "Boss 被击杀时掉落心之容器的最小数量。");
        Items.Heart.bossMax = integer(CAT_ITEMS, "Hearts Dropped by Boss Max", 6, 0, 64,
                "Boss 被击杀时掉落心之容器的最大数量。");
        Items.Heart.chanceHostile = floating(CAT_ITEMS, "Heart Drop Chance", 0.01f, 0f, 1f,
                "敌对怪物被击杀时掉落心之容器的概率。");
        Items.Heart.chancePassive = floating(CAT_ITEMS, "Heart Drop Chance (Passive)", 0.001f, 0f, 1f,
                "被动生物被击杀时掉落心之容器的概率。");
        Items.Heart.dropShardsInstead = bool(CAT_ITEMS, "Drop Shards Instead of Containers", false,
                "是否掉落心之水晶碎片而不是完整心之容器。");
        Items.Heart.healingEvent = bool(CAT_ITEMS, "Heart Healing Event", true,
                "心之容器立即治疗时是否触发标准治疗事件。关闭可避免其它模组取消治疗。");
        Items.Heart.healthRestored = integer(CAT_ITEMS, "Hearts Health Restored", 4, 0, Integer.MAX_VALUE,
                "使用心之容器时立即恢复的生命值。");
        Items.Heart.healthPerContainer = integer(CAT_ITEMS, "Health Per Heart Container", 2, 1, Integer.MAX_VALUE,
                "每个永久心之容器增加的最大生命值，单位为半颗心。");
        Items.Heart.xpCost = integer(CAT_ITEMS, "Heart XP Level Cost", 3, 0, Integer.MAX_VALUE,
                "使用心之容器需要消耗的经验等级。");
        Items.cursedHeartChange = floating(CAT_CURSED_HEART, "Difficulty Change", 10f, -Float.MAX_VALUE, Float.MAX_VALUE,
                "使用诅咒之心时改变的玩家难度。正数增加难度。");
        Items.enchantedHeartChange = floating(CAT_ENCHANTED_HEART, "Difficulty Change", -10f, -Float.MAX_VALUE, Float.MAX_VALUE,
                "使用蕴魔之心时改变的玩家难度。负数降低难度。");
        Items.healingItemFireEvent = bool(CAT_ITEMS, "Healing Items Fire Healing Event", true,
                "绷带和医疗包治疗时是否触发标准治疗事件。关闭可避免其它模组取消治疗。");
    }

    private void loadWealth() {
        Wealth.enabled = bool(CAT_WEALTH, "Enable Wealth Drops", true,
                "是否启用基于难度的额外掉落。掉落表文件为 config/scalinghealth/wealth_drops.json。");
        Wealth.warnInvalidDrops = bool(CAT_WEALTH, "Warn Invalid Wealth Drops", true,
                "加载难度掉落表时，是否在日志中警告无效实体、物品、区间或 NBT。");
        Wealth.difficultyBasedOnPlayer = bool(CAT_WEALTH, "Difficulty Based On Player", true,
                "为 true 时使用击杀玩家的难度计算额外掉落；为 false 时使用被杀实体生成时记录的难度。");
        Wealth.respectMobLootGameRule = bool(CAT_WEALTH, "Respect Mob Loot Game Rule", false,
                "是否遵守 doMobLoot 游戏规则。关闭时保持旧 Scaling Wealth 行为。");
        Wealth.requireRecentlyHit = bool(CAT_WEALTH, "Require Recently Hit", false,
                "是否要求实体最近被玩家攻击才产生额外掉落。关闭时保持旧 Scaling Wealth 行为。");
        Wealth.allowFakePlayers = bool(CAT_WEALTH, "Allow Fake Players", true,
                "是否允许假玩家击杀触发额外掉落。");
    }

    private void loadDifficulty() {
        Difficulty.groupAreaBonus = floating(CAT_DIFFICULTY, "Group Area Bonus", 0.05f, -10f, 10f,
                "局部难度中每个额外附近玩家增加的倍率。公式为 1 + 本值 * (附近玩家数 - 1)。");
        Difficulty.lostOnDeath = floating(CAT_DIFFICULTY, "Lost On Death", 0f, -10000f, 10000f,
                "玩家死亡时损失的难度。负数会让玩家死亡后增加难度。");
        Difficulty.maxValue = floating(CAT_DIFFICULTY, "Max Value", Integer.MAX_VALUE, 0f, Float.MAX_VALUE,
                "难度可达到的最大值。");
        Difficulty.minValue = floating(CAT_DIFFICULTY, "Min Value", 0f, 0f, Float.MAX_VALUE,
                "难度可达到的最小值。可以和初始值不同。");
        Difficulty.perBossKill = floating(CAT_DIFFICULTY, "Difficulty Per Boss Kill", 0f, -10000f, 10000f,
                "每次击杀 Boss 改变的难度。");
        Difficulty.perHostileKill = floating(CAT_DIFFICULTY, "Difficulty Per Kill", 0f, -10000f, 10000f,
                "每次击杀敌对怪物改变的难度。");
        Difficulty.perPassiveKill = floating(CAT_DIFFICULTY, "Difficulty Per Passive Kill", 0f, -10000f, 10000f,
                "每次击杀被动生物改变的难度。");
        Difficulty.searchRadius = integer(CAT_DIFFICULTY, "Search Radius", 48, 0, Short.MAX_VALUE,
                "怪物生成时搜索玩家的半径。0 表示无限范围。");
        Difficulty.startValue = floating(CAT_DIFFICULTY, "Starting Value", 0f, 0f, Float.MAX_VALUE,
                "新玩家或新世界的初始难度。");
        Difficulty.statsConsumeDifficulty = bool(CAT_DIFFICULTY, "Stats Consume Difficulty", false,
                "怪物生成属性和药水效果时是否消耗生成难度，开启后更接近旧版本行为。");
        Difficulty.AREA_DIFFICULTY_MODE = loadEnum("Area Mode", CAT_DIFFICULTY, EnumAreaDifficultyMode.class,
                EnumAreaDifficultyMode.LOCAL_PLAYERS,
                "怪物生成时如何计算区域难度：SINGLE_PLAYER 取最近玩家；LOCAL_PLAYERS 取附近玩家加权平均。");

        Difficulty.DIFFICULTY_EXEMPT_PLAYERS.clear();
        for (String name : stringList(CAT_DIFFICULTY, "Exempt Players", new String[0],
                "难度豁免玩家列表。豁免玩家参与计算时视为 0 难度。")) {
            Difficulty.DIFFICULTY_EXEMPT_PLAYERS.add(name);
        }

        Difficulty.DIFFICULTY_PER_KILL_BY_MOB.clear();
        for (String line : stringList(CAT_DIFFICULTY, "Difficulty Per Kill By Mob", new String[0],
                "为特定实体设置击杀难度变化。每行两个字段：实体ID 难度变化，例如 minecraft:zombie 0.1。")) {
            String[] parts = ConfigLineParser.split("Difficulty Per Kill By Mob", line, 2, "\\s+");
            if (parts == null) continue;
            Float value = ConfigLineParser.parseFloat("Difficulty Per Kill By Mob", parts[1], line);
            if (value != null) {
                Difficulty.DIFFICULTY_PER_KILL_BY_MOB.put(parts[0], value);
            }
        }

        Difficulty.DIMENSION_INCREASE_MULTIPLIER.clear();
        for (String line : stringList(CAT_DIFFICULTY, "Difficulty Dimension Multiplier", new String[0],
                "为维度设置难度增长倍率。每行两个字段：维度ID 倍率，例如 -1 1.5。")) {
            String[] parts = ConfigLineParser.split("Difficulty Dimension Multiplier", line, 2, "\\s+");
            if (parts == null) continue;
            Integer dimension = ConfigLineParser.parseInt("Difficulty Dimension Multiplier", parts[0], line);
            Float value = ConfigLineParser.parseFloat("Difficulty Dimension Multiplier", parts[1], line);
            if (dimension != null && value != null) {
                Difficulty.DIMENSION_INCREASE_MULTIPLIER.put(dimension, value);
            }
        }

        Difficulty.DIMENSION_VALUE_FACTOR.clear();
        for (String line : stringList(CAT_DIFFICULTY, "Dimension Value Factor", new String[0],
                "为维度设置区域难度数值修正。每行两个字段：维度ID 表达式，例如 -1 *2.0 或 1 +20。")) {
            String[] parts = ConfigLineParser.split("Dimension Value Factor", line, 2, "\\s+");
            if (parts == null) continue;
            Integer dimension = ConfigLineParser.parseInt("Dimension Value Factor", parts[0], line);
            if (dimension != null) {
                SimpleExpression.from(parts[1]).ifPresent(exp -> Difficulty.DIMENSION_VALUE_FACTOR.put(dimension, exp));
            }
        }

        Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS_ENABLED = bool(CAT_DIFFICULTY_LUNAR_PHASES, "Lunar Phases Enabled", false,
                "是否启用月相难度倍率。只在夜晚根据月相调整怪物生成难度。");
        int lunarPhaseIndex = 0;
        for (String line : stringList(CAT_DIFFICULTY_LUNAR_PHASES, "Lunar Phase Multipliers",
                Difficulty.DEFAULT_DIFFICULTY_LUNAR_MULTIPLIERS,
                "8 个数字，依次对应月相倍率。第一项为满月，第五项为新月。")) {
            Float value = ConfigLineParser.parseFloat("Lunar Phase Multipliers", line, line);
            if (value != null && lunarPhaseIndex < Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS.length) {
                Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS[lunarPhaseIndex] = value;
            }
            ++lunarPhaseIndex;
        }
        if (lunarPhaseIndex != Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS.length) {
            ScalingHealth.LOGGER.warn("配置项 \"Lunar Phase Multipliers\" 需要 8 个值，当前为 {}。", lunarPhaseIndex);
        }
    }

    private boolean bool(String category, String name, boolean defaultValue, String comment) {
        Property property = config.get(category, name, defaultValue, comment);
        applyMetadata(property, category, name);
        return property.getBoolean(defaultValue);
    }

    private int integer(String category, String name, int defaultValue, int min, int max, String comment) {
        Property property = config.get(category, name, defaultValue, comment, min, max);
        applyMetadata(property, category, name);
        return property.getInt(defaultValue);
    }

    private float floating(String category, String name, float defaultValue, float min, float max, String comment) {
        Property property = config.get(category, name, defaultValue, comment, min, max);
        applyMetadata(property, category, name);
        return (float) property.getDouble(defaultValue);
    }

    private String string(String category, String name, String defaultValue, String comment) {
        Property property = config.get(category, name, defaultValue, comment);
        applyMetadata(property, category, name);
        return property.getString();
    }

    private String[] stringList(String category, String name, String[] defaults, String comment) {
        Property property = config.get(category, name, defaults, comment);
        applyMetadata(property, category, name);
        return property.getStringList();
    }

    private void applyMetadata(Property property, String category, String name) {
        property.setLanguageKey(langKey(category, name));
    }

    private void setLanguageKey(String category, String name) {
        config.getCategory(category).get(name).setLanguageKey(langKey(category, name));
    }

    private String langKey(String category, String name) {
        return "config." + Tags.MOD_ID + "." + category.replace(Configuration.CATEGORY_SPLITTER, ".")
                + "." + normalize(name);
    }

    private void applyCategoryLanguageKeys() {
        for (String category : ROOT_CATEGORIES) {
            config.setCategoryLanguageKey(category,
                    "config." + Tags.MOD_ID + "." + category.replace(Configuration.CATEGORY_SPLITTER, "."));
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase().replace("(", "").replace(")", "").replace("-", "")
                .replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    private void ensureConfig() {
        if (config == null) {
            throw new IllegalStateException("Config has not been initialized");
        }
    }
}
