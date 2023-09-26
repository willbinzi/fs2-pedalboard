package util

import fs2.Pipe
import fs2.Chunk

def toSamples[F[_]]: Pipe[F, Byte, Float] =
  _.mapChunks(unpack)

def unpack(chunk: Chunk[Byte]): Chunk[Float] =
  val bytes: Array[Byte] = chunk.toArray
  val samples: Array[Float] = new Array[Float](bytes.length / 2)
  unpack(bytes, samples, bytes.length)
  Chunk.array(samples)

def unpack(
  bytes: Array[Byte],
  samples: Array[Float],
  blen: Int
): Unit =
  // byte iterator
  var i: Int = 0
  // sample iterator
  var s: Int = 0
  while i < blen do
    // As proportion of full scale
    samples(s) = extendSign(unpackSample(bytes, i)) / fullScale.toFloat
    i += 2
    s += 1
    ()

def unpackSample(bytes: Array[Byte], i: Int): Long =
  ((bytes(i) & 0xffL) | ((bytes(i + 1) & 0xffL) << 8L))

def extendSign(sample: Long): Long =
  // 64 - 16 = 48
  (sample << 48) >> 48
