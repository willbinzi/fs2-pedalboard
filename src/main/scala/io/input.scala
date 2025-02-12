package io

import cats.effect.Sync
import constants.FRAMES_PER_BUFFER
import fs2.{Chunk, Pull, Stream}
import portaudio.aliases.PaStream
import portaudio.functions

import scala.scalanative.libc.string.memcpy
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

def inputStreamFromPointer[F[_]](pStream: Ptr[PaStream])(implicit
    F: Sync[F]
): Stream[F, Float] =
  Pull
    .eval(F.blocking {
      val pFloat: Ptr[Float] = stackalloc[Float](FRAMES_PER_BUFFER)
      val pByte: Ptr[Byte] = pFloat.toBytePointer
      functions.Pa_ReadStream(pStream, pByte, FRAMES_PER_BUFFER.toCSize)
      arrayChunk(pFloat, FRAMES_PER_BUFFER * 4)
    })
    .flatMap(Pull.output)
    .streamNoScope
    .repeat

private def arrayChunk(pointer: Ptr[Float], length: Int): Chunk[Float] =
  val array = new Array[Float](length)
  memcpy(array.atUnsafe(0), pointer, length.toCSize)
  Chunk.ArraySlice(array, 0, length)
