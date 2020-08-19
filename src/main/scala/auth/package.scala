package object auth {

  case class Token(id: String, userId: String, issuedAt: String, expiresAt: Option[String], serialized: String)

  case class TokenParams(userId: String, lifetime: Option[Int])

  sealed trait TokenError {
    val errorMessage: String
  }
  case class InvalidToken(token: String, errorMessage: String) extends TokenError
  case class TokenExpired(token: String, errorMessage: String) extends TokenError
}
