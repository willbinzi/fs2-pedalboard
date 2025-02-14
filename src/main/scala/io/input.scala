package io

import cats.effect.Sync
import constants.{ BYTES_PER_BUFFER, FRAMES_PER_BUFFER }
import fs2.{Chunk, Pull, Stream}
import portaudio.aliases.PaStream
import portaudio.functions

import scala.scalanative.libc.string.memcpy
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
      arrayChunk(pByte, BYTES_PER_BUFFER)
    })
    .flatMap(Pull.output)
    .streamNoScope
    .repeat

private def arrayChunk(pointer: Ptr[Byte], length: Int): Chunk[Float] =
  val array = new Array[Float](length)
  memcpy(array.atUnsafe(0).toBytePointer, pointer, length.toULong)
  Chunk.ArraySlice(array, 0, length)
