package mixer

import cats.effect.kernel.Sync

import javax.sound.sampled.{ AudioSystem, Mixer }

def getMixer[F[_]: Sync](mixerName: String): F[Mixer] =
  Sync[F].delay {
    AudioSystem
      .getMixerInfo
      .find(_.getName == mixerName).fold(
        throw new RuntimeException(s"No output mixer '$mixerName' found")
      )(info => AudioSystem.getMixer(info))
  }