import play.api.libs.json.{JsPath, JsValue, Json, Reads, Writes}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import java.util.Date

case class Set(
                setId: String,
                createdAt: Date,
                path: String,
                size: Int,
                sqon: JsValue,
                ids: Seq[String] = Nil,
                tag: String,
                _type: String,
                userId: String
              )

object Set{
  implicit val residentWrites: Writes[Set] = new Writes[Set] {
    def writes(set: Set): JsObject = Json.obj(
      "setId" -> set.setId,
      "createdAt"  -> set.createdAt,
      "path"  -> set.path,
      "size" -> set.size,
      "sqon" -> set.sqon,
      "ids" -> set.ids,
      "tag" -> set.tag,
      "type" -> set._type,
      "userId" -> set.userId,
    )
  }

  implicit val locationReads: Reads[Set] = (
    (JsPath \ "setId").read[String] and
      (JsPath \ "createdAt").read[Date] and
      (JsPath \ "path").read[String] and
      (JsPath \ "size").read[Int] and
      (JsPath \ "sqon").read[JsValue] and
      (JsPath \ "ids").read[Seq[String]] and
      (JsPath \ "tag").read[String] and
      (JsPath \ "type").read[String] and
      (JsPath \ "userId").read[String]
    )(Set.apply _)

}
