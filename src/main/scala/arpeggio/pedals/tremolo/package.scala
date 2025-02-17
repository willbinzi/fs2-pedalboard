package arpeggio
package pedals

import arpeggio.pedals.tremolo.waveforms.{getWaveform, WaveFormType}

package object tremolo:
  def apply[F[_]](
      waveformType: WaveFormType,
      cycleLengthInSeconds: Float
  ): Pedal[F] =
    _.zipWith(getWaveform(waveformType, cycleLengthInSeconds))(_ * _)
