package com.oozing.settings;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OozingsSettings implements ModInitializer {
    private static final List<Runnable> PENDING = new ArrayList<>();

    @Override
    public void onInitialize() {
        Config.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
            dispatcher.register(Commands.literal("oozings")
                // If this line fails to compile on your build, use: .requires(src -> src.hasPermission(2))
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    Menus.openMain(ctx.getSource().getPlayerOrException());
                    return 1;
                })));

        // Item cooldowns
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer)) return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            int secs = Config.cooldownSeconds(stack.getItem());
            if (secs <= 0) return InteractionResult.PASS;
            if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
            ItemStack copy = stack.copy();
            // apply after vanilla finishes the use so our value replaces vanilla's (e.g. pearl's 1s)
            PENDING.add(() -> player.getCooldowns().addCooldown(copy, secs * 20));
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) return;
            List<Runnable> run = new ArrayList<>(PENDING);
            PENDING.clear();
            run.forEach(Runnable::run);
        });
    }
}
