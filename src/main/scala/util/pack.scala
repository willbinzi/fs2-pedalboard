package util

import fs2.{ Chunk, Pipe }

val bytesPerSample: Int = 2

val fullScale: Int = 32768

def toBytes[F[_]]: Pipe[F, Float, Byte] =
  _.mapChunks(pack)

def pack(chunk: Chunk[Float]): Chunk[Byte] =
  val byteArray: Array[Byte] = new Array[Byte](chunk.size * 2)
  pack(chunk.toArray, byteArray, chunk.size)
  Chunk.array(byteArray)

def pack(
  samples: Array[Float],
  bytes: Array[Byte],
  slen: Int
): Unit =
  // byte iterator
  var i: Int = 0
  // sample iterator
  var s: Int = 0
  while s < slen do
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
