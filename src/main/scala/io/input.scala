package io

import cats.effect.{ Resource, Sync }
import cats.syntax.functor.*
import fs2.{ Pull, Stream }
import portaudio.aliases.PaStream
import portaudio.functions
import portaudio.structs.PaStreamParameters

import scala.reflect.ClassTag
import scala.scalanative.runtime.Boxes
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

private def inputStreamPointer[F[_]: Sync](using zone: Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val inputDevice = functions.Pa_GetDefaultInputDevice()
    val inputLatency = (!functions.Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency
    val inputParams = PaStreamParameters(
      inputDevice,
      1,
      paFloat32,
      inputLatency,
      null
    )
    unsafeOpenStream(stackalloc(), inputParams, null)
  })(closeStream)

def inputR[F[_]](using Zone)(implicit F: Sync[F]): Resource[F, Stream[F, Float]] =
  inputStreamPointer.map(pStream =>
    val floatBuffer: Ptr[Float] = alloc[Float](FRAMES_PER_BUFFER)
    val byteBuffer: Ptr[Byte] = Boxes.boxToPtr(Boxes.unboxToPtr(floatBuffer))
    Pull.eval(F.blocking {
      functions.Pa_ReadStream(pStream, byteBuffer, FRAMES_PER_BUFFER.toULong)
      arrayChunk(floatBuffer, FRAMES_PER_BUFFER)
    }).flatMap(Pull.output).streamNoScope.repeat
  )
