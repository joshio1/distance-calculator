
// @GENERATOR:play-routes-compiler
// @SOURCE:C:/Omkar/NCSU/SOC/Project 1/play-java-seed/conf/routes
// @DATE:Tue Aug 29 16:15:40 EDT 2017


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
