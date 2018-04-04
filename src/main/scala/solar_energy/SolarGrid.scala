package solar_energy

import squants.energy._
import squants.time.Hours
import squants.space._
import squants.radio._
import utils._
import grid._
import scala.io.Source
import java.io.PrintStream

object SolarGrid {
  def _0_1deg = apply("../resources/data_solar/0_1deg_protected", Degrees(0.1))
  def _0_5deg = apply("../resources/data_solar/0_5deg_slopes", Degrees(0.5))

  def apply(name: String, res: Angle) = {
    val list = Source.fromFile(name).getLines().toList
    new SolarGrid(list.map(SolarCell(_, res)).toList)
  }
}

class SolarGrid(val cells: List[SolarCell]) {
  def country(c: String) = cells.filter(_.country.equalsIgnoreCase(c))

  def write(logFile: String) {
    val out_stream = new PrintStream(new java.io.FileOutputStream(logFile))

    cells.map(c => out_stream.print(c.center.latitude.toDegrees + "\t" + c.center.longitude.toDegrees + "\t" +
      c.distanceToCoast.toKilometers + "\n"))
    out_stream.close()
  }
}

class SolarCell(val center: GeoPoint, val resolution: Angle, val ghi: Irradiance, val dni: Irradiance,
    val landCover: LandCoverType, val distanceToCoast: Length, val elevation: Length,
    val country: String, val protected_area: Boolean = false, val slope : SlopeGradients) {

  val area = Helper.areaRectangle(center, resolution)
  val suitableArea = if (protected_area) SquareKilometers(0) else landCover.solarFactor.mean * area * (1.0 - slope.slope_leq(5, true))
  def area(lcType: LandCoverType): Area = if (lcType.equals(landCover)) area else SquareKilometers(0)

  // Actual area occupied by pv panles / heliostat / ...
  def panelArea(tech: SolarTechnology) = suitableArea / tech.occupationRatio
  def potential(tech: SolarTechnology) = panelArea(tech) * (if (tech.directOnly) dni else ghi) * tech.efficiency * tech.performanceRatio
  def installedCapacity(tech : SolarTechnology) = panelArea(tech) * tech.efficiency * (if(tech.directOnly) dni else WattsPerSquareMeter(1000))
 
  def eroi(tech: SolarTechnology) : Double = {
    if((tech.directOnly && dni.value == 0)||ghi.value == 0 || suitableArea.value == 0) 0.0
    else {
      val out_year = Hours(365 * 24) * potential(tech)
      tech.ee.lifeTime* (out_year / tech.ee.embodiedEnergy(installedCapacity(tech), out_year))
    }
  }
  
}

object SolarCell {
  def apply(line: String, resolution: Angle) = {
    val i = line.split("\t")
    new SolarCell(GeoPoint(Degrees(i(1).toDouble), Degrees(i(0).toDouble)), resolution,
      WattsPerSquareMeter(i(2).toDouble / 24 * 1000), WattsPerSquareMeter(i(3).toDouble / 24 * 1000),
      GlobCoverClasses.landCoverType(i(4).toDouble.toInt), Kilometers(i(5).toDouble), Meters(i(6).toDouble),
      i(7).toString, i(8).toInt == 1, SlopeGradients(i))
  }
  def slope(i: String) = {
    val s = i.toDouble
    if (s > 100) println("Slope > 100 !" + s)
    math.max(0, s / 100.0)
  }
}

object SolarUtils {
  def areaList(list: List[SolarCell]) = list.map(_.area).foldLeft(SquareKilometers(0))(_ + _)
  def suitableAreaList(list: List[SolarCell]) = list.map(_.suitableArea).foldLeft(SquareKilometers(0))(_ + _)
}

/*SLOPES	Slope class
CL1 % ≤ slope ≤ 0.5 %
CL2	0.5 % ≤ slope ≤ 2 %
CL3	2 % ≤ slope ≤ 5 %
CL4	5 % ≤ slope ≤ 10 %
CL5	10 % ≤ slope ≤ 15 %
CL6	15 % ≤ slope ≤ 30 %
CL7	30 % ≤ slope ≤ 45 %
CL8	Slope > 45 %*/

class SlopeGradients(val gradients : List[((Double,Double),Double)]) {
  // Count the percentage of area with slope less that a given threshold
  // If include, the interval contains the given percentage, if not it is strictly less
  val total = gradients.map(_._2).sum
  def slope_leq(percent : Double, include : Boolean = true) = {
    assert(percent <= 100)
    val lastIndex = 
      if(gradients.exists(_._1._2 == percent)) gradients.indexWhere(i => i._1._2 == percent)
      else if(include) gradients.indexWhere(i => percent >= i._1._1 && percent < i._1._2)
      else if(percent <= 0.5/100) 0
      else gradients.indexWhere(i => percent >= i._1._1 && percent < i._1._2) - 1
      
    (0 to lastIndex).map(gradients(_)._2).sum
  }
}
object SlopeGradients {
  val classes = List((0.0,0.5),(0.5,2.0),(2.0,5.0),(5.0,10.0),(10.0,15.0),(15.0,30.0),(30.0,45.0),(45.10,100.0))
  def degreesToPercent(d : Angle) : Double = math.tan(d.toRadians)*100
  def percentToDegrees(p : Double) : Angle = {
    assert(p <= 100)
    Radians(math.atan(p/100.0))
  }
  def apply(line : Array[String]) = {
    val slopes = (9 to 16).toList.map(line(_).toDouble/10000.0)
    new SlopeGradients((0 to 7).toList.map(i => (classes(i),slopes(i)))) 
  }
}
    
    