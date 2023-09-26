package line.output

import io.pipe
import javax.sound.sampled.{ AudioInputStream, SourceDataLine }

extension (line: SourceDataLine)
  def play(input: AudioInputStream): Unit =
    line.open(input.getFormat)
    line.start()
    pipe(input, line)
    line.drain()
    line.close()
    input.close()
