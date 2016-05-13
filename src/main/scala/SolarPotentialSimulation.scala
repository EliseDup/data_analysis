import utils.Helper
import scala.io.Source
import java.io.PrintStream
import utils.PlotHelper
import gridData.GridCellSolar
import squants.space.Degrees
import squants.energy.KilowattHours

object SolarPotentialSimulation {

  def main(args: Array[String]): Unit = {

    val res = Helper.getLines(Helper.resultsPy + "irradiance").map(l => ( l(0).toDouble,l(1).toDouble,l(2).toDouble))
   
    val sorted = res.sortBy(_._2)
    val sorted2 = sorted.sortBy(i => -i._1)
    val out_stream = new PrintStream(new java.io.FileOutputStream("sorted"))
    
    sorted2.map(l => out_stream.print(l._1.toString + "\t" +l._2.toString + "\t" +l._3.toString + "\n")) 
  }

  /**
   * CRU file format :
   * http://www.ipcc-data.org/observ/clim/observed_fileformat.html
   */
  def readIrradianceData() {

    val lon = (0 until 720).map(i => 0.25 + i * 0.5)
    val lat = (0 until 360).map(i => (89.75 - i * 0.5)).toList

    val irradiance = Source.fromFile("../ressources/CRU/crad6190/crad6190.dat").getLines().toList
    val dim = irradiance.size
    val months = dim / 360

    var rad = Array.ofDim[Double](360, 720)
    for (k <- 0 until months) {
      for (i <- 0 until 360) {
        val res = irradiance(k * 360 + i + 2).split("-|\\ ").filter(i => i != "")
        assert(res.size == 720)
        for (j <- 0 until 720) {
          val value = if (res(j) == "9999") 0.0 else res(j).toString.toDouble
          rad(i)(j) = rad(i)(j) + value
        }
      }
    }
    
    val out_stream = new PrintStream(new java.io.FileOutputStream("irradiance"))
    for (i <- 0 until 360) {
      for (j <- 0 until 720) {
        rad(i)(j) = rad(i)(j) / 12.0
        out_stream.print(lat(i) + "\t" + lon(j) + "\t" + rad(i)(j) + "\n")
      }
    }
    out_stream.close()

  }
}