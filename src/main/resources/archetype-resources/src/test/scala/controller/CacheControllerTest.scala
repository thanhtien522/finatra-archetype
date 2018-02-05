package ${package}.controller

import com.twitter.finagle.Thrift
import com.twitter.finagle.http.Status
import com.twitter.finagle.http.Status.Ok
import com.twitter.finagle.service.RetryBudget
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.inject.server.FeatureTestMixin
import com.twitter.util.{Duration, Future}
import org.scalatest.{Assertions, FunSpec}

import ${package}.Server
import ${package}.domain.thrift.{TUserID, TUserInfo}
import ${package}.service.TUserCacheService


/**
  * Created by SangDang on 9/18/16.
  */
class CacheControllerTest extends FunSpec with FeatureTestMixin {
  override protected val server = new EmbeddedHttpServer(twitterServer = new Server) with ThriftClient

  describe("[HTTP] Put cache") {
    it("successfull") {
      server.httpPost(
        path = "/addUser",
        postBody =
          """
            {
              "user_id":{
                "id":"1"
              },
              "user_info":{
                "user_id":{
                  "id":"1"
                },
                "user_name":"test_1",
                "age":99,
                "sex":"male"
              }
            }
          """.stripMargin,
        andExpect = Ok
      )
    }
    it("be able to get back") {
      server.httpGet(
        path = "/getUser?user_id=1",
        andExpect = Status.Ok,
        withJsonBody =
          """
            {
              "user_id": {
                "id": "1"
              },
              "user_name": "test_1",
              "age": 99,
              "sex": "male"
            }
          """.stripMargin

      )
    }
  }

  describe("[Thrift]") {

    lazy val client = server.thriftClient[TUserCacheService[Future]](clientId = "1")
    lazy val externalClient = Thrift.client
      .withRequestTimeout(Duration.fromSeconds(1))
      .withRetryBudget(RetryBudget())
      .build[TUserCacheService.MethodPerEndpoint](s"localhost:${server.thriftExternalPort}", "unit_test_client")

    it("put cache successful") {
      client.addUser(TUserInfo(TUserID("101"), "test", 100, "male"))
      client.getUser(TUserID("101")).onSuccess(userInfo => {
        Assertions.assert(userInfo.userId.id.equals("101"))
        Assertions.assert(userInfo.username.equals("test"))
        Assertions.assert(userInfo.age.equals(100))
        Assertions.assert(userInfo.sex.equals("male"))
      }).onFailure(fn => throw fn)
    }

    it("external put cache successful") {
      externalClient.addUser(TUserInfo(TUserID("111"), "t_test", 101, "female"))
      externalClient.getUser(TUserID("111")).onSuccess(userInfo => {
        Assertions.assert(userInfo.userId.id.equals("111"))
        Assertions.assert(userInfo.username.equals("t_test"))
        Assertions.assert(userInfo.age.equals(101))
        Assertions.assert(userInfo.sex.equals("female"))
      }).onFailure(fn => throw fn)

    }
  }
}