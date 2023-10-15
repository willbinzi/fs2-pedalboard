package io

import cats.effect.Sync
import cats.syntax.functor.*
import fs2.Chunk
import portaudio.aliases.{ PaError, PaStream }
import portaudio.enumerations.PaErrorCode
import portaudio.functions
import portaudio.structs.PaStreamParameters

import scala.reflect.ClassTag
import scala.scalanative.runtime.Boxes
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

val FRAMES_PER_BUFFER = 256

private[io] def unsafeOpenStream(
  ppStream: Ptr[Ptr[PaStream]],
  inputParams: Ptr[PaStreamParameters],
  outputParams: Ptr[PaStreamParameters]
  ): Ptr[PaStream] =
    val err: PaError = functions.Pa_OpenStream(
      ppStream,
      inputParams,
      outputParams,
      constants.SAMPLE_RATE,
      FRAMES_PER_BUFFER.toULong,
      paClipOff,
      null,
      null
    )

    if err != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream open terminated with exit code $err")
    val e: PaError = functions.Pa_StartStream(!ppStream)
    if e != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream start terminated with exit code $e")
    !ppStream

private[io] def closeStream[F[_]: Sync](pStream: Ptr[PaStream]): F[Unit] =
  Sync[F].delay {
    functions.Pa_StopStream(pStream)
    functions.Pa_CloseStream(pStream)
  }.void

def arrayChunk[O: Tag: ClassTag](pointer: Ptr[O], length: Int): Chunk[O] =
  val array = new Array[O](length)
  (0 until length).foreach(i =>
    array(i) = pointer(i)
  )
  Chunk.array(array)

extension (pFloat: Ptr[Float])
  def toBytePointer: Ptr[Byte] =
    Boxes.boxToPtr(Boxes.unboxToPtr(pFloat))
