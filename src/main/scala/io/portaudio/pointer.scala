package io.portaudio

import cats.effect.Resource
import cats.effect.Sync
import cats.syntax.functor.*
import cbindings.portaudio.aliases.{PaError, PaStream, PaStreamFlags}
import cbindings.portaudio.enumerations.PaErrorCode
import cbindings.portaudio.functions
import cbindings.portaudio.structs.PaStreamParameters
import constants.FRAMES_PER_BUFFER

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

val paClipOff = PaStreamFlags(0x00000001.toULong)

private def unsafeOpenStream(
    ppStream: Ptr[Ptr[PaStream]],
    inputParams: Ptr[PaStreamParameters],
    outputParams: Ptr[PaStreamParameters]
): Ptr[PaStream] =
  val err: PaError = functions.Pa_OpenStream(
    stream = ppStream,
    inputParameters = inputParams,
    outputParameters = outputParams,
    sampleRate = constants.SAMPLE_RATE,
    framesPerBuffer = FRAMES_PER_BUFFER.toULong,
    streamFlags = paClipOff,
    streamCallback = null,
    userData = null
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

def inputOutputStreamPointer[F[_]: Sync]: Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay(Zone { implicit z =>
    val inputDevice = functions.Pa_GetDefaultInputDevice()
    val inputLatency =
      (!functions.Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency
    val inputParams = PaStreamParameters(
      device = inputDevice,
      channelCount = 1,
      sampleFormat = paFloat32,
      suggestedLatency = inputLatency,
      hostApiSpecificStreamInfo = null
    )
    val outputDevice = functions.Pa_GetDefaultOutputDevice()
    val outputLatency =
      (!functions.Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency
    val outputParams = PaStreamParameters(
      device = outputDevice,
      channelCount = 1,
      sampleFormat = paFloat32,
      suggestedLatency = outputLatency,
      hostApiSpecificStreamInfo = null
    )
    unsafeOpenStream(stackalloc(), inputParams, outputParams)
  }))(closeStream)
