package io

import cats.effect.Sync
import constants.FRAMES_PER_BUFFER
import fs2.{Chunk, Pull, Stream}
import portaudio.aliases.PaStream
import portaudio.functions

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

def inputStreamFromPointer[F[_]](pStream: Ptr[PaStream])(implicit
    F: Sync[F]
): Stream[F, Float] =
  val pFloat: Ptr[Float] = stackalloc[Float](FRAMES_PER_BUFFER)
  val pByte: Ptr[Byte] = pFloat.toBytePointer
  Pull
    .eval(F.blocking {
      functions.Pa_ReadStream(pStream, pByte, FRAMES_PER_BUFFER.toULong)
      arrayChunk(pFloat, FRAMES_PER_BUFFER)
    })
    .flatMap(Pull.output)
    .streamNoScope
    .repeat

private def arrayChunk(pointer: Ptr[Float], length: Int): Chunk[Float] =
  val array = new Array[Float](length)
  (0 until length).foreach(i => array(i) = pointer(i))
  Chunk.array(array)
