package de.onebacon.drops_into_shulker.mixin;


import de.onebacon.drops_into_shulker.DropsIntoShulker;
import de.onebacon.drops_into_shulker.ShulkerInventoryWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
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
    private void onPlayerTouch(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemStack shulker = this.offHand.get(0);
        Block possible_shulker = Block.getBlockFromItem(shulker.getItem());
        if (possible_shulker instanceof ShulkerBoxBlock) {

            DropsIntoShulker.LOGGER.info("Has shulker");

            NbtCompound tag = shulker.getOrCreateSubNbt("BlockEntityTag");
            ShulkerInventoryWrapper inv = new ShulkerInventoryWrapper(BlockPos.ORIGIN, possible_shulker.getDefaultState());

            if (!inv.canInsert(0, stack, null))
            {   //Prevent shulker stacking
                return;
            }

            inv.readNbt(tag);

            ItemStack s = inv.addStack(stack);


            NbtCompound nbt_out = new NbtCompound();
            inv._writeNbt(nbt_out);

            if (s.getCount() != stack.getCount()) {
                stack.setCount(s.getCount());
                tag.put("Items", nbt_out.get("Items"));
                if (s.isEmpty()){
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }
}