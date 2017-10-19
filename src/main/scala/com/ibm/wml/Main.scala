package com.ibm.wml

import java.io.{FileNotFoundException, InputStream}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.io.Source
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, RawHeader}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by Marius Danciu on 9/21/2017.
  */
object Main extends App with DefaultJsonProtocol { //with SprayJsonSupport {
  val filename = "ibm_df_customer_products_cluster.csv"
  val readmeText = Source.fromResource(filename).getLines.map(_.split(",").map(_.toInt)).toList
  println("Clusters : " + readmeText)

  println("Score with WML ...")

  /**
    * WML Bluemix service credentials:
    *
    * Owner: marius.danciu@ro.ibm.com
    *
    * {
    * "url": "https://ibm-watson-ml.mybluemix.net",
    * "access_key": "xbUq5l1+Iow6TOlTFBu3sYntAKHgUTV23RU96ISe5nuX9zfF1hc7M3L0h5+fnTnWHxGxQ3pIogjgEOjN0TGDTcL0h32gVzPkwMbmHXNpi+FQYUqQmv73SQJrb1WXWeZv",
    * "username": "67555e68-00e3-4859-99e2-8d2cff36d2e4",
    * "password": "72b73a80-9eca-423d-a5d1-33691916ae37",
    * "instance_id": "0e5fc23d-9277-496b-9949-e639a0a336f0"
    * }
    *
    */

  val url = "https://ibm-watson-ml.mybluemix.net"
//  val user = "67555e68-00e3-4859-99e2-8d2cff36d2e4"
//  val password = "72b73a80-9eca-423d-a5d1-33691916ae37"

  // Pixie app using Polong Lin's WatsonML credentials
  val user = "2feca66a-f443-43fc-87ef-712ae93922fc"
  val password = "163637ed-8a0e-4c0d-b95a-e1cefb5bcf3d"
  val instanceId = "648d4fc3-8a42-4f23-825b-714e650ca11c"

  implicit val system = ActorSystem("wml")
  implicit val materializer = ActorMaterializer()

  val authorization = Authorization(BasicHttpCredentials(user, password))

  // Hardcoded on purpose for exemplification
  val predictRecord =
    """{
      | "fields": [
      |   "sum(Baby Food)",
      |   "sum(Diapers)",
      |   "sum(Formula)",
      |   "sum(Lotion)",
      |   "sum(Baby wash)",
      |   "sum(Wipes)",
      |   "sum(Fresh Fruits)",
      |   "sum(Fresh Vegetables)",
      |   "sum(Beer)",
      |   "sum(Wine)",
      |   "sum(Club Soda)",
      |   "sum(Sports Drink)",
      |   "sum(Chips)",
      |   "sum(Popcorn)",
      |   "sum(Oatmeal)",
      |   "sum(Medicines)",
      |   "sum(Canned Foods)",
      |   "sum(Cigarettes)",
      |   "sum(Cheese)",
      |   "sum(Cleaning Products)",
      |   "sum(Condiments)",
      |   "sum(Frozen Foods)",
      |   "sum(Kitchen Items)",
      |   "sum(Meat)",
      |   "sum(Office Supplies)",
      |   "sum(Personal Care)",
      |   "sum(Pet Supplies)",
      |   "sum(Sea Food)",
      |   "sum(Spices)"
      | ],
      | "values": [
      |   [
      |     1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 9
      |   ]
      | ]
      |}
    """.stripMargin
  println(predictRecord)

  val wml = new WMLOps()
  val result = for {
  // Obtain WML Token: GET /v3/identity/token. Same token can be used until it expires and can be renewed. So
  // you should not create a new token on every score request. See http://watson-ml-api.mybluemix.net/?url=token.json for more details.
    token <- wml.getToken(url, user, password)
    // predict with the scoring URL provided (HATEAOS)
    scorred <- wml.predict(token, predictRecord)
  } yield {
    scorred
  }

  result.onComplete {
    case Success(scored) => {
      println("Scored : " + scored.fields)
      for ((k, v) <- scored.fields) printf("Key %s, value %s\n", k, v)
    }
    case Failure(e) => {
      e.printStackTrace()
    }
  }
}

class WMLOps(implicit sys: ActorSystem, mat: ActorMaterializer) {

  def getToken(url: String, user: String, password: String): Future[String] = {
    val authorization = Authorization(BasicHttpCredentials(user, password))

    for {
      HttpResponse(StatusCodes.OK, _, tokenResp, _) <- Http()
        .singleRequest(HttpRequest(uri = s"$url/v3/identity/token", headers = List(authorization)))
      Seq(JsString(token)) <- Unmarshal(tokenResp).to[String].map {
        _.parseJson.asJsObject.getFields("token")
      }
    } yield {
      token
    }
  }

  def predict(token: String, data: String): Future[JsObject] = {
    // This URL is obtained from the WML deployment I created in DSX UI.
    //val scoringUrl = "https://ibm-watson-ml.mybluemix.net/v3/wml_instances/0e5fc23d-9277-496b-9949-e639a0a336f0/published_models/3645131f-52c7-4223-81d4-12150c3a1013/deployments/c7467f3e-5cab-476b-bbf7-be1f06f13d2b/online"
    //var scoringUrl =  "https://ibm-watson-ml.mybluemix.net/v3/wml_instances/648d4fc3-8a42-4f23-825b-714e650ca11c/published_models/763fe896-0102-490a-a824-b3edf2490044/deployments/2451810e-d238-42d9-aa31-ac10ce2f5cca/online"
    var scoringUrl =  "https://ibm-watson-ml.mybluemix.net/v3/wml_instances/648d4fc3-8a42-4f23-825b-714e650ca11c/published_models/0aa7449a-7f39-4746-a8a8-79b9a42642e9/deployments/72dcefb9-1dce-41ae-9286-1c0d3da9c34b/online"
    for {
      body <- Marshal(data.getBytes("utf-8")).to[RequestEntity]
      reps <- Http().singleRequest(HttpRequest(
        method = HttpMethods.POST,
        uri = scoringUrl,
        headers = List(RawHeader("Authorization", s"Bearer $token")),
        entity = body
      ))
      scored <- Unmarshal(reps.entity).to[String]
    } yield {
      scored.parseJson.asJsObject
    }
  }
}
