package portaudio

import cats.effect.Sync
import scala.scalanative.unsafe.*
import portaudio.aliases.PaStream
import portaudio.aliases.PaError
import portaudio.extern_functions.Pa_OpenDefaultStream
import scalanative.unsigned.UnsignedRichInt
import portaudio.structs.PaStreamCallbackTimeInfo
import portaudio.aliases.PaStreamCallbackFlags
import portaudio.enumerations.PaErrorCode
import scala.scalanative.runtime.Boxes
import portaudio.extern_functions.Pa_CloseStream
import portaudio.extern_functions.Pa_StopStream
import portaudio.extern_functions.Pa_Terminate
import cats.effect.kernel.Resource
import portaudio.extern_functions.Pa_ReadStream
import portaudio.extern_functions.Pa_WriteStream
import portaudio.structs.PaDeviceInfo
import portaudio.extern_functions.Pa_GetDefaultInputDevice
import portaudio.structs.PaStreamParameters
import portaudio.extern_functions.Pa_GetDeviceInfo
import portaudio.extern_functions.Pa_GetDefaultOutputDevice
import portaudio.extern_functions.Pa_OpenStream

val paFloat32 = aliases.PaSampleFormat(0x00000001.toULong)
val paClipOff = aliases.PaStreamFlags(0x00000001.toULong)

val FRAMES_PER_BUFFER = 256

def zone[F[_]: Sync]: Resource[F, Zone]  =
  Resource.make[F, Zone](Sync[F].delay(Zone.open()))(z => Sync[F].delay(z.close()))

def streamPointer[F[_]: Sync](using zone: Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val inputDevice = Pa_GetDefaultInputDevice()
    val inputLatency = (!Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency
    val inputParameters = PaStreamParameters(
      inputDevice,
      1,
      paFloat32,
      inputLatency,
      null
    )
    val outputDevice = Pa_GetDefaultOutputDevice()
    val outputLatency = (!Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency
    val outputParameters = PaStreamParameters(
      outputDevice,
      1,
      paFloat32,
      outputLatency,
      null
    )

    val streamPtrPtr: Ptr[Ptr[PaStream]] = stackalloc()
    val err: PaError = Pa_OpenStream(
      streamPtrPtr,
      inputParameters,
      outputParameters,
      constants.SAMPLE_RATE,
      FRAMES_PER_BUFFER.toULong,
      paClipOff,
      null,
      null
    )

    if err != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream open terminated with exit code $err")
    val e: PaError = functions.Pa_StartStream(!streamPtrPtr)
    if e != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream start terminated with exit code $e")
    val buffer: Ptr[Byte] = stackalloc[Byte](FRAMES_PER_BUFFER * 4)
    while true do
      Pa_ReadStream(!streamPtrPtr, buffer, FRAMES_PER_BUFFER.toULong)
      Pa_WriteStream(!streamPtrPtr, buffer, 256.toULong)
      ()
    !streamPtrPtr
  })(ptr => Sync[F].delay {
    Pa_StopStream(ptr)
    Pa_CloseStream(ptr)
    Pa_Terminate()
    ()
  })
