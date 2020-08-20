package start

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import auth.{ AuthService, RedisTokenStorage, TokenServiceImpl }
import com.typesafe.config.ConfigFactory
import http.controller.LoginControllerImpl
import http.routes.LoginRoute
import redis.RedisClient

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object MainApplication extends App {

  implicit val sys = ActorSystem("NoteStore")

  case class StoreConfig(signingKey: String)

  //    lazy val config = ConfigFactory.load()
  //
  //    val storeConfig = StoreConfig(
  //      signingKey = config.getString("auth.signing-key")
  //    )

  val st = "somekeyeyeyeyyeyeeyeyeyoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoeoe"

  val loginControllerThreadPool = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  val tokenThreadPool = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  val client = RedisClient("127.0.0.1", 6379)
  val tokenStorage = new RedisTokenStorage(client)
  val tokenService = new TokenServiceImpl(st.getBytes, tokenStorage)(tokenThreadPool)
  val authService = new AuthService(tokenService)
  val loginController = new LoginControllerImpl(authService)(loginControllerThreadPool)
  val routes = {
    new LoginRoute(loginController).route
  }

  Http().bindAndHandle(routes, "localhost", 9200) //.bind(routes)

}
