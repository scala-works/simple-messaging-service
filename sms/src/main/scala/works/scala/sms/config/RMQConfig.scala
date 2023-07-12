package works.scala.sms.config

case class RMQConfig(
    host: String,
    port: Int,
    user: String,
    password: String,
    mgmtUrl: String,
    mgmtUser: String,
    mgmtPassword: String
)
