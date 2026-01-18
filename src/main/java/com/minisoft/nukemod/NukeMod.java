package com.minisoft.nukemod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(NukeMod.MODID)
public class NukeMod {
    public static final String MODID = "nukemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 方块注册 - 使用简单的方式
    public static final DeferredBlock<Block> NUKE_BLOCK = BLOCKS.registerSimpleBlock("nuke_block",
            p -> p.mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6000.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel((state) -> 7));

    // 方块物品注册
    public static final DeferredItem<BlockItem> NUKE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("nuke_block", NUKE_BLOCK);

    // 物品注册 - 简化食物属性，不使用effect方法
    public static final DeferredItem<Item> NUKE_ITEM = ITEMS.registerSimpleItem("nuke_item",
            p -> p.food(new FoodProperties.Builder()
                            .alwaysEdible()
                            .nutrition(1)
                            .saturationModifier(2f)
                            .build())
                    .stacksTo(16));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NUKE_TAB = CREATIVE_MODE_TABS.register("nuke_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nukemod"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> NUKE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(NUKE_ITEM.get());
                output.accept(NUKE_BLOCK_ITEM.get());
            }).build());

    public NukeMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("NukeMod 正在初始化...");

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(NUKE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}