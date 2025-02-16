package io

import cats.effect.Sync
import cbindings.portaudio.aliases.PaStream
import cbindings.portaudio.functions
import constants.FRAMES_PER_BUFFER
import fs2.{Chunk, Pull, Stream}

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

def inputStreamFromPointer[F[_]](pStream: Ptr[PaStream])(implicit
    F: Sync[F]
): Stream[F, Float] =
  // Scala native 0.4 is single threaded so we can re-use the same buffer
  // Once this project moves to use multithreading, this will no longer be possible
  val buffer = new Array[Float](FRAMES_PER_BUFFER)
  Pull
    .eval(F.blocking {
      functions.Pa_ReadStream(
        pStream,
        buffer.atUnsafe(0).toBytePointer,
        FRAMES_PER_BUFFER.toULong
      )
      Chunk.ArraySlice(buffer, 0, FRAMES_PER_BUFFER)
    })
    .flatMap(Pull.output)
    .streamNoScope
    .repeat
