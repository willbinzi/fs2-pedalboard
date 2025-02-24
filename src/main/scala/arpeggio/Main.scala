package arpeggio

import arpeggio.io.portaudio.PortAudioAudioSuite
import arpeggio.pedals.reverb
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = PortAudioAudioSuite
    .resource[IO]
    .use(audioSuite =>
      audioSuite.input
        .through(
          reverb.schroeder(predelayMillis = 30, decayMillis = 100, mix = 1)
        )
        .through(audioSuite.output)
        .compile
        .drain
    )
