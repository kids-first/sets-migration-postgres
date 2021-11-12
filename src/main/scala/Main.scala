object Main extends App{

  val Array(esUrl, con_str, user, pwd, table) = args

  val sets = Utils.getSetsFromES(esUrl)

  Utils.insertSetsToPostgres(con_str, user, pwd)(sets, table)
}
