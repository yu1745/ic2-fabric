s/CustomPacketPayload\.Id</CustomPacketPayload.Type</g
s/CustomPacketPayload\.Id(/CustomPacketPayload.Type(/g
s/override fun getId(): CustomPacketPayload\.Id/override fun type(): CustomPacketPayload.Type/g
s/override fun getId(): CustomPacketPayload\.Type/override fun type(): CustomPacketPayload.Type/g
s/override fun getId() = ID/override fun type() = ID/g
s/\.criterion(/.unlockedBy(/g
s/Shapes\.cuboid(/Shapes.box(/g
s/Block\.createCuboidShape(/Shapes.box(/g
s/\.checkSize(/.checkContainerSize(/g
s/\.addProperties(/.addDataSlots(/g
s/\.onTakeItem(/.onTake(/g
s/override fun onTakeItem(/override fun onTake(/g
s/override fun appendProperties(/override fun createBlockStateDefinition(/g
s/override fun isItemBarVisible(/override fun isBarVisible(/g
s/override fun getItemBarStep(/override fun getBarWidth(/g
s/override fun getItemBarColor(/override fun getBarColor(/g
s/override fun getRenderType(/override fun getRenderShape(/g
s/override fun createBlockEntity(/override fun newBlockEntity(/g
s/override fun read(id: ResourceLocation, json: JsonObject)/override fun fromJson(id: ResourceLocation, json: JsonObject, lookup: HolderLookup.Provider)/g
