package de.onebacon.drops_into_shulker;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Wrapper of ShulkerBoxBlockEntity, to allow writing of NBT and adding stacks,
 * similar to SimpleInventory.
 */
public class ShulkerInventoryWrapper extends ShulkerBoxBlockEntity {

    private ShulkerBoxBlockEntity parent;

    public ShulkerInventoryWrapper(@Nullable DyeColor color, BlockPos pos, BlockState state, ShulkerBoxBlockEntity parent) {
        super(color, pos, state);
        this.parent = parent;
    }

    public ItemStack addStack(ItemStack stack) {
        ItemStack itemStack = stack.copy();
        // Prevent shulker stacking
        if (!parent.canInsert(0, stack, null)) {
            return itemStack;
        }

        this.addToExistingSlot(itemStack);
        this.addToNewSlot(itemStack);

        return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
    }

    private void addToExistingSlot(ItemStack stack) {
        for (int i = 0; i < parent.size(); ++i) {
            ItemStack itemStack = parent.getStack(i);
            if (ItemStack.areItemsEqual(itemStack, stack)) {
                this.transfer(stack, itemStack);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

    }

    private void addToNewSlot(ItemStack stack) {
        for (int i = 0; i < parent.size(); ++i) {
            ItemStack itemStack = parent.getStack(i);
            if (itemStack.isEmpty()) {
                parent.setStack(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

    }

    private void transfer(ItemStack source, ItemStack target) {
        int i = Math.min(parent.getMaxCountPerStack(), target.getMaxCount());
        int j = Math.min(source.getCount(), i - target.getCount());
        if (j > 0) {
            target.increment(j);
            source.decrement(j);
            // this.markDirty();
        }
    }

}
