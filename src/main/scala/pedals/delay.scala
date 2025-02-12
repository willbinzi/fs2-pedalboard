package pedals

import cats.effect.Concurrent
import constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import fs2.concurrent.Channel
import fs2.{Chunk, Stream}
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.semigroup.*

def silence[F[_]](timeInSeconds: Float): Stream[F, Chunk[Float]] =
  val delayTimeInChunks = (timeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunk = Chunk.ArraySlice(Array.fill(FRAMES_PER_BUFFER)(0f), 0, FRAMES_PER_BUFFER)
  Stream.emit(silenceChunk).repeatN(delayTimeInChunks)

extension [F[_]](stream: Stream[F, Chunk[Float]])
  def delayed(timeInSeconds: Float): Stream[F, Chunk[Float]] =
    silence(timeInSeconds) ++ stream

def combFilterF[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): F[Pedal[F]] =
  Channel
    .unbounded[F, Chunk[Float]]
    .map(repeatsChannel =>
      stream =>
        (
          stream.chunks |+|
            repeatsChannel.stream
              .map(_ * repeatGain)
              .delayed(delayTimeInSeconds)
        ).evalTap(repeatsChannel.send).unchunks
    )

def allPassFilterF[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): F[Pedal[F]] =
  for {
    outputCopyChannel <- Channel.unbounded[F, Chunk[Float]]
    inputCopyChannel <- Channel.unbounded[F, Chunk[Float]]
  } yield { stream =>
    (
      stream.chunks.evalTap(inputCopyChannel.send).map(_ * (-repeatGain)) |+|
        inputCopyChannel.stream.delayed(delayTimeInSeconds) |+|
        outputCopyChannel.stream.map(_ * repeatGain).delayed(delayTimeInSeconds)
    ).evalTap(outputCopyChannel.send).unchunks
  }
