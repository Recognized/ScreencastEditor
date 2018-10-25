package vladsaif.syncedit.plugin.lang.script.psi

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeModelTest {

  @Test
  fun `test zero depth complete statement boundary`() {
    val model = codeBlockModel {
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
    val model = codeBlockModel {
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
    val model = codeBlockModel {
      statement("statement", 200)
    }
    val beingFind = model.blocks[0] as Statement
    val expected = -1 to -1
    assertEquals(expected, model.findDragBoundary(beingFind))
  }

  @Test
  fun `test complete block boundary`() {
    val model = codeBlockModel {
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
    val model = codeBlockModel {
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
  fun `test merging statement with same time`() {
    val model = codeBlockModel {
      statement("code1", 200)
      statement("code2", 200)
    }
    assertEquals(1, model.blocks.size)
    assertEquals("code1\ncode2", model.blocks[0].code)
  }

  @Test
  fun `test merging statement with block`() {
    val model = codeBlockModel {
      statement("code1", 200)
      block("block2", 200..300) {
      }
    }
    assertEquals(1, model.blocks.size)
    assertEquals("code1\nblock2", model.blocks[0].code)
  }

  @Test
  fun `test replace statement`() {
    val model = codeBlockModel {
      block("block1", 200..300) {
        statement("code1", 250)
      }
    }
  }
}