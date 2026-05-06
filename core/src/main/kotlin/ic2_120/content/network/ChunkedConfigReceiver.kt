package ic2_120.content.network

class ChunkedConfigReceiver(
    private val onComplete: (String) -> Unit
) {
    private var totalChunks = 0
    private var chunks = arrayOfNulls<ByteArray>(0)
    private var receivedCount = 0

    fun accept(totalChunks: Int, chunkIndex: Int, chunkData: ByteArray) {
        if (this.totalChunks != totalChunks) {
            this.totalChunks = totalChunks
            chunks = arrayOfNulls(totalChunks)
            receivedCount = 0
        }

        if (chunks[chunkIndex] != null) return
        chunks[chunkIndex] = chunkData
        receivedCount++

        if (receivedCount == totalChunks) {
            val totalSize = chunks.sumOf { it!!.size }
            val fullData = ByteArray(totalSize)
            var offset = 0
            for (i in 0 until totalChunks) {
                val chunk = chunks[i]!!
                chunk.copyInto(fullData, offset)
                offset += chunk.size
            }
            this.totalChunks = 0
            chunks = arrayOfNulls(0)
            receivedCount = 0
            val json = String(fullData, Charsets.UTF_8)
            onComplete(json)
        }
    }
}
