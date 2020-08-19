import java.nio.charset.StandardCharsets

import auth.{ TokenParams, TokenServiceImpl }
import com.typesafe.config.ConfigFactory

object Play extends App {

  val tokenId = java.util.UUID.randomUUID().toString.replaceAll("-", "")

  case class StoreConfig(signingKey: String)

  val config = ConfigFactory.load()

  val storeConfig = StoreConfig(
    signingKey = config.getString("auth.signing-key")
  )

  val tokenService = new TokenServiceImpl(storeConfig.signingKey.getBytes(StandardCharsets.UTF_8))

  val token = tokenService.issueToken(TokenParams("1234567890", None))

  //println(token.serialized)

  println(tokenService.verifyToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTk3ODQzNTI1LCJqdGkiOiIzMWY4Mjc1MGJjYTM0MzdlYTcwM2YwZTIxMzk5ZmVlMiJ9.GbMhI1g32tkpYDcIhy-h62YnpmbkKo9vAT-DUQf_hlg"))

}
