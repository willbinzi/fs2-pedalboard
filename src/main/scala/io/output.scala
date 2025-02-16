package io

import cats.effect.Sync
import constants.FRAMES_PER_BUFFER
import fs2.Pipe
import portaudio.aliases.PaStream
import portaudio.functions

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

def outputPipeFromPointer[F[_]](pStream: Ptr[PaStream])(implicit
    F: Sync[F]
): Pipe[F, Float, Nothing] =
  _.chunks.foreach { chunk =>
    F.blocking {
      val buffer = new Array[Float](FRAMES_PER_BUFFER)
      chunk.copyToArray(buffer, 0)
      functions.Pa_WriteStream(
        pStream,
        buffer.atUnsafe(0).toBytePointer,
        FRAMES_PER_BUFFER.toCSize
      )
      ()
    }
  }
