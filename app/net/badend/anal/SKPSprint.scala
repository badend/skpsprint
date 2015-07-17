package net.badend.anal

import java.nio.charset.Charset
import java.nio.file.{StandardOpenOption, Paths, Files}

import com.twitter.penguin.korean.tokenizer.KoreanTokenizer.KoreanToken
import com.twitter.penguin.korean.util.KoreanPos._
import net.badend.dha.Dha
import org.joda.time.format.DateTimeFormat

import scala.util.Try


/**
 * Created by jihoonkang on 7/17/15.
 */
object SKPSprint {

  val filterOut = Seq(

  Josa, Eomi, PreEomi, Conjunction,
  KoreanParticle,
  Punctuation, ScreenName,
  Email, URL, CashTag,

  // Functional POS
  Space)


  val LABEL_ITEM =Seq("GLSS_ID",
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
  val items = scala.io.Source.fromFile(getClass.getResource("/round2_itemInfo.tsv").toURI).getLines.foreach(x=>{

    val item = x.split("\t", -1)

    val li = collection.mutable.Map.empty[String, Any] ++ LABEL_ITEM.zip(item).toMap


//    val titles: Seq[KoreanToken] = Try{Dha.getFo(li.get("TITLE").get.asInstanceOf[String]).filter(x =>{!filterOut.contains(x.pos)})}.getOrElse(Seq.empty[KoreanToken])
 //   val syno: Seq[KoreanToken] = Try{Dha.getFo(li.get("SYNOPSIS").get.asInstanceOf[String]).filter(x => !filterOut.contains(x.pos))}.getOrElse(Seq.empty[KoreanToken])

    (li("GLSS_ID"), li)

  })
  val topic = scala.io.Source.fromFile(getClass.getResource("/topic.csv").toURI).getLines.map(x=>{
    val s = x.split(",", -1)
    (s.head, s.drop(1).map(x=>x.toDouble))
  }).toMap



  val pur = scala.io.Source.fromFile(getClass.getResource("/round2_purchaseRecord.tsv").toURI).getLines.map(x=>{
    val s = x.split("\t", -1)
    (Map("USER_ID"->s(0), "BUY_DT" -> buyFtFormat.parseDateTime(s(1)), "GLSS_ID" -> s(2), "TOT_USE_AMOUT" -> s(3)))
  }).toList

  val userPur = pur.groupBy(x=>x("USER_ID"))
  val glssPur = pur.groupBy(x=>x("GLSS_ID"))

  def main(args:Array[String]) = {

  }
}
