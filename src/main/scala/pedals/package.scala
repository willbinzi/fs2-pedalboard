package pedals

import fs2.{Pipe, Stream}

type Pedal[F[_]] = Pipe[F, Float, Float]

// Noop pedal - just passes through the input
def passThrough[F[_]] = identity[Stream[F, Float]]
