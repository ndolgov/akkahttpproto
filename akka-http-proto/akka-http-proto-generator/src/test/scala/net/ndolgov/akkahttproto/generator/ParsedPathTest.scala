package net.ndolgov.akkahttproto.generator

import org.scalatest.{Assertions, FlatSpec}

class ParsedPathTest extends FlatSpec with Assertions {
  private val KEY = "k"
  private val VALUE = "v"
  private val PARAM1 = "T123"
  private val PARAM2 = "Param456"

  it should "preserve default semantics for fixed requests" in {
    val path = ParsedPath("/tree/trunk/branch/leaf/get", "chunk")
    assert(path.matchedTuple() == "")
    assert(path.pathMatcher() == "\"tree\" / \"trunk\" / \"branch\" / \"leaf\" / \"get\"")
    assert(path.requestParams() == "")
  }

  it should "support multiple path parameters" in {
    val path = ParsedPath("/tree/trunk/branch/leaf/get/{template}/padding/{param}/", "chunk")
    assert(path.matchedTuple() == " (template, param) =>")
    assert(path.pathMatcher() == "\"tree\" / \"trunk\" / \"branch\" / \"leaf\" / \"get\" / chunk / \"padding\" / chunk")
    assert(path.requestParams() == "template, param")
  }

//  it should "support multiple URL parameter templates" in {
//    assertTwoParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/padding/{param}/"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/padding/$PARAM2/")
//
//    assertTwoParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/{param}/suffix"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/$PARAM2/suffix")
//
//    assertTwoParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/{param}"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/$PARAM2")
//
//    assertTwoParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/{param}/"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/$PARAM2/")
//  }
//
//  it should "merge template and ordinary URL parameters" in {
//    assertMixedParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/padding/{param}/"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/padding/$PARAM2/?$KEY=$VALUE")
//
//    assertMixedParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/{param}/suffix"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/$PARAM2/suffix?$KEY=$VALUE")
//
//    assertMixedParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/{param}"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/$PARAM2?$KEY=$VALUE")
//
//    assertMixedParams(
//      UrlTemplate("/tree/trunk/branch/leaf/get/{template}/{param}/"),
//      s"/tree/trunk/branch/leaf/get/$PARAM1/$PARAM2/?$KEY=$VALUE")
//  }
}