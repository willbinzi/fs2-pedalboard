package line.input

import fs2.io.readInputStream
import fs2.Stream

import cats.effect.kernel.Resource
import javax.sound.sampled.{ AudioFormat, AudioInputStream, TargetDataLine }
import cats.effect.kernel.Sync
import cats.syntax.functor.*
import java.io.InputStream

val BUFFER_SIZE: Int = 4096

extension (line: TargetDataLine)
  def inputStream[F[_]: Sync](format: AudioFormat): F[AudioInputStream] =
    Sync[F].delay {
      line.open(format)
      line.start()
      new AudioInputStream(line)
    }

  def inputStreamResource[F[_]: Sync](format: AudioFormat): Resource[F, AudioInputStream] =
    Resource.make(Sync[F].delay {
      line.open(format)
      line.start()
      new AudioInputStream(line)
    })( (_: AudioInputStream) => Sync[F].delay {
      line.stop()
      line.close()
    })

  def captureSamples[F[_]: Sync](format: AudioFormat): Stream[F, Float] =
    captureBytes(format).through(util.toSamples)

  def captureBytes[F[_]: Sync](format: AudioFormat): Stream[F, Byte] =
    readInputStream(inputStream(format).widen[InputStream], BUFFER_SIZE)
