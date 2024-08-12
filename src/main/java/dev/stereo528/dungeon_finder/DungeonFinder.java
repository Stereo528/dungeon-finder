package dev.stereo528.dungeon_finder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class DungeonFinder implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, context, environment) -> LocateDungeonCommand.register(dispatcher)));
    }
}

