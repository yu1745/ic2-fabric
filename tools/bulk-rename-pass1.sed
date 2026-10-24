s/\bhasItem(/has(/g
s/\bconditionsFromItem(/has(/g
s/\bmarkDirty\b/setChanged/g
s/\.formatted(/.withStyle(/g
s/\.setBlockState(/.setBlock(/g
s/\.defaultState\b/.defaultBlockState/g
s/\.squaredDistanceTo(/.distanceToSqr(/g
s/\.decrement(/.shrink(/g
s/\.increment(/.grow(/g
s/\.openHandledScreen(/.openMenu(/g
s/\binsertItem(/moveItemStackTo(/g
s/\.getStackInHand(/.getItemInHand(/g
s/\.getEquippedStack(/.getItemBySlot(/g
s/\.nonOpaque()/ .noOcclusion()/g
s/\.isSneaking\b/.isCrouching/g
s/\.dataTracker\b/.entityData/g
s/\.playerManager\b/.playerList/g
s/\.hungerManager\b/.foodData/g
s/\.chunkManager\b/.chunkSource/g
s/\.toTranslationKey(/.toLanguageKey(/g
s/\.dropItem(/.spawnAtLocation(/g
s/\.dropStack(/.spawnAtLocation(/g
s/\.createNbtWithIdentifyingData(/.saveWithFullMetadata(/g
s/\.createNbt(/.saveWithoutMetadata(/g
s/\.insertStack(/.safeInsert(/g
s/\.ofFloored(/.containing(/g
s/validateTicker(world, /BlockEntityType.validateTicker(world, /g
