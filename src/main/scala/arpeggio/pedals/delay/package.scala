package arpeggio
package pedals.delay

import arpeggio.constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import arpeggio.pubsub.ChunkedChannel.*
import arpeggio.routing.Fork
import cats.effect.Concurrent
import fs2.{Chunk, Stream}
import cats.syntax.semigroup.*

def silence[F[_]](timeInSeconds: Float): Stream[F, Float] =
  val delayTimeInChunks = (timeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunkArray = Array.fill(FRAMES_PER_BUFFER)(0f)
  val silenceChunk = Chunk.array(silenceChunkArray)
  Stream.chunk(silenceChunk).repeatN(delayTimeInChunks)

def delayLine[F[_]](timeInSeconds: Float): Pedal[F] =
  silence(timeInSeconds) ++ _

def combFilter[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): Pedal[F] = stream =>
  Stream
    .resource(Fork.throttled[F].both(Fork[F]))
    .flatMap((fork1, fork2) =>
      (fork1.lOut |+| fork2.lOut)
        .concurrently(fork1.in(stream))
        .concurrently(
          fork2.in(
            (fork1.rOut |+| fork2.rOut).through(delayLine(delayTimeInSeconds)).map(_ * repeatGain)
          )
        )
    )

def allPassFilter[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): Pedal[F] = stream =>
  for {
    outputCopyChannel <- Stream.eval(ChunkedChannel.unbounded[F, Float])
    inputCopyChannel <- Stream.eval(ChunkedChannel.unbounded[F, Float])
    result <- (
      stream.through(inputCopyChannel.observeSend).map(_ * -repeatGain) |+|
        (silence(delayTimeInSeconds) ++ inputCopyChannel.stream) |+|
        (silence(delayTimeInSeconds) ++ outputCopyChannel.stream.map(_ * repeatGain))
    ).through(outputCopyChannel.observeSend)
  } yield result
