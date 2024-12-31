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
  val pFloat: Ptr[Float] = stackalloc[Float](FRAMES_PER_BUFFER)
  val pByte: Ptr[Byte] = pFloat.toBytePointer
  _.chunks.foreach { chunk =>
    F.blocking {
      (0 until chunk.size).foreach(i => pFloat(i) = chunk(i))
      functions.Pa_WriteStream(pStream, pByte, FRAMES_PER_BUFFER.toCSize)
      ()
    }
  }
