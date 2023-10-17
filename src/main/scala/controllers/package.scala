package controllers

import cats.effect.{Ref, Sync, Temporal}
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import glfw.functions.{glfwSetErrorCallback, glfwGetGamepadState}
import glfw.structs.GLFWgamepadstate
import cats.effect.syntax.resource.*

import scala.concurrent.duration._
import glfw.functions.glfwInit
import scala.scalanative.unsafe.*
import glfw.aliases.GLFWerrorfun
import cats.effect.kernel.Resource
import glfw.functions.glfwTerminate

val GLFW_JOYSTICK_1 = 0
val GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER = 5

def errorCallBack(error: Int, description: CString): Unit =
  System.err.println(s"Error: $error, ${fromCString(description)}}")

def setUp[F[_]: Sync]: Resource[F, Unit] =
  Resource.make(Sync[F].delay {
  glfwSetErrorCallback(GLFWerrorfun(errorCallBack))
  if (glfwInit() != 1)
    throw new IllegalStateException(s"Unable to initialize GLFW")
  ()
})(_ => Sync[F].delay(glfwTerminate()))

def zone[F[_]: Sync]: Resource[F, Zone] =
  Resource.make[F, Zone](Sync[F].delay(Zone.open()))(z =>
    Sync[F].delay(z.close())
  )

def pollControllerStream[F[_]: Sync: Temporal](
  triggerRef: Ref[F, Float],
  // overdriveRef: Ref[F, Boolean],
  // reverbRef: Ref[F, Boolean]
): Resource[F, Stream[F, Float]] =
  for {
    _ <- setUp[F]
    given Zone <- zone[F]
    state <- Sync[F].delay(GLFWgamepadstate()).toResource
  } yield Stream
      .fixedRateStartImmediately(17.millis)
      .zipRight(
        Stream.repeatEval[F, Float](
          go(triggerRef, state)
        )
      )

def go[F[_]: Sync](ref: Ref[F, Float], state: Ptr[GLFWgamepadstate]): F[Float] =
  for {
    connected <- Sync[F].delay(glfwGetGamepadState(GLFW_JOYSTICK_1, state))
    triggerState <- if (connected == 1) updateTriggerState(ref, state) else ref.get
  } yield triggerState

def updateTriggerState[F[_]: Sync](
    ref: Ref[F, Float],
    state: Ptr[GLFWgamepadstate]
): F[Float] =
  // Value is read between -1 and 1, we want between 0 and 1
  val triggerState = (1f - (!state).axes(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)) * 0.5f
  ref.set(triggerState).as(triggerState)
