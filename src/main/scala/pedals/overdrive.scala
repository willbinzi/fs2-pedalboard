package pedals

def overdrive[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => math.min(math.max(sample, -threshold), threshold))

def asymmetricOverdrive[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => math.min(sample, threshold))
