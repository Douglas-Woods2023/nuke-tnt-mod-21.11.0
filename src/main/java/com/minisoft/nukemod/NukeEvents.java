package com.minisoft.nukemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = NukeMod.MODID)
public class NukeEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // 检查破坏的方块是否是我们的核弹方块
        if (event.getState().is(NukeMod.NUKE_BLOCK.get())) {
            // 检查是否是服务端
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                var pos = event.getPos();
                var player = event.getPlayer();

                // 先取消事件，防止掉落物和正常破坏
                event.setCanceled(true);

                // 移除核弹方块
                serverLevel.removeBlock(pos, false);

                // 创建自定义的爆炸破坏计算器，确保可以破坏方块
                ExplosionDamageCalculator damageCalculator = new ExplosionDamageCalculator() {
                    @Override
                    public boolean shouldBlockExplode(net.minecraft.world.level.Explosion explosion, net.minecraft.world.level.BlockGetter blockGetter, net.minecraft.core.BlockPos blockPos, BlockState blockState, float power) {
                        // 允许破坏几乎所有方块（除了基岩等）
                        return !blockState.isAir() && blockState.getDestroySpeed(blockGetter, blockPos) >= 0;
                    }

                    @Override
                    public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, net.minecraft.world.entity.Entity entity) {
                        return true;
                    }

                    @Override
                    public Optional<Float> getBlockExplosionResistance(net.minecraft.world.level.Explosion explosion, net.minecraft.world.level.BlockGetter blockGetter, net.minecraft.core.BlockPos blockPos, BlockState blockState, FluidState fluidState) {
                        // 降低方块的爆炸抗性，使爆炸更容易破坏方块
                        return Optional.of(Math.min(blockState.getBlock().getExplosionResistance(), 6.0F));
                    }
                };

                // 产生巨大的爆炸 - 使用自定义的爆炸破坏计算器
                serverLevel.explode(
                        player,
                        null,
                        damageCalculator,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        300.0F, // 增大爆炸威力
                        true, // 是否产生火焰
                        Level.ExplosionInteraction.TNT
                );

                // 给爆炸范围内的所有生物添加中毒II效果
                double explosionRadius = 305.0; // 爆炸半径
                AABB affectedArea = new AABB(
                        pos.getX() - explosionRadius,
                        pos.getY() - explosionRadius,
                        pos.getZ() - explosionRadius,
                        pos.getX() + explosionRadius,
                        pos.getY() + explosionRadius,
                        pos.getZ() + explosionRadius
                );

                // 获取范围内的所有实体
                List<Entity> entitiesInRange = serverLevel.getEntities(null, affectedArea);

                for (Entity entity : entitiesInRange) {
                    if (entity instanceof LivingEntity livingEntity) {
                        // 添加中毒II效果，持续30秒 (30 * 20 ticks)
                        livingEntity.addEffect(new MobEffectInstance(
                                MobEffects.POISON, // 中毒效果
                                30 * 20, // 持续时间（30秒）
                                1, // 等级II（0为I级，1为II级）
                                false, // 环境效果（云状效果）
                                true // 显示粒子效果
                        ));

                        // 添加虚弱效果，持续20秒
                        livingEntity.addEffect(new MobEffectInstance(
                                MobEffects.WEAKNESS, // 虚弱效果
                                20 * 20, // 持续时间（20秒）
                                0, // 等级I
                                false,
                                true
                        ));

                        // 如果实体是玩家，发送消息
                        if (livingEntity instanceof Player affectedPlayer) {
                            affectedPlayer.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal("你被核辐射污染了！"),
                                    true
                            );
                        }
                    }
                }

                // 添加更多的粒子效果
                for (int i = 0; i < 200; i++) {
                    serverLevel.sendParticles(
                            ParticleTypes.FLAME,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            10,
                            serverLevel.random.nextDouble() * 3 - 1.5,
                            serverLevel.random.nextDouble() * 3,
                            serverLevel.random.nextDouble() * 3 - 1.5,
                            0.5
                    );
                }

                // 添加烟雾粒子
                for (int i = 0; i < 100; i++) {
                    serverLevel.sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            5,
                            serverLevel.random.nextDouble() * 2 - 1,
                            serverLevel.random.nextDouble() * 2,
                            serverLevel.random.nextDouble() * 2 - 1,
                            0.3
                    );
                }

                // 使用其他粒子代替 ENTITY_EFFECT，比如 SOUL_FIRE_FLAME 或 DRIPPING_OBSIDIAN_TEAR
                for (int i = 0; i < 50; i++) {
                    serverLevel.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME, // 灵魂火焰粒子，有绿色效果
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            5,
                            serverLevel.random.nextDouble() * 2 - 1,
                            serverLevel.random.nextDouble() * 2,
                            serverLevel.random.nextDouble() * 2 - 1,
                            0.5
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        // 检查使用的物品是否是我们的铀块
        if (event.getItem().is(NukeMod.NUKE_ITEM.get())) {
            LivingEntity entity = event.getEntity();

            // 添加中毒II效果，持续30秒 (30 * 20 ticks)
            entity.addEffect(new MobEffectInstance(
                    MobEffects.POISON, // 中毒效果
                    30 * 20, // 持续时间（30秒）
                    1, // 等级II（0为I级，1为II级）
                    false,
                    true
            ));

            // 添加恶心效果
            entity.addEffect(new MobEffectInstance(
                    MobEffects.NAUSEA, // 恶心效果
                    60 * 20, // 持续60秒
                    0,
                    false,
                    true
            ));

            // 添加虚弱效果
            entity.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, // 虚弱效果
                    20 * 20, // 持续20秒
                    0,
                    false,
                    true
            ));

            // 如果实体是玩家，发送消息
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("你吃了铀块！现在中毒了！"),
                        true
                );
            }
        }
    }
}