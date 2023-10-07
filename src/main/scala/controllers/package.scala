package controllers

import cats.effect.{ Ref, Sync, Temporal }
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import org.lwjgl.glfw.GLFW.{ GLFW_JOYSTICK_1, GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, glfwInit, glfwSetErrorCallback, glfwGetGamepadState }
import org.lwjgl.glfw.{ GLFWErrorCallback, GLFWGamepadState }

import scala.concurrent.duration._

def setUp[F[_]: Sync]: F[Unit] = Sync[F].delay {
  glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err))
  if (!glfwInit())
    throw new IllegalStateException("Unable to initialize GLFW")
  else ()
}

class TriggerStr[F[_]: Sync : Temporal](ref: Ref[F, Float]) {
  var state: GLFWGamepadState = GLFWGamepadState.create()
  def stream: fs2.Stream[F, Float] =
    Stream.fixedRateStartImmediately(17.millis).zipRight(
      Stream.repeatEval[F, Float](
        go(ref, state)
      )
    )
}

object TriggerStr {
  def apply[F[_]: Sync : Temporal](ref: Ref[F, Float]): Stream[F, Float] = new TriggerStr[F](ref).stream
}

def go[F[_]: Sync](ref: Ref[F, Float], state: GLFWGamepadState): F[Float] =
  for {
    connected <- Sync[F].delay(glfwGetGamepadState(GLFW_JOYSTICK_1, state))
    triggerState <- if (connected) updateTriggerState(ref, state) else ref.get
  } yield triggerState

def updateTriggerState[F[_]: Sync](ref: Ref[F, Float], state: GLFWGamepadState): F[Float] =
  // Value is read between -1 and 1, we want between 0 and 1
  val triggerState = (1f - state.axes(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)) * 0.5f
  ref.set(triggerState).as(triggerState)
