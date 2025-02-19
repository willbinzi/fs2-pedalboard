package arpeggio
package pedals.delay

import arpeggio.constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import arpeggio.pubsub.ChunkedChannel.*
import cats.effect.Concurrent
import fs2.{Chunk, Stream}
import cats.syntax.semigroup.*

def silence[F[_]](timeInSeconds: Float): Stream[F, Float] =
  val delayTimeInChunks = (timeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunkArray = Array.fill(FRAMES_PER_BUFFER)(0f)
  val silenceChunk = Chunk.array(silenceChunkArray)
  Stream.chunk(silenceChunk).repeatN(delayTimeInChunks)

def combFilter[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): Pedal[F] = stream =>
  Stream
    .eval(
      ChunkedChannel
        .unbounded[F, Float]
    )
    .flatMap(repeatsChannel =>
      (
        stream |+|
          (silence(delayTimeInSeconds) ++ repeatsChannel.stream.map(_ * repeatGain))
      ).through(repeatsChannel.observeSend)
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
