package ic2_120.content.block

import java.util.UUID

/**
 * 方块实体所有者接口。
 * 实现此接口的 BlockEntity 可记录放置者 UUID，用于领地保护检查。
 */
interface IOwned {
    var ownerUuid: UUID?
}
