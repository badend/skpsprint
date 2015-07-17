package net.badend.dha

import java.io.{BufferedWriter, OutputStreamWriter, BufferedReader, InputStreamReader}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}


import com.twitter.penguin.korean.TwitterKoreanProcessor
import com.twitter.penguin.korean.phrase_extractor.KoreanPhraseExtractor.KoreanPhrase
import com.twitter.penguin.korean.tokenizer.KoreanTokenizer.KoreanToken
import com.twitter.penguin.korean.util.KoreanPos
import org.apache.hadoop.fs.Path


/**
 * Created by jihoonkang on 5/29/15.
 */
object Dha {
  val filterOut = Set(KoreanPos.Noun, KoreanPos.Verb, KoreanPos.Adjective)
  def getFo(text:String): Seq[KoreanToken] = {

    try {

      // Normalize
      val normalized: CharSequence = TwitterKoreanProcessor.normalize(text.trim)

      // 한국어를 처리하는 예시입니다ㅋㅋ #한국어
      //proximity + bm25

      if (normalized.length() >= 1) {
        val tokens: Seq[KoreanToken] = TwitterKoreanProcessor.tokenize(normalized)
        //println(tokens)
        // List(한국어(Noun: 0, 3), 를(Josa: 3, 1),  (Space: 4, 1), 처리(Noun: 5, 2), 하는(Verb: 7, 2),  (Space: 9, 1), 예시(Noun: 10, 2), 입니(Adjective: 12, 2), 다(Eomi: 14, 1), ㅋㅋ(KoreanParticle: 15, 2),  (Space: 17, 1), #한국어(Hashtag: 18, 4))

        // Stemming
        if (tokens.size >= 1) {
          val stemmed: Seq[KoreanToken] = TwitterKoreanProcessor.stem(tokens)

          //println(stemmed)
          // List(한국어(Noun: 0, 3), 를(Josa: 3, 1),  (Space: 4, 1), 처리(Noun: 5, 2), 하다(Verb: 7, 2),  (Space: 9, 1), 예시(Noun: 10, 2), 이다(Adjective: 12, 3), ㅋㅋ(KoreanParticle: 15, 2),  (Space: 17, 1), #한국어(Hashtag: 18, 4))
          val fo = stemmed//.filter(x => filterOut.contains(x.pos))
          fo
        } else {
          Seq.empty[KoreanToken]
        }
      } else {
        Seq.empty[KoreanToken]
      }
    }catch{
      case e:Exception => {
        e.printStackTrace()
        Seq.empty[KoreanToken]
      }
    }
  }

}
