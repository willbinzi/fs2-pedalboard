package util

import config.BYTES_BUFFER_SIZE
import fs2.{ Chunk, Pipe }

val fullScale: Int = 32768

def toBytes[F[_]]: Pipe[F, Float, Byte] =
  val buffer: Array[Byte] = new Array[Byte](BYTES_BUFFER_SIZE)
  _.mapChunks(pack(buffer))

def pack(bytes: Array[Byte])(samples: Chunk[Float]): Chunk[Byte] =
  // byte iterator
  var i: Int = 0
  // sample iterator
  var s: Int = 0
  while s < samples.size do
    packBits(bytes, i, (samples(s) * fullScale).toLong)
    i += 2
    s += 1
    ()
  Chunk.array(bytes)

def packBits(
  bytes: Array[Byte],
  i: Int,
  rawSample: Long
): Unit =
  bytes(i) = (rawSample & 0xFF).toByte
  bytes(i + 1) = ((rawSample >> 8) & 0xFF).toByte
  ()
