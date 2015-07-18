package net.badend.anal

import java.net.URLEncoder

import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.core.JsonParser.Feature._
import org.json4s.jackson.JsonMethods
/**
 * Created by jihoonkang on 7/17/15.
 */
object SearchCounter {

  val url = "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q="

  def searchCnt(q:String) = {
    //val json = scala.io.Source.fromURL(url+URLEncoder.encode(q, "UTF8")).mkString

    try {
      0L
      //parse(json).values.asInstanceOf[Map[String, AnyRef]]("responseData").asInstanceOf[Map[String, AnyRef]]("cursor").asInstanceOf[Map[String, AnyRef]]("resultCount").asInstanceOf[String].replaceAll(",", "").toLong
    }catch{
      case e:Exception=>
        //e.printStackTrace()
        //println(q)
        //println(json)
        0L
    }
  }

  def main(args:Array[String]): Unit ={
    println(searchCnt("누구냐넌"))
  }
}
