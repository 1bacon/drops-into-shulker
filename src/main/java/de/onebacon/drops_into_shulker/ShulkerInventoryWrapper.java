package de.onebacon.drops_into_shulker;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper of ShulkerBoxBlockEntity, to allow writing of NBT and adding stacks, similar to SimpleInventory.
 */
public class ShulkerInventoryWrapper extends ShulkerBoxBlockEntity {
    @SuppressWarnings("unused")
    public ShulkerInventoryWrapper(@Nullable DyeColor color, BlockPos pos, BlockState state) {
        super(color, pos, state);
    }

    public ShulkerInventoryWrapper(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public void _writeNbt (NbtCompound nbt) {
        super.writeNbt(nbt);
    }

    public ItemStack addStack(ItemStack stack) {
        ItemStack itemStack = stack.copy();
        this.addToExistingSlot(itemStack);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.addToNewSlot(itemStack);
            return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
        }
    }
    private void addToExistingSlot(ItemStack stack) {
        for(int i = 0; i < INVENTORY_SIZE; ++i) {
            ItemStack itemStack = this.getStack(i);
            if (ItemStack.canCombine(itemStack, stack)) {
                this.transfer(stack, itemStack);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

    }
    private void addToNewSlot(ItemStack stack) {
        for(int i = 0; i < INVENTORY_SIZE; ++i) {
            ItemStack itemStack = this.getStack(i);
            if (itemStack.isEmpty()) {
                this.setStack(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

    }
    private void transfer(ItemStack source, ItemStack target) {
        int i = Math.min(this.getMaxCountPerStack(), target.getMaxCount());
        int j = Math.min(source.getCount(), i - target.getCount());
        if (j > 0) {
            target.increment(j);
            source.decrement(j);
            //this.markDirty();
        }
    }


}
