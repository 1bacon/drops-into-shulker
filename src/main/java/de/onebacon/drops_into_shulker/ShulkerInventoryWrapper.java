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

/**
 * Wrapper of ShulkerBoxBlockEntity, to allow writing of NBT and adding stacks,
 * similar to SimpleInventory.
 */
public class ShulkerInventoryWrapper extends ShulkerBoxBlockEntity {
    @SuppressWarnings("unused")
    public ShulkerInventoryWrapper(@Nullable DyeColor color, BlockPos pos, BlockState state) {
        super(color, pos, state);
    }

    public ShulkerInventoryWrapper(ItemStack item, NbtCompound nbt) {
        super(BlockPos.ORIGIN, Block.getBlockFromItem(item.getItem()).getDefaultState());

        // properly get the shulkerbox size and prime the entity inventory
        var correctEntity = (ShulkerSizeExt) ((BlockWithEntity) Block.getBlockFromItem(item.getItem()))
                .createBlockEntity(BlockPos.ORIGIN, getCachedState());
        setInvStackList(DefaultedList.ofSize(correctEntity.getInvSize(), ItemStack.EMPTY));

        readNbt(nbt);
    }

    public NbtCompound _writeNbt() {
        NbtCompound out = new NbtCompound();
        super.writeNbt(out);
        return out;
    }

    public ItemStack addStack(ItemStack stack) {
        ItemStack itemStack = stack.copy();
        // Prevent shulker stacking
        if (!this.canInsert(0, stack, null)) {
            return itemStack;
        }

        this.addToExistingSlot(itemStack);
        this.addToNewSlot(itemStack);

        return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
    }

    private void addToExistingSlot(ItemStack stack) {
        for (int i = 0; i < this.size(); ++i) {
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
        for (int i = 0; i < this.size(); ++i) {
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
            // this.markDirty();
        }
    }

}
