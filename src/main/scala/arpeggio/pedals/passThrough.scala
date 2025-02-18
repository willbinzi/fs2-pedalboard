package arpeggio
package pedals

import fs2.Stream

// Noop pedal - just passes through the input
def passThrough[F[_]]: Pedal[F] = identity[Stream[F, Float]]
