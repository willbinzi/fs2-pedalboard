package line.output

import io.pipe
import fs2.Pipe
import javax.sound.sampled.{ AudioInputStream, SourceDataLine }
import io.writeSourceDataLine
import cats.effect.kernel.Sync
import javax.sound.sampled.AudioFormat

extension (line: SourceDataLine)
  def play(input: AudioInputStream): Unit =
    line.open(input.getFormat)
    line.start()
    pipe(input, line)
    line.drain()
    line.close()
    input.close()

  def playSamples[F[_]: Sync](format: AudioFormat): Pipe[F, Float, Unit] =
    util.toBytes andThen
      writeSourceDataLine(
        Sync[F].delay {
          line.open(format)
          line.start()
          line
        }
      )
