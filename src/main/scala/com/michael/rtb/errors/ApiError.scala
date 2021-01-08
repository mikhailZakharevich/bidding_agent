package com.michael.rtb.errors

sealed trait ApiError extends Throwable

object ApiError {

  case class NotFoundError(message: String) extends ApiError

}
