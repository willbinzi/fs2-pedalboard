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
import fs2.Chunk
import scala.reflect.ClassTag

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
    !streamPtrPtr
  })(ptr => Sync[F].delay {
    Pa_StopStream(ptr)
    Pa_CloseStream(ptr)
    Pa_Terminate()
    ()
  })

def foo[F[_]](pStream: Ptr[PaStream])(using Zone)(implicit F: Sync[F]): fs2.Stream[F, Float] =
  val buffer: Ptr[Byte] = alloc[Byte](FRAMES_PER_BUFFER * 4)
  val floatBuffer: Ptr[Float] = Boxes.boxToPtr(Boxes.unboxToPtr(buffer))
  fs2.Pull.eval(F.blocking {
    Pa_ReadStream(pStream, buffer, FRAMES_PER_BUFFER.toULong)
    pointer(floatBuffer, FRAMES_PER_BUFFER)
  }).flatMap(fs2.Pull.output).streamNoScope.repeat

def bar[F[_]](pStream: Ptr[PaStream])(using Zone)(implicit F: Sync[F]): fs2.Pipe[F, Float, Nothing] =
  val floatBuffer: Ptr[Float] = alloc[Float](FRAMES_PER_BUFFER)
  val buffer: Ptr[Byte] = Boxes.boxToPtr(Boxes.unboxToPtr(floatBuffer))
  _.chunks.foreach { chunk =>
    F.blocking {
      for i <- 0 until (chunk.size - 1) do
        floatBuffer(i) = chunk(i)
      Pa_WriteStream(pStream, buffer, FRAMES_PER_BUFFER.toULong)
      ()
    }
  }

case class Pointer[O: Tag](values: Ptr[O], offset: Int, length: Int) extends Chunk[O] {
  def size = length
  def apply(i: Int): O =
    if (i < 0 || i >= size) throw new IndexOutOfBoundsException()
    else values(offset + i)

  def copyToArray[O2 >: O](xs: Array[O2], start: Int): Unit =
    var i = start
    var j = offset
    val end = size
    while (j < end) {
      xs(i) = values(j)
      i += 1
      j += 1
    }


  def splitAtChunk_(n: Int): (Chunk[O], Chunk[O]) =
    Pointer(values, offset, n) -> Pointer(values, offset + n, length - n)
}

def pointer[O: Tag](values: Ptr[O], length: Int): Chunk[O] =
  Pointer(values, 0, length)
