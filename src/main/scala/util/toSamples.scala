package util

import fs2.{ Chunk, Pipe }

val FLOAT_BUFFER_SIZE: Int = BYTES_BUFFER_SIZE / 2

def toSamples[F[_]]: Pipe[F, Byte, Float] =
  val buffer: Array[Float] = new Array[Float](FLOAT_BUFFER_SIZE)
  _.mapChunks(unpack(buffer))

def unpack(buffer: Array[Float])(chunk: Chunk[Byte]): Chunk[Float] =
  unpack(chunk, buffer)
  Chunk.array(buffer)

def unpack(
  bytes: Chunk[Byte],
  samples: Array[Float]
): Unit =
  // byte iterator
  var i: Int = 0
  // sample iterator
  var s: Int = 0
  while i < bytes.size do
    // As proportion of full scale
    samples(s) = extendSign(unpackSample(bytes, i)) / fullScale.toFloat
    i += 2
    s += 1
    ()

def unpackSample(bytes: Chunk[Byte], i: Int): Long =
  ((bytes(i) & 0xffL) | ((bytes(i + 1) & 0xffL) << 8L))

def extendSign(sample: Long): Long =
  // 64 - 16 = 48
  (sample << 48) >> 48
