package portaudio

import cats.effect.Sync
import cats.syntax.functor.*
import fs2.Pipe
import scala.scalanative.unsafe.*
import portaudio.aliases.PaStream
import portaudio.aliases.PaError
import scalanative.unsigned.UnsignedRichInt
import portaudio.enumerations.PaErrorCode
import scala.scalanative.runtime.Boxes
import portaudio.extern_functions.Pa_CloseStream
import portaudio.extern_functions.Pa_StopStream
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
import fs2.Stream

val paFloat32 = aliases.PaSampleFormat(0x00000001.toULong)
val paClipOff = aliases.PaStreamFlags(0x00000001.toULong)

val FRAMES_PER_BUFFER = 256

def zone[F[_]: Sync]: Resource[F, Zone]  =
  Resource.make[F, Zone](Sync[F].delay(Zone.open()))(z => Sync[F].delay(z.close()))

def unsafeOpenStream(
  ppStream: Ptr[Ptr[PaStream]],
  inputParams: Ptr[PaStreamParameters],
  outputParams: Ptr[PaStreamParameters]
  ): Ptr[PaStream] =
    val err: PaError = Pa_OpenStream(
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

def closeStream[F[_]: Sync](pStream: Ptr[PaStream]): F[Unit] =
  Sync[F].delay {
    Pa_StopStream(pStream)
    Pa_CloseStream(pStream)
  }.void

def inputStreamPointer[F[_]: Sync](using zone: Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val inputDevice = Pa_GetDefaultInputDevice()
    val inputLatency = (!Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency
    val inputParams = PaStreamParameters(
      inputDevice,
      1,
      paFloat32,
      inputLatency,
      null
    )
    val ppStream: Ptr[Ptr[PaStream]] = stackalloc()
    unsafeOpenStream(ppStream, inputParams, null)
  })(closeStream)

def outputStreamPointer[F[_]: Sync](using Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val outputDevice = Pa_GetDefaultOutputDevice()
    val outputLatency = (!Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency
    val outputParams = PaStreamParameters(
      outputDevice,
      1,
      paFloat32,
      outputLatency,
      null
    )

    val ppStream: Ptr[Ptr[PaStream]] = stackalloc()
    unsafeOpenStream(ppStream, null, outputParams)
  })(closeStream)

def inputR[F[_]](using Zone)(implicit F: Sync[F]): Resource[F, Stream[F, Float]] =
  inputStreamPointer.map(pStream =>
    val buffer: Ptr[Byte] = alloc[Byte](FRAMES_PER_BUFFER * 4)
    val floatBuffer: Ptr[Float] = Boxes.boxToPtr(Boxes.unboxToPtr(buffer))
    fs2.Pull.eval(F.blocking {
      Pa_ReadStream(pStream, buffer, FRAMES_PER_BUFFER.toULong)
      pointer(floatBuffer, FRAMES_PER_BUFFER)
    }).flatMap(fs2.Pull.output).streamNoScope.repeat
  )

def outputR[F[_]](using Zone)(implicit F: Sync[F]): Resource[F, Pipe[F, Float, Nothing]] =
  outputStreamPointer.map(pStream =>
    _.chunks.foreach { chunk =>
      val floatBuffer: Ptr[Float] =
        if chunk.isInstanceOf[Pointer[Float]] then
          chunk.asInstanceOf[Pointer[Float]].pointer
        else
          // Note: stackalloc here creates horrible noise for some reason
          val floats = alloc[Float](FRAMES_PER_BUFFER)
          for i <- 0 until (chunk.size - 1) do
            floats(i) = chunk(i)
          floats
      val buffer: Ptr[Byte] = Boxes.boxToPtr(Boxes.unboxToPtr(floatBuffer))
      F.blocking {
        Pa_WriteStream(pStream, buffer, FRAMES_PER_BUFFER.toULong)
        ()
      }
    }
  )

case class Pointer[O: Tag](pointer: Ptr[O], offset: Int, length: Int) extends Chunk[O] {
  def size = length
  def apply(i: Int): O =
    if (i < 0 || i >= size) throw new IndexOutOfBoundsException()
    else pointer(offset + i)

  def copyToArray[O2 >: O](xs: Array[O2], start: Int): Unit =
    var i = start
    var j = offset
    val end = size
    while (j < end) {
      xs(i) = pointer(j)
      i += 1
      j += 1
    }


  def splitAtChunk_(n: Int): (Chunk[O], Chunk[O]) =
    Pointer(pointer, offset, n) -> Pointer(pointer, offset + n, length - n)
}

def pointer[O: Tag](pointer: Ptr[O], length: Int): Chunk[O] =
  Pointer(pointer, 0, length)
