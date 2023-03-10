package de.onebacon.drops_into_shulker.mixin;


import de.onebacon.drops_into_shulker.DropsIntoShulker;
import de.onebacon.drops_into_shulker.ShulkerInventoryWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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

    @Inject(method = "insertStack", at = @At("HEAD"), cancellable = true)
    private void insertStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemStack offhand_item = this.offHand.get(0);
        Block offhand_block = offhand_item.getItem() instanceof BlockItem ? ((BlockItem)offhand_item.getItem()).getBlock() : Blocks.AIR;


        if (Block.getBlockFromItem(offhand_item.getItem()) instanceof ShulkerBoxBlock) {

            NbtCompound tag = offhand_item.getOrCreateSubNbt("BlockEntityTag");
            ShulkerInventoryWrapper temp_shulker = new ShulkerInventoryWrapper(BlockPos.ORIGIN, offhand_block.getDefaultState());

            DropsIntoShulker.LOGGER.info(BlockPos.ORIGIN.toString());
            DropsIntoShulker.LOGGER.info(BlockPos.ORIGIN.toShortString());

            if (!temp_shulker.canInsert(0, stack, null))
            {   //Prevent shulker stacking
                return;
            }

            temp_shulker.readNbt(tag);

            ItemStack return_stack = temp_shulker.addStack(stack);


            NbtCompound nbt_out = new NbtCompound();
            temp_shulker._writeNbt(nbt_out);

            if (return_stack.getCount() != stack.getCount()) {
                stack.setCount(return_stack.getCount());
                tag.put("Items", nbt_out.get("Items"));
                if (return_stack.isEmpty()){
                    cir.setReturnValue(true);
                }
            }
        }
    }
}