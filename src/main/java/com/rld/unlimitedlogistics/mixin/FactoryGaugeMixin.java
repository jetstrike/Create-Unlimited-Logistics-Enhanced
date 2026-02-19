package com.rld.unlimitedlogistics.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.rld.unlimitedlogistics.CreateUnlimitedLogistics.LOGGER;


//allows us to fill up to 100 vaults
@Mixin(FactoryPanelBehaviour.class)
public abstract class FactoryGaugeMixin extends FilteringBehaviourMixin {
    @Unique int mode = 0;
    @Unique private static final int MAX_DRAWER_CAPACITY = 1048576;

    @Unique private int getVaultMultiplier() {
        return AllConfigs.server().logistics.vaultCapacity.get();
    }

    @Inject(method = "createBoard", at = @At("HEAD"), cancellable = true)
    public void onCreateBoard(Player player, BlockHitResult hitResult, CallbackInfoReturnable<ValueSettingsBoard> cir) {
        int maxAmount = 100; // 100 represents 100%
        cir.setReturnValue(
                new ValueSettingsBoard(CreateLang.translate("factory_panel.target_amount").component(),
                        maxAmount, 10,
                        List.of(
                                CreateLang.translate("schedule.condition.threshold.items").component(),
                                CreateLang.translate("schedule.condition.threshold.stacks").component(),
                                Component.literal("Percentage of Max (1.04M)") // Clearer label for the 3rd row
                        ),
                        new ValueSettingsFormatter((settings) -> {
                            if(settings.value() == 0) return CreateLang.translateDirect("gui.factory_panel.inactive");
                            int val = Math.max(0, settings.value());
                            return switch(settings.row()) {
                                case 1 -> Component.literal(val + "▤");
                                case 2 -> Component.literal(val + "%M"); // Your clever "% of M" representation
                                default -> Component.literal(String.valueOf(val));
                            };
                        })));
        cir.cancel();
    }

    @Inject(method = "setValueSettings", at = @At(value = "INVOKE", target = "Lorg/joml/Math;max(II)I"))
    public void onSetValueSettings(
            Player player, ValueSettingsBehaviour.ValueSettings settings, boolean ctrlDown, CallbackInfo ci
    ) {
        mode = settings.row();
        LOGGER.info("Row {} Value {} Mode {}", settings.row(), settings.value(), mode);
    }

    @Inject(method = "write", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V", ordinal = 0
    ))
    public void onWrite(
            CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci, @Local(name = "panelTag") CompoundTag panelTag
    ) { panelTag.putInt("Mode", mode); }

    @Inject(method = "writeSafe", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V", ordinal = 0
    ))

    public void onWriteSafe(
            CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci, @Local(name = "panelTag") CompoundTag panelTag
    ) { panelTag.putInt("Mode", mode); }

    @Inject(method = "read", at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/filter/FilterItemStack;of(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lcom/simibubi/create/content/logistics/filter/FilterItemStack;",
            ordinal = 0
    ))
    public void onRead(
            CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci, @Local(name = "panelTag") CompoundTag panelTag
    ) { mode = panelTag.getInt("Mode"); }

    /**
     * @author GPorubanTKK
     * @reason Adds support for Vaults Row
     */
    @Overwrite public ValueSettingsBehaviour.ValueSettings getValueSettings() {
        return new ValueSettingsBehaviour.ValueSettings(mode, count);
    }

    @ModifyVariable(method = "getCountLabelForValueBox", at = @At("STORE"), name = "inStorage")
    public int modifyInStorage(int is, @Local(name = "levelInStorage") int levelInStorage) {
        if (mode == 2) {
            // Calculate what % of the 1,048,576 we currently have
            return (int) ((levelInStorage / (double) MAX_DRAWER_CAPACITY) * 100);
        }
        return levelInStorage / (mode == 1 ? getFilter().getMaxStackSize() : 1);
    }

    @ModifyVariable(method = "getCountLabelForValueBox", at = @At("STORE"), name = "stacks")
    public String modifyStacks(String stacks) {
        return switch(mode) {
            case 0 -> "";
            case 1 -> "▤";
            case 2 -> "%M"; // Or "％" if you want it cleaner
            default -> "";
        };
    }

    @ModifyVariable(method = "tickStorageMonitor", at = @At("STORE"), name = "demand")
    public int modifyDemand(int original) {
        if (mode == 2) {
            // The 'getAmount()' is the 1-100 value from the slider
            double percentage = getAmount() / 100.0;
            return (int) (MAX_DRAWER_CAPACITY * percentage);
        }
        return getAmount() * (mode == 1 ? getFilter().getMaxStackSize() : 1);
    }
}
