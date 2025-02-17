package arpeggio.pedals.tremolo

import fs2.{Pure, Stream}

package object waveforms:
  def getWaveform(
      waveformType: WaveFormType,
      cycleLengthInSeconds: Float
  ): Stream[Pure, Float] =
    waveformType match
      case WaveFormType.Sine     => sineWave(cycleLengthInSeconds)
      case WaveFormType.Triangle => triangleWave(cycleLengthInSeconds)
      case WaveFormType.Square   => squareWave(cycleLengthInSeconds)
