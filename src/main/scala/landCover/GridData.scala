package landCover

import scala.io.Source
import utils.Helper
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVWriter
import utils.GeoPoint
import java.io.PrintStream
import squants.motion.Velocity
import squants.mass.KilogramsPerCubicMeter
import squants.radio.WattsPerSquareMeter
import squants.time.Hours
import squants.energy.Megawatts
import squants.energy.Energy
import squants.energy.Terajoules
import squants.motion.MetersPerSecond
import squants.energy.KilowattHours
import squants.energy.GigawattHours
import utils.TerawattHours
import squants.space.Degrees
import squants.space.Angle
import squants.energy.Watts
import squants.energy.WattHours
import utils.PlotHelper
import landCover._
import operation._
import squants.space.Length
import squants.space.Meters

class GridData(name: String, val gridSize: Angle, val onshoreTurbine: WindTurbine, val offshoreTurbine: WindTurbine) {
  // Coefficients for wind extrapolation depends on Land Cover class
  val clcClasses = new CorineLandCoverClasses()
  val glcClasses = new GlobCoverClasses()

  val grids: List[GridObject] = {
    val lines = Source.fromFile(Helper.ressourcesPy + name).getLines().toList
    lines.map(l => GridObject(l, this)).toList
  }
  val clcGrids = grids.filter(_.clc.isDefined)

  def windSpeeds(gr: List[GridObject] = grids, atHub : Boolean = false) = if(atHub) gr.map(_.windSpeedAtHub().value) else gr.map(_.windSpeed.value)
  def powerDensities(gr: List[GridObject] = grids, atHub : Boolean = false) = if(atHub) gr.map(_.powerDensityAtHub().value) else gr.map(_.powerDensity.value)
  def energyGenerated(gr: List[GridObject] = grids, minEROI: Double = 0.0) = gr.map(_.energyGeneratedPerYear(minEROI = minEROI)).foldLeft(TerawattHours(0.0))(_ + _)
  def nTurbines(gr: List[GridObject] = grids) = gr.map(_.nTurbines).sum
  def erois(gr: List[GridObject] = grids) = gr.map(_.EROI)

  def plotEROIVSCumulatedProduction(gr: List[GridObject] = grids) = {
    val eroiPro = gr.map(g => (g.EROI, g.energyGeneratedPerYear())).sortBy(_._1).reverse
    var tot = 0.0
    val eroiCum = eroiPro.map(i => {
      tot = tot + i._2.to(TerawattHours)
      (i._1, tot)
    })
    PlotHelper.plotXY(List((eroiCum.map(_._2), eroiCum.map(_._1), "")), xLabel = "Cumulated Annual Production [TWh]", yLabel = "EROI")
  }

  def landGrids(gr: List[GridObject] = grids) = gr.filter(g => !g.water)
  def agriculturalAreas(gr: List[GridObject] = grids) = gr.filter(_.lc.isAgriculturalArea)
  def offshoreGrids(gr: List[GridObject] = grids) = gr.filter(g => g.seaLevel <= Meters(50))
  println("Total n grids" + grids.size + " - clc grids " + clcGrids.size)
  println("Total Energy Generated : " + energyGenerated() + "\t" + nTurbines())
  println("Energy Generated No Water: " + energyGenerated(landGrids()) + "\t" + nTurbines(landGrids(clcGrids)))
  println("Energy Generated Agriculture: " + energyGenerated(agriculturalAreas()) + "\t" + nTurbines(agriculturalAreas(clcGrids)))

  def writeGridToCSV(name: String, gr: List[GridObject] = grids) {
    val writer = new CSVWriter(new FileWriter(name))
    writer.writeNext(Array("LATITUDE", "LONGITUDE", "WIND_SPEED", "WIND_SPEED_AT_HUB"))
    gr.map(g => {
      writer.writeNext(Array(g.center.latitude.toString, g.center.longitude.toString, g.windSpeed.toString, g.windSpeedAtHub().value.toString))
    })
    writer.close()
  }
  def writeGrid(name: String, gr: List[GridObject] = grids) {
    val out_stream = new PrintStream(new java.io.FileOutputStream(name))
    gr.map(g => {
      out_stream.print(g.center.latitude.value.toString + "\t" + g.center.longitude.value.toString +
        "\t" + g.uWind.value.toString + "\t" + g.vWind.value.toString +
        "\t" + g.windSpeed.value.toString + "\t" + g.windSpeedAtHub().value.toString +
        "\t" + g.clcCode.toDouble.toString + "\t" + g.glcCode.toDouble.toString +
        "\t" + g.loadHours().value.toString + "\n")
    })
    out_stream.close()
  }
}

/**
 * From data of ERA-40 dataset
 *
 */
class GridObject(val center: GeoPoint, val gridSize: Angle, val turbine: WindTurbine, val uWind: Velocity,
    val vWind: Velocity, val windSpeed: Velocity,
    val clc: Option[CorineLandCoverClass], val glc: Option[GlobCoverClass],
    val seaLevel : Length) {
val water = seaLevel.value < 0
  val lc: LandCoverClass = if (clc.isDefined) clc.get else glc.get
  val clcCode: Int = if (clc.isDefined) clc.get.code else -1
  val glcCode: Int = if (glc.isDefined) glc.get.code else -1
  val h0 = Meters(10)
  def windSpeedAtHub(h: Length = turbine.hubHeight): Velocity = {
    Math.log(h.toMeters / lc.z0.toMeters) / Math.log(h0.toMeters / lc.z0.toMeters) * windSpeed
  }
  val airDensity = KilogramsPerCubicMeter(1.225)
  val powerDensity = WattsPerSquareMeter(0.5 * airDensity.value * Math.pow(windSpeed.value, 3))
  def powerDensityAtHub(h: Length = turbine.hubHeight) = WattsPerSquareMeter(0.5 * airDensity.value * Math.pow(windSpeedAtHub(h).value, 3))

  /**
   * Calculate the cell size in km^2
   * Lat,Lon represent the center of the cell
   * =>  ___________  lat+0125/2
   *    |						|
   *    |						|
   *    |			o 		|
   *    |						|
   *    |						|
   *     ___________ lat-0125/2
   * lon-0.125/2     lon+0.125/2
   *
   */

  val s = gridSize / 2.0;
  val lowerLeftCorner = GeoPoint(center.latitude - s, center.longitude - s)
  val upperRightCorner = GeoPoint(center.latitude + s, center.longitude + s)
  val area = Helper.areaRectangle(lowerLeftCorner, upperRightCorner)

  /**
   * Regarding average wind energy production potential per square kilometre,
   * it is considered that five 2 MW wind turbines can be sited per square kilometre onshore.
   *
   * And the load hours according to the rule : y = 626,38x – 2003,3
   *
   * => Result in Wh
   */

  def loadHours(h: Length = turbine.hubHeight) = Hours(Math.max(0, 626.38 * windSpeedAtHub(h).value - 2003.3))
  val nTurbines = if (loadHours().value <= 0) 0 else area.toSquareKilometers * turbine.nPerSquareKM
  val powerInstalled = nTurbines * turbine.ratedPower
  def energyGeneratedPerYear(minSpeed: Velocity = MetersPerSecond(0), minEROI: Double = 0.0, h: Length = turbine.hubHeight): Energy = {
    if (windSpeedAtHub(h) < minSpeed || EROI < minEROI) WattHours(0)
    else powerInstalled * loadHours(h)
  }

  val EROI = {
    if (nTurbines == 0) 0.0
    else {
      val out = turbine.lifeTime * turbine.ratedPower * loadHours()
      val in = turbine.components.embodiedEnergy
      out / in
    }
  }
}

object GridObject {
  def apply(line: String, data: GridData) = {
    val csvLine = line.split("\t")
    val clcClass = if (csvLine(5).equals("NA") || csvLine(5).toInt == 0) None else Some(data.clcClasses(csvLine(5).toInt))
    val glcClass = if (csvLine(7).equals("NA")) None else Some(data.glcClasses(csvLine(7).toInt))
    val lc = if (clcClass.isDefined) clcClass.get else glcClass.get
    val turbine = if(csvLine(8).toDouble<0) data.offshoreTurbine else data.onshoreTurbine
    new GridObject(GeoPoint(Degrees(csvLine(0).toDouble), Degrees(csvLine(1).toDouble)), data.gridSize, turbine,
      MetersPerSecond(csvLine(2).toDouble), MetersPerSecond(csvLine(3).toDouble), MetersPerSecond(csvLine(4).toDouble), clcClass, glcClass, Meters(csvLine(8).toDouble))
  }
}

