package burvy.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Set;
/**
 Breaking blocks
 1. Runs when no reachable players are found
 2. I am not exposed to the sky if the player is above me
 3. Break blocks above me if player is above me
 4. Break blocks below me if player is below me
 5. Break blocks in front of me if player is on the same Y level
 6. Skip SAFE_BLOCKS
 */
public class BlockDestructionGoal extends Goal {

    private final Zombie zombie;
    private int breakTimer;
    private static final int BREAK_DELAY = 100; // 5 seconds in ticks

    // add more zombie-safe blocks here
    private static final Set<net.minecraft.world.level.block.Block> SAFE_BLOCKS = Set.of(
            Blocks.BLACK_CONCRETE
    );

    public BlockDestructionGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        breakTimer = 0;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = zombie.getTarget();
        if (target == null) return false;

        // can pathfind to player?
        if (isPathfindingSuccessful()) return false;

        // if player is above, i must not be exposed to the sky
        int yDiff = target.blockPosition().getY() - zombie.blockPosition().getY();
        if (yDiff > 0 && isExposedToSky()) return false;

        // i can dig down if player is below me
        return true;
    }

    @Override
    public void start() {
        breakTimer = BREAK_DELAY;
    }

    @Override
    public void tick() {
        if (breakTimer > 0) {
            breakTimer--;
        } else {
            breakBlocks();
            breakTimer = BREAK_DELAY; // Reset for next cycle
        }
    }

    /** break blocks with this setup:
     - X - -
     - Z X -
     - Z X -
     - X - -
     X represents blocks, Z represents the 2 blocks the zombie is in, - is ignored
     */
    private void breakBlocks() {
        LivingEntity target = zombie.getTarget();
        if (target == null) return;

        BlockPos zombiePos = zombie.blockPosition();
        int yDiff = target.blockPosition().getY() - zombiePos.getY();

        // relative to zombie facing direction
        Direction facing = zombie.getDirection();
        BlockPos forward = zombiePos.relative(facing);
        BlockPos backward = zombiePos.relative(facing.getOpposite());

        // determine the blocks to break based on player Y difference
        java.util.List<BlockPos> blocksToBreak = new java.util.ArrayList<>();

        // above me
        if (yDiff > 0) {
            // break above and eye level
            blocksToBreak.add(zombiePos.above());
            blocksToBreak.add(forward);
            // below me
        } else if (yDiff < 0) {
            // break below and foot level
            blocksToBreak.add(zombiePos.below());
            blocksToBreak.add(forward.below());
            // same Y level
        } else {
            // break eye level and foot level
            blocksToBreak.add(forward);
            blocksToBreak.add(forward.below());
        }

        // break blocks serverside
        net.minecraft.server.level.ServerLevel world = (net.minecraft.server.level.ServerLevel) zombie.level();
        for (BlockPos pos : blocksToBreak) {
            BlockState blockState = world.getBlockState(pos);
            net.minecraft.world.level.block.Block block = blockState.getBlock();

            // skip safe blocks and drop blocks
            if (!SAFE_BLOCKS.contains(block)) {
                world.destroyBlock(pos, true);
            }
        }
    }

    // check if i am exposed to the sky
    private boolean isExposedToSky() {
        return zombie.level().canSeeSky(zombie.blockPosition().above(2));
    }

    // check if i found a path
    private boolean isPathfindingSuccessful() {
        net.minecraft.world.entity.ai.navigation.PathNavigation navigation = zombie.getNavigation();
        return navigation.isDone() &&
                navigation.getPath() != null &&
                !navigation.getPath().isDone();
    }
}