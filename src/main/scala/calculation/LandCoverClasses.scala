package calculation

import utils.Helper
import org.apache.poi.hssf.usermodel.HSSFRow

abstract class LandCoverClass(val code: Int, val label: String, val hubHeigthConversionRatio: Double, val classes: LandCoverClasses[_]) {
  override def toString() = "Land Cover Class :" + label + "(" + code + ")"

  def isInWater = classes.waterIndexes.contains(code)
  def isAgriculturalArea = classes.agriculturalAreas.contains(code)
def isOffshoreLess50km = classes.offshoreLess50km.contains(code)

}
abstract class LandCoverClasses[C <: LandCoverClass](legendFileName: String) {
  val waterIndexes: List[Int]
  val openSpaces: List[Int]
  val agriculturalAreas: List[Int]
  val offshoreLess50km: List[Int]
  
  val classes: Map[Int, C] = {
    val sheet = Helper.xlsSheet(Helper.ressourcesPy + "/landCover/" + legendFileName + ".xls", 0)
    (1 to sheet.getLastRowNum).map(r => {
      val c = landCoverClass(sheet.getRow(r))
      (c.code -> c)
    }).toMap
  }
  def landCoverClass(row: HSSFRow): C
  def apply(c: Int) = classes(c)
}

/**
 * Clc_code	Land cover code		varchar(3)	0
 * Grid_code			int(4)	0
 * Label1	Land cover nomenclature labels at level 1		varchar(255)	0
 * Label2	Land cover nomenclature labels at level 2		varchar(255)	0
 * Label3	Land cover nomenclature labels at level 3		varchar(255)	0
 * Level1	Land cover code at level 1		varchar(255)	0
 * Level2	Land cover code at level 2		varchar(255)	0
 * Level3	Land cover code at level 3		varchar(255)	0
 * RGB	RGB color code		varchar(15)	0
 *
 */
class CorineLandCoverClass(val grid_code: Int,
  val level1: Int, val level2: Int, val level3: Int, val clc_code: Int,
  val label1: String, val label2: String, val label3: String, val grb: String,
  hubHeigthConversionRatio: Double, classes: CorineLandCoverClasses) extends LandCoverClass(grid_code, label1 + label2 + label3, hubHeigthConversionRatio, classes)

object CorineLandCoverClass {
  def apply(row: HSSFRow, cl: CorineLandCoverClasses) = {
    new CorineLandCoverClass(Helper.toInt(row, 0), Helper.toInt(row, 1), Helper.toInt(row, 2),
      Helper.toInt(row, 3), Helper.toInt(row, 4), Helper.toString(row, 5),
      Helper.toString(row, 6), Helper.toString(row, 7), Helper.toString(row, 8),
      Helper.toDouble(row, 10), cl)
  }
}

class CorineLandCoverClasses extends LandCoverClasses[CorineLandCoverClass]("clc/clc2000legend") {
  val waterIndexes = (40 to 44).toList ++ List(48, 49, 50, 255)
  val openSpaces = (30 to 34).toList
  val agriculturalAreas = (12 to 22).toList
val offshoreLess50km = List(44)
  def landCoverClass(row: HSSFRow) = CorineLandCoverClass(row, this)
  // 0 also corresponds to sea and ocean ?
  //override def apply(index: Int) = if (index == 0) classes(44) else super.apply(index)
}

class GlobalLandCoverClass(code: Int, label: String, ratio: Double, classes: GlobalLandCoverClasses) extends LandCoverClass(code, label, ratio, classes)
object GlobalLandCoverClass {
  def apply(row: HSSFRow, cl: GlobalLandCoverClasses) = new GlobalLandCoverClass(Helper.toInt(row, 0), Helper.toString(row, 1), Helper.toDouble(row, 5), cl)
}
class GlobalLandCoverClasses extends LandCoverClasses[GlobalLandCoverClass]("glc/Global_Legend") {
  val waterIndexes = (20 to 23).toList
  val openSpaces = List(11, 12)
  val agriculturalAreas = List(16)
  val offshoreLess50km = List()
  def landCoverClass(row: HSSFRow) = GlobalLandCoverClass(row, this)

}