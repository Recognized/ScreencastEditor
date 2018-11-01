package vladsaif.syncedit.plugin.lang.script.psi

import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeModelTest {

  @Test
  fun `test zero depth complete statement boundary`() {
    val model = codeModel {
      block("a", 50..100) {
      }

      statement("statement", 200)

      block("b", 300..400) {
      }
    }
    val beingFind = model.blocks[1] as Statement
    val expected = 100 to 300
    assertEquals(expected, model.findDragBoundary(beingFind))
  }

  @Test
  fun `test zero depth incomplete left statement boundary`() {
    val model = codeModel {
      statement("statement", 200)

      block("b", 300..400) {
      }
    }
    val beingFind = model.blocks[0] as Statement
    val expected = -1 to 300
    assertEquals(expected, model.findDragBoundary(beingFind))
  }

  @Test
  fun `test zero depth incomplete statement boundaries`() {
    val model = codeModel {
      statement("statement", 200)
    }
    val beingFind = model.blocks[0] as Statement
    val expected = -1 to -1
    assertEquals(expected, model.findDragBoundary(beingFind))
  }

  @Test
  fun `test complete block boundary`() {
    val model = codeModel {
      block("outer", 30..900) {
        block("a", 50..100) {
        }

        block("block", 200..250) {
        }

        block("b", 300..400) {
        }
      }
    }
    val beingFind = (model.blocks[0] as Block).innerBlocks[1] as Block
    val expectedLeft = 100 to 250
    val expectedRight = 200 to 300
    assertEquals(expectedLeft, model.findDragBoundary(beingFind, true))
    assertEquals(expectedRight, model.findDragBoundary(beingFind, false))
  }

  @Test
  fun `test complete block boundary with inner`() {
    val model = codeModel {
      block("outer", 30..900) {
        block("a", 50..100) {
        }

        block("block", 200..250) {
          statement("hello", 210)
          block("block2", 220..230) {
          }
        }

        block("b", 300..400) {
        }
      }
    }
    val beingFind = (model.blocks[0] as Block).innerBlocks[1] as Block
    val expectedLeft = 100 to 210
    val expectedRight = 230 to 300
    assertEquals(expectedLeft, model.findDragBoundary(beingFind, true))
    assertEquals(expectedRight, model.findDragBoundary(beingFind, false))
  }

  @Test
  fun `test replace statement`() {
    val model = codeModel {
      block("block1", 200..300) {
        statement("code1", 250)
      }
    }
    model.replace(model.blocks[0].cast<Block>().innerBlocks[0], Statement("newCode", 222))
    val expected = codeModel {
      block("block1", 200..300) {
        statement("newCode", 222)
      }
    }
    assertEquals(expected, model)
  }

  @Test
  fun `test replace block`() {
    val model = codeModel {
      block("block1", 200..300) {
        statement("code1", 250)
      }
      block("block2", 300..500) {
        block("block3", 400..450) {
          statement("hello", 430)
        }
      }
    }
    println(model)
    model.replace(model.blocks[1].cast<Block>().innerBlocks[0], Statement("newCode", 350))
    val expected = codeModel {
      block("block1", 200..300) {
        statement("code1", 250)
      }
      block("block2", 300..500) {
        statement("newCode", 350)
      }
    }
    assertEquals(expected, model)
  }

  @Test
  fun `test offset offsets`() {
    val expected = codeModel {
      block("block1", 200..300) {
        statement("code1", 250)
      }
      block("block2", 300..500) {
        statement("newCode", 350)
      }
    }
    val markedText = expected.createTextWithoutOffsets()
    var newText = expected.serialize()
    for (range in markedText.ranges) {
      newText = newText.replaceRange(range, "")
    }
    assertEquals(markedText.text, newText)
  }
}