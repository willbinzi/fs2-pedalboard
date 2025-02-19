package arpeggio

import arpeggio.io.portaudio.PortAudioAudioSuite
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = PortAudioAudioSuite
    .resource[IO]
    .use(audioSuite =>
      audioSuite.input
        .through(pedals.overdrive.blended(0.7, 0.1))
        .through(pedals.reverb.reverb(0.7, 0.1))
        .through(audioSuite.output)
        .compile
        .drain
    )
