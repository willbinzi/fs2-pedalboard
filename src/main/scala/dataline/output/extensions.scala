package dataline.output

import cats.effect.kernel.Sync
import fs2.Pipe
import io.writeSourceDataLine

import javax.sound.sampled.{ AudioFormat, SourceDataLine }

val BUFFER_SIZE: Int = 4096

extension (line: SourceDataLine)
  def playSamples[F[_]: Sync](format: AudioFormat): Pipe[F, Float, Unit] =
    util.toBytes(new Array[Byte](BUFFER_SIZE)) andThen
      writeSourceDataLine(
        Sync[F].delay {
          line.open(format)
          line.start()
          line
        }
      )
