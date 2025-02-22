package arpeggio
package pedals.delay

import arpeggio.constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import arpeggio.routing.Fork
import cats.effect.Concurrent
import fs2.{Chunk, Stream}
import cats.syntax.semigroup.*
import arpeggio.routing.parallel
import arpeggio.pedals.passThrough

def silence[F[_]](timeInSeconds: Float): Stream[F, Float] =
  val delayTimeInChunks = (timeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunkArray = Array.fill(FRAMES_PER_BUFFER)(0f)
  val silenceChunk = Chunk.array(silenceChunkArray)
  Stream.chunk(silenceChunk).repeatN(delayTimeInChunks)

def delayLine[F[_]](timeInSeconds: Float): Pedal[F] =
  silence(timeInSeconds) ++ _

def echo[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): Pedal[F] =
  parallel(
    passThrough,
    echoRepeats(repeatGain, delayTimeInSeconds).andThen(_.map(_ * repeatGain))
  )

def echoRepeats[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): Pedal[F] = stream =>
  Stream
    .resource(Fork[F])
    .flatMap(fork =>
      fork.lOut
        .concurrently(
          fork.in(stream |+| fork.rOut.through(delayLine(delayTimeInSeconds)).map(_ * repeatGain))
        )
        .through(delayLine(delayTimeInSeconds))
    )

def allPassStage[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): Pedal[F] = parallel(
  _.map(_ * -repeatGain),
  echoRepeats(repeatGain, delayTimeInSeconds).andThen(_.map(_ * (1 - repeatGain * repeatGain)))
)
