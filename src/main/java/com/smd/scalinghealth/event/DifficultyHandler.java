package com.smd.scalinghealth.event;

import com.smd.scalinghealth.Tags;
import com.smd.scalinghealth.utils.EntityMatchList;
import com.smd.scalinghealth.utils.MobPotionMap;
import com.smd.scalinghealth.utils.ModifierHandler;
import com.smd.scalinghealth.utils.SHUtils;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.service.PlayerStateService;
import com.smd.scalinghealth.utils.EntityDifficultyChangeList.DifficultyChanges;
import com.smd.scalinghealth.utils.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class DifficultyHandler {
    public static final String NBT_ENTITY_DIFFICULTY = Tags.MOD_ID + ":difficulty";
    public static DifficultyHandler INSTANCE = new DifficultyHandler();

    private static final int MAX_PENDING_PROCESS_PER_TICK = 64;
    private static final int MAX_PENDING_PROCESS_ATTEMPTS = 40;
    private static int POTION_APPLY_TIME = 10 * 1200;
    private static final String[] POTION_DEFAULTS = {
            "minecraft:strength,30,1",
            "minecraft:speed,10,1",
            "minecraft:speed,50,2",
            "minecraft:fire_resistance,10,1",
            "minecraft:invisibility,25,1",
            "minecraft:resistance,30,1"
    };

    private int debugMobsProcessed;
    private int serverTicks;

    public MobPotionMap potionMap = new MobPotionMap();
    private final List<PendingEntity> pendingEntities = new ArrayList<>();
    private final Set<UUID> pendingEntityIds = new HashSet<>();

    public void initPotionMap() {
        potionMap.clear();

        String[] lines = Config.INSTANCE.getConfiguration().getStringList("Mob Potions",
                Config.CAT_MOB_POTION, POTION_DEFAULTS,
                "The potion effects that mobs can spawn with. You can add effects from other mods if you"
                        + " want to, or remove existing ones. Each line has 3 values separated by commas: the"
                        + " potion ID, the minimum difficulty (higher = less common), and the level (1 = level I,"
                        + " 2 = level II, etc).");
        Config.INSTANCE.getConfiguration().getCategory(Config.CAT_MOB_POTION).get("Mob Potions")
                .setLanguageKey("config.scalinghealth.main.mob.potion.mob_potions");

        for (String line : lines) {
            String[] params = line.split(",");
            if (params.length >= 3) {
                // Ignore extra parameters
                if (params.length > 3) {
                    ScalingHealth.LOGGER.warn("{}. Ignoring extra parameters and processing the first 3.", "Mob potion effects: extra parameters in line: " + line);
                }

                // Parse parameters.
                int index = -1;
                String id = "null";
                Potion potion;
                int minDiff, level;
                try {
                    id = params[++index];
                    potion = Potion.REGISTRY.getObject(new ResourceLocation(id));
                    if (potion == null)
                        throw new NullPointerException();
                    minDiff = Integer.parseInt(params[++index]);
                    level = Integer.parseInt(params[++index]);
                } catch (NumberFormatException ex) {
                    ScalingHealth.LOGGER.warn("{}{}", "Mob potion effects: could not parse parameter " + index
                            + " as integer. Ignoring entire line: ", line);
                    continue;
                } catch (NullPointerException ex) {
                    ScalingHealth.LOGGER.warn("{}\" does not exist.", "Mob potion effects: potion \"" + id);
                    continue;
                }

                // Put it in the map if nothing goes wrong!
                potionMap.put(potion, minDiff, level - 1);
            } else {
                ScalingHealth.LOGGER.warn("{}Ignoring entire line.", "Mob potion effects: malformed line (need 3 comma-separated values): "
                        + line);
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityLivingBase) {
            queueForProcessing((EntityLivingBase) event.getEntity());
        }
    }

    private boolean process(EntityLivingBase entity) {
        if (entity == null || entity.world.isRemote || isProcessed(entity) || !(entity instanceof EntityLiving)) {
            return false;
        }

        boolean difficultyEnabled = Config.Difficulty.maxValue > 0
                && entity.world.getGameRules().getBoolean(ScalingHealth.GAME_RULE_DIFFICULTY);
        if (!difficultyEnabled) {
            markProcessedWithoutDifficulty(entity);
            return true;
        }

        if (entity.ticksExisted <= 1) {
            return false;
        }

        if (entityBlacklistedFromHealthIncrease(entity) || !hasOpenHealthModifierSlot(entity)) {
            markProcessedWithoutDifficulty(entity);
            return true;
        }

        increaseEntityHealth(entity);
        return true;
    }

    public boolean recalculate(EntityLivingBase entity) {
        SHUtils.removeModifier(entity, SharedMonsterAttributes.ATTACK_DAMAGE, ModifierHandler.MODIFIER_ID_DAMAGE);
        SHUtils.removeModifier(entity, SharedMonsterAttributes.MAX_HEALTH, ModifierHandler.MODIFIER_ID_HEALTH);
        entity.getEntityData().setInteger(NBT_ENTITY_DIFFICULTY, 0);
        return process(entity);
    }

    private static boolean isProcessed(EntityLivingBase entity) {
        return entity.getEntityData().getInteger(NBT_ENTITY_DIFFICULTY) != 0;
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        EntityLivingBase killed = event.getEntityLiving();
        DamageSource source = event.getSource();

        if (source.getTrueSource() instanceof EntityPlayer) {
            DifficultyChanges changes = Config.Difficulty.DIFFICULTY_PER_KILL_BY_MOB.get(killed);
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            IPlayerState state = PlayerStateAccess.get(player);
            if (state != null) {
                float amount = changes.value;
                if (Config.Debug.debugMode) {
                    ScalingHealth.LOGGER.info("{}{}", "Killed " + killed.getName()
                            + ": difficulty " + (amount > 0 ? "+" : ""), amount);
                }
                PlayerStateService.incrementDifficulty(player, state, amount);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        processPendingEntities();
        ++serverTicks;
    }

    public static int debugGetMobsProcessed() {
        return INSTANCE.debugMobsProcessed;
    }

    public static float debugGetMobsProcessedRate() {
        return INSTANCE.debugMobsProcessed / (INSTANCE.serverTicks / 20f);
    }

    private void queueForProcessing(EntityLivingBase entity) {
        if (entity == null || isProcessed(entity) || !(entity instanceof EntityLiving)) {
            return;
        }

        UUID id = entity.getPersistentID();
        if (pendingEntityIds.add(id)) {
            pendingEntities.add(new PendingEntity(entity, id));
        }
    }

    private void processPendingEntities() {
        Iterator<PendingEntity> iterator = pendingEntities.iterator();
        int scannedThisTick = 0;
        int processedThisTick = 0;
        while (iterator.hasNext()) {
            if (++scannedThisTick > MAX_PENDING_PROCESS_PER_TICK) {
                break;
            }

            PendingEntity pending = iterator.next();
            EntityLivingBase entity = pending.entity;
            if (entity == null || entity.isDead || entity.world == null || entity.world.isRemote || isProcessed(entity)) {
                pendingEntityIds.remove(pending.id);
                iterator.remove();
                continue;
            }

            if (process(entity)) {
                ++debugMobsProcessed;
                ++processedThisTick;
                pendingEntityIds.remove(pending.id);
                iterator.remove();
                if (processedThisTick >= MAX_PENDING_PROCESS_PER_TICK) {
                    break;
                }
                continue;
            }

            if (++pending.attempts >= MAX_PENDING_PROCESS_ATTEMPTS) {
                markProcessedWithoutDifficulty(entity);
                pendingEntityIds.remove(pending.id);
                iterator.remove();
            }
        }
    }

    private static void markProcessedWithoutDifficulty(EntityLivingBase entity) {
        entity.getEntityData().setInteger(NBT_ENTITY_DIFFICULTY, -1);
    }

    private void increaseEntityHealth(EntityLivingBase entityLiving) {
        if (Config.Difficulty.maxValue <= 0) return;

        World world = entityLiving.world;
        float difficulty = (float) Config.Difficulty.AREA_DIFFICULTY_MODE.getAreaDifficulty(world, entityLiving.getPosition());
        float originalDifficulty = difficulty;
        float originalMaxHealth = entityLiving.getMaxHealth();
        Random rand = ScalingHealth.random;
        boolean isHostile = entityLiving instanceof IMob;

        // Lunar phase multipliers?
        if (Config.Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS_ENABLED && world.getWorldTime() % 24000 > 12000) {
            int moonPhase = world.provider.getMoonPhase(world.getWorldTime()) % 8;
            float multi = Config.Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS[moonPhase];
            difficulty *= multi;
        }

        entityLiving.getEntityData().setInteger(NBT_ENTITY_DIFFICULTY, (int) difficulty);

        float totalDifficulty = difficulty;

        float genAddedHealth = difficulty;
        float baseMaxHealth = (float) entityLiving
                .getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue();
        float healthMultiplier = isHostile ? Config.Mob.Health.hostileHealthMultiplier
                : Config.Mob.Health.peacefulHealthMultiplier;

        genAddedHealth *= healthMultiplier;

        if (Config.Difficulty.statsConsumeDifficulty) difficulty -= genAddedHealth;

        if (difficulty > 0) {
            float diffIncrease = 2 * healthMultiplier * difficulty * rand.nextFloat();
            if (Config.Difficulty.statsConsumeDifficulty) difficulty -= diffIncrease;
            genAddedHealth += diffIncrease;
        }

        // Increase attack damage.
        float genAddedDamage = 0;
        if (difficulty > 0 && !Config.Mob.damageBonusBlacklist.contains(entityLiving)) {
            float diffIncrease = difficulty * rand.nextFloat();
            genAddedDamage = diffIncrease * Config.Mob.damageMultiplier;
            // Clamp the value so it doesn't go over the maximum config.
            if (Config.Mob.maxDamageBoost > 0f)
                genAddedDamage = MathHelper.clamp(genAddedDamage, 0f, Config.Mob.maxDamageBoost);

            // Decrease difficulty based on the damage actually added, instead of diffIncrease.
            if (Config.Difficulty.statsConsumeDifficulty)
                difficulty -= genAddedDamage / Config.Mob.damageMultiplier;
        }

        // Random potion effect
        float potionChance = isHostile ? Config.Mob.hostilePotionChance
                : Config.Mob.passivePotionChance;
        if (difficulty > 0 && rand.nextFloat() < potionChance) {
            MobPotionMap.PotionEntry pot = potionMap.getRandom(rand, (int) difficulty);
            if (pot != null) {
                entityLiving.addPotionEffect(new PotionEffect(pot.potion, POTION_APPLY_TIME, pot.amplifier));
            }
        }

        // Apply extra health and damage.
        float healthMulti;
        float healthScaleDiff = Math.max(0, baseMaxHealth - 20f);
        switch (Config.Mob.Health.healthScalingMode) {
            case ADD:
                ModifierHandler.setMaxHealth(entityLiving, genAddedHealth + baseMaxHealth, 0);
                break;
            case MULTI:
                healthMulti = genAddedHealth / 20f;
                ModifierHandler.setMaxHealth(entityLiving, healthMulti + baseMaxHealth, 1);
                break;
            case MULTI_HALF:
                healthMulti = genAddedHealth / (20f + healthScaleDiff * 0.5f);
                ModifierHandler.setMaxHealth(entityLiving, healthMulti + baseMaxHealth, 1);
                break;
            case MULTI_QUARTER:
                healthMulti = genAddedHealth / (20f + healthScaleDiff * 0.75f);
                ModifierHandler.setMaxHealth(entityLiving, healthMulti + baseMaxHealth, 1);
                break;
            default:
                ScalingHealth.LOGGER.fatal("Unknown mob health scaling mode: {}", Config.Mob.Health.healthScalingMode.name());
                break;
        }
        ModifierHandler.addAttackDamage(entityLiving, genAddedDamage, 0);

        // Heal.
        if (entityLiving.getMaxHealth() != originalMaxHealth) {
            entityLiving.setHealth(entityLiving.getMaxHealth());
        }

        if (Config.Debug.debugMode && Config.Debug.logSpawns && originalDifficulty > 0f) {
            BlockPos pos = entityLiving.getPosition();
            String line = "Spawn debug: %s (%d, %d, %d): Difficulty=%.2f, Health +%.2f, Damage +%.2f";
            line = String.format(line, entityLiving.getName(), pos.getX(), pos.getY(), pos.getZ(),
                    totalDifficulty, genAddedHealth, genAddedDamage);
            ScalingHealth.LOGGER.info(line);
        }
    }

    private static boolean entityBlacklistedFromHealthIncrease(EntityLivingBase entityLiving) {
        if (entityLiving == null) return true;

        boolean isBoss = !entityLiving.isNonBoss();
        boolean isHostile = entityLiving instanceof IMob;
        boolean isPassive = !isHostile;

        if ((isHostile && (Config.Mob.Health.hostileHealthMultiplier == 0 || !Config.Mob.Health.allowHostile))
                || (isPassive && (Config.Mob.Health.peacefulHealthMultiplier == 0 || !Config.Mob.Health.allowPeaceful))
                || (isBoss && (Config.Mob.Health.hostileHealthMultiplier == 0 || !Config.Mob.Health.allowBoss)))
            return true;

        EntityMatchList blacklist = Config.Mob.Health.mobBlacklist;
        List<Integer> dimBlacklist = Config.Mob.Health.dimensionBlacklist;

        if (blacklist == null || dimBlacklist == null) return false;

        return blacklist.contains(entityLiving) || dimBlacklist.contains(entityLiving.dimension);
    }

    private static boolean hasOpenHealthModifierSlot(EntityLivingBase entity) {
        AttributeModifier modifier = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getModifier(ModifierHandler.MODIFIER_ID_HEALTH);

        return modifier == null || modifier.getAmount() == 0.0 || Double.isNaN(modifier.getAmount());
    }

    private static final class PendingEntity {
        private final EntityLivingBase entity;
        private final UUID id;
        private int attempts;

        private PendingEntity(EntityLivingBase entity, UUID id) {
            this.entity = entity;
            this.id = id;
        }
    }
}
