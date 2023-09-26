package pedals

def sineWave(timestamp: Int, cycleLength: Double, frameRate: Float): Float =
  Math.sin(timestamp / cycleLength * 2 * Math.PI).toFloat
