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
    val statement = conn.createStatement()
    try {


      //      statement.executeUpdate(s"""CREATE TABLE IF NOT EXISTS $table (
      //                                 |   id INT NOT NULL,
      //                                 |   alias VARCHAR ( 255 ),
      //                                 |   content JSON,
      //                                 |   creation_date TIMESTAMP,
      //                                 |   shared_publicly BOOLEAN NOT NULL,
      //                                 |   uid VARCHAR ( 255 ),
      //                                 |   updated_date TIMESTAMP
      //                                 |)""".stripMargin)

      val myOneSet = Seq(sets.filter(set => set.userId == "add74102-24f9-4a6c-8cc1-ede1394d70bf").head)
      println(myOneSet)

      myOneSet.foreach( set => {
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
        //              prep.setInt(1, i) //FIXME no need to set?
        prep.setString(1,  set.tag )
        prep.setObject(2, jsonObject)
        prep.setTimestamp(3, date)
        prep.setBoolean(4, false)
        prep.setString(5, set.userId)
        prep.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()))
        prep.executeUpdate
      })


//      val res = statement.executeQuery(s"""SELECT * from $table WHERE uid = "add74102-24f9-4a6c-8cc1-ede1394d70bf" LIMIT 60 """)
val gg = conn.prepareStatement(s"SELECT * from $table WHERE uid = ? LIMIT 60 ")
      gg.setString(1, "add74102-24f9-4a6c-8cc1-ede1394d70bf")
      val res = gg.executeQuery()
      while( res.next) {
        println(res.getString("id"))
        println(res.getString("alias"))
        println(res.getString("content"))
        println(res.getString("creation_date"))
        println(res.getString("shared_publicly"))
        println(res.getString("uid"))
        println(res.getString("updated_date"))
        println("------------------")
      }

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
