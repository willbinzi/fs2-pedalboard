package pedals.tremolo.waveforms.util

import constants.FLOAT_BUFFER_SIZE
import fs2.{ Chunk, Pull, Pure, Stream }

def waveSection(sectionLengthInChunks: Int, f: Long => Float): Stream[Pure, Float] =
  val buffer = new Array[Float](FLOAT_BUFFER_SIZE)

  def go(chunkNumber: Long): Pull[Pure, Float, Option[Long]] =
    for (i <- 0 until FLOAT_BUFFER_SIZE) do
      buffer(i) = f((chunkNumber * FLOAT_BUFFER_SIZE) + i)
    Pull.output(Chunk.array(buffer)) >> (
      if chunkNumber < sectionLengthInChunks then
        Pull.pure(Some(chunkNumber + 1))
      else
        Pull.pure(None)
    )
  Pull.loop[Pure, Float, Long](go)(0).stream
