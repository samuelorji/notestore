package http.model

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._

object Models {

  implicit val config = JsonConfiguration(SnakeCase)
  case class SignUpUserInput(userId: String)
  object SignUpUserInput {
    implicit val signupReads: Reads[SignUpUserInput] = Json.reads[SignUpUserInput]
  }

}
