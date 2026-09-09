package qikahome.fabricatedforgefluid.transfer;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 把"绑定单一流体的桶"暴露为 Fabric transfer 流体容器（{@code FluidStorage.ITEM}）。
 *
 * <p>Porting Lib 2.3.x 的 {@code io.github.fabricators_of_create.porting_lib.transfer.fluid.item.
 * FluidBucketWrapper} 在 PL 3.x（MC 1.21.1）中被移除，本类作为 PL 的补充扩展将其语义补回：
 * 每个桶 Item 固定绑定一种流体（{@code fluid}/{@code filledBucket} 由注册方给定，无需读取
 * vanilla {@code BucketItem} 的私有字段）。整桶即一个 bucket 容量：读取/抽取 = 整桶换空桶
 * （{@link Items#BUCKET}）；灌装要求当前为空桶且该流体存在对应桶物品（水/岩浆或
 * {@link Fluid#getBucket()} 非空）。
 */
public class FluidBucketWrapper implements SingleSlotStorage<FluidVariant>
{
    private final ContainerItemContext context;
    private final Fluid fluid;
    private final Item filledBucket;

    public FluidBucketWrapper(ContainerItemContext context, Fluid fluid, Item filledBucket)
    {
        this.context = context;
        this.fluid = fluid;
        this.filledBucket = filledBucket;
    }

    private boolean isFilled()
    {
        return context.getItemVariant().getItem() == filledBucket;
    }

    @Override
    public boolean isResourceBlank()
    {
        return getResource().isBlank();
    }

    @Override
    public FluidVariant getResource()
    {
        return isFilled() ? FluidVariant.of(fluid) : FluidVariant.blank();
    }

    @Override
    public long getAmount()
    {
        return isFilled() ? FluidConstants.BUCKET : 0;
    }

    @Override
    public long getCapacity()
    {
        return FluidConstants.BUCKET;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction)
    {
        if (context.getAmount() != 1 || maxAmount < FluidConstants.BUCKET || isFilled())
            return 0;

        // 与 PL 参考实现一致：水/岩浆可直接灌装，其余流体要求其存在对应的桶物品。
        if (resource.getFluid() != Fluids.WATER && resource.getFluid() != Fluids.LAVA
                && new ItemStack(resource.getFluid().getBucket()).isEmpty())
            return 0;

        try (Transaction nested = transaction.openNested())
        {
            if (context.exchange(ItemVariant.of(filledBucketFor(resource)), 1, nested) == 1)
            {
                nested.commit();
                return FluidConstants.BUCKET;
            }
        }
        return 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction)
    {
        if (context.getAmount() != 1 || maxAmount < FluidConstants.BUCKET || !isFilled() || !resource.isOf(fluid))
            return 0;

        try (Transaction nested = transaction.openNested())
        {
            if (context.exchange(ItemVariant.of(Items.BUCKET), 1, nested) == 1)
            {
                nested.commit();
                return FluidConstants.BUCKET;
            }
        }
        return 0;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator()
    {
        return new Iterator<>()
        {
            private boolean hasNext = true;

            @Override
            public boolean hasNext()
            {
                return hasNext;
            }

            @Override
            public StorageView<FluidVariant> next()
            {
                if (!hasNext)
                    throw new NoSuchElementException();
                hasNext = false;
                return FluidBucketWrapper.this;
            }
        };
    }

    private static Item filledBucketFor(FluidVariant variant)
    {
        Fluid fluid = variant.getFluid();
        if (fluid == Fluids.WATER)
            return Items.WATER_BUCKET;
        if (fluid == Fluids.LAVA)
            return Items.LAVA_BUCKET;
        return fluid.getBucket();
    }
}
