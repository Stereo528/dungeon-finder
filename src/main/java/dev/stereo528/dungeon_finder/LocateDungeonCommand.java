package dev.stereo528.dungeon_finder;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
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
    private static final DynamicCommandExceptionType ERROR_NO_SPAWNER_FOUND = new DynamicCommandExceptionType((object) -> Component.literal("FUCK"));

    public LocateDungeonCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext) {
        commandDispatcher.register(Commands.literal("locate_spawner")
                .requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .executes((context -> locateSpawner(context.getSource(), ResourceOrTagArgument.getResourceOrTag(context, "spawner", Registries.BLOCK))))
        );
    }

    private static int locateSpawner(CommandSourceStack commandSourceStack, ResourceOrTagArgument.Result<Block> result) throws CommandSyntaxException {
        BlockPos blockPos = BlockPos.containing(commandSourceStack.getPosition());
        System.out.println(blockPos);
        ServerLevel serverLevel = commandSourceStack.getLevel();
        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
        List<BlockPos> blockPosList = getBlockPosList(blockPos, serverLevel);
        System.out.println(blockPosList);
        stopwatch.stop();
        if (blockPosList.isEmpty()) {
            throw ERROR_NO_SPAWNER_FOUND.create(result.asPrintable());
        }
        else {
            return locateSuccess(commandSourceStack, result, blockPos, blockPosList, stopwatch.elapsed());
        }

    }

    private static int locateSuccess(CommandSourceStack commandSourceStack, ResourceOrTagArgument.Result<Block> result, BlockPos blockPos, List<BlockPos> blockPosList, Duration duration) {
        BlockPos closest = blockPosList.getFirst();
        int distance = Mth.floor(Mth.sqrt((float) blockPos.distSqr(closest)));
        String blockPosListString = blockPosList.toString();
        Component component = ComponentUtils.wrapInSquareBrackets(Component.literal(closest.getX() + " " + closest.getY() + " " + closest.getZ()))
                .withStyle(style ->
                        style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + closest.getX() + " " + closest.getY() + " " + closest.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                );
        commandSourceStack.sendSuccess(() -> Component.translatable("dungeon_finder.found_dungeon.success", "spawner", component, distance, blockPosListString), false);
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

    private static List<BlockPos> getBlockPosList(BlockPos blockPos, Level level) {
        AABB box = new AABB(blockPos).inflate(25);
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

