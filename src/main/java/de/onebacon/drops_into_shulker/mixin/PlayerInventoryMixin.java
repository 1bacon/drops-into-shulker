package de.onebacon.drops_into_shulker.mixin;


import de.onebacon.drops_into_shulker.ShulkerInventoryWrapper;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements Inventory, Nameable {
    @Shadow
    @Final
    public DefaultedList<ItemStack> offHand;
    @Shadow
    @Final
    public PlayerEntity player;

    @Inject(method = "insertStack", at = @At("HEAD"), cancellable = true)
    private void insertStack(ItemStack collected, CallbackInfoReturnable<Boolean> cir) {
        ItemStack offhand_item = this.offHand.get(0);

        if (!(Block.getBlockFromItem(offhand_item.getItem()) instanceof ShulkerBoxBlock)) {
            return; // Offhand is not a Shulker
        }

        // Fix for a dupe using carpet stackableShulkerBoxes
        if (offhand_item.getCount() > 1) {
            //display error message
            MutableText msg = Text.of("Drops-Into-Shulker only works with non-stacked shulker boxes.").copy();
            msg.setStyle(Style.EMPTY.withFormatting(Formatting.RED));
            // This API is pretty clunky...
            ((ServerPlayerEntity) player).networkHandler.sendPacket(new OverlayMessageS2CPacket(msg));
            return;
        }


        NbtCompound offhand_inventory = offhand_item.getOrCreateSubNbt("BlockEntityTag");
        ShulkerInventoryWrapper temp_shulker = new ShulkerInventoryWrapper(offhand_item, offhand_inventory);

        //Try to add the collected stack to temp_shulker
        ItemStack return_stack = temp_shulker.addStack(collected);

        //Save the inventory of temp_shulker
        Optional<NbtElement> nbt_out = Optional.ofNullable(temp_shulker._writeNbt().get("Items"));

        //Award Advancements if something got picked up.
        if (return_stack.getCount() != collected.getCount())
            Criteria.INVENTORY_CHANGED.trigger((ServerPlayerEntity) player, player.getInventory(), collected);

        collected.setCount(return_stack.getCount());

        nbt_out.ifPresent(nbt -> offhand_inventory.put("Items", nbt));

        //Returns true to play animation, increase stats, etc ...
        if (return_stack.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}