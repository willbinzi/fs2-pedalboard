package line.input

import javax.sound.sampled.{ AudioFormat, AudioInputStream, TargetDataLine }

extension (line: TargetDataLine)
  def record(format: AudioFormat): AudioInputStream =
    line.open(format)
    line.start()
    new AudioInputStream(line)