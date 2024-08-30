package dev.stereo528.dungeon_finder;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LocateDungeonCommand {
    private static final DynamicCommandExceptionType ERROR_NO_SPAWNER_FOUND = new DynamicCommandExceptionType((object) -> Component.literal("No Nearby Spawner(s)"));

    public LocateDungeonCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(Commands.literal("locate_spawner")
                .requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                        .then(Commands.literal("list").executes((context -> locateSpawner(context.getSource(), true, 32)))
                                .then(Commands.literal("size").then(Commands.argument("size", IntegerArgumentType.integer(1, 64)).executes(context -> locateSpawner(context.getSource(), true, IntegerArgumentType.getInteger(context, "size"))))))
                        .then(Commands.literal("size").then(Commands.argument("size", IntegerArgumentType.integer(1, 64)).executes(context -> locateSpawner(context.getSource(), false, IntegerArgumentType.getInteger(context, "size")))))
                .executes((context -> locateSpawner(context.getSource(), false, 32)))
        );
    }

    private static int locateSpawner(CommandSourceStack commandSourceStack, boolean printList, int size) throws CommandSyntaxException {
        BlockPos blockPos = BlockPos.containing(commandSourceStack.getPosition());
        ServerLevel serverLevel = commandSourceStack.getLevel();
        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
        List<BlockPos> blockPosList = getBlockPosList(blockPos, serverLevel, (double) size);
        stopwatch.stop();
        if (blockPosList.isEmpty()) {
            throw ERROR_NO_SPAWNER_FOUND.create("");
        }
        else {
            return locateSuccess(commandSourceStack, blockPos, blockPosList, printList, stopwatch.elapsed());
        }

    }

    private static int locateSuccess(CommandSourceStack commandSourceStack , BlockPos blockPos, List<BlockPos> blockPosList, boolean printList, Duration duration) {
        BlockPos closest = blockPosList.getFirst();
        for (BlockPos blockPos0 : blockPosList) {
            int dist = Mth.floor(Mth.sqrt((float)blockPos.distSqr(blockPos0)));
            int closestDist = Mth.floor(Mth.sqrt((float)blockPos.distSqr(closest)));
            if (dist < closestDist) {
                closest = blockPos0;
            }
        }
        BlockPos finalClosest = closest;
        int distance = Mth.floor(Mth.sqrt((float) blockPos.distSqr(finalClosest)));
        String distanceStr = " (" + distance + " Blocks Away) ";
        StringBuilder blockPosListString = new StringBuilder();
        if(printList) {
            for (BlockPos blockPos1 : blockPosList) {
                blockPosListString.append("\n").append("[").append(blockPos1.getX()).append(", ").append(blockPos1.getY()).append(", ").append(blockPos1.getZ()).append("]");
            }
        }

        Component component = ComponentUtils.wrapInSquareBrackets(Component.literal(finalClosest.getX() + " " + finalClosest.getY() + " " + finalClosest.getZ()))
                .withStyle(style ->
                        style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + finalClosest.getX() + " " + finalClosest.getY() + " " + finalClosest.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                );
        commandSourceStack.sendSuccess(() -> Component.literal("Found Nearest Spawner At ").append(component).append(distanceStr).append(blockPosListString.toString()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static List<BlockPos> getBlockPosInAABB(AABB aabb) {
        List<BlockPos> blocks = new ArrayList<BlockPos>();
        for (double y = aabb.minY; y < aabb.maxY; ++y) {
            for (double x = aabb.minX; x < aabb.maxX; ++x) {
                for (double z = aabb.minZ; z < aabb.maxZ; ++z) {
                    blocks.add(new BlockPos((int) x, (int) y, (int) z));
                }
            }
        }
        return blocks;
    }

    private static List<BlockPos> getBlockPosList(BlockPos blockPos, Level level, double size) {
        AABB box = new AABB(blockPos).inflate(size, size, size);
        List<BlockPos> blockPosList = new ArrayList<>();
        for(BlockPos blockPos1 : getBlockPosInAABB(box)) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos1);
            if(blockEntity instanceof SpawnerBlockEntity) {
                blockPosList.add(blockPos1);
            }
        }
        return blockPosList;
    }
}

