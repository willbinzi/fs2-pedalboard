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

val paFloat32 = aliases.PaSampleFormat(0x00000001.toULong)

def zone[F[_]: Sync]: Resource[F, Zone]  =
  Resource.make[F, Zone](Sync[F].delay(Zone.open()))(z => Sync[F].delay(z.close()))

def streamPointer[F[_]: Sync](using zone: Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay{
    val streamPtrPtr: Ptr[Ptr[PaStream]] = alloc() // or alloc to put it on the heap? I genuinely have no idea what is more appropriate

    val err: PaError = Pa_OpenDefaultStream(
      streamPtrPtr,
      1,
      1,
      paFloat32,
      constants.SAMPLE_RATE,
      256.toULong,
      // TODO: WTF???
      // Boxes.boxToPtr(Boxes.unboxToPtr(CFuncPtr.toPtr(playSawtoothCallback))),
      Boxes.boxToPtr(Boxes.unboxToPtr(CFuncPtr.toPtr(passthroughCallback))),
      null
      // Boxes.boxToPtr[Byte](Boxes.unboxToPtr(userDataPointer))
    )
    println("Opened stream")
    if err != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream open terminated with exit code $err")
    val e: PaError = functions.Pa_StartStream(!streamPtrPtr)
    if e != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream start terminated with exit code $e")
    println("Started stream")
    !streamPtrPtr
  })(ptr => Sync[F].delay {
    Pa_StopStream(ptr)
    println("Stopped stream")
    Pa_CloseStream(ptr)
    println("Closed stream")
    Pa_Terminate()
    println("Terminated portaudio")
  })


def passthrough(
  input: Ptr[Byte],
  output: Ptr[Byte],
  frameCount: CUnsignedLongInt,
  timeInfo: Ptr[PaStreamCallbackTimeInfo],
  statusFlags: PaStreamCallbackFlags,
  userData: Ptr[Byte]
): Int =
  var in: Ptr[Float] = Boxes.boxToPtr[Float](Boxes.unboxToPtr(input))
  var out: Ptr[Float] = Boxes.boxToPtr[Float](Boxes.unboxToPtr(output))
  for (i <- 0 to frameCount.toInt - 1)
    !out = !in
    in = in + 1
    out = out + 1
  0

def sawtoothFn(
  input: Ptr[Byte],
  output: Ptr[Byte],
  frameCount: CUnsignedLongInt,
  timeInfo: Ptr[PaStreamCallbackTimeInfo],
  statusFlags: PaStreamCallbackFlags,
  userData: Ptr[Byte]
): Int =
  val data: Ptr[CStruct3[Float, Float, Long]] =
    Boxes.boxToPtr[CStruct3[Float, Float, Long]](Boxes.unboxToPtr(userData))
  var out: Ptr[Float] =
    Boxes.boxToPtr[Float](Boxes.unboxToPtr(output))
  // Technically this could cause weird behaviour if up if the frameCount is too big to be an Int
  for (i <- 0 to frameCount.toInt - 1)
    out = out + 1
    !out = data._1
    out = out + 1
    !out = data._2

    // Input for next iteration
    data._1 = data._1 + 0.001f
    // When signal reaches top, drop back down
    if data._1 >= 0.2f then data._1 = -0.2f
    // Higher pitch to distinguish left from right
    data._2 = data._2 + 0.003f
    // When signal reaches top, drop back down
    if data._2 >= 0.2f then data._2 = -0.2f
  data._3 = data._3 +1
  println(s"Call count: ${data._3}")
  0

def passthroughCallback: aliases.PaStreamCallback =
  CFuncPtr6.fromScalaFunction(passthrough)

def playSawtoothCallback: aliases.PaStreamCallback =
  CFuncPtr6.fromScalaFunction(sawtoothFn)
