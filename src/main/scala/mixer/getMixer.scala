package mixer

import javax.sound.sampled.{ AudioSystem, Mixer }

def getMixer(mixerName: String): Mixer =
  AudioSystem
    .getMixerInfo
    .find(_.getName == mixerName).fold(
      throw new RuntimeException(s"No output mixer '$mixerName' found")
    )(AudioSystem.getMixer)