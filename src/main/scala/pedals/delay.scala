package pedals

import cats.effect.{ Concurrent, Resource }
import constants.{ CHUNKS_PER_SECOND, FLOAT_BUFFER_SIZE }
import fs2.concurrent.Channel
import fs2.{ Chunk, Stream }
import cats.syntax.functor.*
import cats.syntax.semigroup.*

def delay[F[_]](delayTimeInSeconds: Float, feedback: Float): Pedal[F] =
  val delayTimeInChunks = (delayTimeInSeconds * CHUNKS_PER_SECOND).toInt
  val delayBuffer = Array.fill[Chunk[Float]](delayTimeInChunks)(Chunk.array(Array.fill(FLOAT_BUFFER_SIZE)(0f)))
  _.scanChunks(0) { (n, chunk) =>
    val i = n % delayTimeInChunks
    val wetChunk = chunk.zipWith(delayBuffer(i))( _ + _ * feedback)
    delayBuffer(i) = wetChunk
    ((n + 1) % delayTimeInChunks, wetChunk)
  }

def delaySilence[F[_]](delayTimeInSeconds: Float): Stream[F, Chunk[Float]] =
  val delayTimeInChunks = (delayTimeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunkArray = Array.fill(FLOAT_BUFFER_SIZE)(0f)
  val silenceChunk = Chunk.array(silenceChunkArray)
  Stream.emit(silenceChunk).repeatN(delayTimeInChunks)

def delayR[F[_]: Concurrent](repeatGain: Float, delayTimeInSeconds: Float): Resource[F, Pedal[F]] =
  for {
    repeatsChannel <- Resource.eval(Channel.unbounded[F, Chunk[Float]])
  } yield (
    stream =>
      (stream.chunks.zipWith(delaySilence(delayTimeInSeconds) ++ repeatsChannel.stream.map(_.map(_ * repeatGain)))( _.zip(_).map(_ + _) ))
        .evalTap(chunk => repeatsChannel.send(chunk).void)
        .unchunks
  )

def delayF[F[_]: Concurrent](repeatGain: Float, delayTimeInSeconds: Float): F[Pedal[F]] =
  for {
    repeatsChannel <- Channel.unbounded[F, Float]
  } yield (
    stream =>
      stream
        |+| (delaySilence(delayTimeInSeconds).unchunks ++ repeatsChannel.stream.map(_ * repeatGain))
        .evalTap(sample => repeatsChannel.send(sample).void)
  )
