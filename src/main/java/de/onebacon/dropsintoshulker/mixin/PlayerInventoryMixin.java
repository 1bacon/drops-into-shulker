package de.onebacon.dropsintoshulker.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow
    @Final
    public EntityEquipment equipment;
    @Shadow
    @Final
    public Player player;

    @Inject(
            method = "add(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void insertIntoShulker(ItemStack collected, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // Not on the Server, this only happens when picking up lava for the first time in a singleplayer world ...
            return;
        }

        ItemStack offhandItem = this.equipment.get(EquipmentSlot.OFFHAND);
        Block offhandBlock = Block.byItem(offhandItem.getItem());

        if (!(offhandBlock instanceof ShulkerBoxBlock shulkerBoxBlock)) {
            return; // Offhand is not a Shulker
        }

        // Fix for a dupe using carpet stackableShulkerBoxes
        if (offhandItem.getCount() > 1) {
            //  Send error message to the hotbar of the player.
            serverPlayer.sendSystemMessage(
                    Component.literal("Drops-Into-Shulker only works with non-stacked shulker boxes.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        // Prevent nested shulker boxes
        if (Block.byItem(collected.getItem()) instanceof ShulkerBoxBlock) {
            return;
        }

        if (!(shulkerBoxBlock.newBlockEntity(BlockPos.ZERO, offhandBlock.defaultBlockState())
                instanceof Container shulkerContainer)) {
            return;
        }

        SimpleContainer shulkerContents = new SimpleContainer(shulkerContainer.getContainerSize());
        // Unfortunately the components do not survive (ItemStack -> Block -> BlockEntity). Read them in again.
        offhandItem.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(shulkerContents.getItems());
        ItemStack remainder = shulkerContents.addItem(collected);

        // Only update on change, to not trigger repeated writes for players trying to pick up an ItemStack with a full inventory
        if (remainder.getCount() != collected.getCount()) {
            offhandItem.set(
                    DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(shulkerContents.getItems())
            );
            //Award Advancements if something got picked up.
            CriteriaTriggers.INVENTORY_CHANGED.trigger(serverPlayer, serverPlayer.getInventory(), collected);
            collected.setCount(remainder.getCount());
        }

        // Returns true to play animation, increase stats, etc ...
        // Yes, this will not play an animation or increase stats when the stack is not picked up entirely,
        // this is consistent with a vanilla bug. (MC-120643)
        if (remainder.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
