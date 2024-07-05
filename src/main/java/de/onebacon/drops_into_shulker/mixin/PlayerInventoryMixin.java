package de.onebacon.drops_into_shulker.mixin;


import de.onebacon.drops_into_shulker.ShulkerInventoryWrapper;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements Inventory, Nameable {
    @Shadow
    @Final
    public DefaultedList<ItemStack> offHand;
    @Shadow
    @Final
    public PlayerEntity player;

    @Shadow
    public abstract boolean insertStack(int slot, ItemStack stack);

    @Inject(method = "insertStack", at = @At("HEAD"), cancellable = true)
    private void insertStack(ItemStack collected, CallbackInfoReturnable<Boolean> cir) {
        ItemStack offhand_item = this.offHand.get(0);
        Block offhand_block = Block.getBlockFromItem(offhand_item.getItem());

        if (!(offhand_block instanceof ShulkerBoxBlock sBlock)) {
            return; // Offhand is not a Shulker
        }

        // Fix for a dupe using carpet stackableShulkerBoxes
        if (offhand_item.getCount() > 1) {
            //  Send error message to the hotbar of the player.
            ((ServerPlayerEntity) player).networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.of("Drops-Into-Shulker only works with non-stacked shulker boxes.").copy().withColor(Formatting.RED.getColorValue())));
            return;
        }

        ShulkerBoxBlockEntity sEntity = (ShulkerBoxBlockEntity) sBlock.createBlockEntity(BlockPos.ORIGIN, null);
        // Unfortunately the components do not survive (ItemStack -> Block -> BlockEntity). Read them in again.
        sEntity.readComponents(offhand_item);

        ShulkerInventoryWrapper wrapper = new ShulkerInventoryWrapper(null, BlockPos.ORIGIN, null, sEntity);
        ItemStack remainder = wrapper.addStack(collected);

        // Only update on change, to not trigger repeated writes for players trying to pick up an ItemStack with a full inventory
        if (remainder.getCount() != collected.getCount()) {
            offhand_item.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(((ShulkerBoxBlockEntityAccessor) sEntity).getInventory()));
            //Award Advancements if something got picked up.
            Criteria.INVENTORY_CHANGED.trigger((ServerPlayerEntity) player, player.getInventory(), collected);
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