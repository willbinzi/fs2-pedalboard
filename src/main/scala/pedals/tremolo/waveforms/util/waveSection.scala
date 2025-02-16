package pedals.tremolo.waveforms.util

import constants.FRAMES_PER_BUFFER
import fs2.{Chunk, Pull, Pure, Stream}

def waveSection(
    sectionLengthInChunks: Int,
    f: Long => Float
): Stream[Pure, Float] =
  val buffer = new Array[Float](FRAMES_PER_BUFFER)

  def go(chunkNumber: Long): Pull[Pure, Float, Option[Long]] =
    for (i <- 0 until FRAMES_PER_BUFFER) do buffer(i) = f((chunkNumber * FRAMES_PER_BUFFER) + i)
    Pull.output(Chunk.array(buffer)) >> (
      if chunkNumber < sectionLengthInChunks then Pull.pure(Some(chunkNumber + 1))
      else Pull.pure(None)
    )
  Pull.loop[Pure, Float, Long](go)(0).stream
