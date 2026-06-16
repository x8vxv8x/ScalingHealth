package com.smd.scalinghealth.event;

import gnu.trove.map.hash.THashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.api.ScalingHealthAPI;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.config.ConfigLineParser;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Map;

public final class DamageScaling {
    private static final Marker MARKER = MarkerManager.getMarker("DamageScaling");
    private static final String[] SOURCES_DEFAULT = {"inFire", "lightningBolt", "onFire", "lava", "hotFloor", "inWall", "cramming", "drown", "starve", "cactus", "fall", "flyIntoWall", "outOfWorld", "generic",
            "magic", "wither", "anvil", "fallingBlock", "dragonBreath", "fireworks"};
    private static final String SOURCES_COMMENT = "Set damage scaling by damage source. All vanilla sources should be included, but set to no scaling. Mod sources can be added too, you'll just need the damage"
            + " type string. The number represents how steeply the damage scales. 0 means no scaling (vanilla), 1 means it will be proportional to difficulty/max health (whichever you set). The scaling"
            + " number can be anything, although I recommend a non-negative number.";

    public static final DamageScaling INSTANCE = new DamageScaling();

    private float genericScale;
    private float difficultyWeight;
    private boolean affectHostileMobs;
    private boolean affectPassiveMobs;
    private Mode scaleMode;
    private final Map<String, Float> scalingMap = new THashMap<>();

    private DamageScaling() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) return;

        // Check entity is allowed to be affected.
        if ((entity instanceof IMob && !affectHostileMobs)
                || (!(entity instanceof EntityPlayer) && !affectPassiveMobs))
            return;

        DamageSource source = event.getSource();
        if (source == null) return;

        // Get scaling factor from map, if it exists. Otherwise, use the generic scale.
        float scale = scalingMap.getOrDefault(source.getDamageType(), genericScale);

        // Get the amount of the damage to affect. Can be many times the base value.
        final float affectedAmount = getEffectScale(entity);

        // Calculate damage to add to the original.
        final float original = event.getAmount();
        final float change = scale * affectedAmount * original;
        if (change != 0) {
            final float newAmount = makeSane(event.getAmount() + change);
            event.setAmount(newAmount);

            if (Config.Debug.debugMode && Config.Debug.logPlayerDamage) {
                ScalingHealth.LOGGER.info(MARKER, "{} on {}: {} -> {} (scale={}, affected={}, change={})",
                        source.damageType, entity.getName(), original, newAmount, scale, affectedAmount, change);
            }
        }
    }

    private float getEffectScale(EntityLivingBase entity) {
        switch (scaleMode) {
            case AREA_DIFFICULTY:
                return (float) ScalingHealthAPI.getAreaDifficulty(entity.world, entity.getPosition()) * difficultyWeight;
            case MAX_HEALTH:
                float baseHealth = entity instanceof EntityPlayer
                        ? Config.Player.Health.startingHealth
                        : (float) entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue();
                return (entity.getMaxHealth() - baseHealth) / baseHealth;
            case PLAYER_DIFFICULTY:
                return (float) ScalingHealthAPI.getEntityDifficulty(entity) * difficultyWeight;
            default:
                throw new IllegalStateException("Unknown damage scaling mode: " + scaleMode);
        }
    }

    private static float makeSane(float scaledAmount) {
        // Clamp scaled damage to sane values (non-negative and finite)
        if (scaledAmount < 0)
            return 0;
        if (!Float.isFinite(scaledAmount))
            return Float.MAX_VALUE;
        return scaledAmount;
    }

    public void loadConfig(Configuration config) {
        final String category = Config.CAT_PLAYER_DAMAGE;

        genericScale = config.getFloat("Generic Scale", category, 0f, -Float.MAX_VALUE, Float.MAX_VALUE,
                "伤害来源未在 Scale By Source 列表中出现时使用的默认缩放值。");
        config.getCategory(category).get("Generic Scale").setLanguageKey("config.scalinghealth.main.player.damage.generic_scale");
        difficultyWeight = config.getFloat("Difficulty Weight", category, 0.04f, 0f, 1000f,
                "每点难度对伤害缩放的影响权重。默认 0.04 表示 250 难度时最多增加 10 倍基础伤害。");
        config.getCategory(category).get("Difficulty Weight").setLanguageKey("config.scalinghealth.main.player.damage.difficulty_weight");
        scaleMode = Config.INSTANCE.loadEnum("Scaling Mode", Config.CAT_PLAYER_DAMAGE, Mode.class, Mode.MAX_HEALTH,
                "伤害缩放参考值：MAX_HEALTH 使用最大生命提升，PLAYER_DIFFICULTY 使用玩家难度，AREA_DIFFICULTY 使用区域难度。");

        affectHostileMobs = config.getBoolean("Affect Hostile Mobs", category, false, "敌对怪物受到伤害时是否也应用伤害缩放。");
        config.getCategory(category).get("Affect Hostile Mobs").setLanguageKey("config.scalinghealth.main.player.damage.affect_hostile_mobs");
        affectPassiveMobs = config.getBoolean("Affect Passive Mobs", category, false, "被动生物受到伤害时是否也应用伤害缩放。");
        config.getCategory(category).get("Affect Passive Mobs").setLanguageKey("config.scalinghealth.main.player.damage.affect_passive_mobs");

        scalingMap.clear();

        // Construct a default values array. Just SOURCES_DEFAULT with 0.0 appended to each element.
        String[] defaults = new String[SOURCES_DEFAULT.length];
        for (int i = 0; i < defaults.length; ++i) {
            defaults[i] = SOURCES_DEFAULT[i] + " 0.0";
        }

        for (String str : config.getStringList("Scale By Source", category, defaults, SOURCES_COMMENT)) {
            String[] values = ConfigLineParser.split("Scale By Source", str, 2, "\\s+");
            if (values == null) continue;
            Float scaleValue = ConfigLineParser.parseFloat("Scale By Source", values[1], str);
            if (scaleValue != null) {
                scalingMap.put(values[0], scaleValue);
            }
        }
        config.getCategory(category).get("Scale By Source").setLanguageKey("config.scalinghealth.main.player.damage.scale_by_source");
    }

    public enum Mode {
        MAX_HEALTH, PLAYER_DIFFICULTY, AREA_DIFFICULTY
    }
}
