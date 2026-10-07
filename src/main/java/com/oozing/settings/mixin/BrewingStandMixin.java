package com.oozing.settings.mixin;

import com.oozing.settings.Config;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandMixin {
    /** Brewing never starts if any bottle would turn into a banned potion. */
    @Inject(method = "isBrewable", at = @At("HEAD"), cancellable = true)
    private static void oozings$blockBanned(PotionBrewing brewing, NonNullList<ItemStack> items,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (Config.INSTANCE.bannedPotions.isEmpty()) return;
        ItemStack ingredient = items.get(3);
        for (int i = 0; i < 3; i++) {
            ItemStack in = items.get(i);
            if (in.isEmpty()) continue;
            ItemStack out = brewing.mix(ingredient, in);
            // No recipe for this bottle: vanilla returns it unchanged, so leave it alone
            if (ItemStack.isSameItemSameComponents(out, in)) continue;
            PotionContents contents = out.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()
                && Config.INSTANCE.bannedPotions.contains(contents.potion().get().getRegisteredName())) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
