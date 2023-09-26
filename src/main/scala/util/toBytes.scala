package util

import fs2.{ Chunk, Pipe }

val fullScale: Int = 32768

def toBytes[F[_]](buffer: Array[Byte]): Pipe[F, Float, Byte] =
  _.mapChunks(pack(buffer))

def pack(buffer: Array[Byte])(chunk: Chunk[Float]): Chunk[Byte] =
  pack(chunk, buffer)
  Chunk.array(buffer)

def pack(
  samples: Chunk[Float],
  bytes: Array[Byte]
): Unit =
  // byte iterator
  var i: Int = 0
  // sample iterator
  var s: Int = 0
  while s < samples.size do
    packBits(bytes, i, (samples(s) * fullScale).toLong)
    i += 2
    s += 1
    ()

def packBits(
  bytes: Array[Byte],
  i: Int,
  rawSample: Long
): Unit =
  bytes(i) = (rawSample & 0xFF).toByte
  bytes(i + 1) = ((rawSample >> 8) & 0xFF).toByte
  ()
