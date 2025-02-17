package arpeggio.io.portaudio

import arpeggio.constants
import cats.effect.Resource
import cats.effect.Sync
import cats.syntax.functor.*
import cbindings.portaudio.aliases.{PaError, PaStream}
import cbindings.portaudio.enumerations.PaErrorCode
import cbindings.portaudio.functions
import cbindings.portaudio.structs.PaStreamParameters

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

private def inputStreamParams(using Zone): Ptr[PaStreamParameters] =
  val inputDevice = functions.Pa_GetDefaultInputDevice()
  PaStreamParameters(
    device = inputDevice,
    channelCount = 1,
    sampleFormat = PaSampleFormat.paFloat32,
    suggestedLatency = (!functions.Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency,
    hostApiSpecificStreamInfo = null
  )

private def outputStreamParams(using Zone): Ptr[PaStreamParameters] =
  val outputDevice = functions.Pa_GetDefaultOutputDevice()
  PaStreamParameters(
    device = functions.Pa_GetDefaultOutputDevice(),
    channelCount = 1,
    sampleFormat = PaSampleFormat.paFloat32,
    suggestedLatency = (!functions.Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency,
    hostApiSpecificStreamInfo = null
  )

def defaultPaStream[F[_]](using
    F: Sync[F]
): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](F.delay(Zone { implicit z =>
    val ppStream: Ptr[Ptr[PaStream]] = stackalloc()
    val err: PaError = functions.Pa_OpenStream(
      stream = ppStream,
      inputParameters = inputStreamParams,
      outputParameters = outputStreamParams,
      sampleRate = constants.SAMPLE_RATE,
      framesPerBuffer = constants.FRAMES_PER_BUFFER.toULong,
      streamFlags = PaStreamFlags.paClipOff,
      streamCallback = null,
      userData = null
    )

    if err != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream open terminated with exit code $err")
    val e: PaError = functions.Pa_StartStream(!ppStream)
    if e != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream start terminated with exit code $e")
    !ppStream
  }))(pStream =>
    F.delay {
      functions.Pa_StopStream(pStream)
      functions.Pa_CloseStream(pStream)
    }.void
  )
