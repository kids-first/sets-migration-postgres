import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.postgresql.util.PGobject
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.ws.{BodyWritable, InMemoryBody, StandaloneWSClient}

import java.sql.{DriverManager, SQLException, Timestamp}
import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Utils {
  implicit val system: ActorSystem             = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val wsClient: StandaloneWSClient    = StandaloneAhcWSClient()
  implicit val writeableOf_JsValue: BodyWritable[JsValue] = {
    BodyWritable(a => InMemoryBody(ByteString.fromArrayUnsafe(Json.toBytes(a))), "application/json")
  }

  val json: JsValue = Json.obj(
    "query" -> Json.obj(
      "bool" -> Json.obj(
        "filter" -> Json.obj(
          "exists" -> Json.obj(
            "field" -> "tag"
          )
        ),
        "must_not" -> Json.arr(
          Json.obj(
            "term" -> Json.obj(
              "tag.keyword" -> ""
            )
          )
        )
      )
    )
  )

  def getSetsFromES (esUrl : String): Seq[Set] = {
    //We assume that is not more that 10k Sets. If more, changes to code are required
    val response =
      wsClient.url(esUrl)
        .post(json)

    val resolvedResposne =  Await.result(response, 10.seconds)

    val body = Json.parse(resolvedResposne.body)

    (body \ "hits" \ "hits").as[JsArray].value.flatMap(e => (e \ "_source" ).asOpt[Set]).toSeq
  }

  def insertSetsToPostgres (connection : String, usr: String, pwd: String)( sets: Seq[Set], table: String) = {
    val conn = DriverManager.getConnection(connection, usr, pwd)

    try {
      sets.foreach( set => {
        val content: JsValue = Json.obj(
          "ids" -> Json.toJson(set.ids),
          "riffType" -> "set",
          "setType" -> "participant",
          "sqon" -> set.sqon,
          "idField" -> "kf_id"
        )

        val jsonObject = new PGobject
        jsonObject.setType("json")
        jsonObject.setValue(content.toString())

        val date: Timestamp = new Timestamp(set.createdAt.getTime)
        val prep = conn.prepareStatement(s"INSERT INTO $table (alias, content, creation_date, shared_publicly, uid, updated_date) VALUES ( ?, ?, ? , ?, ?, ?) ")
        prep.setString(1,  set.tag )
        prep.setObject(2, jsonObject)
        prep.setTimestamp(3, date)
        prep.setBoolean(4, false)
        prep.setString(5, set.userId)
        prep.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()))
        prep.executeUpdate
      })


      println("END")
      conn.close()
    }
    catch {
      case sqlEroor: SQLException => println(sqlEroor)
      case e => println(s"Got some other kind of Throwable exception: ${e.toString}")
    } finally {
    }
  }
}
