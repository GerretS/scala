package scala.collection.immutable

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test
import org.junit.Assert._

import scala.collection.GenTraversableOnce
import scala.tools.testing.AllocationTest

@RunWith(classOf[JUnit4])
class SeqTest extends AllocationTest{

  @Test def emptyNonAllocating(): Unit = {
    nonAllocating(Seq.empty)
    nonAllocating(Seq())
  }
  @Test def mapOverListWithSeqCBF(): Unit = {
    val t1 = Seq[String]()
    object fn extends Function1[String, String] {
      override def apply(v1: String): String = ""
    }
    assertSame(Nil, nonAllocating(t1 map fn))
    val t2 = Seq[String]("abc")
    //each :: is 20 bytes (padded to 24)
    exactAllocates(24) (t2 map fn)
  }
  @Test def flatMapOverListWithSeqCBF(): Unit = {
    val t1 = Seq[String]()
    object fn1 extends Function1[String, Seq[String]] {
      override def apply(v1: String): Seq[String] = Nil
    }
    object fn2 extends Function1[String, GenTraversableOnce[String]] {
      val r = Seq("xx")
      override def apply(v1: String): GenTraversableOnce[String] = r
    }
    object fn3 extends Function1[String, GenTraversableOnce[String]] {
      val r = List("xx")
      override def apply(v1: String): GenTraversableOnce[String] = {
        if (v1 == "def") Nil
        else r
      }
    }
    assertSame(Nil, nonAllocating(t1 flatMap fn1))
    val t2 = Seq[String]("abc")
    val t3 = List[String]("abc", "def")
    //flatMap on not empty generates an appender (20 bytes + padding) = 24
    //each :: is 20 bytes (padded to 24)

    //nonAllocating as fn results are Nil
    assertSame(Nil, nonAllocating(t2 flatMap fn1))
    assertSame(Nil, nonAllocating(t3 flatMap fn1))

    //doesnt generate the appender, shares the last (only) list
    assertEquals(List("xx"), nonAllocating(t2 flatMap fn2))

    //generates the appender, one ::, shares the last list
    exactAllocates(48)(t3 flatMap fn2)

    //doesnt generate the appender, shares the last (non empty) list
    assertEquals(List("xx"), nonAllocating(t3 flatMap fn3))
  }
  @Test def collectOverListWithSeqCBF(): Unit = {
    val t1 = Seq[String]()
    object fn extends PartialFunction[String, Seq[String]] {
      override def isDefinedAt(x: String): Boolean = true
      override def apply(v1: String): Seq[String] = Nil
    }
    assertSame(Nil, nonAllocating(t1 collect fn))
    val t2 = Seq[String]("abc")
    //each :: is 20 bytes (padded to 24)
    exactAllocates(24) (t2 map fn)
  }
}
