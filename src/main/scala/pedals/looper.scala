package pedals

import constants.{ CHUNKS_PER_SECOND, FLOAT_BUFFER_SIZE }
import fs2.Chunk

// TODO: Can we neaten this up somehow by using Stream.repeat for playback?
def looper[F[_]](loopTimeInSeconds: Float): Pedal[F] =
  val loopTimeInChunks = (loopTimeInSeconds * CHUNKS_PER_SECOND).toInt
  val chunkArray: Array[Chunk.ArraySlice[Float]] =
    Array.fill(loopTimeInChunks)(Chunk.ArraySlice(new Array[Float](FLOAT_BUFFER_SIZE)))
  _.scanChunks((0, false)) { case ((n, playback), chunk) =>
    val next = (n + 1) % loopTimeInChunks
    if playback then
      ((next, true), chunk.zipWith(chunkArray(n))(_ + _))
    else
      chunk.copyToArray(chunkArray(n).values)
      ((next, next == 0), chunk)
  }
