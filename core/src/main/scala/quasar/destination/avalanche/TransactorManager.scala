package quasar.destination.avalanche

import quasar.connector.{Credentials, GetAuth}

import java.lang.String
import java.lang.Exception
import java.lang.Throwable

import scala.{Either, Option, Right, Left, Some, None, Unit}

import cats.MonadError
import cats.implicits._
import cats.effect.Resource
import cats.effect.concurrent.Ref

import doobie.Transactor

import fs2.Hotswap
import quasar.connector.ExternalCredentials
import java.time.Instant
import quasar.connector.Expires
import quasar.lib.jdbc.TransactorConfig
import cats.effect.Clock
import cats.Monad
import cats.effect.Concurrent
import cats.effect.ContextShift
import cats.effect.Timer

class TransactorManager[F[_]] private (hotswap: Hotswap[F, Transactor[F]], token: F[Option[Credentials.Token]], currentTransactor: Ref[F, Transactor[F]]) {
  def transactor: Resource[F, Transactor[F]] = 
    scala.Predef.???
}

object TransactorManager {

  case class Error(message: String) extends Exception(message)

  private def verifyCreds(cred: Credentials): Either[Error, Credentials.Token] = cred match {
    case t: Credentials.Token => Right(t)
    case _ => Left(Error("Unsupported auth type provided by the configured auth key; only `Token` credentials are supported"))
  }

  def acquireValidToken[F[_]: MonadError[?[_], Throwable]: Clock](
      token: F[ExternalCredentials.Temporary[F]])
      : F[Expires[Credentials.Token]] = {
    for {
      ExternalCredentials.Temporary(acquire, renew) <- token
      tempCreds  <- acquire
      (creds, expiresAt) <- tempCreds.nonExpired.flatMap {
        case Some(c) => (c, tempCreds.expiresAt).pure[F]
        case None => 
          renew >>
            acquire
              .flatMap { v => 
                v.nonExpired
                  .flatMap(_.liftTo[F](Error("Failed to acquire a non-expired token")))
                   .map((_, v.expiresAt))
              }
      }
      token <- verifyCreds(creds).liftTo[F]
    } yield Expires(token, expiresAt)
  }

  def apply2[F[_]: Concurrent: Timer: ContextShift](
      auth: AvalancheAuth.ExternalAuth,
      token: F[ExternalCredentials.Temporary[F]],
      buildTransactor: (Email, Credentials.Token) => Resource[F, Transactor[F]])
      : Resource[F, F[Transactor[F]]] = {

    val AvalancheAuth.ExternalAuth(authId, userinfoUri, userinfoField) = auth

    def inner: F[] = ???

    for {
      expiringToken <- Resource.eval(acquireValidToken[F](token))
      client <- Http4sEmberClient.client[F]
      email <- Resource.eval(UserInfoGetter.emailFromUserinfo[F](client, expiringToken.value, userinfoUri, userinfoField)
        .flatMap(_.liftTo[F](Error(
          "Querying user info using the token acquired via the auth key did not yield an email. Check whether the userinfoField is correctly set."))))
      firstTransactor <- buildTransactor(email, expiringToken.value)
      ref <- Resource.eval(Ref[F].of(firstTransactor))
      hotswap <- Hotswap.create[F, Transactor[F]]
    } yield {

      expiringToken.flatMap(_.isExpired).flatMap { isExpired =>
        if (isExpired)
          acquireValidToken(token).flatMap { expiringToken => 


          }


        else
          ref.get

      }

    }

    // for {
    //   ExternalCredentials.Temporary(acquire, renew) <- token
    //   potato @ Expires(creds, expiresAt)  <- acquire
    //   expired <- potato.isExpired
    //   result <- 
    //     if (expired)
    //       renew >>
    //         acquire
    //           .flatMap()
          
    //     else 
    //       ().pure[F]

    //   // result <- creds match {
    //   //   case None => 
    //   //     renew >> 
    //   //       acquire
    //   //         .flatMap(_.nonExpired)
    //   //         .map(_.toRight(Error("Failed to acquire a non-expired token")))
    //   //         .rethrow
    //   //   case Some(t) => 
    //   //     t.pure[F]
    //   // }
    // // } yield result.flatMap(verifyCreds)
    // } yield ()

    scala.Predef.???

  }

  def apply[F[_]: Concurrent: Timer: ContextShift](
      auth: AvalancheAuth.ExternalAuth,
      getAuth: GetAuth[F],
      buildTransactor: (Email, Credentials.Token) => Resource[F, Transactor[F]])
      : Resource[F, TransactorManager[F]] = {

    val token: F[ExternalCredentials.Temporary[F]] = 
      getAuth(auth.authId)
        .flatMap(_.liftTo[F](Error("No credentials were returned by the configured external auth ID")))
        .map {
          case ExternalCredentials.Perpetual(t) =>
            // Shouldn't really happen, but not particularly a problem if it does
            ExternalCredentials.Temporary[F](
              Expires(t, None).pure[F], 
              ().pure[F])
            
          case t: ExternalCredentials.Temporary[F] => t
        }

    apply2[F](auth, token, buildTransactor)

    // (
    //   for {
    //     token <- EitherT(getToken[F](getAuth, authId))
    //     email <- EitherT.fromOptionF(
    //       UserInfoGetter.emailFromUserinfo(token, userinfoUri, userinfoField),
    //       "Querying user info using the token acquired via the auth key did not yield an email. Check the scopes granted to the token.")
    //   } yield AvalancheTransactorConfig.fromToken(connectionUri, Username(email.asString), token)
    // )


    // for {
    //   hotswap <- Hotswap.create[F]
    // }

    scala.Predef.???

  }

}
