package ua.kpi.ipsa

import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.quick._
import ua.kpi.ipsa.MainApp.appLayer
import ua.kpi.ipsa.domain.types.{HotelStarDescription, HotelStarRegion, HotelStars}
import ua.kpi.ipsa.dto.{ApiCreateHotelStarCategory, ApiHotelStarCategory, ApiUpdateHotelStarCategory}
import zio._
import zio.json._
import zio.test.Assertion.equalTo
import zio.test.assert

object HotelStarsFunctionalTest extends BaseFunTest {

  override def spec = suite("Hotel Stars Management")(
    testM("check create") {
      (for {
        _ <- evalDb("hotel_stars/clean_db.sql")
        _ <- MainApp.appProgramResource
        b <- AsyncHttpClientZioBackend().toManaged_
      } yield {
        for {
          created <- c.post(uri"http://localhost:8093/api/v1.0/hotel_stars").body(ApiCreateHotelStarCategory(HotelStars(5), HotelStarDescription("5 star Egypt"), HotelStarRegion("Egypt")).toJson).send(b).flatMap(asHotel)
        } yield {
          assert(created.stars)(equalTo(HotelStars(5))) &&
          assert(created.description)(equalTo(HotelStarDescription("5 star Egypt"))) &&
          assert(created.region)(equalTo(HotelStarRegion("Egypt")))
        }
      }).use(identity).provideLayer(appLayer)
    },
    testM("check get single") {
      (for {
        _ <- evalDb("hotel_stars/clean_db.sql")
        _ <- MainApp.appProgramResource
        b <- AsyncHttpClientZioBackend().toManaged_
      } yield {
        for {
          notFound <- c.get(uri"http://localhost:8093/api/v1.0/hotel_stars/123").send(b)
          created  <- c.post(uri"http://localhost:8093/api/v1.0/hotel_stars").body(ApiCreateHotelStarCategory(HotelStars(5), HotelStarDescription("5 star Egypt"), HotelStarRegion("Egypt")).toJson).send(b).flatMap(asHotel)
          loaded   <- c.get(uri"http://localhost:8093/api/v1.0/hotel_stars/${created.id}").send(b).flatMap(asHotel)
        } yield {
          assert(notFound.statusText)(equalTo("Not Found")) &&
          assert(loaded)(equalTo(created))
        }
      }).use(identity).provideLayer(appLayer)
    },
    testM("check get list") {
      (for {
        _ <- evalDb("hotel_stars/clean_db.sql")
        _ <- MainApp.appProgramResource
        b <- AsyncHttpClientZioBackend().toManaged_
      } yield {
        for {
          emptyList    <- c.get(uri"http://localhost:8093/api/v1.0/hotel_stars").send(b)
          created      <- c.post(uri"http://localhost:8093/api/v1.0/hotel_stars").body(ApiCreateHotelStarCategory(HotelStars(5), HotelStarDescription("5 star Egypt"), HotelStarRegion("Egypt")).toJson).send(b).flatMap(asHotel)
          nonEmptyList <- c.get(uri"http://localhost:8093/api/v1.0/hotel_stars").send(b).flatMap(asHotelList)
        } yield {
          assert(emptyList.statusText)(equalTo("OK")) &&
          assert(emptyList.body)(equalTo("[]")) && assert(created.stars)(equalTo(HotelStars(5))) &&
          assert(nonEmptyList)(equalTo(List(created)))
        }
      }).use(identity).provideLayer(appLayer)
    },
    testM("check update") {
      (for {
        _ <- evalDb("hotel_stars/clean_db.sql")
        _ <- MainApp.appProgramResource
        b <- AsyncHttpClientZioBackend().toManaged_
      } yield {
        for {
          notFoundUpdate <- c.put(uri"http://localhost:8093/api/v1.0/hotel_stars/123").body(ApiUpdateHotelStarCategory(HotelStars(5), HotelStarDescription("5 star Egypt Hurghada"), HotelStarRegion("Egypt Hurghada")).toJson).send(b)
          created        <- c.post(uri"http://localhost:8093/api/v1.0/hotel_stars").body(ApiCreateHotelStarCategory(HotelStars(5), HotelStarDescription("5 star Egypt"), HotelStarRegion("Egypt")).toJson).send(b).flatMap(asHotel)
          updated <-
            c.put(uri"http://localhost:8093/api/v1.0/hotel_stars/${created.id}").body(ApiUpdateHotelStarCategory(HotelStars(5), HotelStarDescription("5 star Egypt Hurghada"), HotelStarRegion("Egypt Hurghada")).toJson).send(b).flatMap(asHotel)
        } yield {
          assert(notFoundUpdate.statusText)(equalTo("Not Found")) &&
          assert(updated.stars)(equalTo(HotelStars(5))) &&
          assert(updated.description)(equalTo(HotelStarDescription("5 star Egypt Hurghada"))) &&
          assert(updated.region)(equalTo(HotelStarRegion("Egypt Hurghada")))
        }
      }).use(identity).provideLayer(appLayer)
    }
  )

  private def asHotel(response: Response[String]): IO[String, ApiHotelStarCategory] =
    ZIO.fromEither(response.body.fromJson[ApiHotelStarCategory].left.map(_ => s"failed construct from: ${response}"))
  private def asHotelList(response: Response[String]): IO[String, List[ApiHotelStarCategory]] =
    ZIO.fromEither(response.body.fromJson[List[ApiHotelStarCategory]].left.map(_ => s"failed construct from: ${response}"))

}
