package net.badend.anal

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{StandardOpenOption, Paths, Files}

import scala.collection.immutable.ListMap


/**
 * Created by jihoonkang on 7/18/15.
 */
object CF {
  def main(args:Array[String]) = {
    val fw = Files.newBufferedWriter(Paths.get(args(0)), Charset.forName("UTF8"), StandardOpenOption.CREATE)
    val callback = (item_id: Int, neighbors: ListMap[Int, Double]) => {
      println("item_id", item_id, "neighbors", neighbors)
      fw.synchronized {
        fw.write(s"$item_id,${neighbors.map(x=>s"${x._1},${x._2}").mkString(",")}")
        fw.newLine()
      }

    }

    val proc = new ParallelCF(callback)
    scala.io.Source.fromFile(getClass.getResource("/round2_purchaseRecord.tsv").toURI).getLines().foreach(x=>{
      val s = x.split("\t")
      proc.import_rating(scala.util.hashing.MurmurHash3.stringHash(s(2)), scala.util.hashing.MurmurHash3.stringHash(s(0)))
    })

    /*(new CSVReader[Unit]((line: Map[Symbol, Int]) => {
      proc.import_rating(line('item_id), line('user_id))
    })).read("ratings.csv")*/
    // create a new cf processor


    proc.process()
  }
}
