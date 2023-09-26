package util

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
