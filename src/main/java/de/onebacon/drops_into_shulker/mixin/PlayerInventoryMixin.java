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
import net.minecraft.server.network.ServerPlayerEntity;
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

    @Inject(method = "insertStack", at = @At("HEAD"), cancellable = true)
    private void insertStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemStack offhand_item = this.offHand.get(0);
        Block offhand_block = Block.getBlockFromItem(offhand_item.getItem());

        if (!(offhand_block instanceof ShulkerBoxBlock)) {
            return; // Offhand is not a Shulker
        }

        NbtCompound tag = offhand_item.getOrCreateSubNbt("BlockEntityTag");
        ShulkerInventoryWrapper temp_shulker = new ShulkerInventoryWrapper(BlockPos.ORIGIN, offhand_block.getDefaultState());

        if (!temp_shulker.canInsert(0, stack, null)) {
            return; //Prevent shulker stacking
        }

        //Fill temp_shulker with off_hand contents
        temp_shulker.readNbt(tag);

        //Try to add the collected stack to temp_shulker
        ItemStack return_stack = temp_shulker.addStack(stack);

        //Save the inventory of temp_shulker
        NbtCompound nbt_out = new NbtCompound();
        temp_shulker._writeNbt(nbt_out);

        // If something got picked up.
        if (return_stack.getCount() != stack.getCount()) {

            //Award Advancements
            Criteria.INVENTORY_CHANGED.trigger((ServerPlayerEntity) player, player.getInventory(), stack);

            stack.setCount(return_stack.getCount());
            tag.put("Items", nbt_out.get("Items"));

            //Returns true to play animation, increase stats, etc ...
            if (return_stack.isEmpty()) {
                cir.setReturnValue(true);
            }
        }
    }
}