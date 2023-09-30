package dataline.input.unpack

import config.{ FLOAT_BUFFER_SIZE, FULL_SCALE }
import fs2.{ Chunk, Pipe }

def toSamples[F[_]]: Pipe[F, Byte, Float] =
  val buffer: Array[Float] = new Array[Float](FLOAT_BUFFER_SIZE)
  _.mapChunks(unpack(buffer))

def unpack(samples: Array[Float])(bytes: Chunk[Byte]): Chunk[Float] =
  // byte iterator
  var i: Int = 0
  // sample iterator
  var s: Int = 0
  while i < bytes.size do
    // As proportion of full scale
    samples(s) = extendSign(unpackSample(bytes, i)) / FULL_SCALE.toFloat
    i += 2
    s += 1
    ()
  Chunk.array(samples)

def unpackSample(bytes: Chunk[Byte], i: Int): Long =
  ((bytes(i) & 0xffL) | ((bytes(i + 1) & 0xffL) << 8L))

// We just transformed a 16 bit number into a 64 bit number. We need to extend the sign bit.
def extendSign(sample: Long): Long =
  // 64 - 16 = 48
  (sample << 48) >> 48
