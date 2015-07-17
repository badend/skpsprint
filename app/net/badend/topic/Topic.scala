package net.badend.topic

import com.fasterxml.jackson.core.JsonParser.Feature._
import com.piki.ds.PikiHdfs
import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.spark.{SparkContext, SparkConf}
import org.json4s.jackson.JsonMethods
import org.json4s.jackson.JsonMethods._
import org.apache.log4j.{Level, Logger}

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.clustering.LDA
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import scala.reflect.runtime.universe._


abstract class AbstractParams[T: TypeTag] {
  private def tag: TypeTag[T] = typeTag[T]
  /**
   * Finds all case class fields in concrete class instance, and outputs them in JSON-style format:
   * {
   *   [field name]:\t[field value]\n
   *   ...
   * }
   */
  override def toString: String = {
    val tpe = tag.tpe
    val allAccessors = tpe.declarations.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }
    val mirror = runtimeMirror(getClass.getClassLoader)
    val instanceMirror = mirror.reflect(this)
    allAccessors.map { f =>
      val paramName = f.name.toString
      val fieldMirror = instanceMirror.reflectField(f)
      val paramValue = fieldMirror.get
      s"  $paramName:\t$paramValue"
    }.mkString("{\n", ",\n", "\n}")
  }
}

case class Params(
                   input: Seq[String] = Seq.empty,
                   k: Int = 40,
                   maxIterations: Int = 150,
                   docConcentration: Double = -1,
                   topicConcentration: Double = -1,
                   vocabSize: Int = 10647500,
                   stopwordFile: String = "",
                   checkpointDir: Option[String] = None,
                   checkpointInterval: Int = 10) extends AbstractParams[Params]
/**
 * Created by jihoonkang on 6/4/15.
 */
object Topic {


  def getSparkContext()= {

    val conf = new SparkConf().setAppName("ㄲㅎ")


    conf.setMaster("yarn-client")
    conf.set("master" , "yarn-client")
    conf.set("spark.app.name" , "ㄲㅎ")
    conf.set("spark.executor.instances" , "10")
    conf.set("spark.executor.memory" , "1500m")
    conf.set("spark.storage.memoryFraction" , "0.5")
    val sc = new SparkContext(conf)
/*
 <property>
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
  </property>
  <property>
    <name>yarn.nodemanager.aux-services.mapreduce_shuffle.class</name>
    <value>org.apache.hadoop.mapred.ShuffleHandler</value>
  </property>
  <property>
    <name>yarn.resourcemanager.resource-tracker.address</name>
    <value>kr-data-h1:8025</value>
  </property>
  <property>
    <name>yarn.resourcemanager.scheduler.address</name>
    <value>kr-data-h1:8030</value>
  </property>
  <property>
    <name>yarn.resourcemanager.address</name>
    <value>kr-data-h1:8035</value>
  </property>
 */
    sc.hadoopConfiguration.addResource(PikiHdfs.pikihdfs.conf)
    sc.hadoopConfiguration.set("fs.defaultFS", "hdfs://kr-data-h1:9000")
    sc.hadoopConfiguration.set("yarn.resourcemanager.resource-tracker.address", "kr-data-h1:8025")
    sc.hadoopConfiguration.set("yarn.resourcemanager.scheduler.address", "kr-data-h1:8030")
    sc.hadoopConfiguration.set("yarn.resourcemanager.address", "kr-data-h1:8035")
    sc

  }

  val sc = getSparkContext()
  def main(args:Array[String]) = {
    getTopic
  }

  def getTopic = {





    val features = Seq(
      ALLOW_COMMENTS,
      ALLOW_NON_NUMERIC_NUMBERS,
      ALLOW_NUMERIC_LEADING_ZEROS,
      ALLOW_SINGLE_QUOTES,
      ALLOW_UNQUOTED_CONTROL_CHARS,
      ALLOW_UNQUOTED_FIELD_NAMES)
    features.foreach(JsonMethods.mapper.configure(_, true))


    // Load and parse the data
    val data = sc.textFile("/user/jihoonkang/idTerm.csv")
    val parsedData = data.map(s => {
      val spt = s.split("\t", -1)
      (spt(0).toLong,
        (spt.drop(1)))})


    val tokenized = parsedData.map(x=>(x._1, x._2)).map(x=>(x._1, x._2.map(z=>(z, scala.util.MurmurHash.stringHash(z).toLong))))
    val voc = tokenized.map(x=>x._2).flatMap(x=>x.map(w=>(w._1, 1L)))

    val wordCounts: RDD[(String, Long)] = voc.reduceByKey(_ + _)
    wordCounts.cache()
    val fullVocabSize = wordCounts.count().toInt

    val vocabSize = fullVocabSize
    // Select vocab
    //  (vocab: Map[word -> id], total tokens after selecting vocab)
    val (vocab: Map[String, Int], selectedTokenCount: Long) = {
      val tmpSortedWC: Array[(String, Long)] = if (vocabSize == -1 || fullVocabSize <= vocabSize) {
        // Use all terms
        wordCounts.collect().sortBy(-_._2)
      } else {
        // Sort terms to select vocab
        wordCounts.sortBy(_._2, ascending = false).take(vocabSize)
      }
      (tmpSortedWC.map(_._1).zipWithIndex.toMap, tmpSortedWC.map(_._2).sum)
    }
    vocab.size
    val vocabArray = new Array[String](vocab.size)
    vocab.foreach { case (term, i) => vocabArray(i) = term }
    val documents = tokenized.map { case (id, tokens) =>
      // Filter tokens by vocabulary, and create word count vector representation of document.
      val wc = new scala.collection.mutable.HashMap[Int, Int]()
      tokens.foreach { term =>
        if (vocab.contains(term._1)) {
          val termIndex = vocab(term._1)
          wc(termIndex) = wc.getOrElse(termIndex, 0) + 1
        }
      }
      val indices = wc.keys.toArray.sorted
      val values = indices.map(i => wc(i).toDouble)
      val sb = Vectors.sparse(vocab.size, indices, values)
      (id, sb)
    }
    documents.take(1)

    val params = Params()
    val (corpus, actualNumTokens) = (documents, selectedTokenCount)
    corpus.cache()
    val actualCorpusSize = corpus.count()
    val actualVocabSize = vocabArray.size

    // Run LDA.
    val ldaParams = new LDA()
    ldaParams.setK(params.k).setMaxIterations(params.maxIterations)      .setDocConcentration(params.docConcentration)      .setTopicConcentration(params.topicConcentration)      .setCheckpointInterval(params.checkpointInterval)

    if (params.checkpointDir.nonEmpty) {
      sc.setCheckpointDir(params.checkpointDir.get)
    }
    val startTime = System.nanoTime()
    val ldaModel = ldaParams.run(corpus)
    val elapsed = (System.nanoTime() - startTime) / 1e9
    val lda1=ldaModel.topicDistributions.map(x=>s"${x._1},${x._2.toArray.mkString(",")}").coalesce(1, false)//.saveAsTextFile("/user/don/lda_k50_i150_4")
    val lda = lda1.map(x=>{
        import au.com.bytecode.opencsv.CSVParser
        val parser = new CSVParser(',', ''')
        try{
          Some(parser.parseLine(x))
        }
        catch{
          case e: Exception => None

        }
      }).flatMap(x=>x).map(x=>(x(0),x.drop(1)))



   val MG_CONTENTS_label = Array("contents_id",
      "status",
      "udate", //reserving time
      "title",
      "description",
      "thumbnail_url",
      "cover_url",
      "headline_url",
      "bgm_source",
      "bgm_text",
      "vertical",
      "uid",
      "cdate", //DEFAULT CURRENT_TIMESTAMP
      "contents_type", //(album(old type), collection(old type), PAST, CHST)
      "view_count", //currently not using
      "edate", //DEFAULT NULL
      "log_detail",
      "original_text",
      "original_url",
      "thumbnail_source_text",
      "thumbnail_source_url",
      "cover_source_text", //about cover source
      "cover_source_url",
      "fdate", //DEFAULT NULL
      "thumbnail_best_url")
    MG_CONTENTS_label.size
    val MG_CONTENTS_text = sc.textFile("hdfs://kr-data-h1:9000/data/db/20150601/MG_CONTENTS/*")
    val MG_CONTENTS = MG_CONTENTS_text.map(x=>{
      import au.com.bytecode.opencsv.CSVParser
      val parser = new CSVParser(',', ''')
      val s = parser.parseLine(x)
      val format_date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      if(s.size==25 && s(1).equals("ACTV"))
        Some(
          MG_CONTENTS_label.zip(s).toMap
        )
      else
        None
    }).flatMap(x=>x).map(x=>(x("contents_id"),(x("title"),x("uid"))))



    /*sc.textFile("/data/db/20150601/USER/").map(x=>{
      import au.com.bytecode.opencsv.CSVParser
      val parser = new CSVParser(',', ''')
      try{

        val p = parser.parseLine(x)
        if(!p(9).equals("NORMAL")){
          Some(p(0), p(1))
        } else None

      }
      catch{
        case e: Exception => None

      }
    }).flatMap(x=>x)
*/
    // val joined = lda.join(MG_CONTENTS)
    //val cid = joined.map(x=>(x._1, x._2._2._2, x._2._1))
  }
}
