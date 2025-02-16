package io.portaudio

import boxing.toBytePointer
import cats.effect.Sync
import cbindings.portaudio.aliases.PaStream
import cbindings.portaudio.functions
import constants.FRAMES_PER_BUFFER
import fs2.Pipe

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

def outputPipeFromPointer[F[_]](pStream: Ptr[PaStream])(implicit
    F: Sync[F]
): Pipe[F, Float, Nothing] =
  // Scala native 0.4 is single threaded so we can re-use the same buffer
  // Once this project moves to use multithreading, this will no longer be possible
  val buffer = new Array[Float](FRAMES_PER_BUFFER)
  _.chunks.foreach { chunk =>
    F.blocking {
      chunk.copyToArray(buffer, 0)
      functions.Pa_WriteStream(
        pStream,
        buffer.atUnsafe(0).toBytePointer,
        FRAMES_PER_BUFFER.toULong
      )
      ()
    }
  }
