package arpeggio
package pedals

import fs2.Stream

def volume[F[_]](volumeControlStream: Stream[F, Float]): Pedal[F] =
  _.zipWith(volumeControlStream)(_ * _)
