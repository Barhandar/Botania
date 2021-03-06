package vazkii.botania.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.AlternativesLootEntry;
import net.minecraft.world.storage.loot.ConstantRange;
import net.minecraft.world.storage.loot.DynamicLootEntry;
import net.minecraft.world.storage.loot.ItemLootEntry;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootFunction;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.TableLootEntry;
import net.minecraft.world.storage.loot.conditions.BlockStateProperty;
import net.minecraft.world.storage.loot.conditions.MatchTool;
import net.minecraft.world.storage.loot.conditions.SurvivesExplosion;
import net.minecraft.world.storage.loot.functions.CopyName;
import net.minecraft.world.storage.loot.functions.CopyNbt;
import net.minecraft.world.storage.loot.functions.ExplosionDecay;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.botania.api.subtile.TileEntityGeneratingFlower;
import vazkii.botania.common.block.BlockAltGrass;
import vazkii.botania.common.block.BlockCacophonium;
import vazkii.botania.common.block.BlockModDoubleFlower;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.block.subtile.generating.SubTileGourmaryllis;
import vazkii.botania.common.block.subtile.generating.SubTileHydroangeas;
import vazkii.botania.common.block.subtile.generating.SubTileMunchdew;
import vazkii.botania.common.block.subtile.generating.SubTileRafflowsia;
import vazkii.botania.common.block.subtile.generating.SubTileSpectrolus;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.lib.LibBlockNames;
import vazkii.botania.common.lib.LibMisc;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class BlockLootProvider implements IDataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final DataGenerator generator;
    private final Map<Block, Function<Block, LootTable.Builder>> functionTable = new HashMap<>();

    public BlockLootProvider(DataGenerator generator) {
        this.generator = generator;

        for (Block b : ForgeRegistries.BLOCKS) {
            if (!LibMisc.MOD_ID.equals(b.getRegistryName().getNamespace()))
                continue;
            if (b instanceof SlabBlock)
                functionTable.put(b, BlockLootProvider::genSlab);
            else if (b instanceof BlockModDoubleFlower)
                functionTable.put(b, BlockLootProvider::genDoubleFlower);
            else if (b instanceof BlockAltGrass)
                functionTable.put(b, BlockLootProvider::genAltGrass);
            else if (b.getRegistryName().getPath().matches(LibBlockNames.METAMORPHIC_PREFIX + "\\w+" + "_stone"))
                functionTable.put(b, BlockLootProvider::genMetamorphicStone);
        }

        // Empty
        functionTable.put(ModBlocks.bifrost, BlockLootProvider::empty);
        functionTable.put(ModBlocks.cocoon, BlockLootProvider::empty);
        functionTable.put(ModBlocks.fakeAir, BlockLootProvider::empty);
        functionTable.put(ModBlocks.manaFlame, BlockLootProvider::empty);
        functionTable.put(ModBlocks.pistonRelay, BlockLootProvider::empty);

        // Redirects
        functionTable.put(ModBlocks.cacophonium, b -> genRegular(Blocks.NOTE_BLOCK));
        functionTable.put(ModBlocks.enchantedSoil, b -> genRegular(Blocks.DIRT));
        functionTable.put(ModBlocks.enchanter, b -> genRegular(Blocks.LAPIS_BLOCK));
        functionTable.put(ModBlocks.mossyAltar, b -> genRegular(ModBlocks.defaultAltar));

        // Special
        functionTable.put(ModBlocks.cellBlock, BlockLootProvider::genCellBlock);
        functionTable.put(ModBlocks.root, BlockLootProvider::genRoot);
        functionTable.put(ModBlocks.solidVines, BlockLootProvider::genSolidVine);
        functionTable.put(ModBlocks.tinyPotato, BlockLootProvider::genTinyPotato);

        // Flower NBT saving
        functionTable.put(ModSubtiles.gourmaryllis, b -> genCopyNbt(b, SubTileGourmaryllis.TAG_LAST_FOOD, SubTileGourmaryllis.TAG_LAST_FOOD_COUNT));
        functionTable.put(ModSubtiles.gourmaryllisFloating, b -> genCopyNbt(b, SubTileGourmaryllis.TAG_LAST_FOOD, SubTileGourmaryllis.TAG_LAST_FOOD_COUNT));
        functionTable.put(ModSubtiles.hydroangeas, b -> genCopyNbt(b, SubTileHydroangeas.TAG_COOLDOWN, TileEntityGeneratingFlower.TAG_PASSIVE_DECAY_TICKS));
        functionTable.put(ModSubtiles.hydroangeasFloating, b -> genCopyNbt(b, SubTileHydroangeas.TAG_COOLDOWN, TileEntityGeneratingFlower.TAG_PASSIVE_DECAY_TICKS));
        functionTable.put(ModSubtiles.munchdew, b -> genCopyNbt(b, SubTileMunchdew.TAG_COOLDOWN));
        functionTable.put(ModSubtiles.munchdewFloating, b -> genCopyNbt(b, SubTileMunchdew.TAG_COOLDOWN));
        functionTable.put(ModSubtiles.rafflowsia, b -> genCopyNbt(b, SubTileRafflowsia.TAG_LAST_FLOWER, SubTileRafflowsia.TAG_LAST_FLOWER_TIMES));
        functionTable.put(ModSubtiles.rafflowsiaFloating, b -> genCopyNbt(b, SubTileRafflowsia.TAG_LAST_FLOWER, SubTileRafflowsia.TAG_LAST_FLOWER_TIMES));
        functionTable.put(ModSubtiles.spectrolus, b -> genCopyNbt(b, SubTileSpectrolus.TAG_NEXT_COLOR));
        functionTable.put(ModSubtiles.spectrolusFloating, b -> genCopyNbt(b, SubTileSpectrolus.TAG_NEXT_COLOR));
        functionTable.put(ModSubtiles.thermalily, b -> genCopyNbt(b, SubTileHydroangeas.TAG_COOLDOWN));
        functionTable.put(ModSubtiles.thermalilyFloating, b -> genCopyNbt(b, SubTileHydroangeas.TAG_COOLDOWN));
    }

    @Override
    public void act(DirectoryCache cache) throws IOException {
        Map<ResourceLocation, LootTable.Builder> tables = new HashMap<>();

        for (Block b : ForgeRegistries.BLOCKS) {
            if(!LibMisc.MOD_ID.equals(b.getRegistryName().getNamespace()))
                continue;
            Function<Block, LootTable.Builder> func = functionTable.getOrDefault(b, BlockLootProvider::genRegular);
            tables.put(b.getRegistryName(), func.apply(b));
        }

        for (Map.Entry<ResourceLocation, LootTable.Builder> e : tables.entrySet()) {
            Path path = getPath(generator.getOutputFolder(), e.getKey());
            IDataProvider.save(GSON, cache, LootTableManager.toJson(e.getValue().setParameterSet(LootParameterSets.BLOCK).build()), path);
        }
    }

    private static Path getPath(Path root, ResourceLocation id) {
        return root.resolve("data/" + id.getNamespace() + "/loot_tables/blocks/" + id.getPath() + ".json");
    }

    private static LootTable.Builder empty(Block b) {
        return LootTable.builder();
    }

    private static LootTable.Builder genCopyNbt(Block b, String... tags) {
        LootEntry.Builder<?> entry = ItemLootEntry.builder(b);
        CopyNbt.Builder func = CopyNbt.func_215881_a(CopyNbt.Source.BLOCK_ENTITY);
        for (String tag : tags) {
            func = func.func_216056_a(tag, "BlockEntityTag." + tag);
        }
        LootPool.Builder pool = LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry)
                .acceptCondition(SurvivesExplosion.builder())
                .acceptFunction(func);
        return LootTable.builder().addLootPool(pool);
    }

    private static LootTable.Builder genCellBlock(Block b) {
        ItemPredicate.Builder silkPred = ItemPredicate.Builder.create()
                .enchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.IntBound.atLeast(1)));
        LootEntry.Builder<?> silk = ItemLootEntry.builder(b)
                .acceptCondition(MatchTool.builder(silkPred));
        return LootTable.builder().addLootPool(LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(silk));
    }

    private static LootTable.Builder genTinyPotato(Block b) {
        LootEntry.Builder<?> entry = ItemLootEntry.builder(b)
                .acceptFunction(CopyName.builder(CopyName.Source.BLOCK_ENTITY));
        LootPool.Builder pool = LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry)
                .acceptCondition(SurvivesExplosion.builder());
        return LootTable.builder().addLootPool(pool);
    }

    private static LootTable.Builder genMetamorphicStone(Block b) {
        String cobbleName = b.getRegistryName().getPath().replaceAll("_stone", "_cobblestone");
        Block cobble = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(LibMisc.MOD_ID, cobbleName));
        if (cobble == Blocks.AIR)
            throw new RuntimeException("Couldn't find metamorphic cobble!");
        return genRegular(cobble);
    }

    private static LootTable.Builder genSolidVine(Block b) {
        LootEntry.Builder<?> entry = TableLootEntry.builder(new ResourceLocation("blocks/vine"));
        return LootTable.builder().addLootPool(LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry));
    }

    private static LootTable.Builder genRoot(Block b) {
        LootEntry.Builder<?> entry = ItemLootEntry.builder(ModItems.livingroot)
                .acceptFunction(SetCount.builder(RandomValueRange.of(2, 4)))
                .acceptFunction(ExplosionDecay.builder());
        return LootTable.builder().addLootPool(LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry));
    }

    private static LootTable.Builder genSlab(Block b) {
        LootEntry.Builder<?> entry = ItemLootEntry.builder(b)
                .acceptFunction(SetCount.builder(ConstantRange.of(2))
                        .acceptCondition(BlockStateProperty.builder(b).with(SlabBlock.TYPE, SlabType.DOUBLE)))
                .acceptFunction(ExplosionDecay.builder());
        return LootTable.builder().addLootPool(LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry));
    }

    private static LootTable.Builder genDoubleFlower(Block b) {
       LootEntry.Builder<?> entry = ItemLootEntry.builder(b)
               .acceptCondition(BlockStateProperty.builder(b).with(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
       LootPool.Builder pool = LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry)
               .acceptCondition(SurvivesExplosion.builder());
       return LootTable.builder().addLootPool(pool);
    }

    private static LootTable.Builder genAltGrass(Block b) {
        ItemPredicate.Builder silkPred = ItemPredicate.Builder.create()
                .enchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.IntBound.atLeast(1)));
        LootEntry.Builder<?> silk = ItemLootEntry.builder(b)
                .acceptCondition(MatchTool.builder(silkPred));
        LootEntry.Builder<?> dirt = ItemLootEntry.builder(Blocks.DIRT)
                .acceptCondition(SurvivesExplosion.builder());
        LootEntry.Builder<?> entry = AlternativesLootEntry.builder(silk, dirt);
        LootPool.Builder pool = LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry);
        return LootTable.builder().addLootPool(pool);
    }

    private static LootTable.Builder genRegular(Block b) {
        LootEntry.Builder<?> entry = ItemLootEntry.builder(b);
        LootPool.Builder pool = LootPool.builder().name("main").rolls(ConstantRange.of(1)).addEntry(entry)
                .acceptCondition(SurvivesExplosion.builder());
        return LootTable.builder().addLootPool(pool);
    }

    @Nonnull
    @Override
    public String getName() {
        return "Botania block loot tables";
    }
}
