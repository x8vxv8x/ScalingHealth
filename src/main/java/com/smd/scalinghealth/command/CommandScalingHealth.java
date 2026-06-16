package com.smd.scalinghealth.command;

import com.google.common.collect.ImmutableList;
import com.smd.scalinghealth.Tags;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.service.PlayerStateService;
import com.smd.scalinghealth.wealth.WealthDropTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommandScalingHealth extends CommandBase {
    private static final String NUMFORMAT = "%.2f";

    @Override
    public String getName() {
        return Tags.MOD_ID;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return usage(getName());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && "config_reload".equals(args[0])) {
            executeConfigReload(sender);
            return;
        }
        if (args.length >= 2 && "wealth".equals(args[0])) {
            executeWealth(sender, args);
            return;
        }
        if (args.length < 2) {
            tell(sender, getUsage(sender), false);
            return;
        }

        String command = args[0];
        SubCommand subCommand = SubCommand.fromArg(args[1]);
        if (subCommand == null) {
            tell(sender, getUsage(sender), false);
            return;
        }

        switch (command) {
            case "difficulty":
                executeDifficulty(server, sender, subCommand, getDoubleValueArg(subCommand, args),
                        getTargetPlayers(server, sender, subCommand == SubCommand.GET, args));
                break;
            case "heart_uses":
                executeHeartUses(sender, subCommand, getIntValueArg(subCommand, args),
                        getTargetPlayers(server, sender, subCommand == SubCommand.GET, args));
                break;
            default:
                tell(sender, getUsage(sender), false);
                break;
        }
    }

    private static double getDoubleValueArg(@Nonnull SubCommand subCommand, String[] args) throws CommandException {
        if (subCommand == SubCommand.GET) {
            return -1D;
        }
        if (args.length < 3) {
            throw new CommandException("commands.generic.num.invalid");
        }
        return parseDouble(args[2]);
    }

    private static int getIntValueArg(@Nonnull SubCommand subCommand, String[] args) throws CommandException {
        if (subCommand == SubCommand.GET) {
            return -1;
        }
        if (args.length < 3) {
            throw new CommandException("commands.generic.num.invalid");
        }
        return parseInt(args[2]);
    }

    private static List<EntityPlayerMP> getTargetPlayers(MinecraftServer server, ICommandSender sender, boolean isGet, String[] args) throws CommandException {
        int index = isGet ? 2 : 3;
        return args.length < index + 1
                ? ImmutableList.of(getCommandSenderAsPlayer(sender))
                : getPlayers(server, sender, args[index]);
    }

    private static void executeDifficulty(MinecraftServer server, ICommandSender sender, @Nonnull SubCommand subCommand, double value, List<EntityPlayerMP> targets) {
        if (targets.isEmpty())
            return;

        for (EntityPlayerMP player : targets) {
            IPlayerState state = PlayerStateAccess.get(player);

            if (state == null) {
                tell(sender, "Player data is null for " + player.getName(), false);
                continue;
            }

            if (subCommand == SubCommand.GET) {
                // Report difficulty
                double current = PlayerStateService.getDifficulty(state);
                String strCurrent = String.format(NUMFORMAT, current);
                String strMax = String.format(NUMFORMAT, Config.Difficulty.maxValue);
                tell(sender, "showDifficulty", true, player.getName(), strCurrent, strMax);
            } else {
                // Try set difficulty
                double current = PlayerStateService.getDifficulty(state);
                double toSet = getValueToSet(subCommand, value, current);
                double min = getMinValue(subCommand, current, Config.Difficulty.minValue, Config.Difficulty.maxValue);
                double max = getMaxValue(subCommand, current, Config.Difficulty.minValue, Config.Difficulty.maxValue);

                // Bounds check
                if (!checkBounds(value, min, max)) {
                    tell(sender, TextFormatting.RED, "outOfBounds", true, String.format(NUMFORMAT, min), String.format(NUMFORMAT, max));
                    return;
                }

                // Change it!
                PlayerStateService.setDifficulty(player, state, toSet);
                tell(sender, "setDifficulty", true, player.getName(), String.format(NUMFORMAT, toSet));
            }
        }
    }

    private static void executeHeartUses(ICommandSender sender, @Nonnull SubCommand subCommand, int value, List<EntityPlayerMP> targets) {
        if (targets.isEmpty()) {
            return;
        }

        for (EntityPlayerMP player : targets) {
            IPlayerState state = PlayerStateAccess.get(player);
            if (state == null) {
                tell(sender, "Player data is null for " + player.getName(), false);
                continue;
            }

            int current = PlayerStateService.getHeartContainerUses(state);
            int maxConfigured = PlayerStateService.getMaxHeartContainerUses();
            String maxDisplay = maxConfigured <= 0 ? "unlimited" : Integer.toString(maxConfigured);

            if (subCommand == SubCommand.GET) {
                tell(sender, "showHeartUses", true, player.getName(), Integer.toString(current), maxDisplay);
                continue;
            }

            int toSet = getValueToSet(subCommand, value, current);
            int min = getMinValue(subCommand, current, 0, maxConfigured <= 0 ? Integer.MAX_VALUE : maxConfigured);
            int max = getMaxValue(subCommand, current, 0, maxConfigured <= 0 ? Integer.MAX_VALUE : maxConfigured);

            if (!checkBounds(value, min, max)) {
                tell(sender, TextFormatting.RED, "outOfBounds", true, Integer.toString(min), formatHeartUsesMax(max));
                continue;
            }

            PlayerStateService.setHeartContainerUses(player, state, toSet);
            tell(sender, "setHeartUses", true, player.getName(),
                    Integer.toString(PlayerStateService.getHeartContainerUses(state)));
        }
    }

    private static void executeConfigReload(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("Attempting to reload config, check log for details"));
        Config.INSTANCE.load();
        WealthDropTable.Stats stats = WealthDropTable.INSTANCE.reload();
        tellWealthStats(sender, stats);
    }

    private static void executeWealth(ICommandSender sender, String[] args) {
        String action = args[1];
        if ("reload".equals(action)) {
            WealthDropTable.Stats stats = WealthDropTable.INSTANCE.reload();
            tell(sender, "wealthReloaded", true, stats.entityCount, stats.poolCount, stats.definitionCount, stats.invalidEntries);
            return;
        }
        if ("stats".equals(action)) {
            tellWealthStats(sender, WealthDropTable.INSTANCE.getStats());
            return;
        }
        tell(sender, usage(Tags.MOD_ID), false);
    }

    private static void tellWealthStats(ICommandSender sender, WealthDropTable.Stats stats) {
        tell(sender, "wealthStats", true, stats.entityCount, stats.poolCount, stats.definitionCount, stats.invalidEntries);
    }

    private static String usage(String commandName) {
        return TextFormatting.RED + "Usage: /" + commandName + " <difficulty|heart_uses> <get|set|add|sub> [value] [player] OR /" + commandName + " wealth <reload|stats>";
    }

    /**
     * Gets the actual value to set, based on subCommand. Does not check that the value is valid.
     *
     * @param subCommand The subcommand (most likely SET/ADD/SUB)
     * @param current    The current value.
     * @return The value that will be set, assuming it is valid.
     */
    private static double getValueToSet(SubCommand subCommand, double value, double current) {
        double toSet = value;
        if (subCommand == SubCommand.ADD)
            toSet = current + value;
        else if (subCommand == SubCommand.SUB)
            toSet = current - value;
        return toSet;
    }

    private static int getValueToSet(SubCommand subCommand, int value, int current) {
        int toSet = value;
        if (subCommand == SubCommand.ADD)
            toSet = current + value;
        else if (subCommand == SubCommand.SUB)
            toSet = current - value;
        return toSet;
    }

    /**
     * Gets the minimum value the player could enter, based on subCommand.
     *
     * @param subCommand The subcommand (most likely SET/ADD/SUB)
     * @param current    The current value.
     * @param min        The minimum allowed absolute value.
     * @param max        The maximum allowed absolute value.
     * @return The minimum value that can be entered, adjusted for the subcommand.
     */
    private static double getMinValue(SubCommand subCommand, double current, double min, double max) {
        if (subCommand == SubCommand.ADD)
            return min - current;
        else if (subCommand == SubCommand.SUB)
            return current - max;
        else
            return min;
    }

    private static int getMinValue(SubCommand subCommand, int current, int min, int max) {
        if (subCommand == SubCommand.ADD)
            return min - current;
        else if (subCommand == SubCommand.SUB)
            return current - max;
        else
            return min;
    }

    /**
     * Gets the maximum value the player could enter, based on subCommand.
     *
     * @param subCommand The subcommand (most likely SET/ADD/SUB)
     * @param current    The current value.
     * @param min        The minimum allowed absolute value.
     * @param max        The maximum allowed absolute value.
     * @return The maximum value that can be entered, adjusted for the subcommand.
     */
    private static double getMaxValue(SubCommand subCommand, double current, double min, double max) {
        if (subCommand == SubCommand.ADD)
            return max - current;
        else if (subCommand == SubCommand.SUB)
            return current - min;
        else
            return max;
    }

    private static int getMaxValue(SubCommand subCommand, int current, int min, int max) {
        if (subCommand == SubCommand.ADD)
            return max - current;
        else if (subCommand == SubCommand.SUB)
            return current - min;
        else
            return max;
    }

    private static boolean checkBounds(double value, double min, double max) {
        return !(value < min || value > max);
    }

    private static boolean checkBounds(int value, int min, int max) {
        return !(value < min || value > max);
    }

    private static String formatHeartUsesMax(int value) {
        return value == Integer.MAX_VALUE ? "unlimited" : Integer.toString(value);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, "difficulty", "heart_uses", "wealth", "config_reload");
        else if (args.length == 2 && "wealth".equals(args[0]))
            return getListOfStringsMatchingLastWord(args, "reload", "stats");
        else if (args.length == 2)
            return getListOfStringsMatchingLastWord(args, "get", "set", "add", "sub");
        else if (isUsernameIndex(args, args.length))
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        else
            return ImmutableList.of();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return args.length > 2 && "get".equals(args[1]) ? index == 3 : index == 4;
    }

    private static void tell(ICommandSender sender, String key, boolean fromLocalizationFile, Object... args) {
        tell(sender, TextFormatting.RESET, key, fromLocalizationFile, args);
    }

    private static void tell(ICommandSender sender, TextFormatting format, String key, boolean fromLocalizationFile, Object... args) {
        key = "command." + Tags.MOD_ID + "." + key;
        if (fromLocalizationFile)
            sender.sendMessage(new TextComponentString(String.valueOf(format)).appendSibling(new TextComponentTranslation(key, args)));
        else
            sender.sendMessage(new TextComponentString(format + String.format(key, args)));
    }

    enum SubCommand {
        GET, SET, ADD, SUB;

        @Nullable
        static SubCommand fromArg(String arg) {

            for (SubCommand val : values())
                if (val.name().equalsIgnoreCase(arg))
                    return val;
            return null;
        }
    }
}
