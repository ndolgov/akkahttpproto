package net.ndolgov.akkahttproto.generator

import scala.collection.mutable.ArrayBuffer

trait ParsedPath {
  /** Generate a string to put inside of the akka-http path directive */
  def pathMatcher(): String

  /** Generate a string representing a tuple returned by the akka-http path directive */
  def matchedTuple(): String

  /** Generate a string representing the request case class parameter list */
  def requestParams(): String
}

private final class ParsedPathImpl(path: String, regex: String) extends ParsedPath {
  private val LCURLY = '{'
  private val RCURLY = '}'
  private val DELIMITER = '/'

  private val pathSegments = ArrayBuffer[String]()

  private val pathParams = ArrayBuffer[String]()

  private var index = 0

  override def pathMatcher(): String = pathSegments.mkString(" / ")

  override def matchedTuple(): String = pathParams.size match {
    case 0 => ""
    case 1 => pathParams(0) + " =>"
    case _ => "(" + pathParams.mkString(", ") + ") =>"
  }

  override def requestParams(): String = pathParams.mkString(", ")

  def parse(): ParsedPath = {
    while (index < path.length) {
      path(index) match {
        case DELIMITER =>
          moveOn()

        case LCURLY =>
          val param = matchParam()
          pathParams += param
          pathSegments += regex

        case _ =>
          pathSegments += "\"" + matchSegment() + "\""
      }
    }

    this
  }

  private def moveOn(): Unit = {
    index += 1
  }

  private def matchChar(ch: Char): Unit = {
    if (path(index) == ch) {
      index += 1
    } else {
      throw new IllegalArgumentException(s"Unexpected character ${path(index)} at $index in $path")
    }
  }

  private def matchParam(): String = {
    matchChar(LCURLY)

    val from = index
    while ((index < path.length) && (path(index) != RCURLY)) {
      index += 1
    }
    val param = path.substring(from, index)

    matchChar(RCURLY)

    param
  }

  private def matchSegment(): String = {
    val from = index
    while ((index < path.length) && (path(index) != DELIMITER)) {
      moveOn()
    }

    path.substring(from, index)
  }
}

object ParsedPath {
  /**
    * @param path an HTTP path extracted from a google.api.http clause
    * @param regex the name of the regexp used to match path parameters in akka-http path directive
    * @return a ParsedPath to use in the code generator
    */
  def apply(path: String, regex: String): ParsedPath = new ParsedPathImpl(path, regex).parse()
}
