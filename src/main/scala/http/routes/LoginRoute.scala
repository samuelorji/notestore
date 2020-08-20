package http.routes

import akka.http.scaladsl.server.Route
import http.controller.LoginController
import http.model.Models.SignUpUserInput

class LoginRoute(lc: LoginController) extends BaseRoute {
  override def route: Route = {

    val signupRoute = (post & path("signup")) {
      def validator = validateJson[SignUpUserInput]
      validator { input =>
        complete(lc.signUpUser(input))
      }
    }

    val stuff = (get & path("stuff")) {
      completeWith(lc.doStuff)
    }
    signupRoute ~ stuff
  }
}
