package io

import cats.effect.Resource
import cats.effect.Sync
import cats.syntax.functor.*
import constants.FRAMES_PER_BUFFER
import portaudio.aliases.{PaError, PaStream}
import portaudio.enumerations.PaErrorCode
import portaudio.functions
import portaudio.structs.PaStreamParameters

import scala.scalanative.runtime.Boxes
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

private def unsafeOpenStream(
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

private def closeStream[F[_]: Sync](pStream: Ptr[PaStream]): F[Unit] =
  Sync[F].delay {
    functions.Pa_StopStream(pStream)
    functions.Pa_CloseStream(pStream)
  }.void

def inputOutputStreamPointer[F[_]: Sync](using
    zone: Zone
): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val inputDevice = functions.Pa_GetDefaultInputDevice()
    val inputLatency =
      (!functions.Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency
    val inputParams = PaStreamParameters(
      inputDevice,
      1,
      paFloat32,
      inputLatency,
      null
    )
    val outputDevice = functions.Pa_GetDefaultOutputDevice()
    val outputLatency =
      (!functions.Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency
    val outputParams = PaStreamParameters(
      outputDevice,
      1,
      paFloat32,
      outputLatency,
      null
    )
    unsafeOpenStream(stackalloc(), inputParams, outputParams)
  })(closeStream)

extension (pFloat: Ptr[Float])
  def toBytePointer: Ptr[Byte] =
    Boxes.boxToPtr(Boxes.unboxToPtr(pFloat))
