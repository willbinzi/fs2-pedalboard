package pedals

import constants.{ CHUNKS_PER_SECOND, FLOAT_BUFFER_SIZE }
import fs2.Chunk

def delay[F[_]](delayTimeInSeconds: Float, feedback: Float): Pedal[F] =
  val delayTimeInChunks = (delayTimeInSeconds * CHUNKS_PER_SECOND).toInt
  val delayBuffer = Array.fill[Chunk[Float]](delayTimeInChunks)(Chunk.array(Array.fill(FLOAT_BUFFER_SIZE)(0f)))
  _.scanChunks(0) { (n, chunk) =>
    val i = n % delayTimeInChunks
    val wetChunk = chunk.zipWith(delayBuffer(i))( _ + _ * feedback)
    delayBuffer(i) = wetChunk
    ((n + 1) % delayTimeInChunks, wetChunk)
  }
