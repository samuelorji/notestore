package object auth {

  case class Token(id: String, userId: String, issuedAt: String, expiresAt: Option[String], serialized: String)

  case class TokenParams(userId: String, lifetime: Option[Int] = None)

  sealed trait TokenError {
    val errorMessage: String
  }

  case class GenericTokenError(errorMessage: String) extends TokenError
  case class InvalidToken(token: String, errorMessage: String) extends TokenError
  case class TokenExpired(token: String, errorMessage: String) extends TokenError
  case class TokenNotExistent(token: String, errorMessage: String) extends TokenError

  sealed trait ValidationError {
    val errorMessage: String
  }

  case class UserNameNotFound(errorMessage: String) extends ValidationError
  case class PasswordIncorrect(errorMessage: String) extends ValidationError
}
