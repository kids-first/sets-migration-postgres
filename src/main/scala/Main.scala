object Main extends App{

//  val Array(esUrl, con_str, user, pwd, table) = args
    val esUrl = "https://vpc-kf-arranger-es-service-isu6667eq7avejkhhenlu6fopu.us-east-1.es.amazonaws.com/arranger-sets/_search?size=20"

    val con_str = "jdbc:postgresql://kf-riff-postgres-service.kf-strides.org:5432/riff"
    val user = "riff-svc"
    val pwd = "30PCSStJQL"
    val table = "riff"

  //  val con_str = "jdbc:postgresql://localhost:5555/"
  //  val user = "postgres"
  //  val pwd = "mysecretpassword"
  //  val table = "riff"

  val sets = Utils.getSetsFromES(esUrl)

  Utils.insertSetsToPostgres(con_str, user, pwd)(sets, table)
}
