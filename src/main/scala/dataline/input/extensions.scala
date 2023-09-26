package dataline.input

import cats.effect.kernel.Sync
import cats.syntax.functor.*
import fs2.io.readInputStream
import fs2.Stream

import java.io.InputStream
import javax.sound.sampled.{ AudioFormat, AudioInputStream, TargetDataLine }

val BUFFER_SIZE: Int = 4096

extension (line: TargetDataLine)
  def inputStream[F[_]: Sync](format: AudioFormat): F[AudioInputStream] =
    Sync[F].delay {
      line.open(format)
      line.start()
      new AudioInputStream(line)
    }

  def captureSamples[F[_]: Sync](format: AudioFormat): Stream[F, Float] =
    captureBytes(format).through(util.toSamples)

  def captureBytes[F[_]: Sync](format: AudioFormat): Stream[F, Byte] =
    readInputStream(inputStream(format).widen[InputStream], BUFFER_SIZE)
