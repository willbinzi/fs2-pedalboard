package mixer

import javax.sound.sampled.{ Mixer, SourceDataLine, TargetDataLine }

extension (mixer: Mixer)
  def getSourceDataLine: SourceDataLine =
    mixer
      .getSourceLineInfo
      .headOption
      .fold(
        throw new RuntimeException("No source data line (output) found")
      )(info => mixer.getLine(info).asInstanceOf[SourceDataLine])

  def getTargetDataLine: TargetDataLine =
    mixer
      .getTargetLineInfo
      .headOption
      .fold(
        throw new RuntimeException("No target data line (input) found")
      )(info => mixer.getLine(info).asInstanceOf[TargetDataLine])
