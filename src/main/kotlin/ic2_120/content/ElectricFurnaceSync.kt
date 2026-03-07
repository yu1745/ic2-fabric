package ic2_120.content

/**
 * 电炉的同步属性——只定义一次，服务端与客户端共享。
 * 添加新属性只需在此处加一行，两端自动对齐 index。
 */
class ElectricFurnaceSync(schema: SyncSchema) {
    var syncCounter by schema.int("SyncCounter")
    var energy by schema.int("Energy", default = 1000)  // 新增
    var progress by schema.int("Progress")
}
