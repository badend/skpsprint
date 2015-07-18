package net.badend.anal

import java.nio.charset.Charset
import java.nio.file.{StandardOpenOption, Paths, Files}
import java.util.zip.GZIPOutputStream

import com.twitter.penguin.korean.tokenizer.KoreanTokenizer.KoreanToken
import com.twitter.penguin.korean.util.KoreanPos._
import net.badend.dha.Dha
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.io.Codec
import scala.util.Try


/**
 * Created by jihoonkang on 7/17/15.
 */
object SKPSprint {



  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val features = new GZIPOutputStream(Files.newOutputStream(Paths.get(args(0)), StandardOpenOption.CREATE))

    val filterOut = Seq(

      Josa, Eomi, PreEomi, Conjunction,
      KoreanParticle,
      Punctuation, ScreenName,
      Email, URL, CashTag,

      // Functional POS
      Space)

    class Item(val glssId:String, val serisId:String, val infoTpId:String, val category:String, val title:String, val openDt:DateTime)

    val LABEL_ITEM = Seq("GLSS_ID",
      "SERIS_ID",
      "INFO_TP_ID",
      "CATEGORY",
      "TITLE",
      "SYNOPSIS",
      "PRODUCTION_YR",
      "OPEN_DT",
      "SERIS_TM",
      "DIRECTOR_NM_L",
      "ACTR_NM_LIST",
      "GENRE_NM_LIST",
      "CNTRY_NM_LIST",
      "LVL_CD",
      "CRT_DTTM")

    val dtFormat = DateTimeFormat.forPattern("yyyyMMdd")
    val buyFtFormat = DateTimeFormat.forPattern("yyyy-MM-dd")


    //val idTermFileWriter = Files.newBufferedWriter(Paths.get("/Users/jihoonkang/git/sprint2/conf/idTerm.csv"), Charset.forName("UTF8"), StandardOpenOption.CREATE)

    /**
     * 아이템 아이디와 아이템 정보들 들어있음 원본
     */
    val items: Map[String, Item] = scala.io.Source.fromFile(getClass.getResource("/round2_itemInfo.tsv").toURI)(Codec.UTF8).getLines.map(x => {

      val item = x.split("\t", -1)
/*
      val li = collection.mutable.Map.empty[String, Any] ++ LABEL_ITEM.zip(item).toMap

      li("OPEN_DT") = dtFormat.formatted(li("OPEN_DT").asInstanceOf[String])*/

      //(li("GLSS_ID").asInstanceOf[String], li)
      (item(0), new Item(item(0), item(1), item(2), item(3), item(4), if(item(5).trim.size>0) dtFormat.parseDateTime((item(5).replaceAll("0000","").replaceAll(".","")+ "01010101").take(8)) else null))


    }).toMap
    val topic = scala.io.Source.fromFile(getClass.getResource("/topic.csv").toURI).getLines.map(x => {
      val s = x.split(",", -1)
      (s.head, s.drop(1).map(x => x.toDouble))
    }).toMap



    class Purchase(val userId:String, val buyDt:DateTime, val glssId:String, val fglassId:String, val totUseAmount:Long)

    //구매 이력, 키가 없는 상태
    val pur = scala.io.Source.fromFile(getClass.getResource("/round2_purchaseRecord.tsv").toURI)(Codec.UTF8).getLines.map(x => {
      val s = x.split("\t", -1)
      (new Purchase(s(0), buyFtFormat.parseDateTime(s(1)),  s(2),  f"${s(2).toLong}%011d", s(3).toLong))
    }).toList


    //구매 이력, 사용자가 키
    val userPur = pur.groupBy(x => x.userId)
    //구매 이력, 컨텐츠가 키
    val glssPur = pur.groupBy(x => x.glssId)



    val bm25k = 1.2D
    val bm25b = 0.75D


    def bm25(tf: Long, viewern: Int, usersn: Int, dN_avgdl: Double) = {

      //println(s"math.log((math.max($usersn - $viewern,0) + 0.5) / ($viewern + 0.5))")
      val idfq = math.log((math.max(usersn - viewern, 0) + 0.5) / (viewern + 0.5))
      //println(s"$idfq * (($tf * ($bm25k+1)) / ($tf + $bm25k * ( 1-bm25b + bm25b*$dN_avgdl)))")
      idfq * ((tf * (bm25k + 1)) / (tf + bm25k * (1 - bm25b + bm25b * dN_avgdl)))

    }


    val output = Files.newBufferedWriter(Paths.get(args(1)), Charset.forName("UTF8"), StandardOpenOption.CREATE)


    userPur.par.map(x => {
      val user = x._1
      val purInfo = x._2

      //println("--------")
      val topicMatching = purInfo.par.map(p => {
        val glssid = p.glssId
        val lk = glssid.asInstanceOf[String].toLong.toString
        val t = topic(lk)
        val item = items(glssid)
        val weight = bm25(p.totUseAmount + 500, glssPur(glssid).size, userPur.size, purInfo.size.toDouble / 65.54216590114436D)

        //print(weight + ":" + item("TITLE") + " " + item("SERIS_TM"))

        t.map(tt=> tt * weight)

      }).fold(Array.fill(40)(0D)) {
        (x, y) =>
          x.zip(y).map(x => x._1 + x._2)
      }.map(x => x / purInfo.size)
      //println("--------")
      val scoresMovie = topic.par.map(t => {
        val key = f"${t._1.toLong}%011d"
        if(purInfo.contains(key)){None}else {
          val matchingsum =topicMatching.zip(t._2).map(x => x._1 * x._2).sum
          val days = if(items(key).openDt != null ) (System.currentTimeMillis() - items(key).openDt.getMillis)/ (1000L * 60 * 60 * 24) else 0
          val recency = 1/(1+math.exp(days * 0.008 - 4))

          val item = items(key)
          val googleCntScore = 1/(1+math.exp(-1 *SearchCounter.searchCnt(item.title + " 다시보기") * 0.00005 + 5))

          val gpur = glssPur.get(key)
          val rev = if(gpur.isDefined) gpur.get.map(x=>x.totUseAmount).sum else 0
          val views = if(gpur.isDefined) gpur.get.size else 0

          val revScore = 1/(1+math.exp(rev * -0.000012 +3))
          val viewScore = 1/(1+math.exp(views * -0.013 + 3 ))
          //println(matchingsum, recency, googleCntScore, revScore, viewScore, item("TITLE"))
          features.synchronized {
            features.write(s"${x._1},${key},${matchingsum},${recency},${googleCntScore},${revScore},${viewScore}\n".getBytes("UTF8"))

          }
          Some(matchingsum*6 + recency*2 + googleCntScore + revScore*2 + viewScore*2, t._1)
        }
      }).flatten.filter(x => x._1 != 0).toArray.sortBy(x => x._1 * -1)


      scoresMovie.take(100).foreach(x => {
        val key = f"${x._2.toLong}%011d"
        if (items.get(key).isDefined) {

          output.synchronized {
            output.write(s"$user,$key")
            output.newLine()
          }

        } else {
          println("왜없지..? " + x)
        }
      })


    })
    output.close
  }
}
