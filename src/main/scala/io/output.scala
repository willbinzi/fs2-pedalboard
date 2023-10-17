package io

import cats.effect.Sync
import cats.syntax.functor.*
import fs2.Pipe
import portaudio.aliases.PaStream
import portaudio.functions

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

def outputPipeFromPointer[F[_]](pStream: Ptr[PaStream])(using Zone)(implicit
    F: Sync[F]
): Pipe[F, Float, Nothing] =
  val pFloat: Ptr[Float] = alloc[Float](FRAMES_PER_BUFFER)
  val pByte: Ptr[Byte] = pFloat.toBytePointer
  _.chunks.foreach { chunk =>
    (0 until chunk.size).foreach(i => pFloat(i) = chunk(i))
    F.blocking {
      functions.Pa_WriteStream(pStream, pByte, FRAMES_PER_BUFFER.toULong)
      ()
    }
  }
